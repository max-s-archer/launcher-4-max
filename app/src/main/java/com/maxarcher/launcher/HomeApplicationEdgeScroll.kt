package com.maxarcher.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** A stable residence key: changing proximity or layout must not restart the activation delay. */
internal data class HomeApplicationScrollOwner(val ribbonId: String?, val direction: Int)

internal data class HomeApplicationScrollRequest(
    val owner: HomeApplicationScrollOwner,
    val bounds: Rect,
    val band: Float,
    val proximity: Float,
) {
    val horizontal: Boolean get() = owner.ribbonId != null
}

internal fun resolveHomeApplicationEdgeScroll(
    pointer: Offset,
    viewport: Rect,
    band: Float,
    mainCanScrollBackward: Boolean,
    mainCanScrollForward: Boolean,
    ribbon: Pair<String, Rect>? = null,
    ribbonCanScrollBackward: Boolean = false,
    ribbonCanScrollForward: Boolean = false,
): HomeApplicationScrollRequest? {
    if (viewport.isEmpty || pointer.x !in viewport.left..viewport.right ||
        pointer.y !in viewport.top..viewport.bottom
    ) return null

    fun request(
        ribbonId: String?,
        bounds: Rect,
        coordinate: Float,
        start: Float,
        end: Float,
        canBackward: Boolean,
        canForward: Boolean,
    ): HomeApplicationScrollRequest? {
        val cappedBand = minOf(band, (end - start) / 2f)
        if (cappedBand <= 0f) return null
        val direction = when {
            coordinate <= start + cappedBand && canBackward -> -1
            coordinate >= end - cappedBand && canForward -> 1
            else -> return null
        }
        val distance = if (direction < 0) coordinate - start else end - coordinate
        return HomeApplicationScrollRequest(
            owner = HomeApplicationScrollOwner(ribbonId = ribbonId, direction = direction),
            bounds = bounds,
            band = cappedBand,
            proximity = (1f - distance / cappedBand).coerceIn(minimumValue = 0f, maximumValue = 1f),
        )
    }

    // A usable horizontal edge belongs to the ribbon under the pointer. Otherwise the main
    // viewport may own vertical scrolling. Never run both axes, including at viewport corners.
    if (ribbon != null && !ribbon.second.isEmpty &&
        pointer.x in ribbon.second.left..ribbon.second.right && pointer.y in ribbon.second.top..ribbon.second.bottom
    ) {
        val horizontal = request(
            ribbonId = ribbon.first, bounds = ribbon.second, coordinate = pointer.x,
            start = ribbon.second.left, end = ribbon.second.right,
            canBackward = ribbonCanScrollBackward, canForward = ribbonCanScrollForward,
        )
        if (horizontal != null) return horizontal
    }
    return request(
        ribbonId = null, bounds = viewport, coordinate = pointer.y,
        start = viewport.top, end = viewport.bottom,
        canBackward = mainCanScrollBackward, canForward = mainCanScrollForward,
    )
}
