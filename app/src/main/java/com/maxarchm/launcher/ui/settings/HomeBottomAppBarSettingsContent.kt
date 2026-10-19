package com.maxarchm.launcher.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.maxarchm.launcher.FavoriteListSize
import com.maxarchm.launcher.HomeBottomAppBarPresentation
import com.maxarchm.launcher.HomeBottomAppBarState
import com.maxarchm.launcher.R
import com.maxarchm.launcher.homeBottomAppBarCapacity
import com.maxarchm.launcher.iconSizeResource
import com.maxarchm.launcher.ui.style.StyleTitleLine
import com.maxarchm.launcher.ui.style.styleSettingsPanelSurface

@Composable
internal fun HomeBottomAppBarSettingsContent(
    state: HomeBottomAppBarState,
    onStateChange: (HomeBottomAppBarState) -> Unit,
    saveInFlight: Boolean = false,
    homeSafeWidthDp: Float? = null,
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
                interactive = !saveInFlight,
                onEnabledChange = { onStateChange(state.copy(enabled = it)) },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(
                                R.dimen.icon_shape_preview_window_inset,
                            ),
                            vertical = dimensionResource(
                                R.dimen.icon_shape_preview_window_inset,
                            ),
                        )
                        .fillMaxSize()
                        .clipToBounds()
                        .testTag("home_bottom_app_bar_preview_window"),
                ) {
                    HomeBottomAppBarPreview(
                        state = state,
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
    interactive: Boolean,
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
            enabled = interactive,
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
        HomeBottomAppBarSelector(
            title = title,
            labels = labels,
            selectedIndex = selectedIndex,
            enabled = enabled,
            optionEnabled = optionEnabled,
            selectorWidth = selectorWidth,
            testTagPrefix = testTagPrefix,
            onSelectIndex = onSelectIndex,
        )
    }
}

@Composable
private fun HomeBottomAppBarSelector(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    selectorWidth: Dp,
    testTagPrefix: String,
    onSelectIndex: (Int) -> Unit,
    optionEnabled: List<Boolean> = List(labels.size) { true },
) {
    val animationDuration = integerResource(
        id = R.integer.short_property_animation_duration_ms,
    )
    val frameShape = RoundedCornerShape(
        size = dimensionResource(id = R.dimen.style_settings_selector_frame_radius),
    )
    Box(
        modifier = Modifier
            .size(
                width = selectorWidth,
                height = dimensionResource(id = R.dimen.style_settings_stepper_target_size),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.style_settings_selector_height))
                .clip(frameShape)
                .background(colorResource(R.color.launcher4max_sheet_surface))
                .border(
                    width = dimensionResource(R.dimen.style_settings_selector_border_width),
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = integerResource(
                            R.integer.style_settings_selector_border_alpha_percent,
                        ) / 100f,
                    ),
                    shape = frameShape,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .selectableGroup()
                .semantics { contentDescription = title }
                .padding(
                    horizontal = dimensionResource(
                        R.dimen.style_settings_selector_inner_padding,
                    ),
                ),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val optionAvailable = enabled && optionEnabled.getOrElse(index) { false }
                val backgroundColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = animationDuration),
                    label = "${testTagPrefix}_background",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        colorResource(id = R.color.launcher4max_sheet_surface)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(durationMillis = animationDuration),
                    label = "${testTagPrefix}_content",
                )
                Box(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .height(dimensionResource(R.dimen.style_settings_stepper_target_size))
                        .clip(
                            shape = RoundedCornerShape(
                                size = dimensionResource(
                                    id = R.dimen.style_settings_selector_thumb_radius,
                                ),
                            ),
                        )
                        .selectable(
                            selected = selected,
                            enabled = optionAvailable,
                            role = Role.RadioButton,
                            onClick = { if (!selected) onSelectIndex(index) },
                        )
                        .testTag(tag = "${testTagPrefix}_$index"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(
                                dimensionResource(R.dimen.style_settings_selector_thumb_height),
                            )
                            .clip(
                                RoundedCornerShape(
                                    dimensionResource(R.dimen.style_settings_selector_thumb_radius),
                                ),
                            )
                            .background(backgroundColor),
                    )
                    Text(
                        text = label,
                        color = if (optionAvailable || selected) {
                            contentColor
                        } else {
                            contentColor.copy(
                                alpha = integerResource(
                                    R.integer.disabled_content_alpha_percent,
                                ) / 100f,
                            )
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = dimensionResource(
                            id = R.dimen.style_settings_secondary_text_size,
                        ).value.sp,
                        lineHeight = dimensionResource(
                            id = R.dimen.style_settings_secondary_line_height,
                        ).value.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBottomAppBarPreview(
    state: HomeBottomAppBarState,
    modifier: Modifier = Modifier,
) {
    val icon = painterResource(R.mipmap.ic_launcher_foreground)
    val iconDimension = dimensionResource(state.iconSize.iconSizeResource())
    val fullHeight = iconDimension + dimensionResource(
        R.dimen.home_bottom_app_bar_preview_vertical_padding,
    ) * 2
    val previewDescription = stringResource(R.string.home_bottom_app_bar_preview)
    Box(
        modifier = modifier
            .clipToBounds()
            .semantics { contentDescription = previewDescription }
            .testTag("home_bottom_app_bar_preview"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val collapsed = state.enabled &&
            state.presentation == HomeBottomAppBarPresentation.SemiHidden
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    if (collapsed) {
                        dimensionResource(R.dimen.home_bottom_app_bar_collapsed_height)
                    } else {
                        fullHeight
                    },
                )
                .clipToBounds()
                .align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(fullHeight)
                    .background(colorResource(R.color.home_bottom_app_bar_preview_background)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(
                            R.dimen.home_bottom_app_bar_preview_horizontal_inset,
                        ),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.home_bottom_app_bar_preview_icon_gap),
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) {
                        Image(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconDimension),
                        )
                    }
                }
            }
            if (collapsed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(fullHeight)
                        .background(
                            color = colorResource(
                                R.color.home_bottom_app_bar_preview_background,
                            ).copy(
                                alpha = integerResource(
                                    R.integer.home_bottom_app_bar_semi_hidden_alpha_percent,
                                ) / 100f,
                            ),
                        ),
                )
            }
        }
    }
}
