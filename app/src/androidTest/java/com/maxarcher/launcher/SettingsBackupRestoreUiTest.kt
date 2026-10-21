package com.maxarcher.launcher

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.maxarcher.launcher.ui.settings.EmptySettingsPlatform
import com.maxarcher.launcher.ui.settings.RestoreConfirmationDialog
import com.maxarcher.launcher.ui.settings.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsBackupRestoreUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dataEntriesArePresentedWhenBackupControlIsAvailable() {
        composeRule.setContent {
            SettingsScreen(
                platform = EmptySettingsPlatform,
                licenseText = "",
                backupController = FakeSettingsBackupControl(),
                onBack = {},
            )
        }

        composeRule
            .onNodeWithTag("settings_data_backup")
            .assertExists()
        composeRule
            .onNodeWithTag("settings_data_restore")
            .assertExists()
    }

    @Test
    fun dataEntriesAreAbsentWithoutBackupControl() {
        composeRule.setContent {
            SettingsScreen(
                platform = EmptySettingsPlatform,
                licenseText = "",
                onBack = {},
            )
        }

        composeRule
            .onNodeWithTag("settings_data_backup")
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag("settings_data_restore")
            .assertDoesNotExist()
    }

    @Test
    fun confirmActionInvokesRestoreAndNeverCancel() {
        var confirmCount = 0
        var cancelCount = 0
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) {
                RestoreConfirmationDialog(
                    onConfirm = {
                        confirmCount += 1
                        visible = false
                    },
                    onCancel = {
                        cancelCount += 1
                        visible = false
                    },
                )
            }
        }

        composeRule
            .onNodeWithTag("settings_restore_confirm")
            .performClick()

        composeRule
            .onNodeWithTag("settings_restore_dialog")
            .assertDoesNotExist()
        assertEquals(1, confirmCount)
        assertEquals(0, cancelCount)
    }

    @Test
    fun cancelActionChangesNothing() {
        var confirmCount = 0
        var cancelCount = 0
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) {
                RestoreConfirmationDialog(
                    onConfirm = {
                        confirmCount += 1
                        visible = false
                    },
                    onCancel = {
                        cancelCount += 1
                        visible = false
                    },
                )
            }
        }

        composeRule
            .onNodeWithTag("settings_restore_cancel")
            .performClick()

        composeRule
            .onNodeWithTag("settings_restore_dialog")
            .assertDoesNotExist()
        assertEquals(0, confirmCount)
        assertEquals(1, cancelCount)
    }

    private class FakeSettingsBackupControl : SettingsBackupControl {
        override fun suggestedFileName(): String = "launcher4max-backup-test.json"

        override suspend fun writeBackup(uri: Uri): Boolean = true

        override suspend fun readBackup(uri: Uri): SettingsBackupState? = null

        override suspend fun restore(backup: SettingsBackupState): Boolean = true
    }
}
