package com.maxarcher.launcher

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.pm.LauncherApps
import android.os.SystemClock
import androidx.core.content.getSystemService

internal fun interface LaunchableEntryLauncher {
    fun launch(entry: LaunchableEntry): Boolean
}

internal class AndroidLaunchableEntryLauncher(
    context: Context,
) : LaunchableEntryLauncher {
    private val launcherApps = checkNotNull(
        context.applicationContext.getSystemService<LauncherApps>(),
    )

    override fun launch(entry: LaunchableEntry): Boolean = try {
        launcherApps.startMainActivity(
            entry.identity.componentName,
            entry.user,
            null,
            null,
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

internal class RapidActivationGuard(
    private val minimumIntervalMillis: Long = DEFAULT_MINIMUM_INTERVAL_MILLIS,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastActivationMillis: Long? = null

    fun tryAcquire(): Boolean {
        val now = elapsedRealtime()
        val previous = lastActivationMillis
        if (previous != null && now - previous < minimumIntervalMillis) return false

        lastActivationMillis = now
        return true
    }

    private companion object {
        const val DEFAULT_MINIMUM_INTERVAL_MILLIS = 600L
    }
}
