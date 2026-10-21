package com.maxarcher.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.FavoriteListSize
import com.maxarcher.launcher.IconShapeClipMode
import com.maxarcher.launcher.IconShapeSettings
import com.maxarcher.launcher.R
import com.maxarcher.launcher.iconSizeResource
import com.maxarcher.launcher.lineHeightResource
import com.maxarcher.launcher.platform.icons.FREEFORM_ICON_CLIP_RANGE
import com.maxarcher.launcher.platform.icons.OEM_ICON_CLIP
import com.maxarcher.launcher.platform.icons.clipLauncherIcon
import com.maxarcher.launcher.platform.icons.prepareLauncherIconBitmap
import com.maxarcher.launcher.textSizeResource
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow
import com.maxarcher.launcher.ui.style.StyleBackgroundOpacityBlock
import com.maxarcher.launcher.ui.style.StyleTitleLine
import com.maxarcher.launcher.ui.style.StyleTwoOptionSelector
import com.maxarcher.launcher.ui.style.styleSettingsPanelSurface

@Composable
internal fun IconShapeSettingsContent(
    settings: IconShapeSettings,
    saveInFlight: Boolean,
    saveFailureToken: Int,
    onCommit: (IconShapeSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = dimensionResource(R.dimen.settings_horizontal_padding)
    val previewInset = dimensionResource(R.dimen.icon_shape_preview_window_inset)
    val cornerRadius = dimensionResource(R.dimen.style_settings_panel_corner_radius)
    val clipModeTitle = stringResource(R.string.icon_shape_clip_mode)
    var draftSettings by remember(settings) { mutableStateOf(settings) }
    LaunchedEffect(settings, saveFailureToken) {
        draftSettings = settings
    }
    val oemClip = draftSettings.mode == IconShapeClipMode.Oem
    val previewClip = if (oemClip) OEM_ICON_CLIP else draftSettings.freeformClip
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("icon_shape_settings_page"),
    ) {
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
            // Parent constraints can collapse the window to zero; never overflow this region.
            Box(
                modifier = Modifier
                    .padding(horizontal = previewInset, vertical = previewInset)
                    .fillMaxSize()
                    .clipToBounds()
                    .drawWithContent {
                        // Clear only this window in the enclosing Settings offscreen layer.
                        drawRoundRect(
                            color = Color.Transparent,
                            cornerRadius = CornerRadius(cornerRadius.toPx()),
                            blendMode = BlendMode.Clear,
                        )
                        drawContent()
                    }
                    .testTag("icon_shape_preview_window"),
                contentAlignment = Alignment.Center,
            ) {
                IconShapeDemoList(clip = previewClip)
            }
        }
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing)
                    .styleSettingsPanelSurface(),
            ) {
                StyleTitleLine(text = clipModeTitle)
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
                        title = clipModeTitle,
                        optionLabels = listOf(
                            stringResource(R.string.icon_shape_oem_clip),
                            stringResource(R.string.icon_shape_freeform_clip),
                        ),
                        selectedIndex = if (oemClip) 0 else 1,
                        selectorWidth = dimensionResource(R.dimen.icon_shape_selector_width),
                        enabled = !saveInFlight,
                        onSelectIndex = {
                            val nextSettings = draftSettings.copy(
                                mode = if (it == 0) {
                                    IconShapeClipMode.Oem
                                } else {
                                    IconShapeClipMode.Freeform
                                },
                            )
                            draftSettings = nextSettings
                            onCommit(nextSettings)
                        },
                        testTagPrefix = "icon_shape_clip_mode",
                    )
                }
                StyleBackgroundOpacityBlock(
                    title = stringResource(R.string.icon_shape_corner_radius),
                    opacity = draftSettings.freeformClip,
                    enabled = !oemClip && !saveInFlight,
                    available = !oemClip && !saveInFlight,
                    onOpacityChange = {
                        draftSettings = draftSettings.copy(
                            freeformClip = it.coerceIn(FREEFORM_ICON_CLIP_RANGE),
                        )
                    },
                    onOpacityChangeFinished = { finalClip ->
                        if (!saveInFlight && !oemClip) {
                            val nextSettings = draftSettings.copy(
                                freeformClip = finalClip.coerceIn(FREEFORM_ICON_CLIP_RANGE),
                            )
                            draftSettings = nextSettings
                            onCommit(nextSettings)
                        }
                    },
                    onOpacityChangeCancelled = { draftSettings = settings },
                    sliderTestTag = "icon_shape_corner_radius",
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.icon_shape_settings_bottom_margin)))
    }
}

@Composable
private fun IconShapeDemoList(clip: Int) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val samples = listOf(
        Triple(
            FavoriteListSize.Large,
            R.string.icon_shape_sample_app_large,
            R.string.icon_shape_preview_large,
        ),
        Triple(
            FavoriteListSize.Medium,
            R.string.icon_shape_sample_app_medium,
            R.string.icon_shape_preview_medium,
        ),
        Triple(
            FavoriteListSize.Small,
            R.string.icon_shape_sample_app_small,
            R.string.icon_shape_preview_small,
        ),
    )
    Column(
        modifier = Modifier.fillMaxWidth().testTag("icon_shape_demo_list"),
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.icon_shape_preview_row_gap),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        samples.forEach { (tier, nameResource, accessibilityResource) ->
            val size = dimensionResource(tier.iconSizeResource())
            val accessibilityName = stringResource(accessibilityResource)
            val pixels = with(density) { size.roundToPx() }
            val source = remember(context, pixels) {
                prepareLauncherIconBitmap(
                    context.packageManager.defaultActivityIcon,
                    pixels,
                    context.resources,
                )
            }
            val icon = remember(source, clip) {
                clipLauncherIcon(source, clip).asImageBitmap()
            }
            Row(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = accessibilityName
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(size),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.home_favorite_icon_label_gap)))
                Text(
                    text = stringResource(nameResource),
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimensionResource(tier.textSizeResource()).value.sp,
                    lineHeight = dimensionResource(tier.lineHeightResource()).value.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = drawerForegroundShadow(),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
