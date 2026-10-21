package com.maxarcher.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.dimensionResource

internal fun FavoriteListSize.iconSizeResource(): Int = when (this) {
    FavoriteListSize.Large -> R.dimen.home_favorite_large_icon_size
    FavoriteListSize.Medium -> R.dimen.home_favorite_icon_size
    FavoriteListSize.Small -> R.dimen.home_companion_favorite_icon_size
}

internal fun FavoriteListSize.rowHeightResource(): Int = when (this) {
    FavoriteListSize.Large -> R.dimen.home_favorite_large_row_min_height
    FavoriteListSize.Medium -> R.dimen.home_favorite_row_min_height
    FavoriteListSize.Small -> R.dimen.home_companion_favorite_row_min_height
}

internal fun FavoriteListSize.belowItemHeightResource(): Int = when (this) {
    FavoriteListSize.Large -> R.dimen.home_favorite_large_below_height
    FavoriteListSize.Medium -> R.dimen.home_favorite_medium_below_height
    FavoriteListSize.Small -> R.dimen.home_favorite_small_below_height
}

internal fun FavoriteListSize.textSizeResource(): Int = when (this) {
    FavoriteListSize.Large -> R.dimen.home_favorite_large_text_size
    FavoriteListSize.Medium -> R.dimen.home_favorite_text_size
    FavoriteListSize.Small -> R.dimen.home_companion_favorite_text_size
}

internal fun FavoriteListSize.lineHeightResource(): Int = when (this) {
    FavoriteListSize.Large -> R.dimen.home_favorite_large_line_height
    FavoriteListSize.Medium -> R.dimen.home_favorite_line_height
    FavoriteListSize.Small -> R.dimen.home_companion_favorite_line_height
}

@Composable
internal fun homeFavoriteHiddenItemHeight(iconSize: FavoriteListSize): Dp =
    dimensionResource(iconSize.iconSizeResource()) +
            dimensionResource(R.dimen.home_favorite_below_top_inset) +
            dimensionResource(R.dimen.home_favorite_below_bottom_inset)
