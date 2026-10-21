package com.maxarcher.launcher

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupStringParityTest {
    @Test
    fun dataStringsResolveInEnglishAndSimplifiedChinese() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val english = context.createConfigurationContext(
            Configuration().apply { setLocale(Locale.US) },
        )
        val simplifiedChinese = context.createConfigurationContext(
            Configuration().apply { setLocale(Locale.SIMPLIFIED_CHINESE) },
        )
        val identifiers = listOf(
            R.string.settings_data_backup,
            R.string.settings_data_restore,
            R.string.settings_data_backup_success,
            R.string.settings_data_backup_failure,
            R.string.settings_data_restore_success,
            R.string.settings_data_restore_failure,
            R.string.settings_data_restore_dialog_title,
            R.string.settings_data_restore_dialog_body,
            R.string.settings_data_restore_confirm,
            R.string.privacy_statement,
            R.string.quick_action_settings,
            R.string.quick_action_double_tap_slot,
            R.string.quick_action_long_press_slot,
            R.string.quick_action_no_action,
            R.string.quick_action_edit_mode,
            R.string.quick_action_screen_lock,
            R.string.accessibility_lock_probe_label,
            R.string.accessibility_lock_probe_description,
            R.string.screen_lock_explanation,
            R.string.accessibility_disclosure_title,
            R.string.accessibility_disclosure_body,
            R.string.home_default_launcher_prompt_title,
            R.string.home_default_launcher_prompt_supporting,
            R.string.home_default_launcher_prompt_dismiss,
        )

        identifiers.forEach { identifier ->
            assertTrue(english.getString(identifier).isNotBlank())
            assertTrue(simplifiedChinese.getString(identifier).isNotBlank())
        }
        val englishConfirmation = english.getString(R.string.settings_data_restore_dialog_body)
        val chineseConfirmation = simplifiedChinese.getString(
            R.string.settings_data_restore_dialog_body,
        )
        assertTrue(englishConfirmation.contains("quick-action bindings"))
        assertTrue(englishConfirmation.contains("icon-shape preference"))
        assertTrue(chineseConfirmation.contains("快捷操作绑定"))
        assertTrue(chineseConfirmation.contains("图标形状偏好"))
    }

    @Test
    fun privacyStatementDisclosesBackupAndRestoreInBothLanguages() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val english = context.createConfigurationContext(
            Configuration().apply { setLocale(Locale.US) },
        )
        val simplifiedChinese = context.createConfigurationContext(
            Configuration().apply { setLocale(Locale.SIMPLIFIED_CHINESE) },
        )

        val englishStatement = english.getString(R.string.privacy_statement)
        val chineseStatement = simplifiedChinese.getString(R.string.privacy_statement)

        assertTrue(englishStatement.contains("manual local backup"))
        assertTrue(englishStatement.contains("never uploaded"))
        assertTrue(englishStatement.contains("quick-action bindings"))
        assertTrue(englishStatement.contains("bound gesture"))
        assertTrue(englishStatement.contains("default-Launcher prompt"))
        assertTrue(englishStatement.contains("not part of the backup file"))
        assertTrue(chineseStatement.contains("手动本地备份"))
        assertTrue(chineseStatement.contains("绝不会被上传"))
        assertTrue(chineseStatement.contains("快捷操作绑定"))
        assertTrue(chineseStatement.contains("已绑定的手势"))
        assertTrue(chineseStatement.contains("默认启动器提示"))
        assertTrue(chineseStatement.contains("不包含在备份文件中"))
    }
}
