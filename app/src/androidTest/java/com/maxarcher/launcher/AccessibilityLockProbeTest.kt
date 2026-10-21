package com.maxarcher.launcher

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityLockProbeTest {
    @Suppress("DEPRECATION")
    @Test
    fun probeManifestUsesSystemBindingPermissionAndMetadata() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val component = ComponentName(context, AccessibilityLockProbeService::class.java)
        val serviceInfo = context.packageManager.getServiceInfo(
            component,
            PackageManager.GET_META_DATA,
        )

        assertTrue(serviceInfo.exported)
        assertEquals(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            serviceInfo.permission,
        )
        assertTrue(serviceInfo.metaData.getInt(AccessibilityService.SERVICE_META_DATA) != 0)
    }

    @After
    fun disconnectProbe() {
        AccessibilityLockConnection.disconnected(fakePort)
    }

    @Test
    fun unavailableActionFailsClosedWithoutCallingPlatform() {
        var called = false

        val result = requestLockAction(emptyList()) {
            called = true
            true
        }

        assertEquals(LockRequestResult.ActionUnavailable, result)
        assertFalse(called)
    }

    @Test
    fun rejectedActionIsReportedWithoutRetry() {
        var calls = 0

        val result = requestLockAction(listOf(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) {
            calls += 1
            false
        }

        assertEquals(LockRequestResult.ActionRejected, result)
        assertEquals(1, calls)
    }

    @Test
    fun availableActionIsRequestedExactlyOnce() {
        var calls = 0

        val result = requestLockAction(listOf(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) {
            calls += 1
            true
        }

        assertEquals(LockRequestResult.Requested, result)
        assertEquals(1, calls)
    }

    @Test
    fun connectionBoundaryDistinguishesDisconnectedAndConnectedState() {
        assertFalse(AccessibilityLockConnection.connectionState.value)
        assertEquals(
            LockRequestResult.ServiceDisconnected,
            AccessibilityLockConnection.requestLock(),
        )

        AccessibilityLockConnection.connected(fakePort)

        assertTrue(AccessibilityLockConnection.connectionState.value)
        assertEquals(LockRequestResult.Requested, AccessibilityLockConnection.requestLock())
    }

    private val fakePort = LockRequestPort { LockRequestResult.Requested }
}
