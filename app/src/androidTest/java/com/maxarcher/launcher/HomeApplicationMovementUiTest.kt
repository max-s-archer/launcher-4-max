package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.home.components.stableKey

class HomeApplicationMovementUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singleColumnMovesByInsertionWithoutLaunchingOrRemoving() = verifyMovement()

    @Test
    fun multiColumnBelowNamesMoveWithoutLosingTheOwningPointer() = verifyMovement(below = true)

    @Test
    fun ribbonUsesTheSameExclusiveWholeItemMovement() = verifyMovement(ribbon = true)

    @Test
    fun verticalToRibbonUsesTheDestinationInsertionAxis() = verifyMovement(destinationRibbon = true)

    @Test
    fun ribbonToVerticalKeepsTheSourcePreviewUntilRelease() = verifyMovement(ribbon = true, destinationRibbon = false)

    @Test
    fun verticalToVerticalUsesAnExplicitDestinationModule() = verifyMovement(destinationRibbon = false)

    @Test
    fun ribbonToRibbonUsesAnExplicitDestinationModule() = verifyMovement(ribbon = true, destinationRibbon = true)

    @Test
    fun finalApplicationCanDropOntoCreateListWithoutAnInsertionLine() = verifyMovement(creationType = OrderedFavoriteModuleType.Vertical, singleSource = true)

    @Test
    fun belowNameApplicationCanDropOntoCreateRibbon() = verifyMovement(below = true, creationType = OrderedFavoriteModuleType.Ribbon)

    @Test
    fun cancellingVerticalLiftRestoresTheSourceAndItsClosedGap() = verifyMovement(cancelMovement = true)

    @Test
    fun cancellingWrappedRowLiftRestoresTheSourceAndItsClosedGap() = verifyMovement(below = true, cancelMovement = true)

    @Test
    fun cancellingRibbonLiftRestoresTheSourceAndItsClosedGap() = verifyMovement(ribbon = true, cancelMovement = true)

    @Test
    fun returningToTheOriginalReducedBoundaryDoesNotCommit() = verifyMovement(unchangedRelease = true)

    @Test
    fun singleVerticalSourceRetainsOnlyAnInvalidAddEntryUntilRestoration() = verifyMovement(singleSource = true)

    @Test
    fun singleRibbonSourceRetainsOnlyAnInvalidAddEntryUntilRestoration() = verifyMovement(singleSource = true, ribbon = true)

    @Test
    fun singleRibbonSourceCanStillCreateANewModule() = verifyMovement(singleSource = true, ribbon = true, creationType = OrderedFavoriteModuleType.Ribbon)

    private fun verifyMovement(
        below: Boolean = false,
        ribbon: Boolean = false,
        destinationRibbon: Boolean? = null,
        creationType: OrderedFavoriteModuleType? = null,
        singleSource: Boolean = false,
        cancelMovement: Boolean = false,
        unchangedRelease: Boolean = false,
    ) {
        val count = if (singleSource) 1 else if (destinationRibbon != null) 4 else if (ribbon) 2 else 3
        val entries = (1..count).map(
            transform = { index ->
                LaunchableEntry(
                    identity = LaunchableIdentity(
                        profileSerialNumber = 1,
                        componentName = ComponentName("test.app$index", "Main"),
                    ),
                    user = Process.myUserHandle(), label = "App $index", icon = ColorDrawable(Color.TRANSPARENT),
                )
            },
        )
        val module = OrderedFavoriteModule(
            id = "source",
            type = if (ribbon) OrderedFavoriteModuleType.Ribbon else OrderedFavoriteModuleType.Vertical,
            identities = entries.take(n = if (destinationRibbon != null) 2 else count).map(transform = { it.identity }),
            namePlacement = if (below) FavoriteNamePlacement.Below else FavoriteNamePlacement.Right,
            itemsPerRow = if (below) 2 else 1,
        )
        val target = destinationRibbon?.let(
            block = {
                OrderedFavoriteModule(
                    id = "target", type = if (it) OrderedFavoriteModuleType.Ribbon else OrderedFavoriteModuleType.Vertical,
                    identities = entries.drop(n = 2).map(transform = { entry -> entry.identity }),
                )
            },
        )
        val modules = listOf(element = module) + listOfNotNull(element = target)
        val containers = modules.map(
            transform = {
                FavoriteContainer(
                    id = it.id, type = if (it.type == OrderedFavoriteModuleType.Ribbon) FavoriteContainerType.FavoriteBar else FavoriteContainerType.VerticalList,
                    identities = it.identities, namePlacement = it.namePlacement, itemsPerRow = it.itemsPerRow,
                )
            },
        )
        val state = FavoriteReadState.Readable(
            aggregate = FavoriteAggregate(
                favoriteBars = containers.filter(predicate = { it.type == FavoriteContainerType.FavoriteBar }),
                verticalLists = containers.filter(predicate = { it.type == FavoriteContainerType.VerticalList }),
            ),
            orderedModules = modules,
        )
        var timeout = 0L
        var change: ApplicationOrderChange? = null
        var removals = 0
        var launches = 0
        composeRule.setContent(
            composable = {
                timeout = LocalViewConfiguration.current.longPressTimeoutMillis
                Launcher4MaxTheme(content = {
                    HomeScreen(
                        favoriteState = state,
                        favoriteAvailability = entries.associate(transform = { it.identity to FavoriteAvailability.Available(entry = it) }),
                        editMode = true,
                        onRemoveApplication = { removals += 1 },
                        onLaunchFavorite = { launches += 1 },
                        onCommitApplicationOrder = { requested, complete ->
                            change = requested
                            complete()
                        },
                    )
                })
            },
        )
        val root = composeRule.onNodeWithTag(testTag = "home_ordered_favorite_modules")
        val origin = root.fetchSemanticsNode().boundsInRoot.topLeft
        val sourceNode = composeRule.onNodeWithTag(testTag = "home_favorite_item:${entries.first().identity.stableKey()}")
        val source = sourceNode.fetchSemanticsNode().boundsInRoot
        composeRule.onAllNodesWithTag(testTag = "remove_favorite_item")[0].performClick()
        composeRule.runOnIdle(action = { assertEquals(1, removals) })
        composeRule.onNodeWithTag(testTag = "home_application_movement_preview").assertDoesNotExist()

        root.performTouchInput(block = { down(position = source.center - origin) })
        composeRule.mainClock.advanceTimeBy(milliseconds = timeout + 50)
        composeRule.onNodeWithTag(testTag = "home_application_movement_preview").assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = "home_application_placeholder").assertDoesNotExist()
        sourceNode.assertDoesNotExist()
        // The real source slot is absent, not an invisible measured item. Remaining entries reflow.
        val firstRemaining = composeRule.onNodeWithTag(
            testTag = if (singleSource) "home_add_favorite_${module.id}" else "home_favorite_item:${entries[1].identity.stableKey()}",
        ).fetchSemanticsNode().boundsInRoot
        assertEquals(source.left, firstRemaining.left, 0.5f)
        assertEquals(source.top, firstRemaining.top, 0.5f)
        val invalidSourceDrop = singleSource && creationType == null
        val destination = if (creationType != null) {
            composeRule.onNodeWithTag(
                testTag = if (creationType == OrderedFavoriteModuleType.Vertical) "home_add_favorite_list" else "home_add_favorite_ribbon",
            ).fetchSemanticsNode().boundsInRoot.center
        } else if (invalidSourceDrop) {
            firstRemaining.center
        } else if (unchangedRelease) {
            Offset(x = firstRemaining.center.x, y = firstRemaining.top + 1f)
        } else {
            // Read the destination after reflow, not the pre-lift source layout.
            val last = composeRule.onNodeWithTag(testTag = "home_favorite_item:${entries.last().identity.stableKey()}")
                .fetchSemanticsNode().boundsInRoot
            if (below || (destinationRibbon ?: ribbon)) {
                Offset(x = last.right - 1f, y = last.center.y)
            } else {
                Offset(x = last.center.x, y = last.bottom - 1f)
            }
        }
        root.performTouchInput(block = { moveTo(position = destination - origin) })
        if (invalidSourceDrop) {
            composeRule.onNodeWithTag(testTag = "home_application_insertion_line").assertDoesNotExist()
            composeRule.onNodeWithTag(testTag = "home_module_creation_drop_outline").assertDoesNotExist()
        } else if (creationType == null) {
            composeRule.onNodeWithTag(testTag = "home_application_insertion_line").assertIsDisplayed()
            composeRule.onNodeWithTag(testTag = "home_module_creation_drop_outline").assertDoesNotExist()
        } else {
            composeRule.onNodeWithTag(testTag = "home_application_insertion_line").assertDoesNotExist()
            composeRule.onNodeWithTag(testTag = "home_module_creation_drop_outline").assertIsDisplayed()
        }
        composeRule.runOnIdle(action = { assertNull(change) })
        root.performTouchInput(block = {
            // Negative coordinates are still inside the module viewport, so they resolve
            // as an end insertion. Release below the list instead.
            if (cancelMovement) moveTo(position = Offset(x = center.x, y = bottom + 1f))
            up()
        })
        composeRule.runOnIdle(action = {
            if (cancelMovement || unchangedRelease || invalidSourceDrop) {
                assertNull(change)
            } else {
                assertNotNull(change)
                val expected = if (creationType != null) emptyList() else if (target == null) module.identities.drop(n = 1) else target.identities
                assertEquals(expected + module.identities.first(), change?.reorderedIdentities())
                assertEquals(target?.id ?: module.id, change?.destinationModuleId)
                assertEquals(creationType, change?.newModuleType)
            }
            assertEquals(0, launches)
            assertEquals(1, removals)
        })
        composeRule.onNodeWithTag(testTag = "home_application_movement_preview").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_application_placeholder").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_module_creation_drop_outline").assertDoesNotExist()
        if (cancelMovement || unchangedRelease || invalidSourceDrop) {
            sourceNode.assertIsDisplayed()
            assertEquals(source, sourceNode.fetchSemanticsNode().boundsInRoot)
        }
    }
}
