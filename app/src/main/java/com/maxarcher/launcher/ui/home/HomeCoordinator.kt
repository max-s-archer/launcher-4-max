package com.maxarcher.launcher.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import com.maxarcher.launcher.ApplicationOrderChange
import com.maxarcher.launcher.FavoriteReadState
import com.maxarcher.launcher.FavoriteStore
import com.maxarcher.launcher.HOME_BOTTOM_APP_BAR_DESTINATION_ID
import com.maxarcher.launcher.HomeFavoriteEditor
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteAggregate
import com.maxarcher.launcher.R
import com.maxarcher.launcher.ui.drawer.DrawerDragDrop
import com.maxarcher.launcher.ui.drawer.DrawerDragJourney
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps Home editing, Drawer-to-Home handoff, and durable favorite mutations together while
 * leaving surface transitions and UI composition in [Launcher4MaxApp]. It writes the current
 * reliable favorite state into [favoriteState] on every recomposition.
 */
@Stable
internal class HomeCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val favoriteStore: FavoriteStore,
    private val favoriteEditor: HomeFavoriteEditor,
) {
    var favoriteState by mutableStateOf<FavoriteReadState>(FavoriteReadState.Loading)
        private set
    var editMode by mutableStateOf(false)
        private set
    /** The module whose inline style settings panel is open, or null when none is open. */
    var openStyleModuleId by mutableStateOf<String?>(null)
        private set
    var editMembership by mutableStateOf<Set<LaunchableIdentity>>(emptySet())
        private set
    var drawerDragJourney by mutableStateOf<DrawerDragJourney?>(null)
        private set
    var drawerDragTouchPosition by mutableStateOf(Offset.Zero)
        private set
    var drawerDragDropping by mutableStateOf(false)
        private set

    fun refreshEditMembership() {
        if (editMode) {
            editMembership = (favoriteStore.state.value as? FavoriteReadState.Readable)
                ?.identities
                ?.toSet()
                .orEmpty()
        }
    }

    fun updateFavoriteState(state: FavoriteReadState) {
        favoriteState = state
    }

    fun updateEditMembership(aggregate: OrderedFavoriteAggregate) {
        editMembership = aggregate.identities.toSet()
    }

    fun requestEditMode() {
        editMembership = (favoriteState as? FavoriteReadState.Readable)
            ?.identities
            ?.toSet()
            .orEmpty()
        editMode = true
    }

    fun dismissEditMode() {
        editMode = false
        openStyleModuleId = null
    }

    /**
     * Opens [moduleId]'s style settings panel, or closes it when it is already open. Opening
     * another module's panel closes the current one in the same state change.
     */
    fun toggleModuleStylePanel(moduleId: String) {
        openStyleModuleId = if (openStyleModuleId == moduleId) null else moduleId
    }

    fun clearEditSelection() {
        openStyleModuleId = null
    }

    fun clearDrawerDrag() {
        drawerDragJourney = null
        drawerDragDropping = false
    }

    fun startDrawerDrag(journey: DrawerDragJourney): Boolean {
        if (favoriteEditor.isSaving) return false
        favoriteEditor.invalidateUndo()
        drawerDragJourney = journey
        drawerDragTouchPosition = journey.touchStartInWindow
        editMode = true
        clearEditSelection()
        return true
    }

    fun moveDrawerDrag(position: Offset) {
        drawerDragTouchPosition = position
    }

    fun endDrawerDrag(cancelled: Boolean) {
        if (cancelled) {
            editMode = false
            openStyleModuleId = null
        } else {
            drawerDragDropping = true
        }
    }

    suspend fun commitDrawerDrop(
        identity: LaunchableIdentity,
        drop: DrawerDragDrop,
        barCapacity: Int? = null,
    ): Boolean {
        return when (drop) {
            is DrawerDragDrop.Insertion -> favoriteEditor.insertExternalFavorite(
                identity = identity,
                destinationModuleId = drop.moduleId,
                boundary = drop.boundary,
                newModuleType = null,
            )

            is DrawerDragDrop.Creation -> favoriteEditor.insertExternalFavorite(
                identity = identity,
                destinationModuleId = null,
                boundary = 0,
                newModuleType = drop.moduleType,
            )

            is DrawerDragDrop.HomeBottomAppBar -> favoriteEditor.insertExternalFavorite(
                identity = identity,
                destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
                boundary = drop.boundary,
                newModuleType = null,
                barCapacity = barCapacity,
            )
        }
    }

    fun finishDrawerDrag(saved: Boolean, cancelled: Boolean) {
        drawerDragDropping = false
        drawerDragJourney = null
        if (!saved && !cancelled) {
            Toast.makeText(
                context,
                R.string.drawer_drag_unable_to_save_favorite,
                Toast.LENGTH_SHORT,
            ).show()
        }
        editMode = false
        openStyleModuleId = null
    }

    fun commitApplicationOrder(change: ApplicationOrderChange, complete: () -> Unit) {
        scope.launch {
            try {
                if (!favoriteEditor.reorderApplication(change = change)) {
                    Toast.makeText(
                        context,
                        R.string.unable_to_move_favorite,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                // The Flow collector may lag behind the completed write. Hand off only after
                // Home receives the current reliable state, never an old source projection.
                snapshotFlow { favoriteState }.first { presented ->
                    presented == favoriteStore.state.value
                }
            } finally {
                complete()
            }
        }
    }

    suspend fun commitFavoriteComposition(
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ): OrderedFavoriteAggregate? {
        val aggregate = favoriteEditor.updateComposition(transform = transform) ?: return null
        if (editMode) editMembership = aggregate.identities.toSet()
        return aggregate
    }
}
