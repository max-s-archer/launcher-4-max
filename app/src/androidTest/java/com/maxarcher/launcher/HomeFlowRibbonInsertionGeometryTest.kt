package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A flow ribbon resolves like a multi-item vertical module, by its wrapped row geometry. */
class HomeFlowRibbonInsertionGeometryTest {
    private val identities = (1L..4L).map(
        transform = { serial ->
            LaunchableIdentity(
                profileSerialNumber = serial,
                componentName = ComponentName("test.app", "Main"),
            )
        },
    )
    private val module = OrderedFavoriteModule(
        id = "flow",
        type = OrderedFavoriteModuleType.Ribbon,
        displayMode = OrderedFavoriteDisplayMode.Flow,
        identities = identities,
    )
    private val viewport = Rect(left = 0f, top = 0f, right = 360f, bottom = 120f)
    private val bounds = mapOf(
        identities[0] to Rect(left = 0f, top = 0f, right = 120f, bottom = 56f),
        identities[1] to Rect(left = 128f, top = 0f, right = 248f, bottom = 56f),
        identities[2] to Rect(left = 0f, top = 64f, right = 80f, bottom = 120f),
    )

    private fun resolve(
        x: Float,
        y: Float,
        items: Map<LaunchableIdentity, Rect> = bounds,
        addition: Rect? = null,
    ): ApplicationInsertion? = resolveApplicationInsertion(
        module = module, pointer = Offset(x = x, y = y), viewport = viewport,
        moduleBounds = viewport, itemBounds = items, addBounds = addition,
    )

    @Test
    fun wrappedRowHalfRuleMatchesTheMultiColumnGeometry() {
        assertEquals(1, resolve(x = 100f, y = 20f)?.boundary)
        assertEquals(2, resolve(x = 140f, y = 20f)?.boundary)
        assertEquals(2, resolve(x = 40f, y = 90f)?.boundary)
        val insertion = resolve(x = 100f, y = 90f)
        assertEquals(3, insertion?.boundary)
        assertEquals(Offset(x = 0f, y = 64f), insertion?.lineStart)
        assertEquals(Offset(x = 80f, y = 120f), insertion?.lineEnd)
    }

    @Test
    fun rowGapTieResolvesToTheFollowingRowStartEdge() {
        val insertion = resolve(x = 200f, y = 58f)
        assertEquals(2, insertion?.boundary)
        assertEquals(Offset(x = 128f, y = 64f), insertion?.lineStart)
        assertEquals(Offset(x = 128f, y = 120f), insertion?.lineEnd)
    }

    @Test
    fun finalRowBlankSpaceUsesTheLastRealBoundary() {
        assertEquals(3, resolve(x = 200f, y = 90f)?.boundary)
    }

    @Test
    fun ribbonLayoutBranchStillUsesTheSharedGapAxis() {
        val ribbon = module.copy(displayMode = OrderedFavoriteDisplayMode.Ribbon)
        val singleRow = mapOf(
            identities[0] to Rect(left = 0f, top = 0f, right = 120f, bottom = 56f),
            identities[1] to Rect(left = 128f, top = 0f, right = 248f, bottom = 56f),
        )
        val insertion = resolveApplicationInsertion(
            module = ribbon, pointer = Offset(x = 124f, y = 28f), viewport = viewport,
            moduleBounds = viewport, itemBounds = singleRow, addBounds = null,
        )
        assertEquals(1, insertion?.boundary)
        assertEquals(Offset(x = 124f, y = 0f), insertion?.lineStart)
    }

    @Test
    fun theBottomAppBarStillUsesTheRibbonAxisRegardlessOfDisplayMode() {
        val barIdentities = identities.take(n = 2)
        val barBounds = mapOf(
            barIdentities[0] to Rect(left = 16f, top = 4f, right = 136f, bottom = 60f),
            barIdentities[1] to Rect(left = 144f, top = 4f, right = 264f, bottom = 60f),
        )
        val insertion = resolveHomeBottomAppBarInsertion(
            bounds = Rect(left = 0f, top = 0f, right = 360f, bottom = 64f),
            identities = barIdentities, capacity = 6, addBounds = null,
            itemBounds = barBounds, pointer = Offset(x = 140f, y = 32f),
            movingIdentity = null,
        )
        assertEquals(1, insertion?.boundary)
        assertEquals(Offset(x = 140f, y = 4f), insertion?.lineStart)
        assertEquals(Offset(x = 140f, y = 60f), insertion?.lineEnd)
    }
}
