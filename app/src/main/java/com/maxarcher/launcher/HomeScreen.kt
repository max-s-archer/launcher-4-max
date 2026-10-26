package com.maxarcher.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.ui.drawer.DrawerDragDrop
import com.maxarcher.launcher.ui.drawer.DrawerDragJourney
import com.maxarcher.launcher.ui.home.HomeFavoriteEditOrchestration
import com.maxarcher.launcher.ui.home.components.HomeApplicationMovementOverlay
import com.maxarcher.launcher.ui.home.components.HomeDrawerDragHandling
import com.maxarcher.launcher.ui.home.components.HomeBasicInformation
import com.maxarcher.launcher.ui.home.components.HomeDockDragGhost
import com.maxarcher.launcher.ui.home.components.HomeDockDragPreview
import com.maxarcher.launcher.ui.home.components.HomeDockSurface
import com.maxarcher.launcher.ui.home.components.HomeDefaultLauncherPrompt
import com.maxarcher.launcher.ui.home.components.HomeFavoriteExitOverlay
import com.maxarcher.launcher.ui.home.components.HomeFavoriteMessage
import com.maxarcher.launcher.ui.home.components.HomeModuleDragPreview
import com.maxarcher.launcher.ui.home.components.HomeOrderedModuleComposition
import com.maxarcher.launcher.ui.home.components.rememberHomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.withPresentationFrom
import com.maxarcher.launcher.ui.style.HomeModuleStylePanel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    favoriteState: FavoriteReadState = FavoriteReadState.Readable(emptyList()),
    favoriteAvailability: Map<LaunchableIdentity, FavoriteAvailability> = emptyMap(),
    favoriteListState: LazyListState = rememberLazyListState(),
    favoriteNestedScrollConnection: NestedScrollConnection? = null,
    editMode: Boolean = false,
    openStyleModuleId: String? = null,
    applicationEditingSaving: Boolean = false,
    onRemoveApplication: (LaunchableIdentity) -> Unit = {},
    onCommitApplicationOrder: (
        change: ApplicationOrderChange,
        onComplete: () -> Unit,
    ) -> Unit = { _, complete -> complete() },
    removalSnackbarHostState: SnackbarHostState? = null,
    onRetryFavorites: () -> Unit = {},
    onRequestEditMode: () -> Unit = {},
    onToggleModuleStylePanel: (String) -> Unit = {},
    onLaunchFavorite: (FavoriteAvailability) -> Unit = {},
    onLongPressFavorite: (LaunchableEntry) -> Unit = {},
    onAddFavoritesToList: (String) -> Unit = {},
    onAddProvisionalFavorites: () -> Unit = {},
    onAddFavoritesToBar: (String) -> Unit = {},
    onAddProvisionalFavoriteBar: () -> Unit = {},
    onAddToHomeBottomAppBar: () -> Unit = {},
    homeBottomAppBarExpanded: Boolean = false,
    onHomeBottomAppBarExpandedChange: (Boolean) -> Unit = {},
    onCommitFavoriteComposition: suspend (
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ) -> OrderedFavoriteAggregate? = { transform -> transform(OrderedFavoriteAggregate()) },
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
        openStyleModuleId = openStyleModuleId,
        favoriteState = favoriteState,
        favoriteAvailability = favoriteAvailability,
        onCommit = onDrawerDragCommit,
        onFinished = onDrawerDragJourneyFinished,
    )
    val favoriteEnterBatch = rememberHomeFavoriteEnterBatch(
        modules = (favoriteState as? FavoriteReadState.Readable)?.orderedModules,
    )
    val applicationMovementActive = orderedApplicationMovement.activeIdentity != null
    BackHandler(enabled = applicationMovementActive) {
        orderedApplicationMovement.cancel()
    }
    val currentFavoriteState by rememberUpdatedState(favoriteState)
    val snackbarHostState = removalSnackbarHostState ?: remember { SnackbarHostState() }
    val editScope = rememberCoroutineScope()
    var dragRootOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var homeDockDragPreview by remember {
        mutableStateOf<HomeDockDragPreview?>(null)
    }
    val edgeScrollBandPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_favorite_edge_scroll_band).toPx()
    }
    val edgeScrollSpeedPxPerSecond = with(LocalDensity.current) {
        integerResource(R.integer.home_favorite_edge_scroll_dp_per_second).dp.toPx()
    }
    val edgeScrollStartDelayMillis =
        integerResource(R.integer.home_favorite_edge_scroll_start_delay_ms).toLong()
    val undoLabel = stringResource(R.string.undo)
    val undoUnavailableMessage = stringResource(R.string.favorite_undo_unavailable)
    val wholeModuleRemovalMessage = stringResource(R.string.home_module_removed)
    val moduleStyleSaveFailureMessage = stringResource(
        R.string.unable_to_save_module_style,
    )
    val moduleOrderSaveFailureMessage = stringResource(
        R.string.unable_to_save_module_order,
    )
    val moduleNameSaveFailureMessage = stringResource(R.string.unable_to_save_module_name)

    val orchestration = remember {
        HomeFavoriteEditOrchestration(
            orderedApplicationMovement = orderedApplicationMovement,
            snackbarHostState = snackbarHostState,
            editScope = editScope,
            context = context,
        )
    }
    orchestration.editMode = editMode
    orchestration.applicationEditingSaving = applicationEditingSaving
    orchestration.openStyleModuleId = openStyleModuleId
    orchestration.favoriteState = currentFavoriteState
    orchestration.favoriteAvailability = favoriteAvailability
    orchestration.undoLabel = undoLabel
    orchestration.undoUnavailableMessage = undoUnavailableMessage
    orchestration.wholeModuleRemovalMessage = wholeModuleRemovalMessage
    orchestration.moduleStyleSaveFailureMessage = moduleStyleSaveFailureMessage
    orchestration.moduleOrderSaveFailureMessage = moduleOrderSaveFailureMessage
    orchestration.moduleNameSaveFailureMessage = moduleNameSaveFailureMessage
    orchestration.onCommitFavoriteComposition = onCommitFavoriteComposition
    orchestration.onCommitModuleOrder = onCommitModuleOrder

    LaunchedEffect(editMode) {
        if (!editMode) {
            val preserveUndo = orchestration.editTransaction.undoSurvivesEditSession
            orchestration.editTransaction.leave()
            orchestration.cancelActiveDragSessions()
            orchestration.editMutationJob?.cancel()
            if (!preserveUndo) snackbarHostState.currentSnackbarData?.dismiss()
        } else {
            orchestration.editTransaction.enter(
                (currentFavoriteState as? FavoriteReadState.Readable)?.orderedAggregate,
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

    LaunchedEffect(favoriteState, editMode) {
        val readable = favoriteState as? FavoriteReadState.Readable
            ?: return@LaunchedEffect
        val currentAggregate = readable.orderedAggregate ?: return@LaunchedEffect
        val committed = orchestration.editTransaction.committedAggregate ?: return@LaunchedEffect
        if ((editMode || orchestration.editTransaction.undoAggregate != null) &&
            currentAggregate != committed &&
            orchestration.editMutationJob?.isActive != true &&
            // The saved application order is the pending handoff, not an external edit.
            orderedApplicationMovement.pendingChange == null
        ) {
            orchestration.cancelActiveDragSessions()
            if (orchestration.editTransaction.reconcileExternal(currentAggregate)) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned { dragRootOriginInWindow = it.positionInWindow() },
    ) {
        val contentPadding = dimensionResource(R.dimen.home_content_padding)
        var moduleStylePreview by remember { mutableStateOf<OrderedFavoriteModule?>(null) }
        LaunchedEffect(editMode, openStyleModuleId) {
            if (!editMode || moduleStylePreview?.id != openStyleModuleId) moduleStylePreview = null
        }
        val styleSaving = applicationEditingSaving ||
            orchestration.editMutationJob?.isActive == true ||
                orchestration.moduleDragSession != null
        val homeContentWidthDp = maxWidth.value
        val persistedBar = (favoriteState as? FavoriteReadState.Readable)
            ?.orderedAggregate
            ?.homeBottomAppBar
        val barVisible = persistedBar?.enabled == true &&
            (editMode || persistedBar.identities.isNotEmpty())
        Column(modifier = Modifier.fillMaxSize()) {
            // The basic-information region stays visible in edit mode; edit mode adds no dock.
            HomeBasicInformation(
                editMode = editMode,
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
            if (showDefaultLauncherPrompt && !editMode) {
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
                        if (!hasFavorites && !editMode) {
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
                                addEntriesEnabled = false,
                                onAddToModule = {},
                                onCreateVerticalModule = {},
                                onCreateRibbon = {},
                                onLaunchFavorite = onLaunchFavorite,
                                onLongPressFavorite = onLongPressFavorite,
                            )
                        } else if (orderedModules != null) {
                            val previewAggregate = orchestration.editTransaction.previewAggregate(
                                favoriteState.orderedAggregate
                                    ?: OrderedFavoriteAggregate(modules = orderedModules),
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
                                openStyleModuleId = openStyleModuleId,
                                headerActionsEnabled = !styleSaving && !applicationMovementActive,
                                editSaving = applicationEditingSaving ||
                                    orchestration.editMutationJob?.isActive == true,
                                // Name saves queue behind the current edit save, so typing stays
                                // open; an unresolved favorite save makes the field read-only.
                                moduleNameEditable = !applicationEditingSaving,
                                moduleNameRestoreKey = orchestration.moduleNameRestoreCount,
                                onToggleModuleStyle = { moduleId ->
                                    if (!styleSaving && !applicationMovementActive) {
                                        onToggleModuleStylePanel(moduleId)
                                    }
                                },
                                onRenameModule = orchestration::commitModuleName,
                                onToggleModuleNameVisible = { module ->
                                    orchestration.commitModuleNameVisibility(
                                        moduleId = module.id,
                                        visible = !module.nameVisible,
                                    )
                                },
                                onRemoveModule = { module ->
                                    orchestration.removeModule(module.id)
                                },
                                stylePanel = { module ->
                                    HomeModuleStylePanelSlot(
                                        module = module,
                                        enabled = !styleSaving,
                                        orchestration = orchestration,
                                        onPreview = { moduleStylePreview = it },
                                    )
                                },
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
                        }
                    }
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
                    onDragPreviewChange = { homeDockDragPreview = it },
                    onCommitApplicationOrder = onCommitApplicationOrder,
                    movement = orderedApplicationMovement,
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
        HomeApplicationMovementOverlay(
            movement = orderedApplicationMovement,
            rootOrigin = dragRootOriginInWindow
        )
        orchestration.moduleDragSession?.let { session ->
            HomeModuleDragPreview(
                session = session,
                rootOriginInWindow = dragRootOriginInWindow,
                // An open panel stays with its owning module while it moves.
                stylePanel = { module ->
                    HomeModuleStylePanelSlot(
                        module = module,
                        enabled = false,
                        orchestration = orchestration,
                        onPreview = {},
                    )
                },
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

/** Connects one module's inline style settings panel to its preview and complete-style saves. */
@Composable
private fun HomeModuleStylePanelSlot(
    module: OrderedFavoriteModule,
    enabled: Boolean,
    orchestration: HomeFavoriteEditOrchestration,
    onPreview: (OrderedFavoriteModule?) -> Unit,
) {
    HomeModuleStylePanel(
        module = module,
        enabled = enabled,
        onPreviewSizes = { iconSize, textSize ->
            onPreview(module.copy(iconSize = iconSize, textSize = textSize))
        },
        onCommitIconSize = { iconSize ->
            onPreview(null)
            orchestration.commitVerticalModuleStyle(module.id) { it.copy(iconSize = iconSize) }
        },
        onCommitTextSize = { textSize ->
            onPreview(null)
            orchestration.commitVerticalModuleStyle(module.id) { it.copy(textSize = textSize) }
        },
        onChangeNamePlacement = { placement ->
            orchestration.commitVerticalModuleStyle(module.id) { container ->
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
        },
        onChangeItemsPerRow = { count ->
            orchestration.commitVerticalModuleStyle(module.id) { it.copy(itemsPerRow = count) }
        },
    )
}
