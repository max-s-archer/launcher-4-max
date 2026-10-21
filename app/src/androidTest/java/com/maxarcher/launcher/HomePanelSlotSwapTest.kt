package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class HomePanelSlotSwapTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun testIdentity(serial: Int): LaunchableIdentity = LaunchableIdentity(
        profileSerialNumber = serial.toLong(),
        componentName = ComponentName(
            "com.example.application",
            "MainActivity$serial",
        ),
    )

    @Test
    fun editDockSitsAtTopWithAStableToggleTargetAcrossPanelStates() {
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(listOf(testIdentity(0))),
                    editMode = true,
                    stylePanelExpanded = expanded,
                    onStylePanelExpandedChange = { expanded = it },
                )
            }
        }
        composeRule.onNodeWithTag(testTag = "home_edit_dock").assertIsDisplayed()
        val dockBounds = composeRule.onNodeWithTag(testTag = "home_edit_dock")
            .fetchSemanticsNode().boundsInRoot
        val expandBounds = composeRule.onNodeWithTag(testTag = "home_style_panel_expand")
            .fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag(testTag = "home_style_panel_expand").performClick()
        composeRule.onNodeWithTag(testTag = "home_style_panel").assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = "home_style_panel_collapse").assertIsDisplayed()

        val dockBoundsExpanded = composeRule.onNodeWithTag(testTag = "home_edit_dock")
            .fetchSemanticsNode().boundsInRoot
        val collapseBounds = composeRule.onNodeWithTag(testTag = "home_style_panel_collapse")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(dockBounds, dockBoundsExpanded)
        assertEquals(expandBounds, collapseBounds)
    }

    @Test
    fun panelTogglePreservesTheMainListScrollPosition() {
        val identities = (0..29).map(::testIdentity)
        val listState = LazyListState(firstVisibleItemIndex = 10)
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(identities),
                    favoriteListState = listState,
                    editMode = true,
                    stylePanelExpanded = expanded,
                    onStylePanelExpandedChange = { expanded = it },
                )
            }
        }
        composeRule.onNodeWithTag(testTag = "home_style_panel_expand").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(10, listState.firstVisibleItemIndex) }
        composeRule.onNodeWithTag(testTag = "home_style_panel_collapse").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(10, listState.firstVisibleItemIndex) }
    }

    @Test
    fun slotSwapConvergesToTheLatestTargetAfterMidAnimationRetoggle() {
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(listOf(testIdentity(0))),
                    editMode = true,
                    stylePanelExpanded = expanded,
                    onStylePanelExpandedChange = { expanded = it },
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(testTag = "home_style_panel_expand").performClick()
        composeRule.mainClock.advanceTimeBy(milliseconds = 100L)
        composeRule.onNodeWithTag(testTag = "home_style_panel_collapse").performClick()
        composeRule.mainClock.advanceTimeBy(milliseconds = 400L)
        composeRule.onNodeWithTag(testTag = "home_style_panel").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_time").assertIsDisplayed()

        composeRule.onNodeWithTag(testTag = "home_style_panel_expand").performClick()
        composeRule.mainClock.advanceTimeBy(milliseconds = 300L)
        composeRule.onNodeWithTag(testTag = "home_style_panel").assertIsDisplayed()
    }
}
