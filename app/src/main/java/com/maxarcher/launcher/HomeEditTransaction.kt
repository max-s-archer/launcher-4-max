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

    var undoSurvivesEditSession by mutableStateOf(false)
        private set

    var pendingModuleOrder by mutableStateOf<List<OrderedFavoriteModule>?>(null)
        private set

    fun enter(initialAggregate: OrderedFavoriteAggregate?) {
        sessionId += 1
        if (!undoSurvivesEditSession) committedAggregate = initialAggregate
    }

    fun leave() {
        val preserveUndo = undoSurvivesEditSession
        sessionId += 1
        if (!preserveUndo) {
            undoSequence += 1
            undoAggregate = null
            undoSurvivesEditSession = false
        }
        pendingAggregate = null
        if (!preserveUndo) committedAggregate = null
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
        undoSurvivesEditSession = false
    }

    fun recordUndo(
        aggregate: OrderedFavoriteAggregate,
        survivesEditSession: Boolean = false,
    ): Int {
        undoSequence += 1
        undoAggregate = aggregate
        undoSurvivesEditSession = survivesEditSession
        return undoSequence
    }

    fun consumeUndo(expectedSequence: Int): OrderedFavoriteAggregate? {
        if (expectedSequence != undoSequence) return null
        val snapshot = undoAggregate ?: return null
        undoAggregate = null
        undoSurvivesEditSession = false
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
