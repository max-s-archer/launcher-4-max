package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRibbonInsertionGeometryTest {
    private val identities = (1L..3L).map(
        transform = { serial -> LaunchableIdentity(profileSerialNumber = serial, componentName = ComponentName("test.app", "Main")) },
    )
    private val module = OrderedFavoriteModule(id = "ribbon", type = OrderedFavoriteModuleType.Ribbon, identities = identities)
    private val viewport = Rect(left = 0f, top = 0f, right = 360f, bottom = 56f)
    private val bounds = mapOf(
        identities[0] to Rect(left = 0f, top = 0f, right = 60f, bottom = 56f),
        identities[1] to Rect(left = 68f, top = 0f, right = 188f, bottom = 56f),
        identities[2] to Rect(left = 196f, top = 0f, right = 276f, bottom = 56f),
    )
    private val add = Rect(left = 284f, top = 0f, right = 360f, bottom = 56f)

    @Test
    fun unequalWidthNeighborsShareOneGapAxisAcrossAllEquivalentHitRegions() {
        listOf(40f, 60f, 63f, 64f, 67f, 68f, 100f).forEach(
            action = { x ->
                val insertion = resolve(x = x)
                assertEquals(1, insertion?.boundary)
                assertEquals(Offset(x = 64f, y = 0f), insertion?.lineStart)
                assertEquals(Offset(x = 64f, y = 56f), insertion?.lineEnd)
            },
        )
        assertEquals(Offset(x = 192f, y = 0f), resolve(x = 150f)?.lineStart)
        assertEquals(resolve(x = 150f), resolve(x = 200f))
    }

    @Test
    fun firstAndLastBoundariesUseActualApplicationAndTrailingEntryGeometry() {
        assertEquals(0, resolve(x = 10f)?.boundary)
        assertEquals(Offset(x = 0f, y = 0f), resolve(x = 10f)?.lineStart)
        assertEquals(3, resolve(x = 250f)?.boundary)
        assertEquals(Offset(x = 280f, y = 0f), resolve(x = 250f)?.lineStart)
        assertEquals(resolve(x = 250f), resolve(x = 282f))
        assertNull(resolve(x = 284f))
        assertNull(resolve(x = 320f))
    }

    @Test
    fun currentGeometryMovesTheGapWithoutSnappingToAViewportEdge() {
        val shifted = bounds.mapValues(transform = { (_, rect) -> rect.translate(offset = Offset(x = -80f, y = 0f)) })
        val shiftedAdd = add.translate(offset = Offset(x = -80f, y = 0f))
        assertNull(resolve(x = 10f, items = shifted, addition = shiftedAdd))
        assertEquals(Offset(x = 112f, y = 0f), resolve(x = 100f, items = shifted, addition = shiftedAdd)?.lineStart)
        // A reflow can temporarily widen the gap; do not assume the resource spacing is its visible width.
        val reflow = bounds + (identities[1] to bounds.getValue(key = identities[1]).translate(offset = Offset(x = 20f, y = 0f)))
        assertEquals(Offset(x = 74f, y = 0f), resolve(x = 40f, items = reflow)?.lineStart)
        assertEquals(resolve(x = 40f, items = reflow), resolve(x = 100f, items = reflow))
    }

    @Test
    fun missingNeighborsNeverTurnTheFirstVisibleApplicationIntoTheModuleStart() {
        val missingFirst = bounds.filterKeys(predicate = { it != identities[0] })
        assertNull(resolve(x = 70f, items = missingFirst))
        assertEquals(2, resolve(x = 150f, items = missingFirst)?.boundary)
        assertNull(resolve(x = 250f, addition = null))
    }

    @Test
    fun boundaryCarriesTheClippingRegionForTheEntireStroke() {
        val clip = Rect(left = 64f, top = 10f, right = 200f, bottom = 40f)
        val insertion = resolve(x = 90f, visible = clip)
        assertEquals(Offset(x = 64f, y = 10f), insertion?.lineStart)
        assertEquals(Offset(x = 64f, y = 40f), insertion?.lineEnd)
        assertEquals(clip, insertion?.clipBounds)
    }

    @Test
    fun interModuleGapAndDirectRibbonHitShareTheSameEndBoundary() {
        val next = OrderedFavoriteModule(
            id = "next", type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(element = LaunchableIdentity(profileSerialNumber = 4, componentName = ComponentName("test.app", "Main"))),
        )
        val insertion = resolveApplicationDestination(
            modules = listOf(module, next), pointer = Offset(x = 300f, y = 58f),
            viewport = Rect(left = 0f, top = 0f, right = 360f, bottom = 120f),
            moduleBounds = mapOf(module.id to viewport, next.id to Rect(left = 0f, top = 64f, right = 360f, bottom = 120f)),
            itemBounds = bounds, addBounds = mapOf(module.id to add),
        )
        assertEquals(module, insertion?.first)
        assertEquals(resolve(x = 250f), insertion?.second)
    }

    private fun resolve(
        x: Float,
        items: Map<LaunchableIdentity, Rect> = bounds,
        addition: Rect? = add,
        visible: Rect = viewport,
    ): ApplicationInsertion? = resolveApplicationInsertion(
        module = module, pointer = Offset(x = x, y = 28f), viewport = visible,
        moduleBounds = viewport, itemBounds = items, addBounds = addition,
    )
}
