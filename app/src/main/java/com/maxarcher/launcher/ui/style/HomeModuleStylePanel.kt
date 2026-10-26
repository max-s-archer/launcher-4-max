package com.maxarcher.launcher.ui.style

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.FavoriteNamePlacement
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.OrderedFavoriteModuleType
import com.maxarcher.launcher.R

/**
 * The inline style settings panel of one module. It measures to its content and does not
 * scroll inside itself; the Home main list scrolls it with its owning module.
 */
@Composable
internal fun HomeModuleStylePanel(
    module: OrderedFavoriteModule,
    enabled: Boolean,
    onPreviewSizes: (FavoriteListSize, FavoriteListSize) -> Unit,
    onCommitIconSize: (FavoriteListSize) -> Unit,
    onCommitTextSize: (FavoriteListSize) -> Unit,
    onChangeNamePlacement: (FavoriteNamePlacement) -> Unit,
    onChangeItemsPerRow: (Int) -> Unit,
) {
    val animationDuration = integerResource(
        R.integer.short_property_animation_duration_ms,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .styleSettingsPanelSurface()
            .animateContentSize(
                animationSpec = tween(durationMillis = animationDuration),
            )
            .testTag("home_style_panel"),
    ) {
        if (module.type == OrderedFavoriteModuleType.Vertical) {
            HomeStyleArrangementRow(
                placement = module.namePlacement,
                value = module.itemsPerRow,
                maximum = homeItemsPerRowRange(module.namePlacement).last,
                enabled = enabled,
                onChangePlacement = onChangeNamePlacement,
                onChangeCount = onChangeItemsPerRow,
            )
            HomeApplicationSizeRow(
                iconSize = module.iconSize,
                textSize = module.textSize,
                iconEnabled = enabled,
                textEnabled = enabled &&
                        module.namePlacement != FavoriteNamePlacement.Hidden,
                onPreview = onPreviewSizes,
                onCommitIconSize = onCommitIconSize,
                onCommitTextSize = onCommitTextSize,
            )
        } else {
            HomeStylePanelRow(
                label = stringResource(R.string.home_ribbon_fixed_style),
            )
        }
    }
}

@Composable
private fun HomeStylePanelRow(label: String, value: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.style_settings_informational_row_height))
            .padding(horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.style_settings_secondary_line_height).value.sp,
        )
        value?.let { Text(text = it, color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun HomeApplicationSizeRow(
    iconSize: FavoriteListSize,
    textSize: FavoriteListSize,
    iconEnabled: Boolean,
    textEnabled: Boolean,
    onPreview: (FavoriteListSize, FavoriteListSize) -> Unit,
    onCommitIconSize: (FavoriteListSize) -> Unit,
    onCommitTextSize: (FavoriteListSize) -> Unit,
) {
    val options = FavoriteListSize.values()
    val labels = options.map { option ->
        stringResource(
            id = when (option) {
                FavoriteListSize.Large -> R.string.favorite_list_large
                FavoriteListSize.Medium -> R.string.favorite_list_medium
                FavoriteListSize.Small -> R.string.favorite_list_small
            },
        )
    }
    val iconSelectedIndex = options.indexOf(iconSize)
    val textSelectedIndex = options.indexOf(textSize)
    var iconPreviewIndex by remember(iconSelectedIndex) { mutableIntStateOf(iconSelectedIndex) }
    var textPreviewIndex by remember(textSelectedIndex) { mutableIntStateOf(textSelectedIndex) }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("home_application_size_setting"),
    ) {
        StyleTitleLine(text = stringResource(id = R.string.home_application_size))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.style_settings_application_size_line_height))
                .padding(horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StyleTaperingSlider(
                selectedStop = iconPreviewIndex,
                stopLabels = labels,
                label = stringResource(R.string.style_settings_application_icon_size),
                increaseLabel = stringResource(
                    R.string.style_settings_increase_application_icon_size,
                ),
                decreaseLabel = stringResource(
                    R.string.style_settings_decrease_application_icon_size,
                ),
                onSelectStop = {
                    iconPreviewIndex = it
                    onPreview(options[it], options[textPreviewIndex])
                },
                onSelectStopFinished = {
                    iconPreviewIndex = it
                    onCommitIconSize(options[it])
                },
                enabled = iconEnabled,
                modifier = Modifier.weight(1f).testTag("home_application_size_icon_slider"),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.style_settings_tapering_slider_gap)))
            StyleTaperingSlider(
                selectedStop = textPreviewIndex,
                stopLabels = labels,
                label = stringResource(R.string.style_settings_application_text_size),
                increaseLabel = stringResource(
                    R.string.style_settings_increase_application_text_size,
                ),
                decreaseLabel = stringResource(
                    R.string.style_settings_decrease_application_text_size,
                ),
                onSelectStop = {
                    textPreviewIndex = it
                    onPreview(options[iconPreviewIndex], options[it])
                },
                onSelectStopFinished = {
                    textPreviewIndex = it
                    onCommitTextSize(options[it])
                },
                enabled = textEnabled,
                modifier = Modifier.weight(1f).testTag("home_application_size_text_slider"),
            )
        }
    }
}

@Composable
private fun HomeStyleArrangementRow(
    placement: FavoriteNamePlacement,
    value: Int,
    maximum: Int,
    enabled: Boolean,
    onChangePlacement: (FavoriteNamePlacement) -> Unit,
    onChangeCount: (Int) -> Unit,
) {
    val options = FavoriteNamePlacement.values()
    StyleArrangementBlock(
        title = stringResource(id = R.string.home_application_arrangement),
        optionLabels = options.map { option ->
            stringResource(
                id = when (option) {
                    FavoriteNamePlacement.Right -> R.string.home_name_right
                    FavoriteNamePlacement.Below -> R.string.home_name_below
                    FavoriteNamePlacement.Hidden -> R.string.home_name_hidden
                },
            )
        },
        selectedIndex = options.indexOf(element = placement),
        value = value,
        minimum = 1,
        maximum = maximum,
        decrementLabel = stringResource(id = R.string.home_decrement_symbol),
        incrementLabel = stringResource(id = R.string.home_increment_symbol),
        enabled = enabled,
        onSelectIndex = { index -> onChangePlacement(options[index]) },
        onChangeValue = onChangeCount,
        testTagPrefix = "home",
    )
}

private fun homeItemsPerRowRange(placement: FavoriteNamePlacement): IntRange = when (placement) {
    FavoriteNamePlacement.Right -> 1..2
    FavoriteNamePlacement.Below -> 1..4
    FavoriteNamePlacement.Hidden -> 1..6
}
