package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeApplicationCrossModuleGeometryTest {
    private val first = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.first", "Main"))
    private val second = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.second", "Main"))
    private val source = OrderedFavoriteModule(id = "source", type = OrderedFavoriteModuleType.Vertical, identities = listOf(element = first))
    private val target = OrderedFavoriteModule(id = "target", type = OrderedFavoriteModuleType.Ribbon, identities = listOf(element = second))
    private val viewport = Rect(left = 0f, top = 0f, right = 200f, bottom = 200f)
    private val sourceBounds = Rect(left = 0f, top = 0f, right = 200f, bottom = 80f)
    private val targetBounds = Rect(left = 0f, top = 88f, right = 200f, bottom = 144f)
    private val cells = mapOf(
        first to Rect(left = 0f, top = 0f, right = 200f, bottom = 40f),
        second to Rect(left = 0f, top = 88f, right = 100f, bottom = 144f),
    )
    private val additions = mapOf(
        source.id to Rect(left = 0f, top = 40f, right = 200f, bottom = 80f),
        target.id to Rect(left = 100f, top = 88f, right = 200f, bottom = 144f),
    )

    @Test
    fun targetUsesItsOwnAxisAndGapTieChoosesFollowingModuleStart() {
        val direct = resolve(pointer = Offset(x = 75f, y = 110f))
        assertEquals(target, direct?.first)
        assertEquals(1, direct?.second?.boundary)
        assertEquals(Offset(x = 100f, y = 88f), direct?.second?.lineStart)
        val gap = resolve(pointer = Offset(x = 150f, y = 84f))
        assertEquals(target, gap?.first)
        assertEquals(0, gap?.second?.boundary)
        assertEquals(Offset(x = 0f, y = 88f), gap?.second?.lineStart)
        assertEquals(source, resolve(pointer = Offset(x = 150f, y = 83f))?.first)
    }

    @Test
    fun trailingAddCellsAndSpaceAfterFinalModuleAreNotImplicitDestinations() {
        assertNull(resolve(pointer = Offset(x = 100f, y = 60f)))
        assertNull(resolve(pointer = Offset(x = 150f, y = 110f)))
        assertNull(resolve(pointer = Offset(x = 100f, y = 170f)))
        assertNull(resolve(pointer = Offset(x = 100f, y = -1f)))
    }

    @Test
    fun virtualizingSourceDoesNotEndMovementOrChangeItsFrozenPresentation() {
        val movement = HomeApplicationMovement()
        movement.updateModules(current = listOf(source, target))
        movement.updateViewport(bounds = viewport)
        movement.updateModule(id = source.id, bounds = sourceBounds)
        movement.updateModule(id = target.id, bounds = targetBounds)
        cells.forEach(action = { (identity, bounds) -> movement.updateItem(identity = identity, bounds = bounds) })
        additions.forEach(action = { (id, bounds) -> movement.updateAdd(id = id, bounds = bounds) })
        assertTrue(movement.start(identity = first, module = source, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 60f, y = 20f)))
        movement.updateItem(identity = first, bounds = null)
        movement.updateModule(id = source.id, bounds = null)
        movement.move(pointer = Offset(x = 75f, y = 110f))
        assertEquals(source, movement.session?.module)
        assertEquals(cells[first], movement.session?.sourceBounds)
        assertEquals(target, movement.session?.destination)
        val result = movement.finish()
        assertEquals(target.id, result?.destinationModuleId)
        assertEquals(listOf(second, first), result?.reorderedIdentities())
        movement.complete(change = checkNotNull(value = result))
        assertNull(movement.activeIdentity)
    }

    @Test
    fun edgeOwnershipIsSingleAxisAndAnExhaustedRibbonCanYieldToTheMainViewport() {
        val ribbon = target.id to Rect(left = 0f, top = 144f, right = 200f, bottom = 200f)
        fun request(horizontalAvailable: Boolean): HomeApplicationScrollRequest? = resolveHomeApplicationEdgeScroll(
            pointer = Offset(x = 199f, y = 190f), viewport = viewport, band = 48f,
            mainCanScrollBackward = false, mainCanScrollForward = true,
            ribbon = ribbon, ribbonCanScrollForward = horizontalAvailable,
        )
        assertEquals(HomeApplicationScrollOwner(ribbonId = target.id, direction = 1), request(horizontalAvailable = true)?.owner)
        assertEquals(HomeApplicationScrollOwner(ribbonId = null, direction = 1), request(horizontalAvailable = false)?.owner)
        assertNull(resolveHomeApplicationEdgeScroll(
            pointer = Offset(x = 199f, y = 190f), viewport = viewport, band = 48f,
            mainCanScrollBackward = false, mainCanScrollForward = false, ribbon = ribbon,
        ))
    }

    @Test
    fun edgeResidenceKeyIgnoresProximityButChangesForContainerAxisAndDirection() {
        fun main(y: Float): HomeApplicationScrollRequest? = resolveHomeApplicationEdgeScroll(
            pointer = Offset(x = 100f, y = y), viewport = viewport, band = 48f,
            mainCanScrollBackward = true, mainCanScrollForward = true,
        )
        val inner = main(y = 152f)
        val middle = main(y = 176f)
        val outer = main(y = 200f)
        assertEquals(inner?.owner, middle?.owner)
        assertEquals(middle?.owner, outer?.owner)
        assertEquals(0f, inner?.proximity)
        assertEquals(0.5f, middle?.proximity)
        assertEquals(1f, outer?.proximity)
        assertEquals(-1, main(y = 1f)?.owner?.direction)
        assertNull(main(y = 100f))
        assertNull(main(y = 201f))
        val narrow = resolveHomeApplicationEdgeScroll(
            pointer = Offset(x = 60f, y = 20f), viewport = viewport, band = 48f,
            mainCanScrollBackward = false, mainCanScrollForward = false,
            ribbon = target.id to Rect(left = 0f, top = 0f, right = 60f, bottom = 40f), ribbonCanScrollForward = true,
        )
        assertEquals(30f, narrow?.band)
        assertEquals(1f, narrow?.proximity)
    }

    @Test
    fun creationHoverReplacesInsertionAndOnlyTheMainEntriesCreateModules() {
        val movement = HomeApplicationMovement()
        movement.updateModules(current = listOf(source, target))
        movement.updateViewport(bounds = viewport)
        movement.updateModule(id = source.id, bounds = sourceBounds)
        movement.updateModule(id = target.id, bounds = targetBounds)
        cells.forEach(action = { (identity, bounds) -> movement.updateItem(identity = identity, bounds = bounds) })
        additions.forEach(action = { (id, bounds) -> movement.updateAdd(id = id, bounds = bounds) })
        movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = Rect(left = 0f, top = 152f, right = 96f, bottom = 200f))
        movement.updateCreationTarget(type = OrderedFavoriteModuleType.Ribbon, bounds = Rect(left = 104f, top = 152f, right = 200f, bottom = 200f))
        assertTrue(movement.start(identity = first, module = source, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 60f, y = 20f)))
        movement.move(pointer = Offset(x = 50f, y = 170f))
        assertEquals(OrderedFavoriteModuleType.Vertical, movement.session?.creation?.first)
        assertNull(movement.session?.insertion)
        assertNull(movement.session?.destination)
        movement.move(pointer = Offset(x = 150f, y = 110f))
        assertNull(movement.session?.creation)
        assertNull(movement.session?.insertion)
        movement.move(pointer = Offset(x = 100f, y = 170f))
        assertNull(movement.session?.creation)
        movement.move(pointer = Offset(x = 75f, y = 110f))
        assertEquals(target, movement.session?.destination)
        assertNull(movement.session?.creation)
        movement.move(pointer = Offset(x = 150f, y = 170f))
        val result = movement.finish()
        assertEquals(OrderedFavoriteModuleType.Ribbon, result?.newModuleType)
        movement.complete(change = checkNotNull(value = result))
        assertNull(movement.session)
    }

    @Test
    fun disposedCreationEntryAndCancellationCannotCommitAnOldDrop() {
        val movement = HomeApplicationMovement()
        movement.updateViewport(bounds = viewport)
        movement.updateItem(identity = first, bounds = cells.getValue(key = first))
        movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = Rect(left = 0f, top = 152f, right = 96f, bottom = 200f))
        assertTrue(movement.start(identity = first, module = source, availability = FavoriteAvailability.Disabled(presentationEntry = null), pointer = Offset(x = 60f, y = 20f)))
        movement.move(pointer = Offset(x = 50f, y = 170f))
        movement.updateCreationTarget(type = OrderedFavoriteModuleType.Vertical, bounds = null)
        assertNull(movement.finish())
        movement.cancel()
        assertNull(movement.finish())
    }

    @Test
    fun creationRejectsCollidingModuleIdWithoutReplacingExistingData() {
        val aggregate = OrderedFavoriteAggregate(modules = listOf(source, target))
        val change = ApplicationOrderChange(
            moduleId = source.id, identity = first, originalOrder = source.identities,
            boundary = 0, newModuleType = OrderedFavoriteModuleType.Ribbon,
        )
        assertNull(applyApplicationOrderChange(aggregate = aggregate, change = change, newModuleId = target.id))
        assertNull(applyApplicationOrderChange(aggregate = aggregate, change = change, newModuleId = ""))
        assertEquals(listOf(source, target), aggregate.modules)
    }

    private fun resolve(pointer: Offset): Pair<OrderedFavoriteModule, ApplicationInsertion>? = resolveApplicationDestination(
        modules = listOf(source, target), pointer = pointer, viewport = viewport,
        moduleBounds = mapOf(source.id to sourceBounds, target.id to targetBounds), itemBounds = cells, addBounds = additions,
    )
}
