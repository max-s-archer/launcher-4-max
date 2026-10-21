package com.maxarcher.launcher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.ui.home.components.HomeFavoriteGrid
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarcher.launcher.ui.home.components.homeFavoriteEnter
import com.maxarcher.launcher.ui.home.components.HomeFavoriteItemFrame
import com.maxarcher.launcher.ui.home.components.HomeModuleAddFavoriteEntry
import com.maxarcher.launcher.ui.home.components.HomeOrderedFavoriteRibbon
import com.maxarcher.launcher.ui.home.components.homeEditSurface
import com.maxarcher.launcher.ui.home.components.HomeFavoriteBelowItem
import com.maxarcher.launcher.ui.home.components.HomeFavoriteRow
import com.maxarcher.launcher.ui.home.components.stableKey

@Composable
internal fun HomeOrderedModuleContent(
    module: OrderedFavoriteModule,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    ribbonListState: LazyListState?,
    editMode: Boolean,
    showAddEntry: Boolean,
    addEntryEnabled: Boolean,
    onAddToModule: () -> Unit,
    onLaunchFavorite: (FavoriteAvailability) -> Unit,
    onLongPressFavorite: (LaunchableEntry) -> Unit,
    applicationEditing: Boolean = false,
    applicationMutationEnabled: Boolean = true,
    onRemoveFavorite: (LaunchableIdentity) -> Unit = {},
    applicationMovement: HomeApplicationMovement? = null,
    enterBatch: HomeFavoriteEnterBatch? = null,
) {
    when (module.type) {
        OrderedFavoriteModuleType.Vertical -> {
            val layoutIdentities = applicationLayoutIdentities(
                module = module, movingIdentity = applicationMovement?.activeIdentity,
            )
            val cells: List<LaunchableIdentity?> =
                layoutIdentities + if (showAddEntry) listOf(element = null) else emptyList()
            HomeFavoriteGrid(
                items = cells,
                columns = module.itemsPerRow,
                itemKey = { identity -> identity?.stableKey() ?: "add:${module.id}" },
                modifier = Modifier.homeEditSurface(enabled = editMode),
                content = { identity ->
                    if (identity == null) {
                        HomeModuleAddFavoriteEntry(
                            modifier = Modifier.fillMaxWidth().onGloballyPositioned(
                                onGloballyPositioned = { coordinates ->
                                    applicationMovement?.updateAdd(
                                        id = module.id,
                                        bounds = Rect(offset = coordinates.positionInWindow(), size = coordinates.size.toSize()),
                                    )
                                },
                            ),
                            module = module,
                            enabled = addEntryEnabled,
                            onClick = onAddToModule,
                        )
                    } else {
                        val availability = availabilityByIdentity[identity]
                            ?: FavoriteAvailability.Unknown(presentationEntry = null)
                        HomeFavoriteItemFrame(
                            modifier = Modifier.fillMaxWidth().homeFavoriteEnter(
                                batch = enterBatch,
                                key = HomeFavoriteEnterKey(moduleId = module.id, identity = identity),
                            ),
                            identity = identity,
                            movement = applicationMovement,
                            showRemove = applicationEditing,
                            removeEnabled = applicationMutationEnabled,
                            namePlacement = module.namePlacement,
                            iconSize = dimensionResource(
                                id = module.iconSize.iconSizeResource(),
                            ),
                            onRemove = { onRemoveFavorite(identity) },
                            content = {
                                if (module.namePlacement == FavoriteNamePlacement.Below) {
                                    HomeFavoriteBelowItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        availability = availability,
                                        iconSize = module.iconSize,
                                        textSize = module.textSize,
                                        interactionEnabled = !editMode,
                                        onClick = { onLaunchFavorite(availability) },
                                        onLongClick = {
                                            availability.presentationEntry?.let(block = onLongPressFavorite)
                                        },
                                    )
                                } else if (module.namePlacement == FavoriteNamePlacement.Right) {
                                    HomeFavoriteRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        availability = availability,
                                        onClick = { onLaunchFavorite(availability) },
                                        onLongClick = {
                                            availability.presentationEntry?.let(block = onLongPressFavorite)
                                        },
                                        editMode = false,
                                        interactionEnabled = !editMode,
                                        compact = false,
                                        iconSize = module.iconSize,
                                        textSize = module.textSize,
                                        exchangeHighlight = false,
                                        onRowBoundsInWindow = { _, _ -> },
                                        onHandleBoundsInWindow = {},
                                    )
                                } else {
                                    HomeFavoriteRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        availability = availability,
                                        onClick = { onLaunchFavorite(availability) },
                                        onLongClick = {
                                            availability.presentationEntry?.let(block = onLongPressFavorite)
                                        },
                                        editMode = false,
                                        interactionEnabled = !editMode,
                                        compact = false,
                                        iconSize = module.iconSize,
                                        textSize = null,
                                        exchangeHighlight = false,
                                        onRowBoundsInWindow = { _, _ -> },
                                        onHandleBoundsInWindow = {},
                                        showName = false,
                                        centerIcon = true,
                                        hiddenNamePlacement = true,
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }

        OrderedFavoriteModuleType.Ribbon -> {
            HomeOrderedFavoriteRibbon(
                enterBatch = enterBatch,
                module = module,
                availabilityByIdentity = availabilityByIdentity,
                listState = requireNotNull(value = ribbonListState),
                editMode = editMode,
                showAddEntry = showAddEntry,
                addEntryEnabled = addEntryEnabled,
                onAddToModule = onAddToModule,
                onLaunchFavorite = onLaunchFavorite,
                onLongPressFavorite = onLongPressFavorite,
                applicationEditing = applicationEditing,
                applicationMutationEnabled = applicationMutationEnabled,
                onRemoveFavorite = onRemoveFavorite,
                applicationMovement = applicationMovement,
            )
        }
    }
}
