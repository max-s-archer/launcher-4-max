package com.maxarcher.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.maxarcher.launcher.ui.components.guardSurfaceMultiTouch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SurfaceMultiTouchGuardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun secondPointerBlocksUntilAllLiftAndNextSingleTouchPassesThrough() {
        var interruptions = 0
        val consumed = mutableListOf<Boolean>()
        composeRule.setContent {
            Box(Modifier.fillMaxSize().guardSurfaceMultiTouch { interruptions++ }) {
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val changes = awaitPointerEvent().changes
                                if (changes.any { it.pressed != it.previousPressed }) {
                                    consumed.add(changes.all { it.isConsumed })
                                }
                            }
                        }
                    },
                )
            }
        }

        composeRule.onRoot().performTouchInput {
            val secondPosition = center.copy(x = center.x / 2f)
            down(0, center)
            down(1, secondPosition)
            up(1)
            down(1, secondPosition)
            up(1)
            up(0)
            down(center)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(1, interruptions)
            assertEquals(listOf(false, true, true, true, true, true, false, false), consumed)
        }
    }
}
