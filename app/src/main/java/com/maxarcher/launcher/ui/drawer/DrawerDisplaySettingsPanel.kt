package com.maxarcher.launcher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import com.maxarcher.launcher.R
import com.maxarcher.launcher.ui.style.StyleArrangementBlock
import com.maxarcher.launcher.ui.style.StyleBackgroundOpacityBlock
import com.maxarcher.launcher.ui.style.StyleSelectorBlock
import com.maxarcher.launcher.ui.style.StyleTaperingSlider
import com.maxarcher.launcher.ui.style.styleSettingsPanelSurface

@Composable
internal fun DrawerDisplaySettingsPanel(
    settings: DrawerDisplaySettings,
    enabled: Boolean,
    onChangeSettings: (DrawerDisplaySettings) -> Unit,
    onPreviewOpacity: (Int?) -> Unit,
    onPreviewApplicationSizes: (DrawerDisplaySettings?) -> Unit = {},
    onDismiss: () -> Unit,
) {
    DrawerPanelAppearance {
        // The panel opens and closes with one height property animation — growing from 0 to
        // its content height on open and retracting back on close — on the shared short
        // property-animation token, matching the Home style panel. Host-driven removals
        // outside this panel remain immediate.
        val animationDuration = integerResource(R.integer.short_property_animation_duration_ms)
        var appeared by remember { mutableStateOf(false) }
        var dismissing by remember { mutableStateOf(false) }
        LaunchedEffect(key1 = Unit) { appeared = true }
        fun requestDismiss() {
            if (!dismissing) dismissing = true
        }
        if (dismissing) {
            LaunchedEffect(key1 = Unit) {
                // Cover the exit animation on its own clock, then hand removal to the host.
                Animatable(0f).animateTo(1f, animationSpec = tween(animationDuration))
                onDismiss()
            }
        }
        BackHandler(onBack = { requestDismiss() })
        DisposableEffect(key1 = Unit) {
            // Any removal before a release (dismiss, external hide, process end) reverts
            // the drag preview: the background and readout fall back to the persisted state
            // and nothing is committed.
            onDispose {
                onPreviewOpacity(null)
                onPreviewApplicationSizes(null)
            }
        }
        var panelBounds by remember { mutableStateOf(Rect.Zero) }
        var modalRootOrigin by remember { mutableStateOf(Offset.Zero) }
        val selection = listOf(
            settings.iconSize,
            settings.textSize,
            settings.namePlacement,
            settings.sectionAnchorPresentation,
        )
        var settledSelection by remember { mutableStateOf(selection) }
        var selectionPending by remember { mutableStateOf(false) }
        var selectionRequest by remember { mutableIntStateOf(0) }
        var initialSelection by remember { mutableStateOf(true) }
        LaunchedEffect(selection, selectionRequest) {
            if (!initialSelection) {
                // Use the selector's animation clock (including reduced-motion settings),
                // not a wall-clock delay. A rollback restarts this interval as well.
                Animatable(0f).animateTo(1f, animationSpec = tween(animationDuration))
            }
            initialSelection = false
            settledSelection = selection
            selectionPending = false
        }
        var previewOpacity by remember { mutableStateOf<Int?>(null) }
        var pendingOpacityCommit by remember { mutableStateOf<Int?>(null) }
        // The release-committed save is the single unresolved change; the gate reopens
        // when the persisted settings round-trip (committed value or restored rollback).
        LaunchedEffect(settings) { pendingOpacityCommit = null }
        val opacityCommitPending = pendingOpacityCommit != null
        val displayOpacity = previewOpacity ?: settings.backgroundOpacity
        // One shared mutation gate. It reads the state at call time — the composition-time
        // consumers evaluate it during recomposition and the changeSettings guard
        // re-evaluates it per event, preserving the synchronous same-frame gate.
        fun mutationAllowed() = enabled && !dismissing && !opacityCommitPending &&
            !selectionPending && selection == settledSelection
        val mutationEnabled = mutationAllowed()
        fun changeSettings(candidate: DrawerDisplaySettings) {
            if (!mutationAllowed() || candidate == settings) {
                return
            }
            if (candidate.iconSize != settings.iconSize ||
                candidate.textSize != settings.textSize ||
                candidate.namePlacement != settings.namePlacement ||
                candidate.sectionAnchorPresentation != settings.sectionAnchorPresentation
            ) {
                // Close the gate synchronously, before another activation can arrive.
                selectionPending = true
                selectionRequest++
            }
            onChangeSettings(candidate)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    modalRootOrigin = coordinates.positionInRoot()
                }
                .pointerInput(key1 = panelBounds) {
                    detectTapGestures { position ->
                        // The tap lands in modal coordinates while the panel bounds track the
                        // root during the height animation; compare both in root space.
                        if (modalRootOrigin + position !in panelBounds) {
                            requestDismiss()
                        }
                    }
                }
                .testTag(tag = "drawer_display_settings_modal"),
        ) {
            AnimatedVisibility(
                visible = appeared && !dismissing,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = animationDuration),
                    expandFrom = Alignment.Bottom,
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = animationDuration),
                    shrinkTowards = Alignment.Bottom,
                ),
                modifier = Modifier.align(alignment = Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                        .padding(
                            start = dimensionResource(
                                id = R.dimen.drawer_display_settings_horizontal_margin,
                            ),
                            end = dimensionResource(
                                id = R.dimen.drawer_display_settings_horizontal_margin,
                            ),
                            bottom = dimensionResource(
                                id = R.dimen.drawer_display_settings_bottom_margin,
                            ),
                        )
                        .fillMaxWidth()
                        .styleSettingsPanelSurface()
                        .onGloballyPositioned { coordinates ->
                            panelBounds = coordinates.boundsInRoot()
                        }
                        .testTag(tag = "drawer_display_settings_panel"),
                ) {
                    val placementOptions = DrawerNamePlacement.values()
                    val validRange = validItemsPerRowRange(
                        namePlacement = settings.namePlacement,
                    )
                    StyleArrangementBlock(
                        title = stringResource(id = R.string.drawer_application_arrangement),
                        optionLabels = placementOptions.map { placement ->
                            stringResource(
                                id = when (placement) {
                                    DrawerNamePlacement.Right -> R.string.drawer_name_right
                                    DrawerNamePlacement.Below -> R.string.drawer_name_below
                                    DrawerNamePlacement.Hidden -> R.string.drawer_name_hidden
                                },
                            )
                        },
                        selectedIndex = placementOptions.indexOf(element = settings.namePlacement),
                        value = settings.itemsPerRow,
                        minimum = validRange.first,
                        maximum = validRange.last,
                        decrementLabel = stringResource(id = R.string.home_decrement_symbol),
                        incrementLabel = stringResource(id = R.string.home_increment_symbol),
                        enabled = mutationEnabled,
                        onSelectIndex = { index ->
                            val placement = placementOptions[index]
                            changeSettings(
                                settings.copy(
                                    namePlacement = placement,
                                    itemsPerRow = settings.itemsPerRow.coerceIn(
                                        range = validItemsPerRowRange(namePlacement = placement),
                                    ),
                                ),
                            )
                        },
                        onChangeValue = { value ->
                            changeSettings(settings.copy(itemsPerRow = value))
                        },
                        testTagPrefix = "drawer",
                    )
                    val anchorOptions = DrawerSectionAnchorPresentation.entries
                    StyleSelectorBlock(
                        title = stringResource(R.string.drawer_section_anchor_presentation),
                        optionLabels = listOf(
                            stringResource(R.string.drawer_section_anchor_inline),
                            stringResource(R.string.drawer_section_anchor_left),
                        ),
                        selectedIndex = anchorOptions.indexOf(settings.sectionAnchorPresentation),
                        enabled = mutationEnabled,
                        onSelectIndex = { index ->
                            changeSettings(
                                settings.copy(
                                    sectionAnchorPresentation = anchorOptions[index],
                                ),
                            )
                        },
                        testTagPrefix = "drawer_section_anchor",
                    )
                    StyleBackgroundOpacityBlock(
                        title = stringResource(R.string.drawer_background_opacity),
                        opacity = displayOpacity,
                        enabled = mutationEnabled,
                        onOpacityChange = { value ->
                            previewOpacity = value
                            onPreviewOpacity(value)
                        },
                        onOpacityChangeFinished = { value ->
                            previewOpacity = null
                            onPreviewOpacity(null)
                            if (value != settings.backgroundOpacity && !opacityCommitPending) {
                                pendingOpacityCommit = value
                                changeSettings(settings.copy(backgroundOpacity = value))
                            }
                        },
                        sliderTestTag = "drawer_background_opacity_slider",
                    )
                    DrawerApplicationSizeSliders(
                        title = stringResource(id = R.string.drawer_application_size),
                        iconSize = settings.iconSize,
                        textSize = settings.textSize,
                        iconEnabled = mutationEnabled,
                        textEnabled = mutationEnabled &&
                                settings.namePlacement != DrawerNamePlacement.Hidden,
                        onPreview = { iconSize, textSize ->
                            onPreviewApplicationSizes(settings.copy(iconSize = iconSize, textSize = textSize))
                        },
                        onCommitIconSize = { iconSize ->
                            onPreviewApplicationSizes(null)
                            changeSettings(settings.copy(iconSize = iconSize))
                        },
                        onCommitTextSize = { textSize ->
                            onPreviewApplicationSizes(null)
                            changeSettings(settings.copy(textSize = textSize))
                        },
                        modifier = Modifier.testTag(tag = "drawer_application_size_setting"),
                    )
                }
            }
        }
    }
}

/**
 * Each slider previews independently and commits only its corresponding persisted field on release.
 */
@Composable
private fun DrawerApplicationSizeSliders(
    title: String,
    iconSize: DrawerApplicationSize,
    textSize: DrawerApplicationSize,
    iconEnabled: Boolean,
    textEnabled: Boolean,
    onPreview: (DrawerApplicationSize, DrawerApplicationSize) -> Unit,
    onCommitIconSize: (DrawerApplicationSize) -> Unit,
    onCommitTextSize: (DrawerApplicationSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = DrawerApplicationSize.values()
    val labels = options.map { option ->
        stringResource(
            id = when (option) {
                DrawerApplicationSize.Large -> R.string.favorite_list_large
                DrawerApplicationSize.Medium -> R.string.favorite_list_medium
                DrawerApplicationSize.Small -> R.string.favorite_list_small
            },
        )
    }
    val iconSelectedIndex = options.indexOf(element = iconSize)
    val textSelectedIndex = options.indexOf(element = textSize)
    var iconPreviewIndex by remember(iconSelectedIndex) { mutableIntStateOf(iconSelectedIndex) }
    var textPreviewIndex by remember(textSelectedIndex) { mutableIntStateOf(textSelectedIndex) }

    Column(modifier = modifier.fillMaxWidth()) {
        StyleSettingsTitleLine(text = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    dimensionResource(R.dimen.style_settings_application_size_line_height),
                )
                .padding(
                    horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset),
                ),
        ) {
            StyleTaperingSlider(
                selectedStop = iconPreviewIndex,
                stopLabels = labels,
                label = stringResource(R.string.style_settings_application_icon_size),
                increaseLabel = stringResource(R.string.style_settings_increase_application_icon_size),
                decreaseLabel = stringResource(R.string.style_settings_decrease_application_icon_size),
                onSelectStop = {
                    iconPreviewIndex = it
                    onPreview(options[it], options[textPreviewIndex])
                },
                onSelectStopFinished = {
                    iconPreviewIndex = it
                    onCommitIconSize(options[it])
                },
                enabled = iconEnabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_application_size_icon_slider"),
            )
            Spacer(
                modifier = Modifier.width(
                    dimensionResource(R.dimen.style_settings_tapering_slider_gap),
                ),
            )
            StyleTaperingSlider(
                selectedStop = textPreviewIndex,
                stopLabels = labels,
                label = stringResource(R.string.style_settings_application_text_size),
                increaseLabel = stringResource(R.string.style_settings_increase_application_text_size),
                decreaseLabel = stringResource(R.string.style_settings_decrease_application_text_size),
                onSelectStop = {
                    textPreviewIndex = it
                    onPreview(options[iconPreviewIndex], options[it])
                },
                onSelectStopFinished = {
                    textPreviewIndex = it
                    onCommitTextSize(options[it])
                },
                enabled = textEnabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_application_size_text_slider"),
            )
        }
    }
}

@Composable
private fun StyleSettingsTitleLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.style_settings_title_line_height))
        .padding(horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
