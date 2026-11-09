package com.maxarcher.launcher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.ui.components.ApplicationItem
import com.maxarcher.launcher.ui.components.ApplicationItemBadge
import com.maxarcher.launcher.ui.components.ApplicationItemNamePlacement
import com.maxarcher.launcher.ui.components.ApplicationItemPadding
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarcher.launcher.ui.home.components.HomeFavoriteFlowRibbon
import com.maxarcher.launcher.ui.home.components.HomeFavoriteGrid
import com.maxarcher.launcher.ui.home.components.HomeFavoriteItemFrame
import com.maxarcher.launcher.ui.home.components.HomeModuleAddFavoriteEntry
import com.maxarcher.launcher.ui.home.components.HomeOrderedFavoriteRibbon
import com.maxarcher.launcher.ui.home.components.favoriteAvailabilityDisplayText
import com.maxarcher.launcher.ui.home.components.homeEditSurface
import com.maxarcher.launcher.ui.home.components.homeFavoriteEnter

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
                                    val bounds = Rect(
                                        offset = coordinates.positionInWindow(),
                                        size = coordinates.size.toSize(),
                                    )
                                    applicationMovement?.updateAdd(
                                        id = module.id,
                                        bounds = bounds,
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
                                key = HomeFavoriteEnterKey(
                                    moduleId = module.id,
                                    identity = identity,
                                ),
                            ),
                            identity = identity,
                            movement = applicationMovement,
                            showRemove = applicationEditing,
                            namePlacement = module.namePlacement,
                            iconSize = dimensionResource(
                                id = module.iconSize.iconSizeResource(),
                            ),
                            contentStart = if (
                                module.namePlacement == FavoriteNamePlacement.Right
                            ) {
                                dimensionResource(R.dimen.home_favorite_list_icon_start_margin)
                            } else {
                                0.dp
                            },
                            contentTop = if (
                                module.namePlacement == FavoriteNamePlacement.Right
                            ) {
                                0.dp
                            } else {
                                dimensionResource(R.dimen.home_favorite_below_top_inset)
                            },
                            contentBottom = if (
                                module.namePlacement == FavoriteNamePlacement.Hidden
                            ) {
                                dimensionResource(R.dimen.home_favorite_below_bottom_inset)
                            } else if (
                                module.namePlacement == FavoriteNamePlacement.Right
                            ) {
                                0.dp
                            } else {
                                dimensionResource(R.dimen.home_favorite_below_top_inset)
                            },
                            content = {
                                val entry = availability.presentationEntry
                                val displayText =
                                    favoriteAvailabilityDisplayText(availability)
                                val placement = when (module.namePlacement) {
                                    FavoriteNamePlacement.Right ->
                                        ApplicationItemNamePlacement.Right
                                    FavoriteNamePlacement.Below ->
                                        ApplicationItemNamePlacement.Below
                                    FavoriteNamePlacement.Hidden ->
                                        ApplicationItemNamePlacement.Hidden
                                }
                                val nameHidden =
                                    placement == ApplicationItemNamePlacement.Hidden
                                ApplicationItem(
                                    icon = entry?.icon,
                                    iconBitmap = entry?.iconBitmap,
                                    iconSize = dimensionResource(
                                        module.iconSize.iconSizeResource(),
                                    ),
                                    name = AnnotatedString(displayText),
                                    namePlacement = placement,
                                    nameStyle = LocalTextStyle.current.copy(
                                        fontSize = dimensionResource(
                                            module.textSize.textSizeResource(),
                                        ).value.sp,
                                        lineHeight = dimensionResource(
                                            module.textSize.lineHeightResource(),
                                        ).value.sp,
                                        shadow = drawerForegroundShadow(),
                                    ),
                                    iconLabelGap = dimensionResource(
                                        if (placement == ApplicationItemNamePlacement.Below) {
                                            R.dimen.home_favorite_below_icon_label_gap
                                        } else {
                                            R.dimen.home_favorite_icon_label_gap
                                        },
                                    ),
                                    contentPadding = if (nameHidden) {
                                        ApplicationItemPadding(
                                            top = dimensionResource(
                                                R.dimen.home_favorite_below_top_inset,
                                            ),
                                            bottom = dimensionResource(
                                                R.dimen.home_favorite_below_bottom_inset,
                                            ),
                                        )
                                    } else if (
                                        placement == ApplicationItemNamePlacement.Below
                                    ) {
                                        ApplicationItemPadding(
                                            start = dimensionResource(
                                                R.dimen.home_favorite_below_horizontal_inset,
                                            ),
                                            top = dimensionResource(
                                                R.dimen.home_favorite_below_top_inset,
                                            ),
                                            end = dimensionResource(
                                                R.dimen.home_favorite_below_horizontal_inset,
                                            ),
                                            bottom = dimensionResource(
                                                R.dimen.home_favorite_below_bottom_inset,
                                            ),
                                        )
                                    } else {
                                        ApplicationItemPadding(
                                            start = dimensionResource(
                                                R.dimen.home_favorite_list_icon_start_margin,
                                            ),
                                        )
                                    },
                                    hiddenNameDescription = displayText
                                        .takeIf { nameHidden },
                                    onClick = { onLaunchFavorite(availability) },
                                    onLongClick = entry?.let {
                                        { onLongPressFavorite(it) }
                                    },
                                    interactionEnabled = !editMode,
                                    contentAlpha = if (
                                        availability is FavoriteAvailability.Available
                                    ) {
                                        1f
                                    } else {
                                        integerResource(
                                            R.integer.disabled_content_alpha_percent,
                                        ) / 100f
                                    },
                                    itemTag = if (nameHidden) {
                                        null
                                    } else if (
                                        placement == ApplicationItemNamePlacement.Below
                                    ) {
                                        "home_favorite_below_item"
                                    } else {
                                        "home_favorite_row"
                                    },
                                    fillNameWidth = true,
                                    badge = if (applicationEditing) {
                                        ApplicationItemBadge.Remove(
                                            onClick = { onRemoveFavorite(identity) },
                                            enabled = applicationMutationEnabled,
                                            tag = "remove_favorite_item",
                                        )
                                    } else {
                                        ApplicationItemBadge.None
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(
                                            when (module.namePlacement) {
                                                FavoriteNamePlacement.Below ->
                                                    dimensionResource(
                                                        module.iconSize
                                                            .belowItemHeightResource(),
                                                    )
                                                FavoriteNamePlacement.Hidden ->
                                                    homeFavoriteHiddenItemHeight(
                                                        module.iconSize,
                                                    )
                                                FavoriteNamePlacement.Right ->
                                                    dimensionResource(
                                                        module.iconSize.rowHeightResource(),
                                                    )
                                            },
                                        ),
                                )
                            },
                        )
                    }
                },
            )
        }

        OrderedFavoriteModuleType.Ribbon -> when {
            module.usesRibbonLayout() -> {
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

            else -> {
                HomeFavoriteFlowRibbon(
                    enterBatch = enterBatch,
                    module = module,
                    availabilityByIdentity = availabilityByIdentity,
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
}
