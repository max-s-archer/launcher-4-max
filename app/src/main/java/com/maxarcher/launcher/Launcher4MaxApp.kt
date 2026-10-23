package com.maxarcher.launcher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import com.maxarcher.launcher.platform.icons.LauncherIconAppearance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import com.maxarcher.launcher.ui.settings.EmptySettingsPlatform
import com.maxarcher.launcher.ui.settings.SettingsPage
import com.maxarcher.launcher.ui.settings.SettingsPlatform
import com.maxarcher.launcher.ui.settings.SettingsScreen
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsReadState
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsStore
import com.maxarcher.launcher.ui.drawer.DrawerDragPreviewOverlay
import com.maxarcher.launcher.ui.drawer.DrawerScreen
import com.maxarcher.launcher.ui.home.HomeCoordinator

internal enum class AppSurface {
    Home,
    Drawer,
}

private data class FavoriteAddTarget(
    val containerId: String?,
    val containerType: FavoriteContainerType,
    val label: String,
    val provisional: Boolean,
    val homeBottomAppBar: Boolean = false,
    val remainingPlaces: Int? = null,
)

private const val DRAWER_MOVEMENT_PER_GESTURE_DP = 1.5f
private const val TARGET_FLING_THRESHOLD_DP_PER_SECOND = 1_000f

internal fun drawerGestureProgress(
    displacementPx: Float,
    fullGestureDistancePx: Float,
): Float = (displacementPx / fullGestureDistancePx).coerceIn(0f, 1f)

internal fun drawerInteractiveDisplacement(gestureDisplacementPx: Float): Float =
    gestureDisplacementPx * DRAWER_MOVEMENT_PER_GESTURE_DP

internal fun transitionTarget(
    origin: AppSurface,
    gestureDisplacementPx: Float,
    completionThresholdPx: Float,
    targetVelocity: Float,
    targetFlingThresholdPx: Float,
): AppSurface {
    val completes = gestureDisplacementPx >= completionThresholdPx ||
        targetVelocity >= targetFlingThresholdPx
    return if (completes) {
        if (origin == AppSurface.Home) AppSurface.Drawer else AppSurface.Home
    } else {
        origin
    }
}

@Composable
internal fun Launcher4MaxApp(systemHomeEvents: MutableSharedFlow<Unit>? = null) {
    val context = LocalContext.current
    val graph = remember(context) {
        // Hosts without the Launcher4Max application (previews, some test environments) fall
        // back to a graph scoped to this composition.
        (context.applicationContext as? Launcher4MaxApplication)?.appGraph
            ?: AppGraph(context = context)
    }
    Launcher4MaxApp(
        systemHomeEvents = systemHomeEvents,
        inventoryLoader = graph.inventoryLoader,
        entryLauncher = graph.entryLauncher,
        favoriteStore = graph.favoriteStore,
        drawerDisplaySettingsStore = graph.drawerDisplaySettingsStore,
        quickActionBindingsStore = graph.quickActionBindingsStore,
        iconShapeSettingsStore = graph.iconShapeSettingsStore,
        defaultLauncherPromptStore = graph.defaultLauncherPromptStore,
        informationLauncher = graph.informationLauncher,
        uninstallLauncher = graph.uninstallLauncher,
        shortcutController = graph.shortcutController,
        settingsPlatform = graph.settingsPlatform,
        licenseText = graph.licenseText,
        thirdPartyNoticesText = graph.thirdPartyNoticesText,
        accessibilityLockController = graph.accessibilityLockController,
    )
}

@Composable
internal fun Launcher4MaxApp(
    systemHomeEvents: MutableSharedFlow<Unit>? = null,
    inventoryLoader: LaunchableInventoryLoader,
    entryLauncher: LaunchableEntryLauncher = LaunchableEntryLauncher { false },
    favoriteStore: FavoriteStore? = null,
    drawerDisplaySettingsStore: DrawerDisplaySettingsStore? = null,
    quickActionBindingsStore: QuickActionBindingsStore? = null,
    iconShapeSettingsStore: IconShapeSettingsStore? = null,
    defaultLauncherPromptStore: DefaultLauncherPromptStore? = null,
    onDrawerDisplaySettingsSaveFailure: (() -> Unit)? = null,
    informationLauncher: ApplicationInformationLauncher = ApplicationInformationLauncher { false },
    uninstallLauncher: ApplicationUninstallLauncher = EmptyApplicationUninstallLauncher,
    shortcutController: ApplicationShortcutController = EmptyApplicationShortcutController,
    settingsPlatform: SettingsPlatform = EmptySettingsPlatform,
    licenseText: String = "",
    thirdPartyNoticesText: String = "",
    accessibilityLockController: AccessibilityLockController = EmptyAccessibilityLockController,
) {
    val androidContext = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val homeFavoriteListState = rememberLazyListState()
    val drawerListState = rememberLazyListState()
    val fullGestureDistancePx = with(density) {
        dimensionResource(R.dimen.drawer_transition_full_gesture_distance).toPx()
    }
    val completionThresholdPx = with(density) {
        dimensionResource(R.dimen.drawer_entry_drag_threshold).toPx()
    }
    val targetFlingThresholdPx = with(density) {
        TARGET_FLING_THRESHOLD_DP_PER_SECOND.dp.toPx()
    }
    var settledSurface by remember { mutableStateOf(AppSurface.Home) }
    var drawerActivated by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var drawerOffsetPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    var gestureDisplacementPx by remember { mutableFloatStateOf(0f) }
    var drawerTransitionOwnsGesture by remember { mutableStateOf(false) }
    var homeTransitionOwnsGesture by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val effectiveFavoriteStore = favoriteStore ?: remember { InMemoryFavoriteStore() }
    val favoriteState by effectiveFavoriteStore.state.collectAsStateWithLifecycle()
    val effectiveDrawerDisplaySettingsStore = drawerDisplaySettingsStore ?: remember(
        androidContext,
    ) {
        DrawerDisplaySettingsStore(context = androidContext)
    }
    val drawerDisplaySettingsState by effectiveDrawerDisplaySettingsStore.state
        .collectAsStateWithLifecycle()
    val effectiveQuickActionBindingsStore = quickActionBindingsStore ?: remember(
        androidContext,
    ) {
        QuickActionBindingsStore(context = androidContext)
    }
    val quickActionBindingsState by effectiveQuickActionBindingsStore.state
        .collectAsStateWithLifecycle()
    val quickActionBindings =
        (quickActionBindingsState as? QuickActionBindingsReadState.Readable)
            ?.bindings
            ?: QuickActionBindings()
    val effectiveIconShapeSettingsStore = iconShapeSettingsStore ?: remember(
        androidContext,
    ) {
        IconShapeSettingsStore(context = androidContext)
    }
    val iconShapeSettingsState by effectiveIconShapeSettingsStore.state
        .collectAsStateWithLifecycle()
    val iconShapeSettings =
        (iconShapeSettingsState as? IconShapeSettingsReadState.Readable)
            ?.settings
            ?: IconShapeSettings()
    var iconShapeSaveInFlight by remember { mutableStateOf(false) }
    var iconShapeSaveFailureToken by remember { mutableIntStateOf(0) }
    val effectiveDefaultLauncherPromptStore = defaultLauncherPromptStore ?: remember(
        androidContext,
    ) {
        DefaultLauncherPromptStore(context = androidContext)
    }
    val defaultLauncherPromptSession = remember { DefaultLauncherPromptSession() }
    var showDefaultLauncherPrompt by remember { mutableStateOf(false) }
    val currentSettingsPlatform by rememberUpdatedState(settingsPlatform)
    val currentPromptStore by rememberUpdatedState(effectiveDefaultLauncherPromptStore)
    val favoriteEditor = remember(
        key1 = effectiveFavoriteStore,
        calculation = { HomeFavoriteEditor(store = effectiveFavoriteStore) },
    )
    var homeSafeWidthDp by remember { mutableFloatStateOf(0f) }
    val settingsBackupController = remember(
        androidContext,
        effectiveFavoriteStore,
        effectiveDrawerDisplaySettingsStore,
        effectiveQuickActionBindingsStore,
        effectiveIconShapeSettingsStore,
    ) {
        val favoritesAccess = effectiveFavoriteStore as? BackupFavoritesAccess
        val settingsAccess = effectiveDrawerDisplaySettingsStore as? BackupSettingsAccess
        val bindingsAccess = effectiveQuickActionBindingsStore as? BackupBindingsAccess
        if (favoritesAccess == null || settingsAccess == null || bindingsAccess == null) {
            null
        } else {
            SettingsBackupController(
                context = androidContext,
                favorites = favoritesAccess,
                settings = settingsAccess,
                bindings = bindingsAccess,
                iconShape = effectiveIconShapeSettingsStore,
                homeSafeWidthDp = { homeSafeWidthDp.takeIf { width -> width > 0f } },
            )
        }
    }
    val removalSnackbarHostState = remember(calculation = { SnackbarHostState() })
    val inventoryCoordinator = remember(inventoryLoader) {
        LaunchableInventoryCoordinator(inventoryLoader)
    }
    val inventoryState by inventoryCoordinator.state.collectAsStateWithLifecycle()
    var favoriteAvailability by remember {
        mutableStateOf<Map<LaunchableIdentity, FavoriteAvailability>>(emptyMap())
    }
    var selectedEntry by remember { mutableStateOf<LaunchableEntry?>(null) }
    var selectedEntryFromHome by remember { mutableStateOf(false) }
    var rootOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var drawerDisplaySettingsCandidate by remember {
        mutableStateOf<DrawerDisplaySettings?>(null)
    }
    var drawerDisplaySettingsSaving by remember { mutableStateOf(false) }
    var favoriteAddTarget by remember { mutableStateOf<FavoriteAddTarget?>(null) }
    var favoriteSelection by remember { mutableStateOf<List<LaunchableIdentity>>(emptyList()) }
    var favoriteSelectionSaving by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf(SettingsPage.Root) }
    var homeBottomAppBarState by remember { mutableStateOf(HomeBottomAppBarState()) }
    var homeBottomAppBarSaveInFlight by remember { mutableStateOf(false) }
    var homeBottomAppBarExpanded by remember { mutableStateOf(false) }
    var externalLaunchPendingReturn by remember { mutableStateOf(false) }
    var inventoryRefreshPendingReturn by remember { mutableStateOf(false) }
    var shortcutOwner by remember { mutableStateOf<LaunchableIdentity?>(null) }
    var applicationShortcuts by remember { mutableStateOf(emptyList<ApplicationShortcut>()) }
    val homeActivationGuard = remember { RapidActivationGuard() }
    val unavailableFavoriteMessage = stringResource(R.string.favorite_application_unavailable)
    val launchFailureMessage = stringResource(R.string.application_unable_to_open)
    val favoritesChangedMessage = stringResource(R.string.favorites_changed_edit_ended)
    val inventoryFailureMessage = stringResource(R.string.inventory_update_failed_edit_ended)
    val addToListLabel = stringResource(R.string.drawer_selection_add_to_list)
    val createListLabel = stringResource(R.string.drawer_selection_create_list)
    val addToFavoriteBarLabel = stringResource(R.string.drawer_selection_add_to_favorite_bar)
    val addToHomeBottomAppBarLabel = stringResource(R.string.home_bottom_app_bar_add)
    val createFavoriteBarLabel = stringResource(
        R.string.drawer_selection_create_favorite_bar,
    )
    val removedMessage = stringResource(id = R.string.favorite_removed)
    val undoLabel = stringResource(id = R.string.undo)
    val durableDrawerDisplaySettings =
        (drawerDisplaySettingsState as? DrawerDisplaySettingsReadState.Readable)?.settings
    val presentedDrawerDisplaySettings = drawerDisplaySettingsCandidate
        ?: durableDrawerDisplaySettings
        ?: DrawerDisplaySettings()
    val drawerDisplaySettingsReady =
        drawerDisplaySettingsState !is DrawerDisplaySettingsReadState.Loading
    val drawerDisplaySettingsMutationEnabled =
        drawerDisplaySettingsState is DrawerDisplaySettingsReadState.Readable &&
            !drawerDisplaySettingsSaving
    val homeCoordinator = remember(
        androidContext,
        scope,
        effectiveFavoriteStore,
        favoriteEditor,
    ) {
        HomeCoordinator(
            context = androidContext,
            scope = scope,
            favoriteStore = effectiveFavoriteStore,
            favoriteEditor = favoriteEditor,
        )
    }
    homeCoordinator.updateFavoriteState(favoriteState)
    val persistedHomeBottomAppBarState =
        (favoriteState as? FavoriteReadState.Readable)
            ?.orderedAggregate
            ?.homeBottomAppBar
    LaunchedEffect(persistedHomeBottomAppBarState, homeBottomAppBarSaveInFlight) {
        if (!homeBottomAppBarSaveInFlight && persistedHomeBottomAppBarState != null) {
            homeBottomAppBarState = persistedHomeBottomAppBarState
        }
    }

    LaunchedEffect(
        key1 = drawerDisplaySettingsState,
        key2 = drawerDisplaySettingsCandidate,
        key3 = drawerDisplaySettingsSaving,
    ) {
        if (!drawerDisplaySettingsSaving &&
            drawerDisplaySettingsCandidate != null &&
            durableDrawerDisplaySettings == drawerDisplaySettingsCandidate
        ) {
            drawerDisplaySettingsCandidate = null
        }
    }

    fun removeHomeFavorite(identity: LaunchableIdentity, offerUndo: Boolean = true) {
        if (favoriteEditor.isSaving) return
        scope.launch(
            block = {
                val saved = favoriteEditor.remove(identity = identity)
                if (saved && !offerUndo) favoriteEditor.invalidateUndo()
                homeCoordinator.refreshEditMembership()
                if (!saved) {
                    Toast.makeText(androidContext, R.string.favorite_remove_failed, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    LaunchedEffect(
        key1 = favoriteEditor.undoRemoval,
        block = {
            val snapshot = favoriteEditor.undoRemoval ?: return@LaunchedEffect
            val result = removalSnackbarHostState.showSnackbar(
                message = removedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // The save belongs to the App scope, not this Snackbar's cancellable effect.
                scope.launch(
                    block = {
                        val restored = favoriteEditor.undo(
                            snapshot = snapshot,
                            currentBarCapacity = { bar ->
                                // Read live width and transaction state after inventory validation.
                                val width = homeSafeWidthDp
                                if (!width.isFinite() || width <= 0f) {
                                    null
                                } else {
                                    val resources = androidContext.resources
                                    val density = resources.displayMetrics.density
                                    homeBottomAppBarCapacity(
                                        safeWidthDp = width,
                                        outerMarginDp = resources.getDimension(
                                            R.dimen.home_bottom_app_bar_outer_margin,
                                        ) / density,
                                        iconSizeDp = resources.getDimension(
                                            bar.iconSize.iconSizeResource(),
                                        ) / density,
                                        horizontalInsetDp = resources.getDimension(
                                            R.dimen.home_bottom_app_bar_preview_horizontal_inset,
                                        ) / density,
                                        iconGapDp = resources.getDimension(
                                            R.dimen.home_bottom_app_bar_preview_icon_gap,
                                        ) / density,
                                    )
                                }
                            },
                            revalidate = {
                                val inventory = (inventoryCoordinator.state.value
                                    as? LaunchableInventoryState.Content)?.snapshot
                                if (inventory == null) {
                                    false
                                } else {
                                    val resolved = inventoryCoordinator.resolveFavorites(
                                        identities = listOf(element = snapshot.identity),
                                        snapshot = inventory,
                                    )
                                    favoriteEditor.reconcileAvailability(availability = resolved)
                                    resolved[snapshot.identity] != FavoriteAvailability.ConfirmedRemoved
                                }
                            },
                        )
                        homeCoordinator.refreshEditMembership()
                        if (!restored) {
                            Toast.makeText(
                                androidContext,
                                R.string.favorite_undo_unavailable,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            } else {
                favoriteEditor.dismissUndo(snapshot = snapshot)
            }
        },
    )

    // The graph performs the one process-wide read before the first frame; this effect
    // only covers hosts with composition-scoped stores and leaves committed state alone.
    LaunchedEffect(effectiveFavoriteStore) {
        if (effectiveFavoriteStore.state.value is FavoriteReadState.Loading) {
            effectiveFavoriteStore.load()
        }
    }

    LaunchedEffect(key1 = effectiveDrawerDisplaySettingsStore) {
        if (
            effectiveDrawerDisplaySettingsStore.state.value is
            DrawerDisplaySettingsReadState.Loading
        ) {
            effectiveDrawerDisplaySettingsStore.load()
        }
    }

    LaunchedEffect(effectiveIconShapeSettingsStore) {
        if (
            effectiveIconShapeSettingsStore.state.value is
            IconShapeSettingsReadState.Loading
        ) {
            effectiveIconShapeSettingsStore.load()
        }
    }

    LaunchedEffect(inventoryCoordinator, effectiveIconShapeSettingsStore) {
        var lastAppearance: LauncherIconAppearance? = null
        effectiveIconShapeSettingsStore.state.collect { state ->
            if (state is IconShapeSettingsReadState.Loading) return@collect
            val appearance = effectiveIconShapeSettingsStore.lastSuccessfulSettings.iconAppearance()
            if (lastAppearance != appearance) {
                lastAppearance = appearance
                // Keep collecting successful saves while a previous inventory load is suspended.
                launch {
                    inventoryCoordinator.load(
                        showLoading = false,
                        preserveContentOnFailure = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(selectedEntry, shortcutController) {
        shortcutOwner = null
        applicationShortcuts = emptyList()
        val entry = selectedEntry ?: return@LaunchedEffect
        val loaded = shortcutController.load(entry)
        if (selectedEntry?.identity == entry.identity) {
            shortcutOwner = entry.identity
            applicationShortcuts = loaded
        }
    }
    LaunchedEffect(favoriteState) {
        val state = favoriteState
        if (state !is FavoriteReadState.Readable && selectedEntryFromHome) {
            selectedEntry = null
            selectedEntryFromHome = false
        }
        if (homeCoordinator.editMode) {
            val readable = state as? FavoriteReadState.Readable
            if (readable == null) {
                homeCoordinator.dismissEditMode()
                Toast.makeText(androidContext, inventoryFailureMessage, Toast.LENGTH_SHORT).show()
            } else if (readable.identities.toSet() != homeCoordinator.editMembership) {
                homeCoordinator.dismissEditMode()
                Toast.makeText(androidContext, favoritesChangedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(homeCoordinator.editMode) {
        if (!homeCoordinator.editMode) {
            favoriteEditor.invalidateUndo()
            homeBottomAppBarExpanded = false
        }
    }

    LaunchedEffect(favoriteState, homeCoordinator.selectedModuleId) {
        val selectedId = homeCoordinator.selectedModuleId
        val moduleIds = (favoriteState as? FavoriteReadState.Readable)
            ?.orderedModules
            ?.mapTo(mutableSetOf(), OrderedFavoriteModule::id)
            .orEmpty()
        if (selectedId != null && selectedId !in moduleIds) {
            homeCoordinator.selectModule(moduleId = null)
        }
    }

    LaunchedEffect(inventoryState) {
        if (homeCoordinator.editMode && inventoryState is LaunchableInventoryState.Error) {
            homeCoordinator.dismissEditMode()
            Toast.makeText(androidContext, inventoryFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(inventoryState, favoriteAddTarget, favoriteSelectionSaving) {
        if (favoriteAddTarget == null || favoriteSelectionSaving) return@LaunchedEffect
        val content = inventoryState as? LaunchableInventoryState.Content
            ?: return@LaunchedEffect
        val availableIdentities = content.snapshot.entries
            .mapTo(mutableSetOf<LaunchableIdentity>(), LaunchableEntry::identity)
        val temporarilyUnavailable = content.snapshot.profileReadStatus
            .filterValues { it == ProfileInventoryReadStatus.Unavailable }
            .keys
        favoriteSelection = favoriteSelection.filter { identity ->
            identity in availableIdentities || identity.profileSerialNumber in temporarilyUnavailable
        }
    }

    val favoriteMembership = (favoriteState as? FavoriteReadState.Readable)?.identities?.toSet()
    // Availability follows which favorites exist, not their order, so keying on membership keeps a
    // reorder from rebuilding the map and refreshing every row instead of the moved positions.
    LaunchedEffect(
        favoriteMembership,
        inventoryState,
        effectiveFavoriteStore,
        inventoryCoordinator,
        favoriteEditor.undoRemoval?.identity,
    ) {
        val identities = favoriteMembership ?: return@LaunchedEffect
        val completeInventory = inventoryState as? LaunchableInventoryState.Content
            ?: return@LaunchedEffect
        val resolved = inventoryCoordinator.resolveFavorites(
            identities = (identities + listOfNotNull(element = favoriteEditor.undoRemoval?.identity)).toList(),
            snapshot = completeInventory.snapshot,
        )
        favoriteAvailability = resolved
        favoriteEditor.reconcileAvailability(availability = resolved)
        val confirmedRemovedIdentities = resolved
            .filterValues { it == FavoriteAvailability.ConfirmedRemoved }
            .keys
        if (confirmedRemovedIdentities.isNotEmpty()) {
            effectiveFavoriteStore.removeAll(confirmedRemovedIdentities)
        }
    }

    fun settleTo(target: AppSurface) {
        drawerTransitionOwnsGesture = false
        homeTransitionOwnsGesture = false
        settleJob?.cancel()
        settleJob = scope.launch {
            val startProgress = progress
            val startOffset = drawerOffsetPx
            val targetProgress = if (target == AppSurface.Drawer) 1f else 0f
            val targetOffset = if (target == AppSurface.Drawer) 0f else containerHeightPx
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(),
            ) { fraction, _ ->
                val boundedFraction = fraction.coerceIn(0f, 1f)
                progress = startProgress + ((targetProgress - startProgress) * boundedFraction)
                drawerOffsetPx = startOffset + ((targetOffset - startOffset) * boundedFraction)
            }
            settledSurface = target
            gestureDisplacementPx = 0f
        }
    }

    fun beginGesture(origin: AppSurface) {
        settleJob?.cancel()
        gestureDisplacementPx = if (origin == AppSurface.Home) {
            progress * fullGestureDistancePx
        } else {
            (1f - progress) * fullGestureDistancePx
        }
        if (origin == AppSurface.Home) drawerActivated = true
    }

    fun dragTowardTarget(origin: AppSurface, targetDisplacement: Float) {
        val previousDisplacement = gestureDisplacementPx
        gestureDisplacementPx =
            (gestureDisplacementPx + targetDisplacement).coerceAtLeast(0f)
        val consumedTargetDisplacement = gestureDisplacementPx - previousDisplacement
        val gestureProgress = drawerGestureProgress(
            gestureDisplacementPx,
            fullGestureDistancePx,
        )
        progress = if (origin == AppSurface.Home) gestureProgress else 1f - gestureProgress
        drawerOffsetPx = if (origin == AppSurface.Home) {
            (drawerOffsetPx - drawerInteractiveDisplacement(consumedTargetDisplacement))
                .coerceIn(0f, containerHeightPx)
        } else {
            (drawerOffsetPx + drawerInteractiveDisplacement(consumedTargetDisplacement))
                .coerceIn(0f, containerHeightPx)
        }
    }

    fun finishGesture(origin: AppSurface, targetVelocity: Float) {
        val target = transitionTarget(
            origin = origin,
            gestureDisplacementPx = gestureDisplacementPx,
            completionThresholdPx = completionThresholdPx,
            targetVelocity = targetVelocity,
            targetFlingThresholdPx = targetFlingThresholdPx,
        )
        if (target == AppSurface.Home &&
            favoriteAddTarget != null &&
            !favoriteSelectionSaving
        ) {
            favoriteAddTarget = null
            favoriteSelection = emptyList()
        }
        settleTo(target)
    }

    fun returnToHome() {
        favoriteEditor.invalidateUndo()
        settleJob?.cancel()
        settledSurface = AppSurface.Home
        drawerActivated = false
        progress = 0f
        drawerOffsetPx = containerHeightPx
        gestureDisplacementPx = 0f
        drawerTransitionOwnsGesture = false
        homeTransitionOwnsGesture = false
        selectedEntry = null
        selectedEntryFromHome = false
        settingsOpen = false
        settingsPage = SettingsPage.Root
        homeCoordinator.dismissEditMode()
        homeCoordinator.clearEditSelection()
        if (!favoriteSelectionSaving) {
            favoriteAddTarget = null
            favoriteSelection = emptyList()
        }
    }

    fun closeFavoriteSelection() {
        favoriteAddTarget = null
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        settleTo(AppSurface.Home)
    }

    fun openFavoriteSelection(containerId: String) {
        if (favoriteEditor.isSaving) return
        val readable = favoriteState as? FavoriteReadState.Readable ?: return
        val modules = readable.orderedAggregate?.modules ?: return
        if (modules.none {
                it.id == containerId && it.type == OrderedFavoriteModuleType.Vertical
            }
        ) return
        favoriteEditor.invalidateUndo()
        favoriteAddTarget = FavoriteAddTarget(
            containerId = containerId,
            containerType = FavoriteContainerType.VerticalList,
            label = addToListLabel,
            provisional = false,
        )
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        drawerActivated = true
        settleTo(AppSurface.Drawer)
    }

    fun openProvisionalFavoriteSelection() {
        if (favoriteEditor.isSaving) return
        val state = favoriteState
        if (state !is FavoriteReadState.Readable) return
        favoriteEditor.invalidateUndo()
        favoriteAddTarget = FavoriteAddTarget(
            containerId = null,
            containerType = FavoriteContainerType.VerticalList,
            label = createListLabel,
            provisional = true,
        )
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        drawerActivated = true
        settleTo(AppSurface.Drawer)
    }

    fun openHomeBottomAppBarSelection() {
        if (favoriteEditor.isSaving) return
        val readable = favoriteState as? FavoriteReadState.Readable ?: return
        val bar = readable.orderedAggregate?.homeBottomAppBar ?: return
        val capacity = homeBottomAppBarCapacity(
            safeWidthDp = homeSafeWidthDp,
            outerMarginDp = androidContext.resources.getDimension(
                R.dimen.home_bottom_app_bar_outer_margin,
            ) / androidContext.resources.displayMetrics.density,
            iconSizeDp = androidContext.resources.getDimension(
                bar.iconSize.iconSizeResource(),
            ) / androidContext.resources.displayMetrics.density,
            horizontalInsetDp = androidContext.resources.getDimension(
                R.dimen.home_bottom_app_bar_preview_horizontal_inset,
            ) / androidContext.resources.displayMetrics.density,
            iconGapDp = androidContext.resources.getDimension(
                R.dimen.home_bottom_app_bar_preview_icon_gap,
            ) / androidContext.resources.displayMetrics.density,
        )
        val remainingPlaces = (capacity - bar.identities.size).coerceAtLeast(0)
        if (remainingPlaces == 0) return
        favoriteEditor.invalidateUndo()
        favoriteAddTarget = FavoriteAddTarget(
            containerId = null,
            containerType = FavoriteContainerType.FavoriteBar,
            label = addToHomeBottomAppBarLabel,
            provisional = false,
            homeBottomAppBar = true,
            remainingPlaces = remainingPlaces,
        )
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        drawerActivated = true
        settleTo(AppSurface.Drawer)
    }

    fun openFavoriteBarSelection(containerId: String) {
        if (favoriteEditor.isSaving) return
        val readable = favoriteState as? FavoriteReadState.Readable ?: return
        val modules = readable.orderedAggregate?.modules ?: return
        if (modules.none {
                it.id == containerId && it.type == OrderedFavoriteModuleType.Ribbon
            }
        ) return
        favoriteEditor.invalidateUndo()
        favoriteAddTarget = FavoriteAddTarget(
            containerId = containerId,
            containerType = FavoriteContainerType.FavoriteBar,
            label = addToFavoriteBarLabel,
            provisional = false,
        )
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        drawerActivated = true
        settleTo(AppSurface.Drawer)
    }

    fun openProvisionalFavoriteBarSelection() {
        if (favoriteEditor.isSaving) return
        if (favoriteState !is FavoriteReadState.Readable) return
        favoriteEditor.invalidateUndo()
        favoriteAddTarget = FavoriteAddTarget(
            containerId = null,
            containerType = FavoriteContainerType.FavoriteBar,
            label = createFavoriteBarLabel,
            provisional = true,
        )
        favoriteSelection = emptyList()
        favoriteSelectionSaving = false
        drawerActivated = true
        settleTo(AppSurface.Drawer)
    }

    fun confirmFavoriteSelection() {
        val target = favoriteAddTarget ?: return
        val selected = favoriteSelection
        if (selected.isEmpty() || favoriteSelectionSaving) return
        favoriteSelectionSaving = true
        scope.launch {
            val inventorySnapshot = (inventoryCoordinator.state.value
                as? LaunchableInventoryState.Content)?.snapshot
            val currentInventoryIdentities = inventorySnapshot?.entries
                ?.mapTo(mutableSetOf<LaunchableIdentity>(), LaunchableEntry::identity)
                .orEmpty()
            val temporarilyUnavailableIdentities = inventorySnapshot?.profileReadStatus
                ?.filterValues { it == ProfileInventoryReadStatus.Unavailable }
                ?.keys
                .orEmpty()
            var updatedAggregate: OrderedFavoriteAggregate? = null
            var targetInvalid = false
            val saved = if (target.homeBottomAppBar) {
                var appended = false
                effectiveFavoriteStore.updateOrderedAggregate { aggregate ->
                    val capacity = homeBottomAppBarCapacity(
                        safeWidthDp = homeSafeWidthDp,
                        outerMarginDp = androidContext.resources.getDimension(
                            R.dimen.home_bottom_app_bar_outer_margin,
                        ) / androidContext.resources.displayMetrics.density,
                        iconSizeDp = androidContext.resources.getDimension(
                            aggregate.homeBottomAppBar.iconSize.iconSizeResource(),
                        ) / androidContext.resources.displayMetrics.density,
                        horizontalInsetDp = androidContext.resources.getDimension(
                            R.dimen.home_bottom_app_bar_preview_horizontal_inset,
                        ) / androidContext.resources.displayMetrics.density,
                        iconGapDp = androidContext.resources.getDimension(
                            R.dimen.home_bottom_app_bar_preview_icon_gap,
                        ) / androidContext.resources.displayMetrics.density,
                    )
                    val appendable = selected.filter { identity ->
                        (identity in currentInventoryIdentities ||
                            identity.profileSerialNumber in temporarilyUnavailableIdentities) &&
                            identity !in aggregate.identities
                    }
                    val capturedPlaces = target.remainingPlaces
                        ?: (capacity - aggregate.homeBottomAppBar.identities.size)
                            .coerceAtLeast(0)
                    if (appendable.size != selected.size ||
                        appendable.size > capturedPlaces ||
                        aggregate.homeBottomAppBar.identities.size + appendable.size > capacity
                    ) {
                        targetInvalid = true
                        aggregate
                    } else {
                        appended = true
                        aggregate.copy(
                            homeBottomAppBar = aggregate.homeBottomAppBar.copy(
                                identities = aggregate.homeBottomAppBar.identities + appendable,
                            ),
                        )
                    }
                } != null && appended
            } else inventorySnapshot?.let {
                favoriteEditor.updateComposition(transform = { aggregate ->
                    val moduleType = when (target.containerType) {
                        FavoriteContainerType.VerticalList -> OrderedFavoriteModuleType.Vertical
                        FavoriteContainerType.FavoriteBar -> OrderedFavoriteModuleType.Ribbon
                    }
                    val container = target.containerId?.let { containerId ->
                        aggregate.modules.firstOrNull {
                            it.id == containerId && it.type == moduleType
                        }
                    }
                    if (target.containerId != null && container == null) {
                        targetInvalid = true
                        return@updateComposition aggregate
                    }
                    val appendable = selected.filter { identity ->
                        (identity in currentInventoryIdentities ||
                            identity.profileSerialNumber in temporarilyUnavailableIdentities) &&
                            identity !in aggregate.identities
                    }
                    if (appendable.isEmpty()) {
                        updatedAggregate = aggregate
                        return@updateComposition aggregate
                    }
                    val updatedContainer = container?.copy(
                        identities = container.identities + appendable,
                    ) ?: OrderedFavoriteModule(
                        id = nextOrderedModuleId(
                            prefix = if (target.containerType ==
                                FavoriteContainerType.VerticalList
                            ) {
                                "vertical-list"
                            } else {
                                "favorite-bar"
                            },
                            modules = aggregate.modules,
                        ),
                        type = moduleType,
                        identities = appendable,
                    )
                    val updated = aggregate.copy(
                        modules = if (target.provisional) {
                            aggregate.modules + updatedContainer
                        } else {
                            aggregate.modules.map { existing ->
                                if (existing.id == updatedContainer.id) {
                                    updatedContainer
                                } else {
                                    existing
                                }
                            }
                        },
                    )
                    updatedAggregate = updated
                    updated
                })
            }
            if (targetInvalid) {
                favoriteSelectionSaving = false
                Toast.makeText(
                    androidContext,
                    R.string.drawer_drag_unable_to_save_favorite,
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            if (saved == true) {
                updatedAggregate?.let(homeCoordinator::updateEditMembership)
                closeFavoriteSelection()
            } else {
                favoriteSelectionSaving = false
                if (!homeCoordinator.editMode) {
                    favoriteAddTarget = null
                    favoriteSelection = emptyList()
                }
                Toast.makeText(
                    androidContext,
                    R.string.favorite_reorder_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    LaunchedEffect(systemHomeEvents) {
        systemHomeEvents?.collect {
            returnToHome()
            // Consume the replayed event so an Activity recreation does not repeat the
            // return-to-home reset, while a pre-subscription emission stays deliverable.
            systemHomeEvents.resetReplayCache()
        }
    }

    DisposableEffect(inventoryCoordinator) {
        val cacheInvalidationObservation = inventoryCoordinator.observe {
            scope.launch {
                inventoryCoordinator.load(showLoading = false)
            }
        }
        onDispose { cacheInvalidationObservation?.stop() }
    }

    DisposableEffect(key1 = lifecycleOwner, key2 = inventoryCoordinator, key3 = favoriteEditor) {
        var hasResumed = false
        var wasPaused = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPaused = true
                    favoriteEditor.invalidateUndo()
                    homeCoordinator.dismissEditMode()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val isDefaultHome = currentSettingsPlatform.isDefaultHome()
                    val dismissedToday = currentPromptStore.isDismissedOn(
                        date = LocalDate.now(),
                    )
                    if (!hasResumed || wasPaused) {
                        showDefaultLauncherPrompt = defaultLauncherPromptSession
                            .evaluateForegroundEntry(
                                isDefaultHome = isDefaultHome,
                                dismissedToday = dismissedToday,
                            )
                    } else if (isDefaultHome) {
                        showDefaultLauncherPrompt = defaultLauncherPromptSession
                            .onBecameDefaultHome()
                    } else {
                        showDefaultLauncherPrompt = defaultLauncherPromptSession.isVisible(
                            isDefaultHome = isDefaultHome,
                        )
                    }
                    if (hasResumed && wasPaused) {
                        val shouldRefreshInventory = externalLaunchPendingReturn ||
                            inventoryRefreshPendingReturn
                        if (externalLaunchPendingReturn) {
                            returnToHome()
                        }
                        externalLaunchPendingReturn = false
                        inventoryRefreshPendingReturn = false
                        if (shouldRefreshInventory) {
                            scope.launch {
                                if (inventoryCoordinator.state.value is LaunchableInventoryState.Content) {
                                    inventoryCoordinator.load(showLoading = false)
                                }
                            }
                        }
                    }
                    hasResumed = true
                    wasPaused = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(
        key1 = windowInfo.isWindowFocused,
        block = {
            // A Launcher4Max action sheet is part of the journey; system-owned overlays are not.
            if (!windowInfo.isWindowFocused && selectedEntry == null) {
                favoriteEditor.invalidateUndo()
                homeCoordinator.dismissEditMode()
            }
        },
    )

    LaunchedEffect(
        key1 = settledSurface,
        block = {
            if (settledSurface != AppSurface.Home) favoriteEditor.invalidateUndo()
        },
    )

    BackHandler(enabled = favoriteAddTarget != null && !favoriteSelectionSaving) {
        closeFavoriteSelection()
    }

    BackHandler(enabled = favoriteAddTarget != null && favoriteSelectionSaving) {}

    BackHandler(enabled = homeCoordinator.editMode && favoriteAddTarget == null) {
        if (homeCoordinator.stylePanelExpanded) {
            homeCoordinator.updateStylePanelExpanded(expanded = false)
        } else {
            homeCoordinator.dismissEditMode()
            homeCoordinator.selectModule(moduleId = null)
        }
    }

    BackHandler(enabled = !homeCoordinator.editMode && !settingsOpen &&
        settledSurface == AppSurface.Home && progress == 0f &&
        homeCoordinator.stylePanelExpanded.not() &&
        selectedEntry == null &&
        homeBottomAppBarExpanded
    ) {
        homeBottomAppBarExpanded = false
    }

    BackHandler(enabled = !homeCoordinator.editMode && !settingsOpen &&
        (settledSurface == AppSurface.Drawer || progress > 0f)
    ) {
        settleTo(AppSurface.Home)
    }

    BackHandler(enabled = settingsOpen && settingsPage != SettingsPage.Root) {
        settingsPage = SettingsPage.Root
    }

    BackHandler(enabled = settingsOpen && settingsPage == SettingsPage.Root) {
        settingsOpen = false
    }

    val gestureModifier = Modifier.pointerInput(
        settledSurface,
        containerHeightPx,
        fullGestureDistancePx,
        completionThresholdPx,
        targetFlingThresholdPx,
    ) {
        val velocityTracker = VelocityTracker()
        var gestureOrigin = settledSurface

        detectVerticalDragGestures(
            onDragStart = { startPosition ->
                gestureOrigin = settledSurface
                beginGesture(gestureOrigin)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(0L, startPosition)
            },
            onVerticalDrag = { change, dragAmount ->
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val targetDisplacement = if (gestureOrigin == AppSurface.Home) {
                    -dragAmount
                } else {
                    dragAmount
                }
                dragTowardTarget(gestureOrigin, targetDisplacement)
            },
            onDragCancel = {
                settleTo(gestureOrigin)
            },
            onDragEnd = {
                val verticalVelocity = velocityTracker.calculateVelocity().y
                val targetVelocity = if (gestureOrigin == AppSurface.Home) {
                    -verticalVelocity
                } else {
                    verticalVelocity
                }
                finishGesture(gestureOrigin, targetVelocity)
            },
        )
    }

    val currentFavoriteSelectionSaving by rememberUpdatedState(
        favoriteSelectionSaving,
    )
    val drawerNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput ||
                    currentFavoriteSelectionSaving
                ) {
                    return Offset.Zero
                }
                val shouldTakeOwnership = drawerTransitionOwnsGesture ||
                    (available.y > 0f && !drawerListState.canScrollBackward)
                if (!shouldTakeOwnership) return Offset.Zero
                if (!drawerTransitionOwnsGesture) {
                    beginGesture(AppSurface.Drawer)
                    drawerTransitionOwnsGesture = true
                }
                dragTowardTarget(AppSurface.Drawer, available.y)
                return Offset(x = 0f, y = available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput ||
                    available.y <= 0f ||
                    currentFavoriteSelectionSaving
                ) {
                    return Offset.Zero
                }
                if (!drawerTransitionOwnsGesture) {
                    beginGesture(AppSurface.Drawer)
                    drawerTransitionOwnsGesture = true
                }
                dragTowardTarget(AppSurface.Drawer, available.y)
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (currentFavoriteSelectionSaving) return Velocity.Zero
                if (!drawerTransitionOwnsGesture) return Velocity.Zero
                finishGesture(AppSurface.Drawer, available.y)
                return Velocity(x = 0f, y = available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (currentFavoriteSelectionSaving) return Velocity.Zero
                if (!drawerTransitionOwnsGesture) return Velocity.Zero
                finishGesture(AppSurface.Drawer, available.y)
                return Velocity(x = 0f, y = available.y)
            }
        }
    }

    fun homeTransitionNestedScrollConnection(listState: LazyListState): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput ||
                    (!homeTransitionOwnsGesture && available.y >= 0f)
                ) {
                    return Offset.Zero
                }
                if (!homeTransitionOwnsGesture && listState.canScrollForward) {
                    return Offset.Zero
                }
                if (!homeTransitionOwnsGesture) {
                    beginGesture(AppSurface.Home)
                    homeTransitionOwnsGesture = true
                }
                dragTowardTarget(AppSurface.Home, -available.y)
                return Offset(x = 0f, y = available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f) {
                    return Offset.Zero
                }
                if (!homeTransitionOwnsGesture) {
                    beginGesture(AppSurface.Home)
                    homeTransitionOwnsGesture = true
                }
                dragTowardTarget(AppSurface.Home, -available.y)
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!homeTransitionOwnsGesture) return Velocity.Zero
                finishGesture(AppSurface.Home, -available.y)
                return Velocity(x = 0f, y = available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!homeTransitionOwnsGesture) return Velocity.Zero
                finishGesture(AppSurface.Home, -available.y)
                return Velocity(x = 0f, y = available.y)
            }
        }

    val homeNestedScrollConnection = remember(homeFavoriteListState) {
        homeTransitionNestedScrollConnection(homeFavoriteListState)
    }

    val drawerPointerSafetyModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var multiplePointersDetected = false
            var pointersRemainPressed: Boolean
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (!multiplePointersDetected && event.changes.count { it.pressed } > 1) {
                    multiplePointersDetected = true
                    if (drawerTransitionOwnsGesture) settleTo(AppSurface.Drawer)
                }
                if (multiplePointersDetected) {
                    event.changes.forEach { change -> change.consume() }
                }
                pointersRemainPressed = event.changes.any { it.pressed }
            } while (pointersRemainPressed)
        }
    }
    val homePointerSafetyModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var multiplePointersDetected = false
            var pointersRemainPressed: Boolean
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (!multiplePointersDetected && event.changes.count { it.pressed } > 1) {
                    multiplePointersDetected = true
                    settleTo(AppSurface.Home)
                }
                if (multiplePointersDetected) {
                    event.changes.forEach { change -> change.consume() }
                }
                pointersRemainPressed = event.changes.any { it.pressed }
            } while (pointersRemainPressed)
        }
    }
    val iconShapePageOpen = settingsOpen && settingsPage == SettingsPage.IconShape

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                rootOriginInWindow = coordinates.positionInWindow()
            }
            .onSizeChanged { size ->
                val previousHeight = containerHeightPx
                containerHeightPx = size.height.toFloat()
                if (settledSurface == AppSurface.Home && progress == 0f &&
                    (drawerOffsetPx == 0f || drawerOffsetPx == previousHeight)
                ) {
                    drawerOffsetPx = containerHeightPx
                }
            },
    ) {
        val homeSafeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val measuredHomeSafeWidthDp = maxWidth -
            homeSafeInsets.calculateLeftPadding(layoutDirection) -
            homeSafeInsets.calculateRightPadding(layoutDirection)
        homeSafeWidthDp = measuredHomeSafeWidthDp.value
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(
                    if (settingsOpen && settingsPage == SettingsPage.IconShape) {
                        0f
                    } else {
                        (1f - (2f * progress)).coerceIn(0f, 1f)
                    },
                )
                .then(if (iconShapePageOpen) Modifier else homePointerSafetyModifier)
                .then(
                    if (iconShapePageOpen || homeCoordinator.editMode) {
                        Modifier
                    } else {
                        gestureModifier
                    },
                )
                .testTag("home_surface"),
        ) {
            HomeScreen(
                favoriteState = favoriteState,
                favoriteListState = homeFavoriteListState,
                favoriteNestedScrollConnection =
                    homeNestedScrollConnection.takeUnless { homeCoordinator.editMode },
                accessibilityLockController = accessibilityLockController,
                quickActionBindings = quickActionBindings,
                showDefaultLauncherPrompt = showDefaultLauncherPrompt,
                onSelectDefaultLauncherPrompt = {
                    settingsPlatform.openDefaultHomeSettings()
                },
                onDismissDefaultLauncherPrompt = {
                    showDefaultLauncherPrompt = defaultLauncherPromptSession.onDismiss()
                    scope.launch {
                        effectiveDefaultLauncherPromptStore.dismissForLocalDate(
                            date = LocalDate.now(),
                        )
                    }
                },
                drawerDragJourney = homeCoordinator.drawerDragJourney,
                drawerDragTouchInWindow = homeCoordinator.drawerDragTouchPosition,
                drawerDragDropping = homeCoordinator.drawerDragDropping,
                onDrawerDragCommit = homeCoordinator::commitDrawerDrop,
                onDrawerDragJourneyFinished = homeCoordinator::finishDrawerDrag,
                favoriteAvailability = favoriteAvailability,
                editMode = homeCoordinator.editMode,
                stylePanelExpanded = homeCoordinator.stylePanelExpanded,
                selectedModuleId = homeCoordinator.selectedModuleId,
                applicationEditingSaving = favoriteEditor.isSaving,
                onRemoveApplication = { identity -> removeHomeFavorite(identity = identity) },
                onCommitApplicationOrder = homeCoordinator::commitApplicationOrder,
                removalSnackbarHostState = removalSnackbarHostState,
                onRetryFavorites = { scope.launch { effectiveFavoriteStore.load() } },
                onRequestEditMode = homeCoordinator::requestEditMode,
                onStylePanelExpandedChange = homeCoordinator::updateStylePanelExpanded,
                onSelectModule = homeCoordinator::selectModule,
                onLongPressFavorite = { entry ->
                    selectedEntryFromHome = true
                    selectedEntry = entry
                },
                onAddFavoritesToList = ::openFavoriteSelection,
                onAddProvisionalFavorites = ::openProvisionalFavoriteSelection,
                onAddFavoritesToBar = ::openFavoriteBarSelection,
                onAddProvisionalFavoriteBar = ::openProvisionalFavoriteBarSelection,
                onAddToHomeBottomAppBar = ::openHomeBottomAppBarSelection,
                homeBottomAppBarExpanded = homeBottomAppBarExpanded,
                onHomeBottomAppBarExpandedChange = { homeBottomAppBarExpanded = it },
                onCommitFavoriteComposition = homeCoordinator::commitFavoriteComposition,
                onCommitModuleOrder = favoriteEditor::reorderModules,
                onLaunchFavorite = { availability ->
                    when {
                        availability !is FavoriteAvailability.Available -> {
                            Toast.makeText(
                                androidContext,
                                unavailableFavoriteMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        homeActivationGuard.tryAcquire() -> {
                            if (entryLauncher.launch(availability.entry)) {
                                externalLaunchPendingReturn = true
                            } else {
                                Toast.makeText(
                                    androidContext,
                                    launchFailureMessage,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                scope.launch { inventoryCoordinator.load(showLoading = false) }
                            }
                        }
                    }
                },
            )
        }

        if (drawerActivated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, drawerOffsetPx.roundToInt()) }
                    .alpha(
                        if (settingsOpen && settingsPage == SettingsPage.IconShape) {
                            0f
                        } else {
                            (2f * progress).coerceIn(0f, 1f)
                        },
                    )
                    .then(if (iconShapePageOpen) Modifier else drawerPointerSafetyModifier)
                    .testTag("drawer_surface"),
            ) {
                DrawerScreen(
                    inventoryLoader = inventoryLoader,
                    inventoryCoordinator = inventoryCoordinator,
                    initialLoadHandledExternally = true,
                    entryLauncher = entryLauncher,
                    onExternalLaunch = { externalLaunchPendingReturn = true },
                    onNavigateBack = { settleTo(target = AppSurface.Home) },
                    modifier = Modifier.nestedScroll(drawerNestedScrollConnection),
                    listState = drawerListState,
                    active = !settingsOpen &&
                        (settledSurface == AppSurface.Drawer || progress > 0f),
                    displaySettings = presentedDrawerDisplaySettings,
                    displaySettingsReady = drawerDisplaySettingsReady,
                    displaySettingsMutationEnabled = drawerDisplaySettingsMutationEnabled,
                    onChangeDisplaySettings = { candidateSettings ->
                        val currentSettingsState =
                            effectiveDrawerDisplaySettingsStore.state.value
                        if (!drawerDisplaySettingsSaving &&
                            currentSettingsState is DrawerDisplaySettingsReadState.Readable
                        ) {
                            drawerDisplaySettingsCandidate = candidateSettings
                            drawerDisplaySettingsSaving = true
                            scope.launch(
                                block = {
                                    val saved = effectiveDrawerDisplaySettingsStore.replace(
                                        settings = candidateSettings,
                                    )
                                    if (!saved) {
                                        drawerDisplaySettingsCandidate = null
                                        if (onDrawerDisplaySettingsSaveFailure != null) {
                                            onDrawerDisplaySettingsSaveFailure()
                                        } else {
                                            Toast.makeText(
                                                androidContext,
                                                R.string.drawer_unable_to_save_display_settings,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                    drawerDisplaySettingsSaving = false
                                },
                            )
                        }
                    },
                    favoriteSelectionTarget = favoriteAddTarget?.label,
                    favoriteAvailability = favoriteAvailability,
                    onDragToFavoriteStart = { journey ->
                        homeCoordinator.startDrawerDrag(journey).also { started ->
                            if (started) {
                                drawerActivated = true
                                settleTo(target = AppSurface.Home)
                            }
                        }
                    },
                    onDragToFavoriteMove = { position ->
                        homeCoordinator.moveDrawerDrag(position)
                    },
                    onDragToFavoriteEnd = { _, cancelled ->
                        homeCoordinator.endDrawerDrag(cancelled)
                    },
                    favoriteSelection = favoriteSelection,
                    favoriteMembership = favoriteMembership.orEmpty(),
                    favoriteSelectionSaving = favoriteSelectionSaving,
                    favoriteSelectionLimit = favoriteAddTarget?.remainingPlaces,
                    onToggleFavoriteSelection = { identity ->
                        val limit = favoriteAddTarget?.remainingPlaces
                        if (identity !in favoriteMembership.orEmpty() &&
                            !favoriteSelectionSaving &&
                            (identity in favoriteSelection ||
                                limit == null ||
                                favoriteSelection.size < limit)
                        ) {
                            favoriteSelection = if (identity in favoriteSelection) {
                                favoriteSelection - identity
                            } else {
                                favoriteSelection + identity
                            }
                        }
                    },
                    onCancelFavoriteSelection = ::closeFavoriteSelection,
                    onConfirmFavoriteSelection = ::confirmFavoriteSelection,
                    onLongPress = { entry ->
                        selectedEntryFromHome = false
                        selectedEntry = entry
                    },
                    onOpenSettings = {
                        selectedEntry = null
                        settingsOpen = true
                    },
                )
            }
        }

        homeCoordinator.drawerDragJourney?.let { journey ->
            DrawerDragPreviewOverlay(
                journey = journey,
                displaySettings = presentedDrawerDisplaySettings,
                touchInWindow = homeCoordinator.drawerDragTouchPosition,
                rootOriginInWindow = rootOriginInWindow,
            )
        }

        selectedEntry?.let { entry ->
            // Both hosts evaluate the default-launcher state when the sheet opens.
            val isDefaultHome = remember(selectedEntry) {
                settingsPlatform.isDefaultHome()
            }
            ApplicationActionSheet(
                entry = entry,
                source = if (selectedEntryFromHome) {
                    ApplicationActionSheetSource.Home
                } else {
                    ApplicationActionSheetSource.Drawer
                },
                isDefaultHome = isDefaultHome,
                favoriteState = favoriteState,
                onDismiss = {
                    selectedEntry = null
                    selectedEntryFromHome = false
                },
                onAddFavorite = {
                    scope.launch(
                        block = {
                            if (effectiveFavoriteStore.add(identity = entry.identity)) {
                                favoriteEditor.invalidateUndo()
                            }
                        },
                    )
                    selectedEntry = null
                },
                onRemoveFavorite = {
                    if ((favoriteState as? FavoriteReadState.Readable)?.orderedModules != null) {
                        removeHomeFavorite(identity = entry.identity, offerUndo = selectedEntryFromHome)
                    } else {
                        scope.launch { effectiveFavoriteStore.remove(entry.identity) }
                    }
                    selectedEntry = null
                    selectedEntryFromHome = false
                },
                onEditFavorites = {
                    homeCoordinator.requestEditMode()
                    homeCoordinator.clearEditSelection()
                    selectedEntry = null
                    selectedEntryFromHome = false
                },
                canEditFavorites = selectedEntryFromHome,
                favoriteMutationEnabled = !favoriteEditor.isSaving,
                informationLauncher = informationLauncher,
                onInformationOpened = { inventoryRefreshPendingReturn = true },
                uninstallAvailable = selectedEntryFromHome &&
                    uninstallLauncher.isAvailable(entry = entry),
                onUninstall = { uninstallLauncher.open(entry = entry) },
                onUninstallOpened = { inventoryRefreshPendingReturn = true },
                shortcuts = applicationShortcuts.takeIf {
                    shortcutOwner == entry.identity
                }.orEmpty(),
                onShortcut = { shortcut ->
                    if (shortcutController.launch(shortcut)) {
                        externalLaunchPendingReturn = true
                    }
                    selectedEntry = null
                    selectedEntryFromHome = false
                },
            )
        }

        if (settingsOpen) {
            SettingsScreen(
                platform = settingsPlatform,
                licenseText = licenseText,
                thirdPartyNoticesText = thirdPartyNoticesText,
                accessibilityLockController = accessibilityLockController,
                backupController = settingsBackupController,
                bindings = quickActionBindings,
                iconShapeSettings = iconShapeSettings,
                iconShapeSaveInFlight = iconShapeSaveInFlight ||
                    iconShapeSettingsState !is IconShapeSettingsReadState.Readable,
                iconShapeSaveFailureToken = iconShapeSaveFailureToken,
                onCommitIconShapeSettings = { nextSettings ->
                    if (
                        !iconShapeSaveInFlight &&
                        settingsOpen && settingsPage == SettingsPage.IconShape &&
                        effectiveIconShapeSettingsStore.state.value is
                        IconShapeSettingsReadState.Readable
                    ) {
                        // Lock before launch so callbacks in the same frame cannot queue saves.
                        iconShapeSaveInFlight = true
                        scope.launch {
                            try {
                                if (!effectiveIconShapeSettingsStore.replace(nextSettings)) {
                                    iconShapeSaveFailureToken += 1
                                    Toast.makeText(
                                        androidContext,
                                        R.string.icon_shape_save_failure,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } finally {
                                iconShapeSaveInFlight = false
                            }
                        }
                    }
                },
                homeBottomAppBarState = homeBottomAppBarState,
                homeBottomAppBarSaveInFlight = homeBottomAppBarSaveInFlight ||
                    favoriteState !is FavoriteReadState.Readable,
                homeBottomAppBarSafeWidthDp = homeSafeWidthDp,
                onHomeBottomAppBarStateChange = { nextState ->
                    if (
                        !homeBottomAppBarSaveInFlight &&
                        favoriteState is FavoriteReadState.Readable
                    ) {
                        val previousState = homeBottomAppBarState
                        homeBottomAppBarState = nextState
                        homeBottomAppBarSaveInFlight = true
                        scope.launch {
                            try {
                                val saved = effectiveFavoriteStore.updateOrderedAggregate {
                                    it.copy(homeBottomAppBar = nextState)
                                } != null
                                if (!saved) {
                                    homeBottomAppBarState = previousState
                                    Toast.makeText(
                                        androidContext,
                                        R.string.home_bottom_app_bar_save_failure,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } finally {
                                homeBottomAppBarSaveInFlight = false
                            }
                        }
                    }
                },
                onBindQuickAction = { slot, action ->
                    scope.launch {
                        effectiveQuickActionBindingsStore.replace(
                            bindings = quickActionBindings.withSlot(
                                slot = slot,
                                action = action,
                            ),
                        )
                    }
                },
                page = settingsPage,
                onPageChange = { settingsPage = it },
                onBack = {
                    if (settingsPage != SettingsPage.Root) {
                        settingsPage = SettingsPage.Root
                    } else {
                        settingsOpen = false
                    }
                },
            )
        }
    }
}

private class InMemoryFavoriteStore : FavoriteStore {
    private val mutableState = kotlinx.coroutines.flow.MutableStateFlow<FavoriteReadState>(
        FavoriteReadState.Readable(
            aggregate = FavoriteAggregate(),
            orderedModules = emptyList(),
            orderedAggregate = OrderedFavoriteAggregate(),
        ),
    )
    override val state: kotlinx.coroutines.flow.StateFlow<FavoriteReadState> = mutableState
    override suspend fun load() = Unit
    override suspend fun add(identity: LaunchableIdentity): Boolean {
        return updateOrderedAggregate { aggregate ->
            if (identity in aggregate.identities) return@updateOrderedAggregate aggregate
            val first = aggregate.modules.firstOrNull()
            if (first?.type == OrderedFavoriteModuleType.Vertical) {
                aggregate.copy(
                    modules = listOf(first.copy(identities = first.identities + identity)) +
                        aggregate.modules.drop(1),
                )
            } else {
                aggregate.copy(
                    modules = aggregate.modules + OrderedFavoriteModule(
                        id = nextOrderedModuleId("vertical-list", aggregate.modules),
                        type = OrderedFavoriteModuleType.Vertical,
                        identities = listOf(identity),
                    ),
                )
            }
        } != null
    }
    override suspend fun remove(identity: LaunchableIdentity): Boolean =
        removeAll(setOf(identity))

    override suspend fun removeAll(identities: Set<LaunchableIdentity>): Boolean =
        updateOrderedAggregate { aggregate ->
            identities.fold(aggregate) { current, identity ->
                removeOrderedFavorite(current, identity)
            }
        } != null

    override suspend fun replaceModuleOrder(moduleIds: List<String>): Boolean {
        val current = (mutableState.value as? FavoriteReadState.Readable)
            ?.orderedAggregate ?: return false
        val modulesById = current.modules.associateBy { it.id }
        if (moduleIds.size != modulesById.size ||
            moduleIds.toSet() != modulesById.keys
        ) return false
        return updateOrderedAggregate { aggregate ->
            aggregate.copy(modules = moduleIds.map(modulesById::getValue))
        } != null
    }

    override suspend fun updateOrderedAggregate(
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ): OrderedFavoriteAggregate? {
        val current = (mutableState.value as? FavoriteReadState.Readable)
            ?.orderedAggregate ?: return null
        val updated = transform(current)
        if (!isValidOrderedFavoriteAggregate(updated)) return null
        mutableState.value = FavoriteReadState.Readable(
            aggregate = updated.toLegacyAggregate(),
            orderedModules = updated.modules,
            orderedAggregate = updated,
        )
        return updated
    }
}
