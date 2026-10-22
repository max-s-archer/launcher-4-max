package com.maxarcher.launcher

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderedFavoriteModuleStoreTest {
    @Test
    fun cleanLoadCreatesOneReadableEmptyOrderedModel(): Unit = runBlocking {
        val file = temporaryFile()
        val store = OrderedFavoriteModuleStore(file)

        store.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(OrderedFavoriteAggregate()),
            store.state.value,
        )
        assertTrue(file.isFile)
        file.delete()
    }

    @Test
    fun unreadableOrderedDataRemainsFailureAndIsNotReplaced(): Unit = runBlocking {
        val file = temporaryFile()
        val unreadableBytes = byteArrayOf(0x01, 0x02, 0x03)
        file.writeBytes(unreadableBytes)
        val store = OrderedFavoriteModuleStore(file)

        store.load()

        assertEquals(OrderedFavoriteReadState.ReadFailure, store.state.value)
        assertEquals(unreadableBytes.toList(), file.readBytes().toList())
        file.delete()
    }

    @Test
    fun readableLegacyFavoritesAreAdoptedAsEmptyWithoutChangingLegacyData(): Unit = runBlocking {
        val file = temporaryFile()
        val legacyFile = temporaryFile()
        val identity = LaunchableIdentity(
            1,
            ComponentName("com.example", "Main"),
        )
        DataOutputStream(legacyFile.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(3)
            output.writeInt(1)
            output.writeUTF("vertical-list-1")
            output.writeInt(FavoriteContainerType.VerticalList.ordinal)
            output.writeInt(FavoriteListSize.Medium.ordinal)
            output.writeInt(1)
            output.writeLong(identity.profileSerialNumber)
            output.writeUTF(identity.componentName.flattenToString())
        }
        val legacyBytes = legacyFile.readBytes()

        val store = OrderedFavoriteModuleStore(file, legacyFile)
        store.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(OrderedFavoriteAggregate()),
            store.state.value,
        )
        assertEquals(legacyBytes.toList(), legacyFile.readBytes().toList())
        file.delete()
        legacyFile.delete()
    }

    @Test
    fun unreadableLegacyFavoritesRemainFailureAndAreNotReplaced(): Unit = runBlocking {
        val file = temporaryFile()
        val legacyFile = temporaryFile()
        DataOutputStream(legacyFile.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(99)
        }
        val legacyBytes = legacyFile.readBytes()

        val store = OrderedFavoriteModuleStore(file, legacyFile)
        store.load()

        assertEquals(OrderedFavoriteReadState.ReadFailure, store.state.value)
        assertEquals(legacyBytes.toList(), legacyFile.readBytes().toList())
        assertTrue(!file.exists())
        file.delete()
        legacyFile.delete()
    }

    @Test
    fun recoverableLegacyBackupIsReadBeforeAdoption(): Unit = runBlocking {
        val file = temporaryFile()
        val legacyFile = temporaryFile()
        val legacyBackup = File(legacyFile.path + ".bak")
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        DataOutputStream(legacyBackup.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(1)
            output.writeInt(1)
            output.writeLong(identity.profileSerialNumber)
            output.writeUTF(identity.componentName.flattenToString())
        }
        val legacyBytes = legacyBackup.readBytes()

        val store = OrderedFavoriteModuleStore(file, legacyFile)
        store.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(OrderedFavoriteAggregate()),
            store.state.value,
        )
        assertEquals(legacyBytes.toList(), legacyFile.readBytes().toList())
        assertFalse(legacyBackup.exists())
        file.delete()
        legacyFile.delete()
    }

    @Test
    fun invalidLegacyAggregateDoesNotCreateOrderedState(): Unit = runBlocking {
        val file = temporaryFile()
        val legacyFile = temporaryFile()
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        DataOutputStream(legacyFile.outputStream()).use { output ->
            output.writeInt(0x4156454E)
            output.writeInt(3)
            output.writeInt(2)
            repeat(2) { index ->
                output.writeUTF("vertical-list-${index + 1}")
                output.writeInt(FavoriteContainerType.VerticalList.ordinal)
                output.writeInt(FavoriteListSize.Medium.ordinal)
                output.writeInt(1)
                output.writeLong(identity.profileSerialNumber)
                output.writeUTF(identity.componentName.flattenToString())
            }
        }

        val store = OrderedFavoriteModuleStore(file, legacyFile)
        store.load()

        assertEquals(OrderedFavoriteReadState.ReadFailure, store.state.value)
        assertFalse(file.exists())
        legacyFile.delete()
    }

    @Test
    fun orderedModulesRoundTripInModuleAndIdentityOrder(): Unit = runBlocking {
        val file = temporaryFile()
        val first = LaunchableIdentity(1, ComponentName("com.example.first", "Main"))
        val second = LaunchableIdentity(2, ComponentName("com.example.second", "Main"))
        val bottom = LaunchableIdentity(3, ComponentName("com.example.bottom", "Main"))
        val aggregate = OrderedFavoriteAggregate(
            modules = listOf(
                OrderedFavoriteModule(
                    id = "vertical-1",
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(first),
                    iconSize = FavoriteListSize.Large,
                    textSize = FavoriteListSize.Small,
                    namePlacement = FavoriteNamePlacement.Below,
                    itemsPerRow = 4,
                ),
                OrderedFavoriteModule(
                    id = "ribbon-1",
                    type = OrderedFavoriteModuleType.Ribbon,
                    identities = listOf(second),
                ),
            ),
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                presentation = HomeBottomAppBarPresentation.Fixed,
                iconSize = FavoriteListSize.Small,
                identities = listOf(bottom),
            ),
        )
        val store = OrderedFavoriteModuleStore(file)
        store.load()

        assertTrue(store.replaceAggregate(aggregate))
        val reloadedStore = OrderedFavoriteModuleStore(file)
        reloadedStore.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(aggregate),
            reloadedStore.state.value,
        )
        assertEquals(listOf(first, second, bottom), aggregate.identities)
        file.delete()
    }

    @Test
    fun versionOneOrderedModuleLoadsWithCurrentVerticalDefaults(): Unit = runBlocking {
        val file = temporaryFile()
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x41464D31)
            output.writeInt(1)
            output.writeInt(1)
            output.writeUTF("vertical-1")
            output.writeInt(OrderedFavoriteModuleType.Vertical.ordinal)
            output.writeInt(1)
            output.writeLong(identity.profileSerialNumber)
            output.writeUTF(identity.componentName.flattenToString())
        }

        val store = OrderedFavoriteModuleStore(file)
        store.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(
                OrderedFavoriteAggregate(
                    listOf(
                        OrderedFavoriteModule(
                            id = "vertical-1",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
            store.state.value,
        )
        file.delete()
    }

    @Test
    fun recoverableOrderedBackupIsReadInsteadOfTriggeringLegacyAdoption(): Unit = runBlocking {
        val file = temporaryFile()
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        val aggregate = OrderedFavoriteAggregate(
            modules = listOf(
                OrderedFavoriteModule(
                    id = "vertical-1",
                    type = OrderedFavoriteModuleType.Vertical,
                    identities = listOf(identity),
                ),
            ),
        )
        val initialStore = OrderedFavoriteModuleStore(file)
        initialStore.load()
        assertTrue(initialStore.replaceAggregate(aggregate))
        val backup = File(file.path + ".bak")
        assertTrue(file.renameTo(backup))

        val reloadedStore = OrderedFavoriteModuleStore(file)
        reloadedStore.load()

        assertEquals(
            OrderedFavoriteReadState.Readable(aggregate),
            reloadedStore.state.value,
        )
        assertTrue(file.isFile)
        assertFalse(backup.exists())
        file.delete()
    }

    @Test
    fun invalidOrderedAggregateRejectsDuplicateIdentities() {
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        assertTrue(
            !isValidOrderedFavoriteAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "one",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(identity),
                        ),
                        OrderedFavoriteModule(
                            id = "two",
                            type = OrderedFavoriteModuleType.Ribbon,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun invalidOrderedAggregateRejectsIdentitySharedWithBottomAppBar() {
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        assertFalse(
            isValidOrderedFavoriteAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "one",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(identity),
                        ),
                    ),
                    homeBottomAppBar = HomeBottomAppBarState(
                        identities = listOf(identity),
                    ),
                ),
            ),
        )
    }

    @Test
    fun aggregateUpdateCanDeleteTheLastModule(): Unit = runBlocking {
        val file = temporaryFile()
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()
        assertTrue(
            adapter.restoreAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "vertical-list-1",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )

        val updated = adapter.updateOrderedAggregate { removeOrderedFavorite(it, identity) }

        assertEquals(OrderedFavoriteAggregate(), updated)
        val readable = adapter.state.value as FavoriteReadState.Readable
        assertTrue(readable.aggregate.identities.isEmpty())
        assertTrue(readable.orderedModules?.isEmpty() == true)
        file.delete()
    }

    @Test
    fun genericAddPreservesAnExistingModule(): Unit = runBlocking {
        val file = temporaryFile()
        val ribbonIdentity = LaunchableIdentity(1, ComponentName("com.ribbon", "Main"))
        val addedIdentity = LaunchableIdentity(1, ComponentName("com.vertical", "Main"))
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()
        assertTrue(
            adapter.restoreAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "favorite-bar-1",
                            type = OrderedFavoriteModuleType.Ribbon,
                            identities = listOf(ribbonIdentity),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(adapter.add(addedIdentity))

        val readable = adapter.state.value as FavoriteReadState.Readable
        val modules = checkNotNull(readable.orderedModules)
        assertEquals(
            listOf(ribbonIdentity, addedIdentity),
            modules.flatMap { module -> module.identities },
        )
        assertEquals(2, modules.size)
        assertEquals(OrderedFavoriteModuleType.Ribbon, modules.first().type)
        assertEquals(OrderedFavoriteModuleType.Vertical, modules.last().type)
        file.delete()
    }

    @Test
    fun styleUpdatePreservesUnchangedModuleInstances(): Unit = runBlocking {
        val file = temporaryFile()
        val firstIdentity = LaunchableIdentity(1, ComponentName("com.first", "Main"))
        val secondIdentity = LaunchableIdentity(2, ComponentName("com.second", "Main"))
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()
        assertTrue(
            adapter.restoreAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "vertical-1",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(firstIdentity),
                        ),
                        OrderedFavoriteModule(
                            id = "vertical-2",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(secondIdentity),
                        ),
                    ),
                ),
            ),
        )
        val unchangedBefore = (adapter.state.value as FavoriteReadState.Readable)
            .orderedModules
            ?.single { it.id == "vertical-2" }

        val updated = adapter.updateOrderedAggregate { aggregate ->
            aggregate.copy(
                modules = aggregate.modules.map {
                    if (it.id == "vertical-1") it.copy(iconSize = FavoriteListSize.Large) else it
                },
            )
        }
        assertEquals(FavoriteListSize.Large, updated?.modules?.first()?.iconSize)

        val unchangedAfter = (adapter.state.value as FavoriteReadState.Readable)
            .orderedModules
            ?.single { it.id == "vertical-2" }
        assertSame(unchangedBefore, unchangedAfter)
        file.delete()
    }

    @Test
    fun moduleOrderReplacementPersistsTheExactRequestedOrder(): Unit = runBlocking {
        val file = temporaryFile()
        val first = LaunchableIdentity(1, ComponentName("com.first", "Main"))
        val second = LaunchableIdentity(2, ComponentName("com.second", "Main"))
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()
        assertTrue(
            adapter.restoreAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "vertical-1",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(first),
                        ),
                        OrderedFavoriteModule(
                            id = "ribbon-1",
                            type = OrderedFavoriteModuleType.Ribbon,
                            identities = listOf(second),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(adapter.replaceModuleOrder(listOf("ribbon-1", "vertical-1")))

        val reloaded = OrderedFavoriteStoreAdapter(file)
        reloaded.load()
        val modules = (reloaded.state.value as FavoriteReadState.Readable).orderedModules
        assertEquals(listOf("ribbon-1", "vertical-1"), modules?.map { it.id })
        file.delete()
    }

    @Test
    fun moduleOrderReplacementRejectsMissingOrUnknownModuleIds(): Unit = runBlocking {
        val file = temporaryFile()
        val identity = LaunchableIdentity(1, ComponentName("com.example", "Main"))
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()
        assertTrue(
            adapter.restoreAggregate(
                OrderedFavoriteAggregate(
                    modules = listOf(
                        OrderedFavoriteModule(
                            id = "vertical-1",
                            type = OrderedFavoriteModuleType.Vertical,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )

        assertFalse(adapter.replaceModuleOrder(emptyList()))
        assertFalse(adapter.replaceModuleOrder(listOf("unknown")))
        assertEquals(
            listOf("vertical-1"),
            (adapter.state.value as FavoriteReadState.Readable)
                .orderedModules
                ?.map { it.id },
        )
        file.delete()
    }

    @Test
    fun concurrentAddsAreSerializedWithoutLosingIdentities(): Unit = runBlocking {
        val file = temporaryFile()
        val identities = (1L..12L).map { serial ->
            LaunchableIdentity(serial, ComponentName("com.example.$serial", "Main"))
        }
        val adapter = OrderedFavoriteStoreAdapter(file)
        adapter.load()

        val results = identities.map { identity ->
            async { adapter.add(identity) }
        }.awaitAll()

        assertTrue(results.all { it })
        val readable = adapter.state.value as FavoriteReadState.Readable
        assertEquals(identities.toSet(), readable.identities.toSet())
        assertEquals(identities.size, readable.identities.size)
        file.delete()
    }

    private fun temporaryFile(): File =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .cacheDir
            .resolve("ordered-favorites-${UUID.randomUUID()}.bin")
}
