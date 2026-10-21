package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import com.maxarcher.launcher.ApplicationOrderChange
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.LaunchableIdentity

/** The viewport owns this node, so disposing a scrolled-off source cell cannot cancel its pointer. */
internal suspend fun PointerInputScope.detectHomeApplicationMovement(
    movement: HomeApplicationMovement,
    onStart: (LaunchableIdentity, Offset) -> Boolean,
    onRecognized: () -> Unit,
    onCommit: (ApplicationOrderChange) -> Unit,
) {
    awaitEachGesture(
        block = {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val identity = movement.hitIdentity(pointer = movement.viewport.topLeft + down.position)
                ?: return@awaitEachGesture
            val recognized = awaitApplicationLongPress(down = down) ?: return@awaitEachGesture
            if (!onStart(identity, movement.viewport.topLeft + recognized.position)) return@awaitEachGesture
            var handedOff = false
            try {
                recognized.consume()
                onRecognized()
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val owner = event.changes.firstOrNull(predicate = { it.id == down.id }) ?: break
                    // Additional pointers never take over, trigger controls, or affect the preview.
                    event.changes.forEach(action = { it.consume() })
                    movement.move(pointer = movement.viewport.topLeft + owner.position)
                    if (owner.changedToUpIgnoreConsumed()) {
                        movement.finish()?.let(block = { change ->
                            onCommit(change)
                            handedOff = true
                        })
                        break
                    }
                }
            } finally {
                if (!handedOff) movement.cancel()
            }
        },
    )
}

private suspend fun AwaitPointerEventScope.awaitApplicationLongPress(
    down: PointerInputChange,
): PointerInputChange? {
    var current = down
    return try {
        withTimeout(
            timeMillis = viewConfiguration.longPressTimeoutMillis,
            block = {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val owner = event.changes.firstOrNull(predicate = { it.id == down.id })
                        ?: return@withTimeout null
                    if (!owner.pressed || owner.isConsumed ||
                        (owner.position - down.position).getDistance() >= viewConfiguration.touchSlop
                    ) return@withTimeout null
                    current = owner
                    // Let the scrollable descendants inspect the sequence before granting ownership.
                    val finalEvent = awaitPointerEvent(pass = PointerEventPass.Final)
                    if (finalEvent.changes.firstOrNull(predicate = { it.id == down.id })?.isConsumed != false) {
                        return@withTimeout null
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                null
            },
        )
    } catch (_: PointerEventTimeoutCancellationException) {
        current
    }
}
