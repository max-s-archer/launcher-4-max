package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsPanel
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.style.HomeModuleStylePanel

class StyleSettingsInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun selectorAnimationBlocksOtherGroupsEvenWhenSaveAlreadySucceeded() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var changes = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerDisplaySettingsPanel(
                    settings = settings,
                    enabled = true,
                    onChangeSettings = { settings = it; changes++ },
                    onPreviewOpacity = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("drawer_name_placement_1").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("drawer_application_size_icon_slider").assertIsNotEnabled()
        composeRule.onNodeWithTag("drawer_items_per_row_increment").assertIsNotEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, changes) }

        finishSelectionAnimation()
        composeRule.onNodeWithTag("drawer_items_per_row_increment").assertIsEnabled()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(2, changes)
            assertEquals(DrawerNamePlacement.Below, settings.namePlacement)
            assertEquals(2, settings.itemsPerRow)
        }
    }

    @Test
    fun rejectedCandidateDoesNotLeaveAnimationGateLocked() {
        var changes = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerDisplaySettingsPanel(
                    settings = DrawerDisplaySettings(),
                    enabled = true,
                    // Model a save failure rolled back before the next composition.
                    onChangeSettings = { changes++ },
                    onPreviewOpacity = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("drawer_name_placement_1").performClick()
        composeRule.onNodeWithTag("drawer_name_placement_0").assertIsSelected()
        composeRule.onNodeWithTag("drawer_name_placement_1").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(2, changes) }
    }

    @Test
    fun outsideDismissalRemainsAvailableDuringAnimationAndSaving() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var saving by mutableStateOf(false)
        var dismissals = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerDisplaySettingsPanel(
                    settings = settings,
                    enabled = !saving,
                    onChangeSettings = { settings = it; saving = true },
                    onPreviewOpacity = {},
                    onDismiss = { dismissals++ },
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("drawer_name_placement_1").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("drawer_display_settings_modal")
            .performTouchInput { click(Offset(center.x, 1f)) }
        // The unresolved save keeps mutation controls disabled while the panel is still
        // present. Host removal happens only after the exit animation.
        composeRule.onNodeWithTag("drawer_items_per_row_increment").assertIsNotEnabled()
        finishSelectionAnimation()
        composeRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(DrawerNamePlacement.Below, settings.namePlacement)
        }
    }

    @Test
    fun selectorHasExplicitSelectedStateAndExtendedTouchTarget() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerDisplaySettingsPanel(
                    settings = settings,
                    enabled = true,
                    onChangeSettings = { settings = it },
                    onPreviewOpacity = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("drawer_name_placement_0").assertIsSelected()
            .assertIsEnabled()
        composeRule.onNodeWithTag("drawer_application_size_icon_slider")
            .assertContentDescriptionEquals(
                context.getString(R.string.style_settings_application_icon_size),
            )
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithTag("drawer_application_size_text_slider")
            .assertContentDescriptionEquals(
                context.getString(R.string.style_settings_application_text_size),
            )
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithTag("drawer_items_per_row_decrement").assertIsNotEnabled()
            .assertContentDescriptionEquals(
                context.getString(R.string.style_settings_decrease_items_per_row),
            )
        composeRule.onNodeWithTag("drawer_name_placement_1")
            .assertHeightIsEqualTo(40.dp)
            // Touch above the visible 40dp frame, inside the option's own target.
            .performTouchInput { click(Offset(center.x, 1f)) }
        composeRule.onNodeWithTag("drawer_name_placement_1").assertIsSelected()
        composeRule.runOnIdle { assertEquals(DrawerNamePlacement.Below, settings.namePlacement) }
    }

    @Test
    fun homeRetainsArrangementControlsAndBoundarySemantics() {
        var module by mutableStateOf(
            OrderedFavoriteModule(
                id = "module",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(LaunchableIdentity(0L, ComponentName("example", "example.Main"))),
                itemsPerRow = 2,
            ),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeModuleStylePanel(
                    selectedModule = module,
                    enabled = true,
                    maximumHeight = 300.dp,
                    onPreviewSizes = { _, _ -> },
                    onCommitIconSize = { module = module.copy(iconSize = it) },
                    onCommitTextSize = { module = module.copy(textSize = it) },
                    onChangeNamePlacement = { module = module.copy(namePlacement = it) },
                    onChangeItemsPerRow = { module = module.copy(itemsPerRow = it) },
                )
            }
        }
        composeRule.onNodeWithTag("home_name_placement_0").assertIsSelected()
        composeRule.onNodeWithTag("home_items_per_row_increment").assertIsNotEnabled()
        composeRule.onNodeWithTag("home_name_placement_1").assertHeightIsEqualTo(40.dp)
            .performClick()
        composeRule.onNodeWithTag("home_items_per_row_increment").assertIsEnabled()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(FavoriteNamePlacement.Below, module.namePlacement)
            assertEquals(3, module.itemsPerRow)
        }
    }

    private fun finishSelectionAnimation() {
        val duration = context.resources.getInteger(R.integer.short_property_animation_duration_ms)
        composeRule.mainClock.advanceTimeBy(duration.toLong() + 64L)
    }
}
