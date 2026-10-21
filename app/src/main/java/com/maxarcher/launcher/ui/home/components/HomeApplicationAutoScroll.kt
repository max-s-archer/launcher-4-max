package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.HomeApplicationScrollRequest
import com.maxarcher.launcher.R
import com.maxarcher.launcher.resolveHomeApplicationEdgeScroll
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun HomeApplicationAutoScroll(
    movement: HomeApplicationMovement,
    mainListState: LazyListState,
    ribbonStates: Map<String, LazyListState>,
) {
    val density = LocalDensity.current
    val edgeBand = dimensionResource(id = R.dimen.home_favorite_edge_scroll_band)
    val bandPx = with(receiver = density, block = { edgeBand.toPx() })
    val delayMillis = integerResource(id = R.integer.home_favorite_edge_scroll_start_delay_ms)
    val speedPx = integerResource(id = R.integer.home_favorite_edge_scroll_dp_per_second) * density.density
    var stalledRequest by remember(calculation = { mutableStateOf<HomeApplicationScrollRequest?>(value = null) })
    val currentPointer = remember(key1 = movement, calculation = {
        derivedStateOf(policy = structuralEqualityPolicy(), calculation = { movement.session?.pointer })
    })

    fun currentRequest(): HomeApplicationScrollRequest? {
        if (!movement.isDragging) return null
        // Module/viewport bounds are plain geometry, not a state read in every application row.
        @Suppress("UNUSED_VARIABLE")
        val geometryRevision = movement.geometryRevision
        val pointer = currentPointer.value ?: return null
        val ribbon = movement.visibleRibbonAt(pointer = pointer)
        val ribbonState = ribbon?.let(block = { ribbonStates[it.first] })
        return resolveHomeApplicationEdgeScroll(
            pointer = pointer, viewport = movement.viewport, band = bandPx,
            mainCanScrollBackward = mainListState.canScrollBackward,
            mainCanScrollForward = mainListState.canScrollForward,
            ribbon = ribbon,
            ribbonCanScrollBackward = ribbonState?.canScrollBackward == true,
            ribbonCanScrollForward = ribbonState?.canScrollForward == true,
        )
    }

    val rawRequest = currentRequest()
    val request = rawRequest.takeUnless(predicate = { it == stalledRequest })
    SideEffect(effect = {
        if (rawRequest != stalledRequest) stalledRequest = null
        movement.updateEdgeFeedback(request = request)
    })
    LaunchedEffect(
        key1 = movement.activeIdentity,
        key2 = request?.owner,
        block = {
            val owner = request?.owner ?: return@LaunchedEffect
            delay(duration = delayMillis.milliseconds)
            var previousFrame = withFrameNanos(onFrame = { it })
            while (movement.isDragging) {
                val frame = withFrameNanos(onFrame = { it })
                val current = currentRequest() ?: break
                if (current.owner != owner) break
                val state = if (owner.ribbonId == null) mainListState else ribbonStates[owner.ribbonId] ?: break
                val seconds = (frame - previousFrame) / 1_000_000_000f
                previousFrame = frame
                val consumed = state.scrollBy(value = owner.direction * speedPx * current.proximity * seconds)
                if (consumed == 0f && current.proximity > 0f) {
                    // Stop even if platform scrollability flags have not yet caught up. A new
                    // pointer/layout request can retry, with a fresh complete residence delay.
                    stalledRequest = current
                    movement.updateEdgeFeedback(request = null)
                    break
                }
                // Layout callbacks re-resolve the destination against the newly measured cells;
                // the owning pointer and source preview do not move when a viewport scrolls.
            }
        },
    )
}
