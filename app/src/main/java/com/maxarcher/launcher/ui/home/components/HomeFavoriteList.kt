package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.testTag
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.LaunchableIdentity

@Composable
internal fun HomeFavoriteList(
    modifier: Modifier,
    identities: List<LaunchableIdentity>,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection?,
    compact: Boolean,
    listSize: FavoriteListSize? = null,
    onLaunchFavorite: (FavoriteAvailability) -> Unit,
    onLongPressFavorite: (LaunchableEntry) -> Unit,
    testTag: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    nestedScrollConnection?.let { Modifier.nestedScroll(it) } ?: Modifier,
                )
                .testTag(testTag ?: if (compact) "home_companion_favorites" else "home_favorites"),
            state = listState,
        ) {
            itemsIndexed(
                items = identities,
                key = { _, identity -> identity.stableKey() },
            ) { _, identity ->
                val availability = availabilityByIdentity[identity]
                    ?: FavoriteAvailability.Unknown(null)
                val entry = availability.presentationEntry
                HomeFavoriteRow(
                    modifier = Modifier.animateItem(placementSpec = null),
                    availability = availability,
                    onClick = { onLaunchFavorite(availability) },
                    onLongClick = {
                        if (entry != null) onLongPressFavorite(entry)
                    },
                    compact = compact,
                    listSize = listSize,
                    editMode = false,
                    exchangeHighlight = false,
                    onRowBoundsInWindow = { _, _ -> },
                    onHandleBoundsInWindow = {},
                )
            }
        }
    }
}

internal suspend fun PointerInputScope.detectModuleReorderDrag(
    onLongPress: () -> Unit,
    onDragStart: (Offset) -> Boolean,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val longPress = awaitHomeHandleLongPress(down = down) ?: return@awaitEachGesture
        var dragging = false
        var cancelled = false
        try {
            longPress.consume()
            if (!onDragStart(longPress.position)) return@awaitEachGesture
            dragging = true
            onLongPress()
            cancelled = !trackModuleDrag(downId = down.id, onDrag = onDrag)
            dragging = false
            if (cancelled) onDragCancel() else onDragEnd()
        } finally {
            if (dragging) onDragCancel()
        }
    }
}

/**
 * Starts module movement when a pointer that went down on a module-reorder handle moves
 * beyond the platform touch slop. [handleAt] resolves the module whose handle contains the
 * list-local touch point together with that point in window coordinates.
 */
internal suspend fun <T> PointerInputScope.detectModuleHandleDrag(
    handleAt: (Offset) -> Pair<T, Offset>?,
    onDragStart: (T, Offset) -> Boolean,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val (target, touchInWindow) = handleAt(down.position) ?: return@awaitEachGesture
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            if (event.changes.any { it.id != down.id && it.pressed }) return@awaitEachGesture
            val change = event.changes.firstOrNull { it.id == down.id }
                ?: return@awaitEachGesture
            if (!change.pressed) return@awaitEachGesture
            if ((change.position - down.position).getDistance() >= viewConfiguration.touchSlop) {
                change.consume()
                break
            }
        }
        var dragging = false
        try {
            if (!onDragStart(target, touchInWindow)) return@awaitEachGesture
            dragging = true
            val completed = trackModuleDrag(downId = down.id, onDrag = onDrag)
            dragging = false
            if (completed) onDragEnd() else onDragCancel()
        } finally {
            if (dragging) onDragCancel()
        }
    }
}

/** Follows one recognized module drag; returns false when the gesture was cancelled. */
private suspend fun AwaitPointerEventScope.trackModuleDrag(
    downId: PointerId,
    onDrag: (Offset) -> Unit,
): Boolean {
    while (true) {
        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
        if (event.changes.any { it.id != downId && it.pressed }) return false
        val change = event.changes.firstOrNull { it.id == downId } ?: return false
        val normalRelease = change.changedToUp()
        if (!change.pressed && !normalRelease) return false
        val movement = change.positionChangeIgnoreConsumed()
        change.consume()
        if (movement != Offset.Zero) onDrag(movement)
        if (normalRelease) return true
    }
}
