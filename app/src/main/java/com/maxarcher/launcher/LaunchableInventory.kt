package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import java.util.Collections
import java.util.Locale
import androidx.core.content.getSystemService
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.maxarcher.launcher.platform.icons.LauncherIconAppearance
import com.maxarcher.launcher.platform.icons.LauncherIconRenderer
import com.maxarcher.launcher.platform.icons.LauncherIconShape
import com.maxarcher.launcher.platform.icons.SystemLauncherIconRenderer
import com.maxarcher.launcher.ui.drawer.DrawerSection
import com.maxarcher.launcher.ui.drawer.buildDrawerSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class LaunchableIdentity(
    val profileSerialNumber: Long,
    val componentName: ComponentName,
)

internal data class LaunchableEntry(
    val identity: LaunchableIdentity,
    val user: UserHandle,
    val label: String,
    val icon: Drawable,
    val iconBitmap: Bitmap? = null,
    val profileBadge: Drawable? = null,
    val sourceIcon: Drawable? = null,
    val iconAppearance: LauncherIconAppearance? = null,
)

internal enum class ProfileInventoryReadStatus {
    Complete,
    Unavailable,
}

internal data class LaunchableInventorySnapshot(
    val entries: List<LaunchableEntry>,
    val profileReadStatus: Map<Long, ProfileInventoryReadStatus>,
    val drawerSections: List<DrawerSection>? = null,
    val drawerSectionsLocale: Locale? = null,
)

internal fun LaunchableInventorySnapshot.drawerSectionsFor(locale: Locale): List<DrawerSection> =
    drawerSections?.takeIf { drawerSectionsLocale == locale }
        ?: buildDrawerSections(entries, locale)

internal fun interface LaunchableInventoryLoader {
    suspend fun load(): LaunchableInventorySnapshot

    fun currentIconAppearance(): LauncherIconAppearance? = null
}

internal sealed interface FavoriteAvailability {
    val presentationEntry: LaunchableEntry?

    data class Available(val entry: LaunchableEntry) : FavoriteAvailability {
        override val presentationEntry: LaunchableEntry = entry
    }

    data class Disabled(override val presentationEntry: LaunchableEntry?) : FavoriteAvailability
    data class TemporarilyUnavailable(
        override val presentationEntry: LaunchableEntry?,
    ) : FavoriteAvailability
    data class Unknown(override val presentationEntry: LaunchableEntry?) : FavoriteAvailability
    data object ConfirmedRemoved : FavoriteAvailability {
        override val presentationEntry: LaunchableEntry? = null
    }
}

internal interface LaunchableIdentityStatusResolver {
    suspend fun resolveMissingIdentity(
        identity: LaunchableIdentity,
        snapshot: LaunchableInventorySnapshot,
        lastKnownEntry: LaunchableEntry?,
    ): FavoriteAvailability

    fun markAvailable(identity: LaunchableIdentity) = Unit

    suspend fun refreshPresentation(entry: LaunchableEntry): LaunchableEntry = entry
}

internal fun interface LaunchableInventoryObservation {
    fun stop()
}

internal sealed interface LaunchableInventoryChange {
    data class PackageRemoved(
        val packageName: String,
        val profileSerialNumber: Long,
    ) : LaunchableInventoryChange

    data object PackageAdded : LaunchableInventoryChange
    data object PackageChanged : LaunchableInventoryChange
    data object PackagesAvailable : LaunchableInventoryChange
    data object PackagesUnavailable : LaunchableInventoryChange
    data object PackagesSuspended : LaunchableInventoryChange
    data object PackagesUnsuspended : LaunchableInventoryChange
}

internal fun interface LaunchableInventoryMonitor {
    fun observe(onInventoryChanged: (LaunchableInventoryChange) -> Unit):
        LaunchableInventoryObservation
}

internal sealed interface LaunchableInventoryState {
    data object Loading : LaunchableInventoryState
    data class Content(val snapshot: LaunchableInventorySnapshot) : LaunchableInventoryState {
        val entries: List<LaunchableEntry> get() = snapshot.entries
    }
    data class Error(val lastKnownSnapshot: LaunchableInventorySnapshot?) :
        LaunchableInventoryState {
        val lastKnownEntries: List<LaunchableEntry> get() = lastKnownSnapshot?.entries.orEmpty()
    }
}

internal class LaunchableInventoryCoordinator(
    private val loader: LaunchableInventoryLoader,
) {
    private val loadMutex = Mutex()
    private var pendingLoad = false
    private var pendingShowLoading = false
    private var pendingPreserveContentOnFailure = true
    private val mutableState = MutableStateFlow<LaunchableInventoryState>(
        LaunchableInventoryState.Loading,
    )
    private var lastSuccessfulSnapshot: LaunchableInventorySnapshot? = null
    private val lastTrustedEntries = mutableMapOf<LaunchableIdentity, LaunchableEntry>()

    val state: StateFlow<LaunchableInventoryState> = mutableState

    suspend fun load(
        showLoading: Boolean,
        preserveContentOnFailure: Boolean = false,
    ) {
        if (!loadMutex.tryLock()) {
            pendingPreserveContentOnFailure = if (pendingLoad) {
                pendingPreserveContentOnFailure && preserveContentOnFailure
            } else {
                preserveContentOnFailure
            }
            pendingLoad = true
            pendingShowLoading = pendingShowLoading || showLoading
            return
        }
        try {
            var shouldShowLoading = showLoading
            var shouldPreserveContentOnFailure = preserveContentOnFailure
            do {
                pendingLoad = false
                if (shouldShowLoading) mutableState.value = LaunchableInventoryState.Loading
                val appearance = loader.currentIconAppearance()
                mutableState.value = try {
                    val snapshot = loader.load()
                    if (appearance != loader.currentIconAppearance()) {
                        pendingLoad = true
                        mutableState.value
                    } else if (snapshot.entries.isEmpty()) {
                        val reliableSnapshot = lastSuccessfulSnapshot
                        if (shouldPreserveContentOnFailure && reliableSnapshot != null) {
                            LaunchableInventoryState.Content(reliableSnapshot)
                        } else {
                            LaunchableInventoryState.Error(reliableSnapshot)
                        }
                    } else {
                        lastSuccessfulSnapshot = snapshot
                        snapshot.entries.forEach { lastTrustedEntries[it.identity] = it }
                        LaunchableInventoryState.Content(snapshot)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    val reliableSnapshot = lastSuccessfulSnapshot
                    if (appearance != loader.currentIconAppearance()) {
                        pendingLoad = true
                        mutableState.value
                    } else if (shouldPreserveContentOnFailure && reliableSnapshot != null) {
                        LaunchableInventoryState.Content(snapshot = reliableSnapshot)
                    } else {
                        LaunchableInventoryState.Error(lastKnownSnapshot = reliableSnapshot)
                    }
                }
                shouldShowLoading = pendingShowLoading
                pendingShowLoading = false
                shouldPreserveContentOnFailure = pendingPreserveContentOnFailure
                pendingPreserveContentOnFailure = true
            } while (pendingLoad)
        } finally {
            loadMutex.unlock()
        }
    }

    fun observe(
        onInventoryChanged: (LaunchableInventoryChange) -> Unit,
    ): LaunchableInventoryObservation? =
        (loader as? LaunchableInventoryMonitor)?.observe(onInventoryChanged)

    suspend fun resolveFavorites(
        identities: List<LaunchableIdentity>,
        snapshot: LaunchableInventorySnapshot,
    ): Map<LaunchableIdentity, FavoriteAvailability> {
        val entriesByIdentity = snapshot.entries.associateBy(LaunchableEntry::identity)
        val resolver = loader as? LaunchableIdentityStatusResolver
        return identities.associateWith { identity ->
            entriesByIdentity[identity]?.let { entry ->
                resolver?.markAvailable(identity)
                FavoriteAvailability.Available(entry)
            }
                ?: resolver?.resolveMissingIdentity(
                    identity = identity,
                    snapshot = snapshot,
                    lastKnownEntry = lastTrustedEntries[identity]?.let {
                        resolver.refreshPresentation(it)
                    },
                )
                ?: FavoriteAvailability.Unknown(lastTrustedEntries[identity])
        }
    }
}

internal class AndroidLaunchableInventoryLoader(
    context: Context,
    private val iconRenderer: LauncherIconRenderer = SystemLauncherIconRenderer(context),
    private val iconAppearance: LauncherIconAppearance = LauncherIconAppearance(),
    private val iconAppearanceProvider: () -> LauncherIconAppearance = { iconAppearance },
) : LaunchableInventoryLoader, LaunchableInventoryMonitor, LaunchableIdentityStatusResolver {
    private val applicationContext = context.applicationContext
    private val launcherApps = checkNotNull(applicationContext.getSystemService<LauncherApps>())
    private val userManager = checkNotNull(applicationContext.getSystemService<UserManager>())
    private val pendingExactComponentDisappearance = Collections.synchronizedSet(
        mutableSetOf<LaunchableIdentity>(),
    )
    private val temporarilyUnavailablePackages = Collections.synchronizedSet(
        mutableSetOf<ProfilePackageKey>(),
    )
    private val renderedIconCache = LruCache<RenderedIconCacheKey, PreparedIcon>(
        MAXIMUM_CACHED_ICONS,
    )

    override fun currentIconAppearance(): LauncherIconAppearance = iconAppearanceProvider()

    override suspend fun load(): LaunchableInventorySnapshot = withContext(Dispatchers.IO) {
        val appearance = currentIconAppearance()
        val density = applicationContext.resources.displayMetrics.densityDpi
        val locale = applicationContext.resources.configuration.locales[0]
        val iconSizePixels = applicationContext.resources.getDimensionPixelSize(
            R.dimen.drawer_application_icon_size,
        )
        val currentUser = Process.myUserHandle()

        val profileStatuses = mutableMapOf<Long, ProfileInventoryReadStatus>()
        val unsortedEntries = launcherApps.profiles
            .flatMap { user ->
                val serialNumber = userManager.getSerialNumberForUser(user)
                val activities = try {
                    launcherApps.getActivityList(null, user).also {
                        profileStatuses[serialNumber] = ProfileInventoryReadStatus.Complete
                    }
                } catch (failure: IllegalStateException) {
                    if (user == currentUser) throw failure
                    profileStatuses[serialNumber] = ProfileInventoryReadStatus.Unavailable
                    emptyList()
                } catch (failure: SecurityException) {
                    if (user == currentUser) throw failure
                    profileStatuses[serialNumber] = ProfileInventoryReadStatus.Unavailable
                    emptyList()
                }

                activities.map { activity ->
                    val identity = LaunchableIdentity(
                        profileSerialNumber = serialNumber,
                        componentName = activity.componentName,
                    )
                    val preparedIcon = preparedIconFor(
                        activity = activity,
                        user = user,
                        identity = identity,
                        density = density,
                        iconSizePixels = iconSizePixels,
                        appearance = appearance,
                    )

                    LaunchableEntry(
                        identity = identity,
                        user = user,
                        label = activity.label.toString(),
                        icon = preparedIcon.drawable,
                        iconBitmap = preparedIcon.bitmap,
                        sourceIcon = preparedIcon.source,
                        iconAppearance = appearance,
                        profileBadge = if (user == currentUser) {
                            null
                        } else {
                            runCatching {
                                val size = (12 * applicationContext.resources.displayMetrics.density)
                                    .toInt()
                                    .coerceAtLeast(1)
                                val transparent = createBitmap(size, size).apply {
                                    eraseColor(Color.TRANSPARENT)
                                }.toDrawable(applicationContext.resources)
                                applicationContext.packageManager.getUserBadgedDrawableForDensity(
                                    transparent,
                                    user,
                                    Rect(0, 0, size, size),
                                    density,
                                )
                            }.getOrNull()
                        },
                    )
                }
            }
            .distinctBy(LaunchableEntry::identity)
        val drawerSections = buildDrawerSections(unsortedEntries, locale)
        val entries = drawerSections.flatMap(DrawerSection::entries)
        LaunchableInventorySnapshot(
            entries = entries,
            profileReadStatus = profileStatuses,
            drawerSections = drawerSections,
            drawerSectionsLocale = locale,
        )
    }

    override suspend fun resolveMissingIdentity(
        identity: LaunchableIdentity,
        snapshot: LaunchableInventorySnapshot,
        lastKnownEntry: LaunchableEntry?,
    ): FavoriteAvailability =
        withContext(Dispatchers.IO) {
            when (snapshot.profileReadStatus[identity.profileSerialNumber]) {
                ProfileInventoryReadStatus.Unavailable -> {
                    return@withContext FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
                }
                null -> {
                    val profileStillExists = userManager.userProfiles.any {
                        userManager.getSerialNumberForUser(it) == identity.profileSerialNumber
                    }
                    return@withContext if (profileStillExists) {
                        FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
                    } else {
                        FavoriteAvailability.ConfirmedRemoved
                    }
                }
                ProfileInventoryReadStatus.Complete -> Unit
            }
            val user = launcherApps.profiles.firstOrNull {
                userManager.getSerialNumberForUser(it) == identity.profileSerialNumber
            } ?: return@withContext FavoriteAvailability.Unknown(lastKnownEntry)
            val profilePackageKey = ProfilePackageKey(
                identity.profileSerialNumber,
                identity.componentName.packageName,
            )
            if (profilePackageKey in temporarilyUnavailablePackages) {
                return@withContext FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
            }
            val applicationInfo = try {
                launcherApps.getApplicationInfo(
                    identity.componentName.packageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS,
                    user,
                )
            } catch (_: PackageManager.NameNotFoundException) {
                return@withContext FavoriteAvailability.ConfirmedRemoved
            } catch (_: SecurityException) {
                return@withContext FavoriteAvailability.Unknown(lastKnownEntry)
            } catch (_: IllegalStateException) {
                return@withContext FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
            }
            try {
                val disabledPresentation = lastKnownEntry ?: if (user == Process.myUserHandle()) {
                    val density = applicationContext.resources.displayMetrics.densityDpi
                    val sourceIcon = runCatching {
                        applicationInfo.loadIcon(applicationContext.packageManager)
                    }.getOrDefault(applicationContext.packageManager.defaultActivityIcon)
                    val appearance = currentIconAppearance()
                    val renderedIcon = iconRenderer.render(sourceIcon, user, appearance)
                    LaunchableEntry(
                        identity = identity,
                        user = user,
                        label = applicationInfo.loadLabel(applicationContext.packageManager)
                            .toString(),
                        icon = renderedIcon,
                        sourceIcon = sourceIcon,
                        iconAppearance = appearance,
                        iconBitmap = runCatching {
                            val iconSizePixels = applicationContext.resources
                                .getDimensionPixelSize(R.dimen.drawer_application_icon_size)
                            renderedIcon.toBitmap(iconSizePixels, iconSizePixels)
                        }.getOrNull(),
                    )
                } else {
                    null
                }
                if (!applicationInfo.enabled) {
                    pendingExactComponentDisappearance.remove(identity)
                    return@withContext FavoriteAvailability.Disabled(disabledPresentation)
                }
                if (user != Process.myUserHandle()) {
                    return@withContext FavoriteAvailability.Unknown(lastKnownEntry)
                }
                @Suppress("DEPRECATION")
                val activityInfo = applicationContext.packageManager.getActivityInfo(
                    identity.componentName,
                    PackageManager.MATCH_DISABLED_COMPONENTS,
                )
                if (activityInfo.enabled) {
                    if (pendingExactComponentDisappearance.add(identity)) {
                        FavoriteAvailability.Unknown(lastKnownEntry)
                    } else {
                        FavoriteAvailability.ConfirmedRemoved
                    }
                } else {
                    pendingExactComponentDisappearance.remove(identity)
                    FavoriteAvailability.Disabled(disabledPresentation)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                if (pendingExactComponentDisappearance.add(identity)) {
                    FavoriteAvailability.Unknown(lastKnownEntry)
                } else {
                    FavoriteAvailability.ConfirmedRemoved
                }
            } catch (_: SecurityException) {
                FavoriteAvailability.Unknown(lastKnownEntry)
            } catch (_: IllegalStateException) {
                FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
            }
        }

    override fun markAvailable(identity: LaunchableIdentity) {
        pendingExactComponentDisappearance.remove(identity)
    }

    override suspend fun refreshPresentation(entry: LaunchableEntry): LaunchableEntry =
        withContext(Dispatchers.IO) {
            val appearance = currentIconAppearance()
            val source = entry.sourceIcon ?: return@withContext entry
            if (entry.iconAppearance == appearance) return@withContext entry
            val rendered = runCatching {
                iconRenderer.render(source, entry.user, appearance)
            }.getOrElse { return@withContext entry }
            val size = applicationContext.resources.getDimensionPixelSize(
                R.dimen.drawer_application_icon_size,
            )
            entry.copy(
                icon = rendered,
                iconBitmap = runCatching { rendered.toBitmap(size, size) }.getOrNull(),
                iconAppearance = appearance,
            )
        }

    private fun preparedIconFor(
        activity: LauncherActivityInfo,
        user: UserHandle,
        identity: LaunchableIdentity,
        density: Int,
        iconSizePixels: Int,
        appearance: LauncherIconAppearance,
    ): PreparedIcon {
        val cacheKey = RenderedIconCacheKey(
            identity = identity,
            density = density,
            uiMode = applicationContext.resources.configuration.uiMode,
            shape = appearance.shape,
        )
        renderedIconCache.get(cacheKey)?.let { return it }

        val fallbackIcon = applicationContext.packageManager.defaultActivityIcon
        var sourceIcon = runCatching {
            activity.getIcon(density)
        }.getOrDefault(fallbackIcon)
        val renderedIcon = runCatching {
            iconRenderer.render(sourceIcon, user, appearance)
        }.getOrElse {
            sourceIcon = fallbackIcon
            iconRenderer.render(fallbackIcon, user, appearance)
        }
        return PreparedIcon(
            drawable = renderedIcon,
            source = sourceIcon,
            bitmap = runCatching {
                renderedIcon.toBitmap(iconSizePixels, iconSizePixels)
            }.getOrNull(),
        ).also { preparedIcon ->
            renderedIconCache.put(cacheKey, preparedIcon)
        }
    }

    private fun invalidateCachedIcons(packageName: String, profileSerialNumber: Long) {
        renderedIconCache.snapshot().keys
            .filter { key ->
                key.identity.profileSerialNumber == profileSerialNumber &&
                    key.identity.componentName.packageName == packageName
            }
            .forEach(renderedIconCache::remove)
    }

    override fun observe(
        onInventoryChanged: (LaunchableInventoryChange) -> Unit,
    ): LaunchableInventoryObservation {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                val serialNumber = userManager.getSerialNumberForUser(user)
                invalidateCachedIcons(packageName, serialNumber)
                temporarilyUnavailablePackages.remove(
                    ProfilePackageKey(serialNumber, packageName),
                )
                onInventoryChanged(LaunchableInventoryChange.PackageAdded)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                val serialNumber = userManager.getSerialNumberForUser(user)
                invalidateCachedIcons(packageName, serialNumber)
                temporarilyUnavailablePackages.remove(
                    ProfilePackageKey(serialNumber, packageName),
                )
                onInventoryChanged(LaunchableInventoryChange.PackageChanged)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                val serialNumber = userManager.getSerialNumberForUser(user)
                invalidateCachedIcons(packageName, serialNumber)
                temporarilyUnavailablePackages.remove(
                    ProfilePackageKey(serialNumber, packageName),
                )
                onInventoryChanged(
                    LaunchableInventoryChange.PackageRemoved(
                        packageName = packageName,
                        profileSerialNumber = serialNumber,
                    ),
                )
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                val serial = userManager.getSerialNumberForUser(user)
                packageNames.forEach { packageName ->
                    temporarilyUnavailablePackages.remove(ProfilePackageKey(serial, packageName))
                }
                onInventoryChanged(LaunchableInventoryChange.PackagesAvailable)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                val serial = userManager.getSerialNumberForUser(user)
                packageNames.forEach { packageName ->
                    temporarilyUnavailablePackages.add(ProfilePackageKey(serial, packageName))
                }
                onInventoryChanged(LaunchableInventoryChange.PackagesUnavailable)
            }

            override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
                val serial = userManager.getSerialNumberForUser(user)
                packageNames.forEach { packageName ->
                    temporarilyUnavailablePackages.add(ProfilePackageKey(serial, packageName))
                }
                onInventoryChanged(LaunchableInventoryChange.PackagesSuspended)
            }

            override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
                val serial = userManager.getSerialNumberForUser(user)
                packageNames.forEach { packageName ->
                    temporarilyUnavailablePackages.remove(ProfilePackageKey(serial, packageName))
                }
                onInventoryChanged(LaunchableInventoryChange.PackagesUnsuspended)
            }
        }

        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return LaunchableInventoryObservation {
            launcherApps.unregisterCallback(callback)
        }
    }

    private data class PreparedIcon(
        val drawable: Drawable,
        val source: Drawable,
        val bitmap: Bitmap?,
    )

    private data class RenderedIconCacheKey(
        val identity: LaunchableIdentity,
        val density: Int,
        val uiMode: Int,
        val shape: LauncherIconShape,
    )

    private companion object {
        const val MAXIMUM_CACHED_ICONS = 256
    }
}

private data class ProfilePackageKey(
    val profileSerialNumber: Long,
    val packageName: String,
)
