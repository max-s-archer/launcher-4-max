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

    var pendingAggregate by mutableStateOf<FavoriteAggregate?>(null)
        private set

    var committedAggregate by mutableStateOf<FavoriteAggregate?>(null)
        private set

    var undoAggregate by mutableStateOf<FavoriteAggregate?>(null)
        private set

    var undoSequence by mutableIntStateOf(0)
        private set

    var pendingModuleOrder by mutableStateOf<List<OrderedFavoriteModule>?>(null)
        private set

    fun enter(initialAggregate: FavoriteAggregate?) {
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

    fun baseAggregate(currentAggregate: FavoriteAggregate?): FavoriteAggregate? =
        pendingAggregate ?: committedAggregate ?: currentAggregate

    fun previewAggregate(fallback: FavoriteAggregate): FavoriteAggregate =
        pendingAggregate ?: committedAggregate ?: fallback

    fun beginMutation(updated: FavoriteAggregate) {
        pendingAggregate = updated
    }

    fun completeMutation(persisted: FavoriteAggregate) {
        committedAggregate = persisted
        pendingAggregate = null
    }

    fun discardPending(expectedPending: FavoriteAggregate) {
        if (pendingAggregate == expectedPending) pendingAggregate = null
    }

    fun restoreCommitted(currentAggregate: FavoriteAggregate?) {
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

    fun recordUndo(aggregate: FavoriteAggregate): Int {
        undoSequence += 1
        undoAggregate = aggregate
        return undoSequence
    }

    fun consumeUndo(expectedSequence: Int): FavoriteAggregate? {
        if (expectedSequence != undoSequence) return null
        val snapshot = undoAggregate ?: return null
        undoAggregate = null
        undoSequence += 1
        return snapshot
    }

    fun reconcileExternal(aggregate: FavoriteAggregate): Boolean {
        val dismissedUndo = undoAggregate != null
        if (dismissedUndo) clearUndo()
        pendingAggregate = null
        committedAggregate = aggregate
        return dismissedUndo
    }
}
