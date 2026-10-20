package com.maxarchm.launcher.ui.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.maxarchm.launcher.FavoriteAvailability
import com.maxarchm.launcher.FavoriteBarContainerDragSession
import com.maxarchm.launcher.FavoriteListDragSession
import com.maxarchm.launcher.FavoriteListSize
import com.maxarchm.launcher.LaunchableIdentity
import com.maxarchm.launcher.R
import com.maxarchm.launcher.iconSizeResource
import com.maxarchm.launcher.lineHeightResource
import com.maxarchm.launcher.rowHeightResource
import com.maxarchm.launcher.textSizeResource
import com.maxarchm.launcher.ui.drawer.drawerForegroundShadow
import com.maxarchm.launcher.ui.home.components.HomeFavoriteRibbonRailDivider
import kotlin.math.roundToInt

@Composable
internal fun HomeFavoriteBarContainerDragPreview(
    session: FavoriteBarContainerDragSession,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    rootOriginInWindow: Offset,
) {
    val density = LocalDensity.current
    val topLeft = session.originInWindow + session.delta - rootOriginInWindow
    val width = with(density) { session.size.width.toDp() }
    val height = with(density) { session.size.height.toDp() }
    val borderAlpha =
        integerResource(R.integer.home_favorite_bar_border_alpha_percent) / 100f
    val fadeAlpha =
        integerResource(R.integer.home_favorite_bar_overflow_fade_alpha_percent) / 100f
    val shape = RoundedCornerShape(dimensionResource(R.dimen.home_favorite_bar_corner_radius))
    Row(
        modifier = Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .size(width, height)
            .clip(shape)
            .background(colorResource(R.color.home_edit_surface))
            .border(
                dimensionResource(R.dimen.home_favorite_bar_border_width),
                MaterialTheme.colorScheme.onBackground.copy(alpha = borderAlpha),
                shape,
            )
            .clearAndSetSemantics {}
            .testTag("home_favorite_bar_container_drag_preview"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(R.dimen.home_favorite_bar_control_target_width))
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.home_favorite_list_remove_badge_size))
                    .clip(
                        RoundedCornerShape(
                            dimensionResource(
                                R.dimen.home_favorite_list_control_surface_radius,
                            ),
                        ),
                    )
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.home_favorite_list_remove_icon_size),
                    ),
                    tint = colorResource(R.color.home_favorite_remove_icon),
                )
            }
        }
        HomeFavoriteRibbonRailDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset {
                        IntOffset(-session.visibleScrollOffset, 0)
                    },
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.home_favorite_bar_item_spacing),
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                session.visibleIdentities.forEach { identity ->
                    Box(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.home_favorite_bar_item_width))
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        HomeFavoritePreviewContent(
                            availability = availabilityByIdentity[identity]
                                ?: FavoriteAvailability.Unknown(null),
                            iconSize = FavoriteListSize.Medium,
                            textSize = FavoriteListSize.Medium,
                            maxWidth = dimensionResource(R.dimen.home_favorite_bar_item_width),
                            shadowElevation = 0f,
                        )
                    }
                }
            }
            val fadeColor = MaterialTheme.colorScheme.background.copy(alpha = fadeAlpha)
            val fadeWidth = dimensionResource(R.dimen.home_favorite_bar_overflow_fade_width)
            if (session.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(fadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(fadeColor, fadeColor.copy(alpha = 0f)),
                            ),
                        ),
                )
            }
            if (session.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(fadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(fadeColor.copy(alpha = 0f), fadeColor),
                            ),
                        ),
                )
            }
        }
        HomeFavoriteRibbonRailDivider()
        Box(
            modifier = Modifier
                .width(dimensionResource(R.dimen.home_favorite_bar_control_target_width))
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                modifier = Modifier.size(
                    dimensionResource(R.dimen.home_favorite_bar_control_icon_size),
                ),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable

internal fun HomeFavoriteListDragPreview(
    session: FavoriteListDragSession,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    rootOriginInWindow: Offset,
) {
    val density = LocalDensity.current
    val width = with(density) { session.size.width.toDp() }
    val height = with(density) { session.size.height.toDp() }
    val topLeft = session.originInWindow + session.delta - rootOriginInWindow
    val previewAlpha = integerResource(R.integer.home_drag_preview_alpha_percent) / 100f
    val rowHeight = dimensionResource(session.sourceContainer.iconSize.rowHeightResource())
    val dividerColor = colorResource(R.color.home_favorite_list_control_border)
    val dividerWidth = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_favorite_list_control_border_width).toPx()
    }
    Column(
        modifier = Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .size(width, height)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.home_edit_surface_radius)))
            .background(colorResource(R.color.home_edit_surface))
            .alpha(previewAlpha)
            .clearAndSetSemantics {}
            .testTag("home_favorite_list_drag_preview"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.home_favorite_list_control_bar_height))
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height - dividerWidth / 2f),
                        end = Offset(size.width, size.height - dividerWidth / 2f),
                        strokeWidth = dividerWidth,
                    )
                },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .width(
                                dimensionResource(
                                    R.dimen.home_favorite_list_control_target_width,
                                ),
                            )
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(
                                    dimensionResource(
                                        R.dimen.home_favorite_list_control_target_width,
                                    ),
                                )
                                .height(
                                    dimensionResource(
                                        R.dimen.home_favorite_list_control_bar_height,
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        dimensionResource(
                                            R.dimen.home_favorite_list_remove_badge_size,
                                        ),
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            dimensionResource(
                                                R.dimen
                                                    .home_favorite_list_control_surface_radius,
                                            ),
                                        ),
                                    )
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = null,
                                    tint = colorResource(R.color.home_favorite_remove_icon),
                                    modifier = Modifier.size(
                                        dimensionResource(
                                            R.dimen.home_favorite_list_remove_icon_size,
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                    Box(
                        Modifier
                            .width(
                                dimensionResource(
                                    R.dimen.home_favorite_list_control_target_width,
                                ),
                            )
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (session.sourceContainer.listSize) {
                                FavoriteListSize.Large ->
                                    stringResource(R.string.favorite_list_large_short)

                                FavoriteListSize.Medium ->
                                    stringResource(R.string.favorite_list_medium_short)

                                FavoriteListSize.Small ->
                                    stringResource(R.string.favorite_list_small_short)
                            },
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = dimensionResource(
                                R.dimen.home_favorite_list_size_control_text_size,
                            ).value.sp,
                        )
                    }
                    Box(
                        Modifier
                            .width(
                                dimensionResource(
                                    R.dimen.home_favorite_list_control_target_width,
                                ),
                            )
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(
                                    dimensionResource(
                                        R.dimen.home_favorite_list_control_target_width,
                                    ),
                                )
                                .height(
                                    dimensionResource(
                                        R.dimen.home_favorite_list_control_bar_height,
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_handle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(
                                    dimensionResource(
                                        R.dimen.home_favorite_list_reorder_icon_size,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            Column(
                modifier = Modifier.offset {
                    IntOffset(x = 0, y = -session.visibleScrollOffset)
                },
            ) {
                session.visibleIdentities.forEach { identity ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = dimensionResource(
                                        R.dimen.home_favorite_list_icon_start_margin,
                                    ),
                                ),
                        ) {
                            HomeFavoritePreviewContent(
                                availability = availabilityByIdentity[identity]
                                    ?: FavoriteAvailability.Unknown(null),
                                iconSize = session.sourceContainer.iconSize,
                                textSize = session.sourceContainer.textSize,
                                maxWidth = width,
                                shadowElevation = 0f,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable

internal fun HomeFavoritePreviewContent(
    availability: FavoriteAvailability,
    iconSize: FavoriteListSize,
    textSize: FavoriteListSize,
    maxWidth: androidx.compose.ui.unit.Dp,
    shadowElevation: Float,
) {
    val entry = availability.presentationEntry
    val iconDimension = dimensionResource(iconSize.iconSizeResource())
    val iconPixels = with(LocalDensity.current) { iconDimension.roundToPx() }
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
    Row(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry == null) {
            Icon(
                painter = painterResource(R.drawable.ic_inventory_error),
                contentDescription = null,
                modifier = Modifier.size(iconDimension),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            val bitmap = entry.iconBitmap?.asImageBitmap() ?: remember(entry.icon, iconPixels) {
                entry.icon.toBitmap(iconPixels, iconPixels).asImageBitmap()
            }
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconDimension))
        }
        Spacer(Modifier.width(dimensionResource(R.dimen.home_favorite_icon_label_gap)))
        Text(
            text = displayText,
            modifier = Modifier.widthIn(max = maxWidth),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = dimensionResource(textSize.textSizeResource()).value.sp,
            lineHeight = dimensionResource(textSize.lineHeightResource()).value.sp,
            style = LocalTextStyle.current.copy(shadow = drawerForegroundShadow()),
        )
    }
}
