package com.maxarchm.launcher

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
