package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxarcher.launcher.ui.drawer.DrawerListPosition
import com.maxarcher.launcher.ui.drawer.DrawerRestorationTarget
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.maxarcher.launcher.ui.home.components.stableKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.maxarcher.launcher.ui.settings.EmptySettingsPlatform
import com.maxarcher.launcher.ui.settings.SettingsPage
import com.maxarcher.launcher.ui.settings.SettingsPlatform
import com.maxarcher.launcher.ui.settings.SettingsScreen
import com.maxarcher.launcher.ui.drawer.DrawerScreen
import com.maxarcher.launcher.ui.drawer.DrawerSection
import com.maxarcher.launcher.ui.drawer.LaunchableEntryComparator
import com.maxarcher.launcher.ui.drawer.buildDrawerSections
import com.maxarcher.launcher.ui.drawer.captureDrawerListPosition
import com.maxarcher.launcher.ui.drawer.resolveDrawerRestorationTarget

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeDisplaysCurrentInformationRegion() {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen()
            }
        }

        composeRule.onNodeWithTag("home_time").assertIsDisplayed()
        composeRule.onNodeWithTag("home_date").assertIsDisplayed()
    }

    @Test
    fun unavailableFavoriteRemainsVisibleInStoredPosition() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.unavailable", "MainActivity"),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(listOf(identity)),
                    favoriteAvailability = mapOf(
                        identity to FavoriteAvailability.Unknown(null),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("home_favorite_row").assertIsDisplayed()
        composeRule.onNodeWithText("Application unavailable").assertIsDisplayed()
    }

    @Test
    fun orderedVerticalModuleDisplaysAndLaunchesItsExactEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.ordered", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Ordered module application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val module = OrderedFavoriteModule(
            id = "vertical-list-1",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(entry.identity),
        )
        var selectedAvailability: FavoriteAvailability? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = module.id,
                                    type = FavoriteContainerType.VerticalList,
                                    identities = module.identities,
                                ),
                            ),
                        ),
                        orderedModules = listOf(module),
                    ),
                    favoriteAvailability = mapOf(
                        entry.identity to FavoriteAvailability.Available(entry),
                    ),
                    onLaunchFavorite = { selectedAvailability = it },
                )
            }
        }

        composeRule.onNodeWithTag("home_ordered_favorite_modules").assertIsDisplayed()
        composeRule.onNodeWithText("Ordered module application").performClick()
        composeRule.runOnIdle {
            assertEquals(FavoriteAvailability.Available(entry), selectedAvailability)
        }
    }

    @Test
    fun emptyOrderedEditModeOffersBothMainListModuleEntries() {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(),
                        orderedModules = emptyList(),
                    ),
                    editMode = true,
                    stylePanelExpanded = true,
                )
            }
        }

        composeRule.onNodeWithTag("home_add_favorite_list").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_favorite_ribbon").assertIsDisplayed()
        composeRule.onNodeWithText("Select a favorite list to edit its style").assertIsDisplayed()
    }

    @Test
    fun expandedPanelSelectsACompleteOrderedModuleAndShowsItsStyleControls() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.selected", "MainActivity"),
        )
        val module = OrderedFavoriteModule(
            id = "vertical-list-1",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(identity),
        )
        val selectedModule = mutableStateOf<String?>(null)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = module.id,
                                    type = FavoriteContainerType.VerticalList,
                                    identities = module.identities,
                                ),
                            ),
                        ),
                        orderedModules = listOf(module),
                    ),
                    editMode = true,
                    stylePanelExpanded = true,
                    selectedModuleId = selectedModule.value,
                    onSelectModule = { selectedModule.value = it },
                )
            }
        }

        composeRule.onNodeWithTag("home_module_selectable").performClick()
        composeRule.onNodeWithTag("home_module_selected").assertIsDisplayed()
        composeRule.onNodeWithText("Application size · Text size").assertIsDisplayed()
        composeRule.onAllNodesWithText("Vertical module").assertCountEquals(0)
    }

    @Test
    fun verticalModuleStyleChangePreviewsAndPersistsThroughOneAggregateSave() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.style", "MainActivity"),
        )
        val module = OrderedFavoriteModule(
            id = "vertical-list-1",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(identity),
        )
        var persisted = OrderedFavoriteAggregate(modules = listOf(module))
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = persisted.toLegacyAggregate(),
                        orderedModules = listOf(module),
                        orderedAggregate = persisted,
                    ),
                    editMode = true,
                    stylePanelExpanded = true,
                    selectedModuleId = module.id,
                    onCommitFavoriteComposition = { transform ->
                        transform(persisted).also { persisted = it }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Below").performClick()
        composeRule.onNodeWithTag("home_favorite_below_item").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_favorite_list")
            .performScrollTo()
            .assertIsEnabled()
        composeRule.runOnIdle {
            assertEquals(
                FavoriteNamePlacement.Below,
                persisted.modules.single().namePlacement,
            )
        }
    }

    @Test
    fun selectedOrderedModuleCanOpenItsExistingAddDestination() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.add", "MainActivity"),
        )
        val module = OrderedFavoriteModule(
            id = "vertical-list-1",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(identity),
        )
        var openedModuleId: String? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = module.id,
                                    type = FavoriteContainerType.VerticalList,
                                    identities = module.identities,
                                ),
                            ),
                        ),
                        orderedModules = listOf(module),
                    ),
                    editMode = true,
                    stylePanelExpanded = false,
                    selectedModuleId = module.id,
                    onAddFavoritesToList = { openedModuleId = it },
                )
            }
        }

        composeRule.onNodeWithTag("home_add_favorite_${module.id}").performClick()
        composeRule.runOnIdle { assertEquals(module.id, openedModuleId) }
    }

    @Test
    fun oneVerticalListUsesOneFullWidthComposition() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.single", "MainActivity"),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = "vertical-list-custom-1",
                                    type = FavoriteContainerType.VerticalList,
                                    identities = listOf(identity),
                                ),
                            ),
                        ),
                        orderedModules = listOf(
                            OrderedFavoriteModule(
                                id = "vertical-list-custom-1",
                                type = OrderedFavoriteModuleType.Vertical,
                                identities = listOf(identity),
                            ),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "home_favorite_item:${identity.stableKey()}")
            .assertIsDisplayed()
    }

    @Test
    fun twoVerticalListsUseTwoIndependentCompositionSlots() {
        val first = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.first", "MainActivity"),
        )
        val second = LaunchableIdentity(
            profileSerialNumber = 43,
            componentName = ComponentName("com.example.second", "MainActivity"),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = "vertical-list-custom-1",
                                    type = FavoriteContainerType.VerticalList,
                                    identities = listOf(first),
                                    listSize = FavoriteListSize.Large,
                                ),
                                FavoriteContainer(
                                    id = "vertical-list-custom-2",
                                    type = FavoriteContainerType.VerticalList,
                                    identities = listOf(second),
                                    listSize = FavoriteListSize.Small,
                                ),
                            ),
                        ),
                        orderedModules = listOf(
                            OrderedFavoriteModule(
                                id = "vertical-list-custom-1",
                                type = OrderedFavoriteModuleType.Vertical,
                                identities = listOf(first),
                            ),
                            OrderedFavoriteModule(
                                id = "vertical-list-custom-2",
                                type = OrderedFavoriteModuleType.Vertical,
                                identities = listOf(second),
                            ),
                        ),
                    ),
                )
            }
        }

        val firstNode = composeRule.onNodeWithTag(
            testTag = "home_favorite_item:${first.stableKey()}",
        ).assertIsDisplayed().fetchSemanticsNode()
        val secondNode = composeRule.onNodeWithTag(
            testTag = "home_favorite_item:${second.stableKey()}",
        ).assertIsDisplayed().fetchSemanticsNode()
        assertTrue(secondNode.boundsInRoot.top >= firstNode.boundsInRoot.bottom)
    }

    @Test
    fun favoriteBarDisplaysAndLaunchesItsExactEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.bar", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Favorite bar application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var selectedAvailability: FavoriteAvailability? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            favoriteBars = listOf(
                                FavoriteContainer(
                                    id = "favorite-bar-1",
                                    type = FavoriteContainerType.FavoriteBar,
                                    identities = listOf(entry.identity),
                                ),
                            ),
                        ),
                        orderedModules = listOf(
                            OrderedFavoriteModule(
                                id = "favorite-bar-1",
                                type = OrderedFavoriteModuleType.Ribbon,
                                identities = listOf(entry.identity),
                            ),
                        ),
                    ),
                    favoriteAvailability = mapOf(
                        entry.identity to FavoriteAvailability.Available(entry),
                    ),
                    onLaunchFavorite = { selectedAvailability = it },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "home_favorite_item:${entry.identity.stableKey()}")
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(FavoriteAvailability.Available(entry), selectedAvailability)
        }
        composeRule.onAllNodesWithTag("home_favorites_empty").assertCountEquals(0)
    }

    @Test
    fun editModeRibbonAddEntryKeepsItsPersistedDestination() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.bar", "MainActivity"),
        )
        var existingTarget: String? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        FavoriteAggregate(
                            favoriteBars = listOf(
                                FavoriteContainer(
                                    id = "favorite-bar-1",
                                    type = FavoriteContainerType.FavoriteBar,
                                    identities = listOf(identity),
                                ),
                            ),
                        ),
                        orderedModules = listOf(
                            OrderedFavoriteModule(
                                id = "favorite-bar-1",
                                type = OrderedFavoriteModuleType.Ribbon,
                                identities = listOf(identity),
                            ),
                        ),
                    ),
                    editMode = true,
                    onAddFavoritesToBar = { existingTarget = it },
                )
            }
        }

        composeRule.onNodeWithTag("home_add_favorite_favorite-bar-1")
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("favorite-bar-1", existingTarget) }
    }

    @Test
    fun editModeKeepsThePersistedListComposition() {
        val identity = LaunchableIdentity(
            profileSerialNumber = 42,
            componentName = ComponentName("com.example.edit", "MainActivity"),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        FavoriteAggregate(
                            verticalLists = listOf(
                                FavoriteContainer(
                                    id = "vertical-list-edit-custom",
                                    type = FavoriteContainerType.VerticalList,
                                    identities = listOf(identity),
                                    listSize = FavoriteListSize.Large,
                                ),
                            ),
                        ),
                        orderedModules = listOf(
                            OrderedFavoriteModule(
                                id = "vertical-list-edit-custom",
                                type = OrderedFavoriteModuleType.Vertical,
                                identities = listOf(identity),
                                iconSize = FavoriteListSize.Large,
                            ),
                        ),
                    ),
                    editMode = true,
                )
            }
        }

        composeRule.onNodeWithTag("home_add_favorite_vertical-list-edit-custom").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home_favorite_row").assertCountEquals(1)
        composeRule.onAllNodesWithTag("remove_favorite_item").assertCountEquals(1)
    }

    @Test
    fun failedEditMutationRetainsNewerReliableInventoryState() {
        val removedEntry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.removed", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Removed during failed save",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val refreshedEntry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.refreshed", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Reliable refreshed application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val initial = OrderedFavoriteAggregate(
            modules = listOf(
                OrderedFavoriteModule(
                    id = "inventory-interruption-list",
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(removedEntry.identity),
                ),
            ),
        )
        val refreshed = initial.copy(
            modules = listOf(
                initial.modules.single().copy(
                    identities = listOf(refreshedEntry.identity),
                ),
            ),
        )
        val favoriteState = mutableStateOf<FavoriteReadState>(
            FavoriteReadState.Readable(
                aggregate = initial.toLegacyAggregate(),
                orderedModules = initial.modules,
                orderedAggregate = initial,
            ),
        )
        var saveAttempts = 0

        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = favoriteState.value,
                    favoriteAvailability = mapOf(
                        removedEntry.identity to FavoriteAvailability.Available(removedEntry),
                        refreshedEntry.identity to FavoriteAvailability.Available(refreshedEntry),
                    ),
                    editMode = true,
                    stylePanelExpanded = true,
                    selectedModuleId = "inventory-interruption-list",
                    onCommitFavoriteComposition = {
                        saveAttempts += 1
                        favoriteState.value = FavoriteReadState.Readable(
                            aggregate = refreshed.toLegacyAggregate(),
                            orderedModules = refreshed.modules,
                            orderedAggregate = refreshed,
                        )
                        null
                    },
                )
            }
        }

        composeRule.onNodeWithText("Below").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(1, saveAttempts) }
        composeRule.onNodeWithText("Reliable refreshed application").performScrollTo()
        composeRule.onNodeWithText("Reliable refreshed application").assertIsDisplayed()
        composeRule.onAllNodesWithText("Removed during failed save").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home_favorite_below_item").assertCountEquals(0)
    }

    @Test
    fun selectingAvailableFavoriteRequestsItsExactEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.favorite", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Favorite application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var selectedAvailability: FavoriteAvailability? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    favoriteAvailability = mapOf(
                        entry.identity to FavoriteAvailability.Available(entry),
                    ),
                    onLaunchFavorite = { selectedAvailability = it },
                )
            }
        }

        composeRule.onNodeWithText("Favorite application").performClick()

        composeRule.runOnIdle {
            assertEquals(FavoriteAvailability.Available(entry), selectedAvailability)
        }
    }

    @Test
    fun failedHomeLaunchDoesNotDeleteFavorite() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.failure", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Launch failure favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val favoriteStore = TestFavoriteStore(listOf(entry.identity))
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    entryLauncher = LaunchableEntryLauncher { false },
                    favoriteStore = favoriteStore,
                )
            }
        }

        composeRule.onNodeWithText("Launch failure favorite").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(entry.identity), favoriteStore.readableIdentities())
        }
    }

    @Test
    fun disabledFavoriteKeepsPresentationAndManagementEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example.disabled", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Disabled application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var selectedAvailability: FavoriteAvailability? = null
        var managedEntry: LaunchableEntry? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    favoriteAvailability = mapOf(
                        entry.identity to FavoriteAvailability.Disabled(entry),
                    ),
                    onLaunchFavorite = { selectedAvailability = it },
                    onLongPressFavorite = { managedEntry = it },
                )
            }
        }

        composeRule.onNodeWithText("Disabled application — Disabled").performClick()
        composeRule.onNodeWithText("Disabled application — Disabled").performTouchInput {
            longClick()
        }

        composeRule.runOnIdle {
            assertEquals(FavoriteAvailability.Disabled(entry), selectedAvailability)
            assertEquals(entry, managedEntry)
        }
    }

    @Test
    fun incompleteProfileAndUnknownIdentityAreNeverConfirmedRemoved() = runBlocking {
        val identity = LaunchableIdentity(7, ComponentName("com.example", "MainActivity"))
        val loader = object : LaunchableInventoryLoader, LaunchableIdentityStatusResolver {
            override suspend fun load() = LaunchableInventorySnapshot(
                entries = emptyList(),
                profileReadStatus = mapOf(7L to ProfileInventoryReadStatus.Unavailable),
            )

            override suspend fun resolveMissingIdentity(
                identity: LaunchableIdentity,
                snapshot: LaunchableInventorySnapshot,
                lastKnownEntry: LaunchableEntry?,
            ) = FavoriteAvailability.TemporarilyUnavailable(lastKnownEntry)
        }
        val snapshot = loader.load()

        val result = LaunchableInventoryCoordinator(loader).resolveFavorites(listOf(identity), snapshot)

        assertEquals(
            FavoriteAvailability.TemporarilyUnavailable(null),
            result.getValue(identity),
        )
    }

    @Test
    fun reconciliationRemovesOnlyConfirmedExactIdentity() = runBlocking {
        val removed = LaunchableIdentity(7, ComponentName("com.example", "RemovedActivity"))
        val retained = LaunchableIdentity(7, ComponentName("com.example", "OtherActivity"))
        val clone = LaunchableIdentity(8, ComponentName("com.example", "RemovedActivity"))
        val loader = object : LaunchableInventoryLoader, LaunchableIdentityStatusResolver {
            override suspend fun load() = LaunchableInventorySnapshot(
                entries = emptyList(),
                profileReadStatus = mapOf(
                    7L to ProfileInventoryReadStatus.Complete,
                    8L to ProfileInventoryReadStatus.Unavailable,
                ),
            )

            override suspend fun resolveMissingIdentity(
                identity: LaunchableIdentity,
                snapshot: LaunchableInventorySnapshot,
                lastKnownEntry: LaunchableEntry?,
            ): FavoriteAvailability = when (identity) {
                removed -> FavoriteAvailability.ConfirmedRemoved
                else -> FavoriteAvailability.Unknown(lastKnownEntry)
            }
        }
        val snapshot = loader.load()

        val result = LaunchableInventoryCoordinator(loader)
            .resolveFavorites(listOf(removed, retained, clone), snapshot)

        assertEquals(FavoriteAvailability.ConfirmedRemoved, result.getValue(removed))
        assertEquals(FavoriteAvailability.Unknown(null), result.getValue(retained))
        assertEquals(FavoriteAvailability.Unknown(null), result.getValue(clone))
    }

    @Test
    fun actionSheetOffersAddForNonFavoriteEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.add", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Add candidate",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var addRequested = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = {},
                    onAddFavorite = { addRequested = true },
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithText("Add favorite").performClick()

        composeRule.runOnIdle { assertEquals(true, addRequested) }
    }

    @Test
    fun actionSheetDoesNotOfferDuplicateAddForFavoriteEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.existing", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Existing favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var removeRequested = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = { removeRequested = true },
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithText("Remove favorite").assertIsDisplayed()
        composeRule.onAllNodesWithText("Add favorite").assertCountEquals(0)
        composeRule.onNodeWithText("Remove favorite").performClick()
        composeRule.runOnIdle { assertEquals(true, removeRequested) }
    }

    @Test
    fun actionSheetDisablesFavoriteMutationWhenPersistenceIsUnavailable() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.failure", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Unavailable favorites",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.ReadFailure,
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithTag("favorite_action").assertIsNotEnabled()
        composeRule.onNodeWithText("Favorites unavailable").assertIsDisplayed()
    }

    @Test
    fun actionSheetInformationActionUsesExactEntryAndDismisses() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 7,
                componentName = ComponentName("com.example.details", "ExactActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Application details",
            icon = ColorDrawable(Color.TRANSPARENT),
            profileBadge = ColorDrawable(Color.WHITE),
        )
        var openedEntry: LaunchableEntry? = null
        var informationOpened = false
        var dismissed = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = { dismissed = true },
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher {
                        openedEntry = it
                        true
                    },
                    onInformationOpened = { informationOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag("application_action_sheet_profile_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("application_information_action").performClick()

        composeRule.runOnIdle {
            assertEquals(entry, openedEntry)
            assertEquals(true, informationOpened)
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun failedInformationLaunchDismissesWithoutSchedulingReturnRefresh() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.missing.details", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Missing application details",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var informationOpened = false
        var dismissed = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Drawer,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = { dismissed = true },
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { false },
                    onInformationOpened = { informationOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "application_information_action").performClick()

        composeRule.runOnIdle {
            assertEquals(false, informationOpened)
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun uninstallAvailabilityExcludesSystemProfilesAndMissingHandlers() {
        assertEquals(
            true,
            isOrdinaryUninstallAvailable(
                isCurrentUser = true,
                applicationFlags = 0,
                hasSystemHandler = true,
            ),
        )
        assertEquals(
            false,
            isOrdinaryUninstallAvailable(
                isCurrentUser = true,
                applicationFlags = ApplicationInfo.FLAG_SYSTEM,
                hasSystemHandler = true,
            ),
        )
        assertEquals(
            false,
            isOrdinaryUninstallAvailable(
                isCurrentUser = true,
                applicationFlags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                hasSystemHandler = true,
            ),
        )
        assertEquals(
            false,
            isOrdinaryUninstallAvailable(
                isCurrentUser = false,
                applicationFlags = 0,
                hasSystemHandler = true,
            ),
        )
        assertEquals(
            false,
            isOrdinaryUninstallAvailable(
                isCurrentUser = true,
                applicationFlags = 0,
                hasSystemHandler = false,
            ),
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun uninstallIntentTargetsTheSystemPackageUninstaller() {
        val intent = createApplicationUninstallIntent(packageName = "com.example.uninstall")

        assertEquals(Intent.ACTION_UNINSTALL_PACKAGE, intent.action)
        assertEquals("package:com.example.uninstall", intent.data.toString())
    }

    @Test
    fun homeUninstallActionDismissesAndSchedulesReturnRefresh() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.uninstall", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Uninstall application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var uninstallRequested = false
        var uninstallOpened = false
        var dismissed = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    onDismiss = { dismissed = true },
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                    uninstallAvailable = true,
                    onUninstall = {
                        uninstallRequested = true
                        true
                    },
                    onUninstallOpened = { uninstallOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "uninstall_application_action").performClick()

        composeRule.runOnIdle {
            assertEquals(true, uninstallRequested)
            assertEquals(true, uninstallOpened)
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun failedUninstallLaunchDismissesWithoutSchedulingReturnRefresh() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.failed.uninstall", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Failed uninstall",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var uninstallOpened = false
        var dismissed = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    onDismiss = { dismissed = true },
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                    uninstallAvailable = true,
                    onUninstall = { false },
                    onUninstallOpened = { uninstallOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag(testTag = "uninstall_application_action").performClick()

        composeRule.runOnIdle {
            assertEquals(false, uninstallOpened)
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun upwardSwipeOpensDrawerLoadingSurface() {
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { awaitCancellation() },
                )
            }
        }

        composeRule.onNodeWithTag("home_surface").performTouchInput {
            swipeUp()
        }

        composeRule.onNodeWithTag("drawer_loading").assertIsDisplayed()
    }

    @Test
    fun upwardSwipeOverDateOpensDrawerWithoutActivatingDate() {
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { awaitCancellation() },
                )
            }
        }

        composeRule.onNodeWithTag("home_date").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - HOME_DRAWER_SWIPE_DISTANCE_PX)
        }

        composeRule.onNodeWithTag("drawer_loading").assertIsDisplayed()
    }

    @Test
    fun editModeKeepsHomeDrawerGestureDisabled() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.edit", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Editable favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val companion = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.edit.companion", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Companion favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader {
                        completeSnapshot(entry, companion)
                    },
                    favoriteStore = TestFavoriteStore(
                        listOf(entry.identity, companion.identity),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Editable favorite").performTouchInput { longClick() }
        composeRule.onNodeWithTag("edit_favorites_action").performClick()
        composeRule.onNodeWithTag("home_surface").performTouchInput { swipeUp() }

        composeRule.onAllNodesWithTag("drawer_surface").assertCountEquals(0)
    }

    @Test
    fun downwardDrawerSwipeDoesNotLaunchApplication() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.drawer", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Drawer swipe target",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var launches = 0
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    entryLauncher = LaunchableEntryLauncher {
                        launches += 1
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithTag("home_surface").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Drawer swipe target").performTouchInput { swipeDown() }

        composeRule.runOnIdle { assertEquals(0, launches) }
        composeRule.onNodeWithTag("home_time").assertIsDisplayed()
    }

    @Test
    fun drawerTransitionUsesContractedProgressAndDisplacement() {
        assertEquals(0.5f, drawerGestureProgress(100f, 200f), 0.001f)
        assertEquals(150f, drawerInteractiveDisplacement(100f), 0.001f)
    }

    @Test
    fun drawerTransitionReleaseUsesDistanceAndFlingBoundaries() {
        assertEquals(
            AppSurface.Home,
            transitionTarget(AppSurface.Home, 119f, 120f, 0f, 1_000f),
        )
        assertEquals(
            AppSurface.Drawer,
            transitionTarget(AppSurface.Home, 120f, 120f, 0f, 1_000f),
        )
        assertEquals(
            AppSurface.Drawer,
            transitionTarget(AppSurface.Home, 80f, 120f, 1_001f, 1_000f),
        )
        assertEquals(
            AppSurface.Home,
            transitionTarget(AppSurface.Home, 80f, 120f, -1_001f, 1_000f),
        )
        assertEquals(
            AppSurface.Home,
            transitionTarget(AppSurface.Drawer, 120f, 120f, 0f, 1_000f),
        )
        assertEquals(
            AppSurface.Drawer,
            transitionTarget(AppSurface.Drawer, 80f, 120f, -1_001f, 1_000f),
        )
    }

    @Test
    fun firstDrawerEntryDoesNotRepeatTheApplicationOwnedInitialLoad() {
        var loadCount = 0
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example", "com.example.MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Example application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader {
                        loadCount += 1
                        completeSnapshot(entry)
                    },
                )
            }
        }

        composeRule.waitUntil { loadCount == 1 }
        composeRule.onNodeWithTag("home_surface").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Example application").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, loadCount) }
    }

    @Test
    fun loadedInventoryDisplaysApplicationList() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example", "com.example.MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Example application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                )
            }
        }

        composeRule.onNodeWithTag("drawer_application_list").assertIsDisplayed()
        composeRule.onNodeWithText("Example application").assertIsDisplayed()
    }

    @Test
    fun selectingApplicationLaunchesItsExactEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 42,
                componentName = ComponentName("com.example", "com.example.ExactActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Exact application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var launchedEntry: LaunchableEntry? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    entryLauncher = LaunchableEntryLauncher {
                        launchedEntry = it
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithText("Exact application").performClick()

        composeRule.runOnIdle { assertEquals(entry, launchedEntry) }
    }

    @Test
    fun retryRecoversFromInventoryFailure() {
        var attempts = 0
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example", "com.example.MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Recovered application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader {
                        attempts += 1
                        if (attempts == 1) error("Expected test failure")
                        completeSnapshot(entry)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("drawer_error_icon").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        composeRule.onNodeWithText("Recovered application").assertIsDisplayed()
    }

    @Test
    fun activeDrawerRefreshesAfterPlatformInventoryChange() {
        fun entry(label: String) = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.live", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = label,
            icon = ColorDrawable(Color.TRANSPARENT),
        )

        var entries = listOf(entry("Before update"))
        var inventoryChanged: (LaunchableInventoryChange) -> Unit = {}
        val inventory = object : LaunchableInventoryLoader, LaunchableInventoryMonitor {
            override suspend fun load(): LaunchableInventorySnapshot =
                completeSnapshot(*entries.toTypedArray())

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
        composeRule.onNodeWithText("Before update").assertIsDisplayed()

        composeRule.runOnIdle {
            entries = listOf(entry("After update"))
            inventoryChanged(LaunchableInventoryChange.PackageChanged)
        }

        composeRule.onNodeWithText("After update").assertIsDisplayed()
    }

    @Test
    fun chineseLabelsAreSortedByPinyinAlongsideLatinLabels() {
        val labels = listOf("微信", "Chrome", "百度", "Amazon", "爱奇艺")
        val entries = labels.mapIndexed { index, label ->
            LaunchableEntry(
                identity = LaunchableIdentity(
                    profileSerialNumber = 0,
                    componentName = ComponentName("com.example.$index", "MainActivity"),
                ),
                user = Process.myUserHandle(),
                label = label,
                icon = ColorDrawable(Color.TRANSPARENT),
            )
        }

        val sortedLabels = entries
            .sortedWith(LaunchableEntryComparator(Locale.SIMPLIFIED_CHINESE))
            .map(LaunchableEntry::label)

        assertEquals(listOf("爱奇艺", "Amazon", "百度", "Chrome", "微信"), sortedLabels)
    }

    @Test
    fun drawerSectionsUseTheNormalizedCompleteLabel() {
        val labels = listOf("微信", "2FAS", "Éclair", "百度", "Amazon")
        val entries = labels.mapIndexed { index, label ->
            LaunchableEntry(
                identity = LaunchableIdentity(
                    profileSerialNumber = 0,
                    componentName = ComponentName("com.example.section.$index", "MainActivity"),
                ),
                user = Process.myUserHandle(),
                label = label,
                icon = ColorDrawable(Color.TRANSPARENT),
            )
        }

        val sections = buildDrawerSections(entries, Locale.SIMPLIFIED_CHINESE)

        assertEquals(listOf("#", "A", "B", "E", "W"), sections.map(DrawerSection::label))
        assertEquals(listOf("2FAS"), sections.first().entries.map(LaunchableEntry::label))
    }

    @Test
    fun drawerSectionsOmitTheNumericSectionWhenNoEntryFallsIntoIt() {
        val labels = listOf("Amazon", "百度", "Chrome")
        val entries = labels.mapIndexed { index, label ->
            LaunchableEntry(
                identity = LaunchableIdentity(
                    profileSerialNumber = 0,
                    componentName = ComponentName("com.example.numeric.$index", "MainActivity"),
                ),
                user = Process.myUserHandle(),
                label = label,
                icon = ColorDrawable(Color.TRANSPARENT),
            )
        }

        val sections = buildDrawerSections(entries, Locale.SIMPLIFIED_CHINESE)

        assertEquals(listOf("A", "B", "C"), sections.map(DrawerSection::label))
    }

    @Test
    fun drawerUsesSectionsPreparedByTheBackgroundInventorySnapshot() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example", "com.example.MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Example application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val preparedSections = listOf(DrawerSection("E", listOf(entry)))
        val snapshot = LaunchableInventorySnapshot(
            entries = listOf(entry),
            profileReadStatus = mapOf(0L to ProfileInventoryReadStatus.Complete),
            drawerSections = preparedSections,
            drawerSectionsLocale = Locale.US,
        )

        assertEquals(preparedSections, snapshot.drawerSectionsFor(Locale.US))
    }

    @Test
    fun liveUpdatePreservesAnchorRelativePosition() {
        val sections = listOf(
            DrawerSection(label = "#", entries = emptyList()),
            DrawerSection(label = "A", entries = emptyList()),
            DrawerSection(label = "B", entries = emptyList()),
        )
        val position = captureDrawerListPosition(
            sections = sections,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 12,
        )

        assertEquals(
            DrawerRestorationTarget(itemIndex = 1, scrollOffset = 12),
            resolveDrawerRestorationTarget(checkNotNull(position), sections),
        )
    }

    @Test
    fun removedAnchorMovesToNextAvailableAnchor() {
        val position = DrawerListPosition(
            sectionLabel = "B",
            relativeItemIndex = 0,
            scrollOffset = 20,
        )
        val updatedSections = listOf(
            DrawerSection(label = "#", entries = emptyList()),
            DrawerSection(label = "C", entries = emptyList()),
        )

        assertEquals(
            DrawerRestorationTarget(itemIndex = 1, scrollOffset = 0),
            resolveDrawerRestorationTarget(position, updatedSections),
        )
    }

    @Test
    fun rapidActivationGuardSuppressesImmediateDuplicate() {
        var now = 1_000L
        val guard = RapidActivationGuard(
            minimumIntervalMillis = 600L,
            elapsedRealtime = { now },
        )

        assertEquals(true, guard.tryAcquire())
        assertEquals(false, guard.tryAcquire())
        now += 600L
        assertEquals(true, guard.tryAcquire())
    }

    @Test
    fun homeFavoriteActionSheetOffersEditForTwoOrMoreFavorites() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.first", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "First favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val second = LaunchableIdentity(
            profileSerialNumber = 0,
            componentName = ComponentName("com.example.second", "MainActivity"),
        )
        var editRequested = false
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity, second)),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    onEditFavorites = { editRequested = true },
                    canEditFavorites = true,
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithTag("edit_favorites_action").performClick()
        composeRule.runOnIdle { assertEquals(true, editRequested) }
    }

    @Test
    fun homeFavoriteActionSheetOffersEditForOneFavorite() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.only", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Only favorite",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    onEditFavorites = {},
                    canEditFavorites = true,
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithTag("edit_favorites_action").assertIsDisplayed()
    }

    @Test
    fun actionSheetShowsAndInvokesExactApplicationShortcut() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 7,
                componentName = ComponentName("com.example.shortcuts", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Shortcut application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val shortcut = ApplicationShortcut(
            packageName = "com.example.shortcuts",
            shortcutId = "exact-shortcut",
            label = "Exact shortcut",
            icon = null,
            user = entry.user,
            rank = 2,
        )
        var invoked: ApplicationShortcut? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    shortcuts = listOf(shortcut),
                    onShortcut = { invoked = it },
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onNodeWithTag("application_shortcut_region").assertIsDisplayed()
        composeRule.onNodeWithTag("application_shortcut_trailing_divider").assertIsDisplayed()
        composeRule.onNodeWithText("Exact shortcut").performClick()
        composeRule.runOnIdle { assertEquals(shortcut, invoked) }
    }

    @Test
    fun drawerActionSheetOmitsHomeActionsAndTerminalShortcutDivider() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.drawer", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Drawer application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val shortcut = ApplicationShortcut(
            packageName = "com.example.drawer",
            shortcutId = "drawer-shortcut",
            label = "Drawer shortcut",
            icon = null,
            user = entry.user,
            rank = 0,
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Drawer,
                    favoriteState = FavoriteReadState.Readable(listOf(entry.identity)),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    onEditFavorites = {},
                    canEditFavorites = true,
                    informationLauncher = ApplicationInformationLauncher { true },
                    uninstallAvailable = true,
                    shortcuts = listOf(shortcut),
                )
            }
        }

        composeRule.onNodeWithText(text = "Drawer shortcut").assertIsDisplayed()
        composeRule.onAllNodesWithTag(testTag = "favorite_action").assertCountEquals(expectedSize = 0)
        composeRule.onAllNodesWithTag(testTag = "edit_favorites_action")
            .assertCountEquals(expectedSize = 0)
        composeRule.onNodeWithTag(testTag = "uninstall_application_action").assertIsDisplayed()
    }

    @Test
    fun drawerActionSheetRemainsAvailableWhenFavoriteStorageFails() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 0,
                componentName = ComponentName("com.example.drawer.failure", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Drawer independent application",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val favoriteStore = TestFavoriteStore(initial = emptyList())
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    favoriteStore = favoriteStore,
                )
            }
        }
        composeRule.onNodeWithTag(testTag = "home_surface").performTouchInput { swipeUp() }
        composeRule.onNodeWithText(text = "Drawer independent application")
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(testTag = "application_action_sheet").assertIsDisplayed()

        composeRule.runOnIdle { favoriteStore.failRead() }

        composeRule.onNodeWithTag(testTag = "application_action_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = "application_information_action").assertIsDisplayed()
    }

    @Test
    fun actionSheetOmitsShortcutRegionWhenNoneAreAvailable() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(0, ComponentName("com.example.none", "MainActivity")),
            user = Process.myUserHandle(),
            label = "No shortcuts",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                )
            }
        }

        composeRule.onAllNodesWithTag("application_shortcut_region").assertCountEquals(0)
    }

    @Test
    fun actionSheetScrollsApplicationShortcutsWithinAvailableHeight() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                0,
                ComponentName("com.example.many.shortcuts", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Many shortcuts",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val shortcuts = List(20) { index ->
            ApplicationShortcut(
                packageName = entry.identity.componentName.packageName,
                shortcutId = "shortcut-$index",
                label = "Shortcut $index",
                icon = null,
                user = entry.user,
                rank = index,
            )
        }
        composeRule.setContent {
            Launcher4MaxTheme {
                ApplicationActionSheet(
                    entry = entry,
                    source = ApplicationActionSheetSource.Home,
                    favoriteState = FavoriteReadState.Readable(emptyList()),
                    onDismiss = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    informationLauncher = ApplicationInformationLauncher { true },
                    shortcuts = shortcuts,
                )
            }
        }

        composeRule.onNodeWithText("Shortcut 19").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeActionSheetLoadsAndLaunchesShortcutForExactEntry() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                profileSerialNumber = 11,
                componentName = ComponentName("com.example.source", "ExactActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Shortcut source",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        val shortcut = ApplicationShortcut(
            packageName = entry.identity.componentName.packageName,
            shortcutId = "open-exact",
            label = "Open exact shortcut",
            icon = null,
            user = entry.user,
            rank = 0,
        )
        var loadedEntry: LaunchableEntry? = null
        var launchedShortcut: ApplicationShortcut? = null
        val controller = object : ApplicationShortcutController {
            override suspend fun load(entry: LaunchableEntry): List<ApplicationShortcut> {
                loadedEntry = entry
                return listOf(shortcut)
            }

            override fun launch(shortcut: ApplicationShortcut): Boolean {
                launchedShortcut = shortcut
                return true
            }
        }
        composeRule.setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    favoriteStore = TestFavoriteStore(listOf(entry.identity)),
                    shortcutController = controller,
                )
            }
        }

        composeRule.onNodeWithText("Shortcut source").performTouchInput { longClick() }
        composeRule.onNodeWithText("Open exact shortcut").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(entry, loadedEntry)
            assertEquals(shortcut, launchedShortcut)
        }
        composeRule.onAllNodesWithTag("application_action_sheet").assertCountEquals(0)
    }

    @Test
    fun drawerSettingsGearJumpsToDedicatedEntryBeforeOpening() {
        val entry = LaunchableEntry(
            identity = LaunchableIdentity(
                0,
                ComponentName("com.example.settings", "MainActivity"),
            ),
            user = Process.myUserHandle(),
            label = "Settings source",
            icon = ColorDrawable(Color.TRANSPARENT),
        )
        var settingsOpened = false
        composeRule.setContent {
            Launcher4MaxTheme {
                DrawerScreen(
                    inventoryLoader = LaunchableInventoryLoader { completeSnapshot(entry) },
                    onOpenSettings = { settingsOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag("drawer_index_settings").performClick()
        composeRule.runOnIdle { assertEquals(false, settingsOpened) }
        composeRule.onNodeWithTag("drawer_settings_anchor").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_settings_entry").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, settingsOpened) }
    }

    @Test
    fun settingsPresentsBasicOfflineLoop() {
        var homeSettingsOpened = false
        var repositoryOpened = false
        var backRequested = false
        val platform = object : SettingsPlatform {
            override fun isDefaultHome() = true

            override fun openDefaultHomeSettings(): Boolean {
                homeSettingsOpened = true
                return true
            }

            override fun openProjectRepository(): Boolean {
                repositoryOpened = true
                return true
            }

            override fun versionText() = "v1.1.0(2)"
        }
        composeRule.setContent {
            Launcher4MaxTheme {
                SettingsScreen(
                    platform = platform,
                    licenseText = "Apache License test content",
                    thirdPartyNoticesText = "Third-party notice test content",
                    onBack = { backRequested = true },
                )
            }
        }

        composeRule.onNodeWithTag("settings_title").assertTextEquals("Settings")
        composeRule.onNodeWithText("Launcher4Max is the default launcher").assertIsDisplayed()
        composeRule.onNodeWithText("v1.1.0(2)").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_default_home").performClick()
        composeRule.runOnIdle { assertEquals(true, homeSettingsOpened) }

        composeRule.onNodeWithTag("settings_project_repository").performClick()
        composeRule.runOnIdle { assertEquals(true, repositoryOpened) }

        composeRule.onNodeWithTag("settings_back").performClick()
        composeRule.runOnIdle { assertEquals(true, backRequested) }

        composeRule.onNodeWithTag("settings_license").performClick()
        composeRule.onNodeWithTag("launcher4max_license_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Apache License test content").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_third_party_notices").performClick()
        composeRule.onNodeWithTag("third_party_notices_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Third-party notice test content").assertIsDisplayed()
    }

    @Test
    fun defaultLauncherPromptShowsWhenRequestedAndRoutesSelection() {
        var selected = false
        var dismissed = false
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    showDefaultLauncherPrompt = true,
                    onSelectDefaultLauncherPrompt = { selected = true },
                    onDismissDefaultLauncherPrompt = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("home_default_launcher_prompt").assertIsDisplayed()
        composeRule.onNodeWithText("Not set as the default launcher yet").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Set as default to return here",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Not set as the default launcher yet").performClick()
        composeRule.runOnIdle { assertEquals(true, selected) }
        composeRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test
    fun defaultLauncherPromptIsHiddenInEditMode() {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    editMode = true,
                    showDefaultLauncherPrompt = true,
                )
            }
        }

        composeRule.onNodeWithTag("home_default_launcher_prompt").assertDoesNotExist()
        composeRule.onNodeWithTag("home_edit_dock").assertIsDisplayed()
    }

    @Test
    fun defaultLauncherPromptIsAbsentWhenNotRequested() {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen()
            }
        }

        composeRule.onNodeWithTag("home_default_launcher_prompt").assertDoesNotExist()
    }

    @Test
    fun defaultBindingsHaveNoHomeAction() {
        val controller = TestAccessibilityLockController(systemEnabled = true, connected = true)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(accessibilityLockController = controller)
            }
        }

        composeRule.onNodeWithTag("home_quick_action_region")
            .performTouchInput { doubleClick() }
        composeRule.onNodeWithTag("home_quick_action_region")
            .performTouchInput { longClick() }

        composeRule.runOnIdle { assertEquals(0, controller.lockRequests) }
    }

    @Test
    fun boundScreenLockRequestsOneActionFromEligibleBlankSpace() {
        val controller = TestAccessibilityLockController(systemEnabled = true, connected = true)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    accessibilityLockController = controller,
                    quickActionBindings = QuickActionBindings(
                        doubleTap = QuickAction.ScreenLock,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("home_quick_action_region")
            .performTouchInput { doubleClick() }

        composeRule.runOnIdle { assertEquals(1, controller.lockRequests) }
    }

    @Test
    fun boundScreenLockDoesNothingWhenServiceIsOff() {
        val controller = TestAccessibilityLockController(systemEnabled = false, connected = false)
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    accessibilityLockController = controller,
                    quickActionBindings = QuickActionBindings(
                        doubleTap = QuickAction.ScreenLock,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("home_quick_action_region")
            .performTouchInput { doubleClick() }

        composeRule.runOnIdle { assertEquals(0, controller.lockRequests) }
    }

    @Test
    fun settingsListContainsQuickActionSettingsAndNoStandaloneLockItem() {
        composeRule.setContent {
            Launcher4MaxTheme {
                SettingsScreen(
                    platform = EmptySettingsPlatform,
                    licenseText = "",
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings_quick_action_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_double_tap_lock").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_privacy").assertIsDisplayed()
    }

    @Test
    fun enableOrientedAccessibilityHandoffRequiresProminentDisclosure() {
        val controller = TestAccessibilityLockController(systemEnabled = false, connected = false)
        composeRule.setContent {
            Launcher4MaxTheme {
                SettingsScreen(
                    platform = EmptySettingsPlatform,
                    licenseText = "",
                    accessibilityLockController = controller,
                    page = SettingsPage.QuickActions,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_action_service_state").performClick()
        composeRule.onNodeWithTag("screen_lock_explanation_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("open_accessibility_settings").performClick()
        composeRule.onNodeWithTag("accessibility_prominent_disclosure").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, controller.settingsRequests) }

        composeRule.onNodeWithTag("accessibility_disclosure_cancel").performClick()
        composeRule.onAllNodesWithTag("accessibility_prominent_disclosure").assertCountEquals(0)
        composeRule.runOnIdle { assertEquals(0, controller.settingsRequests) }
    }

    @Test
    fun disclosureContinueOpensSystemSettingsWithoutRetainedToggle() {
        val controller = TestAccessibilityLockController(systemEnabled = false, connected = false)
        composeRule.setContent {
            Launcher4MaxTheme {
                SettingsScreen(
                    platform = EmptySettingsPlatform,
                    licenseText = "",
                    accessibilityLockController = controller,
                    page = SettingsPage.QuickActions,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_action_service_state").performClick()
        composeRule.onNodeWithTag("open_accessibility_settings").performClick()
        composeRule.onNodeWithTag("accessibility_disclosure_continue").performClick()

        composeRule.runOnIdle { assertEquals(1, controller.settingsRequests) }
        composeRule.onAllNodesWithTag("accessibility_prominent_disclosure").assertCountEquals(0)
    }

    @Test
    fun selectingScreenLockWhileServiceOffSavesBindingAndEntersAuthorization() {
        val controller = TestAccessibilityLockController(systemEnabled = false, connected = false)
        var boundSlot: QuickActionSlot? = null
        var boundAction: QuickAction? = null
        composeRule.setContent {
            Launcher4MaxTheme {
                SettingsScreen(
                    platform = EmptySettingsPlatform,
                    licenseText = "",
                    accessibilityLockController = controller,
                    page = SettingsPage.QuickActions,
                    onBindQuickAction = { slot, action ->
                        boundSlot = slot
                        boundAction = action
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_action_slot_double_tap").performClick()
        composeRule.onNodeWithTag("quick_action_option_screen_lock").performClick()

        composeRule.runOnIdle {
            assertEquals(QuickActionSlot.DoubleTap, boundSlot)
            assertEquals(QuickAction.ScreenLock, boundAction)
        }
        composeRule.onNodeWithTag("screen_lock_explanation_sheet").assertIsDisplayed()
    }

}

// Exceeds the Home-Drawer completion distance even when the gesture starts on a short node.
private const val HOME_DRAWER_SWIPE_DISTANCE_PX = 900f

private fun completeSnapshot(vararg entries: LaunchableEntry): LaunchableInventorySnapshot =
    LaunchableInventorySnapshot(
        entries = entries.toList(),
        profileReadStatus = entries
            .map { it.identity.profileSerialNumber }
            .distinct()
            .associateWith { ProfileInventoryReadStatus.Complete },
    )

private class TestFavoriteStore(initial: List<LaunchableIdentity>) : FavoriteStore {
    private val initialAggregate = OrderedFavoriteAggregate(
        modules = if (initial.isEmpty()) emptyList() else listOf(
            OrderedFavoriteModule(PRIMARY_LIST_ID, OrderedFavoriteModuleType.Vertical, initial),
        ),
    )
    private val mutableState = MutableStateFlow<FavoriteReadState>(
        FavoriteReadState.Readable(
            aggregate = initialAggregate.toLegacyAggregate(),
            orderedModules = initialAggregate.modules,
            orderedAggregate = initialAggregate,
        ),
    )
    override val state: StateFlow<FavoriteReadState> = mutableState
    override suspend fun load() = Unit
    override suspend fun add(identity: LaunchableIdentity): Boolean {
        return updateOrderedAggregate { aggregate ->
            if (identity in aggregate.identities) return@updateOrderedAggregate aggregate
            val first = aggregate.modules.firstOrNull()
            aggregate.copy(
                modules = if (first == null) {
                    listOf(
                        OrderedFavoriteModule(
                            PRIMARY_LIST_ID, OrderedFavoriteModuleType.Vertical, listOf(identity),
                        ),
                    )
                } else {
                    listOf(first.copy(identities = first.identities + identity)) +
                        aggregate.modules.drop(1)
                },
            )
        } != null
    }
    override suspend fun remove(identity: LaunchableIdentity): Boolean =
        removeAll(setOf(identity))
    override suspend fun removeAll(identities: Set<LaunchableIdentity>): Boolean {
        return updateOrderedAggregate { aggregate ->
            identities.fold(aggregate) { current, identity ->
                removeOrderedFavorite(current, identity)
            }
        } != null
    }

    override suspend fun updateOrderedAggregate(
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ): OrderedFavoriteAggregate? {
        val current = (mutableState.value as? FavoriteReadState.Readable)
            ?.orderedAggregate ?: return null
        val updated = transform(current)
        if (!isValidOrderedFavoriteAggregate(updated)) return null
        mutableState.value = FavoriteReadState.Readable(
            aggregate = updated.toLegacyAggregate(),
            orderedModules = updated.modules,
            orderedAggregate = updated,
        )
        return updated
    }

    fun readableIdentities(): List<LaunchableIdentity> =
        (state.value as FavoriteReadState.Readable).identities

    fun failRead() {
        mutableState.value = FavoriteReadState.ReadFailure
    }
}

private class TestAccessibilityLockController(
    private var systemEnabled: Boolean,
    connected: Boolean,
) : AccessibilityLockController {
    private val mutableConnectionState = MutableStateFlow(connected)
    override val availableForValidation = true
    override val connectionState: StateFlow<Boolean> = mutableConnectionState
    var settingsRequests = 0
        private set
    var lockRequests = 0
        private set

    override fun isSystemEnabled() = systemEnabled

    override fun openAccessibilitySettings(): Boolean {
        settingsRequests += 1
        return true
    }

    override fun requestLock(): LockRequestResult {
        lockRequests += 1
        return LockRequestResult.Requested
    }
}
