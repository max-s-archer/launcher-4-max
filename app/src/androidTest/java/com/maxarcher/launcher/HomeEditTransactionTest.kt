package com.maxarcher.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEditTransactionTest {
    @Test
    fun mutationPreviewCompletesAndFailureRestoresCurrentAggregate() {
        val initial = aggregate("initial")
        val pending = aggregate("pending")
        val persisted = aggregate("persisted")
        val refreshed = aggregate("refreshed")
        val transaction = HomeEditTransaction()

        transaction.enter(initial)
        assertEquals(initial, transaction.baseAggregate(null))

        transaction.beginMutation(pending)
        assertEquals(pending, transaction.previewAggregate(initial))
        transaction.completeMutation(persisted)
        assertEquals(persisted, transaction.committedAggregate)
        assertNull(transaction.pendingAggregate)

        transaction.beginMutation(pending)
        transaction.discardPending(pending)
        transaction.restoreCommitted(refreshed)
        assertEquals(refreshed, transaction.committedAggregate)
        assertNull(transaction.pendingAggregate)
    }

    @Test
    fun undoCanOnlyBeConsumedOnceForItsSequence() {
        val transaction = HomeEditTransaction()
        val snapshot = aggregate("undo")

        val sequence = transaction.recordUndo(snapshot)

        assertNull(transaction.consumeUndo(sequence + 1))
        assertEquals(snapshot, transaction.consumeUndo(sequence))
        assertNull(transaction.consumeUndo(sequence))
    }

    @Test
    fun externalReconciliationClearsPendingAndInvalidatesUndo() {
        val transaction = HomeEditTransaction()
        val initial = aggregate("initial")
        val pending = aggregate("pending")
        val external = aggregate("external")
        transaction.enter(initial)
        transaction.beginMutation(pending)
        transaction.recordUndo(initial)

        assertTrue(transaction.reconcileExternal(external))
        assertEquals(external, transaction.committedAggregate)
        assertNull(transaction.pendingAggregate)
        assertNull(transaction.undoAggregate)
        assertFalse(transaction.reconcileExternal(external))
    }

    @Test
    fun leavingInvalidatesSessionAndClearsEditSnapshots() {
        val transaction = HomeEditTransaction()
        transaction.enter(aggregate("initial"))
        transaction.beginMutation(aggregate("pending"))
        transaction.recordUndo(aggregate("undo"))
        transaction.beginModuleOrder(emptyList())
        val activeSession = transaction.sessionId

        transaction.leave()

        assertTrue(transaction.sessionId > activeSession)
        assertNull(transaction.pendingAggregate)
        assertNull(transaction.committedAggregate)
        assertNull(transaction.undoAggregate)
        assertNull(transaction.pendingModuleOrder)
    }

    private fun aggregate(id: String) = FavoriteAggregate(
        verticalLists = listOf(
            FavoriteContainer(
                id = id,
                type = FavoriteContainerType.VerticalList,
                identities = emptyList(),
            ),
        ),
    )
}
