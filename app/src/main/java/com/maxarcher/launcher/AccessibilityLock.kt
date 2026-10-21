package com.maxarcher.launcher

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class LockRequestResult {
    Requested,
    ServiceDisconnected,
    ActionUnavailable,
    ActionRejected,
}

internal fun interface LockRequestPort {
    fun requestLock(): LockRequestResult
}

internal interface AccessibilityLockController : LockRequestPort {
    val availableForValidation: Boolean
    val connectionState: StateFlow<Boolean>
    fun isSystemEnabled(): Boolean
    fun openAccessibilitySettings(): Boolean
}

internal object EmptyAccessibilityLockController : AccessibilityLockController {
    override val availableForValidation = false
    override val connectionState = MutableStateFlow(false)
    override fun isSystemEnabled() = false
    override fun openAccessibilitySettings() = false
    override fun requestLock() = LockRequestResult.ServiceDisconnected
}

/**
 * The only application-to-service seam. It owns no Context, event, or window data.
 */
internal object AccessibilityLockConnection : LockRequestPort {
    @Volatile
    private var connectedService: LockRequestPort? = null
    private val mutableConnectionState = MutableStateFlow(false)

    val connectionState: StateFlow<Boolean> = mutableConnectionState.asStateFlow()

    override fun requestLock(): LockRequestResult =
        connectedService?.requestLock() ?: LockRequestResult.ServiceDisconnected

    fun connected(service: LockRequestPort) {
        connectedService = service
        mutableConnectionState.value = true
    }

    fun disconnected(service: LockRequestPort) {
        if (connectedService === service) {
            connectedService = null
            mutableConnectionState.value = false
        }
    }
}

internal class AndroidAccessibilityLockController(
    context: Context,
    private val serviceComponent: ComponentName,
) : AccessibilityLockController {
    private val applicationContext = context.applicationContext
    private val accessibilityManager =
        applicationContext.getSystemService(AccessibilityManager::class.java)

    override val availableForValidation = true
    override val connectionState = AccessibilityLockConnection.connectionState

    override fun isSystemEnabled(): Boolean = accessibilityManager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
            ComponentName(serviceInfo.packageName, serviceInfo.name) == serviceComponent
        }

    override fun openAccessibilitySettings(): Boolean = try {
        applicationContext.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    override fun requestLock(): LockRequestResult = AccessibilityLockConnection.requestLock()
}

internal fun accessibilityLockServiceComponent(context: Context): ComponentName =
    ComponentName(
        context.packageName,
        "${context.packageName}.AccessibilityLockProbeService",
    )
