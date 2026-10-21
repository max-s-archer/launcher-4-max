package com.maxarcher.launcher

import android.content.ComponentName
import android.graphics.drawable.ColorDrawable
import android.os.Process
import com.maxarcher.launcher.ui.drawer.DrawerDragEligibility
import com.maxarcher.launcher.ui.drawer.resolveDrawerDragEligibility
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveDrawerDragEligibilityTest {
    @Test
    fun memberIdentityIsAlreadyFavorited() {
        assertEquals(
            DrawerDragEligibility.AlreadyFavorited,
            resolveDrawerDragEligibility(
                isFavoriteMember = true,
                availability = FavoriteAvailability.Unknown(presentationEntry = null),
            ),
        )
    }

    @Test
    fun reliablyDisabledIdentityIsNotDragEligible() {
        assertEquals(
            DrawerDragEligibility.ReliablyDisabled,
            resolveDrawerDragEligibility(
                isFavoriteMember = false,
                availability = FavoriteAvailability.Disabled(presentationEntry = null),
            ),
        )
    }

    @Test
    fun availableTemporarilyUnavailableUnknownAndMissingStatesStayEligible() {
        listOf(
            null,
            FavoriteAvailability.Available(entry = testEntry()),
            FavoriteAvailability.TemporarilyUnavailable(presentationEntry = null),
            FavoriteAvailability.Unknown(presentationEntry = null),
        ).forEach { availability ->
            assertEquals(
                DrawerDragEligibility.Eligible,
                resolveDrawerDragEligibility(
                    isFavoriteMember = false,
                    availability = availability,
                ),
            )
        }
    }

    private fun testEntry() = LaunchableEntry(
        identity = LaunchableIdentity(
            profileSerialNumber = 1,
            componentName = ComponentName("com.example", "Main"),
        ),
        user = Process.myUserHandle(),
        label = "Example",
        icon = ColorDrawable(),
    )
}
