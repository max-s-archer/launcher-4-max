package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.maxarcher.launcher.ui.home.components.HomeApplicationMovementOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.home.components.stableKey

class HomeApplicationHandoffUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pointerUpdatesMoveThePreviewWithoutRecomposingThePhaseReader() {
        val identity = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app", "Main"))
        val module = OrderedFavoriteModule(id = "source", type = OrderedFavoriteModuleType.Vertical, identities = listOf(element = identity))
        val creation = Rect(left = 0f, top = 160f, right = 300f, bottom = 240f)
        val movement = HomeApplicationMovement().apply(block = {
            updateModules(current = listOf(element = module))
            updateViewport(bounds = Rect(left = 0f, top = 0f, right = 600f, bottom = 600f))
            updateItem(identity = identity, bounds = Rect(left = 0f, top = 0f, right = 240f, bottom = 56f))
            updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = creation)
            check(value = start(
                identity = identity, module = module,
                availability = FavoriteAvailability.Disabled(presentationEntry = null),
                pointer = Offset(x = 50f, y = 28f),
            ))
            move(pointer = creation.center)
        })
        var compositions = 0
        composeRule.setContent(composable = {
            Launcher4MaxTheme(content = {
                val dragging = movement.isDragging
                SideEffect(effect = {
                    check(value = dragging)
                    compositions += 1
                })
                Box(modifier = Modifier.fillMaxSize(), content = {
                    HomeApplicationMovementOverlay(movement = movement, rootOrigin = Offset.Zero)
                })
            })
        })
        composeRule.runOnIdle(action = {
            // Bisect: the movement session must still be dragging with a captured
            // preview source before blaming the overlay's render gate.
            check(value = movement.isDragging)
            check(value = movement.previewSource != null)
            return@runOnIdle null
        })
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(
                testTag = "home_application_movement_preview",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val preview = composeRule.onNodeWithTag(
            testTag = "home_application_movement_preview",
            useUnmergedTree = true,
        )
        val before = preview.fetchSemanticsNode().boundsInRoot
        val count = compositions
        composeRule.runOnIdle(action = {
            movement.move(pointer = creation.center + Offset(x = 24f, y = 0f))
            movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = creation.translate(offset = Offset(x = 0f, y = 2f)))
        })
        val after = preview.fetchSemanticsNode().boundsInRoot
        assertEquals(before.left + 24f, after.left, 1f)
        assertEquals(before.top, after.top, 1f)
        composeRule.runOnIdle(action = { assertEquals(count, compositions) })
    }

    @Test
    fun sameModuleSaveKeepsOnePreviewUntilTheSavedOrderIsPresented() = verifyHandoff(sameModule = true)

    @Test
    fun crossModuleSaveDoesNotRestoreTheSourceOrDuplicateTheDestination() = verifyHandoff()

    @Test
    fun creationSaveKeepsThePreviewWhenTheSourceModuleDisappears() = verifyHandoff(createModule = true)

    @Test
    fun failedSaveRestoresTheSourceWithoutASecondVisualInstance() = verifyHandoff(failSave = true)

    @Test
    fun leavingEditModeClearsThePreviewWithoutCancellingTheSave() = verifyHandoff(leaveHome = true)

    private fun verifyHandoff(sameModule: Boolean = false, createModule: Boolean = false, failSave: Boolean = false, leaveHome: Boolean = false) {
        val entries = (1..2).map(transform = { index ->
            LaunchableEntry(
                identity = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app$index", "Main")),
                user = Process.myUserHandle(), label = "App $index", icon = ColorDrawable(Color.TRANSPARENT),
            )
        })
        val source = OrderedFavoriteModule(
            id = "source", type = OrderedFavoriteModuleType.Vertical,
            identities = entries.take(n = if (sameModule) 2 else 1).map(transform = { it.identity }),
        )
        val target = OrderedFavoriteModule(id = "target", type = OrderedFavoriteModuleType.Ribbon, identities = listOf(element = entries[1].identity))
        var modules by mutableStateOf(value = if (sameModule) listOf(element = source) else listOf(source, target))
        var editing by mutableStateOf(value = true)
        var change: ApplicationOrderChange? = null
        var complete: (() -> Unit)? = null
        var timeout = 0L
        composeRule.setContent(composable = {
            timeout = LocalViewConfiguration.current.longPressTimeoutMillis
            val containers = modules.map(transform = { module ->
                FavoriteContainer(
                    id = module.id, type = if (module.type == OrderedFavoriteModuleType.Ribbon) FavoriteContainerType.FavoriteBar else FavoriteContainerType.VerticalList,
                    identities = module.identities,
                )
            })
            Launcher4MaxTheme(content = {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = FavoriteAggregate(
                            verticalLists = containers.filter(predicate = { it.type == FavoriteContainerType.VerticalList }),
                            favoriteBars = containers.filter(predicate = { it.type == FavoriteContainerType.FavoriteBar }),
                        ),
                        orderedModules = modules,
                    ),
                    favoriteAvailability = entries.associate(transform = { it.identity to FavoriteAvailability.Available(entry = it) }),
                    editMode = editing,
                    onCommitApplicationOrder = { requested, onComplete ->
                        change = requested
                        complete = onComplete
                    },
                )
            })
        })
        val root = composeRule.onNodeWithTag(testTag = "home_ordered_favorite_modules")
        val origin = root.fetchSemanticsNode().boundsInRoot.topLeft
        val sourceTag = "home_favorite_item:${entries[0].identity.stableKey()}"
        val sourceNode = composeRule.onNodeWithTag(testTag = sourceTag)
        val originalBounds = sourceNode.fetchSemanticsNode().boundsInRoot
        root.performTouchInput(block = { down(position = originalBounds.center - origin) })
        composeRule.mainClock.advanceTimeBy(milliseconds = timeout + 50)
        val destination = if (createModule) {
            composeRule.onNodeWithTag(testTag = "home_add_favorite_ribbon").fetchSemanticsNode().boundsInRoot.center
        } else {
            val bounds = composeRule.onNodeWithTag(testTag = "home_favorite_item:${entries[1].identity.stableKey()}")
                .fetchSemanticsNode().boundsInRoot
            if (sameModule) Offset(x = bounds.center.x, y = bounds.bottom - 1f) else Offset(x = bounds.right - 1f, y = bounds.center.y)
        }
        root.performTouchInput(block = { moveTo(position = destination - origin) })
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(
                testTag = "home_application_movement_preview",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val preview = composeRule.onNodeWithTag(
            testTag = "home_application_movement_preview",
            useUnmergedTree = true,
        )
        val releaseBounds = preview.fetchSemanticsNode().boundsInRoot
        root.performTouchInput(block = { up() })
        composeRule.runOnIdle(action = { assertNotNull(change); assertNotNull(complete) })
        preview.assertIsDisplayed()
        assertEquals(releaseBounds, preview.fetchSemanticsNode().boundsInRoot)
        sourceNode.assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_application_insertion_line").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_module_creation_drop_outline").assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = "home_application_edge_feedback").assertDoesNotExist()

        if (leaveHome) {
            composeRule.runOnIdle(action = { editing = false })
            preview.assertDoesNotExist()
        }
        if (!failSave) {
            composeRule.runOnIdle(action = {
                modules = checkNotNull(value = applyApplicationOrderChange(
                    aggregate = OrderedFavoriteAggregate(modules = modules), change = checkNotNull(value = change),
                    newModuleId = if (createModule) "created" else null,
                )).modules
            })
        }
        if (!leaveHome) {
            // Model Flow delivery before the completion acknowledgement. Even if the saved source
            // module no longer exists, the real destination must stay omitted until handoff.
            preview.assertIsDisplayed()
            assertEquals(releaseBounds, preview.fetchSemanticsNode().boundsInRoot)
            sourceNode.assertDoesNotExist()
        }
        composeRule.runOnIdle(action = { checkNotNull(value = complete).invoke() })
        preview.assertDoesNotExist()
        composeRule.onAllNodesWithTag(testTag = sourceTag).assertCountEquals(expectedSize = 1)
        sourceNode.assertIsDisplayed()
        if (failSave) assertEquals(originalBounds, sourceNode.fetchSemanticsNode().boundsInRoot)
    }
}
