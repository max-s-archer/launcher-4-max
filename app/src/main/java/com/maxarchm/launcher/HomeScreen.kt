package com.maxarchm.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.maxarchm.launcher.ui.drawer.DrawerDragDrop
import com.maxarchm.launcher.ui.drawer.DrawerDragJourney
import com.maxarchm.launcher.ui.home.HomeFavoriteEditOrchestration
import com.maxarchm.launcher.ui.home.components.HomeApplicationMovementOverlay
import com.maxarchm.launcher.ui.home.components.HomeDrawerDragHandling
import com.maxarchm.launcher.ui.home.components.HomeBasicInformation
import com.maxarchm.launcher.ui.home.components.HomeDockDragGhost
import com.maxarchm.launcher.ui.home.components.HomeDockDragPreview
import com.maxarchm.launcher.ui.home.components.HomeDockSurface
import com.maxarchm.launcher.ui.home.components.HomeDefaultLauncherPrompt
import com.maxarchm.launcher.ui.home.components.HomeEditDock
import com.maxarchm.launcher.ui.home.components.HomeFavoriteBarContainerDragPreview
import com.maxarchm.launcher.ui.home.components.HomeFavoriteExitOverlay
import com.maxarchm.launcher.ui.home.components.HomeFavoriteList
import com.maxarchm.launcher.ui.home.components.HomeFavoriteListDragPreview
import com.maxarchm.launcher.ui.home.components.HomeFavoriteMessage
import com.maxarchm.launcher.ui.home.components.HomeFavoriteProvisionalList
import com.maxarchm.launcher.ui.home.components.HomeFavoriteRibbon
import com.maxarchm.launcher.ui.home.components.HomeFavoriteRibbonActions
import com.maxarchm.launcher.ui.home.components.HomeFavoriteRibbonDragActions
import com.maxarchm.launcher.ui.home.components.HomeFavoriteRibbonDragState
import com.maxarchm.launcher.ui.home.components.HomeModuleDragPreview
import com.maxarchm.launcher.ui.home.components.HomeOrderedModuleComposition
import com.maxarchm.launcher.ui.home.components.rememberHomeFavoriteEnterBatch
import com.maxarchm.launcher.ui.home.components.stableKey
import com.maxarchm.launcher.ui.home.components.withPresentationFrom
import com.maxarchm.launcher.ui.style.HomeModuleStylePanel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.maxarchm.launcher.ApplicationDragContainerDescriptor
import com.maxarchm.launcher.ApplicationDragAxis
import com.maxarchm.launcher.ApplicationDragTargetSession
import com.maxarchm.launcher.ApplicationDragTargetMode
import com.maxarchm.launcher.FavoriteBarDragSession
import com.maxarchm.launcher.FavoriteDragSession
import com.maxarchm.launcher.NANOS_PER_SECOND
import com.maxarchm.launcher.PRIMARY_LIST_ID
import com.maxarchm.launcher.PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0
import com.maxarchm.launcher.PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1





@Composable
internal fun HomeScreen(
    favoriteState: FavoriteReadState = FavoriteReadState.Readable(emptyList()),
    favoriteAvailability: Map<LaunchableIdentity, FavoriteAvailability> = emptyMap(),
    favoriteListState: LazyListState = rememberLazyListState(),
    favoriteNestedScrollConnection: NestedScrollConnection? = null,
    companionFavoriteListState: LazyListState = rememberLazyListState(),
    companionFavoriteNestedScrollConnection: NestedScrollConnection? = null,
    editMode: Boolean = false,
    stylePanelExpanded: Boolean = false,
    selectedModuleId: String? = null,
    applicationEditingSaving: Boolean = false,
    onRemoveApplication: (LaunchableIdentity) -> Unit = {},
    onCommitApplicationOrder: (change: ApplicationOrderChange, onComplete: () -> Unit) -> Unit = { _, complete -> complete() },
    removalSnackbarHostState: SnackbarHostState? = null,
    onRetryFavorites: () -> Unit = {},
    onRequestEditMode: () -> Unit = {},
    onStylePanelExpandedChange: (Boolean) -> Unit = {},
    onSelectModule: (String) -> Unit = {},
    onLaunchFavorite: (FavoriteAvailability) -> Unit = {},
    onLongPressFavorite: (LaunchableEntry) -> Unit = {},
    onAddFavoritesToList: (String) -> Unit = {},
    onAddProvisionalFavorites: () -> Unit = {},
    onAddFavoritesToBar: (String) -> Unit = {},
    onAddProvisionalFavoriteBar: () -> Unit = {},
    onAddToHomeBottomAppBar: () -> Unit = {},
    onReorderHomeBottomAppBar: (List<LaunchableIdentity>) -> Unit = {},
    homeBottomAppBarExpanded: Boolean = false,
    onHomeBottomAppBarExpandedChange: (Boolean) -> Unit = {},
    favoriteRevealContainerId: String? = null,
    favoriteRevealContainerType: FavoriteContainerType? = null,
    favoriteRevealIdentity: LaunchableIdentity? = null,
    onFavoriteRevealComplete: () -> Unit = {},
    onCommitFavoriteComposition: suspend (
        transform: (FavoriteAggregate) -> FavoriteAggregate,
    ) -> FavoriteAggregate? = { transform -> transform(FavoriteAggregate()) },
    onCommitModuleOrder: suspend (List<String>) -> Boolean = { false },
    accessibilityLockController: AccessibilityLockController = EmptyAccessibilityLockController,
    quickActionBindings: QuickActionBindings = QuickActionBindings(),
    showDefaultLauncherPrompt: Boolean = false,
    onSelectDefaultLauncherPrompt: () -> Unit = {},
    onDismissDefaultLauncherPrompt: () -> Unit = {},
    drawerDragJourney: DrawerDragJourney? = null,
    drawerDragTouchInWindow: Offset = Offset.Zero,
    drawerDragDropping: Boolean = false,
    onDrawerDragCommit: suspend (LaunchableIdentity, DrawerDragDrop) -> Boolean = { _, _ -> false },
    onDrawerDragJourneyFinished: (saved: Boolean, cancelled: Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val orderedApplicationMovement = remember(calculation = { HomeApplicationMovement() })
    HomeDrawerDragHandling(
        movement = orderedApplicationMovement,
        journey = drawerDragJourney,
        touchInWindow = drawerDragTouchInWindow,
        dropping = drawerDragDropping,
        editMode = editMode,
        stylePanelExpanded = stylePanelExpanded,
        favoriteState = favoriteState,
        favoriteAvailability = favoriteAvailability,
        onCommit = onDrawerDragCommit,
        onFinished = onDrawerDragJourneyFinished,
    )
    val favoriteEnterBatch = rememberHomeFavoriteEnterBatch(
        modules = (favoriteState as? FavoriteReadState.Readable)?.orderedModules,
    )
    val applicationMovementActive = orderedApplicationMovement.activeIdentity != null
    val favoriteBarItemWidthPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_favorite_bar_item_width).toPx()
    }
    val favoriteBarItemStridePx = with(LocalDensity.current) {
        favoriteBarItemWidthPx + dimensionResource(
            R.dimen.home_favorite_bar_item_spacing,
        ).toPx()
    }
    var dragGeneration by remember { mutableIntStateOf(0) }
    val currentFavoriteState by rememberUpdatedState(favoriteState)
    val snackbarHostState = removalSnackbarHostState ?: remember { SnackbarHostState() }
    val editScope = rememberCoroutineScope()
    var dragRootOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var homeDockDragPreview by remember {
        mutableStateOf<HomeDockDragPreview?>(null)
    }
    // Band at a row's top and bottom edge that a cross-group drag reads as an insertion boundary
    // instead of the favorite's body, because adjacent rows leave no gap between them.
    val insertionBoundaryBandPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_favorite_insertion_boundary_band).toPx()
    }
    // Band at a group's leading and trailing edge where an active drag scrolls that group, so
    // favorites outside the viewport stay reachable without releasing the drag.
    val edgeScrollBandPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_favorite_edge_scroll_band).toPx()
    }
    val edgeScrollSpeedPxPerSecond = with(LocalDensity.current) {
        integerResource(R.integer.home_favorite_edge_scroll_dp_per_second).dp.toPx()
    }
    val edgeScrollStartDelayMillis =
        integerResource(R.integer.home_favorite_edge_scroll_start_delay_ms).toLong()
    val hapticFeedback = LocalHapticFeedback.current
    val undoLabel = stringResource(R.string.undo)
    val favoriteListSizeMessage = stringResource(R.string.favorite_list_size)
    val favoriteListRemovedMessage = stringResource(R.string.favorite_list_removed)
    val favoriteBarRemovedMessage = stringResource(R.string.favorite_bar_removed)
    val favoriteRemovedMessage = stringResource(R.string.favorite_removed)
    val undoUnavailableMessage = stringResource(R.string.favorite_undo_unavailable)
    val moduleStyleSaveFailureMessage = stringResource(
        R.string.unable_to_save_module_style,
    )
    val moduleOrderSaveFailureMessage = stringResource(
        R.string.unable_to_save_module_order,
    )

    val orchestration = remember {
        HomeFavoriteEditOrchestration(
            orderedApplicationMovement = orderedApplicationMovement,
            favoriteListState = favoriteListState,
            companionFavoriteListState = companionFavoriteListState,
            snackbarHostState = snackbarHostState,
            editScope = editScope,
            context = context,
            hapticFeedback = hapticFeedback,
        )
    }
    orchestration.editMode = editMode
    orchestration.applicationEditingSaving = applicationEditingSaving
    orchestration.selectedModuleId = selectedModuleId
    orchestration.favoriteState = currentFavoriteState
    orchestration.favoriteAvailability = favoriteAvailability
    orchestration.undoLabel = undoLabel
    orchestration.favoriteRemovedMessage = favoriteRemovedMessage
    orchestration.favoriteBarRemovedMessage = favoriteBarRemovedMessage
    orchestration.undoUnavailableMessage = undoUnavailableMessage
    orchestration.moduleStyleSaveFailureMessage = moduleStyleSaveFailureMessage
    orchestration.moduleOrderSaveFailureMessage = moduleOrderSaveFailureMessage
    orchestration.insertionBoundaryBandPx = insertionBoundaryBandPx
    orchestration.edgeScrollBandPx = edgeScrollBandPx
    orchestration.edgeScrollSpeedPxPerSecond = edgeScrollSpeedPxPerSecond
    orchestration.edgeScrollStartDelayMillis = edgeScrollStartDelayMillis
    orchestration.favoriteBarItemStridePx = favoriteBarItemStridePx
    orchestration.onCommitFavoriteComposition = onCommitFavoriteComposition
    orchestration.onCommitModuleOrder = onCommitModuleOrder

    LaunchedEffect(
        favoriteRevealContainerId,
        favoriteRevealContainerType,
        favoriteRevealIdentity,
        favoriteState,
        editMode,
    ) {
        val containerId = favoriteRevealContainerId ?: return@LaunchedEffect
        val identity = favoriteRevealIdentity ?: return@LaunchedEffect
        if (!editMode || favoriteState !is FavoriteReadState.Readable) {
            return@LaunchedEffect
        }
        val aggregate = favoriteState.aggregate
        val containerType = favoriteRevealContainerType
            ?: FavoriteContainerType.VerticalList
        val containers = when (containerType) {
            FavoriteContainerType.VerticalList -> aggregate.verticalLists
            FavoriteContainerType.FavoriteBar -> aggregate.favoriteBars
        }
        val containerIndex = containers.indexOfFirst { it.id == containerId }
        val container = containers.getOrNull(containerIndex)
        val itemIndex = container?.identities?.indexOf(identity) ?: -1
        if (container == null || itemIndex < 0) {
            onFavoriteRevealComplete()
            return@LaunchedEffect
        }
        val listState = when (containerType) {
            FavoriteContainerType.VerticalList -> when (containerIndex) {
                0 -> orchestration.editListStates.getOrPut(container.id) { favoriteListState }
                1 -> orchestration.editListStates.getOrPut(container.id) {
                    companionFavoriteListState
                }
                else -> null
            }

            FavoriteContainerType.FavoriteBar -> orchestration.favoriteBarStates.getOrPut(
                container.id,
            ) { LazyListState() }
        }
        if (listState == null) {
            onFavoriteRevealComplete()
            return@LaunchedEffect
        }
        withFrameNanos { }
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val target = visibleItems.firstOrNull { it.index == itemIndex }
        if (target != null) {
            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val targetEnd = target.offset + target.size
            when {
                target.offset < viewportStart -> {
                    listState.scrollBy((target.offset - viewportStart).toFloat())
                }

                targetEnd > viewportEnd -> {
                    listState.scrollBy((targetEnd - viewportEnd).toFloat())
                }
            }
        } else if (visibleItems.isNotEmpty() &&
            containerType == FavoriteContainerType.FavoriteBar
        ) {
            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val firstVisible = visibleItems.first()
            val targetStart = firstVisible.offset +
                    ((itemIndex - firstVisible.index) * favoriteBarItemStridePx)
            val targetEnd = targetStart + favoriteBarItemWidthPx
            when {
                targetStart < viewportStart -> listState.scrollBy(targetStart - viewportStart)
                targetEnd > viewportEnd -> listState.scrollBy(targetEnd - viewportEnd)
            }
        } else if (visibleItems.isNotEmpty()) {
            val firstVisibleIndex = visibleItems.first().index
            val lastVisibleIndex = visibleItems.last().index
            if (itemIndex < firstVisibleIndex) {
                listState.scrollToItem(itemIndex)
            } else if (itemIndex > lastVisibleIndex) {
                listState.scrollToItem(
                    (itemIndex - visibleItems.size + 1).coerceAtLeast(0),
                )
            }
        }
        onFavoriteRevealComplete()
    }

    val applicationEdgeScroll = orchestration.applicationDragTargetSession?.edgeScroll(
        descriptors = orchestration.applicationContainerDescriptors,
        bandPx = edgeScrollBandPx,
        primaryListState = favoriteListState,
        companionListState = companionFavoriteListState,
        editListStates = orchestration.editListStates,
        favoriteBarStates = orchestration.favoriteBarStates,
    )

    LaunchedEffect(editMode) {
        if (!editMode) {
            orchestration.editTransaction.leave()
            orchestration.cancelActiveDragSessions()
            orchestration.editMutationJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
        } else {
            orchestration.editTransaction.enter(
                (currentFavoriteState as? FavoriteReadState.Readable)?.aggregate,
            )
        }
    }

    val moduleEdgeScrollDirection = orchestration.moduleDragSession?.let { session ->
        val bounds = orchestration.moduleListBoundsInWindow
        val band = edgeScrollBandPx.coerceAtMost(bounds.height / 2f)
        when {
            bounds == Rect.Zero || !bounds.contains(session.touchInWindow) -> 0
            session.touchInWindow.y < bounds.top + band -> -1
            session.touchInWindow.y > bounds.bottom - band -> 1
            else -> 0
        }
    } ?: 0

    LaunchedEffect(orchestration.moduleDragSession?.sourceModule?.id, moduleEdgeScrollDirection) {
        if (moduleEdgeScrollDirection == 0) return@LaunchedEffect
        delay(duration = edgeScrollStartDelayMillis.milliseconds)
        var previousFrameNanos = withFrameNanos { it }
        while (orchestration.moduleDragSession != null) {
            val session = orchestration.moduleDragSession ?: break
            val bounds = orchestration.moduleListBoundsInWindow
            val band = edgeScrollBandPx.coerceAtMost(bounds.height / 2f)
            val direction = when {
                !bounds.contains(session.touchInWindow) -> 0
                session.touchInWindow.y < bounds.top + band -> -1
                session.touchInWindow.y > bounds.bottom - band -> 1
                else -> 0
            }
            if (direction == 0 || direction != moduleEdgeScrollDirection) break
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
            previousFrameNanos = frameNanos
            val edgeDistance = if (direction < 0) {
                (session.touchInWindow.y - bounds.top).coerceIn(0f, band)
            } else {
                (bounds.bottom - session.touchInWindow.y).coerceIn(0f, band)
            }
            val proximity = if (band == 0f) 0f else 1f - (edgeDistance / band)
            val consumed = favoriteListState.scrollBy(
                direction * edgeScrollSpeedPxPerSecond * proximity * elapsedSeconds,
            )
            if (consumed == 0f) break
            orchestration.advanceModuleDrag(Offset.Zero)
        }
    }

    LaunchedEffect(stylePanelExpanded) {
        if (!stylePanelExpanded) orchestration.moduleDragSession = null
    }

    LaunchedEffect(favoriteState, editMode) {
        val readable = favoriteState as? FavoriteReadState.Readable
            ?: return@LaunchedEffect
        val committed = orchestration.editTransaction.committedAggregate ?: return@LaunchedEffect
        if (editMode &&
            readable.aggregate != committed &&
            orchestration.editMutationJob?.isActive != true &&
            // The saved application order is the pending handoff, not an external edit.
            orderedApplicationMovement.pendingChange == null
        ) {
            orchestration.cancelActiveDragSessions()
            if (orchestration.editTransaction.reconcileExternal(readable.aggregate)) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    LaunchedEffect(
        applicationEdgeScroll?.containerKey,
        applicationEdgeScroll?.axis,
        applicationEdgeScroll?.forward,
    ) {
        val initialRequest = applicationEdgeScroll ?: return@LaunchedEffect
        delay(duration = edgeScrollStartDelayMillis.milliseconds)
        var previousFrame = 0L
        while (true) {
            val request = orchestration.applicationDragTargetSession?.edgeScroll(
                descriptors = orchestration.applicationContainerDescriptors,
                bandPx = edgeScrollBandPx,
                primaryListState = favoriteListState,
                companionListState = companionFavoriteListState,
                editListStates = orchestration.editListStates,
                favoriteBarStates = orchestration.favoriteBarStates,
            ) ?: break
            if (request.containerKey != initialRequest.containerKey ||
                request.axis != initialRequest.axis ||
                request.forward != initialRequest.forward
            ) {
                break
            }
            val state = when (request.axis) {
                ApplicationDragAxis.Vertical -> when {
                    request.containerKey == PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0 ||
                            request.containerKey == PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1 -> null

                    else -> orchestration.editListStates[request.containerKey.substringAfter(':')]
                        ?: if (request.containerKey == "vertical-list:${PRIMARY_LIST_ID}") {
                            favoriteListState
                        } else {
                            companionFavoriteListState
                        }
                }

                ApplicationDragAxis.Horizontal ->
                    orchestration.favoriteBarStates[request.containerKey.substringAfter(':')]
            } ?: break
            if (previousFrame == 0L) {
                previousFrame = withFrameNanos { it }
                continue
            }
            val frame = withFrameNanos { it }
            val elapsedSeconds = (frame - previousFrame) / NANOS_PER_SECOND
            previousFrame = frame
            val distance = edgeScrollSpeedPxPerSecond *
                    request.proximity.coerceIn(0f, 1f) *
                    elapsedSeconds
            val consumed = state.scrollBy(
                if (request.forward) distance else -distance,
            )
            if (consumed == 0f) break
            if (orchestration.dragSession != null) {
                orchestration.advanceAndPersistDrag(Offset.Zero)
            } else if (orchestration.favoriteBarDragSession != null) {
                orchestration.advanceAndPersistFavoriteBarDrag(Offset.Zero)
            }
            if (!state.canScroll(request.forward)) break
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned { dragRootOriginInWindow = it.positionInWindow() },
    ) {
        val contentPadding = dimensionResource(R.dimen.home_content_padding)
        val animationDuration = integerResource(R.integer.short_property_animation_duration_ms)
        val orderedModules = (favoriteState as? FavoriteReadState.Readable)
            ?.orderedModules
            .orEmpty()
        var moduleStylePreview by remember { mutableStateOf<OrderedFavoriteModule?>(null) }
        val previewAggregate = orchestration.editTransaction.previewAggregate(
            (favoriteState as? FavoriteReadState.Readable)?.aggregate ?: FavoriteAggregate(),
        )
        val displayedModules = orderedModules.withPresentationFrom(previewAggregate).map { module ->
            moduleStylePreview?.takeIf { it.id == module.id } ?: module
        }
        LaunchedEffect(editMode, stylePanelExpanded, selectedModuleId) {
            if (!editMode || !stylePanelExpanded) moduleStylePreview = null
        }
        val selectedModule = displayedModules.firstOrNull { it.id == selectedModuleId }
        val styleSaving = applicationEditingSaving ||
            orchestration.editMutationJob?.isActive == true ||
                orchestration.moduleDragSession != null
        val stylePanelMaximumHeight = (
                maxHeight -
                        dimensionResource(R.dimen.home_edit_dock_height) -
                        contentPadding -
                        dimensionResource(R.dimen.home_style_panel_minimum_list_viewport)
                ).coerceAtLeast(0.dp)
        val homeContentWidthDp = maxWidth.value
        val persistedBar = (favoriteState as? FavoriteReadState.Readable)
            ?.orderedAggregate
            ?.homeBottomAppBar
        val barVisible = persistedBar?.enabled == true &&
            (editMode || persistedBar.identities.isNotEmpty())
        Column(modifier = Modifier.fillMaxSize()) {
            if (editMode) {
                HomeEditDock(
                    hasFavorites = orderedModules.isNotEmpty(),
                    expanded = stylePanelExpanded,
                    onToggleExpanded = {
                        if (!applicationMovementActive) {
                            onStylePanelExpandedChange(!stylePanelExpanded)
                        }
                    },
                )
                AnimatedContent(
                    targetState = stylePanelExpanded,
                    modifier = Modifier.padding(horizontal = contentPadding),
                    transitionSpec = {
                        expandVertically(
                            animationSpec = tween(durationMillis = animationDuration),
                            expandFrom = Alignment.Top,
                        ) togetherWith shrinkVertically(
                            animationSpec = tween(durationMillis = animationDuration),
                            shrinkTowards = Alignment.Top,
                        )
                    },
                    label = "home_panel_slot_swap",
                ) { panelExpanded ->
                    if (panelExpanded) {
                        Column {
                            HomeModuleStylePanel(
                                selectedModule = selectedModule,
                                enabled = !styleSaving,
                                maximumHeight = stylePanelMaximumHeight,
                                onPreviewSizes = { iconSize, textSize ->
                                    selectedModule?.let { module ->
                                        moduleStylePreview = module.copy(
                                            iconSize = iconSize,
                                            textSize = textSize,
                                        )
                                    }
                                },
                                onCommitIconSize = { iconSize ->
                                    selectedModule?.let { module ->
                                        moduleStylePreview = null
                                        orchestration.commitVerticalModuleStyle(module.id) {
                                            it.copy(iconSize = iconSize)
                                        }
                                    }
                                },
                                onCommitTextSize = { textSize ->
                                    selectedModule?.let { module ->
                                        moduleStylePreview = null
                                        orchestration.commitVerticalModuleStyle(module.id) {
                                            it.copy(textSize = textSize)
                                        }
                                    }
                                },
                                onChangeNamePlacement = { placement ->
                                    selectedModule?.let { module ->
                                        orchestration.commitVerticalModuleStyle(
                                            module.id,
                                        ) { container ->
                                            container.copy(
                                                namePlacement = placement,
                                                itemsPerRow = container.itemsPerRow.coerceIn(
                                                    when (placement) {
                                                        FavoriteNamePlacement.Right -> 1..2
                                                        FavoriteNamePlacement.Below -> 1..4
                                                        FavoriteNamePlacement.Hidden -> 1..6
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                },
                                onChangeItemsPerRow = { count ->
                                    selectedModule?.let { module ->
                                        orchestration.commitVerticalModuleStyle(module.id) {
                                            it.copy(itemsPerRow = count)
                                        }
                                    }
                                },
                            )
                            Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                        }
                    } else {
                        Column {
                            HomeBasicInformation(
                                editMode = true,
                                accessibilityLockController = accessibilityLockController,
                                onRequestEditMode = onRequestEditMode,
                                bindings = quickActionBindings,
                            )
                            Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                        }
                    }
                }
            } else {
                HomeBasicInformation(
                    editMode = false,
                    accessibilityLockController = accessibilityLockController,
                    onRequestEditMode = onRequestEditMode,
                    bindings = quickActionBindings,
                    modifier = Modifier.padding(
                        top = contentPadding,
                        start = contentPadding,
                        end = contentPadding,
                    ),
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                if (showDefaultLauncherPrompt) {
                    HomeDefaultLauncherPrompt(
                        onSelect = onSelectDefaultLauncherPrompt,
                        onDismiss = onDismissDefaultLauncherPrompt,
                        modifier = Modifier.padding(
                            start = contentPadding,
                            end = contentPadding,
                        ),
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = contentPadding,
                        end = contentPadding,
                        bottom = contentPadding,
                    )
                    .then(
                        if (barVisible &&
                            !editMode &&
                            homeBottomAppBarExpanded &&
                            persistedBar?.presentation ==
                            HomeBottomAppBarPresentation.SemiHidden
                        ) {
                            Modifier.clickable {
                                onHomeBottomAppBarExpandedChange(false)
                            }
                        } else {
                            Modifier
                        },
                    )
                    .onGloballyPositioned(onGloballyPositioned = { coordinates ->
                        favoriteEnterBatch.exitTransitions?.viewport = Rect(
                            offset = coordinates.positionInWindow(),
                            size = coordinates.size.toSize(),
                        )
                    }),
            ) {
                when (favoriteState) {
                    FavoriteReadState.Loading -> HomeFavoriteMessage(
                        message = stringResource(R.string.loading_favorites),
                        showProgress = true,
                        onRetry = null,
                    )

                    FavoriteReadState.ReadFailure -> HomeFavoriteMessage(
                        message = stringResource(R.string.unable_to_load_favorites),
                        showProgress = false,
                        onRetry = onRetryFavorites,
                    )

                    is FavoriteReadState.Readable -> {
                        val orderedModules = favoriteState.orderedModules
                        val hasFavorites = orderedModules?.isNotEmpty()
                            ?: favoriteState.aggregate.identities.isNotEmpty()
                        if (!hasFavorites && editMode && orderedModules == null) {
                            HomeFavoriteProvisionalList(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onAddProvisionalFavorites,
                                testTag = "favorite_provisional_add_0",
                                applicationDropHighlight =
                                    orchestration.applicationDragTargetSession
                                        ?.showsContainerHighlight(
                                        PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0,
                                    ) == true,
                                onBoundsInWindow = {
                                    orchestration.applicationContainerBoundsInWindow[
                                        PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0
                                    ] = it
                                    orchestration.applicationContainerDescriptors[
                                        PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0
                                    ] = ApplicationDragContainerDescriptor(
                                        key = PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0,
                                        type = FavoriteContainerType.VerticalList,
                                        axis = ApplicationDragAxis.Vertical,
                                        bounds = it,
                                    )
                                },
                                onDisposed = {
                                    orchestration.applicationContainerBoundsInWindow.remove(
                                        PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0,
                                    )
                                    orchestration.applicationContainerDescriptors.remove(
                                        PROVISIONAL_VERTICAL_LIST_DRAG_KEY_0,
                                    )
                                },
                            )
                        } else if (!hasFavorites && !editMode) {
                            Text(
                                text = stringResource(R.string.home_empty_favorites),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("home_favorites_empty"),
                            )
                        } else if (!editMode) {
                            HomeOrderedModuleComposition(
                                enterBatch = favoriteEnterBatch,
                                modules = orderedModules
                                    ?: (
                                            favoriteState.aggregate.verticalLists.map { container ->
                                                OrderedFavoriteModule(
                                                    id = container.id,
                                                    type = OrderedFavoriteModuleType.Vertical,
                                                    identities = container.identities,
                                                )
                                            } + favoriteState.aggregate.favoriteBars.map { container ->
                                                OrderedFavoriteModule(
                                                    id = container.id,
                                                    type = OrderedFavoriteModuleType.Ribbon,
                                                    identities = container.identities,
                                                )
                                            }
                                            ),
                                availabilityByIdentity = favoriteAvailability,
                                listState = favoriteListState,
                                nestedScrollConnection = favoriteNestedScrollConnection,
                                editMode = false,
                                selectionEnabled = false,
                                selectionInteractionEnabled = false,
                                selectedModuleId = null,
                                onSelectModule = {},
                                addEntriesEnabled = false,
                                onAddToModule = {},
                                onCreateVerticalModule = {},
                                onCreateRibbon = {},
                                onLaunchFavorite = onLaunchFavorite,
                                onLongPressFavorite = onLongPressFavorite,
                            )
                        } else if (orderedModules != null) {
                            val previewAggregate = orchestration.editTransaction.previewAggregate(
                                favoriteState.aggregate,
                            )
                            val styledModules = orderedModules
                                .withPresentationFrom(previewAggregate)
                                .map { module ->
                                    moduleStylePreview?.takeIf { it.id == module.id } ?: module
                                }
                            val displayedModules = orchestration.moduleDragSession?.remainingModules
                                ?: orchestration.editTransaction.pendingModuleOrder
                                ?: styledModules
                            HomeOrderedModuleComposition(
                                enterBatch = favoriteEnterBatch,
                                modules = displayedModules,
                                availabilityByIdentity = favoriteAvailability,
                                listState = favoriteListState,
                                nestedScrollConnection = null,
                                editMode = true,
                                selectionEnabled = stylePanelExpanded,
                                selectionInteractionEnabled = !applicationEditingSaving &&
                                        orchestration.editMutationJob?.isActive != true &&
                                        orchestration.moduleDragSession == null,
                                selectionVisualEnabled = !applicationEditingSaving &&
                                    orchestration.editMutationJob?.isActive != true,
                                selectedModuleId = selectedModuleId,
                                onSelectModule = onSelectModule,
                                addEntriesEnabled = !applicationEditingSaving &&
                                        orchestration.editMutationJob?.isActive != true &&
                                        orchestration.moduleDragSession == null &&
                                            !applicationMovementActive,
                                onRemoveFavorite = onRemoveApplication,
                                applicationMovement = orderedApplicationMovement,
                                onCommitApplicationOrder = { change ->
                                    onCommitApplicationOrder(change) {
                                        orderedApplicationMovement.complete(
                                            change = change
                                        )
                                    }
                                },
                                onAddToModule = { module ->
                                    when (module.type) {
                                        OrderedFavoriteModuleType.Vertical ->
                                            onAddFavoritesToList(module.id)

                                        OrderedFavoriteModuleType.Ribbon ->
                                            onAddFavoritesToBar(module.id)
                                    }
                                },
                                onCreateVerticalModule = onAddProvisionalFavorites,
                                onCreateRibbon = onAddProvisionalFavoriteBar,
                                onLaunchFavorite = {},
                                onLongPressFavorite = {},
                                moduleEdgeScrollDirection = moduleEdgeScrollDirection,
                                moduleInsertionIndex =
                                    orchestration.moduleDragSession?.insertionIndex,
                                onModuleBoundsInWindow = { id, bounds ->
                                    orchestration.moduleBoundsInWindow[id] = bounds
                                },
                                onModuleDisposed = { id ->
                                    orchestration.moduleBoundsInWindow.remove(id)
                                },
                                onModuleListBoundsInWindow = {
                                    orchestration.moduleListBoundsInWindow = it
                                },
                                onModuleDragStart = { module, touch ->
                                    orchestration.startModuleDrag(module, styledModules, touch)
                                },
                                onModuleDrag = orchestration::advanceModuleDrag,
                                onModuleDragEnd = orchestration::finishModuleDrag,
                                onModuleDragCancel = { orchestration.moduleDragSession = null },
                            )
                        } else if (editMode) {
                            val persistedEditAggregate =
                                orchestration.editTransaction.previewAggregate(
                                favoriteState.aggregate,
                            )
                            val editAggregate = orchestration.listDragSession?.let { session ->
                                persistedEditAggregate.copy(
                                    verticalLists = session.displayedLists,
                                )
                            } ?: persistedEditAggregate
                            val primaryContainer = editAggregate.verticalLists.getOrNull(0)
                            val companionContainer = editAggregate.verticalLists.getOrNull(1)
                            val primaryEditListState = primaryContainer?.let { container ->
                                orchestration.editListStates.getOrPut(container.id) {
                                    favoriteListState
                                }
                            } ?: favoriteListState
                            val companionEditListState = companionContainer?.let { container ->
                                orchestration.editListStates.getOrPut(container.id) {
                                    companionFavoriteListState
                                }
                            } ?: companionFavoriteListState
                            val primaryIdentities = primaryContainer?.identities.orEmpty()
                            val companionIdentities = companionContainer?.identities.orEmpty()
                            val activeSession = orchestration.dragSession
                                ?.takeIf { it.hasInGroupExchange }
                            val activeDraggedIdentity = orchestration.dragSession
                                ?.takeUnless { it.released }
                                ?.identity
                            val primaryDisplayed = activeSession?.displayedPrimary
                                ?: primaryIdentities
                            val companionDisplayed = activeSession?.displayedCompanion
                                ?: companionIdentities
                            val applicationTarget = orchestration.applicationDragTargetSession
                            // A release completes the current exchange or insertion when the touch
                            // point is inside either group; any other area restores the saved state.
                            val endDrag: () -> Unit = endDrag@{
                                val session = orchestration.dragSession
                                val applicationTarget = orchestration.applicationDragTargetSession
                                if (applicationTarget?.targetContainerType != null &&
                                    applicationTarget.targetContainerKey !=
                                    applicationTarget.sourceContainerKey
                                ) {
                                    orchestration.commitCrossContainerDrag(applicationTarget)
                                    return@endDrag
                                }
                                orchestration.applicationDragTargetSession = null
                                if (session != null &&
                                    (session.crossGroupTarget != null || session.hasInsertion) &&
                                    (
                                            orchestration.primaryListBoundsInWindow
                                                .contains(session.touchInWindow) ||
                                                    orchestration.companionListBoundsInWindow
                                                        .contains(session.touchInWindow)
                                            )
                                ) {
                                    val committed = session.committedComposition()
                                    val generation = session.generation
                                    orchestration.dragSession = session.copy(released = true)
                                    orchestration.commitEditAggregate(
                                        transform = { aggregate ->
                                            aggregate.replaceVerticalComposition(
                                                committed.first,
                                                committed.second,
                                            )
                                        },
                                        onCommitted = {
                                            if (orchestration.dragSession?.generation ==
                                                    generation
                                            ) {
                                                orchestration.dragSession = null
                                            }
                                        },
                                        onFailed = {
                                            if (orchestration.dragSession?.generation ==
                                                    generation
                                            ) {
                                                orchestration.dragSession = null
                                            }
                                        },
                                    )
                                } else if (session?.hasInGroupExchange == true) {
                                    // In-group exchanges are persisted as they happen. End the session
                                    // immediately so releasing near an edge cannot re-enable the legacy
                                    // edge-scroll effect during the release recomposition.
                                    orchestration.dragSession = null
                                } else {
                                    orchestration.dragSession = null
                                }
                            }
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val primaryContentHeight = primaryContainer?.let { container ->
                                    dimensionResource(container.listSize.rowHeightResource()) *
                                            container.identities.size
                                } ?: 0.dp
                                val companionContentHeight = companionContainer?.let { container ->
                                    dimensionResource(container.listSize.rowHeightResource()) *
                                            container.identities.size
                                } ?: 0.dp
                                val addControlHeight = if (editMode) {
                                    dimensionResource(R.dimen.home_favorite_add_control_height)
                                } else {
                                    0.dp
                                }
                                val contentHeight = (
                                        maxOf(
                                            primaryContentHeight,
                                            companionContentHeight,
                                        ).coerceAtLeast(addControlHeight)
                                        ).coerceAtMost(maxHeight)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(contentHeight),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        dimensionResource(R.dimen.home_favorite_group_spacing),
                                    ),
                                ) {
                                    if (primaryContainer != null) {
                                        HomeFavoriteList(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            identities = primaryDisplayed,
                                            availabilityByIdentity = favoriteAvailability,
                                            listState = primaryEditListState,
                                            nestedScrollConnection = favoriteNestedScrollConnection,
                                            editMode = editMode,
                                            compact = false,
                                            listSize = primaryContainer.listSize,
                                            draggedIdentity = activeDraggedIdentity,
                                            exchangeTargetIdentity =
                                                if (applicationTarget?.targetContainerKey ==
                                                    primaryContainer.applicationDragKey() &&
                                                    applicationTarget.targetMode ==
                                                    ApplicationDragTargetMode.Exchange
                                                ) {
                                                    applicationTarget.targetIdentity
                                                } else {
                                                    orchestration.dragSession?.crossGroupTarget
                                                },
                                            insertionBoundaryIndex =
                                                if (applicationTarget?.targetContainerKey ==
                                                    primaryContainer.applicationDragKey() &&
                                                    applicationTarget.targetMode ==
                                                    ApplicationDragTargetMode.Insertion
                                                ) {
                                                    applicationTarget.targetIndex
                                                } else {
                                                    orchestration.dragSession
                                                        ?.insertionBoundaryIn(companion = false)
                                                },
                                            onBoundsInWindow = {
                                                orchestration.primaryListBoundsInWindow = it
                                                orchestration.applicationContainerBoundsInWindow[
                                                    primaryContainer.applicationDragKey()
                                                ] = it
                                                orchestration.applicationContainerDescriptors[
                                                    primaryContainer.applicationDragKey()
                                                ] = primaryContainer.applicationDragDescriptor(it)
                                            },
                                            applicationDropHighlight =
                                                orchestration.applicationDragTargetSession
                                                    ?.showsContainerHighlight(
                                                    primaryContainer.applicationDragKey(),
                                                ) == true,
                                            applicationDragKey =
                                                primaryContainer.applicationDragKey(),
                                            applicationDragActive =
                                                orchestration.applicationDragTargetSession != null,
                                            applicationEdgeScroll = applicationEdgeScroll,
                                            onLaunchFavorite = onLaunchFavorite,
                                            onLongPressFavorite = onLongPressFavorite,
                                            onRemoveFavorite = { identity ->
                                                orchestration.removeFavoriteFromContainer(
                                                    primaryContainer.id,
                                                    identity,
                                                )
                                            },
                                            listIndex = 0,
                                            listCount = editAggregate.verticalLists.size,
                                            onChangeListSize = { size ->
                                                orchestration.commitEditAggregate(
                                                    { aggregate ->
                                                        aggregate.updateVerticalList(
                                                            primaryContainer.id,
                                                        ) {
                                                            it.copy(listSize = size)
                                                        }
                                                    },
                                                    favoriteListSizeMessage,
                                                )
                                            },
                                            onRemoveList = {
                                                orchestration.commitEditAggregate(
                                                    { aggregate ->
                                                        aggregate.updateVerticalList(
                                                            primaryContainer.id,
                                                        ) { null }
                                                    },
                                                    favoriteListRemovedMessage,
                                                    recordUndo = true,
                                                )
                                            },
                                            onAddFavorites = {
                                                onAddFavoritesToList(primaryContainer.id)
                                            },
                                            onContainerBoundsInWindow = {
                                                orchestration.primaryContainerBoundsInWindow = it
                                            },
                                            onContainerDisposed = {
                                                orchestration
                                                    .applicationContainerBoundsInWindow.remove(
                                                    primaryContainer.applicationDragKey(),
                                                )
                                                orchestration
                                                    .applicationContainerDescriptors.remove(
                                                    primaryContainer.applicationDragKey(),
                                                )
                                                orchestration.applicationItemBoundsInWindow.keys
                                                    .filter {
                                                        it.startsWith(
                                                            "${primaryContainer.applicationDragKey()}:",
                                                        )
                                                    }
                                                    .forEach(
                                                        orchestration
                                                            .applicationItemBoundsInWindow::remove,
                                                    )
                                            },
                                            onApplicationItemBounds = { identity, bounds ->
                                                orchestration.applicationItemBoundsInWindow[
                                                    "${primaryContainer.applicationDragKey()}:${identity.stableKey()}"
                                                ] = bounds
                                            },
                                            sourceListPlaceholder =
                                                orchestration.listDragSession
                                                    ?.sourceContainer?.id ==
                                                        primaryContainer.id,
                                            listExchangeHighlight =
                                                orchestration.listDragSession?.let { session ->
                                                session.sourceContainer.id != primaryContainer.id &&
                                                        orchestration.primaryContainerBoundsInWindow
                                                            .contains(session.touchInWindow)
                                            } == true,
                                            listDragActive = orchestration.listDragSession != null,
                                            onListDragStart = { touch ->
                                                orchestration.startListDrag(
                                                    container = primaryContainer,
                                                    index = 0,
                                                    bounds =
                                                        orchestration
                                                            .primaryContainerBoundsInWindow,
                                                    touchInWindow = touch,
                                                    listState = primaryEditListState,
                                                    displayedLists = editAggregate.verticalLists,
                                                )
                                            },
                                            onListDrag = orchestration::advanceListDrag,
                                            onListDragEnd = orchestration::finishListDrag,
                                            onListDragCancel = {
                                                orchestration.listDragSession = null
                                            },
                                            onDragStart = { identity, origin, size, touch ->
                                                dragGeneration += 1
                                                orchestration.dragSession = FavoriteDragSession(
                                                    generation = dragGeneration,
                                                    identity = identity,
                                                    iconSize = primaryContainer.iconSize,
                                                    textSize = primaryContainer.textSize,
                                                    originInWindow = origin,
                                                    size = size,
                                                    touchStartInWindow = touch,
                                                    displayedPrimary =
                                                        primaryIdentities,
                                                    displayedCompanion =
                                                        companionIdentities,
                                                )
                                                orchestration.applicationDragTargetSession =
                                                    ApplicationDragTargetSession(
                                                        sourceContainerKey =
                                                            primaryContainer.applicationDragKey(),
                                                        sourceIdentity = identity,
                                                        sourceContainerType =
                                                            FavoriteContainerType.VerticalList,
                                                        sourceAxis = ApplicationDragAxis.Vertical,
                                                        touchStartInWindow = touch,
                                                    )
                                            },
                                            onDrag = orchestration::advanceAndPersistDrag,
                                            onDragEnd = endDrag,
                                            onDragCancel = {
                                                orchestration.dragSession = null
                                                orchestration.applicationDragTargetSession = null
                                            },
                                        )
                                    }
                                    if (companionContainer != null) {
                                        HomeFavoriteList(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            identities = companionDisplayed,
                                            availabilityByIdentity = favoriteAvailability,
                                            listState = companionEditListState,
                                            nestedScrollConnection =
                                                companionFavoriteNestedScrollConnection,
                                            editMode = editMode,
                                            compact = false,
                                            listSize = companionContainer.listSize,
                                            draggedIdentity = activeDraggedIdentity,
                                            exchangeTargetIdentity =
                                                if (applicationTarget?.targetContainerKey ==
                                                    companionContainer.applicationDragKey() &&
                                                    applicationTarget.targetMode ==
                                                    ApplicationDragTargetMode.Exchange
                                                ) {
                                                    applicationTarget.targetIdentity
                                                } else {
                                                    orchestration.dragSession?.crossGroupTarget
                                                },
                                            insertionBoundaryIndex =
                                                if (applicationTarget?.targetContainerKey ==
                                                    companionContainer.applicationDragKey() &&
                                                    applicationTarget.targetMode ==
                                                    ApplicationDragTargetMode.Insertion
                                                ) {
                                                    applicationTarget.targetIndex
                                                } else {
                                                    orchestration.dragSession
                                                        ?.insertionBoundaryIn(companion = true)
                                                },
                                            onBoundsInWindow = {
                                                orchestration.companionListBoundsInWindow = it
                                                orchestration.applicationContainerBoundsInWindow[
                                                    companionContainer.applicationDragKey()
                                                ] = it
                                                orchestration.applicationContainerDescriptors[
                                                    companionContainer.applicationDragKey()
                                                ] = companionContainer.applicationDragDescriptor(it)
                                            },
                                            applicationDropHighlight =
                                                orchestration.applicationDragTargetSession
                                                    ?.showsContainerHighlight(
                                                    companionContainer.applicationDragKey(),
                                                ) == true,
                                            applicationDragKey =
                                                companionContainer.applicationDragKey(),
                                            applicationDragActive =
                                                orchestration.applicationDragTargetSession != null,
                                            applicationEdgeScroll = applicationEdgeScroll,
                                            onLaunchFavorite = onLaunchFavorite,
                                            onLongPressFavorite = onLongPressFavorite,
                                            onRemoveFavorite = { identity ->
                                                orchestration.removeFavoriteFromContainer(
                                                    companionContainer.id,
                                                    identity,
                                                )
                                            },
                                            listIndex = 1,
                                            listCount = editAggregate.verticalLists.size,
                                            onChangeListSize = { size ->
                                                orchestration.commitEditAggregate(
                                                    { aggregate ->
                                                        aggregate.updateVerticalList(
                                                            companionContainer.id,
                                                        ) {
                                                            it.copy(listSize = size)
                                                        }
                                                    },
                                                    favoriteListSizeMessage,
                                                )
                                            },
                                            onRemoveList = {
                                                orchestration.commitEditAggregate(
                                                    { aggregate ->
                                                        aggregate.updateVerticalList(
                                                            companionContainer.id,
                                                        ) { null }
                                                    },
                                                    favoriteListRemovedMessage,
                                                    recordUndo = true,
                                                )
                                            },
                                            onAddFavorites = {
                                                onAddFavoritesToList(companionContainer.id)
                                            },
                                            onContainerBoundsInWindow = {
                                                orchestration.companionContainerBoundsInWindow = it
                                            },
                                            onContainerDisposed = {
                                                orchestration
                                                    .applicationContainerBoundsInWindow.remove(
                                                    companionContainer.applicationDragKey(),
                                                )
                                                orchestration
                                                    .applicationContainerDescriptors.remove(
                                                    companionContainer.applicationDragKey(),
                                                )
                                                orchestration.applicationItemBoundsInWindow.keys
                                                    .filter {
                                                        it.startsWith(
                                                            "${companionContainer.applicationDragKey()}:",
                                                        )
                                                    }
                                                    .forEach(
                                                        orchestration
                                                            .applicationItemBoundsInWindow::remove,
                                                    )
                                            },
                                            onApplicationItemBounds = { identity, bounds ->
                                                orchestration.applicationItemBoundsInWindow[
                                                    "${companionContainer.applicationDragKey()}:${identity.stableKey()}"
                                                ] = bounds
                                            },
                                            sourceListPlaceholder =
                                                orchestration.listDragSession
                                                    ?.sourceContainer?.id ==
                                                        companionContainer.id,
                                            listExchangeHighlight =
                                                orchestration.listDragSession?.let { session ->
                                                session.sourceContainer.id != companionContainer.id &&
                                                        orchestration
                                                            .companionContainerBoundsInWindow
                                                            .contains(session.touchInWindow)
                                            } == true,
                                            listDragActive = orchestration.listDragSession != null,
                                            onListDragStart = { touch ->
                                                orchestration.startListDrag(
                                                    container = companionContainer,
                                                    index = 1,
                                                    bounds =
                                                        orchestration
                                                            .companionContainerBoundsInWindow,
                                                    touchInWindow = touch,
                                                    listState = companionEditListState,
                                                    displayedLists = editAggregate.verticalLists,
                                                )
                                            },
                                            onListDrag = orchestration::advanceListDrag,
                                            onListDragEnd = orchestration::finishListDrag,
                                            onListDragCancel = {
                                                orchestration.listDragSession = null
                                            },
                                            onDragStart = { identity, origin, size, touch ->
                                                dragGeneration += 1
                                                orchestration.dragSession = FavoriteDragSession(
                                                    generation = dragGeneration,
                                                    identity = identity,
                                                    iconSize = companionContainer.iconSize,
                                                    textSize = companionContainer.textSize,
                                                    originInWindow = origin,
                                                    size = size,
                                                    touchStartInWindow = touch,
                                                    displayedPrimary =
                                                        primaryIdentities,
                                                    displayedCompanion =
                                                        companionIdentities,
                                                )
                                                orchestration.applicationDragTargetSession =
                                                    ApplicationDragTargetSession(
                                                        sourceContainerKey =
                                                            companionContainer.applicationDragKey(),
                                                        sourceIdentity = identity,
                                                        sourceContainerType =
                                                            FavoriteContainerType.VerticalList,
                                                        sourceAxis = ApplicationDragAxis.Vertical,
                                                        touchStartInWindow = touch,
                                                    )
                                            },
                                            onDrag = orchestration::advanceAndPersistDrag,
                                            onDragEnd = endDrag,
                                            onDragCancel = {
                                                orchestration.dragSession = null
                                                orchestration.applicationDragTargetSession = null
                                            },
                                        )
                                    }
                                    if (companionContainer == null) {
                                        HomeFavoriteProvisionalList(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onClick = onAddProvisionalFavorites,
                                            testTag = "favorite_provisional_add_1",
                                            applicationDropHighlight =
                                                orchestration.applicationDragTargetSession
                                                    ?.showsContainerHighlight(
                                                    PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1,
                                                ) == true,
                                            onBoundsInWindow = {
                                                orchestration.applicationContainerBoundsInWindow[
                                                    PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1
                                                ] = it
                                                orchestration.applicationContainerDescriptors[
                                                    PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1
                                                ] = ApplicationDragContainerDescriptor(
                                                    key = PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1,
                                                    type = FavoriteContainerType.VerticalList,
                                                    axis = ApplicationDragAxis.Vertical,
                                                    bounds = it,
                                                )
                                            },
                                            onDisposed = {
                                                orchestration
                                                    .applicationContainerBoundsInWindow.remove(
                                                    PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1,
                                                )
                                                orchestration
                                                    .applicationContainerDescriptors.remove(
                                                    PROVISIONAL_VERTICAL_LIST_DRAG_KEY_1,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            (favoriteState as? FavoriteReadState.Readable)?.aggregate?.let { aggregate ->
                val renderedAggregate = if (editMode) {
                    orchestration.editTransaction.previewAggregate(aggregate)
                } else {
                    aggregate
                }
                val itemReorderedBars = orchestration.favoriteBarDragSession?.let { session ->
                    renderedAggregate.favoriteBars.map { bar ->
                        if (bar.id == session.barId) {
                            bar.copy(identities = session.displayedIdentities)
                        } else {
                            bar
                        }
                    }
                } ?: renderedAggregate.favoriteBars
                val renderedBars = orchestration.favoriteBarContainerDragSession?.displayedBars
                    ?: itemReorderedBars
                if (editMode &&
                    favoriteState.orderedModules == null &&
                    (renderedBars.isNotEmpty() || renderedBars.size < 5)
                ) {
                    if (aggregate.verticalLists.isNotEmpty() || editMode) {
                        Spacer(Modifier.height(dimensionResource(R.dimen.home_module_spacing)))
                    }
                    HomeFavoriteRibbon(
                        favoriteRibbons = renderedBars,
                        availabilityByIdentity = favoriteAvailability,
                        editMode = editMode,
                        layoutRegistry = orchestration.favoriteRibbonLayoutRegistry,
                        dragState = HomeFavoriteRibbonDragState(
                            applicationDropTargetKey =
                                orchestration.applicationDragTargetSession?.targetContainerKey,
                            applicationEdgeScroll = applicationEdgeScroll,
                            applicationDropTargetIdentity =
                                orchestration.applicationDragTargetSession?.targetIdentity,
                            applicationDropTargetMode =
                                orchestration.applicationDragTargetSession?.targetMode,
                            applicationDropTargetIndex =
                                orchestration.applicationDragTargetSession?.targetIndex,
                            draggedIdentity = orchestration.favoriteBarDragSession?.identity,
                            draggedRibbonId =
                                orchestration.favoriteBarContainerDragSession?.sourceContainer?.id,
                            highlightedRibbonId =
                                orchestration.favoriteBarContainerDragSession?.targetContainerId,
                        ),
                        actions = object : HomeFavoriteRibbonActions {
                            override fun launchFavorite(availability: FavoriteAvailability) {
                                onLaunchFavorite(availability)
                            }

                            override fun longPressFavorite(entry: LaunchableEntry) {
                                onLongPressFavorite(entry)
                            }

                            override fun addFavorites(ribbonId: String) {
                                onAddFavoritesToBar(ribbonId)
                            }

                            override fun removeFavorite(
                                ribbonId: String,
                                identity: LaunchableIdentity,
                            ) {
                                orchestration.removeFavoriteFromContainer(
                                    containerId = ribbonId,
                                    identity = identity,
                                )
                            }

                            override fun removeRibbon(ribbonId: String) {
                                orchestration.removeFavoriteBar(containerId = ribbonId)
                            }
                        },
                        dragActions = object : HomeFavoriteRibbonDragActions {
                            override fun startRibbonDrag(
                                ribbon: FavoriteContainer,
                                index: Int,
                                touch: Offset,
                            ) {
                                orchestration.startFavoriteBarContainerDrag(
                                    bar = ribbon,
                                    index = index,
                                    touchInWindow = touch,
                                    displayedBars = renderedBars,
                                )
                            }

                            override fun dragRibbon(delta: Offset) {
                                orchestration.advanceFavoriteBarContainerDrag(amount = delta)
                            }

                            override fun finishRibbonDrag() {
                                orchestration.finishFavoriteBarContainerDrag()
                            }

                            override fun cancelRibbonDrag() {
                                orchestration.favoriteBarContainerDragSession = null
                            }

                            override fun startApplicationDrag(
                                ribbon: FavoriteContainer,
                                identity: LaunchableIdentity,
                                origin: Offset,
                                size: IntSize,
                                touch: Offset,
                            ) {
                                dragGeneration += 1
                                orchestration.favoriteBarDragSession = FavoriteBarDragSession(
                                    generation = dragGeneration,
                                    barId = ribbon.id,
                                    identity = identity,
                                    displayedIdentities = ribbon.identities,
                                    originInWindow = origin,
                                    size = size,
                                )
                                orchestration.applicationDragTargetSession =
                                    ApplicationDragTargetSession(
                                    sourceContainerKey = ribbon.applicationDragKey(),
                                    sourceIdentity = identity,
                                    sourceContainerType = FavoriteContainerType.FavoriteBar,
                                    sourceAxis = ApplicationDragAxis.Horizontal,
                                    touchStartInWindow = touch,
                                )
                            }

                            override fun dragApplication(delta: Offset) {
                                orchestration.advanceAndPersistFavoriteBarDrag(amount = delta)
                            }

                            override fun finishApplicationDrag() {
                                val target = orchestration.applicationDragTargetSession
                                if (target?.targetContainerType != null &&
                                    target.targetContainerKey != target.sourceContainerKey
                                ) {
                                    orchestration.commitCrossContainerDrag(targetSession = target)
                                } else {
                                    orchestration.favoriteBarDragSession = null
                                    orchestration.applicationDragTargetSession = null
                                }
                            }

                            override fun cancelApplicationDrag() {
                                orchestration.favoriteBarDragSession = null
                                orchestration.applicationDragTargetSession = null
                            }
                        },
                    )
                }
            }
            if (barVisible && persistedBar != null) {
                HomeDockSurface(
                    state = persistedBar,
                    safeWidthDp = homeContentWidthDp,
                    expanded = homeBottomAppBarExpanded,
                    editMode = editMode,
                    availabilityByIdentity = favoriteAvailability,
                    mutationEnabled = !applicationEditingSaving,
                    onExpandedChange = onHomeBottomAppBarExpandedChange,
                    onLaunch = onLaunchFavorite,
                    onLongPress = onLongPressFavorite,
                    onRemove = onRemoveApplication,
                    onAdd = onAddToHomeBottomAppBar,
                    onReorder = onReorderHomeBottomAppBar,
                    onDragPreviewChange = { homeDockDragPreview = it },
                )
            }
        }
        HomeFavoriteExitOverlay(
            owner = favoriteEnterBatch.exitTransitions,
            rootOrigin = dragRootOriginInWindow
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    action = {
                        data.visuals.actionLabel?.let(
                            block = { label ->
                                TextButton(
                                    enabled = !applicationEditingSaving &&
                                        orchestration.editMutationJob?.isActive != true &&
                                            !applicationMovementActive,
                                    onClick = { data.performAction() },
                                    content = {
                                        Text(
                                            text = label,
                                            color = MaterialTheme.colorScheme.inversePrimary
                                        )
                                    },
                                )
                            },
                        )
                    },
                    content = { Text(text = data.visuals.message) },
                )
            },
        )
        orchestration.listDragSession?.let { session ->
            HomeFavoriteListDragPreview(
                session = session,
                availabilityByIdentity = favoriteAvailability,
                rootOriginInWindow = dragRootOriginInWindow,
            )
        }
        orchestration.dragSession?.let { session ->
            if (!session.released) {
                HomeFavoriteDragPreview(
                    session = session,
                    availability = favoriteAvailability[session.identity]
                        ?: FavoriteAvailability.Unknown(null),
                    rootOriginInWindow = dragRootOriginInWindow,
                )
            }
        }
        orchestration.favoriteBarDragSession?.let { session ->
            HomeFavoriteBarDragPreview(
                session = session,
                availability = favoriteAvailability[session.identity]
                    ?: FavoriteAvailability.Unknown(null),
                rootOriginInWindow = dragRootOriginInWindow,
            )
        }
        orchestration.favoriteBarContainerDragSession?.let { session ->
            HomeFavoriteBarContainerDragPreview(
                session = session,
                availabilityByIdentity = favoriteAvailability,
                rootOriginInWindow = dragRootOriginInWindow,
            )
        }
        HomeApplicationMovementOverlay(
            movement = orderedApplicationMovement,
            rootOrigin = dragRootOriginInWindow
        )
        orchestration.moduleDragSession?.let { session ->
            HomeModuleDragPreview(
                session = session,
                rootOriginInWindow = dragRootOriginInWindow,
            )
        }
        homeDockDragPreview?.let { preview ->
            HomeDockDragGhost(
                preview = preview,
                rootOriginInWindow = dragRootOriginInWindow,
            )
        }
    }
}


internal data class FavoriteBarContainerDragSession(
    val sourceContainer: FavoriteContainer,
    val currentIndex: Int,
    val originInWindow: Offset,
    val size: IntSize,
    val touchStartInWindow: Offset,
    val displayedBars: List<FavoriteContainer>,
    val initialDisplayedBars: List<FavoriteContainer>,
    val visibleIdentities: List<LaunchableIdentity>,
    val visibleScrollOffset: Int,
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
    val targetContainerId: String? = null,
    val released: Boolean = false,
    val exchangeGeneration: Int = 0,
    val delta: Offset = Offset.Zero,
) {
    val touchInWindow: Offset get() = touchStartInWindow + delta
}


internal data class FavoriteListDragSession(
    val sourceContainer: FavoriteContainer,
    val currentIndex: Int,
    val originInWindow: Offset,
    val size: IntSize,
    val touchStartInWindow: Offset,
    val displayedLists: List<FavoriteContainer>,
    val initialDisplayedLists: List<FavoriteContainer>,
    val visibleIdentities: List<LaunchableIdentity>,
    val visibleScrollOffset: Int,
    val released: Boolean = false,
    val exchangeGeneration: Int = 0,
    val delta: Offset = Offset.Zero,
) {
    val touchInWindow: Offset get() = touchStartInWindow + delta
}

internal data class FavoriteDragAnchor(
    val identity: LaunchableIdentity,
    val handleBoundsInWindow: Rect = Rect.Zero,
    val rowOriginInWindow: Offset = Offset.Zero,
    val rowSize: IntSize = IntSize.Zero,
)
