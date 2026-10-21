package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeApplicationMovementTest {
    private val identities = (1L..4L).map(
        transform = { serial -> LaunchableIdentity(profileSerialNumber = serial, componentName = ComponentName("test.app", "Main")) },
    )
    private val viewport = Rect(left = 0f, top = 0f, right = 200f, bottom = 200f)

    @Test
    fun singleColumnUsesHalvesAndNeverExchangesAtTheCenter() {
        val module = module(count = 3)
        val bounds = mapOf(
            identities[0] to rect(left = 0f, top = 0f, right = 200f, bottom = 40f),
            identities[1] to rect(left = 0f, top = 40f, right = 200f, bottom = 80f),
            identities[2] to rect(left = 0f, top = 80f, right = 200f, bottom = 120f),
        )
        assertEquals(1, resolve(module = module, bounds = bounds, x = 100f, y = 59f)?.boundary)
        val after = resolve(module = module, bounds = bounds, x = 100f, y = 60f)
        assertEquals(2, after?.boundary)
        assertEquals(Offset(x = 0f, y = 80f), after?.lineStart)
        assertEquals(Offset(x = 200f, y = 80f), after?.lineEnd)
    }

    @Test
    fun wrappedRowGapTieChoosesOnlyTheFollowingRowStartEdge() {
        val module = module(count = 4, columns = 2)
        val bounds = gridBounds()
        val insertion = resolve(module = module, bounds = bounds, x = 150f, y = 45f)
        assertEquals(2, insertion?.boundary)
        assertEquals(Offset(x = 0f, y = 50f), insertion?.lineStart)
        assertEquals(Offset(x = 0f, y = 90f), insertion?.lineEnd)
        assertEquals(2, resolve(module = module, bounds = bounds, x = 151f, y = 20f)?.boundary)
        assertEquals(3, resolve(module = module, bounds = bounds, x = 50f, y = 70f)?.boundary)
    }

    @Test
    fun finalRowBlankSpaceUsesLastRealBoundaryButTheAddCellIsInvalid() {
        val module = module(count = 3, columns = 2)
        val bounds = gridBounds().filterKeys(predicate = { it in module.identities })
        assertEquals(3, resolve(module = module, bounds = bounds, x = 150f, y = 70f)?.boundary)
        assertNull(
            resolve(
                module = module, bounds = bounds, x = 150f, y = 70f,
                add = rect(left = 100f, top = 50f, right = 200f, bottom = 90f),
            ),
        )
    }

    @Test
    fun ribbonGapsAndPartiallyVisibleItemsDoNotInventInvisibleFeedback() {
        val module = module(count = 3, ribbon = true)
        val bounds = mapOf(
            identities[0] to rect(left = -40f, top = 0f, right = 40f, bottom = 56f),
            identities[1] to rect(left = 48f, top = 0f, right = 128f, bottom = 56f),
            identities[2] to rect(left = 136f, top = 0f, right = 216f, bottom = 56f),
        )
        assertEquals(1, resolve(module = module, bounds = bounds, x = 44f, y = 20f)?.boundary)
        assertEquals(2, resolve(module = module, bounds = bounds, x = 100f, y = 20f)?.boundary)
        // The after edge lies outside the viewport: no visible line means no valid release.
        assertNull(resolve(module = module, bounds = bounds, x = 190f, y = 20f))
        assertNull(resolve(module = module, bounds = bounds, x = 210f, y = 20f))
    }

    @Test
    fun insertionUsesTheReducedOrderAndOnlyTheOriginalOrderIsNoChange() {
        val change = ApplicationOrderChange(
            moduleId = "module", identity = identities[1], originalOrder = identities, boundary = 3,
        )
        assertEquals(listOf(identities[0], identities[2], identities[3], identities[1]), change.reorderedIdentities())
        assertEquals(identities, change.copy(boundary = 1).reorderedIdentities())
        assertEquals(listOf(identities[0], identities[2], identities[1], identities[3]), change.copy(boundary = 2).reorderedIdentities())
        assertEquals(listOf(identities[1], identities[0], identities[2], identities[3]), change.copy(boundary = 0).reorderedIdentities())
        assertNull(change.copy(boundary = 4).reorderedIdentities())
    }

    @Test
    fun removalTargetIsExcludedAndPreviewOffsetSurvivesGeometryUpdates() {
        val movement = HomeApplicationMovement()
        val module = module(count = 4, columns = 2)
        movement.updateViewport(bounds = viewport)
        movement.updateModule(id = module.id, bounds = viewport)
        gridBounds().forEach(action = { (identity, bounds) -> movement.updateItem(identity = identity, bounds = bounds) })
        movement.updateItem(
            identity = identities[0], bounds = gridBounds().getValue(key = identities[0]),
            remove = rect(left = 0f, top = 0f, right = 20f, bottom = 20f),
        )
        assertNull(movement.hitIdentity(pointer = Offset(x = 10f, y = 10f)))
        assertEquals(identities[0], movement.hitIdentity(pointer = Offset(x = 30f, y = 20f)))
        assertTrue(movement.start(identity = identities[0], module = module, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 30f, y = 20f)))
        movement.move(pointer = Offset(x = 170f, y = 70f))
        movement.updateItem(identity = identities[0], bounds = rect(left = 0f, top = -20f, right = 100f, bottom = 20f))
        assertEquals(Offset(x = 140f, y = 50f), movement.session?.previewOrigin)
        assertFalse(movement.start(identity = identities[1], module = module, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 130f, y = 20f)))
        val change = movement.finish()
        assertNotNull(change)
        assertEquals(3, change?.boundary)
        assertFalse(movement.isDragging)
        assertNotNull(movement.session)
        movement.complete(change = checkNotNull(value = change))
        assertNull(movement.session)
        movement.cancel()
        assertNull(movement.finish())
    }

    private fun resolve(
        module: OrderedFavoriteModule,
        bounds: Map<LaunchableIdentity, Rect>,
        x: Float,
        y: Float,
        add: Rect? = null,
    ): ApplicationInsertion? = resolveApplicationInsertion(
        module = module, pointer = Offset(x = x, y = y), viewport = viewport,
        moduleBounds = viewport, itemBounds = bounds, addBounds = add,
    )

    private fun module(count: Int, columns: Int = 1, ribbon: Boolean = false): OrderedFavoriteModule =
        OrderedFavoriteModule(
            id = "module", type = if (ribbon) OrderedFavoriteModuleType.Ribbon else OrderedFavoriteModuleType.Vertical,
            identities = identities.take(n = count), itemsPerRow = columns,
        )

    private fun gridBounds(): Map<LaunchableIdentity, Rect> = mapOf(
        identities[0] to rect(left = 0f, top = 0f, right = 100f, bottom = 40f),
        identities[1] to rect(left = 100f, top = 0f, right = 200f, bottom = 40f),
        identities[2] to rect(left = 0f, top = 50f, right = 100f, bottom = 90f),
        identities[3] to rect(left = 100f, top = 50f, right = 200f, bottom = 90f),
    )

    private fun rect(left: Float, top: Float, right: Float, bottom: Float): Rect =
        Rect(left = left, top = top, right = right, bottom = bottom)
}
