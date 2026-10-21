package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarcher.launcher.ui.home.components.HomeFavoriteExitOverlay
import com.maxarcher.launcher.ui.home.components.HomeFavoriteExitTransitions
import com.maxarcher.launcher.ui.home.components.homeFavoriteEnter
import com.maxarcher.launcher.ui.home.components.rememberHomeFavoriteEnterBatch
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.maxarcher.launcher.ui.home.components.stableKey

class HomeFavoriteEnterUiTest {
    private var scale = 1f
    @get:Rule
    val composeRule = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float get() = scale
    })

    private val a = identity(index = 0)
    private val b = identity(index = 1)
    private val first = module(id = "one", identities = listOf(element = a))
    private var modules by mutableStateOf(value = listOf(element = first))
    private var showContent by mutableStateOf(value = true)
    private var readable by mutableStateOf(value = true)
    private var mounted by mutableStateOf(value = true)
    private var duration = 0
    private var scrollTo: (Int) -> Unit = {}
    private lateinit var exits: HomeFavoriteExitTransitions
    private lateinit var batch: HomeFavoriteEnterBatch

    @Test
    fun removalLeavesOnlyDrawingWhileTheRealApplicationIsAlreadyGone() {
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        val oldBounds = composeRule.onNodeWithTag(testTag = b.stableKey()).fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        composeRule.onNodeWithTag(testTag = b.stableKey()).assertDoesNotExist()
        composeRule.runOnIdle(action = { assertEquals(1, exits.ghosts.size) })
        settle()
        assertEquals(0f, sceneBrightness(bounds = oldBounds), 0.02f)
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.isEmpty()) })
    }

    @Test
    fun removingTheLastApplicationFadesOneModuleAndReleasesItsLayer() {
        show()
        composeRule.runOnIdle(action = { modules = emptyList() })
        advanceHalfway()
        composeRule.onNodeWithTag(testTag = a.stableKey()).assertDoesNotExist()
        val record = composeRule.runOnIdle(action = {
            assertEquals(1, exits.ghosts.size)
            exits.ghosts.single().record.also(block = { assertEquals(HomeFavoriteEnterKey(moduleId = "one"), it.key) })
        })
        settle()
        composeRule.runOnIdle(action = { assertTrue(record.layer.isReleased) })
    }

    @Test
    fun deletingTheRemainingModuleReplacesItsEarlierChildRemnant() {
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        composeRule.runOnIdle(action = {
            assertEquals(1, exits.ghosts.size)
            modules = emptyList()
        })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val parent = composeRule.runOnIdle(action = {
            assertEquals(1, exits.ghosts.size)
            exits.ghosts.single().record.also(block = {
                assertEquals(HomeFavoriteEnterKey(moduleId = "one"), it.key)
                assertTrue(!it.layer.isReleased)
            })
        })
        settle()
        composeRule.runOnIdle(action = {
            assertTrue(exits.ghosts.isEmpty())
            assertTrue(parent.layer.isReleased)
        })
    }

    @Test
    fun quickUndoReplacesTheRemnantAndContinuesFromItsVisibleOpacity() {
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        val priorAlpha = composeRule.runOnIdle(action = { exits.ghosts.single().alpha.value })
        composeRule.runOnIdle(action = { modules = listOf(element = first.copy(identities = listOf(a, b))) })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.isEmpty()) })
        val restored = composeRule.runOnIdle(action = {
            batch.tickets.single(predicate = { it.key.identity == b }).opacity
        })
        assertTrue(restored >= priorAlpha - 0.03f)
        settle()
        assertEquals(1f, brightness(tag = b.stableKey()), 0.02f)
    }

    @Test
    fun movingAnExistingIdentityToANewModuleCreatesNoExitRemnant() {
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        composeRule.runOnIdle(action = {
            modules = listOf(first, module(id = "new", identities = listOf(element = b)))
        })
        advanceHalfway()
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.isEmpty()) })
        assertEquals(1f, brightness(tag = b.stableKey()), 0.02f)
    }

    @Test
    fun disabledAnimationsRemoveTheLastModuleWithoutARemnant() {
        scale = 0f
        show()
        composeRule.runOnIdle(action = { modules = emptyList() })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(testTag = a.stableKey()).assertDoesNotExist()
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.isEmpty()) })
    }

    @Test
    fun unreadableStateClearsTheRemnantAndRecoveryDoesNotReplayAnEntry() {
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        val record = composeRule.runOnIdle(action = { exits.ghosts.single().record })
        composeRule.runOnIdle(action = { readable = false })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        // Lazy disposal may follow the membership SideEffect; detached captures get one
        // additional frame for cleanup, independently of the unfinished fade duration.
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle(action = {
            assertTrue(exits.ghosts.isEmpty())
            assertTrue(record.layer.isReleased)
            readable = true
        })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertEquals(1f, brightness(tag = a.stableKey()), 0.02f)
        composeRule.onNodeWithTag(testTag = b.stableKey()).assertDoesNotExist()
    }

    @Test
    fun disposingTheOwnerReleasesAnUnfinishedModuleExit() {
        show()
        composeRule.runOnIdle(action = { modules = emptyList() })
        advanceHalfway()
        val owner = exits
        val record = composeRule.runOnIdle(action = { owner.ghosts.single().record })
        composeRule.runOnIdle(action = { mounted = false })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle(action = {
            assertTrue(owner.ghosts.isEmpty())
            assertTrue(record.layer.isReleased)
        })
    }

    @Test
    fun anotherAdditionCanEnterWhileThePreviousRemovalIsStillFading() {
        val c = identity(index = 2)
        modules = listOf(element = first.copy(identities = listOf(a, b)))
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        val remnant = composeRule.runOnIdle(action = { exits.ghosts.single() })
        composeRule.runOnIdle(action = {
            modules = listOf(element = first.copy(identities = listOf(a, c)))
        })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(testTag = b.stableKey()).assertDoesNotExist()
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.single() === remnant) })
        assertEquals(1f, brightness(tag = a.stableKey()), 0.02f)
        settle()
        assertEquals(1f, brightness(tag = c.stableKey()), 0.02f)
        composeRule.runOnIdle(action = { assertTrue(exits.ghosts.isEmpty()) })
    }

    @Test
    fun existingModuleAdditionFadesOnlyTheNewApplication() {
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first.copy(identities = listOf(a, b))) })
        advanceHalfway()
        assertEquals(1f, brightness(tag = a.stableKey()), 0.02f)
        settle()
        assertEquals(1f, brightness(tag = b.stableKey()), 0.02f)
    }

    @Test
    fun restoredModuleUsesOneFadeForBothItsMarkerAndItsApplication() {
        modules = emptyList()
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first) })
        advanceHalfway()
        val parent = brightness(tag = "marker:one")
        assertEquals(parent, brightness(tag = a.stableKey()), 0.02f)
        settle()
        assertEquals(1f, brightness(tag = a.stableKey()), 0.02f)
    }

    @Test
    fun returningAndScrollingToPreviouslyOffscreenAdditionsDoNotReplayFades() {
        show()
        composeRule.runOnIdle(action = {
            modules = (0..8).map(transform = { index -> module(id = if (index == 0) "one" else "module$index", identities = listOf(element = identity(index = index))) })
        })
        settle()
        composeRule.runOnIdle(action = { scrollTo(8) })
        composeRule.waitForIdle()
        assertEquals(1f, brightness(tag = identity(index = 8).stableKey()), 0.02f)
        composeRule.runOnIdle(action = { showContent = false })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle(action = { showContent = true })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertEquals(1f, brightness(tag = identity(index = 8).stableKey()), 0.02f)
    }

    @Test
    fun disabledAnimationsNeverHideANewApplication() {
        scale = 0f
        show()
        composeRule.runOnIdle(action = { modules = listOf(element = first.copy(identities = listOf(a, b))) })
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertEquals(1f, brightness(tag = b.stableKey()), 0.02f)
    }

    private fun show() {
        composeRule.setContent(composable = {
            if (!mounted) return@setContent
            duration = integerResource(id = R.integer.short_property_animation_duration_ms)
            val batch = rememberHomeFavoriteEnterBatch(modules = modules.takeIf(predicate = { readable }))
            this@HomeFavoriteEnterUiTest.batch = batch
            exits = checkNotNull(value = batch.exitTransitions)
            var rootOrigin by remember(calculation = { mutableStateOf(value = Offset.Zero) })
            val state = rememberLazyListState()
            val scope = rememberCoroutineScope()
            scrollTo = { index -> scope.launch(block = { state.scrollToItem(index = index) }) }
            Box(modifier = Modifier.width(width = 240.dp).height(height = 240.dp).background(color = Color.Black)
                .testTag(tag = "scene").onGloballyPositioned(onGloballyPositioned = { rootOrigin = it.positionInWindow() })) {
                if (showContent && readable) {
                    LazyColumn(state = state, content = {
                        items(items = modules, key = { it.id }, itemContent = { module ->
                            Column(modifier = Modifier.homeFavoriteEnter(batch = batch, key = HomeFavoriteEnterKey(moduleId = module.id))) {
                                Box(modifier = Modifier.width(width = 100.dp).height(height = 16.dp).background(color = Color.White).testTag(tag = "marker:${module.id}"))
                                module.identities.forEach(action = { identity ->
                                    key(identity) {
                                        Box(modifier = Modifier
                                            .homeFavoriteEnter(batch = batch, key = HomeFavoriteEnterKey(moduleId = module.id, identity = identity))
                                            .width(width = 100.dp).height(height = 48.dp)
                                            .background(color = Color.White).testTag(tag = identity.stableKey()))
                                    }
                                })
                            }
                        })
                    })
                }
                HomeFavoriteExitOverlay(owner = exits, rootOrigin = rootOrigin)
            }
        })
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        assertEquals(1f, if (modules.isEmpty()) 1f else brightness(tag = a.stableKey()), 0.02f)
    }

    private fun advanceHalfway() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(milliseconds = duration.toLong() / 2)
        composeRule.waitForIdle()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(milliseconds = duration.toLong() * 2)
        composeRule.waitForIdle()
    }

    private fun brightness(tag: String): Float {
        val pixels = composeRule.onNodeWithTag(testTag = tag).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2].red
    }

    private fun sceneBrightness(bounds: Rect): Float {
        val scene = composeRule.onNodeWithTag(testTag = "scene")
        val origin = scene.fetchSemanticsNode().boundsInRoot.topLeft
        val pixels = scene.captureToImage().toPixelMap()
        val point = bounds.center - origin
        return pixels[point.x.toInt(), point.y.toInt()].red
    }

    private fun identity(index: Int): LaunchableIdentity = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app$index", "Main"))
    private fun module(id: String, identities: List<LaunchableIdentity>): OrderedFavoriteModule = OrderedFavoriteModule(id = id, type = OrderedFavoriteModuleType.Vertical, identities = identities)
}
