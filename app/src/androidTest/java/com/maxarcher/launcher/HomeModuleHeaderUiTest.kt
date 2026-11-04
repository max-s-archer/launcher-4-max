package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeModuleHeaderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val first = module(id = "first", serial = 1)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val undoLabel get() = context.getString(R.string.undo)
    private val second = module(
        id = "second",
        serial = 2,
        type = OrderedFavoriteModuleType.Ribbon,
    )

    @Test
    fun everyModuleShowsOneFortyDpHeaderInTheContractedOrder() {
        show(aggregate(first, second))

        listOf(first, second).forEach { module ->
            composeRule.onNodeWithTag("home_module_header_${module.id}")
                .assertHeightIsEqualTo(40.dp)
            val order = listOf("remove", "name", "name_visibility", "style", "reorder").map {
                composeRule.onNodeWithTag("home_module_${it}_${module.id}")
                    .getBoundsInRoot().left
            }
            assertEquals(order.sorted(), order)
        }
    }

    @Test
    fun styleAffordanceIsTheOnlyPanelToggleAndOneModuleIsOpenAtATime() {
        var open by mutableStateOf<String?>(null)
        show(aggregate(first, second), openModuleId = { open }, onToggle = { id ->
            open = id.takeUnless { it == open }
        })

        composeRule.onNodeWithTag("home_style_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("home_module_style_first").performClick()
        composeRule.onNodeWithTag("home_style_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("home_module_selected").assertIsDisplayed()

        composeRule.onNodeWithTag("home_module_style_second").performClick()
        composeRule.runOnIdle { assertEquals("second", open) }

        composeRule.onNodeWithTag("home_module_style_second").performClick()
        composeRule.runOnIdle { assertEquals(null, open) }
        composeRule.onNodeWithTag("home_module_selected").assertDoesNotExist()
    }

    @Test
    fun openingThePanelPreservesTheMainListScrollPosition() {
        val modules = (1..12).map { module(id = "module-$it", serial = it.toLong()) }
        val listState = LazyListState(firstVisibleItemIndex = 4)
        var open by mutableStateOf<String?>(null)
        show(
            aggregate(*modules.toTypedArray()),
            listState = listState,
            openModuleId = { open },
            onToggle = { id -> open = id.takeUnless { it == open } },
        )

        composeRule.onNodeWithTag("home_module_style_module-5").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(4, listState.firstVisibleItemIndex) }
    }

    @Test
    fun nameChangesAndVisibilitySaveTheCompleteModuleState() {
        var persisted = aggregate(first)
        show(persisted, onCommit = { transform -> transform(persisted).also { persisted = it } })

        composeRule.onNodeWithTag("home_module_name_first").performTextReplacement("Work")
        composeRule.runOnIdle { assertEquals("Work", persisted.modules.single().name) }

        composeRule.onNodeWithTag("home_module_name_visibility_first").performClick()
        composeRule.runOnIdle { assertTrue(persisted.modules.single().nameVisible) }
    }

    @Test
    fun failedNameSaveRestoresTheLastSavedNameInTheActiveField() {
        var attempts = 0
        show(aggregate(first.copy(name = "Saved")), onCommit = {
            attempts += 1
            null
        })

        composeRule.onNodeWithTag("home_module_name_first").performTextReplacement("Lost")
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(attempts > 0) }
        composeRule.onNodeWithTag("home_module_name_first").assertTextContains("Saved")
    }

    @Test
    fun headerActionsAreUnavailableWhileAnExclusiveSaveIsUnresolved() {
        show(aggregate(first), applicationEditingSaving = true)

        composeRule.onNodeWithTag("home_module_remove_first").assertIsNotEnabled()
        composeRule.onNodeWithTag("home_module_style_first").assertIsNotEnabled()
        composeRule.onNodeWithTag("home_module_name_visibility_first").assertIsNotEnabled()
    }

    @Test
    fun removingAWholeModuleOffersUndoAndRestoresTheCompleteModule() {
        val original = first.copy(name = "Work", nameVisible = true)
        var persisted = aggregate(original, second)
        show(
            aggregate = persisted,
            onCommit = { transform ->
                transform(persisted).also { persisted = it }
            },
        )

        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(listOf("second"), persisted.modules.map { it.id })
        }
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(listOf(original, second), persisted.modules)
        }
    }

    @Test
    fun wholeModuleUndoRemainsAvailableAfterLeavingEditMode() {
        var editing by mutableStateOf(true)
        var persisted = aggregate(first, second)
        show(
            aggregate = persisted,
            editMode = { editing },
            onCommit = { transform ->
                transform(persisted).also { persisted = it }
            },
        )

        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(second), persisted.modules)
        }
        composeRule.runOnIdle { editing = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(listOf("first", "second"), persisted.modules.map { it.id })
        }
    }

    @Test
    fun failedWholeModuleRemovalKeepsTheSavedModulesWithoutUndo() {
        val persisted = aggregate(first, second)
        show(
            aggregate = persisted,
            onCommit = { null },
        )

        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(listOf("first", "second"), persisted.modules.map { it.id })
        }
        composeRule.onNodeWithText(undoLabel).assertDoesNotExist()
    }

    @Test
    fun laterWholeRemovalReplacesTheSnackbarAndRestoresOnlyTheLatestModule() {
        show(aggregate(first, second))
        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed()
        composeRule.onNodeWithTag("home_module_remove_second").performClick()
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("home_module_header_first").assertDoesNotExist()
        composeRule.onNodeWithTag("home_module_header_second").assertIsDisplayed()
    }

    @Test
    fun unrelatedNameChangePreservesUndoAndIsNotRolledBackByRestoration() {
        show(aggregate(first, second))
        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.onNodeWithTag("home_module_name_second").performTextReplacement("Updated")
        composeRule.onNodeWithText(undoLabel).assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("home_module_header_first").assertIsDisplayed()
        composeRule.onNodeWithTag("home_module_name_second").assertTextContains("Updated")
    }

    @Test
    fun dismissingTheWholeRemovalSnackbarDiscardsUndoWithoutRestoringTheModule() {
        show(aggregate(first, second))
        composeRule.onNodeWithTag("home_module_remove_first").performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_removal_dismiss))
            .performClick()

        composeRule.onNodeWithText(undoLabel).assertDoesNotExist()
        composeRule.onNodeWithTag("home_module_header_first").assertDoesNotExist()
    }

    @Test
    fun displayModePreviewsDuringSaveAndPersistsOnlyItsOwningModuleStyle() {
        val original = second.copy(name = "Saved ribbon", nameVisible = true)
        val initial = aggregate(first, original)
        var persisted = initial
        var attempts = 0
        val gate = CompletableDeferred<Unit>()
        show(
            initial,
            openModuleId = { second.id },
            onCommit = { transform ->
                attempts++
                gate.await()
                transform(persisted).also { persisted = it }
            },
        )
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").performClick()
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").assertIsSelected()
        composeRule.onNodeWithTag("home_ribbon_display_mode_0").assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag("home_module_remove_first").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(initial, persisted)
            assertEquals(1, attempts)
            gate.complete(Unit)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").assertIsSelected().assertIsEnabled()
        composeRule.runOnIdle {
            assertEquals(
                aggregate(first, original.copy(displayMode = OrderedFavoriteDisplayMode.Flow)),
                persisted,
            )
            assertEquals(1, attempts)
        }
    }

    @Test
    fun failedDisplayModeSaveRestoresTheSelectorAndAllowsExplicitRetry() {
        val initial = aggregate(first, second)
        var persisted = initial
        var attempts = 0
        show(
            initial,
            openModuleId = { second.id },
            onCommit = { transform ->
                attempts++
                if (attempts == 1) null else transform(persisted).also { persisted = it }
            },
        )
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_ribbon_display_mode_0").assertIsSelected()
        composeRule.onNodeWithTag("home_style_panel").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(initial, persisted)
            assertEquals(1, attempts)
        }
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(OrderedFavoriteDisplayMode.Flow, persisted.modules.last().displayMode)
            assertEquals(2, attempts)
        }
    }

    @Test
    fun closingThePanelDuringDisplayModeSaveDoesNotReopenItOrCancelTheSave() {
        val initial = aggregate(second)
        var persisted = initial
        var open by mutableStateOf<String?>(second.id)
        val gate = CompletableDeferred<Unit>()
        show(
            initial,
            openModuleId = { open },
            onCommit = { transform ->
                gate.await()
                transform(persisted).also { persisted = it }
            },
        )
        composeRule.onNodeWithTag("home_ribbon_display_mode_1").performClick()
        composeRule.runOnIdle { open = null }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_style_panel").assertDoesNotExist()
        composeRule.runOnIdle { gate.complete(Unit) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_style_panel").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(OrderedFavoriteDisplayMode.Flow, persisted.modules.single().displayMode)
        }
    }

    private fun show(
        aggregate: OrderedFavoriteAggregate,
        listState: LazyListState = LazyListState(),
        editMode: () -> Boolean = { true },
        openModuleId: () -> String? = { null },
        onToggle: (String) -> Unit = {},
        applicationEditingSaving: Boolean = false,
        onCommit: (suspend (
            (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ) -> OrderedFavoriteAggregate?)? = null,
    ) {
        val store = HeaderTestStore(aggregate, onCommit)
        val editor = HomeFavoriteEditor(store)
        val hostState = SnackbarHostState()
        composeRule.setContent {
            Launcher4MaxTheme {
                val presentedState by store.state.collectAsState()
                val scope = rememberCoroutineScope()
                val editing = editMode()
                LaunchedEffect(editing) {
                    if (!editing) editor.invalidateApplicationUndo()
                }
                HomeRemovalFeedback(editor, hostState)
                HomeScreen(
                    favoriteState = presentedState,
                    favoriteListState = listState,
                    editMode = editing,
                    openStyleModuleId = openModuleId(),
                    onToggleModuleStylePanel = onToggle,
                    applicationEditingSaving = applicationEditingSaving || editor.isSaving,
                    removalSnackbarHostState = hostState,
                    onRemoveModule = { id -> scope.launch { editor.removeModule(id) } },
                    onRemoveApplication = { id -> scope.launch { editor.remove(id) } },
                    onCommitFavoriteComposition = editor::updateComposition,
                    onCommitModuleOrder = editor::reorderModules,
                )
            }
        }
    }

    /** Publishes successful writes like the application store, including removal and Undo. */
    private class HeaderTestStore(
        initial: OrderedFavoriteAggregate,
        val commit: (suspend (
            (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ) -> OrderedFavoriteAggregate?)?,
    ) : FavoriteStore {
        override val state = MutableStateFlow<FavoriteReadState>(readable(initial))

        override suspend fun load() = Unit
        override suspend fun add(identity: LaunchableIdentity): Boolean = error("Not used")
        override suspend fun remove(identity: LaunchableIdentity): Boolean = error("Not used")
        override suspend fun removeAll(identities: Set<LaunchableIdentity>): Boolean =
            error("Not used")

        override suspend fun updateOrderedAggregate(
            transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ): OrderedFavoriteAggregate? {
            val result = if (commit != null) {
                commit.invoke(transform)
            } else {
                val current = (state.value as FavoriteReadState.Readable).orderedAggregate
                transform(checkNotNull(current))
            }
            return result?.also { state.value = readable(it) }
        }

        companion object {
            private fun readable(value: OrderedFavoriteAggregate) = FavoriteReadState.Readable(
                aggregate = value.toLegacyAggregate(),
                orderedModules = value.modules,
                orderedAggregate = value,
            )
        }
    }

    private fun aggregate(vararg modules: OrderedFavoriteModule) =
        OrderedFavoriteAggregate(modules = modules.toList())

    private fun module(
        id: String,
        serial: Long,
        type: OrderedFavoriteModuleType = OrderedFavoriteModuleType.Vertical,
    ) = OrderedFavoriteModule(
        id = id,
        type = type,
        identities = listOf(
            LaunchableIdentity(serial, ComponentName("com.example.$serial", "Main")),
        ),
    )
}
