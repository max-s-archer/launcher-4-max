package com.maxarcher.launcher

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
    horizontalInsetDp: Float,
    iconGapDp: Float,
): Int {
    val availableWidthDp = safeWidthDp - horizontalInsetDp * 2
    if (availableWidthDp < iconSizeDp) return 0
    return floor((availableWidthDp + iconGapDp) / (iconSizeDp + iconGapDp)).toInt()
}

internal fun OrderedFavoriteAggregate.restoreHomeBottomAppBarForWidth(
    safeWidthDp: Float,
    horizontalInsetDp: Float,
    iconGapDp: Float,
    iconSizeDp: (FavoriteListSize) -> Float,
): OrderedFavoriteAggregate? {
    if (safeWidthDp <= 0f) return null
    val capacity = homeBottomAppBarCapacity(
        safeWidthDp = safeWidthDp,
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
