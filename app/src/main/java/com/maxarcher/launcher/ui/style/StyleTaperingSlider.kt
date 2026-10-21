package com.maxarcher.launcher.ui.style

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.maxarcher.launcher.R
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val TAPERING_SLIDER_STOP_COUNT = 3

@Composable
internal fun StyleTaperingSlider(
    selectedStop: Int,
    stopLabels: List<String>,
    label: String,
    increaseLabel: String,
    decreaseLabel: String,
    onSelectStop: (Int) -> Unit,
    onSelectStopFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val stopCount = TAPERING_SLIDER_STOP_COUNT
    require(selectedStop in 0 until stopCount)
    require(stopLabels.size == stopCount)
    val activeColor = MaterialTheme.colorScheme.onBackground
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val startHeight = dimensionResource(
        id = R.dimen.style_settings_tapering_track_start_height,
    )
    val endHeight = dimensionResource(
        id = R.dimen.style_settings_tapering_track_end_height,
    )
    val thumbWidth = dimensionResource(
        id = R.dimen.style_settings_tapering_thumb_width,
    )
    val thumbHeight = dimensionResource(
        id = R.dimen.style_settings_tapering_thumb_height,
    )
    val thumbRadius = dimensionResource(
        id = R.dimen.style_settings_tapering_thumb_radius,
    )
    val thumbTrackGap = dimensionResource(
        id = R.dimen.style_settings_tapering_thumb_track_gap,
    )
    val thumbWidthPx = with(receiver = LocalDensity.current) { thumbWidth.toPx() }
    val stopTargetPx = with(receiver = LocalDensity.current) {
        dimensionResource(id = R.dimen.style_settings_tapering_stop_target_size).toPx()
    }
    val lastStop = stopCount - 1
    val currentSelectedStop by rememberUpdatedState(newValue = selectedStop)
    val currentOnSelectStop by rememberUpdatedState(newValue = onSelectStop)
    val currentOnSelectStopFinished by rememberUpdatedState(
        newValue = onSelectStopFinished,
    )
    val accessibilityActions = buildList {
        if (enabled && selectedStop > 0) {
            add(
                CustomAccessibilityAction(label = increaseLabel) {
                    currentOnSelectStopFinished(selectedStop - 1)
                    true
                },
            )
        }
        if (enabled && selectedStop < lastStop) {
            add(
                CustomAccessibilityAction(label = decreaseLabel) {
                    currentOnSelectStopFinished(selectedStop + 1)
                    true
                },
            )
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .progressSemantics(
                value = selectedStop.toFloat(),
                valueRange = 0f..lastStop.toFloat(),
                steps = lastStop,
            )
            .semantics {
                contentDescription = label
                stateDescription = stopLabels[selectedStop]
                if (!enabled) disabled()
                setProgress { target ->
                    if (!enabled) return@setProgress false
                    currentOnSelectStopFinished(
                        target.roundToInt().coerceIn(0, lastStop),
                    )
                    true
                }
                customActions = accessibilityActions
            }
            .pointerInput(enabled, thumbWidthPx, stopTargetPx) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val width = size.width.toFloat()
                    val origin = stopForPress(
                        x = down.position.x,
                        width = width,
                        thumbWidth = thumbWidthPx,
                        lastStop = lastStop,
                        selectedStop = currentSelectedStop,
                        stopTarget = stopTargetPx,
                    )
                    var current = origin
                    currentOnSelectStop(current)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change -> change.consume() }
                        val active = event.changes.firstOrNull { change ->
                            change.id == down.id
                        }
                        if (active?.pressed == true) {
                            current = stopForDrag(
                                x = active.position.x,
                                originX = down.position.x,
                                originStop = origin,
                                width = width,
                                thumbWidth = thumbWidthPx,
                                lastStop = lastStop,
                            )
                            currentOnSelectStop(current)
                        }
                    } while (event.changes.any { change ->
                        change.pressed && change.id == down.id
                    })
                    currentOnSelectStopFinished(current)
                }
            },
        onDraw = {
            val startHeightPx = startHeight.toPx()
            val endHeightPx = endHeight.toPx()
            val drawnThumbWidth = thumbWidth.toPx()
            val thumbHeightPx = thumbHeight.toPx()
            val gapPx = thumbTrackGap.toPx()
            val trackBottom = (size.height + startHeightPx) / 2f
            val trackPath = taperingTrackPath(
                width = size.width,
                startHeight = startHeightPx,
                endHeight = endHeightPx,
                bottom = trackBottom,
            )
            val travel = (size.width - drawnThumbWidth).coerceAtLeast(
                minimumValue = 0f,
            )
            val progress = selectedStop / lastStop.toFloat()
            val thumbCenterX = drawnThumbWidth / 2f + travel * progress
            val thumbLeft = thumbCenterX - drawnThumbWidth / 2f
            val thumbRight = thumbCenterX + drawnThumbWidth / 2f
            val activeEnd = (thumbLeft - gapPx).coerceAtLeast(minimumValue = 0f)
            val inactiveStart = (thumbRight + gapPx).coerceAtMost(
                maximumValue = size.width,
            )
            clipPath(path = trackPath) {
                if (activeEnd > 0f) {
                    drawRect(
                        color = activeColor,
                        topLeft = Offset.Zero,
                        size = Size(width = activeEnd, height = size.height),
                    )
                }
                if (inactiveStart < size.width) {
                    drawRect(
                        color = inactiveColor,
                        topLeft = Offset(x = inactiveStart, y = 0f),
                        size = Size(
                            width = size.width - inactiveStart,
                            height = size.height,
                        ),
                    )
                }
            }
            val thumbTop = ((size.height - thumbHeightPx) / 2f).coerceAtLeast(
                minimumValue = 0f,
            )
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(x = thumbLeft, y = thumbTop),
                size = Size(
                    width = drawnThumbWidth,
                    height = thumbHeightPx.coerceAtMost(maximumValue = size.height),
                ),
                cornerRadius = CornerRadius(
                    x = thumbRadius.toPx(),
                    y = thumbRadius.toPx(),
                ),
            )
        },
    )
}

private fun taperingTrackPath(
    width: Float,
    startHeight: Float,
    endHeight: Float,
    bottom: Float,
): Path {
    val leftRadius = startHeight / 2f
    val rightRadius = endHeight / 2f
    val topLeft = bottom - startHeight
    val topRight = bottom - endHeight
    return Path().apply {
        moveTo(x = leftRadius, y = topLeft)
        lineTo(x = width - rightRadius, y = topRight)
        arcTo(
            rect = Rect(
                left = width - 2f * rightRadius,
                top = topRight,
                right = width,
                bottom = topRight + 2f * rightRadius,
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(x = width, y = bottom - rightRadius)
        arcTo(
            rect = Rect(
                left = width - 2f * rightRadius,
                top = bottom - 2f * rightRadius,
                right = width,
                bottom = bottom,
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(x = leftRadius, y = bottom)
        arcTo(
            rect = Rect(
                left = 0f,
                top = bottom - 2f * leftRadius,
                right = 2f * leftRadius,
                bottom = bottom,
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(x = 0f, y = topLeft + leftRadius)
        arcTo(
            rect = Rect(
                left = 0f,
                top = topLeft,
                right = 2f * leftRadius,
                bottom = topLeft + 2f * leftRadius,
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        close()
    }
}

private fun thumbTravel(width: Float, thumbWidth: Float): Float =
    (width - thumbWidth).coerceAtLeast(minimumValue = 1f)

private fun stopCenterX(stop: Int, width: Float, thumbWidth: Float, lastStop: Int): Float {
    val progress = stop / lastStop.toFloat()
    return thumbWidth / 2f + thumbTravel(width = width, thumbWidth = thumbWidth) * progress
}

private fun nearestStop(
    x: Float,
    width: Float,
    thumbWidth: Float,
    lastStop: Int,
): Int {
    var bestStop = 0
    var bestDistance = Float.POSITIVE_INFINITY
    for (stop in 0..lastStop) {
        val distance = abs(
            stopCenterX(
                stop = stop,
                width = width,
                thumbWidth = thumbWidth,
                lastStop = lastStop,
            ) - x,
        )
        if (distance < bestDistance) {
            bestStop = stop
            bestDistance = distance
        }
    }
    return bestStop
}

private fun stopForPress(
    x: Float,
    width: Float,
    thumbWidth: Float,
    lastStop: Int,
    selectedStop: Int,
    stopTarget: Float,
): Int {
    val hitRadius = stopTarget / 2f
    val selectedCenter = stopCenterX(
        stop = selectedStop,
        width = width,
        thumbWidth = thumbWidth,
        lastStop = lastStop,
    )
    if (abs(x - selectedCenter) <= hitRadius) {
        return selectedStop
    }
    val nearest = nearestStop(
        x = x,
        width = width,
        thumbWidth = thumbWidth,
        lastStop = lastStop,
    )
    val nearestCenter = stopCenterX(
        stop = nearest,
        width = width,
        thumbWidth = thumbWidth,
        lastStop = lastStop,
    )
    if (abs(x - nearestCenter) <= hitRadius) {
        return nearest
    }
    return selectedStop
}

private fun stopForDrag(
    x: Float,
    originX: Float,
    originStop: Int,
    width: Float,
    thumbWidth: Float,
    lastStop: Int,
): Int {
    val grabbedX = stopCenterX(
        stop = originStop,
        width = width,
        thumbWidth = thumbWidth,
        lastStop = lastStop,
    )
    return nearestStop(
        x = grabbedX + (x - originX),
        width = width,
        thumbWidth = thumbWidth,
        lastStop = lastStop,
    )
}
