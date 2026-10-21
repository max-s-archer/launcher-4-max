package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerScreen
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation

class DrawerSectionAnchorUiTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var listState: LazyListState
    private lateinit var scope: CoroutineScope

    @Test
    fun leftAnchorPinsAndIsPushedOutAtItsSectionEnd() {
        showDrawer()
        scrollTo(10)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inset = context.resources.getDimensionPixelSize(R.dimen.drawer_section_anchor_top_inset)
        val listTop = composeRule.onNodeWithTag("drawer_application_list").fetchSemanticsNode().boundsInRoot.top
        val anchorTop = composeRule.onNodeWithTag("drawer_section_A").fetchSemanticsNode().boundsInRoot.top
        assertEquals(listTop + inset, anchorTop, 1f)
        composeRule.onNodeWithTag("drawer_section_A").assertHasNoClickAction()
        val anchorRight = composeRule.onNodeWithTag("drawer_section_A").fetchSemanticsNode().boundsInRoot.right
        val applicationLeft = composeRule.onAllNodesWithTag("drawer_application_row")[0]
            .fetchSemanticsNode().boundsInRoot.left
        assertEquals(anchorRight, applicationLeft, 1f)

        // The row is the rendered application height, not the old favorite minimum.
        val rowHeight = composeRule.onAllNodesWithTag("drawer_application_row")[0]
            .fetchSemanticsNode().boundsInRoot.height
        scrollTo(39, (rowHeight - 10f).toInt())
        val leaving = composeRule.onNodeWithTag("drawer_section_A").fetchSemanticsNode().boundsInRoot
        val arriving = composeRule.onNodeWithTag("drawer_section_B").fetchSemanticsNode().boundsInRoot
        assertTrue(leaving.bottom <= listTop + 11f)
        assertTrue(leaving.bottom <= arriving.top)
    }

    @Test
    fun settingsUsesGearAndSearchRebuildsOnlyApplicationAnchors() {
        showDrawer()
        scrollTo(80)
        composeRule.onNodeWithTag("drawer_settings_anchor").assertIsDisplayed()
            .assertHasNoClickAction().assertHeightIsEqualTo(16.dp)
        composeRule.onNodeWithTag("drawer_search_field").performClick()
        composeRule.onNodeWithTag("drawer_search_input").performTextInput("B application")
        composeRule.onNodeWithTag("drawer_section_B").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_section_A").assertDoesNotExist()
        composeRule.onNodeWithTag("drawer_settings_anchor").assertDoesNotExist()
        composeRule.onNodeWithTag("drawer_search_cancel").performClick()
        composeRule.onNodeWithTag("drawer_settings_anchor").assertIsDisplayed()
    }

    @Test
    fun switchingAnchorsAndRollingBackPreservesTheTopApplication() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var saving by mutableStateOf(false)
        composeRule.setContent {
            listState = rememberLazyListState()
            scope = rememberCoroutineScope()
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory,
                    listState = listState,
                    displaySettings = settings,
                    displaySettingsMutationEnabled = !saving,
                    onChangeDisplaySettings = {
                        settings = it
                        saving = true
                    },
                )
            }
        }
        scrollTo(11, 12)
        val before = topRowKey()
        composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag("drawer_section_anchor_1").performClick()
        composeRule.onNodeWithTag("drawer_section_anchor_1").assertIsSelected()
        composeRule.onNodeWithTag("drawer_left_section_anchors").assertIsDisplayed()
        assertEquals(before, topRowKey())
        // Simulate the host restoring its last durable state after a failed save.
        composeRule.runOnIdle {
            settings = DrawerDisplaySettings()
            saving = false
        }
        composeRule.onNodeWithTag("drawer_left_section_anchors").assertDoesNotExist()
        assertEquals(before, topRowKey())
    }

    @Test
    fun successfulSaveDoesNotUndoSubsequentScrolling() {
        var settings by mutableStateOf(DrawerDisplaySettings())
        var saving by mutableStateOf(false)
        composeRule.setContent {
            listState = rememberLazyListState()
            scope = rememberCoroutineScope()
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory,
                    listState = listState,
                    displaySettings = settings,
                    displaySettingsMutationEnabled = !saving,
                    onChangeDisplaySettings = { settings = it; saving = true },
                )
            }
        }
        scrollTo(11, 12)
        composeRule.onNodeWithTag("drawer_display_settings_entry").performClick()
        composeRule.onNodeWithTag("drawer_section_anchor_1").performClick()
        // Moving the list models a new position reached after dismissing the panel.
        scrollTo(25, 7)
        val current = topRowKey()
        composeRule.runOnIdle { saving = false }
        assertEquals(current, topRowKey())
        composeRule.runOnIdle { assertEquals(7, listState.firstVisibleItemScrollOffset) }
    }

    @Test
    fun favoriteSelectionUsesLeftAnchorsWithoutSettingsOrDisplayControls() {
        showDrawer(selection = true)
        scrollTo(10)
        composeRule.onNodeWithTag("drawer_left_section_anchors").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_section_A").assertHasNoClickAction()
        composeRule.onNodeWithTag("drawer_settings_anchor").assertDoesNotExist()
        composeRule.onNodeWithTag("drawer_display_settings_entry").assertDoesNotExist()
    }

    private fun showDrawer(selection: Boolean = false) {
        composeRule.setContent {
            listState = rememberLazyListState()
            scope = rememberCoroutineScope()
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = inventory,
                    listState = listState,
                    displaySettings = DrawerDisplaySettings(
                        sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
                    ),
                    favoriteSelectionTarget = if (selection) "Favorites" else null,
                )
            }
        }
        composeRule.onNodeWithTag("drawer_left_section_anchors").assertIsDisplayed()
    }

    private fun scrollTo(index: Int, offset: Int = 0) {
        composeRule.waitForIdle()
        composeRule.runOnIdle { scope.launch { listState.scrollToItem(index, offset) } }
        composeRule.waitForIdle()
    }

    private fun topRowKey(): Any = composeRule.runOnIdle {
        listState.layoutInfo.visibleItemsInfo.first { it.key.toString().startsWith("row:") }.key
    }

    private val inventory = LaunchableInventoryLoader {
        LaunchableInventorySnapshot(
            entries = ('A'..'B').flatMap { section ->
                List(40) { index ->
                    LaunchableEntry(
                        identity = LaunchableIdentity(0, ComponentName("example.$section$index", "Main")),
                        user = Process.myUserHandle(),
                        label = "$section application ${index.toString().padStart(2, '0')}",
                        icon = ColorDrawable(Color.TRANSPARENT),
                    )
                }
            },
            profileReadStatus = mapOf(0L to ProfileInventoryReadStatus.Complete),
        )
    }
}
