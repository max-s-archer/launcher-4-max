package com.maxarcher.launcher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import android.util.AtomicFile
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerApplicationSize
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsReadState
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsStore
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation

class DrawerSettingsIntegrationTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun dismissedSavePersistsCompleteStateAcrossActivityRecreation() = exerciseSave(false)

    @Test fun dismissedFailedSaveRollsBackAndAllowsRetry() = exerciseSave(true)

    @Test fun backDuringSaveDoesNotCancelPersistence() = exerciseSave(false, useBack = true)

    @Test fun failedSaveWithPanelOpenReportsOnceAndAllowsRetry() =
        exerciseSave(true, dismissDuringSave = false)

    @SuppressLint("CheckResult")
    @Test fun recreationDuringWriteWaitsForCompletePersistedState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "drawer-recreation-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val file = File(directory, "settings.bin")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val loader = inventory()
        val oldStore = DrawerDisplaySettingsStore(object : AtomicFile(file) {
            override fun startWrite(): FileOutputStream {
                val stream = super.startWrite()
                entered.countDown()
                if (!release.await(30, TimeUnit.SECONDS)) {
                    super.failWrite(stream)
                    throw IOException("Write gate timed out")
                }
                return stream
            }
        })
        try {
            composeRule.setContent {
                Launcher4MaxTheme {
                    Launcher4MaxApp(
                        inventoryLoader = loader,
                        drawerDisplaySettingsStore = oldStore,
                    )
                }
            }
            composeRule.waitUntil(5_000) { oldStore.state.value is DrawerDisplaySettingsReadState.Readable }
            openDrawerPanel()
            val slider = composeRule.onNodeWithTag("drawer_background_opacity_slider")
            slider.assertIsDisplayed()
            slider.assertIsEnabled()
            slider.performTouchInput { swipeRight(startX = center.x) }
            awaitSavingState()
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            composeRule.activityRule.scenario.recreate()
            val recreatedStore = DrawerDisplaySettingsStore(file)
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    Launcher4MaxTheme {
                        Launcher4MaxApp(
                            inventoryLoader = loader,
                            drawerDisplaySettingsStore = recreatedStore,
                        )
                    }
                }
            }
            composeRule.onNodeWithTag("home_surface").performTouchInput { swipeUp() }
            composeRule.onNodeWithTag("drawer_loading").assertIsDisplayed()
            composeRule.onNodeWithTag("drawer_display_settings_entry").assertDoesNotExist()
            release.countDown()
            val expected = DrawerDisplaySettingsReadState.Readable(
                DrawerDisplaySettings(backgroundOpacity = 100),
            )
            composeRule.waitUntil(5_000) { recreatedStore.state.value == expected }
            composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
            composeRule.onNodeWithTag("drawer_background_opacity_slider").assertExists()
        } finally {
            release.countDown()
            runBlocking { oldStore.load() }
            composeRule.waitForIdle()
            directory.deleteRecursively()
        }
    }

    @SuppressLint("CheckResult")
    private fun exerciseSave(
        failFirstWrite: Boolean,
        useBack: Boolean = false,
        dismissDuringSave: Boolean = true,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "drawer-integration-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val file = File(directory, "settings.bin")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        var intercept = true
        val initial = DrawerDisplaySettings(
            iconSize = DrawerApplicationSize.Small,
            textSize = DrawerApplicationSize.Small,
            namePlacement = DrawerNamePlacement.Below,
            itemsPerRow = 4,
            sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
        )
        val candidate = initial.copy(backgroundOpacity = 100)
        runBlocking {
            DrawerDisplaySettingsStore(file).apply { load(); assertTrue(replace(initial)) }
        }
        val store = DrawerDisplaySettingsStore(object : AtomicFile(file) {
            override fun startWrite(): FileOutputStream {
                if (intercept) {
                    intercept = false
                    entered.countDown()
                    if (!release.await(30, TimeUnit.SECONDS)) throw IOException("Write gate timed out")
                    if (failFirstWrite) {
                        finished.countDown()
                        throw IOException("Injected save failure")
                    }
                }
                return super.startWrite()
            }

            override fun finishWrite(stream: FileOutputStream?) {
                super.finishWrite(stream)
                finished.countDown()
            }
        })
        val loader = inventory()
        val failureNotifications = AtomicInteger()
        try {
            composeRule.setContent {
                Launcher4MaxTheme {
                    Launcher4MaxApp(
                        inventoryLoader = loader,
                        drawerDisplaySettingsStore = store,
                        onDrawerDisplaySettingsSaveFailure = { failureNotifications.incrementAndGet() },
                    )
                }
            }
            composeRule.waitUntil(5_000) { store.state.value is DrawerDisplaySettingsReadState.Readable }
            openDrawerPanel()
            val slider = composeRule.onNodeWithTag("drawer_background_opacity_slider")
            // Bisect the gesture failure first: the slider must be visible and enabled,
            // otherwise a swipe cannot start the release-committed save.
            slider.assertIsDisplayed()
            slider.assertIsEnabled()
            slider.performTouchInput { swipeRight(startX = center.x) }
            awaitSavingState()
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            composeRule.onNodeWithTag("drawer_background_opacity_slider").assertIsNotEnabled()
            if (dismissDuringSave) {
                dismissPanel(useBack)
                composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
            }
            for (tag in listOf("drawer_application_size_icon_slider", "drawer_name_placement_0",
                "drawer_section_anchor_0", "drawer_items_per_row_decrement")) {
                composeRule.onNodeWithTag(tag).assertIsNotEnabled()
            }
            composeRule.onNodeWithTag("drawer_background_opacity_slider").assertIsNotEnabled()
            if (dismissDuringSave) dismissPanel(useBack)
            release.countDown()
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            // A dismissed panel must stay dismissed after either result.
            composeRule.waitForIdle()
            if (dismissDuringSave) {
                composeRule.onNodeWithTag("drawer_display_settings_panel").assertDoesNotExist()
                composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
            } else {
                composeRule.onNodeWithTag("drawer_display_settings_panel").assertIsDisplayed()
            }
            if (failFirstWrite) {
                assertEquals(1, failureNotifications.get())
                assertEquals(DrawerDisplaySettingsReadState.Readable(initial), store.state.value)
                composeRule.onNodeWithTag("drawer_background_opacity_slider")
                    .performTouchInput { swipeRight(startX = center.x) }
            } else {
                composeRule.waitUntil(5_000) {
                    (store.state.value as? DrawerDisplaySettingsReadState.Readable)
                        ?.settings?.backgroundOpacity == 100
                }
            }
            composeRule.waitUntil(5_000) {
                store.state.value == DrawerDisplaySettingsReadState.Readable(candidate)
            }
            val reloaded = DrawerDisplaySettingsStore(file)
            runBlocking { reloaded.load() }
            assertEquals(DrawerDisplaySettingsReadState.Readable(candidate), reloaded.state.value)
            assertEquals(if (failFirstWrite) 1 else 0, failureNotifications.get())
            // A new Activity and a fresh store read the actual persisted file.
            val recreatedStore = DrawerDisplaySettingsStore(file)
            composeRule.activityRule.scenario.recreate()
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    Launcher4MaxTheme {
                        Launcher4MaxApp(
                            inventoryLoader = loader,
                            drawerDisplaySettingsStore = recreatedStore,
                        )
                    }
                }
            }
            composeRule.waitUntil(5_000) { recreatedStore.state.value is DrawerDisplaySettingsReadState.Readable }
            openDrawerPanel()
            for (tag in listOf("drawer_name_placement_1", "drawer_section_anchor_1")) {
                composeRule.onNodeWithTag(tag).assertIsSelected()
            }
            composeRule.onNodeWithTag("drawer_application_size_icon_slider").assertIsEnabled()
            assertEquals(DrawerDisplaySettingsReadState.Readable(candidate), recreatedStore.state.value)
        } finally {
            release.countDown()
            runBlocking { store.load() }
            composeRule.waitForIdle()
            directory.deleteRecursively()
        }
    }


    /** Bisects a silent release-commit failure: the panel must enter its
     *  single-unresolved-save state (slider disabled) before any store write is pending. */
    private fun awaitSavingState() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("drawer_background_opacity_slider")
                .fetchSemanticsNodes().firstOrNull()
                ?.config?.contains(SemanticsProperties.Disabled) == true
        }
    }

    private fun openDrawerPanel() {
        composeRule.onNodeWithTag("home_surface").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
        // The panel's height animation moves the slider while gestures inject; settle
        // the animation clock deterministically before interacting with the slider.
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(milliseconds = 500)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    private fun dismissPanel(useBack: Boolean = false) {
        if (useBack) {
            composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        } else {
            composeRule.onNodeWithTag("drawer_display_settings_modal").performTouchInput {
                click(Offset(centerX, 1f))
            }
        }
        composeRule.onNodeWithTag("drawer_display_settings_panel").assertDoesNotExist()
    }

    private fun inventory() = LaunchableInventoryLoader {
        LaunchableInventorySnapshot(
            entries = listOf(LaunchableEntry(
                identity = LaunchableIdentity(0, ComponentName("com.example.application", "MainActivity")),
                user = Process.myUserHandle(),
                label = "Example application",
                icon = ColorDrawable(Color.TRANSPARENT),
            )),
            profileReadStatus = mapOf(0L to ProfileInventoryReadStatus.Complete),
        )
    }
}
