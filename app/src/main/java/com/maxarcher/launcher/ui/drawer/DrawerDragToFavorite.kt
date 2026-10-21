package com.maxarcher.launcher.ui.drawer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.OrderedFavoriteModuleType
import com.maxarcher.launcher.R

/** Eligibility of a Drawer application row for the drag-to-favorite journey. */
internal enum class DrawerDragEligibility {
    Eligible,
    AlreadyFavorited,
    ReliablyDisabled,
}

/**
 * Resolves the drag-to-favorite eligibility of one Drawer row. An identity that is
 * already assigned to any favorite module or reliably disabled is not drag-eligible;
 * temporarily unavailable and unknown states stay eligible and resolve through the
 * ordinary save-time rules.
 */
internal fun resolveDrawerDragEligibility(
    isFavoriteMember: Boolean,
    availability: FavoriteAvailability?,
): DrawerDragEligibility = when {
    isFavoriteMember -> DrawerDragEligibility.AlreadyFavorited
    availability is FavoriteAvailability.Disabled -> DrawerDragEligibility.ReliablyDisabled
    else -> DrawerDragEligibility.Eligible
}

/**
 * One active drag-to-favorite journey: the dragged row and its window geometry captured
 * at drag start. The live finger position is reported separately by the row detector,
 * so the preview keeps following the finger across the programmatic transition.
 */
internal data class DrawerDragJourney(
    val entry: LaunchableEntry,
    val originInWindow: Offset,
    val size: IntSize,
    val touchStartInWindow: Offset,
) {
    /** Finger offset inside the row at drag start; the preview preserves it. */
    val grabOffsetInRow: Offset get() = touchStartInWindow - originInWindow
}

/** Resolved release target of the Drawer drag-to-favorite journey. */
internal sealed interface DrawerDragDrop {
    data class Insertion(val moduleId: String, val boundary: Int) : DrawerDragDrop

    data class Creation(val moduleType: OrderedFavoriteModuleType) : DrawerDragDrop

    data class HomeBottomAppBar(val boundary: Int) : DrawerDragDrop
}

/**
 * Drawer application-row gesture discrimination for drag-to-favorite. The row's click
 * layer performs the tap launch and the long-press haptic; this detector continues the
 * same pointer sequence after the long-press timeout and dispatches exactly one branch:
 * releasing without moving beyond the platform touch slop opens the application action
 * sheet; the first movement beyond the touch slop starts the drag, after which every
 * move and the final release (or pointer cancellation) are reported. Dispatched changes
 * are consumed so the list does not scroll the dragged row away.
 */
internal fun Modifier.drawerDragToFavoriteDetection(
    key1: Any?,
    onReleaseAfterLongPress: () -> Unit,
    onDragBeyondSlop: (PointerInputChange) -> Boolean,
    onDragMove: (PointerInputChange) -> Unit,
    onDragEnd: (PointerInputChange?, cancelled: Boolean) -> Unit,
): Modifier = this.pointerInput(key1) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        var dispatched = false
        var beyondSlopHandled = false
        var endDelivered = false
        // Cumulative distance since the long-press, tracked with consumption ignored so
        // a slow drag still crosses the platform touch slop before the list scrolls.
        var draggedDistance = 0f
        try {
        while (true) {
            val event = awaitPointerEvent()
            val tracked = event.changes.firstOrNull { it.id == longPress.id }
                ?: event.changes.firstOrNull()
                ?: continue
            if (event.changes.none(PointerInputChange::pressed)) {
                // A stream end without a genuine up transition is a system
                // cancellation: it must not commit a drop or open the action sheet.
                val cancelled = tracked.changedToUp().not()
                if (dispatched) {
                    endDelivered = true
                    onDragEnd(tracked, cancelled)
                } else if (!cancelled) {
                    endDelivered = true
                    onReleaseAfterLongPress()
                }
                break
            }
            draggedDistance += tracked.positionChangeIgnoreConsumed().getDistance()
            val trackedUp = tracked.changedToUp()
            if (!dispatched) {
                if (draggedDistance > viewConfiguration.touchSlop && !beyondSlopHandled) {
                    // The slop crossing is dispatched exactly once; the journey only
                    // owns the gesture when the start is confirmed, and a rejected
                    // start keeps the release-opens-sheet behavior.
                    beyondSlopHandled = true
                    dispatched = onDragBeyondSlop(tracked)
                } else if (trackedUp) {
                    endDelivered = true
                    onReleaseAfterLongPress()
                    break
                }
            } else {
                endDelivered = true
                if (!trackedUp) onDragMove(tracked) else onDragEnd(tracked, false)
                if (trackedUp) break
            }
            if (dispatched) {
                event.changes.forEach { if (!it.changedToUp()) it.consume() }
            }
        }
        } finally {
            // A cancelled detector coroutine must still end the journey, or the
            // frozen preview and edit mode are left dangling without an owner.
            if (dispatched && !endDelivered) onDragEnd(null, true)
        }
    }
}

/**
 * Root-level non-interactive preview of a dragged Drawer row. The preview renders the
 * row's current real-time presentation at its captured drag-start geometry, positioned
 * from the live finger position reported by the row detector, and never consumes input.
 */
@Composable
internal fun DrawerDragPreviewOverlay(
    journey: DrawerDragJourney,
    displaySettings: DrawerDisplaySettings,
    touchInWindow: Offset,
    rootOriginInWindow: Offset,
    modifier: Modifier = Modifier,
) {
    val topLeft = touchInWindow - journey.grabOffsetInRow - rootOriginInWindow
    Box(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val previewAlpha = integerResource(R.integer.home_drag_preview_alpha_percent) / 100f
        Box(
            modifier = Modifier
                .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                .size(
                    width = with(density) { journey.size.width.toDp() },
                    height = with(density) { journey.size.height.toDp() },
                )
                .alpha(previewAlpha)
                .clearAndSetSemantics {}
                .testTag("drawer_drag_preview"),
        ) {
            DrawerApplicationContent(
                entry = journey.entry,
                displaySettings = displaySettings,
                searchQuery = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
