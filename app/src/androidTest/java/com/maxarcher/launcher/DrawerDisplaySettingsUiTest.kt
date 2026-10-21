package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.maxarcher.launcher.ui.drawer.DrawerApplicationSize
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerScreen

@RunWith(AndroidJUnit4::class)
class DrawerDisplaySettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun settlePanelAnimation() {
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(milliseconds = 500)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    @Test
    fun ordinaryEntryOpensModalAndOutsideSelectionDismissesIt() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(inventoryLoader = inventory())
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_display_settings_panel").assertIsDisplayed()

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_modal")
            .performTouchInput { click() }
        composeRule.onNodeWithTag(testTag = "drawer_display_settings_panel").assertDoesNotExist()
    }

    @Test
    fun applicationSizeSlidersPreviewIndependentlyAndCommitOnlyReleasedField() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var changes = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = settings,
                    onChangeDisplaySettings = { candidateSettings ->
                        settings = candidateSettings
                        changes++
                    },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_application_row")
            .assertHeightIsEqualTo(expectedHeight = 60.dp)
        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_application_size_text_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { action -> action(2f) }

        composeRule.onNodeWithTag(testTag = "drawer_application_row")
            .assertHeightIsEqualTo(expectedHeight = 60.dp)
        composeRule.onNodeWithTag(testTag = "drawer_display_settings_panel").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, changes)
            assertEquals(DrawerApplicationSize.Medium, settings.iconSize)
            assertEquals(DrawerApplicationSize.Small, settings.textSize)
        }
    }

    @Test
    fun sliderPreviewChangesOnlyTheTargetVisualDimension() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = DrawerDisplaySettings(),
                )
            }
        }

        composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
        settlePanelAnimation()
        // A press only selects a stop inside the 40dp stop target. swipeRight starts at
        // the node edge, outside that target, so the preview never leaves the current size.
        composeRule.onNodeWithTag("drawer_application_size_icon_slider")
            .performTouchInput {
                swipeRight(startX = center.x, endX = right)
            }
        composeRule.onNodeWithTag("drawer_application_row")
            .assertHeightIsEqualTo(60.dp)
        composeRule.onNodeWithText("Example application").assertTextEquals("Example application")
    }

    @Test
    fun iconSizeCommitDoesNotChangeTextSize() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var changes = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = settings,
                    onChangeDisplaySettings = { candidateSettings ->
                        settings = candidateSettings
                        changes++
                    },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_application_size_icon_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { action -> action(0f) }

        composeRule.runOnIdle {
            assertEquals(1, changes)
            assertEquals(DrawerApplicationSize.Large, settings.iconSize)
            assertEquals(DrawerApplicationSize.Medium, settings.textSize)
        }
    }

    @Test
    fun belowPlacementUsesBothIndependentSizeTiersForRowHeight() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = DrawerDisplaySettings(
                        iconSize = DrawerApplicationSize.Small,
                        textSize = DrawerApplicationSize.Large,
                        namePlacement = DrawerNamePlacement.Below,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("drawer_application_row")
            .assertHeightIsEqualTo(92.dp)
    }

    @Test
    fun rightPlacementUsesTheLargerOfIndependentIconAndTextTiers() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = DrawerDisplaySettings(
                        iconSize = DrawerApplicationSize.Small,
                        textSize = DrawerApplicationSize.Large,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("drawer_application_row")
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun searchModeHidesDisplaySettingsEntry() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(inventoryLoader = inventory())
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_search_field").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_search_input")
            .performTextInput(text = "Example")

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").assertDoesNotExist()
    }

    @Test
    fun unresolvedSaveDisablesFurtherSizeMutation() {
        var changeCount = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettingsMutationEnabled = false,
                    onChangeDisplaySettings = { changeCount += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_application_size_icon_slider")
            .assertIsNotEnabled()

        composeRule.runOnIdle { assertEquals(0, changeCount) }
    }

    @Test
    fun belowPlacementEnablesFourColumnsAndRightClampsToTwo() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = settings,
                    onChangeDisplaySettings = { candidateSettings ->
                        settings = candidateSettings
                    },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_display_settings_entry").performClick()
        settlePanelAnimation()
        composeRule.onNodeWithTag(testTag = "drawer_name_placement_1").performClick()
        settlePanelAnimation()
        repeat(times = 3) {
            composeRule.onNodeWithTag(testTag = "drawer_items_per_row_increment")
                .performTouchInput { click(position = topCenter) }
            settlePanelAnimation()
        }
        val description = composeRule.onNodeWithTag(
            testTag = "drawer_items_per_row_value",
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertEquals("4", description)

        composeRule.onNodeWithTag(testTag = "drawer_name_placement_0").performClick()

        composeRule.runOnIdle {
            assertEquals(DrawerNamePlacement.Right, settings.namePlacement)
            assertEquals(2, settings.itemsPerRow)
        }
    }

    @Test
    fun hiddenPlacementEnablesSixColumnsAndDisablesTextSize() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory(),
                    displaySettings = settings,
                    onChangeDisplaySettings = { settings = it },
                )
            }
        }

        composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
        settlePanelAnimation()
        composeRule.onNodeWithTag("drawer_name_placement_2").performClick()
        settlePanelAnimation()
        repeat(5) {
            composeRule.onNodeWithTag("drawer_items_per_row_increment")
                .performTouchInput { click(position = topCenter) }
            settlePanelAnimation()
        }

        composeRule.onNodeWithTag("drawer_application_size_text_slider").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(DrawerNamePlacement.Hidden, settings.namePlacement)
            assertEquals(6, settings.itemsPerRow)
        }
    }

    private fun inventory(): LaunchableInventoryLoader = LaunchableInventoryLoader {
        LaunchableInventorySnapshot(
            entries = listOf(
                LaunchableEntry(
                    identity = LaunchableIdentity(
                        profileSerialNumber = 0,
                        componentName = ComponentName(
                            "com.example.application",
                            "MainActivity",
                        ),
                    ),
                    user = Process.myUserHandle(),
                    label = "Example application",
                    icon = ColorDrawable(Color.TRANSPARENT),
                ),
            ),
            profileReadStatus = mapOf(0L to ProfileInventoryReadStatus.Complete),
        )
    }
}
