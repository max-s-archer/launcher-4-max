package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.FavoriteNamePlacement
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.R

/**
 * Reports the item and its removal control to an active movement. The control itself is drawn
 * on the icon, 4dp outside the icon's top-start corner. [contentStart] and [contentTop] are
 * the icon's insets inside this frame, so the reported area uses that same corner.
 */
@Composable
internal fun HomeFavoriteItemFrame(
    modifier: Modifier,
    showRemove: Boolean,
    namePlacement: FavoriteNamePlacement,
    iconSize: Dp,
    identity: LaunchableIdentity,
    movement: HomeApplicationMovement? = null,
    contentStart: Dp = 0.dp,
    contentTop: Dp = 0.dp,
    contentBottom: Dp = contentTop,
    content: @Composable BoxScope.() -> Unit,
) {
    val targetSize = dimensionResource(R.dimen.home_favorite_bar_remove_target_size)
    val outset = dimensionResource(R.dimen.home_favorite_remove_icon_outset)
    val density = LocalDensity.current
    DisposableEffect(movement, identity) {
        onDispose { movement?.updateItem(identity = identity, bounds = null) }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (movement == null) return@onGloballyPositioned
                val origin = coordinates.positionInWindow()
                val bounds = Rect(offset = origin, size = coordinates.size.toSize())
                val remove = with(density) {
                    val iconLeft = when (namePlacement) {
                        FavoriteNamePlacement.Right -> bounds.left + contentStart.toPx()
                        FavoriteNamePlacement.Below,
                        FavoriteNamePlacement.Hidden,
                        -> bounds.center.x - iconSize.toPx() / 2
                    }
                    val iconTop = when (namePlacement) {
                        FavoriteNamePlacement.Right ->
                            bounds.center.y - iconSize.toPx() / 2
                        FavoriteNamePlacement.Below -> bounds.top + contentTop.toPx()
                        FavoriteNamePlacement.Hidden -> bounds.top + contentTop.toPx() +
                            (
                                bounds.height - contentTop.toPx() - contentBottom.toPx() -
                                    iconSize.toPx()
                                ) / 2
                    }
                    val x = iconLeft - outset.toPx()
                    val y = iconTop - outset.toPx()
                    Rect(
                        left = x,
                        top = y,
                        right = x + targetSize.toPx(),
                        bottom = y + targetSize.toPx(),
                    )
                }
                movement.updateItem(
                    identity = identity,
                    bounds = bounds,
                    remove = remove.takeIf { showRemove },
                )
            }
            .testTag("home_favorite_item:${identity.stableKey()}"),
        content = content,
    )
}
