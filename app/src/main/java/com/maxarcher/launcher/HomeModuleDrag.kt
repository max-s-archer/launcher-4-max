package com.maxarcher.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize

internal data class ModuleDragSession(
    val sourceModule: OrderedFavoriteModule,
    val sourceSelected: Boolean,
    val sourceAvailability: Map<LaunchableIdentity, FavoriteAvailability>,
    val initialModules: List<OrderedFavoriteModule>,
    val remainingModules: List<OrderedFavoriteModule>,
    val insertionIndex: Int?,
    val originInWindow: Offset,
    val size: IntSize,
    val touchStartInWindow: Offset,
    val delta: Offset = Offset.Zero,
) {
    val touchInWindow: Offset get() = touchStartInWindow + delta

    fun advanced(
        amount: Offset,
        listBoundsInWindow: Rect,
        moduleBoundsInWindow: Map<String, Rect>,
    ): ModuleDragSession {
        val moved = copy(delta = delta + amount)
        if (!listBoundsInWindow.contains(moved.touchInWindow)) {
            return moved.copy(insertionIndex = null)
        }
        val touchY = moved.touchInWindow.y
        val resolvedIndex = remainingModules.indexOfFirst { module ->
            val bounds = moduleBoundsInWindow[module.id] ?: return@indexOfFirst false
            touchY < bounds.center.y
        }.let { index ->
            if (index < 0) remainingModules.size else index
        }
        return moved.copy(insertionIndex = resolvedIndex)
    }

    fun completedModules(): List<OrderedFavoriteModule>? {
        val targetIndex = insertionIndex ?: return null
        val reordered = remainingModules.toMutableList().apply {
            add(targetIndex.coerceIn(0, size), sourceModule)
        }.toList()
        return reordered.takeUnless {
            it.map(OrderedFavoriteModule::id) == initialModules.map(OrderedFavoriteModule::id)
        }
    }
}

internal fun DrawScope.drawCornerMark(
    color: Color,
    corner: Offset,
    horizontalEnd: Offset,
    verticalEnd: Offset,
    radius: Float,
    stroke: Float,
    startAngle: Float,
) {
    val horizontalDirection = if (horizontalEnd.x >= corner.x) 1f else -1f
    val verticalDirection = if (verticalEnd.y >= corner.y) 1f else -1f
    val horizontalStart = Offset(corner.x + horizontalDirection * radius, corner.y)
    val verticalStart = Offset(corner.x, corner.y + verticalDirection * radius)
    val arcTopLeft = Offset(
        x = if (horizontalDirection > 0f) corner.x else corner.x - radius * 2f,
        y = if (verticalDirection > 0f) corner.y else corner.y - radius * 2f,
    )
    drawLine(color, horizontalStart, horizontalEnd, stroke, StrokeCap.Round)
    drawLine(color, verticalStart, verticalEnd, stroke, StrokeCap.Round)
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
