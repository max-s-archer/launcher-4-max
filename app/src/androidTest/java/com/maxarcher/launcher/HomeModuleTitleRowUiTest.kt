package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.maxarcher.launcher.ui.home.components.HomeOrderedModuleComposition
import org.junit.Rule
import org.junit.Test

class HomeModuleTitleRowUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun hiddenNameAddsNoTitleRow() {
        show(module(id = "list", name = "Work", nameVisible = false))

        composeRule.onNodeWithTag("home_module_title_list").assertDoesNotExist()
        composeRule.onNodeWithText("Work").assertDoesNotExist()
    }

    @Test
    fun shownNameUsesOneNonInteractiveFortyDpRow() {
        show(module(id = "list", name = "Work", nameVisible = true))

        composeRule.onNodeWithTag("home_module_title_list")
            .assertHeightIsEqualTo(40.dp)
            .assertHasNoClickAction()
        composeRule.onNodeWithText("Work").assertExists()
    }

    @Test
    fun unnamedModulesUseTheirStableDefaultLabels() {
        show(
            module(id = "list", nameVisible = true),
            module(
                id = "ribbon",
                type = OrderedFavoriteModuleType.Ribbon,
                serial = 2,
                nameVisible = true,
            ),
        )

        composeRule.onNodeWithText(
            resources.getString(R.string.home_module_default_name_list),
        ).assertExists()
        composeRule.onNodeWithText(
            resources.getString(R.string.home_module_default_name_ribbon),
        ).assertExists()
    }

    @Test
    fun editModeDoesNotShowTheOrdinaryTitleRow() {
        show(module(id = "list", name = "Work", nameVisible = true), editMode = true)

        composeRule.onNodeWithTag("home_module_title_list").assertDoesNotExist()
    }

    private fun show(vararg modules: OrderedFavoriteModule, editMode: Boolean = false) {
        composeRule.setContent {
            Launcher4MaxTheme {
                HomeOrderedModuleComposition(
                    modules = modules.toList(),
                    availabilityByIdentity = emptyMap(),
                    listState = rememberLazyListState(),
                    nestedScrollConnection = null,
                    editMode = editMode,
                    addEntriesEnabled = false,
                    onAddToModule = {},
                    onCreateVerticalModule = {},
                    onCreateRibbon = {},
                    onLaunchFavorite = {},
                    onLongPressFavorite = {},
                )
            }
        }
    }

    private fun module(
        id: String,
        type: OrderedFavoriteModuleType = OrderedFavoriteModuleType.Vertical,
        serial: Long = 1,
        name: String = "",
        nameVisible: Boolean,
    ) = OrderedFavoriteModule(
        id = id,
        type = type,
        identities = listOf(
            LaunchableIdentity(serial, ComponentName("com.example.$serial", "Main")),
        ),
        name = name,
        nameVisible = nameVisible,
    )
}
