package com.maxarcher.launcher

import kotlinx.coroutines.flow.StateFlow

enum class FavoriteContainerType {
    VerticalList,
    FavoriteBar,
}

enum class FavoriteListSize {
    Large,
    Medium,
    Small,
}

enum class FavoriteNamePlacement {
    Right,
    Below,
    Hidden,
}

data class FavoriteContainer(
    val id: String,
    val type: FavoriteContainerType,
    val identities: List<LaunchableIdentity>,
    val listSize: FavoriteListSize = FavoriteListSize.Medium,
    val iconSize: FavoriteListSize = listSize,
    val textSize: FavoriteListSize = listSize,
    val namePlacement: FavoriteNamePlacement = FavoriteNamePlacement.Right,
    val itemsPerRow: Int = 1,
)

data class FavoriteAggregate(
    val verticalLists: List<FavoriteContainer> = emptyList(),
    val favoriteBars: List<FavoriteContainer> = emptyList(),
) {
    val identities: List<LaunchableIdentity>
        get() = (verticalLists + favoriteBars).flatMap(FavoriteContainer::identities)
}

internal sealed interface FavoriteReadState {
    data object Loading : FavoriteReadState
    data class Readable(
        val aggregate: FavoriteAggregate,
        val orderedModules: List<OrderedFavoriteModule>? = null,
        val orderedAggregate: OrderedFavoriteAggregate? = null,
    ) : FavoriteReadState {
        constructor(
            primaryIdentities: List<LaunchableIdentity>,
            companionIdentities: List<LaunchableIdentity> = emptyList(),
        ) : this(
            aggregate = FavoriteAggregate(
                verticalLists = legacyVerticalLists(
                    primaryIdentities,
                    companionIdentities,
                ),
            ),
        )

        val primaryIdentities: List<LaunchableIdentity>
            get() = aggregate.verticalLists
                .firstOrNull { it.id == PRIMARY_LIST_ID }
                ?.identities
                .orEmpty()

        val companionIdentities: List<LaunchableIdentity>
            get() = aggregate.verticalLists
                .firstOrNull { it.id == COMPANION_LIST_ID }
                ?.identities
                .orEmpty()

        val identities: List<LaunchableIdentity>
            get() = orderedAggregate?.identities ?: aggregate.identities
    }
    data object ReadFailure : FavoriteReadState
}

internal interface FavoriteStore {
    val state: StateFlow<FavoriteReadState>
    suspend fun load()
    suspend fun add(identity: LaunchableIdentity): Boolean
    suspend fun remove(identity: LaunchableIdentity): Boolean
    suspend fun removeAll(identities: Set<LaunchableIdentity>): Boolean
    suspend fun replaceModuleOrder(moduleIds: List<String>): Boolean = false
    suspend fun updateOrderedAggregate(
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ): OrderedFavoriteAggregate? = null
}

const val PRIMARY_LIST_ID = "vertical-list-1"
private const val COMPANION_LIST_ID = "vertical-list-2"

private fun legacyVerticalLists(
    primaryIdentities: List<LaunchableIdentity>,
    companionIdentities: List<LaunchableIdentity>,
): List<FavoriteContainer> = buildList {
    if (primaryIdentities.isNotEmpty()) {
        add(
            FavoriteContainer(
                id = PRIMARY_LIST_ID,
                type = FavoriteContainerType.VerticalList,
                identities = primaryIdentities,
            ),
        )
    }
    if (companionIdentities.isNotEmpty()) {
        add(
            FavoriteContainer(
                id = COMPANION_LIST_ID,
                type = FavoriteContainerType.VerticalList,
                identities = companionIdentities,
            ),
        )
    }
}

private fun List<FavoriteContainer>.replaceContainer(
    id: String,
    value: FavoriteContainer,
): List<FavoriteContainer> = map { container ->
    if (container.id == id) value else container
}

fun FavoriteAggregate.removeIdentity(identity: LaunchableIdentity): FavoriteAggregate =
    copy(
        verticalLists = verticalLists.mapNotNull { container ->
            container.copy(identities = container.identities - identity)
                .takeIf { it.identities.isNotEmpty() }
        },
        favoriteBars = favoriteBars.mapNotNull { container ->
            container.copy(identities = container.identities - identity)
                .takeIf { it.identities.isNotEmpty() }
        },
    )

fun FavoriteAggregate.removeIdentities(
    identities: Set<LaunchableIdentity>,
): FavoriteAggregate = copy(
    verticalLists = verticalLists.mapNotNull { container ->
        container.copy(identities = container.identities.filterNot(identities::contains))
            .takeIf { it.identities.isNotEmpty() }
    },
    favoriteBars = favoriteBars.mapNotNull { container ->
        container.copy(identities = container.identities.filterNot(identities::contains))
            .takeIf { it.identities.isNotEmpty() }
    },
)

fun FavoriteAggregate.replaceVerticalList(
    id: String,
    identities: List<LaunchableIdentity>,
): FavoriteAggregate {
    if (identities.isEmpty()) {
        return copy(
            verticalLists = verticalLists.filterNot { it.id == id },
        )
    }
    val existing = verticalLists.firstOrNull { it.id == id }
    val replacement = existing?.copy(identities = identities) ?: FavoriteContainer(
        id = id,
        type = FavoriteContainerType.VerticalList,
        identities = identities,
    )
    return if (existing == null) {
        copy(verticalLists = verticalLists + replacement)
    } else {
        copy(verticalLists = verticalLists.replaceContainer(id, replacement))
    }
}

internal fun FavoriteAggregate.replaceVerticalComposition(
    primaryIdentities: List<LaunchableIdentity>,
    companionIdentities: List<LaunchableIdentity>,
): FavoriteAggregate {
    val replacementIdentities = listOf(primaryIdentities, companionIdentities)
    val updatedLists = replacementIdentities.mapIndexedNotNull { index, identities ->
        if (identities.isEmpty()) {
            null
        } else {
            verticalLists.getOrNull(index)?.copy(identities = identities)
                ?: FavoriteContainer(
                    id = if (index == 0) PRIMARY_LIST_ID else COMPANION_LIST_ID,
                    type = FavoriteContainerType.VerticalList,
                    identities = identities,
                )
        }
    }
    return copy(verticalLists = updatedLists)
}

internal fun FavoriteAggregate.moveFavorite(
    sourceContainerId: String,
    targetContainerId: String,
    identity: LaunchableIdentity,
    targetIndex: Int?,
    exchangeIdentity: LaunchableIdentity?,
): FavoriteAggregate {
    if (sourceContainerId == targetContainerId) return this
    val allContainers = verticalLists + favoriteBars
    val source = allContainers.firstOrNull { it.id == sourceContainerId } ?: return this
    val target = allContainers.firstOrNull { it.id == targetContainerId } ?: return this
    if (identity !in source.identities) return this
    if (exchangeIdentity != null && exchangeIdentity !in target.identities) return this
    val sourceUpdated = source.identities.filterNot { it == identity }
    val targetUpdated = target.identities.toMutableList()
    if (exchangeIdentity != null) {
        val targetSlot = targetUpdated.indexOf(exchangeIdentity)
        if (targetSlot < 0) return this
        targetUpdated[targetSlot] = identity
        val sourceSlot = sourceUpdated.size.coerceAtMost(source.identities.indexOf(identity))
        val sourceWithExchange = sourceUpdated.toMutableList().also {
            it.add(sourceSlot, exchangeIdentity)
        }
        return copy(
            verticalLists = verticalLists.mapNotNull { container ->
                when (container.id) {
                    sourceContainerId -> container.copy(identities = sourceWithExchange)
                        .takeIf { it.identities.isNotEmpty() }
                    targetContainerId -> container.copy(identities = targetUpdated)
                    else -> container
                }
            },
            favoriteBars = favoriteBars.mapNotNull { container ->
                when (container.id) {
                    sourceContainerId -> container.copy(identities = sourceWithExchange)
                        .takeIf { it.identities.isNotEmpty() }
                    targetContainerId -> container.copy(identities = targetUpdated)
                    else -> container
                }
            },
        )
    } else {
        targetUpdated.add(targetIndex?.coerceIn(0, targetUpdated.size) ?: targetUpdated.size, identity)
    }
    fun update(container: FavoriteContainer): FavoriteContainer? = when (container.id) {
        sourceContainerId -> container.copy(identities = sourceUpdated)
            .takeIf { it.identities.isNotEmpty() }
        targetContainerId -> container.copy(identities = targetUpdated)
        else -> container
    }
    return copy(
        verticalLists = verticalLists.mapNotNull(::update),
        favoriteBars = favoriteBars.mapNotNull(::update),
    )
}

internal fun FavoriteAggregate.containerForDragKey(
    key: String,
): FavoriteContainer? = when {
    key.startsWith("vertical-list:") ->
        verticalLists.firstOrNull { "vertical-list:${it.id}" == key }
    key.startsWith("favorite-bar:") ->
        favoriteBars.firstOrNull { "favorite-bar:${it.id}" == key }
    else -> null
}

internal fun FavoriteAggregate.removeIdentityFromContainer(
    containerId: String,
    identity: LaunchableIdentity,
): FavoriteAggregate = copy(
    verticalLists = verticalLists.mapNotNull { container ->
        if (container.id != containerId) {
            container
        } else {
            container.copy(identities = container.identities - identity)
                .takeIf { it.identities.isNotEmpty() }
        }
    },
    favoriteBars = favoriteBars.mapNotNull { container ->
        if (container.id != containerId) {
            container
        } else {
            container.copy(identities = container.identities - identity)
                .takeIf { it.identities.isNotEmpty() }
        }
    },
)

internal fun FavoriteAggregate.moveVerticalList(
    fromIndex: Int,
    toIndex: Int,
): FavoriteAggregate {
    if (fromIndex !in verticalLists.indices || toIndex !in verticalLists.indices) return this
    if (fromIndex == toIndex) return this
    val reordered = verticalLists.toMutableList()
    val moved = reordered.removeAt(fromIndex)
    reordered.add(toIndex, moved)
    return copy(verticalLists = reordered)
}

internal fun isValidAggregate(aggregate: FavoriteAggregate): Boolean {
    val containers = aggregate.verticalLists + aggregate.favoriteBars
    if (containers.any { it.identities.isEmpty() || it.id.isBlank() }) return false
    if (containers.map(FavoriteContainer::id).distinct().size != containers.size) return false
    val identities = containers.flatMap(FavoriteContainer::identities)
    return identities.distinct().size == identities.size &&
        aggregate.verticalLists.all { it.type == FavoriteContainerType.VerticalList } &&
        aggregate.favoriteBars.all { it.type == FavoriteContainerType.FavoriteBar } &&
        containers.all { container ->
            when (container.type) {
                FavoriteContainerType.VerticalList ->
                    container.listSize in FavoriteListSize.values() &&
                        container.itemsPerRow in when (container.namePlacement) {
                            FavoriteNamePlacement.Right -> 1..2
                            FavoriteNamePlacement.Below -> 1..4
                            FavoriteNamePlacement.Hidden -> 1..6
                        }
                FavoriteContainerType.FavoriteBar ->
                    container.listSize == FavoriteListSize.Medium &&
                        container.namePlacement == FavoriteNamePlacement.Right &&
                        container.itemsPerRow == 1
            }
        }
}

internal fun isValidReplacement(
    current: List<LaunchableIdentity>,
    replacement: List<LaunchableIdentity>,
): Boolean = replacement.size == current.size &&
    replacement.distinct().size == replacement.size &&
    replacement.toSet() == current.toSet()

internal fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
