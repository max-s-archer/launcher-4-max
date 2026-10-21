package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeApplicationPendingMovementTest {
    private val identity = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.app", "Main"))
    private val source = OrderedFavoriteModule(id = "source", type = OrderedFavoriteModuleType.Vertical, identities = listOf(element = identity))
    private val viewport = Rect(left = 0f, top = 0f, right = 300f, bottom = 300f)
    private val creation = Rect(left = 0f, top = 200f, right = 140f, bottom = 256f)

    @Test
    fun releaseFreezesThePreviewAndRequestAndStopsAllPointerFeedback() {
        val movement = movement()
        lift(movement = movement)
        val previewSource = movement.previewSource
        val origin = movement.session?.previewOrigin
        val change = checkNotNull(value = movement.finish())
        assertFalse(movement.isDragging)
        assertSame(change, movement.pendingChange)
        assertSame(previewSource, movement.previewSource)
        assertEquals(identity, movement.activeIdentity)
        assertNull(movement.session?.creation)
        assertNull(movement.session?.insertion)
        assertNull(movement.edgeFeedback)
        movement.move(pointer = Offset(x = 250f, y = 250f))
        movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = null)
        movement.updateViewport(bounds = Rect(left = 0f, top = 0f, right = 300f, bottom = 100f))
        movement.updateEdgeFeedback(request = HomeApplicationScrollRequest(
            owner = HomeApplicationScrollOwner(ribbonId = null, direction = 1), bounds = viewport, band = 48f, proximity = 1f,
        ))
        assertEquals(origin, movement.session?.previewOrigin)
        assertNull(movement.edgeFeedback)
        assertSame(change, movement.pendingChange)
        assertNull(movement.finish())
        movement.complete(change = change)
        assertNull(movement.session)
        assertNull(movement.previewSource)
        assertNull(movement.activeIdentity)
    }

    @Test
    fun layoutAndFrozenPreviewReadersDoNotObservePointerOrCandidateTicks() {
        val movement = movement()
        lift(movement = movement)
        val previewSource = movement.previewSource
        Snapshot.sendApplyNotifications()
        var invalidations = 0
        val observer = SnapshotStateObserver(onChangedExecutor = { command -> command() })
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = { _: Unit -> invalidations += 1 },
                block = {
                    assertTrue(movement.isDragging)
                    assertEquals(identity, movement.activeIdentity)
                    assertSame(previewSource, movement.previewSource)
                },
            )
            movement.move(pointer = creation.center + Offset(x = 4f, y = 0f))
            movement.updateCreationTarget(
                type = OrderedFavoriteModuleType.Vertical,
                bounds = creation.translate(offset = Offset(x = 0f, y = 2f)),
            )
            Snapshot.sendApplyNotifications()
            assertEquals(0, invalidations)
            assertSame(previewSource, movement.previewSource)

            // Phase changes must still update Back handling, scrolling and edit availability.
            assertNotNull(movement.finish())
            Snapshot.sendApplyNotifications()
            assertEquals(1, invalidations)
            assertFalse(movement.isDragging)
        } finally {
            observer.stop()
            observer.clear()
        }
    }

    @Test
    fun geometryReadersStillObserveTheLatestAnimatedCandidate() {
        val movement = movement()
        lift(movement = movement)
        Snapshot.sendApplyNotifications()
        var invalidations = 0
        val observer = SnapshotStateObserver(onChangedExecutor = { command -> command() })
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = { _: Unit -> invalidations += 1 },
                block = { assertEquals(creation, movement.session?.creation?.second) },
            )
            val movedBounds = creation.translate(offset = Offset(x = 0f, y = 2f))
            movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = movedBounds)
            Snapshot.sendApplyNotifications()
            assertEquals(1, invalidations)
            assertEquals(movedBounds, movement.session?.creation?.second)
        } finally {
            observer.stop()
            observer.clear()
        }
    }

    @Test
    fun navigationClearsFeedbackButAnOldCompletionCannotClearANewerGesture() {
        val movement = movement()
        lift(movement = movement)
        val old = checkNotNull(value = movement.finish())
        movement.cancel()
        assertNull(movement.pendingChange)
        lift(movement = movement)
        movement.complete(change = old)
        assertTrue(movement.isDragging)
        val current = checkNotNull(value = movement.finish())
        // Structurally equal requests still belong to different saves.
        assertEquals(old, current)
        movement.complete(change = old)
        assertSame(current, movement.pendingChange)
        assertNotNull(movement.session)
        movement.complete(change = current)
        assertNull(movement.session)
    }

    private fun movement(): HomeApplicationMovement = HomeApplicationMovement().apply(block = {
        updateModules(current = listOf(element = source))
        updateViewport(bounds = this@HomeApplicationPendingMovementTest.viewport)
        updateCreationTarget(
            type = OrderedFavoriteModuleType.Vertical,
            bounds = this@HomeApplicationPendingMovementTest.creation,
        )
    })

    private fun lift(movement: HomeApplicationMovement) {
        movement.updateItem(identity = identity, bounds = Rect(left = 0f, top = 0f, right = 300f, bottom = 56f))
        assertTrue(movement.start(identity = identity, module = source, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 50f, y = 28f)))
        movement.move(pointer = creation.center)
    }
}
