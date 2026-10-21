package com.maxarcher.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.maxarcher.launcher.ui.drawer.drawerDragToFavoriteDetection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawerDragToFavoriteDetectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val releasedCount = AtomicInteger(0)
    private val dragStarts = AtomicInteger(0)
    private val moves = AtomicInteger(0)
    private val dragEnds = AtomicInteger(0)
    private val lastEndCancelled = AtomicBoolean(false)

    private fun setContent(startResult: Boolean = true) {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .requiredSize(width = 200.dp, height = 200.dp)
                    .testTag("detection_target")
                    .drawerDragToFavoriteDetection(
                        key1 = Unit,
                        onReleaseAfterLongPress = { releasedCount.incrementAndGet() },
                        onDragBeyondSlop = { dragStarts.incrementAndGet(); startResult },
                        onDragMove = { moves.incrementAndGet() },
                        onDragEnd = { _, cancelled ->
                            dragEnds.incrementAndGet()
                            lastEndCancelled.set(cancelled)
                        },
                    ),
            )
        }
    }

    @Test
    fun releasingAfterLongPressWithoutSlopOpensTheActionSheet() {
        setContent()

        composeRule.onNodeWithTag("detection_target").performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        assertEquals(1, releasedCount.get())
        assertEquals(0, dragStarts.get())
        assertEquals(0, dragEnds.get())
    }

    @Test
    fun longPressFollowedBySlopCrossingStartsThenEndsTheDrag() {
        setContent()

        composeRule.onNodeWithTag("detection_target").performTouchInput {
            down(center)
            // A sub-slop hold advances the event clock past the long-press timeout
            // without lifting the injected pointer or crossing the touch slop.
            moveBy(Offset(x = 0.5f, y = 0f), delayMillis = 600)
            // The crossing event itself is the drag dispatch; the next move is the
            // first tracked drag move.
            moveBy(Offset(x = 120f, y = 0f))
            moveBy(Offset(x = 20f, y = 0f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, dragStarts.get())
        assertEquals(1, moves.get())
        assertEquals(1, dragEnds.get())
        assertTrue(lastEndCancelled.get().not())
    }

    @Test
    fun rejectedStartKeepsTheReleaseBehavior() {
        setContent(startResult = false)

        composeRule.onNodeWithTag("detection_target").performTouchInput {
            down(center)
            // A sub-slop hold advances the event clock past the long-press timeout
            // without lifting the injected pointer or crossing the touch slop.
            moveBy(Offset(x = 0.5f, y = 0f), delayMillis = 600)
            moveBy(Offset(x = 120f, y = 0f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(1, dragStarts.get())
        assertEquals(0, moves.get())
        assertEquals(1, releasedCount.get())
        assertEquals(0, dragEnds.get())
    }
}
