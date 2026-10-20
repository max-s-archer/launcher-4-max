package com.maxarchm.launcher

import android.content.ComponentName
import android.content.Context
import android.util.AtomicFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    suspend fun replaceOrder(identities: List<LaunchableIdentity>): Boolean
    suspend fun replaceComposition(
        primaryIdentities: List<LaunchableIdentity>,
        companionIdentities: List<LaunchableIdentity>,
    ): Boolean
    suspend fun replaceAggregate(aggregate: FavoriteAggregate): Boolean
    suspend fun updateAggregate(
        transform: (FavoriteAggregate) -> FavoriteAggregate,
    ): FavoriteAggregate?
    suspend fun replaceModuleOrder(moduleIds: List<String>): Boolean = false
    suspend fun updateOrderedAggregate(
        transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
    ): OrderedFavoriteAggregate? = null
}

internal class AtomicFileFavoriteStore private constructor(
    private val atomicFile: AtomicFile,
) : FavoriteStore {
    constructor(context: Context) : this(AtomicFile(context.filesDir.resolve(FILE_NAME)))
    internal constructor(file: java.io.File) : this(AtomicFile(file))
    private val mutationMutex = Mutex()
    private val mutableState = MutableStateFlow<FavoriteReadState>(FavoriteReadState.Loading)

    override val state: StateFlow<FavoriteReadState> = mutableState

    override suspend fun load() = mutationMutex.withLock {
        mutableState.value = FavoriteReadState.Loading
        mutableState.value = withContext(Dispatchers.IO) {
            if (!atomicFile.baseFile.exists()) {
                FavoriteReadState.Readable(emptyList())
            } else {
                try {
                    val document = readDocument()
                    val state = FavoriteReadState.Readable(document.aggregate)
                    if (document.schemaVersion == LEGACY_SCHEMA_VERSION ||
                        document.schemaVersion == LEGACY_COMPOSITION_SCHEMA_VERSION
                    ) {
                        // Favorites that were read successfully stay readable even when the
                        // upgrade write fails; the legacy document remains on disk and the next
                        // load retries the migration.
                        try {
                            writeDocument(state.aggregate)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            // Keep the readable state read from the legacy document.
                        }
                    }
                    state
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    FavoriteReadState.ReadFailure
                }
            }
        }
    }

    override suspend fun add(identity: LaunchableIdentity): Boolean = mutationMutex.withLock {
        val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
        if (identity in readable.identities) return true
        val primary = readable.aggregate.verticalLists
            .firstOrNull { it.id == PRIMARY_LIST_ID }
        val verticalLists = if (primary == null) {
            readable.aggregate.verticalLists + FavoriteContainer(
                id = PRIMARY_LIST_ID,
                type = FavoriteContainerType.VerticalList,
                identities = listOf(identity),
            )
        } else {
            readable.aggregate.verticalLists.replaceContainer(
                id = PRIMARY_LIST_ID,
                value = primary.copy(identities = primary.identities + identity),
            )
        }
        val updated = readable.aggregate.copy(verticalLists = verticalLists)
        val writeSucceeded = withContext(Dispatchers.IO) {
            try {
                writeDocument(updated)
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
        }
        if (writeSucceeded) {
            mutableState.value = FavoriteReadState.Readable(updated)
        }
        writeSucceeded
    }

    override suspend fun remove(identity: LaunchableIdentity): Boolean = mutationMutex.withLock {
        val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
        if (identity !in readable.identities) return true

        val updated = readable.aggregate.removeIdentity(identity)
        val writeSucceeded = withContext(Dispatchers.IO) {
            try {
                writeDocument(updated)
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
        }
        if (writeSucceeded) {
            mutableState.value = FavoriteReadState.Readable(updated)
        }
        writeSucceeded
    }

    override suspend fun removeAll(identities: Set<LaunchableIdentity>): Boolean =
        mutationMutex.withLock {
            val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
            val updated = readable.aggregate.removeIdentities(identities)
            if (updated == readable.aggregate) return true

            val writeSucceeded = withContext(Dispatchers.IO) {
                try {
                    writeDocument(updated)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
            }
            if (writeSucceeded) {
                mutableState.value = FavoriteReadState.Readable(updated)
            }
            writeSucceeded
        }

    override suspend fun replaceOrder(identities: List<LaunchableIdentity>): Boolean =
        mutationMutex.withLock {
            val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
            if (!isValidReplacement(readable.primaryIdentities, identities)) return false
            if (identities == readable.primaryIdentities) return true

            val updated = readable.aggregate.replaceVerticalList(
                id = PRIMARY_LIST_ID,
                identities = identities,
            )
            val writeSucceeded = withContext(Dispatchers.IO) {
                try {
                    writeDocument(updated)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
            }
            if (writeSucceeded) {
                mutableState.value = FavoriteReadState.Readable(updated)
            }
            writeSucceeded
        }

    override suspend fun replaceComposition(
        primaryIdentities: List<LaunchableIdentity>,
        companionIdentities: List<LaunchableIdentity>,
    ): Boolean = mutationMutex.withLock {
        val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
        val replacement = primaryIdentities + companionIdentities
        if (!isValidReplacement(readable.identities, replacement)) return false
        val updated = readable.aggregate.replaceLegacyComposition(
            primaryIdentities,
            companionIdentities,
        )
        if (updated == readable.aggregate) return true

        val writeSucceeded = try {
            withContext(Dispatchers.IO) {
                try {
                    writeDocument(updated)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
        if (writeSucceeded) {
            mutableState.value = FavoriteReadState.Readable(updated)
        }
        writeSucceeded
    }

    override suspend fun replaceAggregate(aggregate: FavoriteAggregate): Boolean =
        mutationMutex.withLock {
            val readable = mutableState.value as? FavoriteReadState.Readable ?: return false
            if (!isValidAggregate(aggregate)) return false
            if (aggregate == readable.aggregate) return true
            val writeSucceeded = withContext(Dispatchers.IO) {
                try {
                    writeDocument(aggregate)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
            }
            if (writeSucceeded) {
                mutableState.value = FavoriteReadState.Readable(aggregate)
            }
            writeSucceeded
        }

    override suspend fun updateAggregate(
        transform: (FavoriteAggregate) -> FavoriteAggregate,
    ): FavoriteAggregate? = mutationMutex.withLock {
        val readable = mutableState.value as? FavoriteReadState.Readable ?: return null
        val updated = transform(readable.aggregate)
        if (!isValidAggregate(updated)) return null
        if (updated == readable.aggregate) return readable.aggregate
        val writeSucceeded = withContext(Dispatchers.IO) {
            try {
                writeDocument(updated)
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
        }
        if (!writeSucceeded) return@withLock null
        mutableState.value = FavoriteReadState.Readable(updated)
        updated
    }

    private data class FavoriteDocument(
        val schemaVersion: Int,
        val aggregate: FavoriteAggregate,
    )

    private fun readDocument(): FavoriteDocument =
        DataInputStream(BufferedInputStream(atomicFile.openRead())).use { input ->
            require(input.readInt() == MAGIC) { "Unrecognized favorites document" }
            val schemaVersion = input.readInt()
            require(
                schemaVersion == LEGACY_SCHEMA_VERSION ||
                    schemaVersion == LEGACY_COMPOSITION_SCHEMA_VERSION ||
                    schemaVersion == SCHEMA_VERSION,
            ) { "Unsupported favorites schema" }
            fun readIdentities(count: Int): List<LaunchableIdentity> = buildList {
                repeat(count) {
                    val serial = input.readLong()
                    val flattenedComponent = input.readUTF()
                    val component = ComponentName.unflattenFromString(flattenedComponent)
                        ?: throw IllegalArgumentException("Invalid launchable component")
                    add(LaunchableIdentity(serial, component))
                }
            }
            val aggregate = if (schemaVersion == LEGACY_SCHEMA_VERSION ||
                schemaVersion == LEGACY_COMPOSITION_SCHEMA_VERSION
            ) {
                val primaryCount = input.readInt()
                require(primaryCount >= 0) { "Invalid primary favorite count" }
                val primary = readIdentities(primaryCount)
                val companion = if (schemaVersion == LEGACY_COMPOSITION_SCHEMA_VERSION) {
                    val companionCount = input.readInt()
                    require(companionCount >= 0) { "Invalid companion favorite count" }
                    readIdentities(companionCount)
                } else {
                    emptyList()
                }
                FavoriteAggregate(
                    verticalLists = legacyVerticalLists(primary, companion),
                )
            } else {
                require(schemaVersion == SCHEMA_VERSION) { "Unsupported favorites schema" }
                val containerCount = input.readInt()
                require(containerCount >= 0) { "Invalid favorite container count" }
                buildList {
                    repeat(containerCount) {
                        val id = input.readUTF()
                        val type = FavoriteContainerType.values().getOrNull(input.readInt())
                            ?: throw IllegalArgumentException("Invalid favorite container type")
                        val listSize = FavoriteListSize.values().getOrNull(input.readInt())
                            ?: throw IllegalArgumentException("Invalid favorite list size")
                        val identityCount = input.readInt()
                        require(identityCount > 0) { "Empty favorite container" }
                        add(
                            FavoriteContainer(
                                id = id,
                                type = type,
                                identities = readIdentities(identityCount),
                                listSize = listSize,
                            ),
                        )
                    }
                }.let { containers ->
                    FavoriteAggregate(
                        verticalLists = containers.filter {
                            it.type == FavoriteContainerType.VerticalList
                        },
                        favoriteBars = containers.filter {
                            it.type == FavoriteContainerType.FavoriteBar
                        },
                    )
                }
            }
            if (input.read() != -1) throw IllegalArgumentException("Trailing favorite data")
            require(isValidAggregate(aggregate)) { "Invalid favorite aggregate" }
            FavoriteDocument(schemaVersion, aggregate)
        }

    private fun writeDocument(aggregate: FavoriteAggregate) {
        var output: FileOutputStream? = atomicFile.startWrite()
        try {
            val data = DataOutputStream(BufferedOutputStream(checkNotNull(output)))
            data.writeInt(MAGIC)
            data.writeInt(SCHEMA_VERSION)
            val containers = aggregate.verticalLists + aggregate.favoriteBars
            data.writeInt(containers.size)
            containers.forEach { container ->
                data.writeUTF(container.id)
                data.writeInt(container.type.ordinal)
                data.writeInt(container.listSize.ordinal)
                data.writeInt(container.identities.size)
                container.identities.forEach { identity ->
                    data.writeLong(identity.profileSerialNumber)
                    data.writeUTF(identity.componentName.flattenToString())
                }
            }
            data.flush()
            atomicFile.finishWrite(output)
            output = null
        } catch (failure: Exception) {
            if (output != null) atomicFile.failWrite(output)
            throw failure
        }
    }

    private companion object {
        const val FILE_NAME = "favorites.bin"
        const val MAGIC = 0x4156454E
        const val LEGACY_SCHEMA_VERSION = 1
        const val LEGACY_COMPOSITION_SCHEMA_VERSION = 2
        const val SCHEMA_VERSION = 3
    }
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

internal fun FavoriteAggregate.updateVerticalList(
    id: String,
    transform: (FavoriteContainer) -> FavoriteContainer?,
): FavoriteAggregate = copy(
    verticalLists = verticalLists.mapNotNull { container ->
        if (container.id == id) transform(container) else container
    },
)

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

private fun FavoriteAggregate.replaceLegacyComposition(
    primaryIdentities: List<LaunchableIdentity>,
    companionIdentities: List<LaunchableIdentity>,
): FavoriteAggregate = copy(
    verticalLists = legacyVerticalLists(
        primaryIdentities,
        companionIdentities,
    ),
)

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
