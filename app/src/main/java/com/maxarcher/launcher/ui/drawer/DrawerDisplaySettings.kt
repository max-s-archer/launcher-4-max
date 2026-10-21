package com.maxarcher.launcher.ui.drawer

import android.content.Context
import android.util.AtomicFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.maxarcher.launcher.BackupSettingsAccess
import com.maxarcher.launcher.R

internal enum class DrawerApplicationSize {
    Large,
    Medium,
    Small,
}

internal enum class DrawerNamePlacement {
    Right,
    Below,
    Hidden,
}

internal enum class DrawerSectionAnchorPresentation {
    Inline,
    LeftSide,
}

internal data class DrawerDisplaySettings(
    val iconSize: DrawerApplicationSize = DrawerApplicationSize.Medium,
    val textSize: DrawerApplicationSize = DrawerApplicationSize.Medium,
    val namePlacement: DrawerNamePlacement = DrawerNamePlacement.Right,
    val itemsPerRow: Int = 1,
    val sectionAnchorPresentation: DrawerSectionAnchorPresentation =
        DrawerSectionAnchorPresentation.Inline,
    val backgroundOpacity: Int = DEFAULT_BACKGROUND_OPACITY,
) {
    init {
        require(itemsPerRow in validItemsPerRowRange(namePlacement = namePlacement)) {
            "Invalid Drawer items-per-row count for the selected name placement"
        }
        require(backgroundOpacity in BACKGROUND_OPACITY_RANGE) {
            "Invalid Drawer background opacity"
        }
    }

    companion object {
        const val DEFAULT_BACKGROUND_OPACITY = 50
        val BACKGROUND_OPACITY_RANGE = 0..100
    }
}

internal fun validItemsPerRowRange(
    namePlacement: DrawerNamePlacement,
): IntRange = when (namePlacement) {
    DrawerNamePlacement.Right -> 1..2
    DrawerNamePlacement.Below -> 1..4
    DrawerNamePlacement.Hidden -> 1..6
}

internal fun DrawerApplicationSize.iconSizeResource(): Int = when (this) {
    DrawerApplicationSize.Large -> R.dimen.home_favorite_large_icon_size
    DrawerApplicationSize.Medium -> R.dimen.home_favorite_icon_size
    DrawerApplicationSize.Small -> R.dimen.home_companion_favorite_icon_size
}

internal fun DrawerApplicationSize.rowHeightResource(): Int = when (this) {
    DrawerApplicationSize.Large -> R.dimen.home_favorite_large_row_min_height
    DrawerApplicationSize.Medium -> R.dimen.home_favorite_row_min_height
    DrawerApplicationSize.Small -> R.dimen.home_companion_favorite_row_min_height
}

internal fun DrawerApplicationSize.belowRowHeightResource(): Int = when (this) {
    DrawerApplicationSize.Large -> R.dimen.drawer_application_below_large_row_height
    DrawerApplicationSize.Medium -> R.dimen.drawer_application_below_medium_row_height
    DrawerApplicationSize.Small -> R.dimen.drawer_application_below_small_row_height
}

internal fun DrawerApplicationSize.textSizeResource(): Int = when (this) {
    DrawerApplicationSize.Large -> R.dimen.home_favorite_large_text_size
    DrawerApplicationSize.Medium -> R.dimen.home_favorite_text_size
    DrawerApplicationSize.Small -> R.dimen.home_companion_favorite_text_size
}

internal fun DrawerApplicationSize.lineHeightResource(): Int = when (this) {
    DrawerApplicationSize.Large -> R.dimen.home_favorite_large_line_height
    DrawerApplicationSize.Medium -> R.dimen.home_favorite_line_height
    DrawerApplicationSize.Small -> R.dimen.home_companion_favorite_line_height
}

internal sealed interface DrawerDisplaySettingsReadState {
    data object Loading : DrawerDisplaySettingsReadState

    data class Readable(
        val settings: DrawerDisplaySettings,
    ) : DrawerDisplaySettingsReadState

    data object ReadFailure : DrawerDisplaySettingsReadState
}

internal class DrawerDisplaySettingsStore internal constructor(
    private val atomicFile: AtomicFile,
) : BackupSettingsAccess {
    constructor(context: Context) : this(
        atomicFile = AtomicFile(context.filesDir.resolve(FILE_NAME)),
    )

    internal constructor(file: File) : this(
        atomicFile = AtomicFile(file),
    )

    // Activity recreation can create a new store while the old owner's IO finishes.
    // AtomicFile itself does not serialize readers/writers across store instances.
    private val mutationMutex = fileMutexes.getOrPut(atomicFile.baseFile.absoluteFile.normalize().path) {
        Mutex()
    }
    private val mutableState = MutableStateFlow<DrawerDisplaySettingsReadState>(
        DrawerDisplaySettingsReadState.Loading,
    )

    val state: StateFlow<DrawerDisplaySettingsReadState> = mutableState

    suspend fun load(): Unit = mutationMutex.withLock {
        mutableState.value = DrawerDisplaySettingsReadState.Loading
        mutableState.value = withContext(context = Dispatchers.IO) {
            if (!atomicFile.hasReadableSource()) {
                DrawerDisplaySettingsReadState.Readable(
                    settings = DrawerDisplaySettings(),
                )
            } else {
                try {
                    val document = readDocument()
                    if (document.fields.keys.containsAll(KNOWN_FIELDS).not()) {
                        try {
                            writeDocument(settings = document.settings)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            // The readable adopted state remains usable. Its source is preserved,
                            // and a later successful user change writes the complete current form.
                        }
                    }
                    DrawerDisplaySettingsReadState.Readable(
                        settings = document.settings,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    DrawerDisplaySettingsReadState.ReadFailure
                }
            }
        }
    }

    /**
     * One synchronous read for [AppGraph] initialization, before the first frame. The
     * graph instance is created once per process and published only afterwards, so this
     * direct call cannot race another accessor.
     */
    fun loadBlocking(): Unit = runBlocking { load() }

    override fun currentSettings(): DrawerDisplaySettings? =
        (mutableState.value as? DrawerDisplaySettingsReadState.Readable)?.settings

    override suspend fun restoreSettings(settings: DrawerDisplaySettings): Boolean =
        replace(settings = settings)

    suspend fun replace(settings: DrawerDisplaySettings): Boolean = mutationMutex.withLock {
        val current = (mutableState.value as? DrawerDisplaySettingsReadState.Readable)
            ?.settings
            ?: return false
        if (current == settings) return true

        val succeeded = try {
            withContext(context = Dispatchers.IO) {
                writeDocument(settings = settings)
                // Publish before returning across the cancellable dispatcher boundary.
                // A committed file must agree with this store even if its caller is destroyed.
                mutableState.value = DrawerDisplaySettingsReadState.Readable(settings = settings)
                true
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        succeeded
    }

    private data class DrawerDisplaySettingsDocument(
        val settings: DrawerDisplaySettings,
        val fields: Map<String, String>,
    )

    private fun readDocument(): DrawerDisplaySettingsDocument =
        DataInputStream(BufferedInputStream(atomicFile.openRead())).use { input ->
            require(input.readInt() == MAGIC) { "Unrecognized Drawer display-settings document" }
            val schemaVersion = input.readInt()
            require(schemaVersion in MIN_READABLE_SCHEMA_VERSION..SCHEMA_VERSION) {
                "Unsupported Drawer display-settings schema"
            }
            val fieldCount = input.readInt()
            require(fieldCount in 0..MAX_FIELD_COUNT) {
                "Invalid Drawer display-settings field count"
            }
            val fields = buildMap {
                repeat(times = fieldCount) {
                    val key = input.readUTF()
                    val value = input.readUTF()
                    require(put(key, value) == null) {
                        "Duplicate Drawer display-settings field"
                    }
                }
            }
            require(input.read() == -1) { "Unexpected Drawer display-settings data" }

            val defaults = DrawerDisplaySettings()
            val namePlacement = fields[FIELD_NAME_PLACEMENT]?.let(
                ::drawerNamePlacementFromStorageValue,
            ) ?: defaults.namePlacement
            val settings = DrawerDisplaySettings(
                iconSize = fields[FIELD_ICON_SIZE]?.let(::drawerApplicationSizeFromStorageValue)
                    ?: fields[FIELD_APPLICATION_SIZE]?.let(::drawerApplicationSizeFromStorageValue)
                    ?: defaults.iconSize,
                textSize = fields[FIELD_TEXT_SIZE]?.let(::drawerApplicationSizeFromStorageValue)
                    ?: fields[FIELD_APPLICATION_SIZE]?.let(::drawerApplicationSizeFromStorageValue)
                    ?: defaults.textSize,
                namePlacement = namePlacement,
                itemsPerRow = fields[FIELD_ITEMS_PER_ROW]?.let { value ->
                    requireNotNull(value.toIntOrNull()) {
                        "Invalid Drawer items-per-row count"
                    }
                } ?: defaults.itemsPerRow,
                sectionAnchorPresentation = fields[FIELD_SECTION_ANCHOR]?.let(
                    ::drawerSectionAnchorPresentationFromStorageValue,
                ) ?: defaults.sectionAnchorPresentation,
                // Version 1 stored a transparent/frosted-glass mode choice under a
                // different field name; that choice is intentionally not mapped and a
                // missing opacity resolves to the contracted default.
                backgroundOpacity = fields[FIELD_BACKGROUND_OPACITY]?.let { value ->
                    requireNotNull(value.toIntOrNull()) {
                        "Invalid Drawer background opacity"
                    }
                } ?: defaults.backgroundOpacity,
            )
            DrawerDisplaySettingsDocument(
                settings = settings,
                fields = fields,
            )
    }

    private fun writeDocument(settings: DrawerDisplaySettings) {
        var outputStream: FileOutputStream? = atomicFile.startWrite()
        try {
            val output = DataOutputStream(BufferedOutputStream(checkNotNull(outputStream)))
            output.writeInt(MAGIC)
            output.writeInt(SCHEMA_VERSION)
            output.writeInt(KNOWN_FIELDS.size)
            output.writeField(key = FIELD_ICON_SIZE, value = settings.iconSize.storageValue)
            output.writeField(key = FIELD_TEXT_SIZE, value = settings.textSize.storageValue)
            output.writeField(
                key = FIELD_NAME_PLACEMENT,
                value = settings.namePlacement.storageValue,
            )
            output.writeField(
                key = FIELD_ITEMS_PER_ROW,
                value = settings.itemsPerRow.toString(),
            )
            output.writeField(
                key = FIELD_SECTION_ANCHOR,
                value = settings.sectionAnchorPresentation.storageValue,
            )
            output.writeField(
                key = FIELD_BACKGROUND_OPACITY,
                value = settings.backgroundOpacity.toString(),
            )
            output.flush()
            atomicFile.finishWrite(outputStream)
            outputStream = null
        } catch (exception: Exception) {
            outputStream?.let(atomicFile::failWrite)
            throw exception
        }
    }

    private fun DataOutputStream.writeField(key: String, value: String) {
        writeUTF(key)
        writeUTF(value)
    }

    private fun AtomicFile.hasReadableSource(): Boolean =
        baseFile.exists() || File(baseFile.path + BACKUP_SUFFIX).exists()

    private companion object {
        val fileMutexes = ConcurrentHashMap<String, Mutex>()
        const val FILE_NAME = "drawer-display-settings.bin"
        const val BACKUP_SUFFIX = ".bak"
        const val MAGIC = 0x44525331
        const val MIN_READABLE_SCHEMA_VERSION = 1
        const val SCHEMA_VERSION = 3
        const val MAX_FIELD_COUNT = 64

        const val FIELD_APPLICATION_SIZE = "application_size"
        const val FIELD_ICON_SIZE = "icon_size"
        const val FIELD_TEXT_SIZE = "text_size"
        const val FIELD_NAME_PLACEMENT = "name_placement"
        const val FIELD_ITEMS_PER_ROW = "items_per_row"
        const val FIELD_SECTION_ANCHOR = "section_anchor"
        const val FIELD_BACKGROUND_OPACITY = "background_opacity"

        val KNOWN_FIELDS = setOf(
            FIELD_ICON_SIZE,
            FIELD_TEXT_SIZE,
            FIELD_NAME_PLACEMENT,
            FIELD_ITEMS_PER_ROW,
            FIELD_SECTION_ANCHOR,
            FIELD_BACKGROUND_OPACITY,
        )
    }
}

private val DrawerApplicationSize.storageValue: String
    get() = when (this) {
        DrawerApplicationSize.Large -> "large"
        DrawerApplicationSize.Medium -> "medium"
        DrawerApplicationSize.Small -> "small"
    }

private fun drawerApplicationSizeFromStorageValue(
    value: String,
): DrawerApplicationSize = when (value) {
    "large" -> DrawerApplicationSize.Large
    "medium" -> DrawerApplicationSize.Medium
    "small" -> DrawerApplicationSize.Small
    else -> throw IllegalArgumentException("Invalid Drawer application size")
}

private val DrawerNamePlacement.storageValue: String
    get() = when (this) {
        DrawerNamePlacement.Right -> "right"
        DrawerNamePlacement.Below -> "below"
        DrawerNamePlacement.Hidden -> "hidden"
    }

private fun drawerNamePlacementFromStorageValue(
    value: String,
): DrawerNamePlacement = when (value) {
    "right" -> DrawerNamePlacement.Right
    "below" -> DrawerNamePlacement.Below
    "hidden" -> DrawerNamePlacement.Hidden
    else -> throw IllegalArgumentException("Invalid Drawer name placement")
}

private val DrawerSectionAnchorPresentation.storageValue: String
    get() = when (this) {
        DrawerSectionAnchorPresentation.Inline -> "inline"
        DrawerSectionAnchorPresentation.LeftSide -> "left_side"
    }

private fun drawerSectionAnchorPresentationFromStorageValue(
    value: String,
): DrawerSectionAnchorPresentation = when (value) {
    "inline" -> DrawerSectionAnchorPresentation.Inline
    "left_side" -> DrawerSectionAnchorPresentation.LeftSide
    else -> throw IllegalArgumentException("Invalid Drawer section-anchor presentation")
}
