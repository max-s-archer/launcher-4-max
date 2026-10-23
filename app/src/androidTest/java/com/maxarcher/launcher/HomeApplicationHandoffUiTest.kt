package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.maxarcher.launcher.ui.home.components.HomeApplicationMovementOverlay
import com.maxarcher.launcher.ui.home.components.detectModuleReorderDrag
import com.maxarcher.launcher.ui.home.HomeCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.maxarcher.launcher.ui.home.components.stableKey

class HomeApplicationHandoffUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val files = TemporaryFolder()

    @Test
    fun dockReorderWaitsForReliablePresentationAndInvalidatesUndo() = verifyDockReorder()

    @Test
    fun fullDockStillReordersThroughTheEditor() = verifyDockReorder(full = true)

    @Test
    fun dockOriginalSlotDoesNotWriteOrInvalidateUndo() = verifyDockReorder(noOp = true)

    @Test
    fun dockFailedSaveKeepsDurableOrderAndClearsPreview() = verifyDockReorder(fail = true)

    @Test
    fun dockBusyEditorRejectsReleaseAndClearsPreview() = verifyDockReorder(busy = true)

    @Test
    fun dockCancelledPointerDoesNotWriteAndClearsPreview() = verifyDockReorder(cancel = true)

    @Test
    fun dockCancelledSaveClearsPreviewAndKeepsDurableOrder() =
        verifyDockReorder(cancelSave = true)

    @Test
    fun dockCrossAreaCancelDoesNotWriteOrInvalidateUndo() =
        verifyCancelledMovement(fromDock = true)

    @Test
    fun moduleSourceCancelDoesNotWriteOrInvalidateUndo() =
        verifyCancelledMovement(fromDock = false)

    @Test
    fun moduleHandleCancelCallsCancelInsteadOfEnd() {
        var timeout = 0L
        var starts = 0
        var moves = 0
        var ends = 0
        var cancellations = 0
        composeRule.setContent {
            timeout = LocalViewConfiguration.current.longPressTimeoutMillis
            Box(
                Modifier.fillMaxSize().testTag("module_handle").pointerInput(Unit) {
                    detectModuleReorderDrag(
                        onLongPress = {},
                        onDragStart = { starts += 1; true },
                        onDrag = { moves += 1 },
                        onDragEnd = { ends += 1 },
                        onDragCancel = { cancellations += 1 },
                    )
                },
            )
        }
        val handle = composeRule.onNodeWithTag("module_handle")
        handle.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(timeout + 50)
        handle.performTouchInput { moveTo(center + Offset(0f, 80f)) }
        composeRule.runOnIdle {
            assertEquals(1, starts)
            assertTrue(moves > 0)
        }
        handle.performTouchInput { cancel() }
        composeRule.runOnIdle {
            assertEquals(0, ends)
            assertEquals(1, cancellations)
        }
    }

    private fun verifyCancelledMovement(fromDock: Boolean) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val identities = (1..3).map {
            LaunchableIdentity(1, ComponentName("cancel.app$it", "Main"))
        }
        val initial = OrderedFavoriteAggregate(
            modules = listOf(
                OrderedFavoriteModule(
                    id = "target",
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(identities[1]),
                ),
            ),
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                identities = listOf(identities[0], identities[2]),
            ),
        )
        val store = OrderedFavoriteStoreAdapter(files.newFolder().resolve("favorites.bin"))
        val controlled = DockControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        runBlocking {
            store.load()
            checkNotNull(store.updateOrderedAggregate { initial })
            check(editor.remove(identities[2]))
        }
        val undo = checkNotNull(editor.undoRemoval)
        val before = store.currentOrderedAggregate()
        controlled.writeAttempts = 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val coordinator = HomeCoordinator(context, scope, controlled, editor)
        var requested: ApplicationOrderChange? = null
        var completions = 0
        var timeout = 0L
        try {
            composeRule.setContent {
                timeout = LocalViewConfiguration.current.longPressTimeoutMillis
                val state by store.state.collectAsState()
                SideEffect { coordinator.updateFavoriteState(state) }
                Launcher4MaxTheme {
                    HomeScreen(
                        favoriteState = state,
                        favoriteAvailability = identities.associateWith {
                            FavoriteAvailability.Disabled(presentationEntry = null)
                        },
                        editMode = true,
                        applicationEditingSaving = editor.isSaving,
                        onCommitApplicationOrder = { change, complete ->
                            requested = change
                            coordinator.commitApplicationOrder(change) {
                                completions += 1
                                complete()
                            }
                        },
                    )
                }
            }
            val root = composeRule.onRoot()
            val origin = root.fetchSemanticsNode().boundsInRoot.topLeft
            val dockTag = "home_bottom_app_bar_item:${identities[0].stableKey()}"
            val moduleTag = "home_favorite_item:${identities[1].stableKey()}"
            val sourceTag = if (fromDock) dockTag else moduleTag
            val targetTag = if (fromDock) moduleTag else dockTag
            val source = composeRule.onNodeWithTag(sourceTag)
            val start = source.fetchSemanticsNode().boundsInRoot.center - origin
            val target = composeRule.onNodeWithTag(targetTag)
                .fetchSemanticsNode().boundsInRoot.center - origin
            root.performTouchInput { down(start) }
            if (!fromDock) composeRule.mainClock.advanceTimeBy(timeout + 50)
            root.performTouchInput { moveTo(target) }
            val preview = composeRule.onNodeWithTag(
                "home_application_movement_preview",
                useUnmergedTree = true,
            )
            preview.assertIsDisplayed()
            composeRule.onNodeWithTag("home_application_insertion_line").assertIsDisplayed()
            root.performTouchInput { cancel() }
            composeRule.runOnIdle {
                assertNull(requested)
                assertEquals(0, controlled.writeAttempts)
                assertEquals(0, completions)
                assertFalse(editor.isSaving)
                assertSame(undo, editor.undoRemoval)
            }
            preview.assertDoesNotExist()
            composeRule.onNodeWithTag(
                "home_bottom_app_bar_drag_preview",
                useUnmergedTree = true,
            ).assertDoesNotExist()
            composeRule.onNodeWithTag("home_application_insertion_line").assertDoesNotExist()
            source.assertIsDisplayed()
            composeRule.onAllNodesWithTag(sourceTag).assertCountEquals(1)
            runBlocking {
                store.load()
                assertEquals(before, store.currentOrderedAggregate())
            }
        } finally {
            scope.cancel()
        }
    }

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

    private fun verifyDockReorder(
        full: Boolean = false,
        noOp: Boolean = false,
        fail: Boolean = false,
        busy: Boolean = false,
        cancel: Boolean = false,
        cancelSave: Boolean = false,
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val identities = (1..3).map {
            LaunchableIdentity(1, ComponentName("dock.app$it", "Main"))
        }
        val initialBar = HomeBottomAppBarState(
            enabled = true,
            identities = identities.take(2),
        )
        val file = files.newFolder().resolve("favorites.bin")
        val store = OrderedFavoriteStoreAdapter(file)
        val controlled = DockControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        runBlocking {
            store.load()
            checkNotNull(store.updateOrderedAggregate {
                it.copy(homeBottomAppBar = initialBar.copy(identities = identities))
            })
            check(editor.remove(identities.last()))
        }
        val undo = checkNotNull(editor.undoRemoval)
        val initialState = store.state.value
        val gate = CompletableDeferred<Unit>()
        controlled.gate = gate
        controlled.failWrites = fail
        controlled.writeAttempts = 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val coordinator = HomeCoordinator(context, scope, controlled, editor)
        var presentSaved by mutableStateOf(false)
        var requested: ApplicationOrderChange? = null
        var completions = 0
        val resources = context.resources
        fun dimension(id: Int) = resources.getDimension(id) / resources.displayMetrics.density
        val icon = dimension(initialBar.iconSize.iconSizeResource())
        val gap = dimension(R.dimen.home_bottom_app_bar_preview_icon_gap)
        val width = 2 * dimension(R.dimen.home_bottom_app_bar_outer_margin) +
            2 * dimension(R.dimen.home_bottom_app_bar_preview_horizontal_inset) +
            (if (full) 2 else 3) * (icon + gap) - gap + 0.5f
        try {
            composeRule.setContent {
                val reliable by store.state.collectAsState()
                val presented = if (presentSaved) reliable else initialState
                SideEffect { coordinator.updateFavoriteState(presented) }
                Launcher4MaxTheme {
                    Box(Modifier.width(width.dp)) {
                        HomeScreen(
                            favoriteState = presented,
                            editMode = true,
                            applicationEditingSaving = editor.isSaving,
                            onCommitApplicationOrder = { change, complete ->
                                requested = change
                                if (busy) {
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        editor.updateComposition { it }
                                    }
                                }
                                coordinator.commitApplicationOrder(change) {
                                    completions += 1
                                    complete()
                                }
                            },
                        )
                    }
                }
            }
            val dock = composeRule.onNodeWithTag("home_bottom_app_bar")
            val dockBounds = dock.fetchSemanticsNode().boundsInRoot
            val sourceTag = "home_bottom_app_bar_item:${identities.first().stableKey()}"
            val source = composeRule.onNodeWithTag(sourceTag)
            val start = source.fetchSemanticsNode().boundsInRoot.center - dockBounds.topLeft
            if (full) {
                composeRule.onNodeWithTag("home_bottom_app_bar_add").assertDoesNotExist()
            }
            dock.performTouchInput {
                down(start)
                moveTo(Offset(if (noOp) 1f else dockBounds.width - 1f, start.y))
            }
            val preview = composeRule.onNodeWithTag(
                "home_bottom_app_bar_drag_preview",
                useUnmergedTree = true,
            )
            val previewNodes = composeRule.onAllNodesWithTag(
                "home_bottom_app_bar_drag_preview",
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            fun diagnosticBounds(): String {
                fun bounds(tag: String): String = composeRule.onAllNodesWithTag(
                    tag,
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().map { it.boundsInRoot }.toString()
                val rootBounds = composeRule.onRoot(useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                return "noOp=$noOp, start=$start, initialDock=$dockBounds, " +
                    "root=$rootBounds, dock=${bounds("home_bottom_app_bar")}, " +
                    "source=${bounds(sourceTag)}, " +
                    "preview=${previewNodes.map { it.boundsInRoot }}"
            }
            assertTrue(
                "Dock preview is absent after horizontal movement; " +
                    "it was not created or was cleared. ${diagnosticBounds()}",
                previewNodes.isNotEmpty(),
            )
            try {
                preview.assertIsDisplayed()
            } catch (failure: AssertionError) {
                throw AssertionError(
                    "Dock preview exists but is not displayed. ${diagnosticBounds()}",
                    failure,
                )
            }
            dock.performTouchInput { if (cancel) this.cancel() else up() }
            if (noOp || cancel) {
                composeRule.runOnIdle {
                    assertNull(requested)
                    assertEquals(0, controlled.writeAttempts)
                    assertSame(undo, editor.undoRemoval)
                }
            } else if (busy) {
                composeRule.runOnIdle {
                    assertNotNull(requested)
                    assertTrue(editor.isSaving)
                    assertEquals(1, controlled.writeAttempts)
                    assertEquals(1, completions)
                    assertSame(undo, editor.undoRemoval)
                    gate.complete(Unit)
                }
            } else {
                composeRule.runOnIdle {
                    val change = checkNotNull(requested)
                    assertEquals(HOME_BOTTOM_APP_BAR_DESTINATION_ID, change.moduleId)
                    assertEquals(initialBar.identities, change.originalOrder)
                    assertEquals(1, change.boundary)
                    assertEquals(if (full) 2 else 3, change.barCapacity)
                    assertTrue(editor.isSaving)
                    assertEquals(0, completions)
                }
                preview.assertIsDisplayed()
                source.assertDoesNotExist()
                composeRule.runOnIdle {
                    if (cancelSave) scope.cancel() else gate.complete(Unit)
                }
                composeRule.waitUntil(5_000) { !editor.isSaving }
                if (!fail && !cancelSave) {
                    // The durable write is finished, but Home still presents the old state.
                    preview.assertIsDisplayed()
                    source.assertDoesNotExist()
                    composeRule.runOnIdle {
                        assertEquals(0, completions)
                        assertNull(editor.undoRemoval)
                        presentSaved = true
                    }
                }
                composeRule.waitUntil(5_000) { completions == 1 }
            }
            composeRule.waitUntil(5_000) { !editor.isSaving }
            preview.assertDoesNotExist()
            composeRule.onNodeWithTag(
                "home_application_movement_preview",
                useUnmergedTree = true,
            ).assertDoesNotExist()
            source.assertIsDisplayed()
            composeRule.onAllNodesWithTag(sourceTag).assertCountEquals(1)
            if (!noOp && !fail && !busy && !cancel && !cancelSave) {
                val second = composeRule.onNodeWithTag(
                    "home_bottom_app_bar_item:${identities[1].stableKey()}",
                ).fetchSemanticsNode().boundsInRoot
                assertTrue(source.fetchSemanticsNode().boundsInRoot.left > second.left)
            }
            composeRule.runOnIdle {
                assertFalse(editor.isSaving)
                if (fail || cancelSave) assertSame(undo, editor.undoRemoval)
            }
            runBlocking {
                store.load()
                val expected = if (noOp || fail || busy || cancel || cancelSave) {
                    initialBar.identities
                } else {
                    initialBar.identities.reversed()
                }
                assertEquals(
                    expected,
                    store.currentOrderedAggregate().homeBottomAppBar.identities,
                )
            }
        } finally {
            scope.cancel()
        }
    }

    private class DockControlledStore(
        private val delegate: FavoriteStore,
    ) : FavoriteStore by delegate {
        var gate: CompletableDeferred<Unit>? = null
        var failWrites = false
        var writeAttempts = 0

        override suspend fun updateOrderedAggregate(
            transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ): OrderedFavoriteAggregate? {
            writeAttempts += 1
            gate?.await()
            return if (failWrites) null else delegate.updateOrderedAggregate(transform)
        }
    }

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
