package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxarcher.launcher.ui.drawer.DrawerRestorationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.maxarcher.launcher.ui.drawer.DrawerScreen
import com.maxarcher.launcher.ui.drawer.DrawerSection
import com.maxarcher.launcher.ui.drawer.captureDrawerListPosition
import com.maxarcher.launcher.ui.drawer.captureDrawerOrdinaryListPosition
import com.maxarcher.launcher.ui.drawer.drawerSearchMatchRanges
import com.maxarcher.launcher.ui.drawer.resolveDrawerOrdinaryRestorationTarget
import com.maxarcher.launcher.ui.drawer.resolveDrawerRestorationTarget

@RunWith(AndroidJUnit4::class)
class DrawerSearchProjectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun latinMatchingIgnoresCaseAndDiacritics() {
        assertEquals(
            listOf(0..5),
            drawerSearchMatchRanges(label = "Résumé", query = "resume"),
        )
    }

    @Test
    fun latinMatchingUsesTheOrderingAsciiNormalization() {
        assertEquals(
            listOf(0..5),
            drawerSearchMatchRanges(label = "Smørrebrød", query = "smorre"),
        )
    }

    @Test
    fun hanMatchingUsesDisplayedCharacters() {
        assertEquals(
            listOf(1..2),
            drawerSearchMatchRanges(label = "微微信读书", query = "微信"),
        )
    }

    @Test
    fun latinQueryDoesNotUsePinyinForHanLabel() {
        assertTrue(drawerSearchMatchRanges(label = "微信", query = "weixin").isEmpty())
    }

    @Test
    fun matchingFindsEveryNonOverlappingDisplayedSpan() {
        assertEquals(
            listOf(0..2, 3..5),
            drawerSearchMatchRanges(label = "AppApp", query = "app"),
        )
    }

    @Test
    fun blankQueryHasNoEmphasisRange() {
        assertTrue(drawerSearchMatchRanges(label = "Calendar", query = "   ").isEmpty())
    }

    @Test
    fun searchCancellationRestoresTheOrdinarySettingsRow() {
        val sections = listOf(
            DrawerSection(
                label = "A",
                entries = listOf(entry(packageSuffix = "application", label = "Application")),
            ),
        )
        val position = captureDrawerOrdinaryListPosition(
            sections = sections,
            firstVisibleItemIndex = 3,
            firstVisibleItemScrollOffset = 7,
        )

        assertEquals(
            DrawerRestorationTarget(itemIndex = 3, scrollOffset = 7),
            resolveDrawerOrdinaryRestorationTarget(
                position = checkNotNull(value = position),
                sections = sections,
            ),
        )
    }

    @Test
    fun geometryChangePreservesTheFormerTopApplicationAndOffset() {
        val sections = listOf(
            DrawerSection(
                label = "A",
                entries = (1..6).map { index ->
                    entry(packageSuffix = "application$index", label = "Application $index")
                },
            ),
        )
        val position = captureDrawerListPosition(
            sections = sections,
            firstVisibleItemIndex = 4,
            firstVisibleItemScrollOffset = 9,
            itemsPerRow = 1,
        )

        assertEquals(
            DrawerRestorationTarget(itemIndex = 2, scrollOffset = 9),
            resolveDrawerRestorationTarget(
                position = checkNotNull(value = position),
                sections = sections,
                itemsPerRow = 3,
            ),
        )
    }

    @Test
    fun multiColumnProjectionKeepsSettingsAfterApplicationRows() {
        val sections = listOf(
            DrawerSection(
                label = "A",
                entries = (1..5).map { index ->
                    entry(packageSuffix = "application$index", label = "Application $index")
                },
            ),
        )
        val position = captureDrawerOrdinaryListPosition(
            sections = sections,
            firstVisibleItemIndex = 5,
            firstVisibleItemScrollOffset = 6,
            itemsPerRow = 2,
        )

        assertEquals(
            DrawerRestorationTarget(itemIndex = 5, scrollOffset = 6),
            resolveDrawerOrdinaryRestorationTarget(
                position = checkNotNull(value = position),
                sections = sections,
                itemsPerRow = 2,
            ),
        )
    }

    @Test
    fun inventoryChangeReappliesActiveQueryToLatestReliableInventory() {
        var entries = listOf(entry(packageSuffix = "before", label = "Before update"))
        var inventoryChanged: (LaunchableInventoryChange) -> Unit = {}
        val inventory = object : LaunchableInventoryLoader, LaunchableInventoryMonitor {
            override suspend fun load(): LaunchableInventorySnapshot = snapshot(entries = entries)

            override fun observe(
                onInventoryChanged: (LaunchableInventoryChange) -> Unit,
            ): LaunchableInventoryObservation {
                inventoryChanged = onInventoryChanged
                return LaunchableInventoryObservation { inventoryChanged = {} }
            }
        }
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(inventoryLoader = inventory)
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_search_field").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_search_input").performTextInput(text = "Target")
        composeRule.onNodeWithTag(testTag = "drawer_search_empty").assertIsDisplayed()

        composeRule.runOnIdle {
            entries = listOf(
                entry(packageSuffix = "target", label = "Target application"),
                entry(packageSuffix = "other", label = "Other application"),
            )
            inventoryChanged(LaunchableInventoryChange.PackageChanged)
        }

        composeRule.onNodeWithText(text = "Target application").assertIsDisplayed()
        composeRule.onNodeWithText(text = "Other application").assertDoesNotExist()
    }

    @Test
    fun clearStaysInSearchAndCancelRestoresOrdinaryDrawer() {
        val applications = listOf(
            entry(packageSuffix = "alpha", label = "Alpha application"),
            entry(packageSuffix = "beta", label = "Beta application"),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader {
                        snapshot(entries = applications)
                    },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "drawer_search_field").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_search_input").performTextInput(text = "Beta")
        composeRule.onNodeWithText(text = "Beta application").assertIsDisplayed()
        composeRule.onNodeWithText(text = "Alpha application").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "drawer_settings_entry").assertDoesNotExist()

        composeRule.onNodeWithTag(testTag = "drawer_search_clear").performClick()
        composeRule.onNodeWithText(text = "Alpha application").assertExists()
        composeRule.onNodeWithText(text = "Beta application").assertExists()
        composeRule.onNodeWithTag(testTag = "drawer_search_clear").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "drawer_settings_entry").assertDoesNotExist()

        composeRule.onNodeWithTag(testTag = "drawer_search_cancel").performClick()
        composeRule.onNodeWithTag(testTag = "drawer_search_input").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "drawer_back").assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = "drawer_settings_entry").assertExists()
    }

    @Test
    fun launchFailureRequestsOneNonBlockingInventoryRefresh() {
        val application = entry(packageSuffix = "failure", label = "Failure application")
        var loadCount = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader {
                        loadCount += 1
                        if (loadCount == 1) {
                            snapshot(entries = listOf(application))
                        } else {
                            error("Expected refresh failure")
                        }
                    },
                    entryLauncher = LaunchableEntryLauncher { false },
                )
            }
        }
        composeRule.waitUntil(condition = { loadCount == 1 })

        composeRule.onNodeWithText(text = "Failure application").performClick()

        composeRule.waitUntil(condition = { loadCount == 2 })
        composeRule.onNodeWithText(text = "Failure application").assertIsDisplayed()
    }

    private fun entry(packageSuffix: String, label: String): LaunchableEntry = LaunchableEntry(
        identity = LaunchableIdentity(
            profileSerialNumber = 0,
            componentName = ComponentName("com.example.$packageSuffix", "MainActivity"),
        ),
        user = Process.myUserHandle(),
        label = label,
        icon = ColorDrawable(Color.TRANSPARENT),
    )

    private fun snapshot(entries: List<LaunchableEntry>) = LaunchableInventorySnapshot(
        entries = entries,
        profileReadStatus = mapOf(0L to ProfileInventoryReadStatus.Complete),
    )
}
