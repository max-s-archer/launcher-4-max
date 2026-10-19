package com.maxarchm.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBottomAppBarTest {
    @Test
    fun capacityCountsOnlyWholeIconsAfterInsetsAndGaps() {
        assertEquals(
            4,
            homeBottomAppBarCapacity(
                safeWidthDp = 280f,
                iconSizeDp = 40f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
        assertEquals(
            3,
            homeBottomAppBarCapacity(
                safeWidthDp = 223f,
                iconSizeDp = 48f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
        assertEquals(
            0,
            homeBottomAppBarCapacity(
                safeWidthDp = 80f,
                iconSizeDp = 48f,
                horizontalInsetDp = 16f,
                iconGapDp = 16f,
            ),
        )
    }
}
