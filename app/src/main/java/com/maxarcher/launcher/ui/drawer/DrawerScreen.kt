package com.maxarcher.launcher.ui.drawer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.maxarcher.launcher.ui.drawer.components.DrawerAlphabetIndex
import com.maxarcher.launcher.ui.drawer.components.DrawerIndexBubble
import com.maxarcher.launcher.ui.drawer.components.DrawerNavigationTopBar
import com.maxarcher.launcher.ui.drawer.components.DrawerSearchTopBar
import com.maxarcher.launcher.ui.drawer.components.DrawerTopBarDivider
import java.util.Locale
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.LaunchableEntry
import com.maxarcher.launcher.LaunchableEntryLauncher
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.LaunchableInventoryCoordinator
import com.maxarcher.launcher.LaunchableInventoryLoader
import com.maxarcher.launcher.LaunchableInventorySnapshot
import com.maxarcher.launcher.LaunchableInventoryState
import com.maxarcher.launcher.R
import com.maxarcher.launcher.RapidActivationGuard
import com.maxarcher.launcher.drawerSectionsFor

private enum class DrawerLoadTrigger {
    Initial,
    ManualRetry,
    LiveUpdate,
    LaunchFailureRefresh,
}

@Composable
internal fun DrawerScreen(
    inventoryLoader: LaunchableInventoryLoader,
    inventoryCoordinator: LaunchableInventoryCoordinator = remember(inventoryLoader) {
        LaunchableInventoryCoordinator(inventoryLoader)
    },
    initialLoadHandledExternally: Boolean = false,
    entryLauncher: LaunchableEntryLauncher = LaunchableEntryLauncher { false },
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    active: Boolean = true,
    displaySettings: DrawerDisplaySettings = DrawerDisplaySettings(),
    displaySettingsReady: Boolean = true,
    displaySettingsMutationEnabled: Boolean = true,
    onChangeDisplaySettings: (DrawerDisplaySettings) -> Unit = {},
    onLongPress: (LaunchableEntry) -> Unit = {},
    onExternalLaunch: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    favoriteSelectionTarget: String? = null,
    favoriteSelection: List<LaunchableIdentity> = emptyList(),
    favoriteMembership: Set<LaunchableIdentity> = emptySet(),
    favoriteSelectionSaving: Boolean = false,
    favoriteSelectionLimit: Int? = null,
    favoriteAvailability: Map<LaunchableIdentity, FavoriteAvailability> = emptyMap(),
    onToggleFavoriteSelection: (LaunchableIdentity) -> Unit = {},
    onCancelFavoriteSelection: () -> Unit = {},
    onConfirmFavoriteSelection: () -> Unit = {},
    onDragToFavoriteStart: (DrawerDragJourney) -> Boolean = { false },
    onDragToFavoriteMove: (Offset) -> Unit = {},
    onDragToFavoriteEnd: (Offset?, Boolean) -> Unit = { _, _ -> },
) {
    // Live drag preview of the background opacity; null means render the persisted value.
    // It never reaches the display-settings store: the release commits exactly one save.
    var backgroundOpacityPreview by remember { mutableStateOf<Int?>(null) }
    var applicationSizesPreview by remember { mutableStateOf<DrawerDisplaySettings?>(null) }
    val renderedDisplaySettings = applicationSizesPreview ?: displaySettings
    DrawerBackgroundSurface(
        opacity = backgroundOpacityPreview ?: displaySettings.backgroundOpacity,
    ) {
        var loadRequest by remember { mutableIntStateOf(0) }
        var loadTrigger by remember { mutableStateOf(DrawerLoadTrigger.Initial) }
        var hasBeenActive by remember { mutableStateOf(false) }
        val state by inventoryCoordinator.state.collectAsStateWithLifecycle()
        val activationGuard = remember { RapidActivationGuard() }
        val context = LocalContext.current
        val locale = LocalConfiguration.current.locales[0]
        val launchFailureMessage = stringResource(R.string.application_unable_to_open)
        val currentState by rememberUpdatedState(state)
        var previousContent by remember { mutableStateOf<LaunchableInventorySnapshot?>(null) }
        var searchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var ordinaryPosition by remember { mutableStateOf<DrawerListPosition?>(null) }
        var displaySettingsPosition by remember { mutableStateOf<DrawerListPosition?>(null) }
        var positionedDisplaySettings by remember { mutableStateOf(displaySettings) }
        var displaySettingsPanelVisible by remember { mutableStateOf(false) }
        val searchFocusRequester = remember { FocusRequester() }
        val searchScope = rememberCoroutineScope()
        val keyboardController = LocalSoftwareKeyboardController.current

        // One hide path for every external removal: the panel leaves the composition and
        // the drag preview falls back to the persisted state in the same step.
        fun hideDisplaySettingsPanel() {
            displaySettingsPanelVisible = false
            backgroundOpacityPreview = null
            applicationSizesPreview = null
        }

        LaunchedEffect(key1 = active) {
            if (!active) {
                searchActive = false
                searchQuery = ""
                ordinaryPosition = null
                displaySettingsPosition = null
                hideDisplaySettingsPanel()
            }
        }

        LaunchedEffect(key1 = favoriteSelectionTarget) {
            if (favoriteSelectionTarget != null) {
                hideDisplaySettingsPanel()
            }
        }

        LaunchedEffect(key1 = state is LaunchableInventoryState.Content) {
            if (state !is LaunchableInventoryState.Content) {
                hideDisplaySettingsPanel()
            }
        }

        LaunchedEffect(inventoryLoader, loadRequest) {
            if (loadRequest == 0 &&
                (initialLoadHandledExternally || state is LaunchableInventoryState.Content)
            ) {
                return@LaunchedEffect
            }
            val positionBeforeRefresh = if (
                loadTrigger == DrawerLoadTrigger.LiveUpdate ||
                loadTrigger == DrawerLoadTrigger.LaunchFailureRefresh
            ) {
                (state as? LaunchableInventoryState.Content)?.let { content ->
                    captureDrawerListPosition(
                        sections = content.snapshot.drawerSectionsForCurrentMode(
                            locale = locale,
                            searchActive = searchActive,
                            searchQuery = searchQuery,
                        ),
                        firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                        itemsPerRow = displaySettings.itemsPerRow,
                        anchorPresentation = displaySettings.sectionAnchorPresentation,
                    )
                }
            } else {
                null
            }
            inventoryCoordinator.load(
                showLoading = loadTrigger != DrawerLoadTrigger.LiveUpdate &&
                    loadTrigger != DrawerLoadTrigger.LaunchFailureRefresh,
                preserveContentOnFailure = loadTrigger == DrawerLoadTrigger.LaunchFailureRefresh,
            )
            val updatedState = inventoryCoordinator.state.value

            if (positionBeforeRefresh != null && updatedState is LaunchableInventoryState.Content) {
                val restorationTarget = resolveDrawerRestorationTarget(
                    position = positionBeforeRefresh,
                    sections = updatedState.snapshot.drawerSectionsForCurrentMode(
                        locale = locale,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                    ),
                    itemsPerRow = displaySettings.itemsPerRow,
                    anchorPresentation = displaySettings.sectionAnchorPresentation,
                )
                if (restorationTarget != null) {
                    withFrameNanos { }
                    listState.scrollToItem(
                        index = restorationTarget.itemIndex,
                        scrollOffset = restorationTarget.scrollOffset,
                    )
                }
            }
        }

        LaunchedEffect(inventoryLoader, active, initialLoadHandledExternally) {
            if (active && !initialLoadHandledExternally) {
                if (!hasBeenActive) {
                    hasBeenActive = true
                    return@LaunchedEffect
                }
                when (currentState) {
                    is LaunchableInventoryState.Content -> {
                        loadTrigger = DrawerLoadTrigger.LiveUpdate
                        loadRequest += 1
                    }

                    is LaunchableInventoryState.Error -> {
                        loadTrigger = DrawerLoadTrigger.Initial
                        loadRequest += 1
                    }

                    LaunchableInventoryState.Loading -> Unit
                }
            }
        }

        LaunchedEffect(state, initialLoadHandledExternally) {
            if (!initialLoadHandledExternally) return@LaunchedEffect
            val content = state as? LaunchableInventoryState.Content ?: return@LaunchedEffect
            val oldContent = previousContent
            previousContent = content.snapshot
            if (oldContent == null) return@LaunchedEffect
            val position = captureDrawerListPosition(
                sections = oldContent.drawerSectionsForCurrentMode(
                    locale = locale,
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                ),
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                itemsPerRow = displaySettings.itemsPerRow,
                anchorPresentation = displaySettings.sectionAnchorPresentation,
            ) ?: return@LaunchedEffect
            val restorationTarget = resolveDrawerRestorationTarget(
                position = position,
                sections = content.snapshot.drawerSectionsForCurrentMode(
                    locale = locale,
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                ),
                itemsPerRow = displaySettings.itemsPerRow,
                anchorPresentation = displaySettings.sectionAnchorPresentation,
            ) ?: return@LaunchedEffect
            withFrameNanos { }
            listState.scrollToItem(
                index = restorationTarget.itemIndex,
                scrollOffset = restorationTarget.scrollOffset,
            )
        }

        DisposableEffect(inventoryLoader, active, initialLoadHandledExternally) {
            val observation = if (active && !initialLoadHandledExternally) {
                inventoryCoordinator.observe {
                    if (currentState is LaunchableInventoryState.Content) {
                        loadTrigger = DrawerLoadTrigger.LiveUpdate
                        loadRequest += 1
                    }
                }
            } else {
                null
            }
            onDispose {
                observation?.stop()
            }
        }

        if (!displaySettingsReady) {
            if (favoriteSelectionTarget == null) {
                DrawerOrdinaryMessage(
                    modifier = modifier,
                    message = stringResource(R.string.drawer_loading_applications),
                    showProgress = true,
                    action = null,
                    testTag = "drawer_loading",
                    onNavigateBack = onNavigateBack,
                )
            } else {
                DrawerSelectionMessage(
                    modifier = modifier,
                    target = favoriteSelectionTarget,
                    message = stringResource(R.string.drawer_loading_applications),
                    showProgress = true,
                    retry = null,
                    selection = favoriteSelection,
                    saving = favoriteSelectionSaving,
                    onCancel = onCancelFavoriteSelection,
                    onConfirm = onConfirmFavoriteSelection,
                )
            }
            return@DrawerBackgroundSurface
        }

        when (val currentState = state) {
            LaunchableInventoryState.Loading -> if (favoriteSelectionTarget == null) {
                DrawerOrdinaryMessage(
                    modifier = modifier,
                    message = stringResource(R.string.drawer_loading_applications),
                    showProgress = true,
                    action = null,
                    testTag = "drawer_loading",
                    onNavigateBack = onNavigateBack,
                )
            } else {
                DrawerSelectionMessage(
                    modifier = modifier,
                    target = favoriteSelectionTarget,
                    message = stringResource(R.string.drawer_loading_applications),
                    showProgress = true,
                    retry = null,
                    selection = favoriteSelection,
                    saving = favoriteSelectionSaving,
                    onCancel = onCancelFavoriteSelection,
                    onConfirm = onConfirmFavoriteSelection,
                )
            }

            is LaunchableInventoryState.Error -> if (favoriteSelectionTarget == null) {
                DrawerOrdinaryMessage(
                    modifier = modifier,
                    message = stringResource(R.string.drawer_unable_to_load_applications),
                    showProgress = false,
                    showErrorIcon = true,
                    action = {
                        TextButton(
                            onClick = {
                                loadTrigger = DrawerLoadTrigger.ManualRetry
                                loadRequest += 1
                            },
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    },
                    testTag = "drawer_error",
                    onNavigateBack = onNavigateBack,
                )
            } else {
                DrawerSelectionMessage(
                    modifier = modifier,
                    target = favoriteSelectionTarget,
                    message = stringResource(R.string.drawer_unable_to_load_applications),
                    showProgress = false,
                    retry = {
                        loadTrigger = DrawerLoadTrigger.ManualRetry
                        loadRequest += 1
                    },
                    selection = favoriteSelection,
                    saving = favoriteSelectionSaving,
                    onCancel = onCancelFavoriteSelection,
                    onConfirm = onConfirmFavoriteSelection,
                )
            }

            is LaunchableInventoryState.Content -> if (favoriteSelectionTarget == null) {
                val completeSections = currentState.snapshot.drawerSectionsFor(locale)
                val visibleSections = remember(
                    key1 = completeSections,
                    key2 = searchActive,
                    key3 = searchQuery,
                    calculation = {
                        if (searchActive) {
                            filterDrawerSections(sections = completeSections, query = searchQuery)
                        } else {
                            completeSections
                        }
                    },
                )
                val exitSearch: () -> Unit = {
                    val restorationPosition = ordinaryPosition
                    searchActive = false
                    searchQuery = ""
                    ordinaryPosition = null
                    keyboardController?.hide()
                    if (restorationPosition != null) {
                        searchScope.launch {
                            withFrameNanos { }
                            resolveDrawerOrdinaryRestorationTarget(
                                position = restorationPosition,
                                sections = completeSections,
                                itemsPerRow = displaySettings.itemsPerRow,
                                anchorPresentation = displaySettings.sectionAnchorPresentation,
                            )?.let { target ->
                                listState.scrollToItem(
                                    index = target.itemIndex,
                                    scrollOffset = target.scrollOffset,
                                )
                            }
                        }
                    }
                }
                BackHandler(enabled = searchActive, onBack = exitSearch)
                LaunchedEffect(key1 = searchActive, key2 = searchQuery) {
                    if (searchActive) {
                        listState.scrollToItem(index = 0)
                    }
                }
                LaunchedEffect(
                    key1 = displaySettings,
                    key2 = displaySettingsMutationEnabled,
                    key3 = completeSections,
                ) {
                    val geometryChanged = positionedDisplaySettings.copy(
                        backgroundOpacity = displaySettings.backgroundOpacity,
                    ) != displaySettings
                    val position = displaySettingsPosition
                    if (!geometryChanged || position == null || searchActive) {
                        positionedDisplaySettings = displaySettings
                        if (displaySettingsMutationEnabled || searchActive) displaySettingsPosition = null
                        return@LaunchedEffect
                    }
                    withFrameNanos { }
                    resolveDrawerOrdinaryRestorationTarget(
                        position = position,
                        sections = completeSections,
                        itemsPerRow = displaySettings.itemsPerRow,
                        anchorPresentation = displaySettings.sectionAnchorPresentation,
                    )?.let { target ->
                        listState.scrollToItem(
                            index = target.itemIndex,
                            scrollOffset = target.scrollOffset,
                        )
                    }
                    positionedDisplaySettings = displaySettings
                    if (displaySettingsMutationEnabled) {
                        displaySettingsPosition = null
                    }
                }
                val alreadyFavoritedDragToast = stringResource(
                    R.string.drawer_drag_already_favorite,
                )
                val unableToAddDragToast = stringResource(
                    R.string.drawer_drag_unable_to_add,
                )
                val startDragToFavorite: (DrawerDragJourney) -> Boolean = { journey ->
                    val started = onDragToFavoriteStart(journey)
                    if (started && searchActive) {
                        // The search-mode journey leaves through the same programmatic
                        // downward transition and clears the transient query and mode
                        // under the existing search-exit rule.
                        searchActive = false
                        searchQuery = ""
                        ordinaryPosition = null
                        keyboardController?.hide()
                    }
                    started
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    DrawerApplicationList(
                        modifier = modifier,
                        listState = listState,
                        sections = visibleSections,
                        displaySettings = renderedDisplaySettings,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        searchFocusRequester = searchFocusRequester,
                        favoriteMembership = favoriteMembership,
                        favoriteAvailability = favoriteAvailability,
                        onLaunch = { entry ->
                            if (activationGuard.tryAcquire()) {
                                if (entryLauncher.launch(entry)) {
                                    searchActive = false
                                    searchQuery = ""
                                    ordinaryPosition = null
                                    keyboardController?.hide()
                                    onExternalLaunch()
                                } else {
                                    Toast.makeText(
                                        context,
                                        launchFailureMessage,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    loadTrigger = DrawerLoadTrigger.LaunchFailureRefresh
                                    loadRequest += 1
                                }
                            }
                        },
                        onLongPress = onLongPress,
                        alreadyFavoritedDragToast = alreadyFavoritedDragToast,
                        unableToAddDragToast = unableToAddDragToast,
                        onDragToFavoriteStart = startDragToFavorite,
                        onDragToFavoriteMove = onDragToFavoriteMove,
                        onDragToFavoriteEnd = onDragToFavoriteEnd,
                        onNavigateBack = onNavigateBack,
                        onEnterSearch = {
                            displaySettingsPosition = null
                            ordinaryPosition = captureDrawerOrdinaryListPosition(
                                sections = completeSections,
                                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                                itemsPerRow = displaySettings.itemsPerRow,
                                anchorPresentation = displaySettings.sectionAnchorPresentation,
                            )
                            searchActive = true
                        },
                        onQueryChange = { query -> searchQuery = query },
                        onClearSearch = { searchQuery = "" },
                        onCancelSearch = exitSearch,
                        onOpenDisplaySettings = { displaySettingsPanelVisible = true },
                        onOpenSettings = onOpenSettings,
                    )
                    if (displaySettingsPanelVisible) {
                        DrawerDisplaySettingsPanel(
                            settings = displaySettings,
                            enabled = displaySettingsMutationEnabled,
                            onChangeSettings = { candidateSettings ->
                                val topApplicationRow = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    it.key.toString().startsWith("row:") &&
                                        it.offset + it.size > listState.layoutInfo.viewportStartOffset
                                }
                                displaySettingsPosition = captureDrawerOrdinaryListPosition(
                                    sections = completeSections,
                                    firstVisibleItemIndex = topApplicationRow?.index ?: listState.firstVisibleItemIndex,
                                    firstVisibleItemScrollOffset = topApplicationRow?.let { -it.offset }
                                        ?: listState.firstVisibleItemScrollOffset,
                                    itemsPerRow = displaySettings.itemsPerRow,
                                    anchorPresentation = displaySettings.sectionAnchorPresentation,
                                    preserveApplicationIdentity = true,
                                )
                                onChangeDisplaySettings(candidateSettings)
                            },
                            onPreviewOpacity = { previewOpacity ->
                                backgroundOpacityPreview = previewOpacity
                            },
                            onPreviewApplicationSizes = { previewSettings ->
                                applicationSizesPreview = previewSettings
                            },
                            onDismiss = {
                                hideDisplaySettingsPanel()
                            },
                        )
                    }
                }
            } else {
                DrawerFavoriteSelectionList(
                    modifier = modifier,
                    listState = listState,
                    sections = currentState.snapshot.drawerSectionsFor(locale),
                    displaySettings = displaySettings,
                    target = favoriteSelectionTarget,
                    selection = favoriteSelection,
                    favoriteMembership = favoriteMembership,
                    saving = favoriteSelectionSaving,
                    selectionLimit = favoriteSelectionLimit,
                    onToggle = onToggleFavoriteSelection,
                    onCancel = onCancelFavoriteSelection,
                    onConfirm = onConfirmFavoriteSelection,
                )
            }
        }
    }
}

private fun LaunchableInventorySnapshot.drawerSectionsForCurrentMode(
    locale: Locale,
    searchActive: Boolean,
    searchQuery: String,
): List<DrawerSection> {
    val completeSections = drawerSectionsFor(locale = locale)
    return if (searchActive) {
        filterDrawerSections(sections = completeSections, query = searchQuery)
    } else {
        completeSections
    }
}

@Composable
private fun DrawerMessage(
    modifier: Modifier,
    message: String,
    showProgress: Boolean,
    showErrorIcon: Boolean = false,
    action: (@Composable () -> Unit)?,
    testTag: String,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .scrollable(
                state = rememberScrollableState { 0f },
                orientation = Orientation.Vertical,
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(
                    dimensionResource(R.dimen.status_progress_indicator_size),
                ),
            )
        }
        if (showErrorIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_inventory_error),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.drawer_error_icon_size))
                    .testTag("drawer_error_icon"),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.status_message_gap)))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = dimensionResource(R.dimen.home_favorite_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.home_favorite_line_height).value.sp,
        )
        action?.let { retryAction ->
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.status_message_gap)))
            retryAction()
        }
    }
}

@Composable
private fun DrawerOrdinaryMessage(
    modifier: Modifier,
    message: String,
    showProgress: Boolean,
    showErrorIcon: Boolean = false,
    action: (@Composable () -> Unit)?,
    testTag: String,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets = WindowInsets.safeDrawing),
    ) {
        DrawerNavigationTopBar(onNavigateBack = onNavigateBack)
        DrawerTopBarDivider()
        DrawerMessage(
            modifier = modifier.weight(weight = 1f),
            message = message,
            showProgress = showProgress,
            showErrorIcon = showErrorIcon,
            action = action,
            testTag = testTag,
        )
    }
}

@Composable
private fun DrawerSelectionMessage(
    modifier: Modifier,
    target: String,
    message: String,
    showProgress: Boolean,
    retry: (() -> Unit)?,
    selection: List<LaunchableIdentity>,
    saving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DrawerSelectionHeader(
            target = target,
            selectionSize = selection.size,
            selectionLimit = null,
            saving = saving,
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
        DrawerTopBarDivider()
        DrawerMessage(
            modifier = Modifier.weight(1f),
            message = message,
            showProgress = showProgress,
            showErrorIcon = !showProgress,
            action = retry?.let { retryAction ->
                {
                    TextButton(onClick = retryAction, enabled = !saving) {
                        Text(stringResource(R.string.retry))
                    }
                }
            },
            testTag = "drawer_selection_message",
        )
    }
}

@Composable
private fun DrawerFavoriteSelectionList(
    modifier: Modifier,
    listState: LazyListState,
    sections: List<DrawerSection>,
    displaySettings: DrawerDisplaySettings,
    target: String,
    selection: List<LaunchableIdentity>,
    favoriteMembership: Set<LaunchableIdentity>,
    saving: Boolean,
    selectionLimit: Int? = null,
    onToggle: (LaunchableIdentity) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val leftAnchors = displaySettings.sectionAnchorPresentation == DrawerSectionAnchorPresentation.LeftSide
    val sectionRanges = remember(sections, displaySettings.itemsPerRow, displaySettings.sectionAnchorPresentation) {
        drawerSectionRanges(sections, displaySettings.itemsPerRow, displaySettings.sectionAnchorPresentation, false)
    }
    val sectionAnchors = sectionRanges.associate { it.label to it.startIndex }
    val coroutineScope = rememberCoroutineScope()
    var activeIndexLabel by remember { mutableStateOf<String?>(null) }
    var indexJumpJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(sectionRanges) {
        indexJumpJob?.cancel()
        activeIndexLabel = null
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DrawerSelectionHeader(
            target = target,
            selectionSize = selection.size,
            selectionLimit = selectionLimit,
            saving = saving,
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
        DrawerTopBarDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drawer_favorite_selection_list"),
                contentPadding = PaddingValues(
                    start = dimensionResource(R.dimen.drawer_application_grid_boundary) +
                        if (leftAnchors) dimensionResource(R.dimen.drawer_section_anchor_column_width) else 0.dp,
                    end = dimensionResource(R.dimen.drawer_application_grid_boundary) +
                        dimensionResource(R.dimen.drawer_index_width),
                ),
                state = listState,
            ) {
                sections.forEach { section ->
                    if (!leftAnchors) {
                        item(key = "selection_section:${section.label}") {
                            DrawerSectionHeader(
                                label = section.label,
                                modifier = Modifier.padding(
                                    start = dimensionResource(
                                        id = R.dimen.drawer_application_cell_horizontal_inset,
                                    ),
                                ),
                            )
                        }
                    }
                    items(
                        items = section.entries.chunked(size = displaySettings.itemsPerRow),
                        key = { entries ->
                            val first = entries.first()
                            "selection_row:${first.identity.profileSerialNumber}:" +
                                first.identity.componentName.flattenToString()
                        },
                    ) { entries ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            entries.forEach { entry ->
                                val alreadyFavorite = entry.identity in favoriteMembership
                                val order = selection.indexOf(entry.identity)
                                DrawerFavoriteSelectionRow(
                                    entry = entry,
                                    displaySettings = displaySettings,
                                    order = order.takeIf { it >= 0 }?.plus(1),
                                    alreadyFavorite = alreadyFavorite,
                                    enabled = !saving && !alreadyFavorite,
                                    onClick = { onToggle(entry.identity) },
                                    modifier = Modifier.weight(weight = 1f),
                                )
                            }
                            repeat(times = displaySettings.itemsPerRow - entries.size) {
                                Spacer(modifier = Modifier.weight(weight = 1f))
                            }
                        }
                    }
                }
            }
            if (leftAnchors) {
                DrawerLeftSectionAnchors(ranges = sectionRanges, listState = listState)
            }
            DrawerAlphabetIndex(
                labels = sections.map(DrawerSection::label),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                onSelect = { label, immediate ->
                    if (!saving) {
                        val anchor = sectionAnchors.getValue(label)
                        indexJumpJob?.cancel()
                        indexJumpJob = coroutineScope.launch {
                            if (immediate) {
                                listState.scrollToItem(anchor)
                            } else {
                                listState.animateScrollToItem(anchor)
                            }
                        }
                    }
                },
                onActiveLabelChange = { label ->
                    activeIndexLabel = if (saving) null else label
                },
                onSelectSettings = {},
                includeSettings = false,
            )
            activeIndexLabel?.let { label ->
                DrawerIndexBubble(
                    label = label,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            end = dimensionResource(R.dimen.drawer_index_width) +
                                dimensionResource(R.dimen.drawer_index_bubble_index_gap),
                        ),
                )
            }
        }
    }
}

@Composable
private fun DrawerSelectionHeader(
    target: String,
    selectionSize: Int,
    selectionLimit: Int?,
    saving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.drawer_top_app_bar_height))
            .padding(horizontal = dimensionResource(R.dimen.drawer_horizontal_padding))
            .testTag("drawer_favorite_selection_header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.testTag("drawer_favorite_selection_cancel"),
            ) {
                Text(stringResource(R.string.drawer_selection_cancel))
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = target,
                modifier = Modifier.testTag("drawer_favorite_selection_title"),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && selectionSize > 0 &&
                    (selectionLimit == null || selectionSize <= selectionLimit),
                modifier = Modifier.testTag("drawer_favorite_selection_confirm"),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.launcher4max_foreground),
                ),
            ) {
                Text(stringResource(R.string.drawer_selection_confirm))
            }
        }
    }
}

@Composable
internal fun DrawerFavoriteSelectionRow(
    entry: LaunchableEntry,
    displaySettings: DrawerDisplaySettings,
    order: Int?,
    alreadyFavorite: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHeight = drawerApplicationRowHeight(displaySettings)
    val disabledAlpha = integerResource(R.integer.disabled_content_alpha_percent) / 100f
    val selectedScale = integerResource(R.integer.drawer_selection_selected_scale_percent) / 100f
    val animationDuration = integerResource(R.integer.short_property_animation_duration_ms)
    val scale by animateFloatAsState(
        targetValue = if (order != null) selectedScale else 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "drawer_favorite_selection_cell_scale",
    )
    val stateLabel = when {
        alreadyFavorite -> stringResource(R.string.drawer_selection_already_favorite_unavailable)
        order != null -> stringResource(R.string.drawer_selection_order_format, order)
        else -> stringResource(R.string.drawer_selection_not_selected)
    }
    Box(
        modifier = modifier
            .height(height = rowHeight)
            .alpha(alpha = if (alreadyFavorite) disabledAlpha else 1f)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { stateDescription = stateLabel }
            .testTag(tag = "drawer_favorite_selection_row")
            .graphicsLayer {
                // Purely visual: the layout bounds and the click target above stay unchanged.
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (order != null) {
                    Modifier.border(
                        width = dimensionResource(R.dimen.drawer_selection_outline_width),
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(
                            size = dimensionResource(
                                R.dimen.drawer_selection_outline_corner_radius,
                            ),
                        ),
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        DrawerApplicationContent(
            entry = entry,
            displaySettings = displaySettings,
            searchQuery = null,
            modifier = Modifier.padding(
                horizontal = dimensionResource(
                    id = R.dimen.drawer_application_cell_horizontal_inset,
                ),
            ),
        )
        if (order != null) {
            DrawerFavoriteSelectionBadge(
                order = order,
                modifier = Modifier.align(alignment = Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun DrawerFavoriteSelectionBadge(
    order: Int,
    modifier: Modifier = Modifier,
) {
    val badgeShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.drawer_selection_badge_corner_radius),
        topEnd = 0.dp,
        bottomStart = 0.dp,
        bottomEnd = dimensionResource(R.dimen.drawer_selection_badge_corner_radius),
    )
    // Overlays the cell without layout space, focus, or a second description; the row's
    // state description already announces the order.
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = dimensionResource(R.dimen.drawer_selection_badge_min_size),
                minHeight = dimensionResource(R.dimen.drawer_selection_badge_min_size),
            )
            .background(color = MaterialTheme.colorScheme.onBackground, shape = badgeShape)
            .padding(
                horizontal = dimensionResource(R.dimen.drawer_selection_badge_content_padding),
            )
            .clearAndSetSemantics { }
            .testTag(tag = "drawer_favorite_selection_number"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            color = colorResource(R.color.launcher4max_sheet_surface),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
            lineHeight = dimensionResource(R.dimen.style_settings_secondary_line_height).value.sp,
            // The badge sits on an opaque primaryTextColor surface; the drawer-wide
            // foreground shadow applies only to content over the variable background.
            style = LocalTextStyle.current.copy(shadow = null),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DrawerApplicationList(
    modifier: Modifier,
    listState: LazyListState,
    sections: List<DrawerSection>,
    displaySettings: DrawerDisplaySettings,
    searchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    favoriteMembership: Set<LaunchableIdentity>,
    favoriteAvailability: Map<LaunchableIdentity, FavoriteAvailability>,
    onLaunch: (LaunchableEntry) -> Unit,
    onLongPress: (LaunchableEntry) -> Unit,
    alreadyFavoritedDragToast: String,
    unableToAddDragToast: String,
    onDragToFavoriteStart: (DrawerDragJourney) -> Boolean,
    onDragToFavoriteMove: (Offset) -> Unit,
    onDragToFavoriteEnd: (Offset?, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onEnterSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val leftAnchors = displaySettings.sectionAnchorPresentation == DrawerSectionAnchorPresentation.LeftSide
    val sectionRanges = remember(sections, displaySettings.itemsPerRow, displaySettings.sectionAnchorPresentation) {
        drawerSectionRanges(sections, displaySettings.itemsPerRow, displaySettings.sectionAnchorPresentation, false)
    }
    val sectionAnchors = sectionRanges.associate { it.label to it.startIndex }
    val allRanges = remember(sectionRanges, searchActive, displaySettings.sectionAnchorPresentation) {
        drawerSectionRanges(sections, displaySettings.itemsPerRow, displaySettings.sectionAnchorPresentation, !searchActive)
    }
    val settingsAnchor = sectionRanges.lastOrNull()?.endIndex ?: 0
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var activeIndexLabel by remember { mutableStateOf<String?>(null) }
    var indexJumpJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchActive, allRanges) {
        indexJumpJob?.cancel()
        activeIndexLabel = null
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets = WindowInsets.safeDrawing),
    ) {
        DrawerSearchTopBar(
            searchActive = searchActive,
            query = searchQuery,
            focusRequester = searchFocusRequester,
            onNavigateBack = onNavigateBack,
            onEnterSearch = onEnterSearch,
            onQueryChange = onQueryChange,
            onClearSearch = onClearSearch,
            onCancelSearch = onCancelSearch,
            onOpenDisplaySettings = onOpenDisplaySettings,
        )
        DrawerTopBarDivider()
        val gridBoundary = dimensionResource(R.dimen.drawer_application_grid_boundary)
        val cellInset = dimensionResource(R.dimen.drawer_application_cell_horizontal_inset)
        val indexWidth = dimensionResource(R.dimen.drawer_index_width)
        Box(
            modifier = modifier
                .weight(weight = 1f)
                .fillMaxWidth(),
        ) {
            if (searchActive && searchQuery.isNotBlank() && sections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(tag = "drawer_search_empty"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.drawer_no_matching_apps),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(tag = "drawer_application_list"),
                    contentPadding = PaddingValues(
                        start = gridBoundary +
                            if (leftAnchors) dimensionResource(R.dimen.drawer_section_anchor_column_width) else 0.dp,
                        end = gridBoundary + indexWidth,
                    ),
                    state = listState,
                ) {
                    sections.forEach { section ->
                        if (!leftAnchors) {
                            item(key = "section:${section.label}") {
                                DrawerSectionHeader(
                                    label = section.label,
                                    modifier = Modifier.padding(start = cellInset),
                                )
                            }
                        }
                        items(
                            items = section.entries.chunked(size = displaySettings.itemsPerRow),
                            key = { entries ->
                                val first = entries.first()
                                "row:${first.identity.profileSerialNumber}:" +
                                    first.identity.componentName.flattenToString()
                            },
                        ) { entries ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                entries.forEach { entry ->
                                    DrawerApplicationRow(
                                        entry = entry,
                                        displaySettings = displaySettings,
                                        searchQuery = searchQuery.takeIf { searchActive },
                                        dragEligibility = resolveDrawerDragEligibility(
                                            isFavoriteMember = entry.identity in favoriteMembership,
                                            availability = favoriteAvailability[entry.identity],
                                        ),
                                        onLaunch = onLaunch,
                                        onLongPress = onLongPress,
                                        onAlreadyFavoritedDrag = {
                                            Toast.makeText(
                                                context,
                                                alreadyFavoritedDragToast,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                        onUnableToAddDrag = {
                                            Toast.makeText(
                                                context,
                                                unableToAddDragToast,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                        onDragToFavoriteStart = onDragToFavoriteStart,
                                        onDragToFavoriteMove = onDragToFavoriteMove,
                                        onDragToFavoriteEnd = onDragToFavoriteEnd,
                                        modifier = Modifier.weight(weight = 1f),
                                    )
                                }
                                repeat(times = displaySettings.itemsPerRow - entries.size) {
                                    Spacer(modifier = Modifier.weight(weight = 1f))
                                }
                            }
                        }
                    }
                    if (!searchActive) {
                        if (!leftAnchors) {
                            item(key = "section:settings") {
                                DrawerSectionHeader(
                                    label = stringResource(id = R.string.settings),
                                    modifier = Modifier
                                        .padding(start = cellInset)
                                        .testTag(tag = "drawer_settings_anchor"),
                                )
                            }
                        }
                        item(key = "settings") {
                            DrawerSettingsRow(
                                onClick = onOpenSettings,
                                modifier = Modifier.padding(horizontal = cellInset),
                            )
                        }
                    }
                }
                if (leftAnchors) {
                    DrawerLeftSectionAnchors(ranges = allRanges, listState = listState)
                }
                if (!searchActive || sections.isNotEmpty()) {
                    DrawerAlphabetIndex(
                        labels = sections.map(transform = DrawerSection::label),
                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                        onSelect = { label, immediate ->
                            val anchor = sectionAnchors.getValue(label)
                            hapticFeedback.performHapticFeedback(
                                hapticFeedbackType = HapticFeedbackType.SegmentTick,
                            )
                            indexJumpJob?.cancel()
                            indexJumpJob = coroutineScope.launch {
                                if (immediate) {
                                    listState.scrollToItem(index = anchor)
                                } else {
                                    listState.animateScrollToItem(index = anchor)
                                }
                            }
                        },
                        onActiveLabelChange = { label -> activeIndexLabel = label },
                        onSelectSettings = { immediate ->
                            if (!searchActive) {
                                hapticFeedback.performHapticFeedback(
                                    hapticFeedbackType = HapticFeedbackType.SegmentTick,
                                )
                                indexJumpJob?.cancel()
                                indexJumpJob = coroutineScope.launch {
                                    if (immediate) {
                                        listState.scrollToItem(index = settingsAnchor)
                                    } else {
                                        listState.animateScrollToItem(index = settingsAnchor)
                                    }
                                }
                            }
                        },
                        includeSettings = !searchActive,
                    )
                }
            }

            activeIndexLabel?.let { label ->
                DrawerIndexBubble(
                    label = label,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .padding(
                            end = indexWidth + dimensionResource(
                                id = R.dimen.drawer_index_bubble_index_gap,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = dimensionResource(id = R.dimen.drawer_section_header_height))
            .testTag(tag = "drawer_section_$label"),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = dimensionResource(
                    id = R.dimen.shared_large_app_name_text_size,
                ).value.sp,
                lineHeight = dimensionResource(
                    id = R.dimen.shared_large_app_name_line_height,
                ).value.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun DrawerSettingsRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.drawer_application_row_min_height))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("drawer_settings_entry"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.drawer_application_icon_size)),
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.drawer_settings_icon_label_gap)))
        Text(
            text = stringResource(R.string.settings),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DrawerApplicationRow(
    entry: LaunchableEntry,
    displaySettings: DrawerDisplaySettings,
    searchQuery: String?,
    dragEligibility: DrawerDragEligibility,
    onLaunch: (LaunchableEntry) -> Unit,
    onLongPress: (LaunchableEntry) -> Unit,
    onAlreadyFavoritedDrag: () -> Unit,
    onUnableToAddDrag: () -> Unit,
    onDragToFavoriteStart: (DrawerDragJourney) -> Boolean,
    onDragToFavoriteMove: (Offset) -> Unit,
    onDragToFavoriteEnd: (Offset?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHeight = drawerApplicationRowHeight(displaySettings)
    val hapticFeedback = LocalHapticFeedback.current
    val openActionsLabel = stringResource(R.string.drawer_open_application_actions)
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .height(height = rowHeight)
            .onGloballyPositioned { rowCoordinates = it }
            .semantics {
                if (displaySettings.namePlacement == DrawerNamePlacement.Hidden) {
                    contentDescription = entry.label
                }
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = openActionsLabel,
                        action = { onLongPress(entry); true },
                    ),
                )
            }
            .combinedClickable(
                role = Role.Button,
                onClick = { onLaunch(entry) },
                // The long-press haptic arms the drag-to-favorite discrimination; the
                // action sheet opens on release through the pointer detector below.
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .drawerDragToFavoriteDetection(
                key1 = entry.identity,
                onReleaseAfterLongPress = { onLongPress(entry) },
                onDragBeyondSlop = { change ->
                    // Never drop the journey silently: if row geometry is not yet
                    // captured, start with degraded geometry rather than no journey.
                    // Ineligible rows keep dispatched=false so the release still
                    // opens the action sheet per the contract.
                    val coordinates = rowCoordinates
                    when (dragEligibility) {
                        DrawerDragEligibility.Eligible -> onDragToFavoriteStart(
                            DrawerDragJourney(
                                entry = entry,
                                originInWindow = coordinates?.localToWindow(
                                    Offset.Zero,
                                ) ?: Offset.Zero,
                                size = coordinates?.size ?: IntSize.Zero,
                                touchStartInWindow = coordinates?.localToWindow(
                                    change.position,
                                ) ?: Offset.Zero,
                            ),
                        )
                        DrawerDragEligibility.AlreadyFavorited -> {
                            onAlreadyFavoritedDrag()
                            false
                        }
                        DrawerDragEligibility.ReliablyDisabled -> {
                            onUnableToAddDrag()
                            false
                        }
                    }
                },
                onDragMove = { change ->
                    rowCoordinates?.let { coordinates ->
                        onDragToFavoriteMove(coordinates.localToWindow(change.position))
                    }
                },
                onDragEnd = { change, cancelled ->
                    onDragToFavoriteEnd(
                        change?.let { currentChange ->
                            rowCoordinates?.localToWindow(currentChange.position)
                        },
                        cancelled,
                    )
                },
            )
            .testTag("drawer_application_row"),
        contentAlignment = Alignment.Center,
    ) {
        DrawerApplicationContent(
            entry = entry,
            displaySettings = displaySettings,
            searchQuery = searchQuery,
            modifier = Modifier.padding(
                horizontal = dimensionResource(
                    id = R.dimen.drawer_application_cell_horizontal_inset,
                ),
            ),
        )
    }
}

@Composable
internal fun DrawerApplicationContent(
    entry: LaunchableEntry,
    displaySettings: DrawerDisplaySettings,
    searchQuery: String?,
    modifier: Modifier = Modifier,
) {
    val iconSize = dimensionResource(id = displaySettings.iconSize.iconSizeResource())
    val iconSizePixels = with(LocalDensity.current) { iconSize.roundToPx() }
    if (displaySettings.namePlacement == DrawerNamePlacement.Right) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(
                        R.dimen.drawer_application_below_vertical_inset,
                    ),
                    bottom = dimensionResource(R.dimen.drawer_application_bottom_inset),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawerApplicationIcon(
                preparedBitmap = entry.iconBitmap,
                icon = entry.icon,
                iconSize = iconSize,
                iconSizePixels = iconSizePixels,
            )
            Spacer(
                modifier = Modifier.width(
                    width = dimensionResource(id = R.dimen.drawer_application_icon_label_gap),
                ),
            )
            DrawerApplicationName(
                label = entry.label,
                searchQuery = searchQuery,
                textSize = displaySettings.textSize,
                modifier = Modifier.weight(weight = 1f),
            )
        }
    } else if (displaySettings.namePlacement == DrawerNamePlacement.Below) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(
                modifier = Modifier.height(
                    height = dimensionResource(
                        id = R.dimen.drawer_application_below_vertical_inset,
                    ),
                ),
            )
            DrawerApplicationIcon(
                preparedBitmap = entry.iconBitmap,
                icon = entry.icon,
                iconSize = iconSize,
                iconSizePixels = iconSizePixels,
            )
            Spacer(
                modifier = Modifier.height(
                    height = dimensionResource(id = R.dimen.drawer_application_icon_label_gap),
                ),
            )
            DrawerApplicationName(
                label = entry.label,
                searchQuery = searchQuery,
                textSize = displaySettings.textSize,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(
                    height = dimensionResource(R.dimen.drawer_application_bottom_inset),
                ),
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.drawer_application_below_vertical_inset),
                    bottom = dimensionResource(R.dimen.drawer_application_bottom_inset),
                ),
            contentAlignment = Alignment.Center,
        ) {
            DrawerApplicationIcon(
                preparedBitmap = entry.iconBitmap,
                icon = entry.icon,
                iconSize = iconSize,
                iconSizePixels = iconSizePixels,
            )
        }
    }
}

@Composable
private fun drawerApplicationRowHeight(settings: DrawerDisplaySettings): Dp {
    val topInset = dimensionResource(R.dimen.drawer_application_below_vertical_inset)
    val bottomInset = dimensionResource(R.dimen.drawer_application_bottom_inset)
    val iconHeight = dimensionResource(settings.iconSize.iconSizeResource())
    val textLineHeight = dimensionResource(settings.textSize.lineHeightResource())
    return when (settings.namePlacement) {
        DrawerNamePlacement.Right -> maxOf(iconHeight, textLineHeight) + topInset + bottomInset
        DrawerNamePlacement.Below -> iconHeight + textLineHeight +
                dimensionResource(R.dimen.drawer_application_icon_label_gap) +
                topInset + bottomInset
        DrawerNamePlacement.Hidden -> iconHeight + topInset + bottomInset
    }
}

@Composable
private fun DrawerApplicationName(
    label: String,
    searchQuery: String?,
    textSize: DrawerApplicationSize,
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    val matchRanges = remember(key1 = label, key2 = searchQuery) {
        searchQuery?.let { query ->
            drawerSearchMatchRanges(label = label, query = query)
        }.orEmpty()
    }
    val emphasizedLabel = remember(key1 = label, key2 = matchRanges) {
        buildAnnotatedString {
            append(text = label)
            matchRanges.forEach { range ->
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Medium),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }
    }
    Text(
        text = emphasizedLabel,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = dimensionResource(id = textSize.textSizeResource()).value.sp,
            lineHeight = dimensionResource(id = textSize.lineHeightResource()).value.sp,
        ),
    )
}

@Composable
private fun DrawerApplicationIcon(
    preparedBitmap: Bitmap?,
    icon: Drawable,
    iconSize: Dp,
    iconSizePixels: Int,
) {
    // Keep the ImageBitmap wrapper across recompositions so scrolling a long
    // distance does not reallocate one per row it passes.
    val bitmap = remember(preparedBitmap, icon, iconSizePixels) {
        preparedBitmap?.asImageBitmap()
            ?: icon.toBitmap(width = iconSizePixels, height = iconSizePixels).asImageBitmap()
    }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(iconSize),
    )
}
