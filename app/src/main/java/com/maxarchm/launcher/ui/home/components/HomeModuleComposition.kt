package com.maxarchm.launcher.ui.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maxarchm.launcher.ApplicationOrderChange
import com.maxarchm.launcher.FavoriteAvailability
import com.maxarchm.launcher.FavoriteContainer
import com.maxarchm.launcher.HomeApplicationMovement
import com.maxarchm.launcher.HomeOrderedModuleContent
import com.maxarchm.launcher.LaunchableEntry
import com.maxarchm.launcher.LaunchableIdentity
import com.maxarchm.launcher.ModuleDragSession
import com.maxarchm.launcher.OrderedFavoriteModule
import com.maxarchm.launcher.OrderedFavoriteModuleType
import com.maxarchm.launcher.R
import com.maxarchm.launcher.rowHeightResource
import com.maxarchm.launcher.drawCornerMark
import com.maxarchm.launcher.ui.home.components.HomeApplicationAutoScroll
import com.maxarchm.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarchm.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarchm.launcher.ui.home.components.HomeMainListAddFavoriteEntry
import com.maxarchm.launcher.ui.home.components.detectHomeApplicationMovement
import com.maxarchm.launcher.ui.home.components.homeFavoriteEnter
import com.maxarchm.launcher.ui.drawer.drawerForegroundShadow
import kotlin.math.roundToInt

@Composable
internal fun HomeEditDock(
    hasFavorites: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.home_edit_dock_height))
            .testTag("home_edit_dock"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                when {
                    !hasFavorites -> R.string.home_edit_add_favorites
                    expanded -> R.string.home_edit_select_and_move_modules
                    else -> R.string.home_edit_move_applications
                },
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.home_edit_dock_text_padding)),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
            lineHeight = dimensionResource(
                R.dimen.style_settings_secondary_line_height,
            ).value.sp,
            style = LocalTextStyle.current.copy(
                shadow = drawerForegroundShadow(),
            ),
        )
        Box(
            modifier = Modifier
                .size(
                    width = dimensionResource(R.dimen.home_edit_dock_affordance_width),
                    height = dimensionResource(R.dimen.home_edit_dock_height),
                )
                .clickable(role = Role.Button, onClick = onToggleExpanded)
                .testTag(if (expanded) "home_style_panel_collapse" else "home_style_panel_expand"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(
                    if (expanded) {
                        R.string.home_collapse_style_panel
                    } else {
                        R.string.home_expand_style_panel
                    },
                ),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.home_edit_dock_icon_size))
                    .rotate(if (expanded) 90f else -90f),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable

internal fun HomeOrderedModuleComposition(
    modules: List<OrderedFavoriteModule>,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection?,
    editMode: Boolean,
    selectionEnabled: Boolean,
    selectionInteractionEnabled: Boolean,
    selectionVisualEnabled: Boolean = selectionInteractionEnabled,
    selectedModuleId: String?,
    onSelectModule: (String) -> Unit,
    addEntriesEnabled: Boolean,
    onAddToModule: (OrderedFavoriteModule) -> Unit,
    onCreateVerticalModule: () -> Unit,
    onCreateRibbon: () -> Unit,
    onLaunchFavorite: (FavoriteAvailability) -> Unit,
    onLongPressFavorite: (LaunchableEntry) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onRemoveFavorite: (LaunchableIdentity) -> Unit = {},
    applicationMovement: HomeApplicationMovement? = null,
    onCommitApplicationOrder: (ApplicationOrderChange) -> Unit = {},
    showModuleAddEntries: Boolean = editMode,
    showMainAddEntries: Boolean = editMode,
    moduleEdgeScrollDirection: Int = 0,
    moduleInsertionIndex: Int? = null,
    onModuleBoundsInWindow: (String, Rect) -> Unit = { _, _ -> },
    onModuleDisposed: (String) -> Unit = {},
    onModuleListBoundsInWindow: (Rect) -> Unit = {},
    onModuleDragStart: (OrderedFavoriteModule, Offset) -> Boolean = { _, _ -> false },
    onModuleDrag: (Offset) -> Unit = {},
    onModuleDragEnd: () -> Unit = {},
    onModuleDragCancel: () -> Unit = {},
    enterBatch: HomeFavoriteEnterBatch? = null,
) {
    val animationDuration = integerResource(id = R.integer.short_property_animation_duration_ms)
    val ribbonListStates = remember {
        mutableStateMapOf<String, LazyListState>()
    }
    if (applicationMovement != null) {
        SideEffect(effect = { applicationMovement.updateModules(current = modules) })
        HomeApplicationAutoScroll(
            movement = applicationMovement,
            mainListState = listState,
            ribbonStates = ribbonListStates,
        )
    }
    val localModuleBounds = remember { mutableMapOf<String, Rect>() }
    var listOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val currentModules by rememberUpdatedState(modules)
    val currentAvailability by rememberUpdatedState(newValue = availabilityByIdentity)
    val currentApplicationMovementEnabled by rememberUpdatedState(newValue = addEntriesEnabled)
    val currentOnCommitApplicationOrder by rememberUpdatedState(newValue = onCommitApplicationOrder)
    val currentOnModuleDragStart by rememberUpdatedState(onModuleDragStart)
    val currentOnModuleDrag by rememberUpdatedState(onModuleDrag)
    val currentOnModuleDragEnd by rememberUpdatedState(onModuleDragEnd)
    val currentOnModuleDragCancel by rememberUpdatedState(onModuleDragCancel)
    val moduleHapticFeedback = LocalHapticFeedback.current
    val insertionLineColor = colorResource(R.color.home_favorite_insertion_line)
    val insertionLineThickness = dimensionResource(
        R.dimen.home_favorite_insertion_line_thickness,
    )
    val edgeFeedbackBand = dimensionResource(R.dimen.home_favorite_edge_scroll_band)
    val edgeFeedbackColor = MaterialTheme.colorScheme.onBackground.copy(
        alpha = integerResource(id = R.integer.home_favorite_edge_feedback_alpha_percent) / 100f,
    )
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .then(
                nestedScrollConnection?.let { Modifier.nestedScroll(it) } ?: Modifier,
            )
            .onGloballyPositioned { coordinates ->
                val origin = coordinates.positionInWindow()
                listOriginInWindow = origin
                onModuleListBoundsInWindow(Rect(origin, coordinates.size.toSize()))
                enterBatch?.updateViewport(
                    bounds = Rect(
                        offset = origin,
                        size = coordinates.size.toSize()
                    )
                )
                applicationMovement?.updateViewport(
                    bounds = Rect(
                        offset = origin,
                        size = coordinates.size.toSize()
                    )
                )
            }
            .then(
                // Keep the pointer-input node stable for the complete expanded-panel
                // lifetime. Removing and recreating it when a drag disables other editing
                // actions would cancel the gesture that owns the active module movement.
                if (selectionEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectModuleReorderDrag(
                            onLongPress = {
                                moduleHapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.LongPress,
                                )
                            },
                            onDragStart = { localTouch ->
                                val touchInWindow = listOriginInWindow + localTouch
                                val module = currentModules.firstOrNull { candidate ->
                                    localModuleBounds[candidate.id]
                                        ?.contains(touchInWindow) == true
                                } ?: return@detectModuleReorderDrag false
                                currentOnModuleDragStart(module, touchInWindow)
                            },
                            onDrag = { currentOnModuleDrag(it) },
                            onDragEnd = { currentOnModuleDragEnd() },
                            onDragCancel = { currentOnModuleDragCancel() },
                        )
                    }
                } else if (editMode && applicationMovement != null) {
                    Modifier.pointerInput(key1 = applicationMovement) {
                        detectHomeApplicationMovement(
                            movement = applicationMovement,
                            onStart = { identity, pointer ->
                                val module =
                                    currentModules.firstOrNull(predicate = { identity in it.identities })
                                val availability = currentAvailability[identity]
                                currentApplicationMovementEnabled && module != null && availability != null &&
                                        applicationMovement.start(
                                            identity = identity,
                                            module = module,
                                            availability = availability,
                                            pointer = pointer,
                                        )
                            },
                            onRecognized = {
                                moduleHapticFeedback.performHapticFeedback(hapticFeedbackType = HapticFeedbackType.LongPress)
                            },
                            onCommit = { change -> currentOnCommitApplicationOrder(change) },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .drawWithContent {
                drawContent()
                if (moduleEdgeScrollDirection != 0) {
                    val band = edgeFeedbackBand.toPx().coerceAtMost(size.height / 2f)
                    if (moduleEdgeScrollDirection < 0) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    edgeFeedbackColor,
                                    edgeFeedbackColor.copy(alpha = 0f),
                                ),
                                startY = 0f,
                                endY = band,
                            ),
                            size = Size(size.width, band),
                        )
                    } else {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    edgeFeedbackColor.copy(alpha = 0f),
                                    edgeFeedbackColor,
                                ),
                                startY = size.height - band,
                                endY = size.height,
                            ),
                            topLeft = Offset(0f, size.height - band),
                            size = Size(size.width, band),
                        )
                    }
                }
            }
            .testTag("home_ordered_favorite_modules"),
        state = listState,
        userScrollEnabled = applicationMovement?.isDragging != true,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.home_module_spacing),
        ),
    ) {
        itemsIndexed(
            items = modules,
            key = { _, module -> module.id },
        ) { index, module ->
            DisposableEffect(module.id) {
                onDispose {
                    localModuleBounds.remove(module.id)
                    onModuleDisposed(module.id)
                    applicationMovement?.updateModule(id = module.id, bounds = null)
                }
            }
            val showsTopInsertion = moduleInsertionIndex == index
            val showsBottomInsertion = index == modules.lastIndex &&
                    moduleInsertionIndex == modules.size
            Box(
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = tween(durationMillis = animationDuration),
                    )
                    .homeFavoriteEnter(
                        batch = enterBatch,
                        key = HomeFavoriteEnterKey(moduleId = module.id)
                    )
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val origin = coordinates.positionInWindow()
                        val bounds = Rect(origin, coordinates.size.toSize())
                        localModuleBounds[module.id] = bounds
                        onModuleBoundsInWindow(module.id, bounds)
                        applicationMovement?.updateModule(id = module.id, bounds = bounds)
                    }
                    .drawWithContent {
                        drawContent()
                        val stroke = insertionLineThickness.toPx()
                        if (showsTopInsertion) {
                            drawLine(
                                insertionLineColor,
                                Offset(0f, stroke / 2f),
                                Offset(size.width, stroke / 2f),
                                stroke,
                            )
                        }
                        if (showsBottomInsertion) {
                            drawLine(
                                insertionLineColor,
                                Offset(0f, size.height - stroke / 2f),
                                Offset(size.width, size.height - stroke / 2f),
                                stroke,
                            )
                        }
                    },
            ) {
                val ribbonListState = if (module.type == OrderedFavoriteModuleType.Ribbon) {
                    ribbonListStates.getOrPut(module.id) { LazyListState() }
                } else {
                    null
                }
                HomeOrderedModuleContent(
                    enterBatch = enterBatch,
                    module = module,
                    availabilityByIdentity = availabilityByIdentity,
                    ribbonListState = ribbonListState,
                    editMode = editMode,
                    showAddEntry = showModuleAddEntries,
                    addEntryEnabled = addEntriesEnabled,
                    onAddToModule = { onAddToModule(module) },
                    onLaunchFavorite = onLaunchFavorite,
                    onLongPressFavorite = onLongPressFavorite,
                    applicationEditing = editMode && !selectionEnabled,
                    applicationMutationEnabled = addEntriesEnabled,
                    onRemoveFavorite = onRemoveFavorite,
                    applicationMovement = applicationMovement,
                )
                if (selectionEnabled) {
                    HomeModuleSelectionLayer(
                        modifier = Modifier.matchParentSize(),
                        selected = module.id == selectedModuleId,
                        enabled = selectionInteractionEnabled,
                        visualEnabled = selectionVisualEnabled,
                        onSelect = { onSelectModule(module.id) },
                    )
                }
            }
        }
        if (showMainAddEntries) {
            item(key = "main-list-add-entries") {
                DisposableEffect(
                    key1 = applicationMovement,
                    effect = {
                        onDispose {
                            OrderedFavoriteModuleType.entries.forEach(
                                action = { type ->
                                    applicationMovement?.updateCreationTarget(
                                        type = type,
                                        bounds = null
                                    )
                                },
                            )
                        }
                    },
                )
                Row(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = tween(durationMillis = animationDuration),
                        )
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.home_add_favorite_entry_gap),
                    ),
                ) {
                    HomeMainListAddFavoriteEntry(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .onGloballyPositioned(
                                onGloballyPositioned = { coordinates ->
                                    applicationMovement?.updateCreationTarget(
                                        type = OrderedFavoriteModuleType.Vertical,
                                        bounds = Rect(
                                            offset = coordinates.positionInWindow(),
                                            size = coordinates.size.toSize()
                                        ),
                                    )
                                },
                            ),
                        label = stringResource(R.string.home_add_favorite_list),
                        testTag = "home_add_favorite_list",
                        enabled = addEntriesEnabled,
                        contentEnabled = addEntriesEnabled || applicationMovement?.activeIdentity != null,
                        onClick = onCreateVerticalModule,
                    )
                    HomeMainListAddFavoriteEntry(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .onGloballyPositioned(
                                onGloballyPositioned = { coordinates ->
                                    applicationMovement?.updateCreationTarget(
                                        type = OrderedFavoriteModuleType.Ribbon,
                                        bounds = Rect(
                                            offset = coordinates.positionInWindow(),
                                            size = coordinates.size.toSize()
                                        ),
                                    )
                                },
                            ),
                        label = stringResource(R.string.home_add_favorite_ribbon),
                        testTag = "home_add_favorite_ribbon",
                        enabled = addEntriesEnabled,
                        contentEnabled = addEntriesEnabled || applicationMovement?.activeIdentity != null,
                        onClick = onCreateRibbon,
                    )
                }
            }
        }
    }
}

@Composable

internal fun HomeModuleDragPreview(
    session: ModuleDragSession,
    rootOriginInWindow: Offset,
) {
    val density = LocalDensity.current
    val width = with(density) { session.size.width.toDp() }
    val height = with(density) { session.size.height.toDp() }
    val shadowElevation = dimensionResource(R.dimen.home_module_drag_shadow_elevation)
    val shape = RoundedCornerShape(
        dimensionResource(R.dimen.home_module_selection_radius),
    )
    HomeOrderedModuleComposition(
        modules = listOf(session.sourceModule),
        availabilityByIdentity = session.sourceAvailability,
        listState = rememberLazyListState(),
        nestedScrollConnection = null,
        editMode = true,
        selectionEnabled = true,
        selectionInteractionEnabled = false,
        selectionVisualEnabled = true,
        selectedModuleId = session.sourceModule.id.takeIf { session.sourceSelected },
        onSelectModule = {},
        addEntriesEnabled = true,
        onAddToModule = {},
        onCreateVerticalModule = {},
        onCreateRibbon = {},
        onLaunchFavorite = {},
        onLongPressFavorite = {},
        showModuleAddEntries = true,
        showMainAddEntries = false,
        modifier = Modifier
            .offset {
                val position = session.originInWindow - rootOriginInWindow + session.delta
                IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }
            .size(width, height)
            .shadow(shadowElevation, shape, clip = false)
            .clearAndSetSemantics { }
            .testTag("home_module_drag_preview"),
    )
}

@Composable

internal fun HomeModuleSelectionLayer(
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    visualEnabled: Boolean,
    onSelect: () -> Unit,
) {
    val selectedShape = RoundedCornerShape(
        dimensionResource(R.dimen.home_module_selection_radius),
    )
    val selectedBorder = dimensionResource(R.dimen.home_module_selection_stroke)
    val markColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (visualEnabled) 1f else 0.38f,
    )
    val markInset = dimensionResource(R.dimen.home_module_mark_inset)
    val markArm = dimensionResource(R.dimen.home_module_mark_arm)
    val markStroke = dimensionResource(R.dimen.home_module_mark_stroke)
    val markRadius = dimensionResource(R.dimen.home_module_mark_radius)
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.border(
                        width = selectedBorder,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (visualEnabled) 1f else 0.38f,
                        ),
                        shape = selectedShape,
                    )
                } else {
                    Modifier.drawWithContent {
                        drawContent()
                        val inset = markInset.toPx()
                        val arm = markArm.toPx()
                        val stroke = markStroke.toPx()
                        val radius = markRadius.toPx()
                        val left = inset
                        val top = inset
                        val right = size.width - inset
                        val bottom = size.height - inset
                        drawCornerMark(
                            color = markColor,
                            corner = Offset(left, top),
                            horizontalEnd = Offset(left + arm, top),
                            verticalEnd = Offset(left, top + arm),
                            radius = radius,
                            stroke = stroke,
                            startAngle = 180f,
                        )
                        drawCornerMark(
                            color = markColor,
                            corner = Offset(right, top),
                            horizontalEnd = Offset(right - arm, top),
                            verticalEnd = Offset(right, top + arm),
                            radius = radius,
                            stroke = stroke,
                            startAngle = 270f,
                        )
                        drawCornerMark(
                            color = markColor,
                            corner = Offset(left, bottom),
                            horizontalEnd = Offset(left + arm, bottom),
                            verticalEnd = Offset(left, bottom - arm),
                            radius = radius,
                            stroke = stroke,
                            startAngle = 90f,
                        )
                        drawCornerMark(
                            color = markColor,
                            corner = Offset(right, bottom),
                            horizontalEnd = Offset(right - arm, bottom),
                            verticalEnd = Offset(right, bottom - arm),
                            radius = radius,
                            stroke = stroke,
                            startAngle = 0f,
                        )
                    }
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onSelect)
            .testTag(if (selected) "home_module_selected" else "home_module_selectable"),
    )
}

@Composable

internal fun HomeFavoriteMessage(
    message: String,
    showProgress: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showProgress) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(
                    dimensionResource(R.dimen.status_progress_indicator_size),
                ),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_inventory_error),
                contentDescription = null,
                modifier = Modifier.size(
                    dimensionResource(R.dimen.home_favorite_error_icon_size),
                ),
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.status_message_gap)))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = dimensionResource(R.dimen.home_favorite_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.home_favorite_line_height).value.sp,
        )
        onRetry?.let { retry ->
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.status_message_gap)))
            TextButton(onClick = retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun HomeFavoriteComposition(
    verticalLists: List<FavoriteContainer>,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    favoriteListState: LazyListState,
    favoriteNestedScrollConnection: NestedScrollConnection?,
    companionFavoriteListState: LazyListState,
    companionFavoriteNestedScrollConnection: NestedScrollConnection?,
    onLaunchFavorite: (FavoriteAvailability) -> Unit,
    onLongPressFavorite: (LaunchableEntry) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val listStates = remember {
            mutableMapOf<String, LazyListState>()
        }
        val contentHeight = verticalLists
            .maxOf { container ->
                dimensionResource(container.listSize.rowHeightResource()) *
                        container.identities.size
            }
            .coerceAtMost(maxHeight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.home_favorite_group_spacing),
            ),
        ) {
            verticalLists.forEachIndexed { index, container ->
                val listState = listStates.getOrPut(container.id) {
                    when (index) {
                        0 -> favoriteListState
                        1 -> companionFavoriteListState
                        else -> LazyListState()
                    }
                }
                val nestedScrollConnection = when (index) {
                    0 -> favoriteNestedScrollConnection
                    1 -> companionFavoriteNestedScrollConnection
                    else -> null
                }
                HomeFavoriteList(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    identities = container.identities,
                    availabilityByIdentity = availabilityByIdentity,
                    listState = listState,
                    nestedScrollConnection = nestedScrollConnection,
                    compact = false,
                    listSize = container.listSize,
                    onLaunchFavorite = onLaunchFavorite,
                    onLongPressFavorite = onLongPressFavorite,
                    testTag = "home_favorite_list_$index",
                )
            }
        }
    }
}
