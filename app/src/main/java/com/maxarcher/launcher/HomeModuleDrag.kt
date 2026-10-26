package com.maxarcher.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
