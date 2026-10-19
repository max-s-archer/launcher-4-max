package com.maxarchm.launcher.ui.style

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt
import com.maxarchm.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarchm.launcher.R

@Composable
internal fun StyleApplicationSizeBlock(
    title: String,
    optionLabels: List<String>,
    optionIconSizes: List<Dp>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelectIndex: (Int) -> Unit,
    optionTestTagPrefix: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    require(optionLabels.size == optionIconSizes.size)
    require(selectedIndex in optionLabels.indices)
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        StyleTitleLine(text = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    height = dimensionResource(
                        id = R.dimen.style_settings_application_size_line_height,
                    ),
                )
                .padding(
                    horizontal = dimensionResource(id = R.dimen.style_settings_panel_row_inset),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(weight = 1f)
                    .selectableGroup()
                    .semantics { contentDescription = title }
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                optionLabels.indices.forEach { index ->
                    val iconSize = optionIconSizes[index]
                    val iconPixels = with(LocalDensity.current) { iconSize.roundToPx() }
                    val icon = remember(key1 = index, key2 = iconPixels) {
                        context.packageManager.defaultActivityIcon
                            .toBitmap(width = iconPixels, height = iconPixels)
                            .asImageBitmap()
                    }
                    Row(
                        modifier = Modifier
                            .height(
                                height = dimensionResource(
                                    id = R.dimen.style_settings_application_size_line_height,
                                ),
                            )
                            .selectable(
                                selected = index == selectedIndex,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { if (index != selectedIndex) onSelectIndex(index) },
                            )
                            .padding(
                                end = dimensionResource(
                                    id = R.dimen.style_settings_size_option_end_padding,
                                ),
                            )
                            .then(
                                optionTestTagPrefix?.let { prefix ->
                                    Modifier.testTag(tag = "${prefix}_$index")
                                } ?: Modifier,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null,
                            modifier = Modifier.size(
                                size = dimensionResource(id = R.dimen.style_settings_indicator_size),
                            ).clearAndSetSemantics { },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.onSurface,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface,
                                disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Spacer(
                            modifier = Modifier.width(
                                width = dimensionResource(
                                    id = R.dimen.style_settings_indicator_icon_gap,
                                ),
                            ),
                        )
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(size = iconSize),
                        )
                        Spacer(
                            modifier = Modifier.width(
                                width = dimensionResource(id = R.dimen.style_settings_icon_label_gap),
                            ),
                        )
                        Text(
                            text = optionLabels[index],
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StyleTitleLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.style_settings_title_line_height))
            .padding(horizontal = dimensionResource(id = R.dimen.style_settings_panel_row_inset)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
internal fun StyleArrangementBlock(
    title: String,
    optionLabels: List<String>,
    selectedIndex: Int,
    value: Int,
    minimum: Int,
    maximum: Int,
    decrementLabel: String,
    incrementLabel: String,
    enabled: Boolean,
    onSelectIndex: (Int) -> Unit,
    onChangeValue: (Int) -> Unit,
    testTagPrefix: String,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    require(optionLabels.size in 2..3)
    require(selectedIndex in optionLabels.indices)
    require(value in minimum..maximum)
    val selectorWidth = dimensionResource(
        id = if (optionLabels.size == 2) {
            R.dimen.style_settings_selector_width
        } else {
            R.dimen.style_settings_three_option_selector_width
        },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag = "${testTagPrefix}_application_arrangement"),
    ) {
        StyleTitleLine(text = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = dimensionResource(id = R.dimen.style_settings_content_line_height))
                .padding(
                    horizontal = dimensionResource(id = R.dimen.style_settings_panel_row_inset),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(weight = 1f)
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(width = selectorWidth)) {
                    StyleTwoOptionSelector(
                        title = title,
                        optionLabels = optionLabels,
                        selectedIndex = selectedIndex,
                        selectorWidth = selectorWidth,
                        enabled = enabled,
                        onSelectIndex = onSelectIndex,
                        testTagPrefix = "${testTagPrefix}_name_placement",
                    )
                }
            }
            Spacer(
                modifier = Modifier.width(
                    width = dimensionResource(id = R.dimen.style_settings_control_gap),
                ),
            )
            StyleItemsPerRowStepper(
                value = value,
                minimum = minimum,
                maximum = maximum,
                decrementLabel = decrementLabel,
                incrementLabel = incrementLabel,
                enabled = enabled,
                onChangeValue = onChangeValue,
                testTagPrefix = "${testTagPrefix}_items_per_row",
            )
        }
    }
}

@Composable
internal fun StyleSelectorBlock(
    title: String,
    optionLabels: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String,
) {
    require(optionLabels.size == 2)
    require(selectedIndex in optionLabels.indices)
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        StyleTitleLine(text = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.style_settings_content_line_height))
                .padding(horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleTwoOptionSelector(
                    title = title,
                    optionLabels = optionLabels,
                    selectedIndex = selectedIndex,
                    selectorWidth = dimensionResource(id = R.dimen.style_settings_selector_width),
                    enabled = enabled,
                    onSelectIndex = onSelectIndex,
                    testTagPrefix = testTagPrefix,
                )
            }
        }
    }
}

@Composable
internal fun StyleTwoOptionSelector(
    title: String,
    optionLabels: List<String>,
    selectedIndex: Int,
    selectorWidth: Dp,
    enabled: Boolean,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String,
    optionEnabled: List<Boolean> = List(optionLabels.size) { true },
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
                        alpha = integerResource(R.integer.style_settings_selector_border_alpha_percent) / 100f,
                    ),
                    shape = frameShape,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .selectableGroup()
                .semantics { contentDescription = title }
                .padding(horizontal = dimensionResource(R.dimen.style_settings_selector_inner_padding)),
        ) {
            optionLabels.forEachIndexed { index, label ->
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
                            .height(dimensionResource(R.dimen.style_settings_selector_thumb_height))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.style_settings_selector_thumb_radius)))
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
private fun StyleItemsPerRowStepper(
    value: Int,
    minimum: Int,
    maximum: Int,
    decrementLabel: String,
    incrementLabel: String,
    enabled: Boolean,
    onChangeValue: (Int) -> Unit,
    testTagPrefix: String,
) {
    val valueLabel = stringResource(R.string.style_settings_items_per_row)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StyleStepperControl(
            text = decrementLabel,
            actionLabel = stringResource(R.string.style_settings_decrease_items_per_row),
            enabled = enabled && value > minimum,
            available = value > minimum,
            testTag = "${testTagPrefix}_decrement",
            onClick = { onChangeValue(value - 1) },
        )
        Box(
            modifier = Modifier
                .testTag(tag = "${testTagPrefix}_value")
                .size(size = dimensionResource(id = R.dimen.style_settings_stepper_target_size))
                .clearAndSetSemantics {
                    contentDescription = valueLabel
                    stateDescription = value.toString()
                    if (!enabled) disabled()
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = dimensionResource(id = R.dimen.style_settings_stepper_value_width),
                        height = dimensionResource(
                            id = R.dimen.style_settings_stepper_visible_height,
                        ),
                    )
                    .clip(
                        shape = RoundedCornerShape(
                            size = dimensionResource(id = R.dimen.style_settings_stepper_radius),
                        ),
                    )
                    .background(color = colorResource(id = R.color.style_settings_control_surface)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        StyleStepperControl(
            text = incrementLabel,
            actionLabel = stringResource(R.string.style_settings_increase_items_per_row),
            enabled = enabled && value < maximum,
            available = value < maximum,
            testTag = "${testTagPrefix}_increment",
            onClick = { onChangeValue(value + 1) },
        )
    }
}

@Composable
private fun StyleStepperControl(
    text: String,
    actionLabel: String,
    enabled: Boolean,
    available: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size = dimensionResource(id = R.dimen.style_settings_stepper_target_size))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.style_settings_stepper_radius)))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = actionLabel,
                onClick = onClick,
            )
            .semantics { contentDescription = actionLabel }
            .alpha(alpha = styleSettingsContentAlpha(available))
            .testTag(tag = testTag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size = dimensionResource(id = R.dimen.style_settings_stepper_visible_size))
                .clip(
                    shape = RoundedCornerShape(
                        size = dimensionResource(id = R.dimen.style_settings_stepper_radius),
                    ),
                )
                .background(color = colorResource(id = R.color.style_settings_control_surface)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun styleSettingsContentAlpha(enabled: Boolean): Float =
    if (enabled) 1f else integerResource(R.integer.disabled_content_alpha_percent) / 100f

/**
 * Shared surface and inward edge for style panels, icon-shape settings, and the Quick
 * action popup. Hosts keep placement, sizing, scrolling, and insets; the popup supplies
 * its own corner radius.
 */
@Composable
internal fun Modifier.styleSettingsPanelSurface(): Modifier {
    val panelCornerRadius = dimensionResource(R.dimen.style_settings_panel_corner_radius)
    return styleSettingsPanelSurface(cornerRadius = panelCornerRadius)
}

@Composable
internal fun Modifier.styleSettingsPanelSurface(cornerRadius: Dp): Modifier {
    val panelCornerRadius = cornerRadius
    val panelShape = RoundedCornerShape(size = panelCornerRadius)
    val surface = colorResource(R.color.launcher4max_sheet_surface)
    val border = colorResource(R.color.style_settings_panel_border)
    val borderWidth = dimensionResource(R.dimen.style_settings_panel_border_width)
    val transitionWidth = dimensionResource(R.dimen.style_settings_panel_inner_transition_width)
    return this
        .clip(shape = panelShape)
        .background(color = surface)
        .drawWithCache {
            val edge = borderWidth.toPx()
            val transition = transitionWidth.toPx()
            val radius = panelCornerRadius.toPx()
            onDrawBehind {
                // Nested opaque fills stay inside the border and decay quickly to the surface.
                // Quarter-pixel steps avoid visible rings at higher display densities.
                val steps = (transition * 4).roundToInt().coerceAtLeast(1)
                for (step in 0..steps) {
                    val progress = step.toFloat() / steps
                    val remaining = 1f - progress
                    val edgeWeight = remaining * remaining * remaining * remaining
                    val inset = edge + transition * progress
                    val width = size.width - inset * 2
                    val height = size.height - inset * 2
                    if (width <= 0f || height <= 0f) break
                    drawRoundRect(
                        color = lerp(surface, border, edgeWeight),
                        topLeft = Offset(inset, inset),
                        size = Size(width, height),
                        cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
                    )
                }
            }
        }
        .border(
            width = borderWidth,
            color = border,
            shape = panelShape,
        )
}

/**
 * The background block: one percentage readout and one platform-standard continuous
 * progress slider on a shared `48dp` content row. The readout is start-aligned inside a
 * fixed-width reservation measured at the complete widest value `100%`, is read-only,
 * and exposes no independent focus target. The active track and thumb use
 * `primaryTextColor`; the inactive track uses `secondaryTextColor`.
 */
@Composable
internal fun StyleBackgroundOpacityBlock(
    title: String,
    opacity: Int,
    enabled: Boolean,
    onOpacityChange: (Int) -> Unit,
    onOpacityChangeFinished: (Int) -> Unit,
    sliderTestTag: String,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    available: Boolean = true,
    onOpacityChangeCancelled: (() -> Unit)? = null,
) {
    require(opacity in DrawerDisplaySettings.BACKGROUND_OPACITY_RANGE) {
        "Invalid Drawer background opacity"
    }
    val view = LocalView.current
    val readoutFormat = stringResource(R.string.drawer_background_opacity_readout)
    val readoutStyle = LocalTextStyle.current.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
        lineHeight = dimensionResource(R.dimen.style_settings_secondary_line_height).value.sp,
    )
    val textMeasurer = rememberTextMeasurer()
    val readoutReservationWidth = with(LocalDensity.current) {
        textMeasurer
            .measure(
                text = AnnotatedString(
                    readoutFormat.format(DrawerDisplaySettings.BACKGROUND_OPACITY_RANGE.last),
                ),
                style = readoutStyle,
            )
            .size
            .width
            .toDp()
    }
    var lastTickedOpacity by remember { mutableIntStateOf(value = opacity) }
    var latestOpacity by remember { mutableIntStateOf(value = opacity) }
    var pointerActive by remember { mutableStateOf(false) }
    var pointerCancelled by remember { mutableStateOf(false) }
    val currentOpacity by rememberUpdatedState(opacity)
    val currentOnCancel by rememberUpdatedState(onOpacityChangeCancelled)
    val opacityActive = remember { booleanArrayOf(true) }
    DisposableEffect(Unit) {
        onDispose { opacityActive[0] = false }
    }
    Column(modifier = modifier.fillMaxWidth().alpha(styleSettingsContentAlpha(available))) {
        StyleTitleLine(text = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = dimensionResource(R.dimen.style_settings_content_line_height))
                .padding(
                    horizontal = dimensionResource(R.dimen.style_settings_panel_row_inset),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = readoutFormat.format(opacity),
                style = readoutStyle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .width(width = readoutReservationWidth)
                    .clearAndSetSemantics { },
            )
            Spacer(
                modifier = Modifier.width(
                    width = dimensionResource(R.dimen.style_settings_readout_slider_gap),
                ),
            )
            Slider(
                value = opacity.toFloat(),
                onValueChange = { value ->
                    // A later accessibility change has no pointer-down event.
                    if (!pointerActive) pointerCancelled = false
                    // The slider exposes the contracted 0..100 percentage range directly.
                    val rounded = value.roundToInt()
                    latestOpacity = rounded
                    if (rounded != opacity) {
                        onOpacityChange(rounded)
                        if (rounded != lastTickedOpacity) {
                            // One short tick per whole-percentage crossing during the drag.
                            lastTickedOpacity = rounded
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                },
                onValueChangeFinished = {
                    if (!opacityActive[0] || pointerCancelled) return@Slider
                    val released = latestOpacity
                    lastTickedOpacity = released
                    onOpacityChangeFinished(released)
                },
                valueRange = 0f..DrawerDisplaySettings.BACKGROUND_OPACITY_RANGE.last.toFloat(),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onBackground,
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // A temporary operation lock keeps this slider's colors: the disabled
                    // palette mirrors the enabled one, so `enabled = false` only rejects
                    // input and exposes disabled semantics without greying out the control.
                    disabledActiveTrackColor = MaterialTheme.colorScheme.onBackground,
                    disabledThumbColor = MaterialTheme.colorScheme.onBackground,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxHeight()
                    .then(
                        if (onOpacityChangeCancelled == null) {
                            Modifier
                        } else {
                            Modifier.pointerInput(enabled) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial,
                                    )
                                    latestOpacity = currentOpacity
                                    pointerActive = true
                                    pointerCancelled = true
                                    try {
                                        do {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val pointer = event.changes.firstOrNull {
                                                it.id == down.id
                                            }
                                            // ACTION_CANCEL is a consumed synthetic up. Observe
                                            // Initial before Slider consumes a real release.
                                            if (pointer?.changedToUp() == true) {
                                                pointerCancelled = false
                                            }
                                        } while (event.changes.any { it.pressed })
                                    } finally {
                                        pointerActive = false
                                        if (pointerCancelled) currentOnCancel?.invoke()
                                    }
                                }
                            }
                        },
                    )
                    .testTag(tag = sliderTestTag),
            )
        }
    }
}
