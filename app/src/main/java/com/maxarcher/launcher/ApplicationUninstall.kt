package com.maxarcher.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process

internal interface ApplicationUninstallLauncher {
    fun isAvailable(entry: LaunchableEntry): Boolean

    fun open(entry: LaunchableEntry): Boolean
}

internal object EmptyApplicationUninstallLauncher : ApplicationUninstallLauncher {
    override fun isAvailable(entry: LaunchableEntry): Boolean = false

    override fun open(entry: LaunchableEntry): Boolean = false
}

internal class AndroidApplicationUninstallLauncher(context: Context) :
    ApplicationUninstallLauncher {
    private val launchContext = context
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    override fun isAvailable(entry: LaunchableEntry): Boolean {
        if (entry.user != Process.myUserHandle()) return false
        val applicationInfo = getApplicationInfo(
            packageName = entry.identity.componentName.packageName,
        ) ?: return false
        val uninstallIntent = createApplicationUninstallIntent(
            packageName = entry.identity.componentName.packageName,
        )
        val hasSystemHandler = packageManager.resolveActivity(
            uninstallIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null
        return isOrdinaryUninstallAvailable(
            isCurrentUser = true,
            applicationFlags = applicationInfo.flags,
            hasSystemHandler = hasSystemHandler,
        )
    }

    override fun open(entry: LaunchableEntry): Boolean {
        if (!isAvailable(entry = entry)) return false
        return try {
            val intent = createApplicationUninstallIntent(
                packageName = entry.identity.componentName.packageName,
            )
            if (launchContext !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchContext.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getApplicationInfo(packageName: String): ApplicationInfo? = try {
        packageManager.getApplicationInfo(packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

internal fun isOrdinaryUninstallAvailable(
    isCurrentUser: Boolean,
    applicationFlags: Int,
    hasSystemHandler: Boolean,
): Boolean {
    val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
    return isCurrentUser && hasSystemHandler && applicationFlags and systemFlags == 0
}

@Suppress("DEPRECATION")
internal fun createApplicationUninstallIntent(packageName: String): Intent = Intent(
    Intent.ACTION_UNINSTALL_PACKAGE,
    Uri.fromParts("package", packageName, null),
)
