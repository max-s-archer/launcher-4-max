package com.maxarcher.launcher.ui.home.components

import android.R.attr.animationDuration
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.FavoriteNamePlacement
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.R
import com.maxarcher.launcher.applicationLayoutIdentities

@Composable
internal fun HomeOrderedFavoriteRibbon(
    module: OrderedFavoriteModule,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    listState: LazyListState,
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
    val animationDuration = integerResource(id = R.integer.short_property_animation_duration_ms)
    val layoutIdentities = applicationLayoutIdentities(
        module = module, movingIdentity = applicationMovement?.activeIdentity,
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = dimensionResource(id = R.dimen.home_favorite_bar_height))
            .homeEditSurface(enabled = editMode),
        state = listState,
        userScrollEnabled = applicationMovement?.isDragging != true,
        horizontalArrangement = Arrangement.spacedBy(
            space = dimensionResource(id = R.dimen.home_favorite_bar_item_spacing),
        ),
        content = {
            itemsIndexed(
                items = layoutIdentities,
                key = { _, identity -> identity.stableKey() },
                itemContent = { _, identity ->
                    val availability = availabilityByIdentity[identity]
                        ?: FavoriteAvailability.Unknown(presentationEntry = null)
                    HomeFavoriteItemFrame(
                        identity = identity,
                        movement = applicationMovement,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween(durationMillis = animationDuration),
                            )
                            .homeFavoriteEnter(
                                batch = enterBatch,
                                key = HomeFavoriteEnterKey(moduleId = module.id, identity = identity),
                            )
                            .widthIn(
                                max = dimensionResource(id = R.dimen.home_favorite_bar_item_width),
                            ),
                        showRemove = applicationEditing,
                        removeEnabled = applicationMutationEnabled,
                        namePlacement = FavoriteNamePlacement.Right,
                        iconSize = dimensionResource(id = R.dimen.home_favorite_icon_size),
                        onRemove = { onRemoveFavorite(identity) },
                        content = {
                            FavoriteRibbonItemIntrinsicWidthContent(
                                displayText = favoriteRibbonDisplayText(
                                    availability = availability,
                                ),
                                iconSize = dimensionResource(id = R.dimen.home_favorite_icon_size),
                            )
                            HomeFavoriteRow(
                                modifier = Modifier.matchParentSize(),
                                availability = availability,
                                onClick = { onLaunchFavorite(availability) },
                                onLongClick = {
                                    availability.presentationEntry?.let(block = onLongPressFavorite)
                                },
                                editMode = false,
                                interactionEnabled = !editMode,
                                compact = true,
                                listSize = FavoriteListSize.Medium,
                                exchangeHighlight = false,
                                onRowBoundsInWindow = { _, _ -> },
                                onHandleBoundsInWindow = {},
                            )
                        },
                    )
                },
            )
            if (showAddEntry) {
                item(
                    key = "add:${module.id}",
                    content = {
                        DisposableEffect(
                            key1 = applicationMovement,
                            key2 = module.id,
                            effect = {
                                onDispose {
                                    applicationMovement?.updateAdd(id = module.id, bounds = null)
                                }
                            },
                        )
                        HomeModuleAddFavoriteEntry(
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween(durationMillis = animationDuration),
                            ).onGloballyPositioned(
                                onGloballyPositioned = { coordinates ->
                                    applicationMovement?.updateAdd(
                                        id = module.id,
                                        bounds = Rect(
                                            offset = coordinates.positionInWindow(),
                                            size = coordinates.size.toSize(),
                                        ),
                                    )
                                },
                            ).width(
                                width = dimensionResource(
                                    id = R.dimen.home_favorite_bar_item_width,
                                ),
                            ),
                            module = module,
                            enabled = addEntryEnabled,
                            onClick = onAddToModule,
                        )
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeFavoriteFlowRibbon(
    module: OrderedFavoriteModule,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
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
    val layoutIdentities = applicationLayoutIdentities(
        module = module, movingIdentity = applicationMovement?.activeIdentity,
    )
    // Flow wraps entries into additional rows and grows with the main list; it owns no
    // horizontal or vertical scroll position of its own. The lookahead scope keeps item
    // identities and placement transitions across row boundaries like the vertical grid.
    LookaheadScope(
        content = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = animationDuration))
                    .homeEditSurface(enabled = editMode),
                horizontalArrangement = Arrangement.spacedBy(
                    space = dimensionResource(id = R.dimen.home_favorite_bar_item_spacing),
                ),
                verticalArrangement = Arrangement.spacedBy(
                    space = dimensionResource(id = R.dimen.home_favorite_bar_row_spacing),
                ),
                content = {
                    layoutIdentities.forEach(action = { identity ->
                        key(identity) {
                            val availability = availabilityByIdentity[identity]
                                ?: FavoriteAvailability.Unknown(presentationEntry = null)
                            Box(
                                modifier = Modifier.animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = animationDuration)
                                    },
                                    animateMotionFrameOfReference = false,
                                ),
                            ) {
                                HomeFavoriteItemFrame(
                                    identity = identity,
                                    movement = applicationMovement,
                                    modifier = Modifier
                                        .homeFavoriteEnter(
                                            batch = enterBatch,
                                            key = HomeFavoriteEnterKey(
                                                moduleId = module.id,
                                                identity = identity,
                                            ),
                                        )
                                        .widthIn(
                                            max = dimensionResource(
                                                id = R.dimen.home_favorite_bar_item_width,
                                            ),
                                        ),
                                    showRemove = applicationEditing,
                                    removeEnabled = applicationMutationEnabled,
                                    namePlacement = FavoriteNamePlacement.Right,
                                    iconSize = dimensionResource(
                                        id = R.dimen.home_favorite_icon_size,
                                    ),
                                    onRemove = { onRemoveFavorite(identity) },
                                    content = {
                                        FavoriteRibbonItemIntrinsicWidthContent(
                                            displayText = favoriteRibbonDisplayText(
                                                availability = availability,
                                            ),
                                            iconSize = dimensionResource(
                                                id = R.dimen.home_favorite_icon_size,
                                            ),
                                            verticalInset = dimensionResource(
                                                id = R.dimen.home_favorite_bar_row_spacing,
                                            ) / 2,
                                        )
                                        HomeFavoriteRow(
                                            modifier = Modifier.matchParentSize(),
                                            availability = availability,
                                            onClick = { onLaunchFavorite(availability) },
                                            onLongClick = {
                                                availability.presentationEntry?.let(
                                                    block = onLongPressFavorite,
                                                )
                                            },
                                            editMode = false,
                                            interactionEnabled = !editMode,
                                            compact = true,
                                            listSize = FavoriteListSize.Medium,
                                            exchangeHighlight = false,
                                            onRowBoundsInWindow = { _, _ -> },
                                            onHandleBoundsInWindow = {},
                                            verticalInset = dimensionResource(
                                                id = R.dimen.home_favorite_bar_row_spacing,
                                            ) / 2,
                                        )
                                    },
                                )
                            }
                        }
                    })
                    if (showAddEntry) {
                        key("add:${module.id}") {
                            DisposableEffect(
                                key1 = applicationMovement,
                                key2 = module.id,
                                effect = {
                                    onDispose {
                                        applicationMovement?.updateAdd(
                                            id = module.id,
                                            bounds = null,
                                        )
                                    }
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .animateBounds(
                                        lookaheadScope = this@LookaheadScope,
                                        boundsTransform = { _, _ ->
                                            tween(durationMillis = animationDuration)
                                        },
                                        animateMotionFrameOfReference = false,
                                    )
                                    .homeFavoriteEnter(
                                        batch = enterBatch,
                                        key = HomeFavoriteEnterKey(moduleId = module.id),
                                    )
                                    .width(
                                        width = dimensionResource(
                                            id = R.dimen.home_favorite_bar_item_width,
                                        ),
                                    ),
                            ) {
                                HomeModuleAddFavoriteEntry(
                                    modifier = Modifier.onGloballyPositioned(
                                        onGloballyPositioned = { coordinates ->
                                            applicationMovement?.updateAdd(
                                                id = module.id,
                                                bounds = Rect(
                                                    offset = coordinates.positionInWindow(),
                                                    size = coordinates.size.toSize(),
                                                ),
                                            )
                                        },
                                    ),
                                    module = module,
                                    enabled = addEntryEnabled,
                                    onClick = onAddToModule,
                                )
                            }
                        }
                    }
                },
            )
        },
    )
}

@Composable
private fun favoriteRibbonDisplayText(
    availability: FavoriteAvailability,
): String {
    val entry = availability.presentationEntry
    return when (availability) {
        is FavoriteAvailability.Available -> availability.entry.label
        is FavoriteAvailability.Disabled -> entry?.let(
            block = {
                stringResource(
                    id = R.string.favorite_disabled_format,
                    formatArgs = arrayOf(it.label),
                )
            },
        ) ?: stringResource(id = R.string.favorite_application_disabled)

        is FavoriteAvailability.TemporarilyUnavailable,
        is FavoriteAvailability.Unknown,
            -> entry?.let(
            block = {
                stringResource(
                    id = R.string.favorite_unavailable_format,
                    formatArgs = arrayOf(it.label),
                )
            },
        ) ?: stringResource(id = R.string.favorite_application_unavailable)

        FavoriteAvailability.ConfirmedRemoved ->
            stringResource(id = R.string.favorite_application_unavailable)
    }
}

@Composable
private fun FavoriteRibbonItemIntrinsicWidthContent(
    displayText: String,
    iconSize: Dp,
    verticalInset: Dp = 0.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                horizontal = dimensionResource(id = R.dimen.home_favorite_bar_item_inset),
                vertical = verticalInset,
            )
            .alpha(alpha = 0f)
            .clearAndSetSemantics(properties = {}),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Spacer(modifier = Modifier.size(size = iconSize))
            Spacer(
                modifier = Modifier.width(
                    width = dimensionResource(id = R.dimen.home_favorite_bar_icon_label_gap),
                ),
            )
            Text(
                text = displayText,
                modifier = Modifier.widthIn(
                    max = dimensionResource(id = R.dimen.home_favorite_bar_label_max_width),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = dimensionResource(id = R.dimen.home_favorite_text_size).value.sp,
                lineHeight = dimensionResource(id = R.dimen.home_favorite_line_height).value.sp,
            )
        },
    )
}
