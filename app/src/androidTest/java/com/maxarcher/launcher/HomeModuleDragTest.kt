package com.maxarcher.launcher

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeModuleDragTest {
    @Test
    fun insertionResolutionUsesRemainingModuleCenters() {
        val first = module("first", 1)
        val source = module("source", 2)
        val last = module("last", 3)
        val session = session(
            source = source,
            initial = listOf(first, source, last),
            remaining = listOf(first, last),
            insertionIndex = 1,
        )
        val bounds = mapOf(
            first.id to Rect(0f, 0f, 100f, 100f),
            last.id to Rect(0f, 100f, 100f, 200f),
        )

        assertEquals(
            0,
            session.advanced(
                amount = Offset(0f, -25f),
                listBoundsInWindow = Rect(0f, 0f, 100f, 300f),
                moduleBoundsInWindow = bounds,
            ).insertionIndex,
        )
        assertEquals(
            2,
            session.advanced(
                amount = Offset(0f, 125f),
                listBoundsInWindow = Rect(0f, 0f, 100f, 300f),
                moduleBoundsInWindow = bounds,
            ).insertionIndex,
        )
    }

    @Test
    fun releaseOutsideTheListHasNoCompletedOrder() {
        val first = module("first", 1)
        val source = module("source", 2)
        val moved = session(
            source = source,
            initial = listOf(first, source),
            remaining = listOf(first),
            insertionIndex = 1,
        ).advanced(
            amount = Offset(150f, 0f),
            listBoundsInWindow = Rect(0f, 0f, 100f, 200f),
            moduleBoundsInWindow = mapOf(first.id to Rect(0f, 0f, 100f, 100f)),
        )

        assertNull(moved.insertionIndex)
        assertNull(moved.completedModules())
    }

    @Test
    fun completedOrderReturnsOnlyAChangedOrder() {
        val first = module("first", 1)
        val source = module("source", 2)
        val last = module("last", 3)
        val initial = listOf(first, source, last)

        assertNull(
            session(source, initial, listOf(first, last), insertionIndex = 1)
                .completedModules(),
        )
        assertEquals(
            listOf("source", "first", "last"),
            session(source, initial, listOf(first, last), insertionIndex = 0)
                .completedModules()
                ?.map(OrderedFavoriteModule::id),
        )
    }

    private fun session(
        source: OrderedFavoriteModule,
        initial: List<OrderedFavoriteModule>,
        remaining: List<OrderedFavoriteModule>,
        insertionIndex: Int?,
    ) = ModuleDragSession(
        sourceModule = source,
        sourceSelected = false,
        sourceAvailability = emptyMap(),
        initialModules = initial,
        remainingModules = remaining,
        insertionIndex = insertionIndex,
        originInWindow = Offset.Zero,
        size = IntSize(100, 100),
        touchStartInWindow = Offset(50f, 50f),
    )

    private fun module(id: String, profileSerialNumber: Long) = OrderedFavoriteModule(
        id = id,
        type = OrderedFavoriteModuleType.Vertical,
        identities = listOf(
            LaunchableIdentity(
                profileSerialNumber,
                ComponentName("com.example.$id", "Main"),
            ),
        ),
    )
}
