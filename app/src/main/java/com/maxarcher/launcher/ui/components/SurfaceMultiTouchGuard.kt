package com.maxarcher.launcher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** A second pointer notifies the surface once and blocks input until all pointers are up. */
internal fun Modifier.guardSurfaceMultiTouch(onMultiplePointers: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var multiplePointersDetected = false
            var pointersRemainPressed: Boolean
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (!multiplePointersDetected && event.changes.count { it.pressed } > 1) {
                    multiplePointersDetected = true
                    onMultiplePointers()
                }
                if (multiplePointersDetected) {
                    event.changes.forEach { change -> change.consume() }
                }
                pointersRemainPressed = event.changes.any { it.pressed }
            } while (pointersRemainPressed)
        }
    }
