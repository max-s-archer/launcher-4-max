package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteNamePlacement
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.OrderedFavoriteModuleType
import com.maxarcher.launcher.R
import com.maxarcher.launcher.iconSizeResource
import com.maxarcher.launcher.lineHeightResource
import com.maxarcher.launcher.textSizeResource
import com.maxarcher.launcher.ui.components.ApplicationItem
import com.maxarcher.launcher.ui.components.ApplicationItemNamePlacement
import com.maxarcher.launcher.ui.components.ApplicationItemPadding
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow
import kotlin.math.roundToInt

@Composable
internal fun HomeApplicationMovementOverlay(
    movement: HomeApplicationMovement,
    rootOrigin: Offset,
) {
    // The Drawer drag-to-favorite journey drives this overlay without a Home-side
    // previewSource: the feedback canvases below render from the session alone, and
    // only the frozen preview Box requires a captured source.
    val source = movement.previewSource
    val density = LocalDensity.current
    val lineColor = MaterialTheme.colorScheme.onBackground
    val lineWidth = dimensionResource(id = R.dimen.home_favorite_insertion_line_thickness)
    val edgeColor = lineColor.copy(alpha = integerResource(id = R.integer.home_favorite_edge_feedback_alpha_percent) / 100f)
    // Membership changes need composition (including test/semantics tags); moving geometry only
    // needs placement or drawing. Never subscribe the preview's icon/text to every pointer tick.
    val showEdge by remember(key1 = movement, calculation = {
        derivedStateOf(policy = structuralEqualityPolicy(), calculation = { movement.edgeFeedback != null })
    })
    val showCreation by remember(key1 = movement, calculation = {
        derivedStateOf(policy = structuralEqualityPolicy(), calculation = { movement.session?.creation != null })
    })
    val showInsertion by remember(key1 = movement, calculation = {
        derivedStateOf(policy = structuralEqualityPolicy(), calculation = {
            movement.session?.insertion != null || movement.session?.barInsertion != null
        })
    })
    val previewOrigin = remember(key1 = movement, calculation = {
        derivedStateOf(policy = structuralEqualityPolicy(), calculation = { movement.session?.previewOrigin })
    })
    if (showEdge) {
        Canvas(
            modifier = Modifier.fillMaxSize().testTag(tag = "home_application_edge_feedback"),
            onDraw = {
                val edge = movement.edgeFeedback ?: return@Canvas
                val bounds = edge.bounds.translate(offset = -rootOrigin)
                val startSide = edge.owner.direction < 0
                val colors = if (startSide) listOf(edgeColor, edgeColor.copy(alpha = 0f)) else listOf(edgeColor.copy(alpha = 0f), edgeColor)
                if (edge.horizontal) {
                    val left = if (startSide) bounds.left else bounds.right - edge.band
                    drawRect(
                        brush = Brush.horizontalGradient(colors = colors, startX = left, endX = left + edge.band),
                        topLeft = Offset(x = left, y = bounds.top),
                        size = Size(width = edge.band, height = bounds.height),
                    )
                } else {
                    val top = if (startSide) bounds.top else bounds.bottom - edge.band
                    drawRect(
                        brush = Brush.verticalGradient(colors = colors, startY = top, endY = top + edge.band),
                        topLeft = Offset(x = bounds.left, y = top),
                        size = Size(width = bounds.width, height = edge.band),
                    )
                }
            },
        )
    }
    if (showCreation) {
        val outlineWidth = dimensionResource(id = R.dimen.home_module_selection_stroke)
        val outlineRadius = dimensionResource(id = R.dimen.home_module_selection_radius)
        Canvas(
            modifier = Modifier.fillMaxSize().testTag(tag = "home_module_creation_drop_outline"),
            onDraw = {
                val targetBounds = movement.session?.creation?.second ?: return@Canvas
                val bounds = targetBounds.translate(offset = -rootOrigin)
                val viewport = movement.viewport.translate(offset = -rootOrigin)
                val stroke = outlineWidth.toPx()
                val radius = (outlineRadius.toPx() - stroke / 2f).coerceAtLeast(minimumValue = 0f)
                clipRect(
                    left = viewport.left, top = viewport.top, right = viewport.right, bottom = viewport.bottom,
                    block = {
                        drawRoundRect(
                            color = lineColor,
                            topLeft = bounds.topLeft + Offset(x = stroke / 2f, y = stroke / 2f),
                            size = Size(width = bounds.width - stroke, height = bounds.height - stroke),
                            cornerRadius = CornerRadius(x = radius, y = radius),
                            style = Stroke(width = stroke),
                        )
                    },
                )
            },
        )
    }
    if (showInsertion) {
        Canvas(
            modifier = Modifier.fillMaxSize().testTag(tag = "home_application_insertion_line"),
            onDraw = {
                val insertion = movement.session?.barInsertion
                    ?: movement.session?.insertion
                    ?: return@Canvas
                val clip = insertion.clipBounds.translate(offset = -rootOrigin)
                clipRect(
                    left = clip.left, top = clip.top, right = clip.right, bottom = clip.bottom,
                    block = {
                        drawLine(
                            color = lineColor,
                            start = insertion.lineStart - rootOrigin,
                            end = insertion.lineEnd - rootOrigin,
                            strokeWidth = lineWidth.toPx(),
                        )
                    },
                )
            },
        )
    }
    if (source != null) {
        val width = with(receiver = density, block = { source.sourceBounds.width.toDp() })
        val height = with(receiver = density, block = { source.sourceBounds.height.toDp() })
        val ribbon = source.module.type == OrderedFavoriteModuleType.Ribbon
        Box(
            modifier = Modifier
                .offset(
                    offset = {
                        val origin = (previewOrigin.value ?: source.previewOrigin) - rootOrigin
                        IntOffset(x = origin.x.roundToInt(), y = origin.y.roundToInt())
                    },
                )
                .size(width = width, height = height)
                .testTag(tag = "home_application_movement_preview"),
            content = {
                val nameHidden = !ribbon &&
                    source.module.namePlacement == FavoriteNamePlacement.Hidden
                val nameBelow = !ribbon &&
                    source.module.namePlacement == FavoriteNamePlacement.Below
                val entry = source.availability.presentationEntry
                val displayText = favoriteAvailabilityDisplayText(source.availability)
                ApplicationItem(
                    icon = entry?.icon,
                    iconBitmap = entry?.iconBitmap,
                    iconSize = dimensionResource(source.module.iconSize.iconSizeResource()),
                    name = AnnotatedString(displayText),
                    namePlacement = when {
                        ribbon -> ApplicationItemNamePlacement.Right
                        nameHidden -> ApplicationItemNamePlacement.Hidden
                        nameBelow -> ApplicationItemNamePlacement.Below
                        else -> ApplicationItemNamePlacement.Right
                    },
                    nameStyle = LocalTextStyle.current.copy(
                        fontSize = dimensionResource(
                            source.module.textSize.textSizeResource(),
                        ).value.sp,
                        lineHeight = dimensionResource(
                            source.module.textSize.lineHeightResource(),
                        ).value.sp,
                        shadow = drawerForegroundShadow(),
                    ),
                    iconLabelGap = dimensionResource(
                        when {
                            ribbon -> R.dimen.home_favorite_bar_icon_label_gap
                            nameBelow -> R.dimen.home_favorite_below_icon_label_gap
                            else -> R.dimen.home_favorite_icon_label_gap
                        },
                    ),
                    contentPadding = when {
                        ribbon -> ApplicationItemPadding(
                            start = dimensionResource(R.dimen.home_favorite_bar_item_inset),
                            end = dimensionResource(R.dimen.home_favorite_bar_item_inset),
                        )
                        nameBelow -> ApplicationItemPadding(
                            start = dimensionResource(
                                R.dimen.home_favorite_below_horizontal_inset,
                            ),
                            top = dimensionResource(R.dimen.home_favorite_below_top_inset),
                            end = dimensionResource(
                                R.dimen.home_favorite_below_horizontal_inset,
                            ),
                            bottom = dimensionResource(
                                R.dimen.home_favorite_below_bottom_inset,
                            ),
                        )
                        nameHidden -> ApplicationItemPadding(
                            top = dimensionResource(R.dimen.home_favorite_below_top_inset),
                            bottom = dimensionResource(
                                R.dimen.home_favorite_below_bottom_inset,
                            ),
                        )
                        else -> ApplicationItemPadding(
                            start = dimensionResource(
                                R.dimen.home_favorite_list_icon_start_margin,
                            ),
                        )
                    },
                    nameMaxWidth = if (ribbon) {
                        dimensionResource(R.dimen.home_favorite_bar_label_max_width)
                    } else {
                        androidx.compose.ui.unit.Dp.Unspecified
                    },
                    fillNameWidth = !ribbon,
                    hiddenNameDescription = displayText.takeIf { nameHidden },
                    interactionEnabled = false,
                    contentAlpha = if (
                        source.availability is FavoriteAvailability.Available
                    ) {
                        1f
                    } else {
                        integerResource(R.integer.disabled_content_alpha_percent) / 100f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {},
                )
            },
        )
    }
}
