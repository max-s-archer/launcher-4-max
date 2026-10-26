package com.maxarcher.launcher.ui.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.ApplicationOrderChange
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteContainer
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.HomeOrderedModuleContent
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.ModuleDragSession
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.OrderedFavoriteModuleType
import com.maxarcher.launcher.R
import com.maxarcher.launcher.rowHeightResource
import com.maxarcher.launcher.ui.home.components.HomeApplicationAutoScroll
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarcher.launcher.ui.home.components.HomeMainListAddFavoriteEntry
import com.maxarcher.launcher.ui.home.components.detectHomeApplicationMovement
import com.maxarcher.launcher.ui.home.components.homeFavoriteEnter
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow
import kotlin.math.roundToInt

@Composable
internal fun OrderedFavoriteModule.displayName(): String = name.ifEmpty {
    stringResource(
        when (type) {
            OrderedFavoriteModuleType.Vertical -> R.string.home_module_default_name_list
            OrderedFavoriteModuleType.Ribbon -> R.string.home_module_default_name_ribbon
        },
    )
}

/** The non-interactive ordinary-Home title row of a module whose name is shown. */
@Composable
internal fun HomeModuleTitleRow(module: OrderedFavoriteModule) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.home_module_header_height))
            .padding(horizontal = dimensionResource(R.dimen.home_module_header_content_inset))
            .testTag("home_module_title_${module.id}"),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = module.displayName(),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
            lineHeight = dimensionResource(
                R.dimen.style_settings_secondary_line_height,
            ).value.sp,
            style = LocalTextStyle.current.copy(shadow = drawerForegroundShadow()),
        )
    }
}

@Composable

internal fun HomeOrderedModuleComposition(
    modules: List<OrderedFavoriteModule>,
    availabilityByIdentity: Map<LaunchableIdentity, FavoriteAvailability>,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection?,
    editMode: Boolean,
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
    openStyleModuleId: String? = null,
    headerActionsEnabled: Boolean = true,
    editSaving: Boolean = false,
    moduleNameEditable: Boolean = true,
    moduleNameRestoreKey: Int = 0,
    onToggleModuleStyle: (String) -> Unit = {},
    onRenameModule: (String, String) -> Unit = { _, _ -> },
    onToggleModuleNameVisible: (OrderedFavoriteModule) -> Unit = {},
    onRemoveModule: ((OrderedFavoriteModule) -> Unit)? = null,
    stylePanel: @Composable (OrderedFavoriteModule) -> Unit = {},
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
    val handleBounds = remember { mutableMapOf<String, Rect>() }
    // Window bounds of the module name field that is being edited, if any.
    val focusedNameBounds = remember { mutableMapOf<String, Rect>() }
    var listOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val currentModules by rememberUpdatedState(modules)
    val currentAvailability by rememberUpdatedState(newValue = availabilityByIdentity)
    val currentApplicationMovementEnabled by rememberUpdatedState(newValue = addEntriesEnabled)
    val currentOnCommitApplicationOrder by rememberUpdatedState(newValue = onCommitApplicationOrder)
    val currentOnModuleDragStart by rememberUpdatedState(onModuleDragStart)
    val currentOnModuleDrag by rememberUpdatedState(onModuleDrag)
    val currentOnModuleDragEnd by rememberUpdatedState(onModuleDragEnd)
    val currentOnModuleDragCancel by rememberUpdatedState(onModuleDragCancel)
    val currentOpenStyleModuleId by rememberUpdatedState(openStyleModuleId)
    val focusManager = LocalFocusManager.current
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
                // The handle gesture lives on the list rather than the handle: the source
                // module leaves list layout when movement starts, which would cancel a
                // gesture owned by the handle itself.
                if (editMode) {
                    Modifier.pointerInput(Unit) {
                        detectModuleHandleDrag(
                            handleAt = { localTouch ->
                                val touchInWindow = listOriginInWindow + localTouch
                                // Touching another module target first ends name editing.
                                if (focusedNameBounds.isNotEmpty() &&
                                    focusedNameBounds.values.none { it.contains(touchInWindow) }
                                ) {
                                    focusManager.clearFocus()
                                }
                                currentModules.firstOrNull { candidate ->
                                    handleBounds[candidate.id]?.contains(touchInWindow) == true
                                }?.let { module -> module to touchInWindow }
                            },
                            onDragStart = { module, touchInWindow ->
                                focusManager.clearFocus()
                                currentOnModuleDragStart(module, touchInWindow).also { started ->
                                    if (started) {
                                        moduleHapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                    }
                                }
                            },
                            onDrag = { currentOnModuleDrag(it) },
                            onDragEnd = { currentOnModuleDragEnd() },
                            onDragCancel = { currentOnModuleDragCancel() },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (editMode && applicationMovement != null) {
                    Modifier.pointerInput(key1 = applicationMovement) {
                        detectHomeApplicationMovement(
                            movement = applicationMovement,
                            onStart = { identity, pointer ->
                                val module =
                                    currentModules.firstOrNull(predicate = { identity in it.identities })
                                val availability = currentAvailability[identity]
                                // An open style panel blocks application targets only inside
                                // its owning module.
                                val started = currentApplicationMovementEnabled &&
                                    module != null &&
                                    module.id != currentOpenStyleModuleId &&
                                    availability != null &&
                                    applicationMovement.start(
                                        identity = identity,
                                        module = module,
                                        availability = availability,
                                        pointer = pointer,
                                    )
                                if (started) focusManager.clearFocus()
                                started
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
                    handleBounds.remove(module.id)
                    focusedNameBounds.remove(module.id)
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
                val styleOpen = editMode && module.id == openStyleModuleId
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (editMode) {
                        HomeModuleOperationHeader(
                            module = module,
                            actionsEnabled = headerActionsEnabled,
                            nameEditable = moduleNameEditable,
                            styleOpen = styleOpen,
                            nameRestoreKey = moduleNameRestoreKey,
                            onRename = { name -> onRenameModule(module.id, name) },
                            onNameFieldBoundsChange = { bounds ->
                                if (bounds == null) {
                                    focusedNameBounds.remove(module.id)
                                } else {
                                    focusedNameBounds[module.id] = bounds
                                }
                            },
                            onToggleNameVisible = { onToggleModuleNameVisible(module) },
                            onToggleStyle = { onToggleModuleStyle(module.id) },
                            onRemoveModule = onRemoveModule?.let { remove -> { remove(module) } },
                            reorderHandleModifier = Modifier.onGloballyPositioned {
                                handleBounds[module.id] = Rect(
                                    offset = it.positionInWindow(),
                                    size = it.size.toSize(),
                                )
                            },
                        )
                    } else if (module.nameVisible) {
                        HomeModuleTitleRow(module = module)
                    }
                    AnimatedVisibility(
                        visible = styleOpen,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = animationDuration),
                            expandFrom = Alignment.Top,
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = animationDuration),
                            shrinkTowards = Alignment.Top,
                        ),
                    ) {
                        Column {
                            stylePanel(module)
                            Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                        }
                    }
                    HomeOrderedModuleContent(
                        enterBatch = enterBatch,
                        module = module,
                        availabilityByIdentity = availabilityByIdentity,
                        ribbonListState = ribbonListState,
                        editMode = editMode,
                        showAddEntry = showModuleAddEntries,
                        // The open panel blocks its owning module's application targets,
                        // including the trailing add-favorite entry.
                        addEntryEnabled = addEntriesEnabled && !styleOpen,
                        onAddToModule = { onAddToModule(module) },
                        onLaunchFavorite = onLaunchFavorite,
                        onLongPressFavorite = onLongPressFavorite,
                        applicationEditing = editMode && !styleOpen,
                        applicationMutationEnabled = addEntriesEnabled,
                        onRemoveFavorite = onRemoveFavorite,
                        applicationMovement = applicationMovement,
                    )
                }
                if (styleOpen) {
                    HomeModuleSelectionOutline(
                        modifier = Modifier.matchParentSize(),
                        saving = editSaving,
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
    stylePanel: @Composable (OrderedFavoriteModule) -> Unit = {},
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
        openStyleModuleId = session.sourceModule.id.takeIf { session.sourceSelected },
        headerActionsEnabled = false,
        moduleNameEditable = false,
        stylePanel = stylePanel,
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

/** The selection outline of the module whose style settings panel is open. */
@Composable
internal fun HomeModuleSelectionOutline(modifier: Modifier, saving: Boolean) {
    // An unresolved exclusive edit save dims the outline to the shared disabled opacity.
    val alpha = if (saving) {
        integerResource(R.integer.disabled_content_alpha_percent) / 100f
    } else {
        1f
    }
    Box(
        modifier = modifier
            .border(
                width = dimensionResource(R.dimen.home_module_selection_stroke),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                shape = RoundedCornerShape(dimensionResource(R.dimen.home_module_selection_radius)),
            )
            .testTag("home_module_selected"),
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
