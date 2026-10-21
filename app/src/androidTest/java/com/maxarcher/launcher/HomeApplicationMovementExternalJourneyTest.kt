package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.maxarcher.launcher.ui.drawer.DrawerDragDrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeApplicationMovementExternalJourneyTest {
    private val viewport = Rect(left = 0f, top = 0f, right = 500f, bottom = 1000f)
    private val moduleBounds = Rect(left = 0f, top = 100f, right = 500f, bottom = 300f)
    private val itemBounds = Rect(left = 10f, top = 110f, right = 240f, bottom = 290f)
    private val creationBounds = Rect(left = 0f, top = 400f, right = 500f, bottom = 500f)

    private fun movementWithModule(): HomeApplicationMovement {
        val movement = HomeApplicationMovement()
        movement.updateViewport(bounds = viewport)
        movement.updateModules(
            current = listOf(
                OrderedFavoriteModule(
                    id = MODULE_ID,
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(identity(serial = 1)),
                ),
            ),
        )
        movement.updateModule(id = MODULE_ID, bounds = moduleBounds)
        movement.updateItem(
            identity = identity(serial = 1),
            bounds = itemBounds,
        )
        return movement
    }

    @Test
    fun externalJourneyResolvesInsertionInsideExistingModule() {
        val movement = movementWithModule()
        val external = identity(serial = 2)

        assertTrue(
            movement.startExternalJourney(
                identity = external,
                pointer = Offset(x = 120f, y = 150f),
            ),
        )
        // The pointer lands in the lower half of the only item, so the resolved
        // boundary is the edge after that item.
        movement.move(pointer = Offset(x = 120f, y = 250f))

        val drop = movement.finishExternalJourney()

        assertNotNull(drop)
        val insertion = drop as DrawerDragDrop.Insertion
        assertEquals(MODULE_ID, insertion.moduleId)
        assertEquals(1, insertion.boundary)
        assertFalse(movement.isDragging)
    }

    @Test
    fun externalJourneyResolvesCreationTarget() {
        val movement = movementWithModule()
        movement.updateCreationTarget(
            type = OrderedFavoriteModuleType.Vertical,
            bounds = creationBounds,
        )
        val external = identity(serial = 2)

        assertTrue(
            movement.startExternalJourney(
                identity = external,
                pointer = Offset(x = 250f, y = 150f),
            ),
        )
        movement.move(pointer = Offset(x = 250f, y = 450f))

        val drop = movement.finishExternalJourney()

        assertEquals(
            DrawerDragDrop.Creation(moduleType = OrderedFavoriteModuleType.Vertical),
            drop,
        )
    }

    @Test
    fun externalJourneyStartFailsWhileAnotherSessionIsActive() {
        val movement = movementWithModule()
        val member = identity(serial = 1)
        assertTrue(
            movement.start(
                identity = member,
                module = OrderedFavoriteModule(
                    id = MODULE_ID,
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(member),
                ),
                availability = FavoriteAvailability.Disabled(presentationEntry = null),
                pointer = Offset(x = 100f, y = 150f),
            ),
        )

        assertFalse(
            movement.startExternalJourney(
                identity = identity(serial = 2),
                pointer = Offset(x = 250f, y = 200f),
            ),
        )
        assertEquals(MODULE_ID, movement.session?.module?.id)
    }

    @Test
    fun finishRejectsHomeOriginatedSessionAndLeavesItIntact() {
        val movement = movementWithModule()
        val member = identity(serial = 1)
        assertTrue(
            movement.start(
                identity = member,
                module = OrderedFavoriteModule(
                    id = MODULE_ID,
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(member),
                ),
                availability = FavoriteAvailability.Disabled(presentationEntry = null),
                pointer = Offset(x = 100f, y = 150f),
            ),
        )

        assertNull(movement.finishExternalJourney())

        assertTrue(movement.isDragging)
        assertEquals(MODULE_ID, movement.session?.module?.id)
        movement.cancel()
    }

    @Test
    fun finishOutsideEveryDestinationReturnsNullAndCancels() {
        val movement = movementWithModule()
        assertTrue(
            movement.startExternalJourney(
                identity = identity(serial = 2),
                pointer = Offset(x = 250f, y = 150f),
            ),
        )
        movement.move(pointer = Offset(x = 250f, y = 950f))

        assertNull(movement.finishExternalJourney())

        assertFalse(movement.isDragging)
    }

    private fun identity(serial: Long) = LaunchableIdentity(
        profileSerialNumber = serial,
        componentName = ComponentName("com.example", "Main"),
    )

    private companion object {
        const val MODULE_ID = "vertical-list-1"
    }
}
