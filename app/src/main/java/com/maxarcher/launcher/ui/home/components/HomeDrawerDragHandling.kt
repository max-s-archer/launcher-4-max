package com.maxarcher.launcher.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import com.maxarcher.launcher.EXTERNAL_JOURNEY_MODULE_ID
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteReadState
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.ui.drawer.DrawerDragDrop
import com.maxarcher.launcher.ui.drawer.DrawerDragJourney

/** Coordinates the Drawer-to-Home drag lifecycle against the shared Home movement model. */
@Composable
internal fun HomeDrawerDragHandling(
    movement: HomeApplicationMovement,
    journey: DrawerDragJourney?,
    touchInWindow: Offset,
    dropping: Boolean,
    editMode: Boolean,
    stylePanelExpanded: Boolean,
    favoriteState: FavoriteReadState,
    favoriteAvailability: Map<LaunchableIdentity, FavoriteAvailability>,
    onCommit: suspend (LaunchableIdentity, DrawerDragDrop) -> Boolean,
    onFinished: (saved: Boolean, cancelled: Boolean) -> Unit,
) {
    LaunchedEffect(journey) {
        val activeJourney = journey ?: return@LaunchedEffect
        val started = movement.startExternalJourney(
            identity = activeJourney.entry.identity,
            pointer = touchInWindow,
        )
        if (!started) onFinished(false, true)
    }
    LaunchedEffect(touchInWindow) {
        if (journey != null) movement.move(touchInWindow)
    }
    LaunchedEffect(dropping) {
        if (!dropping) return@LaunchedEffect
        val activeJourney = journey ?: return@LaunchedEffect
        val drop = movement.finishExternalJourney()
        // An invalid release (no drop target) resolves as a cancellation, not as a
        // save failure; only a resolved drop that fails to persist reports failure.
        val saved = drop != null && onCommit(activeJourney.entry.identity, drop)
        onFinished(saved, drop == null)
    }
    BackHandler(enabled = editMode && movement.isDragging, onBack = {
        if (movement.session?.module?.id == EXTERNAL_JOURNEY_MODULE_ID) {
            // Back during the Drawer journey is a cancellation: end the whole journey
            // without a mutation and without the save-failure feedback path.
            movement.cancel()
            onFinished(false, true)
        } else {
            movement.cancel()
        }
    })
    LaunchedEffect(editMode, stylePanelExpanded, favoriteState, favoriteAvailability) {
        val session = movement.session ?: return@LaunchedEffect
        val module = (favoriteState as? FavoriteReadState.Readable)?.orderedModules
            ?.firstOrNull(predicate = { it.id == session.module.id })
        val availability = favoriteAvailability[session.identity]
        val invalidSource = movement.isDragging &&
            session.module.id != EXTERNAL_JOURNEY_MODULE_ID &&
            (module != session.module ||
                (availability !is FavoriteAvailability.Available &&
                    availability !is FavoriteAvailability.Disabled))
        if (!editMode || stylePanelExpanded || invalidSource ||
            availability == FavoriteAvailability.ConfirmedRemoved
        ) {
            movement.cancel()
        }
    }
}
