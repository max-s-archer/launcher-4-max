package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerFavoriteSelectionRow

@RunWith(AndroidJUnit4::class)
class DrawerFavoriteSelectionRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun selectedRowAnnouncesItsOrderWithoutASecondFocusTarget() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerFavoriteSelectionRow(
                    entry = testEntry(),
                    displaySettings = DrawerDisplaySettings(),
                    order = 2,
                    alreadyFavorite = false,
                    enabled = true,
                    onClick = {},
                )
            }
        }
        composeRule.onNode(
            matcher = SemanticsMatcher.expectValue(
                key = SemanticsProperties.StateDescription,
                expectedValue = context.getString(R.string.drawer_selection_order_format, 2),
            ),
        ).assertExists()
        // The order badge is composed on the cell but excluded from the accessibility
        // tree, so the row stays the single focus target announcing the order.
        composeRule.onNodeWithTag(testTag = "drawer_favorite_selection_number")
            .assertDoesNotExist()
    }

    @Test
    fun unselectedRowAnnouncesPlainStateWithoutBadge() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerFavoriteSelectionRow(
                    entry = testEntry(),
                    displaySettings = DrawerDisplaySettings(),
                    order = null,
                    alreadyFavorite = false,
                    enabled = true,
                    onClick = {},
                )
            }
        }
        composeRule.onNode(
            matcher = SemanticsMatcher.expectValue(
                key = SemanticsProperties.StateDescription,
                expectedValue = context.getString(R.string.drawer_selection_not_selected),
            ),
        ).assertExists()
    }

    @Test
    fun alreadyFavoriteRowReportsUnavailabilityAndStaysDisabled() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerFavoriteSelectionRow(
                    entry = testEntry(),
                    displaySettings = DrawerDisplaySettings(),
                    order = null,
                    alreadyFavorite = true,
                    enabled = false,
                    onClick = {},
                )
            }
        }
        composeRule.onNode(
            matcher = SemanticsMatcher.expectValue(
                key = SemanticsProperties.StateDescription,
                expectedValue = context.getString(
                    R.string.drawer_selection_already_favorite_unavailable,
                ),
            ),
        ).assertExists()
        composeRule.onNodeWithTag(testTag = "drawer_favorite_selection_row")
            .assertIsNotEnabled()
    }

    @Test
    fun clickingAnEnabledRowTogglesTheSelectionModel() {
        var toggles = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerFavoriteSelectionRow(
                    entry = testEntry(),
                    displaySettings = DrawerDisplaySettings(),
                    order = null,
                    alreadyFavorite = false,
                    enabled = true,
                    onClick = { toggles++ },
                )
            }
        }
        composeRule.onNodeWithTag(testTag = "drawer_favorite_selection_row").performClick()
        composeRule.runOnIdle { assertEquals(1, toggles) }
    }

    @Test
    fun belowRowHeightUsesIndependentIconAndTextTiers() {
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerFavoriteSelectionRow(
                    entry = testEntry(),
                    displaySettings = DrawerDisplaySettings(
                        iconSize = com.maxarcher.launcher.ui.drawer.DrawerApplicationSize.Small,
                        textSize = com.maxarcher.launcher.ui.drawer.DrawerApplicationSize.Large,
                        namePlacement = com.maxarcher.launcher.ui.drawer.DrawerNamePlacement.Below,
                    ),
                    order = null,
                    alreadyFavorite = false,
                    enabled = true,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("drawer_favorite_selection_row")
            .assertHeightIsEqualTo(92.dp)
    }

    private fun testEntry(): LaunchableEntry = LaunchableEntry(
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
    )
}
