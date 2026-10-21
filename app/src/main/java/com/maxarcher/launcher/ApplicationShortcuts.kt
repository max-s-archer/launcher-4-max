package com.maxarcher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.core.content.getSystemService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ApplicationShortcut(
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val icon: Drawable?,
    val user: UserHandle,
    val rank: Int,
)

internal interface ApplicationShortcutController {
    suspend fun load(entry: LaunchableEntry): List<ApplicationShortcut>
    fun launch(shortcut: ApplicationShortcut): Boolean
}

internal object EmptyApplicationShortcutController : ApplicationShortcutController {
    override suspend fun load(entry: LaunchableEntry) = emptyList<ApplicationShortcut>()
    override fun launch(shortcut: ApplicationShortcut) = false
}


internal class AndroidApplicationShortcutController(context: Context) :
    ApplicationShortcutController {
    private val applicationContext = context.applicationContext
    private val launcherApps =
        checkNotNull(applicationContext.getSystemService<LauncherApps>())

    override suspend fun load(entry: LaunchableEntry): List<ApplicationShortcut> =
        withContext(Dispatchers.IO) {
            try {
                if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()
                val component = entry.identity.componentName
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(component.packageName)
                    .setActivity(component)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
                    )
                launcherApps.getShortcuts(query, entry.user)
                    .orEmpty()
                    .asSequence()
                    .filter { shortcut ->
                        shortcut.isEnabled &&
                            shortcut.`package` == component.packageName &&
                            shortcut.activity == component &&
                            shortcut.userHandle == entry.user
                    }
                    .mapNotNull { shortcut ->
                        val label = shortcut.shortLabel?.toString()?.takeIf(String::isNotBlank)
                            ?: return@mapNotNull null
                        val icon = try {
                            launcherApps.getShortcutIconDrawable(
                                shortcut,
                                applicationContext.resources.displayMetrics.densityDpi,
                            )
                        } catch (_: IllegalStateException) {
                            null
                        } catch (_: SecurityException) {
                            null
                        }
                        ApplicationShortcut(
                            packageName = shortcut.`package`,
                            shortcutId = shortcut.id,
                            label = label,
                            icon = icon,
                            user = shortcut.userHandle,
                            rank = shortcut.rank,
                        )
                    }
                    .toList()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalStateException) {
                emptyList()
            } catch (_: SecurityException) {
                emptyList()
            }
        }

    override fun launch(shortcut: ApplicationShortcut): Boolean = try {
        launcherApps.startShortcut(
            shortcut.packageName,
            shortcut.shortcutId,
            null,
            null,
            shortcut.user,
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalStateException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
