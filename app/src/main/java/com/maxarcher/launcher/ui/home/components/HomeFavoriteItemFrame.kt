package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.FavoriteNamePlacement
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.R

/** Removal is a sibling overlay, so unavailable-content opacity never dims the control. */
@Composable
internal fun HomeFavoriteItemFrame(
    modifier: Modifier,
    showRemove: Boolean,
    removeEnabled: Boolean,
    namePlacement: FavoriteNamePlacement,
    iconSize: Dp,
    onRemove: () -> Unit,
    identity: LaunchableIdentity,
    movement: HomeApplicationMovement? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val targetSize = dimensionResource(id = R.dimen.home_favorite_bar_remove_target_size)
    val iconInset = dimensionResource(id = R.dimen.home_favorite_list_icon_start_margin)
    val topInset = dimensionResource(id = R.dimen.home_favorite_below_top_inset)
    val density = LocalDensity.current
    DisposableEffect(
        key1 = movement,
        key2 = identity,
        effect = { onDispose { movement?.updateItem(identity = identity, bounds = null) } },
    )
    Box(
        modifier = modifier
            .onGloballyPositioned(
                onGloballyPositioned = { coordinates ->
                    if (movement != null) {
                        val origin = coordinates.positionInWindow()
                        val bounds = Rect(offset = origin, size = coordinates.size.toSize())
                        val remove = with(receiver = density, block = {
                            val x = if (namePlacement != FavoriteNamePlacement.Right) {
                                bounds.center.x - iconSize.toPx() / 2 - iconInset.toPx()
                            } else bounds.left
                            val y = if (namePlacement != FavoriteNamePlacement.Right) {
                                bounds.top + topInset.toPx() - iconInset.toPx()
                            } else bounds.top
                            Rect(left = x, top = y, right = x + targetSize.toPx(), bottom = y + targetSize.toPx())
                        })
                        movement.updateItem(identity = identity, bounds = bounds, remove = remove.takeIf(predicate = { showRemove }))
                    }
                },
            )
            .testTag(tag = "home_favorite_item:${identity.stableKey()}"),
        content = {
            content()
            if (showRemove) {
                val parentConfiguration = LocalViewConfiguration.current
                val removeConfiguration = remember(
                    key1 = parentConfiguration,
                    key2 = targetSize,
                    calculation = {
                        object : ViewConfiguration by parentConfiguration {
                            override val minimumTouchTargetSize = DpSize(width = targetSize, height = targetSize)
                        }
                    },
                )
                val placement = if (namePlacement != FavoriteNamePlacement.Right) {
                    Modifier
                        .align(alignment = Alignment.TopCenter)
                        .offset(
                            x = targetSize / 2 - iconSize / 2 - iconInset,
                            y = dimensionResource(id = R.dimen.home_favorite_below_top_inset) - iconInset,
                        )
                } else {
                    Modifier.align(alignment = Alignment.TopStart)
                }
                // This author-accepted 20dp target must not expand over the item's movement area.
                CompositionLocalProvider(
                    value = LocalViewConfiguration provides removeConfiguration,
                    content = {
                        Box(
                            modifier = placement
                                .size(size = targetSize)
                                .clip(shape = CircleShape)
                                .background(color = MaterialTheme.colorScheme.error)
                                .clickable(
                                    enabled = removeEnabled,
                                    role = Role.Button,
                                    onClick = onRemove,
                                )
                                .testTag(tag = "remove_favorite_item"),
                            contentAlignment = Alignment.Center,
                            content = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = stringResource(id = R.string.remove_favorite_item),
                                    modifier = Modifier.size(
                                        size = dimensionResource(id = R.dimen.home_favorite_bar_remove_icon_size),
                                    ),
                                    tint = colorResource(id = R.color.home_favorite_remove_icon),
                                )
                            },
                        )
                    },
                )
            }
        },
    )
}
