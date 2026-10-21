package com.maxarcher.launcher

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CandidateConfigurationTest {
    @Test
    fun packagedCandidateUsesApprovedApplicationAndVersionIdentity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        assertEquals("com.maxarcher.launcher", context.packageName)
        assertEquals("1.7.2", packageInfo.versionName)
        assertEquals(10L, packageInfo.longVersionCode)
    }
}
