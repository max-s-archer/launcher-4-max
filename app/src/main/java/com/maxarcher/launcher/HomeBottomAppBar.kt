package com.maxarcher.launcher

import android.content.res.Resources
import kotlin.math.floor

internal enum class HomeBottomAppBarPresentation {
    Fixed,
    SemiHidden,
}

internal data class HomeBottomAppBarState(
    val enabled: Boolean = false,
    val presentation: HomeBottomAppBarPresentation =
        HomeBottomAppBarPresentation.SemiHidden,
    val iconSize: FavoriteListSize = FavoriteListSize.Medium,
    val identities: List<LaunchableIdentity> = emptyList(),
)

internal fun homeBottomAppBarCapacity(
    safeWidthDp: Float,
    iconSizeDp: Float,
    outerMarginDp: Float,
    horizontalInsetDp: Float,
    iconGapDp: Float,
): Int {
    val availableWidthDp = safeWidthDp - (outerMarginDp + horizontalInsetDp) * 2
    if (availableWidthDp < iconSizeDp) return 0
    return floor((availableWidthDp + iconGapDp) / (iconSizeDp + iconGapDp)).toInt()
}

internal fun homeBottomAppBarCapacity(
    resources: Resources,
    safeWidthDp: Float,
    iconSize: FavoriteListSize,
): Int {
    val density = resources.displayMetrics.density
    fun dimensionDp(id: Int) = resources.getDimension(id) / density
    return homeBottomAppBarCapacity(
        safeWidthDp = safeWidthDp,
        iconSizeDp = dimensionDp(iconSize.iconSizeResource()),
        outerMarginDp = dimensionDp(R.dimen.home_bottom_app_bar_outer_margin),
        horizontalInsetDp = dimensionDp(R.dimen.home_bottom_app_bar_preview_horizontal_inset),
        iconGapDp = dimensionDp(R.dimen.home_bottom_app_bar_preview_icon_gap),
    )
}

internal fun OrderedFavoriteAggregate.restoreHomeBottomAppBarForWidth(
    safeWidthDp: Float,
    outerMarginDp: Float,
    horizontalInsetDp: Float,
    iconGapDp: Float,
    iconSizeDp: (FavoriteListSize) -> Float,
): OrderedFavoriteAggregate? {
    if (safeWidthDp <= 0f) return null
    val capacity = homeBottomAppBarCapacity(
        safeWidthDp = safeWidthDp,
        outerMarginDp = outerMarginDp,
        iconSizeDp = iconSizeDp(homeBottomAppBar.iconSize),
        horizontalInsetDp = horizontalInsetDp,
        iconGapDp = iconGapDp,
    )
    return copy(
        homeBottomAppBar = homeBottomAppBar.copy(
            identities = homeBottomAppBar.identities.take(capacity),
        ),
    )
}
