package com.maxarcher.launcher

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

internal class AccessibilityLockProbeService : AccessibilityService(), LockRequestPort {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityLockConnection.connected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AccessibilityLockConnection.disconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityLockConnection.disconnected(this)
        super.onDestroy()
    }

    override fun requestLock(): LockRequestResult = requestLockAction(
        availableActionIds = systemActions.map { it.id },
        perform = { performGlobalAction(it) },
    )
}

internal fun requestLockAction(
    availableActionIds: Collection<Int>,
    perform: (Int) -> Boolean,
): LockRequestResult {
    val action = AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
    if (action !in availableActionIds) return LockRequestResult.ActionUnavailable
    return if (perform(action)) LockRequestResult.Requested else LockRequestResult.ActionRejected
}
