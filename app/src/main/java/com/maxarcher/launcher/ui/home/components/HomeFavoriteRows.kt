package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.drawable.toBitmap
import com.maxarcher.launcher.FavoriteAggregate
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteContainer
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.OrderedFavoriteModuleType
import com.maxarcher.launcher.R
import com.maxarcher.launcher.belowItemHeightResource
import com.maxarcher.launcher.iconSizeResource
import com.maxarcher.launcher.homeFavoriteHiddenItemHeight
import com.maxarcher.launcher.lineHeightResource
import com.maxarcher.launcher.rowHeightResource
import com.maxarcher.launcher.textSizeResource
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow

@Composable
internal fun HomeFavoriteBelowItem(
    modifier: Modifier,
    availability: FavoriteAvailability,
    iconSize: FavoriteListSize,
    textSize: FavoriteListSize,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    interactionEnabled: Boolean = true,
) {
    val entry = availability.presentationEntry
    val iconDimension = dimensionResource(iconSize.iconSizeResource())
    val iconPixels = with(LocalDensity.current) { iconDimension.roundToPx() }
    val disabledAlpha = integerResource(R.integer.disabled_content_alpha_percent) / 100f
    val interactionSource = remember(entry?.identity) { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val displayText = when (availability) {
        is FavoriteAvailability.Available -> availability.entry.label
        is FavoriteAvailability.Disabled -> entry?.let {
            stringResource(R.string.favorite_disabled_format, it.label)
        } ?: stringResource(R.string.favorite_application_disabled)

        is FavoriteAvailability.TemporarilyUnavailable,
        is FavoriteAvailability.Unknown,
            -> entry?.let {
            stringResource(R.string.favorite_unavailable_format, it.label)
        } ?: stringResource(R.string.favorite_application_unavailable)

        FavoriteAvailability.ConfirmedRemoved ->
            stringResource(R.string.favorite_application_unavailable)
    }
    Column(
        modifier = modifier
            .height(dimensionResource(iconSize.belowItemHeightResource()))
            .then(
                other = if (interactionEnabled) Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colorResource(R.color.home_favorite_ripple)),
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = {
                        if (entry != null) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    },
                ) else Modifier
            )
            .padding(
                start = dimensionResource(R.dimen.home_favorite_below_horizontal_inset),
                top = dimensionResource(R.dimen.home_favorite_below_top_inset),
                end = dimensionResource(R.dimen.home_favorite_below_horizontal_inset),
                bottom = dimensionResource(R.dimen.home_favorite_below_bottom_inset),
            )
            .alpha(if (availability is FavoriteAvailability.Available) 1f else disabledAlpha)
            .testTag("home_favorite_below_item"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (entry == null) {
            Icon(
                painter = painterResource(R.drawable.ic_inventory_error),
                contentDescription = null,
                modifier = Modifier.size(iconDimension),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            val bitmap = entry.iconBitmap?.asImageBitmap()
                ?: remember(entry.icon, iconPixels) {
                    entry.icon.toBitmap(iconPixels, iconPixels).asImageBitmap()
                }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(iconDimension),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.home_favorite_below_icon_label_gap)))
        Text(
            text = displayText,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = dimensionResource(textSize.textSizeResource()).value.sp,
            lineHeight = dimensionResource(textSize.lineHeightResource()).value.sp,
            style = LocalTextStyle.current.copy(shadow = drawerForegroundShadow()),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)

internal fun HomeFavoriteRow(
    modifier: Modifier,
    availability: FavoriteAvailability,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveFavorite: () -> Unit = {},
    editMode: Boolean,
    compact: Boolean,
    listSize: FavoriteListSize? = null,
    iconSize: FavoriteListSize? = listSize,
    textSize: FavoriteListSize? = listSize,
    exchangeHighlight: Boolean,
    onRowBoundsInWindow: (Offset, IntSize) -> Unit,
    onHandleBoundsInWindow: (Rect) -> Unit,
    interactionEnabled: Boolean = true,
    showName: Boolean = true,
    centerIcon: Boolean = false,
    hiddenNamePlacement: Boolean = false,
) {
    val entry = availability.presentationEntry
    val iconDimension = dimensionResource(
        iconSize?.iconSizeResource()
            ?: if (compact) R.dimen.home_companion_favorite_icon_size
            else R.dimen.home_favorite_icon_size,
    )
    val disabledAlpha = integerResource(R.integer.disabled_content_alpha_percent) / 100f
    val iconPixels = with(LocalDensity.current) { iconDimension.roundToPx() }
    val interactionSource = remember(entry?.identity) { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val handleTargetSize = dimensionResource(R.dimen.home_reorder_handle_target_size)
    val removeTargetSize = dimensionResource(
        R.dimen.home_favorite_bar_remove_target_size,
    )
    val removeIconSize = dimensionResource(R.dimen.home_favorite_bar_remove_icon_size)
    val iconStartMargin = dimensionResource(R.dimen.home_favorite_list_icon_start_margin)
    val ribbonShape = RoundedCornerShape(
        dimensionResource(R.dimen.home_favorite_bar_corner_radius),
    )
    val removeInteractionSource = remember(entry?.identity) { MutableInteractionSource() }
    val displayText = when (availability) {
        is FavoriteAvailability.Available -> availability.entry.label
        is FavoriteAvailability.Disabled -> entry?.let {
            stringResource(R.string.favorite_disabled_format, it.label)
        } ?: stringResource(R.string.favorite_application_disabled)

        is FavoriteAvailability.TemporarilyUnavailable,
        is FavoriteAvailability.Unknown,
            -> entry?.let {
            stringResource(R.string.favorite_unavailable_format, it.label)
        } ?: stringResource(R.string.favorite_application_unavailable)

        FavoriteAvailability.ConfirmedRemoved ->
            stringResource(R.string.favorite_application_unavailable)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (iconSize == null) {
                    Modifier.heightIn(
                        min = if (compact) {
                            dimensionResource(R.dimen.home_companion_favorite_row_min_height)
                        } else {
                            dimensionResource(R.dimen.home_favorite_row_min_height)
                        },
                    )
                } else {
                    Modifier.height(
                        if (hiddenNamePlacement) {
                            homeFavoriteHiddenItemHeight(checkNotNull(iconSize))
                        } else {
                            dimensionResource(checkNotNull(iconSize).rowHeightResource())
                        },
                    )
                },
            )
            .then(
                if (compact) {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.onBackground.copy(
                                alpha = integerResource(
                                    R.integer.home_favorite_bar_item_background_alpha_percent,
                                ) / 100f,
                            ),
                            ribbonShape,
                        )
                        .border(
                            width = dimensionResource(R.dimen.home_favorite_bar_border_width),
                            color = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = integerResource(
                                    R.integer.home_favorite_bar_border_alpha_percent,
                                ) / 100f,
                            ),
                            shape = ribbonShape,
                        )
                } else {
                    Modifier
                },
            )
            .onGloballyPositioned {
                onRowBoundsInWindow(it.positionInWindow(), it.size)
            }
            .then(
                // A cross-group exchange marks its target favorite with a border; a group gap keeps
                // no border because in-group gaps never accept a placement.
                if (!exchangeHighlight) {
                    Modifier
                } else {
                    Modifier.border(
                        width = dimensionResource(R.dimen.home_favorite_exchange_border_width),
                        color = colorResource(R.color.home_favorite_exchange_border),
                        shape = RoundedCornerShape(
                            dimensionResource(R.dimen.home_favorite_exchange_border_radius),
                        ),
                    )
                },
            )
            .then(
                other = if (interactionEnabled) Modifier.combinedClickable(
                    enabled = !editMode,
                    interactionSource = interactionSource,
                    indication = if (editMode) {
                        null
                    } else {
                        ripple(color = colorResource(R.color.home_favorite_ripple))
                    },
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = {
                        if (entry != null) {
                            hapticFeedback.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                            )
                            onLongClick()
                        }
                    },
                ) else Modifier
            )
            .semantics {
                if (!showName) contentDescription = displayText
            }
            .alpha(if (availability is FavoriteAvailability.Available) 1f else disabledAlpha)
            .testTag("home_favorite_row"),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    start = if (centerIcon) 0.dp else iconStartMargin,
                    end = when {
                        editMode -> handleTargetSize
                        compact -> dimensionResource(R.dimen.home_favorite_bar_item_inset)
                        else -> 0.dp
                    },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (centerIcon) Arrangement.Center else Arrangement.Start,
        ) {
            Box(
                modifier = Modifier.size(iconDimension),
                contentAlignment = Alignment.Center,
            ) {
                if (entry == null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_inventory_error),
                        contentDescription = null,
                        modifier = Modifier.size(iconDimension),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    val bitmap = entry.iconBitmap?.asImageBitmap()
                        ?: remember(entry.icon, iconPixels) {
                            entry.icon.toBitmap(iconPixels, iconPixels).asImageBitmap()
                        }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(iconDimension),
                    )
                }
            }
            if (showName) {
                Spacer(
                    Modifier.width(
                        if (compact) {
                            dimensionResource(R.dimen.home_favorite_bar_icon_label_gap)
                        } else {
                            dimensionResource(R.dimen.home_favorite_icon_label_gap)
                        },
                    ),
                )
            }
            if (showName) Text(
                text = displayText,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = dimensionResource(
                    textSize?.textSizeResource()
                        ?: if (compact) R.dimen.home_companion_favorite_text_size
                        else R.dimen.home_favorite_text_size,
                ).value.sp,
                lineHeight = dimensionResource(
                    textSize?.lineHeightResource()
                        ?: if (compact) R.dimen.home_companion_favorite_line_height
                        else R.dimen.home_favorite_line_height,
                ).value.sp,
                style = LocalTextStyle.current.copy(shadow = drawerForegroundShadow()),
            )
        }
        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(removeTargetSize)
                    .clickable(
                        interactionSource = removeInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onRemoveFavorite,
                    )
                    .testTag("remove_favorite_item"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(removeTargetSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.remove_favorite_item),
                        modifier = Modifier.size(removeIconSize),
                        tint = colorResource(R.color.home_favorite_remove_icon),
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.favorite_reorder_handle),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(handleTargetSize)
                    .fillMaxHeight()
                    .onGloballyPositioned {
                        onHandleBoundsInWindow(
                            Rect(offset = it.positionInWindow(), size = it.size.toSize()),
                        )
                    }
                    .padding(
                        (
                                handleTargetSize -
                                        dimensionResource(R.dimen.home_reorder_handle_size)
                                ) / 2,
                    )
                    .testTag("favorite_reorder_handle"),
            )
        }
    }
}

internal fun List<OrderedFavoriteModule>.withPresentationFrom(
    aggregate: FavoriteAggregate,
): List<OrderedFavoriteModule> {
    val verticalById = aggregate.verticalLists.associateBy(FavoriteContainer::id)
    return map { module ->
        val container = verticalById[module.id]
        if (module.type != OrderedFavoriteModuleType.Vertical || container == null) {
            module
        } else if (
            module.iconSize == container.iconSize &&
            module.textSize == container.textSize &&
            module.namePlacement == container.namePlacement &&
            module.itemsPerRow == container.itemsPerRow
        ) {
            module
        } else {
            module.copy(
                iconSize = container.iconSize,
                textSize = container.textSize,
                namePlacement = container.namePlacement,
                itemsPerRow = container.itemsPerRow,
            )
        }
    }
}

internal fun LaunchableIdentity.stableKey(): String =
    "$profileSerialNumber:${componentName.flattenToString()}"
