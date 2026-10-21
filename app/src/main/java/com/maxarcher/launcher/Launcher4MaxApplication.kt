package com.maxarcher.launcher

import android.app.Application
import android.content.Context
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsStore
import com.maxarcher.launcher.ui.settings.AndroidSettingsPlatform
import com.maxarcher.launcher.ui.settings.readLicense
import com.maxarcher.launcher.ui.settings.readThirdPartyNotices

/**
 * Process-wide owner of the stores and platform adapters [Launcher4MaxApp] consumes. Holding
 * them outside the composition keeps their in-memory state readable across Activity
 * recreation, so returning to Home never re-reads the durable files or re-queries the
 * inventory while the process is alive.
 */
class Launcher4MaxApplication : Application() {
    internal val appGraph: AppGraph by lazy { AppGraph(context = this) }
}

internal class AppGraph(context: Context) {
    val entryLauncher: AndroidLaunchableEntryLauncher =
        AndroidLaunchableEntryLauncher(context = context)
    val favoriteStore: OrderedFavoriteStoreAdapter =
        OrderedFavoriteStoreAdapter(context = context)
    val drawerDisplaySettingsStore: DrawerDisplaySettingsStore =
        DrawerDisplaySettingsStore(context = context)
    val quickActionBindingsStore: QuickActionBindingsStore =
        QuickActionBindingsStore(context = context)
    val iconShapeSettingsStore: IconShapeSettingsStore =
        IconShapeSettingsStore(context = context)
    val inventoryLoader: AndroidLaunchableInventoryLoader =
        AndroidLaunchableInventoryLoader(
            context = context,
            iconAppearanceProvider = {
                iconShapeSettingsStore.lastSuccessfulSettings.iconAppearance()
            },
        )
    val defaultLauncherPromptStore: DefaultLauncherPromptStore =
        DefaultLauncherPromptStore(context = context)
    val informationLauncher: AndroidApplicationInformationLauncher =
        AndroidApplicationInformationLauncher(context = context)
    val uninstallLauncher: AndroidApplicationUninstallLauncher =
        AndroidApplicationUninstallLauncher(context = context)
    val shortcutController: AndroidApplicationShortcutController =
        AndroidApplicationShortcutController(context = context)
    val settingsPlatform: AndroidSettingsPlatform =
        AndroidSettingsPlatform(context = context)
    val licenseText: String = readLicense(context = context)
    val thirdPartyNoticesText: String = readThirdPartyNotices(context = context)
    val accessibilityLockController: AccessibilityLockController =
        AndroidAccessibilityLockController(
            context = context,
            serviceComponent = accessibilityLockServiceComponent(context = context),
        )

    init {
        // One synchronous read per process puts the durable favorites and display settings
        // in front of the first frame; later reloads stay async and mutex-guarded.
        favoriteStore.loadBlocking()
        drawerDisplaySettingsStore.loadBlocking()
        quickActionBindingsStore.loadBlocking()
        iconShapeSettingsStore.loadBlocking()
        defaultLauncherPromptStore.loadBlocking()
    }
}
