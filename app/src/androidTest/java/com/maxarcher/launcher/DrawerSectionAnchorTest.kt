package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import com.maxarcher.launcher.ui.drawer.DrawerListPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerRestorationTarget
import com.maxarcher.launcher.ui.drawer.DrawerSection
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation
import com.maxarcher.launcher.ui.drawer.captureDrawerListPosition
import com.maxarcher.launcher.ui.drawer.captureDrawerOrdinaryListPosition
import com.maxarcher.launcher.ui.drawer.drawerPinnedAnchorTop
import com.maxarcher.launcher.ui.drawer.drawerSectionRanges
import com.maxarcher.launcher.ui.drawer.headerItemCount
import com.maxarcher.launcher.ui.drawer.resolveDrawerOrdinaryRestorationTarget
import com.maxarcher.launcher.ui.drawer.resolveDrawerRestorationTarget
import com.maxarcher.launcher.ui.drawer.validItemsPerRowRange

class DrawerSectionAnchorTest {
    @Test
    fun sectionRangesAndPositionsRoundTripForEveryArrangement() {
        val sections = listOf(section("A", 9), section("B", 5))
        for (placement in DrawerNamePlacement.entries) {
            for (columns in validItemsPerRowRange(placement)) {
                for (anchors in DrawerSectionAnchorPresentation.entries) {
                    val ranges = drawerSectionRanges(sections, columns, anchors, true)
                    assertEquals(anchors.headerItemCount + (9 + columns - 1) / columns, ranges[1].startIndex)
                    assertEquals(ranges[1].endIndex, ranges[2].startIndex)
                    for (index in 0 until ranges.last().endIndex) {
                        val captured = captureDrawerOrdinaryListPosition(
                            sections, index, 7, columns, anchors,
                        )
                        assertNotNull(captured)
                        assertEquals(
                            DrawerRestorationTarget(index, 7),
                            resolveDrawerOrdinaryRestorationTarget(checkNotNull(captured), sections, columns, anchors),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun geometryChangePreservesIdentityWhenInventoryInsertsBeforeIt() {
        val original = section("A", 10)
        val captured = checkNotNull(captureDrawerListPosition(
            sections = listOf(original),
            firstVisibleItemIndex = 4,
            firstVisibleItemScrollOffset = 13,
            preserveApplicationIdentity = true,
        ))
        val updated = original.copy(entries = listOf(entry("new")) + original.entries)
        assertEquals(
            DrawerRestorationTarget(2, 13),
            resolveDrawerRestorationTarget(captured, listOf(updated), 2, DrawerSectionAnchorPresentation.LeftSide),
        )
        // The old fourth application is now fifth and remains in the top row.
        assertEquals(original.entries[3].identity, captured.applicationIdentity)
    }

    @Test
    fun settingsAndMissingSectionNeverResolveToAnApplicationByMistake() {
        val sections = listOf(section("A", 3), section("C", 2))
        val settings = checkNotNull(captureDrawerOrdinaryListPosition(sections, 8, 4))
        assertEquals(
            DrawerRestorationTarget(5, 4),
            resolveDrawerOrdinaryRestorationTarget(settings, sections, 1, DrawerSectionAnchorPresentation.LeftSide),
        )
        assertEquals(
            DrawerRestorationTarget(3, 0),
            resolveDrawerRestorationTarget(DrawerListPosition("B", 1, 8), sections, 1, DrawerSectionAnchorPresentation.LeftSide),
        )
        assertEquals(
            DrawerRestorationTarget(4, 0),
            resolveDrawerRestorationTarget(DrawerListPosition("Z", 1, 8), sections, 1, DrawerSectionAnchorPresentation.LeftSide),
        )
    }

    @Test
    fun anchorFollowsItsSectionPinsAndLeavesBeforeTheNextSection() {
        assertEquals(108, drawerPinnedAnchorTop(100, 300, 28, 8))
        assertEquals(8, drawerPinnedAnchorTop(-100, 200, 28, 8))
        assertEquals(-18, drawerPinnedAnchorTop(-100, 10, 28, 8))
        assertEquals(18, drawerPinnedAnchorTop(10, 200, 28, 8))
        assertEquals(-6, drawerPinnedAnchorTop(-50, 10, 16, 8))
    }

    private fun section(label: String, count: Int) =
        DrawerSection(label, List(count) { entry("$label$it") })

    private fun entry(name: String) = LaunchableEntry(
        identity = LaunchableIdentity(0, ComponentName("example.$name", "Main")),
        user = Process.myUserHandle(),
        label = name,
        icon = ColorDrawable(Color.TRANSPARENT),
    )
}
