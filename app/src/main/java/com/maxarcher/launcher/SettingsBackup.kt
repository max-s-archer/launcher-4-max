package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.maxarcher.launcher.ui.drawer.DrawerApplicationSize
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation

/** Read and restore access to the favorite state for the backup and restore journey. */
internal interface BackupFavoritesAccess {
    fun currentOrderedAggregate(): OrderedFavoriteAggregate?

    suspend fun restoreAggregate(aggregate: OrderedFavoriteAggregate): Boolean
}

/** Read and restore access to the Drawer display settings for the backup journey. */
internal interface BackupSettingsAccess {
    fun currentSettings(): DrawerDisplaySettings?

    suspend fun restoreSettings(settings: DrawerDisplaySettings): Boolean
}

/** Read and restore access to the successfully saved icon-shape preference. */
internal interface BackupIconShapeAccess {
    fun currentIconShapeSettings(): IconShapeSettings?

    suspend fun restoreIconShapeSettings(settings: IconShapeSettings): Boolean
}

/** One parsed backup document containing the saved favorites and settings. */
internal data class SettingsBackupState(
    val aggregate: OrderedFavoriteAggregate,
    val settings: DrawerDisplaySettings,
    val bindings: QuickActionBindings = QuickActionBindings(),
    val iconShape: IconShapeSettings = IconShapeSettings(),
)

/**
 * Builds the suggested backup display name from the current application version name
 * and the device's local date and time. The timestamp pattern is evaluated under a
 * fixed locale so the `yyyyMMddHHmm` form never changes with locale or 12/24-hour
 * settings.
 */
internal fun backupFileName(
    versionName: String,
    timestamp: LocalDateTime,
): String {
    val stamp = timestamp.format(
        DateTimeFormatter.ofPattern(BACKUP_TIMESTAMP_PATTERN).withLocale(Locale.US),
    )
    return "launcher4max-backup-$versionName-$stamp.json"
}

private const val BACKUP_TIMESTAMP_PATTERN = "yyyyMMddHHmm"

/**
 * Serializes and parses the schema-versioned backup document. A backup whose schema
 * version is higher than the current application schema is rejected; a backup whose
 * schema version is equal to or lower is interpreted under the current schema. A
 * missing section restores that section's default state, and a missing field inside a
 * present section restores that field's default value.
 */
internal object SettingsBackupJson {
    const val BACKUP_SCHEMA_VERSION = 6

    fun serialize(
        aggregate: OrderedFavoriteAggregate,
        settings: DrawerDisplaySettings,
        bindings: QuickActionBindings = QuickActionBindings(),
        iconShape: IconShapeSettings = IconShapeSettings(),
    ): String {
        val favorites = JSONObject().put(
            "modules",
            JSONArray().apply {
                aggregate.modules.forEach { module ->
                    put(
                        JSONObject()
                            .put("id", module.id)
                            .put("type", module.type.storageValue)
                            .put("iconSize", module.iconSize.storageValue)
                            .put("textSize", module.textSize.storageValue)
                            .put("namePlacement", module.namePlacement.storageValue)
                            .put("itemsPerRow", module.itemsPerRow)
                            .put(
                                "identities",
                                module.identities.toJson(),
                            ),
                    )
                }
            },
        ).put(
            "homeBottomAppBar",
            JSONObject()
                .put("enabled", aggregate.homeBottomAppBar.enabled)
                .put("presentation", aggregate.homeBottomAppBar.presentation.storageValue)
                .put("iconSize", aggregate.homeBottomAppBar.iconSize.storageValue)
                .put("identities", aggregate.homeBottomAppBar.identities.toJson()),
        )
        val displaySettings = JSONObject()
            .put("iconSize", settings.iconSize.storageValue)
            .put("textSize", settings.textSize.storageValue)
            .put("namePlacement", settings.namePlacement.storageValue)
            .put("itemsPerRow", settings.itemsPerRow)
            .put(
                "sectionAnchorPresentation",
                settings.sectionAnchorPresentation.storageValue,
            )
            .put("backgroundOpacity", settings.backgroundOpacity)
        val quickActionBindings = JSONObject()
            .put("doubleTap", bindings.doubleTap.storageValue)
            .put("longPress", bindings.longPress.storageValue)
        return JSONObject()
            .put("schemaVersion", BACKUP_SCHEMA_VERSION)
            .put("favorites", favorites)
            .put("displaySettings", displaySettings)
            .put("quickActionBindings", quickActionBindings)
            .put(
                "iconShape",
                JSONObject()
                    .put("mode", iconShape.mode.storageValue)
                    .put("freeformClip", iconShape.freeformClip),
            )
            .toString(2)
    }

    fun parse(text: String): SettingsBackupState? = try {
        val root = JSONObject(text)
        val schemaVersion = root.getInt("schemaVersion")
        if (schemaVersion > BACKUP_SCHEMA_VERSION ||
            (root.has("favorites") && root.optJSONObject("favorites") == null) ||
            (root.has("displaySettings") && root.optJSONObject("displaySettings") == null) ||
            (root.has("quickActionBindings") &&
                root.optJSONObject("quickActionBindings") == null)
        ) {
            null
        } else {
            val aggregate = parseFavorites(root.optJSONObject("favorites"))
            val settings = parseDisplaySettings(root.optJSONObject("displaySettings"))
            val bindings = parseBindings(root.optJSONObject("quickActionBindings"))
            if (aggregate == null || settings == null || bindings == null ||
                !isValidOrderedFavoriteAggregate(aggregate)
            ) {
                null
            } else {
                SettingsBackupState(
                    aggregate = aggregate,
                    settings = settings,
                    bindings = bindings,
                    iconShape = parseIconShape(root.optJSONObject("iconShape")),
                )
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun parseFavorites(favorites: JSONObject?): OrderedFavoriteAggregate? {
        if (favorites == null) return OrderedFavoriteAggregate()
        val modules = favorites.optJSONArray("modules") ?: return OrderedFavoriteAggregate()
        val parsed = buildList {
            repeat(modules.length()) { index ->
                val module = parseModule(modules.optJSONObject(index) ?: return null)
                    ?: return null
                add(module)
            }
        }
        return try {
            val homeBottomAppBar = parseHomeBottomAppBar(
                favorites.optJSONObject("homeBottomAppBar"),
            ) ?: return null
            OrderedFavoriteAggregate(
                modules = parsed,
                homeBottomAppBar = homeBottomAppBar,
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseModule(module: JSONObject): OrderedFavoriteModule? = try {
        OrderedFavoriteModule(
            id = module.getString("id"),
            type = module.enumField(
                name = "type",
                default = OrderedFavoriteModuleType.Vertical,
            ) { value -> orderedFavoriteModuleTypeFromStorageValue(value) } ?: return null,
            iconSize = module.enumField(
                name = "iconSize",
                default = module.enumField(
                    name = "applicationSize",
                    default = FavoriteListSize.Medium,
                ) { value -> favoriteListSizeFromStorageValue(value) } ?: return null,
            ) { value -> favoriteListSizeFromStorageValue(value) } ?: return null,
            textSize = module.enumField(
                name = "textSize",
                default = module.enumField(
                    name = "applicationSize",
                    default = FavoriteListSize.Medium,
                ) { value -> favoriteListSizeFromStorageValue(value) } ?: return null,
            ) { value -> favoriteListSizeFromStorageValue(value) } ?: return null,
            namePlacement = module.enumField(
                name = "namePlacement",
                default = FavoriteNamePlacement.Right,
            ) { value -> favoriteNamePlacementFromStorageValue(value) } ?: return null,
            itemsPerRow = module.optInt("itemsPerRow", 1),
            identities = parseIdentities(
                module.optJSONArray("identities") ?: return null,
            ),
        )
    } catch (_: Exception) {
        null
    }

    private fun parseHomeBottomAppBar(section: JSONObject?): HomeBottomAppBarState? {
        if (section == null) return HomeBottomAppBarState()
        return try {
            HomeBottomAppBarState(
                enabled = section.getBoolean("enabled"),
                presentation = section.enumField(
                    name = "presentation",
                    default = HomeBottomAppBarPresentation.SemiHidden,
                ) { value -> homeBottomAppBarPresentationFromStorageValue(value) }
                    ?: return null,
                iconSize = section.enumField(
                    name = "iconSize",
                    default = FavoriteListSize.Medium,
                ) { value -> favoriteListSizeFromStorageValue(value) } ?: return null,
                identities = parseIdentities(
                    section.optJSONArray("identities") ?: return null,
                ),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun List<LaunchableIdentity>.toJson(): JSONArray = JSONArray().apply {
        forEach { identity ->
            put(
                JSONObject()
                    .put("serial", identity.profileSerialNumber)
                    .put("component", identity.componentName.flattenToString()),
            )
        }
    }

    private fun parseIdentities(identities: JSONArray): List<LaunchableIdentity> = buildList {
        repeat(identities.length()) { index ->
            val identity = identities.optJSONObject(index) ?: throw IllegalArgumentException()
            val component = ComponentName.unflattenFromString(
                identity.getString("component"),
            ) ?: throw IllegalArgumentException()
            add(
                LaunchableIdentity(
                    profileSerialNumber = identity.getLong("serial"),
                    componentName = component,
                ),
            )
        }
    }

    private fun parseDisplaySettings(displaySettings: JSONObject?): DrawerDisplaySettings? {
        val defaults = DrawerDisplaySettings()
        if (displaySettings == null) return defaults
        return try {
            DrawerDisplaySettings(
                iconSize = displaySettings.enumField(
                    name = "iconSize",
                    default = displaySettings.enumField(
                        name = "applicationSize",
                        default = defaults.iconSize,
                    ) { value -> drawerApplicationSizeFromStorageValue(value) } ?: return null,
                ) { value -> drawerApplicationSizeFromStorageValue(value) } ?: return null,
                textSize = displaySettings.enumField(
                    name = "textSize",
                    default = displaySettings.enumField(
                        name = "applicationSize",
                        default = defaults.textSize,
                    ) { value -> drawerApplicationSizeFromStorageValue(value) } ?: return null,
                ) { value -> drawerApplicationSizeFromStorageValue(value) } ?: return null,
                namePlacement = displaySettings.enumField(
                    name = "namePlacement",
                    default = defaults.namePlacement,
                ) { value -> drawerNamePlacementFromStorageValue(value) } ?: return null,
                itemsPerRow = displaySettings.optInt(
                    "itemsPerRow",
                    defaults.itemsPerRow,
                ),
                sectionAnchorPresentation = displaySettings.enumField(
                    name = "sectionAnchorPresentation",
                    default = defaults.sectionAnchorPresentation,
                ) { value ->
                    drawerSectionAnchorPresentationFromStorageValue(value)
                } ?: return null,
                // Backups written before the opacity contract carried a transparent /
                // frosted-glass mode choice under "backgroundMode"; that choice is
                // intentionally not mapped and a missing opacity resolves to the
                // contracted default.
                backgroundOpacity = displaySettings.optInt(
                    "backgroundOpacity",
                    defaults.backgroundOpacity,
                ),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseIconShape(section: JSONObject?): IconShapeSettings {
        if (section == null) return IconShapeSettings()
        val mode = IconShapeClipMode.entries.firstOrNull {
            it.storageValue == section.opt("mode")
        } ?: IconShapeClipMode.Oem
        // Do not use optInt: it coerces strings and truncates fractional numbers.
        val rawClip = section.opt("freeformClip")
        val clip = when (rawClip) {
            is Int -> rawClip
            is Long -> rawClip.takeIf { it in 0L..100L }?.toInt()
            else -> null
        }?.takeIf { it in IconShapeSettings.FREEFORM_CLIP_RANGE }
            ?: IconShapeSettings.DEFAULT_FREEFORM_CLIP
        return IconShapeSettings(mode = mode, freeformClip = clip)
    }

    private fun parseBindings(bindings: JSONObject?): QuickActionBindings? {
        val defaults = QuickActionBindings()
        if (bindings == null) return defaults
        return try {
            QuickActionBindings(
                doubleTap = bindings.enumField(
                    name = "doubleTap",
                    default = defaults.doubleTap,
                ) { value -> quickActionFromStorageValueOrNull(value) } ?: return null,
                longPress = bindings.enumField(
                    name = "longPress",
                    default = defaults.longPress,
                ) { value -> quickActionFromStorageValueOrNull(value) } ?: return null,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads one enum-like field. A missing field keeps its default value; a present
     * but unrecognized value fails the whole backup interpretation.
     */
    private inline fun <T> JSONObject.enumField(
        name: String,
        default: T,
        fromStorageValue: (String) -> T?,
    ): T? = if (has(name)) fromStorageValue(getString(name)) else default

    private fun homeBottomAppBarPresentationFromStorageValue(
        value: String,
    ): HomeBottomAppBarPresentation? = when (value) {
        "fixed" -> HomeBottomAppBarPresentation.Fixed
        "semi_hidden" -> HomeBottomAppBarPresentation.SemiHidden
        else -> null
    }

    private val HomeBottomAppBarPresentation.storageValue: String
        get() = when (this) {
            HomeBottomAppBarPresentation.Fixed -> "fixed"
            HomeBottomAppBarPresentation.SemiHidden -> "semi_hidden"
        }

    private fun orderedFavoriteModuleTypeFromStorageValue(
        value: String,
    ): OrderedFavoriteModuleType? = when (value) {
        "vertical" -> OrderedFavoriteModuleType.Vertical
        "ribbon" -> OrderedFavoriteModuleType.Ribbon
        else -> null
    }

    private fun favoriteListSizeFromStorageValue(value: String): FavoriteListSize? =
        when (value) {
            "large" -> FavoriteListSize.Large
            "medium" -> FavoriteListSize.Medium
            "small" -> FavoriteListSize.Small
            else -> null
        }

    private fun favoriteNamePlacementFromStorageValue(value: String): FavoriteNamePlacement? =
        when (value) {
            "right" -> FavoriteNamePlacement.Right
            "below" -> FavoriteNamePlacement.Below
            "hidden" -> FavoriteNamePlacement.Hidden
            else -> null
        }

    private val DrawerApplicationSize.storageValue: String
        get() = when (this) {
            DrawerApplicationSize.Large -> "large"
            DrawerApplicationSize.Medium -> "medium"
            DrawerApplicationSize.Small -> "small"
        }

    private val FavoriteListSize.storageValue: String
        get() = when (this) {
            FavoriteListSize.Large -> "large"
            FavoriteListSize.Medium -> "medium"
            FavoriteListSize.Small -> "small"
        }

    private val FavoriteNamePlacement.storageValue: String
        get() = when (this) {
            FavoriteNamePlacement.Right -> "right"
            FavoriteNamePlacement.Below -> "below"
            FavoriteNamePlacement.Hidden -> "hidden"
        }

    private val OrderedFavoriteModuleType.storageValue: String
        get() = when (this) {
            OrderedFavoriteModuleType.Vertical -> "vertical"
            OrderedFavoriteModuleType.Ribbon -> "ribbon"
        }

    private val DrawerNamePlacement.storageValue: String
        get() = when (this) {
            DrawerNamePlacement.Right -> "right"
            DrawerNamePlacement.Below -> "below"
            DrawerNamePlacement.Hidden -> "hidden"
        }

    private val DrawerSectionAnchorPresentation.storageValue: String
        get() = when (this) {
            DrawerSectionAnchorPresentation.Inline -> "inline"
            DrawerSectionAnchorPresentation.LeftSide -> "left_side"
        }

    private fun drawerApplicationSizeFromStorageValue(
        value: String,
    ): DrawerApplicationSize? = when (value) {
        "large" -> DrawerApplicationSize.Large
        "medium" -> DrawerApplicationSize.Medium
        "small" -> DrawerApplicationSize.Small
        else -> null
    }

    private fun drawerNamePlacementFromStorageValue(value: String): DrawerNamePlacement? =
        when (value) {
            "right" -> DrawerNamePlacement.Right
            "below" -> DrawerNamePlacement.Below
            "hidden" -> DrawerNamePlacement.Hidden
            else -> null
        }

    private fun drawerSectionAnchorPresentationFromStorageValue(
        value: String,
    ): DrawerSectionAnchorPresentation? = when (value) {
        "inline" -> DrawerSectionAnchorPresentation.Inline
        "left_side" -> DrawerSectionAnchorPresentation.LeftSide
        else -> null
    }

    private fun quickActionFromStorageValueOrNull(value: String): QuickAction? = try {
        quickActionFromStorageValue(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Replaces the complete current state with a parsed backup as one coordinated
 * operation. Favorites, display settings, bindings, and icon shape are replaced in
 * that order. Later failures compensate earlier writes. This is not a cross-file
 * atomic transaction; compensation can itself fail.
 */
internal class SettingsBackupRestoreCoordinator(
    private val favorites: BackupFavoritesAccess,
    private val settings: BackupSettingsAccess,
    private val bindings: BackupBindingsAccess,
    private val iconShape: BackupIconShapeAccess,
) {
    suspend fun restore(backup: SettingsBackupState): Boolean {
        val previousFavorites = favorites.currentOrderedAggregate() ?: return false
        val previousSettings = settings.currentSettings() ?: return false
        val previousBindings = bindings.currentBindings() ?: return false
        val previousIconShape = iconShape.currentIconShapeSettings() ?: return false
        val favoritesChanged = backup.aggregate != previousFavorites
        val settingsChanged = backup.settings != previousSettings
        val bindingsChanged = backup.bindings != previousBindings
        val iconShapeChanged = backup.iconShape != previousIconShape

        if (favoritesChanged && !favorites.restoreAggregate(backup.aggregate)) {
            return false
        }
        if (settingsChanged && !settings.restoreSettings(backup.settings)) {
            if (favoritesChanged) rollbackFavorites(previousFavorites)
            return false
        }
        if (bindingsChanged && !bindings.restoreBindings(backup.bindings)) {
            if (settingsChanged) settings.restoreSettings(previousSettings)
            if (favoritesChanged) rollbackFavorites(previousFavorites)
            return false
        }
        if (iconShapeChanged) {
            val shapeRestored = try {
                iconShape.restoreIconShapeSettings(backup.iconShape)
            } catch (cancellation: CancellationException) {
                compensateIconShapeFailure(
                    previousIconShape, previousBindings, previousSettings, previousFavorites,
                    bindingsChanged, settingsChanged, favoritesChanged,
                )
                throw cancellation
            } catch (_: Exception) {
                false
            }
            if (!shapeRestored) {
                compensateIconShapeFailure(
                    previousIconShape, previousBindings, previousSettings, previousFavorites,
                    bindingsChanged, settingsChanged, favoritesChanged,
                )
                return false
            }
        }
        return true
    }

    private suspend fun compensateIconShapeFailure(
        previousIconShape: IconShapeSettings,
        previousBindings: QuickActionBindings,
        previousSettings: DrawerDisplaySettings,
        previousFavorites: OrderedFavoriteAggregate,
        bindingsChanged: Boolean,
        settingsChanged: Boolean,
        favoritesChanged: Boolean,
    ) = withContext(NonCancellable) {
        // Also compensate the last stage: cancellation may arrive after its disk write.
        retryCompensation { iconShape.restoreIconShapeSettings(previousIconShape) }
        if (bindingsChanged) {
            retryCompensation { bindings.restoreBindings(previousBindings) }
        }
        if (settingsChanged) {
            retryCompensation { settings.restoreSettings(previousSettings) }
        }
        if (favoritesChanged) {
            retryCompensation { favorites.restoreAggregate(previousFavorites) }
        }
    }

    private suspend fun retryCompensation(restore: suspend () -> Boolean) {
        repeat(2) {
            val succeeded = try {
                restore()
            } catch (_: Exception) {
                false
            }
            if (succeeded) return
        }
    }

    private suspend fun rollbackFavorites(previous: OrderedFavoriteAggregate) {
        if (!favorites.restoreAggregate(previous)) {
            // A transient rollback write failure is retried once; restore keeps
            // reporting failure either way so a mixed state is never presented as
            // a completed restore.
            favorites.restoreAggregate(previous)
        }
    }
}

/** UI-facing operations for the Settings Data backup and restore entries. */
internal interface SettingsBackupControl {
    fun suggestedFileName(): String

    suspend fun writeBackup(uri: Uri): Boolean

    /** Returns the parsed backup, or null when the file is invalid or unreadable. */
    suspend fun readBackup(uri: Uri): SettingsBackupState?

    suspend fun restore(backup: SettingsBackupState): Boolean
}

internal class SettingsBackupController(
    private val context: Context,
    private val favorites: BackupFavoritesAccess,
    private val settings: BackupSettingsAccess,
    private val bindings: BackupBindingsAccess,
    private val iconShape: BackupIconShapeAccess,
    private val homeSafeWidthDp: () -> Float?,
) : SettingsBackupControl {
    private val coordinator = SettingsBackupRestoreCoordinator(
        favorites = favorites,
        settings = settings,
        bindings = bindings,
        iconShape = iconShape,
    )

    override fun suggestedFileName(): String {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
        return backupFileName(
            versionName = versionName,
            timestamp = LocalDateTime.now(),
        )
    }

    override suspend fun writeBackup(uri: Uri): Boolean = withContext(context = Dispatchers.IO) {
        try {
            val aggregate = favorites.currentOrderedAggregate() ?: return@withContext false
            val currentSettings = settings.currentSettings() ?: return@withContext false
            val currentBindings = bindings.currentBindings() ?: return@withContext false
            val currentIconShape = iconShape.currentIconShapeSettings() ?: return@withContext false
            val document = SettingsBackupJson.serialize(
                aggregate = aggregate,
                settings = currentSettings,
                bindings = currentBindings,
                iconShape = currentIconShape,
            )
            context.contentResolver.openOutputStream(uri, WRITE_MODE)?.use { output ->
                output.write(document.toByteArray())
                output.flush()
            } ?: return@withContext false
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun readBackup(uri: Uri): SettingsBackupState? =
        withContext(context = Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext null
                SettingsBackupJson.parse(text = content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun restore(backup: SettingsBackupState): Boolean {
        val safeWidthDp = homeSafeWidthDp() ?: return false
        val resources = context.resources
        val aggregate = backup.aggregate.restoreHomeBottomAppBarForWidth(
            safeWidthDp = safeWidthDp,
            horizontalInsetDp = resources.getDimension(
                R.dimen.home_bottom_app_bar_preview_horizontal_inset,
            ) / resources.displayMetrics.density,
            iconGapDp = resources.getDimension(
                R.dimen.home_bottom_app_bar_preview_icon_gap,
            ) / resources.displayMetrics.density,
            iconSizeDp = { size ->
                resources.getDimension(size.iconSizeResource()) /
                    resources.displayMetrics.density
            },
        ) ?: return false
        return coordinator.restore(backup = backup.copy(aggregate = aggregate))
    }

    private companion object {
        const val WRITE_MODE = "wt"
    }
}
