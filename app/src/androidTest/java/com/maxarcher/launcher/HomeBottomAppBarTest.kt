package com.maxarcher.launcher

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeBottomAppBarTest {
    @Test
    fun capacityCountsOnlyWholeIconsAfterOuterMarginsInsetsAndGaps() {
        assertEquals(
            4,
            homeBottomAppBarCapacity(
                safeWidthDp = 280f,
                outerMarginDp = 16f,
                iconSizeDp = 40f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
        assertEquals(
            2,
            homeBottomAppBarCapacity(
                safeWidthDp = 223f,
                outerMarginDp = 16f,
                iconSizeDp = 48f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
        assertEquals(
            0,
            homeBottomAppBarCapacity(
                safeWidthDp = 80f,
                outerMarginDp = 16f,
                iconSizeDp = 48f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
    }

    @Test
    fun capacityChangesOnlyWhenAnotherWholeIconFitsInsideBothMargins() {
        val cases = listOf(
            0f to 0,
            111f to 0,
            112f to 1,
            175f to 1,
            176f to 2,
            239f to 2,
            240f to 3,
        )
        for ((width, expected) in cases) {
            assertEquals(
                "Safe width: $width",
                expected,
                homeBottomAppBarCapacity(
                    safeWidthDp = width,
                    iconSizeDp = 48f,
                    outerMarginDp = 16f,
                    horizontalInsetDp = 16f,
                    iconGapDp = 16f,
                ),
            )
        }
    }

    @Test
    fun insertionPlacesAModuleFavoriteOnTheBarAndRemovesTheSource() {
        val first = identity(serial = 1)
        val incoming = identity(serial = 3)
        val source = OrderedFavoriteModule(
            id = "module",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(element = incoming),
        )
        val aggregate = OrderedFavoriteAggregate(
            modules = listOf(element = source),
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                identities = listOf(first),
            ),
        )
        val change = ApplicationOrderChange(
            moduleId = "module",
            identity = incoming,
            originalOrder = listOf(element = incoming),
            boundary = 1,
            destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
            destinationOrder = listOf(first),
            destinationBar = true,
            barCapacity = 2,
        )

        val saved = applyApplicationOrderChange(aggregate = aggregate, change = change)

        assertEquals(listOf(first, incoming), saved?.homeBottomAppBar?.identities)
        assertEquals(emptyList<OrderedFavoriteModule>(), saved?.modules)
    }

    @Test
    fun fullBarRejectsANewMemberAndStillReordersItsMembers() {
        val first = identity(serial = 1)
        val second = identity(serial = 2)
        val incoming = identity(serial = 3)
        val source = OrderedFavoriteModule(
            id = "module",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(element = incoming),
        )
        val aggregate = OrderedFavoriteAggregate(
            modules = listOf(element = source),
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                identities = listOf(first, second),
            ),
        )
        val rejected = ApplicationOrderChange(
            moduleId = "module",
            identity = incoming,
            originalOrder = listOf(element = incoming),
            boundary = 2,
            destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
            destinationOrder = listOf(first, second),
            destinationBar = true,
            barCapacity = 2,
        )
        val reorder = ApplicationOrderChange(
            moduleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
            identity = second,
            originalOrder = listOf(first, second),
            boundary = 0,
            destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
            destinationOrder = listOf(first, second),
            destinationBar = true,
            barCapacity = 2,
        )

        assertNull(rejected.reorderedIdentities())
        assertNull(applyApplicationOrderChange(aggregate = aggregate, change = rejected))
        assertEquals(listOf(second, first), reorder.reorderedIdentities())
        assertEquals(
            listOf(second, first),
            applyApplicationOrderChange(aggregate = aggregate, change = reorder)
                ?.homeBottomAppBar
                ?.identities,
        )
    }

    @Test
    fun drawerDropAppendsUntilTheCapturedCapacityIsFull() {
        val first = identity(serial = 1)
        val incoming = identity(serial = 3)
        val open = ApplicationOrderChange(
            moduleId = EXTERNAL_JOURNEY_MODULE_ID,
            identity = incoming,
            originalOrder = listOf(element = incoming),
            boundary = 1,
            destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
            destinationOrder = listOf(first),
            destinationBar = true,
            barCapacity = 2,
        )
        val full = open.copy(boundary = 2, destinationOrder = listOf(first, identity(serial = 2)))

        assertEquals(listOf(first, incoming), open.reorderedIdentities())
        assertNull(full.reorderedIdentities())
    }

    private fun identity(serial: Long): LaunchableIdentity = LaunchableIdentity(
        profileSerialNumber = serial,
        componentName = ComponentName("test.app", "Main"),
    )
}
