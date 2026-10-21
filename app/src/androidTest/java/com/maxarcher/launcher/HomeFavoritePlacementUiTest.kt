package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.ui.home.components.HomeFavoriteGrid
import com.maxarcher.launcher.ui.home.components.HomeFavoriteItemFrame
import com.maxarcher.launcher.ui.home.components.HomeOrderedFavoriteRibbon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import com.maxarcher.launcher.ui.home.components.stableKey

class HomeFavoritePlacementUiTest {
    private var animationScale = 1f

    @get:Rule
    val composeRule = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float get() = animationScale
    })

    private val original = (1..3).map(transform = { index ->
        LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app$index", "Main"))
    })
    private var items by mutableStateOf(value = original)
    private var visible by mutableStateOf(value = true)
    private var containerOffset by mutableStateOf(value = 0.dp)
    private val compositionTokens = mutableMapOf<LaunchableIdentity, Any>()
    private val movement = HomeApplicationMovement()
    private var windowOrigin = Offset.Zero
    private var duration = 0

    @Test
    fun wrappedRemovalPreservesIdentityAndUsesTheVisibleAnimatedHitTarget() {
        showGrid()
        val before = bounds(index = 2)
        val destination = bounds(index = 1)
        val token = compositionTokens[original[2]]
        composeRule.runOnIdle(action = { items = original.drop(n = 1) })
        advanceIntoTransition()

        composeRule.onNodeWithTag(testTag = tag(index = 0)).assertDoesNotExist()
        val moving = bounds(index = 2)
        assertTrue(moving.left > before.left && moving.left < destination.left)
        assertTrue(moving.top < before.top && moving.top > destination.top)
        assertSame(token, compositionTokens[original[2]])
        composeRule.runOnIdle(action = {
            // The cells can cross during reflow; use the exposed bottom-right of this cell.
            assertEquals(original[2], movement.hitIdentity(pointer = moving.bottomRight - Offset(x = 1f, y = 1f) + windowOrigin))
        })

        settle()
        assertEquals(destination, bounds(index = 2))
    }

    @Test
    fun ribbonRemovalReportsTheCurrentPlacedCellRatherThanItsDestination() {
        composeRule.setContent(composable = {
            duration = integerResource(id = R.integer.short_property_animation_duration_ms)
            Launcher4MaxTheme(content = {
                Box(modifier = Modifier.width(width = 320.dp).onGloballyPositioned(
                    onGloballyPositioned = { coordinates ->
                        windowOrigin = coordinates.positionInWindow() - coordinates.positionInRoot()
                        movement.updateViewport(bounds = Rect(offset = coordinates.positionInWindow(), size = coordinates.size.toSize()))
                    },
                )) {
                    HomeOrderedFavoriteRibbon(
                        module = OrderedFavoriteModule(id = "ribbon", type = OrderedFavoriteModuleType.Ribbon, identities = items),
                        availabilityByIdentity = emptyMap(), listState = rememberLazyListState(),
                        editMode = true, showAddEntry = false, addEntryEnabled = false,
                        onAddToModule = {}, onLaunchFavorite = {}, onLongPressFavorite = {},
                        applicationMovement = movement,
                    )
                }
            })
        })
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        val destination = bounds(index = 0)
        val before = bounds(index = 1)
        composeRule.runOnIdle(action = { items = original.drop(n = 1) })
        advanceIntoTransition()
        val moving = bounds(index = 1)
        assertTrue(moving.left > destination.left && moving.left < before.left)
        composeRule.onNodeWithTag(testTag = tag(index = 0)).assertDoesNotExist()
        composeRule.runOnIdle(action = {
            assertEquals(original[1], movement.hitIdentity(pointer = moving.center + windowOrigin))
        })
        settle()
        assertEquals(destination, bounds(index = 1))
    }

    @Test
    fun anotherChangeRetargetsFromTheCurrentPositionWithoutRecreatingTheCell() {
        showGrid()
        val destination = bounds(index = 0)
        val token = compositionTokens[original[2]]
        composeRule.runOnIdle(action = { items = original.drop(n = 1) })
        advanceIntoTransition()
        val midway = bounds(index = 2)

        composeRule.runOnIdle(action = { items = listOf(element = original[2]) })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val retargeted = bounds(index = 2)
        // A new target must not snap either to the obsolete target or the new first-cell slot.
        assertTrue(abs(retargeted.left - midway.left) < midway.width / 4f)
        assertTrue(retargeted.left > destination.left)
        assertSame(token, compositionTokens[original[2]])
        settle()
        assertEquals(destination, bounds(index = 2))
    }

    @Test
    fun partialRowsKeepColumnWidthsAndReturningDoesNotReplayPlacement() {
        showGrid()
        assertEquals(bounds(index = 0).width, bounds(index = 2).width, 1f)
        val initial = bounds(index = 2)
        settle()
        assertEquals(initial, bounds(index = 2))
        composeRule.runOnIdle(action = { visible = false })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle(action = { visible = true })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertEquals(initial, bounds(index = 2))
        settle()
        assertEquals(initial, bounds(index = 2))
    }

    @Test
    fun parentTranslationDoesNotStartAnotherCellAnimation() {
        showGrid()
        val initial = bounds(index = 2)
        composeRule.runOnIdle(action = { containerOffset = 32.dp })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val translated = bounds(index = 2)
        assertEquals(initial.top + initial.height / 2, translated.top, 1f)
        settle()
        assertEquals(translated, bounds(index = 2))
    }

    @Test
    fun disabledSystemAnimationsKeepImmediateLayoutAndHitTargets() {
        animationScale = 0f
        showGrid()
        val destination = bounds(index = 1)
        composeRule.runOnIdle(action = { items = original.drop(n = 1) })
        repeat(times = 3, action = {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        })
        assertEquals(destination, bounds(index = 2))
        composeRule.runOnIdle(action = {
            assertEquals(original[2], movement.hitIdentity(pointer = destination.center + windowOrigin))
        })
    }

    private fun showGrid() {
        composeRule.setContent(composable = {
            duration = integerResource(id = R.integer.short_property_animation_duration_ms)
            Box(modifier = Modifier.offset(y = containerOffset).width(width = 240.dp).height(height = 400.dp).onGloballyPositioned(
                onGloballyPositioned = { coordinates ->
                    windowOrigin = coordinates.positionInWindow() - coordinates.positionInRoot()
                    movement.updateViewport(bounds = Rect(offset = coordinates.positionInWindow(), size = coordinates.size.toSize()))
                },
            )) {
                if (visible) {
                    HomeFavoriteGrid(
                        items = items, columns = 2, itemKey = { identity -> identity.stableKey() },
                        content = { identity ->
                            compositionTokens[identity] = remember(calculation = { Any() })
                            HomeFavoriteItemFrame(
                                modifier = Modifier.fillMaxWidth(), identity = identity, movement = movement,
                                showRemove = false, removeEnabled = false,
                                namePlacement = FavoriteNamePlacement.Right, iconSize = 24.dp, onRemove = {},
                                content = { Box(modifier = Modifier.fillMaxWidth().height(height = 64.dp)) },
                            )
                        },
                    )
                }
            }
        })
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun advanceIntoTransition() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(milliseconds = duration.toLong() / 2)
        composeRule.waitForIdle()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(milliseconds = duration.toLong() * 2)
        composeRule.waitForIdle()
    }

    private fun tag(index: Int): String = "home_favorite_item:${original[index].stableKey()}"

    private fun bounds(index: Int): Rect = composeRule.onNodeWithTag(testTag = tag(index = index)).fetchSemanticsNode().boundsInRoot
}
