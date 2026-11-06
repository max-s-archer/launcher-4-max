package com.maxarcher.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.maxarcher.launcher.R

/** Where the prepared name sits relative to the icon. Width stays on [Modifier]. */
internal enum class ApplicationItemNamePlacement {
    Right,
    Below,
    Hidden,
}

/** At most one badge. The selection number is a sibling of the selection cell, not this. */
internal sealed interface ApplicationItemBadge {
    data object None : ApplicationItemBadge

    data class Remove(
        val onClick: () -> Unit,
        val enabled: Boolean = true,
        val interactionSource: MutableInteractionSource? = null,
        val tag: String? = null,
    ) : ApplicationItemBadge
}

internal data class ApplicationItemPadding(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

/**
 * One application icon, its prepared name, and at most one remove badge.
 * Callers own the item size, the name string, and any gesture outside this item.
 */
@Composable
internal fun ApplicationItem(
    icon: Drawable?,
    iconBitmap: Bitmap?,
    iconSize: Dp,
    name: AnnotatedString,
    namePlacement: ApplicationItemNamePlacement,
    nameStyle: TextStyle,
    iconLabelGap: Dp,
    contentPadding: ApplicationItemPadding,
    modifier: Modifier = Modifier,
    badge: ApplicationItemBadge = ApplicationItemBadge.None,
    iconContentDescription: String? = null,
    hiddenNameDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionEnabled: Boolean = true,
    contentAlpha: Float = 1f,
    itemTag: String? = null,
    nameMaxWidth: Dp = Dp.Unspecified,
    fillNameWidth: Boolean = false,
) {
    val clickable = interactionEnabled && (onClick != null || onLongClick != null)
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val nameHidden = namePlacement == ApplicationItemNamePlacement.Hidden
    Box(
        modifier = modifier.fillMaxSize()
            .then(
                if (clickable) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = colorResource(R.color.home_favorite_ripple)),
                        role = Role.Button,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick?.let { action ->
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                action()
                            }
                        },
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (nameHidden && hiddenNameDescription != null) {
                    Modifier.semantics { contentDescription = hiddenNameDescription }
                } else {
                    Modifier
                },
            )
            .then(itemTag?.let { Modifier.testTag(it) } ?: Modifier),
    ) {
        val contentModifier = Modifier
            .padding(
                start = contentPadding.start,
                top = contentPadding.top,
                end = contentPadding.end,
                bottom = contentPadding.bottom,
            )
            .alpha(contentAlpha)
        when (namePlacement) {
            ApplicationItemNamePlacement.Right -> Row(
                modifier = Modifier.matchParentSize().then(contentModifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ApplicationItemIcon(
                    icon = icon,
                    iconBitmap = iconBitmap,
                    iconSize = iconSize,
                    contentDescription = iconContentDescription,
                )
                Spacer(Modifier.width(iconLabelGap))
                ApplicationItemName(
                    name = name,
                    nameStyle = nameStyle,
                    textAlign = null,
                    modifier = if (fillNameWidth) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.widthIn(max = nameMaxWidth)
                    },
                )
            }

            ApplicationItemNamePlacement.Below -> Column(
                modifier = Modifier.matchParentSize().then(contentModifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ApplicationItemIcon(
                    icon = icon,
                    iconBitmap = iconBitmap,
                    iconSize = iconSize,
                    contentDescription = iconContentDescription,
                )
                Spacer(Modifier.height(iconLabelGap))
                ApplicationItemName(
                    name = name,
                    nameStyle = nameStyle,
                    textAlign = TextAlign.Center,
                    modifier = if (fillNameWidth) Modifier.fillMaxWidth() else Modifier,
                )
            }

            ApplicationItemNamePlacement.Hidden -> Box(
                modifier = Modifier.matchParentSize().then(contentModifier),
                contentAlignment = Alignment.Center,
            ) {
                ApplicationItemIcon(
                    icon = icon,
                    iconBitmap = iconBitmap,
                    iconSize = iconSize,
                    contentDescription = iconContentDescription,
                )
            }
        }
        if (badge is ApplicationItemBadge.Remove) {
            Box(modifier = Modifier.matchParentSize()) {
                ApplicationItemRemoveBadge(
                    badge = badge,
                    iconSize = iconSize,
                    namePlacement = namePlacement,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun ApplicationItemIcon(
    icon: Drawable?,
    iconBitmap: Bitmap?,
    iconSize: Dp,
    contentDescription: String?,
) {
    val iconPixels = with(LocalDensity.current) { iconSize.roundToPx() }
    val bitmap = remember(iconBitmap, icon, iconPixels) {
        iconBitmap?.asImageBitmap()
            ?: icon?.toBitmap(
                width = iconPixels.coerceAtLeast(1),
                height = iconPixels.coerceAtLeast(1),
            )?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_inventory_error),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ApplicationItemName(
    name: AnnotatedString,
    nameStyle: TextStyle,
    textAlign: TextAlign?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = nameStyle,
    )
}

/** Drawn on the icon, 4dp outside its top-start corner, never inside the name's layout. */
@Composable
private fun BoxScope.ApplicationItemRemoveBadge(
    badge: ApplicationItemBadge.Remove,
    iconSize: Dp,
    namePlacement: ApplicationItemNamePlacement,
    contentPadding: ApplicationItemPadding,
) {
    val targetSize = dimensionResource(R.dimen.home_favorite_bar_remove_target_size)
    val outset = dimensionResource(R.dimen.home_favorite_remove_icon_outset)
    val anchor = when (namePlacement) {
        ApplicationItemNamePlacement.Right -> Alignment.CenterStart
        ApplicationItemNamePlacement.Below -> Alignment.TopCenter
        ApplicationItemNamePlacement.Hidden -> Alignment.Center
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .padding(
                start = contentPadding.start,
                top = contentPadding.top,
                end = contentPadding.end,
                bottom = contentPadding.bottom,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(anchor)
                .size(iconSize),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -outset, y = -outset)
                    .size(targetSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(
                        interactionSource = badge.interactionSource,
                        indication = null,
                        enabled = badge.enabled,
                        role = Role.Button,
                        onClick = badge.onClick,
                    )
                    .then(badge.tag?.let { Modifier.testTag(it) } ?: Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.remove_favorite_item),
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.home_favorite_bar_remove_icon_size),
                    ),
                    tint = colorResource(R.color.home_favorite_remove_icon),
                )
            }
        }
    }
}

/** Selection order sits on the cell, not on the icon. The row already announces it. */
@Composable
internal fun ApplicationSelectionOrderBadge(
    order: Int,
    modifier: Modifier = Modifier,
) {
    val badgeShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.drawer_selection_badge_corner_radius),
        topEnd = 0.dp,
        bottomStart = 0.dp,
        bottomEnd = dimensionResource(R.dimen.drawer_selection_badge_corner_radius),
    )
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = dimensionResource(R.dimen.drawer_selection_badge_min_size),
                minHeight = dimensionResource(R.dimen.drawer_selection_badge_min_size),
            )
            .background(color = MaterialTheme.colorScheme.onBackground, shape = badgeShape)
            .padding(
                horizontal = dimensionResource(R.dimen.drawer_selection_badge_content_padding),
            )
            .clearAndSetSemantics { }
            .testTag("drawer_favorite_selection_number"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            color = colorResource(R.color.launcher4max_sheet_surface),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.style_settings_secondary_line_height).value.sp,
            style = LocalTextStyle.current.copy(shadow = null),
        )
    }
}
