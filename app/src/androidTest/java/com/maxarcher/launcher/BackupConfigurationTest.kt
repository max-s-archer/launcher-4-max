package com.maxarcher.launcher

import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class BackupConfigurationTest {
    private val excludedDomains = setOf(
        "root",
        "file",
        "database",
        "sharedpref",
        "external",
    )

    @Test
    fun packagedApplicationDisablesBackupAndExcludesStorageFromCloudAndDeviceTransfer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)

        val exclusions = buildSet {
            val parser = context.resources.getXml(R.xml.data_extraction_rules)
            var section: String? = null
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> section = parser.name
                        "exclude" -> add(
                            Triple(
                                section,
                                parser.getAttributeValue(null, "domain"),
                                parser.getAttributeValue(null, "path"),
                            ),
                        )
                    }
                } else if (
                    parser.eventType == XmlPullParser.END_TAG &&
                    parser.name == section
                ) {
                    section = null
                }
                parser.next()
            }
        }

        listOf("cloud-backup", "device-transfer").forEach { section ->
            excludedDomains.forEach { domain ->
                assertTrue(Triple(section, domain, ".") in exclusions)
            }
        }
    }
}
