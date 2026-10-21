package com.maxarchm.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeApplicationRemovalUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val files = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @Test
    fun everyItemArrangementHasOneTwentyDpRemoveTargetWithoutOrdinaryActivation() {
        val entry = entry()
        var removals = 0
        var activations = 0
        val editing = mutableStateOf(value = true)
        val mutationEnabled = mutableStateOf(value = true)
        composeRule.setContent(
            composable = {
                Launcher4MaxTheme(content = {
                    Column(
                        modifier = Modifier.width(width = 320.dp),
                        content = {
                            for (variant in 0..2) {
                                HomeOrderedModuleContent(
                                    module = OrderedFavoriteModule(
                                        id = "module-$variant",
                                        type = if (variant == 2) OrderedFavoriteModuleType.Ribbon else OrderedFavoriteModuleType.Vertical,
                                        identities = listOf(element = entry.identity),
                                        namePlacement = if (variant == 1) FavoriteNamePlacement.Below else FavoriteNamePlacement.Right,
                                    ),
                                    availabilityByIdentity = mapOf(entry.identity to FavoriteAvailability.Available(entry = entry)),
                                    ribbonListState = rememberLazyListState(),
                                    editMode = true,
                                    showAddEntry = false,
                                    addEntryEnabled = false,
                                    onAddToModule = {},
                                    onLaunchFavorite = { activations += 1 },
                                    onLongPressFavorite = { activations += 1 },
                                    applicationEditing = editing.value,
                                    applicationMutationEnabled = mutationEnabled.value,
                                    onRemoveFavorite = { removals += 1 },
                                )
                            }
                        },
                    )
                })
            },
        )
        val controls = composeRule.onAllNodesWithTag(testTag = "remove_favorite_item")
        controls.assertCountEquals(expectedSize = 3)
        for (index in 0..2) {
            controls[index].assertWidthIsEqualTo(expectedWidth = 20.dp)
            controls[index].assertHeightIsEqualTo(expectedHeight = 20.dp)
            controls[index].performClick()
        }
        composeRule.onNodeWithTag(testTag = "home_favorite_below_item")
            .performTouchInput(block = { click(position = center) })
            .performTouchInput(block = { longClick(position = center) })
        composeRule.runOnIdle(
            action = {
                assertEquals(3, removals)
                assertEquals(0, activations)
                mutationEnabled.value = false
            },
        )
        controls[0].assertIsNotEnabled()
        composeRule.runOnIdle(action = { editing.value = false })
        controls.assertCountEquals(expectedSize = 0)
    }

    @Test
    fun normalHomeActionSheetRemovalOffersUndoAndRestoresTheFinalModule() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entry = entry()
        val module = OrderedFavoriteModule(
            id = "single-ribbon",
            type = OrderedFavoriteModuleType.Ribbon,
            identities = listOf(element = entry.identity),
        )
        val file = files.newFolder().resolve(relative = "favorites.bin")
        val store = OrderedFavoriteModuleStore(file = file)
        runBlocking(
            block = {
                store.load()
                assertTrue(store.replaceAggregate(aggregate = OrderedFavoriteAggregate(modules = listOf(element = module))))
            },
        )
        val adapter = OrderedFavoriteStoreAdapter(file = file)
        composeRule.setContent(
            composable = {
                Launcher4MaxTheme(content = {
                    Launcher4MaxApp(
                        inventoryLoader = LaunchableInventoryLoader {
                            LaunchableInventorySnapshot(
                                entries = listOf(element = entry),
                                profileReadStatus = mapOf(entry.identity.profileSerialNumber to ProfileInventoryReadStatus.Complete),
                            )
                        },
                        favoriteStore = adapter,
                    )
                })
            },
        )
        composeRule.waitUntil(
            timeoutMillis = 5_000,
            condition = {
                composeRule.onAllNodesWithText(text = entry.label).fetchSemanticsNodes().isNotEmpty()
            },
        )
        composeRule.onNodeWithText(text = entry.label).performTouchInput(block = { longClick(position = center) })
        composeRule.onNodeWithTag(testTag = "favorite_action").performClick()
        composeRule.waitUntil(
            timeoutMillis = 5_000,
            condition = {
                composeRule.onAllNodesWithText(text = context.getString(R.string.favorite_removed))
                    .fetchSemanticsNodes().isNotEmpty()
            },
        )
        composeRule.onNodeWithText(text = context.getString(R.string.favorite_removed)).assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = "home_favorite_row").assertDoesNotExist()
        composeRule.onNodeWithText(text = context.getString(R.string.undo)).performClick()
        composeRule.waitUntil(
            timeoutMillis = 5_000,
            condition = {
                composeRule.onAllNodesWithText(text = entry.label).fetchSemanticsNodes().isNotEmpty()
            },
        )
        composeRule.onNodeWithText(text = entry.label).assertIsDisplayed()
        composeRule.runOnIdle(
            action = {
                assertEquals(listOf(element = module), (adapter.state.value as FavoriteReadState.Readable).orderedModules)
            },
        )
    }

    @Test
    fun removingTheFinalFavoriteBarItemDeletesItsContainerAndOffersUndo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entry = entry()
        val module = OrderedFavoriteModule(
            id = "edit-single-ribbon",
            type = OrderedFavoriteModuleType.Ribbon,
            identities = listOf(entry.identity),
        )
        val initial = OrderedFavoriteAggregate(modules = listOf(module))
        val file = files.newFolder().resolve("favorites.bin")
        val store = OrderedFavoriteModuleStore(file = file)
        runBlocking {
            store.load()
            assertTrue(store.replaceAggregate(initial))
        }
        val adapter = OrderedFavoriteStoreAdapter(file = file)
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader {
                        LaunchableInventorySnapshot(
                            entries = listOf(entry),
                            profileReadStatus = mapOf(
                                entry.identity.profileSerialNumber to
                                    ProfileInventoryReadStatus.Complete,
                            ),
                        )
                    },
                    favoriteStore = adapter,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(entry.label).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(entry.label).performTouchInput { longClick(center) }
        composeRule.onNodeWithTag("edit_favorites_action").performClick()
        composeRule.onNodeWithTag("remove_favorite_item").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            (adapter.state.value as? FavoriteReadState.Readable)?.orderedModules?.isEmpty() == true
        }
        composeRule.onNodeWithText(context.getString(R.string.favorite_removed)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("home_favorite_row").assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.undo)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            (adapter.state.value as? FavoriteReadState.Readable)?.orderedAggregate == initial
        }
        composeRule.onNodeWithText(entry.label).assertIsDisplayed()
        composeRule.onNodeWithTag("remove_favorite_item").assertIsDisplayed()
    }

    private fun entry(): LaunchableEntry = LaunchableEntry(
        identity = LaunchableIdentity(
            profileSerialNumber = 1,
            componentName = ComponentName("com.example", "Main"),
        ),
        user = Process.myUserHandle(),
        label = "Removal test application",
        icon = ColorDrawable(Color.TRANSPARENT),
    )
}
