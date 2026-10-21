package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeApplicationSourceOmissionTest {
    private val identities = (1L..4L).map(
        transform = { serial -> LaunchableIdentity(profileSerialNumber = serial, componentName = ComponentName("test.app", "Main")) },
    )
    private val viewport = Rect(left = 0f, top = 0f, right = 300f, bottom = 200f)

    @Test
    fun everySourceAndReducedBoundaryInsertsExactlyOnceWithoutChangingOtherModules() {
        for (type in OrderedFavoriteModuleType.entries) {
            val source = OrderedFavoriteModule(id = "source", type = type, identities = identities.take(n = 3))
            val other = OrderedFavoriteModule(id = "other", type = type, identities = identities.takeLast(n = 1))
            val aggregate = OrderedFavoriteAggregate(modules = listOf(source, other))
            for (moving in source.identities) {
                val remaining = applicationLayoutIdentities(module = source, movingIdentity = moving)
                for (boundary in 0..remaining.size) {
                    val change = ApplicationOrderChange(moduleId = source.id, identity = moving, originalOrder = source.identities, boundary = boundary)
                    val expected = remaining.toMutableList().apply(block = { add(index = boundary, element = moving) })
                    assertEquals(expected, change.reorderedIdentities())
                    val saved = applyApplicationOrderChange(aggregate = aggregate, change = change)
                    assertEquals(listOf(source.copy(identities = expected), other), saved?.modules)
                    assertEquals(identities, aggregate.identities)
                }
            }
        }
    }

    @Test
    fun singletonLayoutIsEmptyWithoutChangingTheDurableModuleOrExposingAnInsertionBoundary() {
        for (type in OrderedFavoriteModuleType.entries) {
            val source = OrderedFavoriteModule(id = "source", type = type, identities = identities.take(n = 1))
            val other = OrderedFavoriteModule(id = "other", type = type, identities = identities.takeLast(n = 1))
            val sourceBounds = Rect(left = 0f, top = 0f, right = 300f, bottom = 56f)
            val bounds = mapOf(identities[0] to sourceBounds)
            assertTrue(applicationLayoutIdentities(module = source, movingIdentity = identities[0]).isEmpty())
            assertEquals(identities.take(n = 1), source.identities)
            assertEquals(source.identities, applicationLayoutIdentities(module = source, movingIdentity = null))
            assertNull(resolveApplicationInsertion(
                module = source, pointer = sourceBounds.center, viewport = viewport, moduleBounds = sourceBounds,
                itemBounds = bounds, addBounds = sourceBounds, movingIdentity = identities[0],
            ))
            // The nearer edge belongs to the temporarily empty source: do not synthesize a boundary.
            assertNull(resolveApplicationDestination(
                modules = listOf(source, other), pointer = Offset(x = 150f, y = 58f), viewport = viewport,
                moduleBounds = mapOf(source.id to sourceBounds, other.id to Rect(left = 0f, top = 64f, right = 300f, bottom = 120f)),
                itemBounds = bounds, addBounds = mapOf(source.id to sourceBounds), movingIdentity = identities[0],
            ))
        }
    }

    @Test
    fun wrappedRowCandidatesUseCompactedIndicesEvenIfTheOldSourceBoundsStillExist() {
        val source = OrderedFavoriteModule(id = "source", type = OrderedFavoriteModuleType.Vertical, identities = identities, itemsPerRow = 2)
        val firstCell = Rect(left = 0f, top = 0f, right = 150f, bottom = 56f)
        val bounds = mapOf(
            identities[0] to firstCell,
            identities[1] to firstCell,
            identities[2] to Rect(left = 150f, top = 0f, right = 300f, bottom = 56f),
            identities[3] to Rect(left = 0f, top = 56f, right = 150f, bottom = 112f),
        )
        val insertion = resolveApplicationInsertion(
            module = source, pointer = Offset(x = 30f, y = 80f), viewport = viewport, moduleBounds = viewport,
            itemBounds = bounds, addBounds = Rect(left = 150f, top = 56f, right = 300f, bottom = 112f),
            movingIdentity = identities[1],
        )
        assertEquals(2, insertion?.boundary)
        assertEquals(Offset(x = 0f, y = 56f), insertion?.lineStart)
        val change = ApplicationOrderChange(moduleId = source.id, identity = identities[1], originalOrder = identities, boundary = checkNotNull(value = insertion).boundary)
        assertEquals(listOf(identities[0], identities[2], identities[1], identities[3]), change.reorderedIdentities())
    }

    @Test
    fun ribbonReflowUsesReducedBoundariesAndCancellationKeepsTheOriginalOrder() {
        val source = OrderedFavoriteModule(id = "source", type = OrderedFavoriteModuleType.Ribbon, identities = identities.take(n = 3))
        val movement = HomeApplicationMovement()
        movement.updateModules(current = listOf(element = source))
        movement.updateViewport(bounds = viewport)
        movement.updateModule(id = source.id, bounds = Rect(left = 0f, top = 0f, right = 300f, bottom = 56f))
        fun lift() {
            movement.updateItem(identity = identities[0], bounds = Rect(left = 0f, top = 0f, right = 80f, bottom = 56f))
            assertTrue(movement.start(
                identity = identities[0], module = source, availability = FavoriteAvailability.Disabled(presentationEntry = null),
                pointer = Offset(x = 30f, y = 28f),
            ))
            // Published layout bounds after closing the source gap.
            movement.updateItem(identity = identities[1], bounds = Rect(left = 0f, top = 0f, right = 80f, bottom = 56f))
            movement.updateItem(identity = identities[2], bounds = Rect(left = 88f, top = 0f, right = 168f, bottom = 56f))
            movement.updateAdd(id = source.id, bounds = Rect(left = 176f, top = 0f, right = 256f, bottom = 56f))
        }
        lift()
        movement.move(pointer = Offset(x = 60f, y = 28f))
        assertEquals(Offset(x = 84f, y = 0f), movement.session?.insertion?.lineStart)
        val result = checkNotNull(value = movement.finish())
        assertEquals(listOf(identities[1], identities[0], identities[2]), result.reorderedIdentities())
        movement.complete(change = result)
        assertEquals(identities.take(n = 3), source.identities)
        lift()
        // Before the first remaining item restores the original order without requesting a save.
        assertNull(movement.finish())
        lift()
        movement.move(pointer = Offset(x = 150f, y = 28f))
        movement.cancel()
        assertNull(movement.activeIdentity)
        assertNull(movement.session)
        assertNull(movement.finish())
        assertEquals(source.identities, applicationLayoutIdentities(module = source, movingIdentity = movement.activeIdentity))
    }
}
