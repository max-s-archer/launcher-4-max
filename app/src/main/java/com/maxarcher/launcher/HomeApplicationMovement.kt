package com.maxarcher.launcher

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.maxarcher.launcher.ui.drawer.DrawerDragDrop

/** Layout projection only: a temporarily empty source must never become a persisted empty module. */
internal fun applicationLayoutIdentities(
    module: OrderedFavoriteModule,
    movingIdentity: LaunchableIdentity?,
): List<LaunchableIdentity> = if (movingIdentity == null || movingIdentity !in module.identities) {
    module.identities
} else {
    module.identities.filterNot(predicate = { it == movingIdentity })
}

/** Boundaries index the destination with the source omitted; original orders only validate the save. */
internal data class ApplicationOrderChange(
    val moduleId: String,
    val identity: LaunchableIdentity,
    val originalOrder: List<LaunchableIdentity>,
    val boundary: Int,
    val destinationModuleId: String = moduleId,
    val destinationOrder: List<LaunchableIdentity> = originalOrder,
    val newModuleType: OrderedFavoriteModuleType? = null,
    val destinationBar: Boolean = false,
    val barCapacity: Int? = null,
) {
    fun reorderedIdentities(): List<LaunchableIdentity>? {
        val sourceIndex = originalOrder.indexOf(element = identity)
        if (sourceIndex < 0) return null
        if (newModuleType != null) return listOf(element = identity).takeIf(predicate = { boundary == 0 })
        if (destinationBar || destinationModuleId == HOME_BOTTOM_APP_BAR_DESTINATION_ID) {
            val capacity = barCapacity ?: return null
            val remaining = destinationOrder.filterNot(predicate = { it == identity })
            if (identity !in destinationOrder && remaining.size >= capacity) return null
            if (boundary !in 0..remaining.size) return null
            return remaining.toMutableList().apply(
                block = { add(index = boundary, element = identity) },
            )
        }
        if (destinationModuleId != moduleId) {
            if (identity in destinationOrder || boundary !in 0..destinationOrder.size) return null
            return destinationOrder.toMutableList().apply(block = { add(index = boundary, element = identity) })
        }
        val remaining = originalOrder.filterNot(predicate = { it == identity })
        if (boundary !in 0..remaining.size) return null
        if (boundary == sourceIndex) return originalOrder
        return remaining.toMutableList().apply(block = { add(index = boundary, element = identity) })
    }
}

/** Validates both orders against current durable state, never against a whole historical aggregate. */
internal fun applyApplicationOrderChange(
    aggregate: OrderedFavoriteAggregate,
    change: ApplicationOrderChange,
    newModuleId: String? = null,
): OrderedFavoriteAggregate? {
    val source = aggregate.modules.firstOrNull(predicate = { it.id == change.moduleId })
    val sourceOrder = source?.identities
        ?: aggregate.homeBottomAppBar.identities.takeIf(
            predicate = { change.moduleId == HOME_BOTTOM_APP_BAR_DESTINATION_ID },
        )
        ?: return null
    if (sourceOrder != change.originalOrder) return null
    if (change.destinationBar || change.destinationModuleId == HOME_BOTTOM_APP_BAR_DESTINATION_ID) {
        val reordered = change.reorderedIdentities() ?: return null
        val modules = aggregate.modules.mapNotNull(
            transform = { module ->
                if (module.id != source?.id) module else {
                    val remaining = module.identities.filterNot(
                        predicate = { it == change.identity },
                    )
                    if (remaining.isEmpty()) null else module.copy(identities = remaining)
                }
            },
        )
        return aggregate.copy(
            modules = modules,
            homeBottomAppBar = aggregate.homeBottomAppBar.copy(identities = reordered),
        )
    }
    if (change.newModuleType != null) {
        val identities = change.reorderedIdentities() ?: return null
        if (newModuleId.isNullOrBlank() || aggregate.modules.any(predicate = { it.id == newModuleId })) return null
        val created = OrderedFavoriteModule(id = newModuleId, type = change.newModuleType, identities = identities)
        val fromBar = change.moduleId == HOME_BOTTOM_APP_BAR_DESTINATION_ID
        val remaining = (if (fromBar) aggregate.homeBottomAppBar.identities else source?.identities)
            ?.filterNot(predicate = { it == change.identity })
            ?: return null
        return aggregate.copy(
            modules = aggregate.modules.mapNotNull(
                transform = { module ->
                    if (module.id != source?.id) {
                        module
                    } else if (remaining.isEmpty()) {
                        null
                    } else {
                        module.copy(identities = remaining)
                    }
                },
            ) + created,
            homeBottomAppBar = if (fromBar) {
                aggregate.homeBottomAppBar.copy(identities = remaining)
            } else {
                aggregate.homeBottomAppBar
            },
        )
    }
    val destination = aggregate.modules.firstOrNull(predicate = { it.id == change.destinationModuleId }) ?: return null
    val fromBar = change.moduleId == HOME_BOTTOM_APP_BAR_DESTINATION_ID
    val expectedDestination = if (source?.id == destination.id) {
        change.originalOrder
    } else {
        change.destinationOrder
    }
    val sourceIdentities = if (fromBar) {
        aggregate.homeBottomAppBar.identities
    } else {
        source?.identities
    }
    if (sourceIdentities != change.originalOrder || destination.identities != expectedDestination) {
        return null
    }
    val reordered = change.reorderedIdentities() ?: return null
    val barRemaining = aggregate.homeBottomAppBar.identities.filterNot(
        predicate = { it == change.identity },
    )
    return aggregate.copy(
        modules = aggregate.modules.mapNotNull(
            transform = { module ->
                when (module.id) {
                    destination.id -> module.copy(identities = reordered)
                    source?.id -> {
                    val remaining = module.identities.filterNot(
                        predicate = { it == change.identity },
                    )
                        if (remaining.isEmpty()) null else module.copy(identities = remaining)
                    }
                    else -> module
                }
            },
        ),
        homeBottomAppBar = if (fromBar) {
            aggregate.homeBottomAppBar.copy(identities = barRemaining)
        } else {
            aggregate.homeBottomAppBar
        },
    )
}

internal const val EXTERNAL_JOURNEY_MODULE_ID = "drawer-journey-source"
internal const val HOME_BOTTOM_APP_BAR_DESTINATION_ID = "home-bottom-app-bar"

internal data class HomeApplicationMovementSession(
    val module: OrderedFavoriteModule,
    val identity: LaunchableIdentity,
    val availability: FavoriteAvailability,
    val sourceBounds: Rect,
    val pointerOffset: Offset,
    val pointer: Offset,
    val insertion: ApplicationInsertion? = null,
    val destination: OrderedFavoriteModule? = null,
    val creation: Pair<OrderedFavoriteModuleType, Rect>? = null,
    val barInsertion: ApplicationInsertion? = null,
) {
    val previewOrigin: Offset get() = pointer - pointerOffset
}

/** Transient gesture state only. All durable writes continue through the App's favorite editor. */
@Stable
internal class HomeApplicationMovement {
    // Item/layout scopes observe ownership and phase, not pointer or candidate geometry.
    var activeIdentity by mutableStateOf<LaunchableIdentity?>(value = null)
        private set
    var session by mutableStateOf<HomeApplicationMovementSession?>(value = null)
        private set
    // Frozen preview content is observed independently from pointer/candidate geometry.
    var previewSource by mutableStateOf<HomeApplicationMovementSession?>(value = null)
        private set
    var pendingChange by mutableStateOf<ApplicationOrderChange?>(value = null)
        private set
    val isDragging: Boolean get() = activeIdentity != null && pendingChange == null
    var edgeFeedback by mutableStateOf<HomeApplicationScrollRequest?>(value = null)
        private set
    var viewport = Rect.Zero
        private set
    private val moduleBounds = mutableMapOf<String, Rect>()
    private val itemBounds = mutableMapOf<LaunchableIdentity, Rect>()
    private val removeBounds = mutableMapOf<LaunchableIdentity, Rect>()
    private val addBounds = mutableMapOf<String, Rect>()
    private val creationBounds = mutableMapOf<OrderedFavoriteModuleType, Rect>()
    private var barBounds: Rect? = null
    private var barIdentities: List<LaunchableIdentity> = emptyList()
    private var barCapacity: Int = 0
    private var barAddBounds: Rect? = null
    private var modules: List<OrderedFavoriteModule> = emptyList()
    var geometryRevision by mutableStateOf(value = 0L)
        private set

    fun updateEdgeFeedback(request: HomeApplicationScrollRequest?) {
        edgeFeedback = request.takeIf(predicate = { isDragging })
    }

    fun updateModules(current: List<OrderedFavoriteModule>) {
        if (modules == current) return
        modules = current
        geometryRevision += 1
        refreshCandidate()
    }

    fun visibleRibbonAt(pointer: Offset): Pair<String, Rect>? {
        return modules.firstNotNullOfOrNull(
            transform = { module ->
                if (module.type != OrderedFavoriteModuleType.Ribbon) return@firstNotNullOfOrNull null
                val bounds = moduleBounds[module.id]?.intersect(other = viewport) ?: return@firstNotNullOfOrNull null
                (module.id to bounds).takeIf(predicate = {
                    !bounds.isEmpty && pointer.x in bounds.left..bounds.right && pointer.y in bounds.top..bounds.bottom
                })
            },
        )
    }

    fun updateViewport(bounds: Rect) {
        if (viewport == bounds) return
        viewport = bounds
        geometryRevision += 1
        refreshCandidate()
    }

    fun updateModule(id: String, bounds: Rect?) {
        if (moduleBounds[id] == bounds) return
        geometryRevision += 1
        if (bounds == null) {
            moduleBounds.remove(key = id)
            addBounds.remove(key = id)
        } else {
            moduleBounds[id] = bounds
        }
        refreshCandidate()
    }

    fun updateItem(identity: LaunchableIdentity, bounds: Rect?, remove: Rect? = null) {
        if (itemBounds[identity] == bounds && removeBounds[identity] == remove) return
        if (bounds == null) itemBounds.remove(key = identity) else itemBounds[identity] = bounds
        if (remove == null) removeBounds.remove(key = identity) else removeBounds[identity] = remove
        refreshCandidate()
    }

    fun updateAdd(id: String, bounds: Rect?) {
        if (addBounds[id] == bounds) return
        if (bounds == null) addBounds.remove(key = id) else addBounds[id] = bounds
        refreshCandidate()
    }

    fun updateHomeBottomAppBar(
        bounds: Rect?,
        identities: List<LaunchableIdentity>,
        capacity: Int,
        addBounds: Rect?,
    ) {
        if (barBounds == bounds && barIdentities == identities &&
            barCapacity == capacity && barAddBounds == addBounds
        ) return
        barBounds = bounds
        barIdentities = identities
        barCapacity = capacity
        barAddBounds = addBounds
        geometryRevision += 1
        refreshCandidate()
    }

    fun updateCreationTarget(type: OrderedFavoriteModuleType, bounds: Rect?) {
        if (creationBounds[type] == bounds) return
        if (bounds == null) creationBounds.remove(key = type) else creationBounds[type] = bounds
        refreshCandidate()
    }

    fun hitIdentity(pointer: Offset): LaunchableIdentity? {
        return itemBounds.entries.firstOrNull(
            predicate = { (identity, bounds) ->
                bounds.contains(offset = pointer) &&
                    removeBounds[identity]?.contains(offset = pointer) != true &&
                    viewport.contains(offset = pointer) ||
                    barBounds?.contains(offset = pointer) == true
            },
        )?.key
    }

    fun start(
        identity: LaunchableIdentity,
        module: OrderedFavoriteModule,
        availability: FavoriteAvailability,
        pointer: Offset,
        carriedBounds: Rect? = null,
    ): Boolean {
        val barSource = identity in barIdentities
        val sourceModule = if (barSource) {
            OrderedFavoriteModule(
                id = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
                type = OrderedFavoriteModuleType.Ribbon,
                identities = barIdentities,
            )
        } else {
            module
        }
        if (session != null || identity !in sourceModule.identities ||
            (availability !is FavoriteAvailability.Available && availability !is FavoriteAvailability.Disabled)
        ) return false
        val bounds = carriedBounds ?: itemBounds[identity] ?: return false
        val source = HomeApplicationMovementSession(
            module = sourceModule,
            identity = identity,
            availability = availability,
            sourceBounds = bounds,
            pointerOffset = pointer - bounds.topLeft,
            pointer = pointer,
        )
        previewSource = source
        session = source
        activeIdentity = identity
        // The preview owns this identity now, even before its old layout node is disposed.
        itemBounds.remove(key = identity)
        removeBounds.remove(key = identity)
        refreshCandidate()
        return true
    }

    fun move(pointer: Offset) {
        if (!isDragging) return
        session = session?.copy(pointer = pointer)
        refreshCandidate()
    }

    private fun refreshCandidate() {
        if (!isDragging) return
        val current = session ?: return
        val creation = creationBounds.entries.firstOrNull(
            predicate = { (_, bounds) -> viewport.contains(offset = current.pointer) && bounds.contains(offset = current.pointer) },
        )?.let(block = { it.key to it.value })
        if (creation != null) {
            session = current.copy(
                insertion = null,
                destination = null,
                creation = creation,
                barInsertion = null,
            )
            return
        }
        val barInsertion = resolveHomeBottomAppBarInsertion(
            bounds = barBounds,
            identities = barIdentities,
            capacity = barCapacity,
            addBounds = barAddBounds,
            itemBounds = itemBounds,
            pointer = current.pointer,
            movingIdentity = current.identity,
        )
        if (barInsertion != null) {
            session = current.copy(
                insertion = null,
                destination = null,
                creation = null,
                barInsertion = barInsertion,
            )
            return
        }
        val resolved = resolveApplicationDestination(
            modules = modules.ifEmpty(defaultValue = { listOf(element = current.module) }),
            pointer = current.pointer, viewport = viewport, moduleBounds = moduleBounds,
            itemBounds = itemBounds, addBounds = addBounds,
            movingIdentity = current.identity,
        )
        session = current.copy(
            insertion = resolved?.second,
            destination = resolved?.first,
            creation = null,
            barInsertion = null,
        )
    }

    fun cancel() {
        session = null
        previewSource = null
        activeIdentity = null
        pendingChange = null
        edgeFeedback = null
    }

    /**
     * Starts candidate tracking for the Drawer drag-to-favorite journey: the dragged
     * identity originates outside every favorite module, so no source row is omitted
     * and no Home-side preview is created; the Drawer preview owns the presentation.
     */
    fun startExternalJourney(identity: LaunchableIdentity, pointer: Offset): Boolean {
        if (session != null) return false
        session = HomeApplicationMovementSession(
            module = OrderedFavoriteModule(
                id = EXTERNAL_JOURNEY_MODULE_ID,
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(element = identity),
            ),
            identity = identity,
            availability = FavoriteAvailability.Unknown(presentationEntry = null),
            sourceBounds = Rect.Zero,
            pointerOffset = Offset.Zero,
            pointer = pointer,
        )
        activeIdentity = identity
        refreshCandidate()
        return true
    }

    /** Resolves the journey release into its drop target, or null for an invalid release. */
    fun finishExternalJourney(): DrawerDragDrop? {
        // Only the external journey session may be consumed here; a concurrent
        // Home-originated drag session must survive this call untouched.
        if (session?.module?.id != EXTERNAL_JOURNEY_MODULE_ID) return null
        refreshCandidate()
        val resolved = session ?: return null
        val drop = when {
            resolved.creation != null -> DrawerDragDrop.Creation(
                moduleType = resolved.creation.first,
            )
            resolved.insertion != null && resolved.destination != null -> DrawerDragDrop.Insertion(
                moduleId = resolved.destination.id,
                boundary = resolved.insertion.boundary,
            )
            resolved.barInsertion != null -> DrawerDragDrop.HomeBottomAppBar(
                boundary = resolved.barInsertion.boundary,
            )
            else -> null
        }
        cancel()
        return drop
    }

    /** Only this save may hand off its preview; a late callback cannot clear a newer gesture. */
    fun complete(change: ApplicationOrderChange) {
        if (pendingChange === change) cancel()
    }

    fun finish(): ApplicationOrderChange? {
        if (!isDragging) return null
        refreshCandidate()
        val current = session ?: return null
        val change = if (current.creation != null) {
            ApplicationOrderChange(
                moduleId = current.module.id, identity = current.identity,
                originalOrder = current.module.identities, boundary = 0,
                newModuleType = current.creation.first,
            )
        } else if (current.barInsertion != null) {
            ApplicationOrderChange(
                moduleId = current.module.id,
                identity = current.identity,
                originalOrder = if (
                    current.module.id == HOME_BOTTOM_APP_BAR_DESTINATION_ID
                ) {
                    barIdentities
                } else {
                    current.module.identities
                },
                boundary = current.barInsertion.boundary,
                destinationModuleId = HOME_BOTTOM_APP_BAR_DESTINATION_ID,
                destinationOrder = barIdentities,
                destinationBar = true,
                barCapacity = barCapacity,
            )
        } else {
            current.insertion?.let(block = { insertion ->
                current.destination?.let(block = { destination ->
                    ApplicationOrderChange(
                        moduleId = current.module.id, identity = current.identity,
                        originalOrder = current.module.identities, boundary = insertion.boundary,
                        destinationModuleId = destination.id, destinationOrder = destination.identities,
                    )
                })
            })
        }
        if (change == null || change.reorderedIdentities() == null ||
            (change.newModuleType == null && change.destinationModuleId == change.moduleId && change.reorderedIdentities() == change.originalOrder)
        ) {
            cancel()
            return null
        }
        // Keep source omission and the preview together until the reliable state reaches the UI.
        pendingChange = change
        session = current.copy(
            insertion = null,
            destination = null,
            creation = null,
            barInsertion = null,
        )
        edgeFeedback = null
        return change
    }
}
