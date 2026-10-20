package com.maxarchm.launcher.ui.home.components

import android.provider.Settings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.maxarchm.launcher.ApplicationOrderChange
import com.maxarchm.launcher.FavoriteAvailability
import com.maxarchm.launcher.HOME_BOTTOM_APP_BAR_DESTINATION_ID
import com.maxarchm.launcher.HomeApplicationMovement
import com.maxarchm.launcher.HomeBottomAppBarPresentation
import com.maxarchm.launcher.HomeBottomAppBarState
import com.maxarchm.launcher.LaunchableEntry
import com.maxarchm.launcher.LaunchableIdentity
import com.maxarchm.launcher.OrderedFavoriteModule
import com.maxarchm.launcher.OrderedFavoriteModuleType
import com.maxarchm.launcher.R
import com.maxarchm.launcher.homeBottomAppBarCapacity
import com.maxarchm.launcher.iconSizeResource
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class HomeDockDragPreview(
    val identity: LaunchableIdentity,
    val availability: FavoriteAvailability?,
    val iconSize: Dp,
    val originInWindow: Offset,
    val size: IntSize,
    val translation: Offset,
)

private fun barDragPreview(
    identities: List<LaunchableIdentity>,
    index: Int,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    iconSize: Dp,
    origin: Offset,
    size: IntSize,
    translation: Offset,
): HomeDockDragPreview {
    val identity = identities[index]
    return HomeDockDragPreview(
        identity = identity,
        availability = availabilityByIdentity[identity],
        iconSize = iconSize,
        originInWindow = origin,
        size = size,
        translation = translation,
    )
}

@Composable
internal fun HomeDockSurface(
    state: HomeBottomAppBarState,
    safeWidthDp: Float,
    expanded: Boolean,
    editMode: Boolean,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    mutationEnabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLaunch: (FavoriteAvailability) -> Unit,
    onLongPress: (LaunchableEntry) -> Unit,
    onRemove: (LaunchableIdentity) -> Unit,
    onAdd: () -> Unit,
    onReorder: (List<LaunchableIdentity>) -> Unit,
    onDragPreviewChange: (HomeDockDragPreview?) -> Unit,
    onCommitApplicationOrder: (ApplicationOrderChange) -> Unit = {},
    movement: HomeApplicationMovement? = null,
    modifier: Modifier = Modifier,
) {
    val iconDimension = dimensionResource(state.iconSize.iconSizeResource())
    val verticalPadding = dimensionResource(R.dimen.home_bottom_app_bar_preview_vertical_padding)
    val fullHeight = iconDimension + verticalPadding * 2
    val iconGap = dimensionResource(R.dimen.home_bottom_app_bar_preview_icon_gap)
    val horizontalInset = dimensionResource(R.dimen.home_bottom_app_bar_preview_horizontal_inset)
    val outerMargin = dimensionResource(R.dimen.home_bottom_app_bar_outer_margin)
    val backgroundAlpha = integerResource(
        R.integer.home_bottom_app_bar_background_alpha_percent,
    ) / 100f
    val semiHidden = state.presentation == HomeBottomAppBarPresentation.SemiHidden
    val presentedExpanded = editMode || !semiHidden || expanded
    val duration = if (
        Settings.Global.getFloat(
            LocalContext.current.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    ) {
        0
    } else {
        integerResource(R.integer.short_property_animation_duration_ms)
    }
    val collapsedExposure = dimensionResource(R.dimen.home_bottom_app_bar_collapsed_exposure)
    val visibleHeight = fullHeight
    val collapsedOffset by animateDpAsState(
        targetValue = if (presentedExpanded) 0.dp else fullHeight - collapsedExposure,
        animationSpec = tween(durationMillis = duration),
        label = "home_bottom_app_bar_offset",
    )
    val collapsedAlpha = integerResource(
        R.integer.home_bottom_app_bar_semi_hidden_alpha_percent,
    ) / 100f
    val opacity by animateFloatAsState(
        targetValue = if (presentedExpanded) 1f else collapsedAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "home_bottom_app_bar_opacity",
    )
    val identities = state.identities
    var draggingIndex by remember(identities) { mutableIntStateOf(-1) }
    var insertionIndex by remember(identities) { mutableIntStateOf(-1) }
    var barCoordinates by remember(identities) { mutableStateOf<LayoutCoordinates?>(null) }
    val iconBounds = remember(identities) { mutableStateMapOf<Int, LayoutCoordinates>() }
    val capacity = homeBottomAppBarCapacity(
        safeWidthDp = safeWidthDp,
        iconSizeDp = iconDimension.value,
        horizontalInsetDp = horizontalInset.value,
        iconGapDp = iconGap.value,
    )
    var dockBounds by remember(identities) { mutableStateOf<Rect?>(null) }
    var addBounds by remember(identities) { mutableStateOf<Rect?>(null) }
    LaunchedEffect(movement, dockBounds, identities, capacity, addBounds, presentedExpanded) {
        movement?.updateHomeBottomAppBar(
            bounds = dockBounds?.takeIf { presentedExpanded },
            identities = identities,
            capacity = capacity,
            addBounds = addBounds,
        )
    }
    val barShape = RoundedCornerShape(
        dimensionResource(R.dimen.home_bottom_app_bar_corner_radius),
    )
    Box(
        modifier = modifier
            .padding(start = outerMargin, end = outerMargin, bottom = outerMargin)
            .fillMaxWidth()
            .height(visibleHeight)
            .graphicsLayer {
                translationY = collapsedOffset.toPx()
                clip = true
            }
            .clip(barShape)
            .clipToBounds()
            .alpha(opacity)
            .background(
                colorResource(R.color.home_bottom_app_bar_preview_background).copy(
                    alpha = backgroundAlpha,
                ),
            )
            .border(
                width = dimensionResource(R.dimen.home_favorite_bar_border_width),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = integerResource(
                        R.integer.home_favorite_bar_border_alpha_percent,
                    ) / 100f,
                ),
                shape = barShape,
            )
            .onGloballyPositioned { coordinates ->
                barCoordinates = coordinates
                dockBounds = Rect(
                    offset = coordinates.positionInWindow(),
                    size = androidx.compose.ui.geometry.Size(
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat(),
                    ),
                )
            }
            .pointerInput(
                identities,
                editMode,
                mutationEnabled,
                presentedExpanded,
                availabilityByIdentity,
                iconDimension,
                movement,
                onCommitApplicationOrder,
            ) {
                if (
                    !editMode || !mutationEnabled || !presentedExpanded ||
                    movement?.isDragging == true
                ) {
                    return@pointerInput
                }
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val bar = barCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                    val startIndex = dockIconAt(iconBounds, bar, down.position)
                        ?: return@awaitEachGesture
                    val startCoordinates = iconBounds[startIndex] ?: return@awaitEachGesture
                    val origin = startCoordinates.positionInWindow()
                    val size = startCoordinates.size
                    var moved = false
                    var totalX = 0f
                    var totalY = 0f
                    val iconCenter = origin.x + size.width / 2f
                    val pointerStart = bar.localToWindow(down.position).x
                    fun translationFor(pointerTravel: Float): Offset = Offset(
                        x = pointerStart + pointerTravel - iconCenter,
                        y = 0f,
                    )
                    fun ghostCenterX(): Float = pointerStart + totalX
                    fun insertionTarget(): Int {
                        // Visible slot among the icons that stay behind. The ghost center is the
                        // finger; crossing an icon center selects the gap after that icon.
                        val center = ghostCenterX()
                        var slot = 0
                        for (index in identities.indices) {
                            if (index == startIndex) continue
                            val coordinates = iconBounds[index]?.takeIf { it.isAttached }
                                ?: continue
                            val restingCenter = coordinates.positionInWindow().x +
                                coordinates.size.width / 2f
                            if (center < restingCenter) return slot
                            slot += 1
                        }
                        return slot
                    }
                    suspend fun AwaitPointerEventScope.handOff(
                        pointer: Offset,
                        pointerId: PointerId,
                    ): Boolean {
                        val identity = identities.getOrNull(startIndex) ?: return false
                        val availability = availabilityByIdentity[identity] ?: return false
                        if (movement == null || !movement.start(
                                identity = identity,
                                module = OrderedFavoriteModule(
                                    id = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
                                    type = OrderedFavoriteModuleType.Ribbon,
                                    identities = identities,
                                ),
                                availability = availability,
                                pointer = pointer,
                                carriedBounds = Rect(
                                    offset = origin + translationFor(totalX).copy(y = totalY),
                                    size = androidx.compose.ui.geometry.Size(
                                        width = size.width.toFloat(),
                                        height = size.height.toFloat(),
                                    ),
                                ),
                            )
                        ) return false
                        onDragPreviewChange(null)
                        draggingIndex = -1
                        insertionIndex = -1
                        var committed = false
                        try {
                            while (true) {
                                val next = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val owner = next.changes.firstOrNull { it.id == pointerId } ?: break
                                next.changes.forEach { it.consume() }
                                movement.move(pointer = bar.localToWindow(owner.position))
                                if (!owner.pressed) {
                                    movement.finish()?.let { order ->
                                        onCommitApplicationOrder(order)
                                        committed = true
                                    }
                                    break
                                }
                            }
                        } finally {
                            if (!committed) movement.cancel()
                        }
                        return true
                    }
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val horizontal = change.position.x - change.previousPosition.x
                        val vertical = change.position.y - change.previousPosition.y
                        totalX += horizontal
                        totalY += vertical
                        val windowPointer = bar.localToWindow(change.position)
                        if (!moved && (abs(totalX) > slop || abs(totalY) > slop)) {
                            if (abs(totalY) > abs(totalX)) {
                                change.consume()
                                if (handOff(windowPointer, down.id)) return@awaitEachGesture
                            }
                            moved = true
                            change.consume()
                            onDragPreviewChange(
                                barDragPreview(
                                    identities = identities,
                                    index = startIndex,
                                    availabilityByIdentity = availabilityByIdentity,
                                    iconSize = iconDimension,
                                    origin = origin,
                                    size = size,
                                    translation = translationFor(totalX),
                                ),
                            )
                            draggingIndex = startIndex
                            insertionIndex = startIndex
                        }
                        if (moved) {
                            change.consume()
                            val outside = dockBounds?.contains(windowPointer) == false &&
                                abs(totalY) > slop
                            if (outside && handOff(windowPointer, down.id)) return@awaitEachGesture
                            onDragPreviewChange(
                                barDragPreview(
                                    identities = identities,
                                    index = startIndex,
                                    availabilityByIdentity = availabilityByIdentity,
                                    iconSize = iconDimension,
                                    origin = origin,
                                    size = size,
                                    translation = translationFor(totalX).copy(y = totalY),
                                ),
                            )
                            insertionIndex = insertionTarget()
                        }
                        if (!change.pressed) break
                    }
                    val target = if (moved) insertionTarget() else startIndex
                    onDragPreviewChange(null)
                    draggingIndex = -1
                    insertionIndex = -1
                    if (moved && target != startIndex) {
                        val movedIdentity = identities[startIndex]
                        onReorder(
                            identities.toMutableList().apply {
                                removeAt(startIndex)
                                add(target.coerceIn(0, this.size), movedIdentity)
                            },
                        )
                    }
                }
            }
            .then(
                if (!presentedExpanded) {
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onExpandedChange(true) },
                        )
                        .pointerInput(Unit) {
                            val slop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                var expandedByDrag = false
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                    if (!change.pressed) break
                    val vertical = change.position.y - down.position.y
                    if (!expandedByDrag && vertical < -slop) {
                                        expandedByDrag = true
                                        onExpandedChange(true)
                                    }
                                    if (expandedByDrag) change.consume()
                                }
                            }
                        }
                        .testTag("home_bottom_app_bar_collapsed")
                } else {
                    Modifier
                        .pointerInput(semiHidden, editMode) {
                            if (!semiHidden || editMode) return@pointerInput
                            val slop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val bar = barCoordinates?.takeIf { it.isAttached }
                                    ?: return@awaitEachGesture
                                if (dockIconAt(iconBounds, bar, down.position) != null) {
                                    return@awaitEachGesture
                                }
                                var collapsing = false
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    val vertical = change.position.y - down.position.y
                                    if (vertical > slop) collapsing = true
                                    if (collapsing) change.consume()
                                    if (!change.pressed) break
                                }
                                if (collapsing) onExpandedChange(false)
                            }
                        }
                        .testTag("home_bottom_app_bar")
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(fullHeight)
                .padding(horizontal = horizontalInset),
            horizontalArrangement = Arrangement.spacedBy(
                space = iconGap,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val visibleIdentities = if (draggingIndex in identities.indices) {
                identities.filterIndexed { index, _ -> index != draggingIndex }
            } else {
                identities
            }
            val insertionSlot = if (
                draggingIndex in identities.indices &&
                insertionIndex in 0..visibleIdentities.size
            ) {
                insertionIndex
            } else {
                -1
            }
            visibleIdentities.forEachIndexed { visibleIndex, identity ->
                val showInsertionBefore = insertionSlot == visibleIndex
                val showInsertionAfter = insertionSlot == visibleIdentities.size &&
                    visibleIndex == visibleIdentities.lastIndex
                HomeDockIcon(
                    identity = identity,
                    availability = availabilityByIdentity[identity],
                    iconSize = iconDimension,
                    slotHeight = fullHeight,
                    editMode = editMode,
                    movementActive = movement?.activeIdentity != null,
                    interactionEnabled = presentedExpanded && mutationEnabled,
                    onPositioned = { coordinates ->
                        iconBounds[identities.indexOf(identity)] = coordinates
                        movement?.updateItem(
                            identity = identity,
                            bounds = Rect(
                                offset = coordinates.positionInWindow(),
                                size = androidx.compose.ui.geometry.Size(
                                    width = coordinates.size.width.toFloat(),
                                    height = coordinates.size.height.toFloat(),
                                ),
                            ),
                        )
                    },
                    onLaunch = onLaunch,
                    onLongPress = onLongPress,
                    onRemove = { onRemove(identity) },
                    showInsertionBefore = showInsertionBefore,
                    showInsertionAfter = showInsertionAfter,
                )
            }
            if (editMode && identities.size < capacity) {
                val addLabel = stringResource(R.string.home_bottom_app_bar_add)
                Box(
                    modifier = Modifier
                        .width(iconDimension)
                        .height(fullHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = mutationEnabled,
                            role = Role.Button,
                            onClick = onAdd,
                        )
                        .semantics { contentDescription = addLabel }
                        .testTag("home_bottom_app_bar_add")
                        .onGloballyPositioned { coordinates ->
                            addBounds = Rect(
                                offset = coordinates.positionInWindow(),
                                size = androidx.compose.ui.geometry.Size(
                                    width = coordinates.size.width.toFloat(),
                                    height = coordinates.size.height.toFloat(),
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(
                            dimensionResource(R.dimen.home_add_favorite_icon_size),
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeDockIcon(
    identity: LaunchableIdentity,
    availability: FavoriteAvailability?,
    iconSize: Dp,
    slotHeight: Dp,
    editMode: Boolean,
    movementActive: Boolean,
    interactionEnabled: Boolean,
    onPositioned: (LayoutCoordinates) -> Unit,
    onLaunch: (FavoriteAvailability) -> Unit,
    onLongPress: (LaunchableEntry) -> Unit,
    onRemove: () -> Unit,
    showInsertionBefore: Boolean,
    showInsertionAfter: Boolean,
) {
    val entry = availability?.presentationEntry
    val label = entry?.label ?: stringResource(R.string.favorite_application_unavailable)
    val disabledAlpha = integerResource(R.integer.disabled_content_alpha_percent) / 100f
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember(identity) { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .onGloballyPositioned(onPositioned)
            .width(iconSize)
            .height(slotHeight)
            .alpha(
                if (availability is FavoriteAvailability.Available || availability == null) {
                    1f
                } else {
                    disabledAlpha
                },
            )
            .then(if (interactionEnabled && !movementActive) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = { if (!editMode) availability?.let(onLaunch) },
                        onLongClick = {
                            if (!editMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                entry?.let(onLongPress)
                            }
                        },
                    )
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = label }
            .testTag("home_bottom_app_bar_item:${identity.stableKey()}"),
        contentAlignment = Alignment.Center,
    ) {
        if (showInsertionBefore) HomeDockInsertionLine(beforeIcon = true)
        if (showInsertionAfter) HomeDockInsertionLine(beforeIcon = false)
        HomeDockIconArtwork(
            availability = availability,
            iconSize = iconSize,
        )
        if (editMode && interactionEnabled) {
            val removeSize = dimensionResource(R.dimen.home_favorite_bar_remove_target_size)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = 0.dp,
                        y = dimensionResource(R.dimen.home_bottom_app_bar_remove_inset),
                    )
                    .size(removeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onRemove,
                    )
                    .testTag("home_bottom_app_bar_remove:${identity.stableKey()}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.remove_favorite_item),
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.home_favorite_bar_remove_icon_size),
                    ),
                    tint = colorResource(R.color.home_favorite_remove_icon),
                )
            }
        }
    }
}

@Composable
internal fun HomeDockDragGhost(
    preview: HomeDockDragPreview,
    rootOriginInWindow: Offset,
) {
    val density = LocalDensity.current
    val topLeft = preview.originInWindow + preview.translation - rootOriginInWindow
    val elevation = dimensionResource(R.dimen.home_reorder_drag_elevation)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = topLeft.x.roundToInt(),
                    y = topLeft.y.roundToInt(),
                )
            }
            .size(
                width = with(density) { preview.size.width.toDp() },
                height = with(density) { preview.size.height.toDp() },
            )
            .shadow(elevation = elevation, clip = false)
            .clearAndSetSemantics {}
            .testTag("home_bottom_app_bar_drag_preview"),
        contentAlignment = Alignment.Center,
    ) {
        HomeDockIconArtwork(
            availability = preview.availability,
            iconSize = preview.iconSize,
        )
    }
}

@Composable
private fun HomeDockIconArtwork(
    availability: FavoriteAvailability?,
    iconSize: Dp,
) {
    val entry = availability?.presentationEntry
    val bitmap = entry?.iconBitmap?.asImageBitmap()
        ?: entry?.icon?.let { icon ->
            remember(icon) {
                icon.toBitmap(
                    icon.intrinsicWidth.coerceAtLeast(1),
                    icon.intrinsicHeight.coerceAtLeast(1),
                ).asImageBitmap()
            }
        }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_inventory_error),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun dockIconAt(
    iconBounds: Map<Int, LayoutCoordinates>,
    bar: LayoutCoordinates,
    localPosition: Offset,
): Int? = iconBounds.entries.firstOrNull { (_, coordinates) ->
    if (!coordinates.isAttached) return@firstOrNull false
    val local = bar.localPositionOf(coordinates, Offset.Zero)
    Rect(
        offset = local,
        size = androidx.compose.ui.geometry.Size(
            width = coordinates.size.width.toFloat(),
            height = coordinates.size.height.toFloat(),
        ),
    ).contains(localPosition)
}?.key

@Composable
private fun BoxScope.HomeDockInsertionLine(beforeIcon: Boolean) {
    val gap = dimensionResource(R.dimen.home_bottom_app_bar_preview_icon_gap)
    val thickness = dimensionResource(R.dimen.home_favorite_insertion_line_thickness)
    Box(
        modifier = Modifier
            .align(if (beforeIcon) Alignment.CenterStart else Alignment.CenterEnd)
            .offset(x = if (beforeIcon) -(gap + thickness) / 2 else (gap + thickness) / 2)
            .width(thickness)
            .fillMaxHeight()
            .background(colorResource(R.color.home_favorite_insertion_line))
            .testTag("home_bottom_app_bar_insertion_line"),
    )
}
