package com.maxarcher.launcher

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteStoreTest {
    private companion object {
        const val EXPECTED_COMPANION_LIST_ID = "vertical-list-2"
    }

    @Test
    fun contextStoreWritesFavoritesInsideTheExcludedFilesDirectory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.filesDir.resolve("favorites.bin")
        val newFile = context.filesDir.resolve("favorites.bin.new")
        val legacyBackupFile = context.filesDir.resolve("favorites.bin.bak")
        file.delete()
        newFile.delete()
        legacyBackupFile.delete()
        val store = AtomicFileFavoriteStore(context)

        try {
            store.load()
            assertTrue(store.add(identity(1, "com.example", "Main")))

            assertTrue(file.isFile)
            assertEquals(context.filesDir.canonicalFile, file.parentFile?.canonicalFile)
        } finally {
            file.delete()
            newFile.delete()
            legacyBackupFile.delete()
        }
    }

    @Test
    fun addDeduplicateRemoveAndBatchRemovePreserveOrder(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val store = AtomicFileFavoriteStore(file)
        val first = identity(1, "com.example.first", "Main")
        val second = identity(1, "com.example.second", "Main")
        val clone = identity(2, "com.example.first", "Main")

        store.load()
        assertTrue(store.add(first))
        assertTrue(store.add(second))
        assertTrue(store.add(first))
        assertTrue(store.add(clone))
        assertEquals(listOf(first, second, clone), store.readableIdentities())
        val reloadedStore = AtomicFileFavoriteStore(file)
        reloadedStore.load()
        assertEquals(listOf(first, second, clone), reloadedStore.readableIdentities())

        assertTrue(store.remove(second))
        assertTrue(store.removeAll(setOf(first)))
        assertEquals(listOf(clone), store.readableIdentities())

        file.delete()
    }

    @Test
    fun damagedDocumentPublishesFailureAndMutationsStayDisabled(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0)
            output.writeInt(1)
            output.writeInt(0)
        }
        val original = file.readBytes()
        val store = AtomicFileFavoriteStore(file)

        store.load()

        assertEquals(FavoriteReadState.ReadFailure, store.state.value)
        assertFalse(store.add(identity(1, "com.example", "Main")))
        assertFalse(store.remove(identity(1, "com.example", "Main")))
        assertEquals(original.toList(), file.readBytes().toList())

        file.delete()
    }

    @Test
    fun failedWriteKeepsLastReadableState(): Unit = runBlocking {
        val blockedParent = temporaryFavoriteFile().apply { writeText("not a directory") }
        val file = File(blockedParent, "favorites.bin")
        val store = AtomicFileFavoriteStore(file)
        val identity = identity(1, "com.example", "Main")
        store.load()

        assertFalse(store.add(identity))
        assertEquals(emptyList<LaunchableIdentity>(), store.readableIdentities())

        blockedParent.delete()
    }

    @Test
    fun replaceOrderPersistsOnlyACompletePermutation(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val store = AtomicFileFavoriteStore(file)
        val first = identity(1, "com.example.first", "Main")
        val second = identity(1, "com.example.second", "Main")
        val third = identity(2, "com.example.third", "Main")
        store.load()
        store.add(first)
        store.add(second)
        store.add(third)

        assertFalse(store.replaceOrder(listOf(first, first, third)))
        assertFalse(store.replaceOrder(listOf(first, second)))
        assertEquals(listOf(first, second, third), store.readableIdentities())
        assertTrue(store.replaceOrder(listOf(third, first, second)))

        val reloadedStore = AtomicFileFavoriteStore(file)
        reloadedStore.load()
        assertEquals(listOf(third, first, second), reloadedStore.readableIdentities())
        file.delete()
    }

    @Test
    fun legacySchemaMigratesFavoritesToPrimaryInOriginalOrder(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val first = identity(1, "com.example.first", "Main")
        val second = identity(2, "com.example.second", "Main")
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(1)
            output.writeInt(2)
            listOf(first, second).forEach {
                output.writeLong(it.profileSerialNumber)
                output.writeUTF(it.componentName.flattenToString())
            }
        }

        val store = AtomicFileFavoriteStore(file)
        store.load()

        val readable = store.state.value as FavoriteReadState.Readable
        assertEquals(listOf(first, second), readable.primaryIdentities)
        assertEquals(emptyList<LaunchableIdentity>(), readable.companionIdentities)
        file.delete()
    }

    @Test
    fun legacyCompositionMigratesPrimaryAndCompanionToMediumLists(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val first = identity(1, "com.example.first", "Main")
        val second = identity(2, "com.example.second", "Main")
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(2)
            output.writeInt(1)
            writeIdentity(output, first)
            output.writeInt(1)
            writeIdentity(output, second)
        }

        val store = AtomicFileFavoriteStore(file)
        store.load()

        val readable = store.state.value as FavoriteReadState.Readable
        assertEquals(
            listOf(
                FavoriteContainer(
                    id = "vertical-list-1",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(first),
                ),
                FavoriteContainer(
                    id = "vertical-list-2",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(second),
                ),
            ),
            readable.aggregate.verticalLists,
        )
        assertTrue(readable.aggregate.favoriteBars.isEmpty())
        file.delete()
    }

    @Test
    fun legacyCompositionPreservesCompanionSlotWhenPrimaryIsEmpty(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val companion = identity(2, "com.example.companion", "Main")
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(2)
            output.writeInt(0)
            output.writeInt(1)
            writeIdentity(output, companion)
        }

        val store = AtomicFileFavoriteStore(file)
        store.load()

        val readable = store.state.value as FavoriteReadState.Readable
        assertTrue(readable.primaryIdentities.isEmpty())
        assertEquals(listOf(companion), readable.companionIdentities)
        assertEquals(EXPECTED_COMPANION_LIST_ID, readable.aggregate.verticalLists.single().id)
        file.delete()
    }

    @Test
    fun aggregateRoundTripsContainerPropertiesAndOrder(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val first = identity(1, "com.example.first", "Main")
        val second = identity(1, "com.example.second", "Main")
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(
                FavoriteContainer(
                    id = "vertical-list-1",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(first),
                    listSize = FavoriteListSize.Large,
                ),
            ),
            favoriteBars = listOf(
                FavoriteContainer(
                    id = "favorite-bar-1",
                    type = FavoriteContainerType.FavoriteBar,
                    identities = listOf(second),
                ),
            ),
        )
        val store = AtomicFileFavoriteStore(file)
        store.load()

        assertTrue(store.replaceAggregate(aggregate))

        val reloadedStore = AtomicFileFavoriteStore(file)
        reloadedStore.load()
        assertEquals(
            aggregate,
            (reloadedStore.state.value as FavoriteReadState.Readable).aggregate,
        )
        file.delete()
    }

    @Test
    fun invalidAggregateRejectsDuplicateIdentityAndEmptyContainer(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val identity = identity(1, "com.example", "Main")
        val store = AtomicFileFavoriteStore(file)
        store.load()

        assertFalse(
            store.replaceAggregate(
                FavoriteAggregate(
                    verticalLists = listOf(
                        FavoriteContainer(
                            id = "vertical-list-1",
                            type = FavoriteContainerType.VerticalList,
                            identities = listOf(identity),
                        ),
                        FavoriteContainer(
                            id = "vertical-list-2",
                            type = FavoriteContainerType.VerticalList,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(
            store.replaceAggregate(
                FavoriteAggregate(
                    favoriteBars = listOf(
                        FavoriteContainer(
                            id = "favorite-bar-1",
                            type = FavoriteContainerType.FavoriteBar,
                            identities = emptyList(),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(
            (store.state.value as FavoriteReadState.Readable).aggregate.identities.isEmpty(),
        )
        file.delete()
    }

    @Test
    fun invalidAggregateRejectsContainersInTheWrongGroup(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val identity = identity(1, "com.example", "Main")
        val store = AtomicFileFavoriteStore(file)
        store.load()

        assertFalse(
            store.replaceAggregate(
                FavoriteAggregate(
                    verticalLists = listOf(
                        FavoriteContainer(
                            id = "favorite-bar-1",
                            type = FavoriteContainerType.FavoriteBar,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(
            store.replaceAggregate(
                FavoriteAggregate(
                    favoriteBars = listOf(
                        FavoriteContainer(
                            id = PRIMARY_LIST_ID,
                            type = FavoriteContainerType.VerticalList,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(
            (store.state.value as FavoriteReadState.Readable).aggregate.identities.isEmpty(),
        )
        file.delete()
    }

    @Test
    fun replaceCompositionPersistsBothGroups(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val first = identity(1, "com.example.first", "Main")
        val second = identity(1, "com.example.second", "Main")
        val third = identity(2, "com.example.third", "Main")
        val store = AtomicFileFavoriteStore(file)
        store.load()
        store.add(first)
        store.add(second)
        store.add(third)

        assertTrue(store.replaceComposition(listOf(second), listOf(third, first)))

        val reloadedStore = AtomicFileFavoriteStore(file)
        reloadedStore.load()
        val readable = reloadedStore.state.value as FavoriteReadState.Readable
        assertEquals(listOf(second), readable.primaryIdentities)
        assertEquals(listOf(third, first), readable.companionIdentities)
        file.delete()
    }

    @Test
    fun removingPrimaryListDoesNotPromoteCompanion(): Unit = runBlocking {
        val file = temporaryFavoriteFile()
        val primary = identity(1, "com.example.primary", "Main")
        val companion = identity(2, "com.example.companion", "Main")
        val store = AtomicFileFavoriteStore(file)
        store.load()
        store.add(primary)
        store.add(companion)
        assertTrue(store.replaceComposition(listOf(primary), listOf(companion)))

        assertTrue(store.remove(primary))

        val readable = store.state.value as FavoriteReadState.Readable
        assertTrue(readable.primaryIdentities.isEmpty())
        assertEquals(listOf(companion), readable.companionIdentities)
        assertEquals(EXPECTED_COMPANION_LIST_ID, readable.aggregate.verticalLists.single().id)
        file.delete()
    }

    @Test
    fun failedCompositionWriteDoesNotPublishUnpersistedState(): Unit = runBlocking {
        val container = temporaryFavoriteFile().apply { assertTrue(mkdir()) }
        val file = File(container, "favorites.bin")
        val first = identity(1, "com.example.first", "Main")
        val second = identity(1, "com.example.second", "Main")
        val store = AtomicFileFavoriteStore(file)
        store.load()
        store.add(first)
        store.add(second)
        val before = store.state.value
        assertTrue(file.delete())
        assertTrue(container.delete())
        container.writeText("not a directory")

        assertFalse(store.replaceComposition(listOf(second), listOf(first)))
        assertEquals(before, store.state.value)

        container.delete()
    }

    @Test
    fun moveFavoriteSupportsEveryContainerTypePair() {
        val moved = identity(1, "com.example.moved", "Main")
        val sourceTail = identity(1, "com.example.source.tail", "Main")
        val targetFirst = identity(1, "com.example.target.first", "Main")
        val targetLast = identity(1, "com.example.target.last", "Main")

        fun verifyMove(
            sourceType: FavoriteContainerType,
            targetType: FavoriteContainerType,
        ) {
            val source = FavoriteContainer(
                id = "source",
                type = sourceType,
                identities = listOf(moved, sourceTail),
            )
            val target = FavoriteContainer(
                id = "target",
                type = targetType,
                identities = listOf(targetFirst, targetLast),
            )
            val aggregate = FavoriteAggregate(
                verticalLists = listOf(source, target)
                    .filter { it.type == FavoriteContainerType.VerticalList },
                favoriteBars = listOf(source, target)
                    .filter { it.type == FavoriteContainerType.FavoriteBar },
            )

            val updated = aggregate.moveFavorite(
                sourceContainerId = source.id,
                targetContainerId = target.id,
                identity = moved,
                targetIndex = 1,
                exchangeIdentity = null,
            )

            assertEquals(listOf(sourceTail), updated.container(source.id)?.identities)
            assertEquals(
                listOf(targetFirst, moved, targetLast),
                updated.container(target.id)?.identities,
            )
            assertTrue(isValidAggregate(updated))
        }

        verifyMove(FavoriteContainerType.VerticalList, FavoriteContainerType.VerticalList)
        verifyMove(FavoriteContainerType.VerticalList, FavoriteContainerType.FavoriteBar)
        verifyMove(FavoriteContainerType.FavoriteBar, FavoriteContainerType.VerticalList)
        verifyMove(FavoriteContainerType.FavoriteBar, FavoriteContainerType.FavoriteBar)
    }

    @Test
    fun moveFavoriteExchangePreservesBothContainerSlotsAndIdentityUniqueness() {
        val moved = identity(1, "com.example.moved", "Main")
        val sourceTail = identity(1, "com.example.source.tail", "Main")
        val exchanged = identity(1, "com.example.exchanged", "Main")
        val targetTail = identity(1, "com.example.target.tail", "Main")
        val source = FavoriteContainer(
            id = "source-list",
            type = FavoriteContainerType.VerticalList,
            identities = listOf(moved, sourceTail),
        )
        val target = FavoriteContainer(
            id = "target-bar",
            type = FavoriteContainerType.FavoriteBar,
            identities = listOf(exchanged, targetTail),
        )
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(source),
            favoriteBars = listOf(target),
        )

        val updated = aggregate.moveFavorite(
            sourceContainerId = source.id,
            targetContainerId = target.id,
            identity = moved,
            targetIndex = null,
            exchangeIdentity = exchanged,
        )

        assertEquals(listOf(exchanged, sourceTail), updated.verticalLists.single().identities)
        assertEquals(listOf(moved, targetTail), updated.favoriteBars.single().identities)
        assertEquals(aggregate.identities.toSet(), updated.identities.toSet())
        assertTrue(isValidAggregate(updated))
    }

    @Test
    fun moveFavoriteDeletesAnEmptySourceAndIgnoresInvalidMoves() {
        val moved = identity(1, "com.example.moved", "Main")
        val targetEntry = identity(1, "com.example.target", "Main")
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(
                FavoriteContainer(
                    id = "source-list",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(moved),
                ),
            ),
            favoriteBars = listOf(
                FavoriteContainer(
                    id = "target-bar",
                    type = FavoriteContainerType.FavoriteBar,
                    identities = listOf(targetEntry),
                ),
            ),
        )

        val updated = aggregate.moveFavorite(
            sourceContainerId = "source-list",
            targetContainerId = "target-bar",
            identity = moved,
            targetIndex = 0,
            exchangeIdentity = null,
        )

        assertTrue(updated.verticalLists.isEmpty())
        assertEquals(listOf(moved, targetEntry), updated.favoriteBars.single().identities)
        assertEquals(
            aggregate,
            aggregate.moveFavorite(
                sourceContainerId = "missing",
                targetContainerId = "target-bar",
                identity = moved,
                targetIndex = 0,
                exchangeIdentity = null,
            ),
        )
    }

    private fun FavoriteAggregate.container(id: String): FavoriteContainer? =
        (verticalLists + favoriteBars).firstOrNull { it.id == id }

    private fun temporaryFavoriteFile() = ApplicationProvider.getApplicationContext<android.content.Context>()
        .cacheDir
        .resolve("favorites-${UUID.randomUUID()}.bin")

    private fun identity(serial: Long, packageName: String, className: String) =
        LaunchableIdentity(serial, ComponentName(packageName, className))

    private fun writeIdentity(output: DataOutputStream, identity: LaunchableIdentity) {
        output.writeLong(identity.profileSerialNumber)
        output.writeUTF(identity.componentName.flattenToString())
    }

    private fun FavoriteStore.readableIdentities(): List<LaunchableIdentity> =
        (state.value as FavoriteReadState.Readable).identities
}
