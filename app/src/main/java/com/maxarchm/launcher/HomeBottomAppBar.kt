package com.maxarchm.launcher

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
    return floor((availableWidthDp + iconGapDp) / (iconSizeDp + iconGapDp))
        .toInt()
}
