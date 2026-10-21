package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.maxarcher.launcher.ui.home.components.HomeMainListAddFavoriteEntry
import com.maxarcher.launcher.ui.home.components.HomeModuleAddFavoriteEntry
import com.maxarcher.launcher.ui.home.components.homeEditSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeAddFavoriteEntryUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun verticalEntryPreservesTheParentSurfaceWithoutAddingFillOrBorder() = verifySurface()

    @Test
    fun ribbonEntryKeepsItsFillAndBorder() = verifySurface(ribbon = true)

    @Test
    fun mainCreationEntryUsesOneEditBackgroundLayerAndRetainsItsBorder() = verifySurface(main = true)

    @Test
    fun disabledVerticalEntryDoesNotDimOrClearTheParentBackground() = verifySurface(enabled = false)

    private fun verifySurface(ribbon: Boolean = false, main: Boolean = false, enabled: Boolean = true) {
        val module = OrderedFavoriteModule(
            id = "test", type = if (ribbon) OrderedFavoriteModuleType.Ribbon else OrderedFavoriteModuleType.Vertical,
            identities = listOf(element = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app", "Main"))),
        )
        var expected = Color.Unspecified
        val tag = if (main) "creation" else "home_add_favorite_test"
        composeRule.setContent(composable = {
            Launcher4MaxTheme(content = {
                expected = colorResource(id = if (ribbon) R.color.home_add_favorite_surface else R.color.home_edit_surface)
                    .compositeOver(background = Color.Black)
                Box(modifier = Modifier.background(color = Color.Black)) {
                    Box(modifier = Modifier.homeEditSurface(enabled = !ribbon && !main)) {
                        if (main) {
                            HomeMainListAddFavoriteEntry(
                                modifier = Modifier.width(width = 200.dp), label = "Create", testTag = tag,
                                enabled = enabled, onClick = {},
                            )
                        } else {
                            HomeModuleAddFavoriteEntry(
                                modifier = Modifier.width(width = 200.dp), module = module, enabled = enabled, onClick = {},
                            )
                        }
                    }
                }
            })
        })
        val node = composeRule.onNodeWithTag(testTag = tag)
        if (!enabled) node.assertIsNotEnabled()
        val pixels = node.captureToImage().toPixelMap()
        // Sample above the icon/label, away from the rounded ends and the border itself.
        val fill = pixels[pixels.width / 2, pixels.height / 10]
        assertEquals(expected.red, fill.red, 0.02f)
        assertEquals(expected.green, fill.green, 0.02f)
        assertEquals(expected.blue, fill.blue, 0.02f)
        val topEdge = pixels[pixels.width / 2, 0]
        if (ribbon || main) {
            assertTrue(topEdge.red > fill.red)
        } else {
            assertEquals(fill.red, topEdge.red, 0.02f)
        }
    }
}
