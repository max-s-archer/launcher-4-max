package com.maxarcher.launcher

import android.annotation.SuppressLint
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.swipeRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsPanel
import com.maxarcher.launcher.ui.style.StyleBackgroundOpacityBlock

class DrawerBackgroundOpacityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var persisted = DrawerDisplaySettings()
    private val previews = mutableListOf<Int?>()
    private val commits = mutableListOf<DrawerDisplaySettings>()
    private var panelVisible by mutableStateOf(true)

    /**
     * The panel's height animation moves the slider while gestures inject; settle the
     * animation clock deterministically before interacting with panel content.
     */
    private fun settlePanelAnimation() {
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(milliseconds = 500)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    private fun setContent() {
        persisted = DrawerDisplaySettings()
        previews.clear()
        commits.clear()
        panelVisible = true
        composeRule.setContent {
            Launcher4MaxTheme {
                var settings by remember { mutableStateOf(persisted) }
                if (panelVisible) {
                    DrawerDisplaySettingsPanel(
                        settings = settings,
                        enabled = true,
                        onChangeSettings = { candidate ->
                            commits.add(candidate)
                            settings = candidate
                        },
                        onPreviewOpacity = { preview -> previews.add(preview) },
                        onDismiss = {},
                    )
                }
            }
        }
        settlePanelAnimation()
    }

    @SuppressLint("CheckResult")
    @Test
    fun sliderReleaseCommitsExactlyOneSaveWithTheNewOpacity() {
        setContent()
        val slider = composeRule.onNodeWithTag("drawer_background_opacity_slider")
        // Bisect the gesture failure first: the slider must be visible and enabled,
        // otherwise a swipe cannot commit.
        slider.assertIsDisplayed()
        slider.assertIsEnabled()
        slider.performSemanticsAction(SemanticsActions.SetProgress) { action ->
            action(100f)
        }
        composeRule.waitForIdle()
        assertEquals(1, commits.size)
        assertEquals(100, commits.single().backgroundOpacity)
        assertEquals(null, previews.last())
    }

    @Test
    fun dragPreviewUpdatesTheBackgroundWithoutCommitting() {
        setContent()
        val slider = composeRule.onNodeWithTag("drawer_background_opacity_slider")
        slider.assertIsDisplayed()
        slider.assertIsEnabled()
        slider.performTouchInput {
            down(center)
            moveBy(percentOffset(x = 0.5f, y = 0f))
            up()
        }
        assertTrue(previews.any { it != null && it != persisted.backgroundOpacity })
        composeRule.waitForIdle()
        assertEquals(1, commits.size)

        // Removing the panel after release leaves the committed value and clears the preview.
        panelVisible = false
        composeRule.waitForIdle()
        assertEquals(null, previews.last())
        assertEquals(1, commits.size)
    }

    @Test
    fun cancelledShapeSliderPreviewDoesNotCommitAndNextReleaseUsesRefreshedValue() {
        var saved by mutableStateOf(50)
        var draft by mutableStateOf(50)
        val released = mutableListOf<Int>()
        var cancellations = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                StyleBackgroundOpacityBlock(
                    title = "Corner radius",
                    opacity = draft,
                    enabled = true,
                    onOpacityChange = { draft = it },
                    onOpacityChangeFinished = { released.add(it) },
                    onOpacityChangeCancelled = {
                        cancellations++
                        draft = saved
                    },
                    sliderTestTag = "shape_slider",
                )
            }
        }
        val slider = composeRule.onNodeWithTag("shape_slider")
        slider.performTouchInput {
            down(center)
            moveBy(percentOffset(x = 0.3f, y = 0f))
        }
        composeRule.runOnIdle {
            assertTrue(draft != saved)
            assertTrue(released.isEmpty())
        }
        slider.performTouchInput { cancel() }
        composeRule.runOnIdle {
            assertEquals(1, cancellations)
            assertEquals(saved, draft)
            assertTrue(released.isEmpty())
        }
        // Accessibility has no pointer-down and must recover from the cancelled gesture.
        slider.performSemanticsAction(SemanticsActions.SetProgress) { action ->
            action(0f)
        }
        composeRule.runOnIdle {
            assertEquals(listOf(0), released)
            assertEquals(0, draft)
            assertEquals(1, cancellations)
            saved = 20
            draft = saved
        }
        composeRule.waitForIdle()
        slider.performTouchInput {
            down(center)
            moveBy(percentOffset(x = 0.2f, y = 0f))
            up()
        }
        composeRule.runOnIdle {
            assertEquals(listOf(0, draft), released)
            assertTrue(released.last() != saved)
            assertEquals(1, cancellations)
        }
    }
}
