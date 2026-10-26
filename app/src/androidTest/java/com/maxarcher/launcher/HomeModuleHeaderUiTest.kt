package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeModuleHeaderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val first = module(id = "first", serial = 1)
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
        composeRule.onNodeWithText("Undo").performClick()
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
        composeRule.runOnIdle { editing = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Undo").performClick()
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
        composeRule.onNodeWithText("Undo").assertDoesNotExist()
    }

    private fun show(
        aggregate: OrderedFavoriteAggregate,
        listState: LazyListState = LazyListState(),
        editMode: () -> Boolean = { true },
        openModuleId: () -> String? = { null },
        onToggle: (String) -> Unit = {},
        applicationEditingSaving: Boolean = false,
        onCommit: suspend (
            (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ) -> OrderedFavoriteAggregate? = { transform -> transform(aggregate) },
    ) {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeScreen(
                    favoriteState = FavoriteReadState.Readable(
                        aggregate = aggregate.toLegacyAggregate(),
                        orderedModules = aggregate.modules,
                        orderedAggregate = aggregate,
                    ),
                    favoriteListState = listState,
                    editMode = editMode(),
                    openStyleModuleId = openModuleId(),
                    onToggleModuleStylePanel = onToggle,
                    applicationEditingSaving = applicationEditingSaving,
                    onCommitFavoriteComposition = onCommit,
                )
            }
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
