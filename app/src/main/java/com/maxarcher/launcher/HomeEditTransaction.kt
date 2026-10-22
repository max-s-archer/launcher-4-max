package com.maxarcher.launcher

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class HomeEditTransaction {
    var sessionId by mutableIntStateOf(0)
        private set

    var pendingAggregate by mutableStateOf<OrderedFavoriteAggregate?>(null)
        private set

    var committedAggregate by mutableStateOf<OrderedFavoriteAggregate?>(null)
        private set

    var undoAggregate by mutableStateOf<OrderedFavoriteAggregate?>(null)
        private set

    var undoSequence by mutableIntStateOf(0)
        private set

    var pendingModuleOrder by mutableStateOf<List<OrderedFavoriteModule>?>(null)
        private set

    fun enter(initialAggregate: OrderedFavoriteAggregate?) {
        sessionId += 1
        committedAggregate = initialAggregate
    }

    fun leave() {
        sessionId += 1
        undoAggregate = null
        pendingAggregate = null
        committedAggregate = null
        pendingModuleOrder = null
    }

    fun baseAggregate(currentAggregate: OrderedFavoriteAggregate?): OrderedFavoriteAggregate? =
        pendingAggregate ?: committedAggregate ?: currentAggregate

    fun previewAggregate(fallback: OrderedFavoriteAggregate): OrderedFavoriteAggregate =
        pendingAggregate ?: committedAggregate ?: fallback

    fun beginMutation(updated: OrderedFavoriteAggregate) {
        pendingAggregate = updated
    }

    fun completeMutation(persisted: OrderedFavoriteAggregate) {
        committedAggregate = persisted
        pendingAggregate = null
    }

    fun discardPending(expectedPending: OrderedFavoriteAggregate) {
        if (pendingAggregate == expectedPending) pendingAggregate = null
    }

    fun restoreCommitted(currentAggregate: OrderedFavoriteAggregate?) {
        committedAggregate = currentAggregate ?: committedAggregate
    }

    fun beginModuleOrder(modules: List<OrderedFavoriteModule>) {
        pendingModuleOrder = modules
    }

    fun completeModuleOrder() {
        pendingModuleOrder = null
    }

    fun clearUndo() {
        undoSequence += 1
        undoAggregate = null
    }

    fun recordUndo(aggregate: OrderedFavoriteAggregate): Int {
        undoSequence += 1
        undoAggregate = aggregate
        return undoSequence
    }

    fun consumeUndo(expectedSequence: Int): OrderedFavoriteAggregate? {
        if (expectedSequence != undoSequence) return null
        val snapshot = undoAggregate ?: return null
        undoAggregate = null
        undoSequence += 1
        return snapshot
    }

    fun reconcileExternal(aggregate: OrderedFavoriteAggregate): Boolean {
        val dismissedUndo = undoAggregate != null
        if (dismissedUndo) clearUndo()
        pendingAggregate = null
        committedAggregate = aggregate
        return dismissedUndo
    }
}
