package com.maxarcher.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.HomeBottomAppBarPresentation
import com.maxarcher.launcher.HomeBottomAppBarState
import com.maxarcher.launcher.IconShapeClipMode
import com.maxarcher.launcher.IconShapeSettings
import com.maxarcher.launcher.R
import com.maxarcher.launcher.homeBottomAppBarCapacity
import com.maxarcher.launcher.iconSizeResource
import com.maxarcher.launcher.platform.icons.OEM_ICON_CLIP
import com.maxarcher.launcher.platform.icons.clipLauncherIcon
import com.maxarcher.launcher.platform.icons.prepareLauncherIconBitmap
import com.maxarcher.launcher.ui.style.StyleTitleLine
import com.maxarcher.launcher.ui.style.StyleTwoOptionSelector
import com.maxarcher.launcher.ui.style.styleSettingsPanelSurface
import kotlin.math.roundToInt

@Composable
internal fun HomeBottomAppBarSettingsContent(
    state: HomeBottomAppBarState,
    onStateChange: (HomeBottomAppBarState) -> Unit,
    saveInFlight: Boolean = false,
    homeSafeWidthDp: Float? = null,
    iconShapeSettings: IconShapeSettings = IconShapeSettings(),
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_bottom_app_bar_settings_page"),
    ) {
        val measuredSafeWidthDp = homeSafeWidthDp ?: maxWidth.value
        val capacity = homeBottomAppBarCapacity(
            safeWidthDp = measuredSafeWidthDp,
            outerMarginDp = dimensionResource(R.dimen.home_bottom_app_bar_outer_margin).value,
            iconSizeDp = dimensionResource(state.iconSize.iconSizeResource()).value,
            horizontalInsetDp = dimensionResource(
                R.dimen.home_bottom_app_bar_preview_horizontal_inset,
            ).value,
            iconGapDp = dimensionResource(
                R.dimen.home_bottom_app_bar_preview_icon_gap,
            ).value,
        )
        val iconTiers = listOf(
            FavoriteListSize.Large,
            FavoriteListSize.Medium,
            FavoriteListSize.Small,
        )
        Column(modifier = Modifier.fillMaxSize()) {
            HomeBottomAppBarSwitch(
                enabled = state.enabled,
                onEnabledChange = { enabled ->
                    if (enabled != state.enabled) onStateChange(state.copy(enabled = enabled))
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            ).consume()
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { change -> change.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                val windowInset = dimensionResource(
                    R.dimen.home_bottom_app_bar_preview_window_inset,
                )
                val windowCorner = dimensionResource(
                    R.dimen.home_bottom_app_bar_preview_window_corner_radius,
                )
                Box(
                    modifier = Modifier
                        .padding(all = windowInset)
                        .fillMaxSize()
                        .clipToBounds()
                        .drawWithContent {
                            // Clear only this window in the enclosing Settings offscreen layer.
                            drawRoundRect(
                                color = Color.Transparent,
                                cornerRadius = CornerRadius(windowCorner.toPx()),
                                blendMode = BlendMode.Clear,
                            )
                            drawContent()
                        }
                        .testTag("home_bottom_app_bar_preview_window"),
                ) {
                    HomeBottomAppBarPreview(
                        state = state,
                        capacity = capacity,
                        homeSafeWidthDp = measuredSafeWidthDp,
                        iconShapeSettings = iconShapeSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(
                                R.dimen.settings_horizontal_padding,
                            ),
                        )
                        .styleSettingsPanelSurface(),
                ) {
                    StyleTitleLine(
                        text = stringResource(R.string.home_bottom_app_bar_presentation),
                    )
                    HomeBottomAppBarSelectorRow(
                        title = stringResource(R.string.home_bottom_app_bar_presentation),
                        labels = listOf(
                            stringResource(R.string.home_bottom_app_bar_fixed),
                            stringResource(R.string.home_bottom_app_bar_semi_hidden),
                        ),
                        selectedIndex = when (state.presentation) {
                            HomeBottomAppBarPresentation.Fixed -> 0
                            HomeBottomAppBarPresentation.SemiHidden -> 1
                        },
                        enabled = state.enabled && !saveInFlight,
                        selectorWidth = dimensionResource(
                            R.dimen.style_settings_selector_width,
                        ),
                        testTagPrefix = "home_bottom_app_bar_presentation",
                        onSelectIndex = {
                            onStateChange(
                                state.copy(
                                    presentation = if (it == 0) {
                                        HomeBottomAppBarPresentation.Fixed
                                    } else {
                                        HomeBottomAppBarPresentation.SemiHidden
                                    },
                                ),
                            )
                        },
                    )
                    StyleTitleLine(
                        text = stringResource(R.string.home_bottom_app_bar_icon_size),
                    )
                    HomeBottomAppBarSelectorRow(
                        title = stringResource(R.string.home_bottom_app_bar_icon_size),
                        labels = listOf(
                            stringResource(R.string.favorite_list_large),
                            stringResource(R.string.favorite_list_medium),
                            stringResource(R.string.favorite_list_small),
                        ),
                        selectedIndex = state.iconSize.ordinal,
                        enabled = state.enabled && !saveInFlight,
                        optionEnabled = iconTiers.map { tier ->
                            homeBottomAppBarCapacity(
                                safeWidthDp = measuredSafeWidthDp,
                                outerMarginDp = dimensionResource(
                                    R.dimen.home_bottom_app_bar_outer_margin,
                                ).value,
                                iconSizeDp = dimensionResource(tier.iconSizeResource()).value,
                                horizontalInsetDp = dimensionResource(
                                    R.dimen.home_bottom_app_bar_preview_horizontal_inset,
                                ).value,
                                iconGapDp = dimensionResource(
                                    R.dimen.home_bottom_app_bar_preview_icon_gap,
                                ).value,
                            ) >= state.identities.size
                        },
                        selectorWidth = dimensionResource(
                            R.dimen.style_settings_three_option_selector_width,
                        ),
                        testTagPrefix = "home_bottom_app_bar_icon_size",
                        onSelectIndex = {
                            onStateChange(state.copy(iconSize = iconTiers[it]))
                        },
                    )
                    Text(
                        text = stringResource(
                            R.string.home_bottom_app_bar_capacity_preview,
                            capacity,
                        ),
                        modifier = Modifier
                            .padding(
                                horizontal = dimensionResource(
                                    R.dimen.style_settings_panel_row_inset,
                                ),
                            )
                            .testTag("home_bottom_app_bar_capacity"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(
                Modifier.height(dimensionResource(R.dimen.icon_shape_settings_bottom_margin)),
            )
        }
    }
}

@Composable
private fun HomeBottomAppBarSwitch(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val description = stringResource(R.string.home_bottom_app_bar_enable)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.settings_primary_one_line_min_height))
            .padding(horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset))
            .testTag("home_bottom_app_bar_enabled"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.semantics {
                contentDescription = description
            },
        )
    }
}

@Composable
private fun HomeBottomAppBarSelectorRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    selectorWidth: Dp,
    testTagPrefix: String,
    onSelectIndex: (Int) -> Unit,
    optionEnabled: List<Boolean> = List(labels.size) { true },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.style_settings_content_line_height))
            .padding(
                horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyleTwoOptionSelector(
            title = title,
            optionLabels = labels,
            selectedIndex = selectedIndex,
            selectorWidth = selectorWidth,
            enabled = enabled,
            optionEnabled = optionEnabled,
            onSelectIndex = onSelectIndex,
            testTagPrefix = testTagPrefix,
        )
    }
}

@Composable
private fun HomeBottomAppBarPreview(
    state: HomeBottomAppBarState,
    capacity: Int,
    homeSafeWidthDp: Float,
    iconShapeSettings: IconShapeSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) {
        dimensionResource(state.iconSize.iconSizeResource()).roundToPx()
    }
    val clip = if (iconShapeSettings.mode == IconShapeClipMode.Oem) {
        OEM_ICON_CLIP
    } else {
        iconShapeSettings.freeformClip
    }
    val bitmap = remember(clip, pixels) {
        val source = prepareLauncherIconBitmap(
            context.packageManager.defaultActivityIcon,
            pixels,
            context.resources,
        )
        clipLauncherIcon(source = source, clip = clip).asImageBitmap()
    }
    val collapsed = state.presentation == HomeBottomAppBarPresentation.SemiHidden
    val previewDescription = stringResource(R.string.home_bottom_app_bar_preview)
    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .semantics { contentDescription = previewDescription }
            .testTag("home_bottom_app_bar_preview"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val scale = if (homeSafeWidthDp <= 0f) 0f else maxWidth.value / homeSafeWidthDp
        val iconSize = dimensionResource(state.iconSize.iconSizeResource()) * scale
        val verticalPadding = dimensionResource(
            R.dimen.home_bottom_app_bar_preview_vertical_padding,
        ) * scale
        val fullHeight = iconSize + verticalPadding * 2
        val exposure = dimensionResource(R.dimen.home_bottom_app_bar_collapsed_exposure) * scale
        val outerMargin = dimensionResource(R.dimen.home_bottom_app_bar_outer_margin) * scale
        val corner = dimensionResource(R.dimen.home_bottom_app_bar_corner_radius) * scale
        val gap = dimensionResource(R.dimen.home_bottom_app_bar_preview_icon_gap) * scale
        val inset = dimensionResource(
            R.dimen.home_bottom_app_bar_preview_horizontal_inset,
        ) * scale
        val backgroundAlpha = integerResource(
            R.integer.home_bottom_app_bar_background_alpha_percent,
        ) / 100f
        val collapsedAlpha = integerResource(
            R.integer.home_bottom_app_bar_semi_hidden_alpha_percent,
        ) / 100f
        Box(
            modifier = Modifier
                .padding(start = outerMargin, end = outerMargin, bottom = outerMargin)
                .fillMaxWidth()
                .height(fullHeight)
                .graphicsLayer {
                    translationY = if (collapsed) (fullHeight - exposure).toPx() else 0f
                    alpha = if (collapsed) collapsedAlpha else 1f
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .clipToBounds(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(fullHeight)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(corner))
                    .background(
                        colorResource(R.color.home_bottom_app_bar_preview_background).copy(
                            alpha = backgroundAlpha,
                        ),
                    )
                    .border(
                        width = dimensionResource(R.dimen.home_favorite_bar_border_width) * scale,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = integerResource(
                                R.integer.home_favorite_bar_border_alpha_percent,
                            ) / 100f,
                        ),
                        shape = RoundedCornerShape(corner),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = inset),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(capacity) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
            }
        }
    }
}
