package com.maxarchm.launcher.ui.home.components

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlinx.coroutines.withTimeout

/**
 * Waits for the platform long-press timeout without consuming the initial press. This keeps
 * scrolling available until the gesture has actually become a reorder drag.
 */
internal suspend fun AwaitPointerEventScope.awaitHomeHandleLongPress(
    down: PointerInputChange,
): PointerInputChange? {
    var current = down
    return try {
        withTimeout(
            timeMillis = viewConfiguration.longPressTimeoutMillis,
            block = {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.changes.any(predicate = { it.id != down.id && it.pressed })) {
                        return@withTimeout null
                    }
                    val change = event.changes.firstOrNull(predicate = { it.id == down.id })
                        ?: return@withTimeout null
                    if (change.changedToUpIgnoreConsumed()) return@withTimeout null
                    if ((change.position - down.position).getDistance() >=
                        viewConfiguration.touchSlop
                    ) {
                        return@withTimeout null
                    }
                    current = change
                }
                @Suppress("UNREACHABLE_CODE")
                null
            },
        )
    } catch (_: PointerEventTimeoutCancellationException) {
        current
    }
}
