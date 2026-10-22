package com.maxarcher.launcher

import android.content.ComponentName
import java.io.DataOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Isolated legacy fixtures, not evidence of APK installation or Android settings retention. */
class OrderedFavoriteUpgradeTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun legacySchemasOneAndTwoInitializePersistedEmptyOrderedModels() = runBlocking {
        for (schema in 1..2) {
            val legacy = temporary.newFile("favorites-$schema.bin")
            writeLegacy(legacy, schema)
            val legacyBytes = legacy.readBytes()
            val ordered = File(temporary.root, "ordered-$schema.bin")
            val store = OrderedFavoriteModuleStore(ordered, legacy)

            store.load()

            val expected = OrderedFavoriteReadState.Readable(OrderedFavoriteAggregate())
            assertEquals("schema $schema", expected, store.state.value)
            assertTrue(ordered.isFile)
            val restarted = OrderedFavoriteModuleStore(ordered)
            restarted.load()
            assertEquals("schema $schema", expected, restarted.state.value)
            assertArrayEquals(legacyBytes, legacy.readBytes())
        }
    }

    @Test
    fun truncatedLegacySchemasDoNotCreateOrderedData() = runBlocking {
        for (schema in 1..3) {
            val legacy = temporary.newFile("truncated-$schema.bin")
            writeLegacy(legacy, schema)
            val bytes = legacy.readBytes()
            legacy.writeBytes(bytes.copyOf(bytes.size - 1))

            assertLegacyRejectedWithoutWriting(legacy)
        }
    }

    @Test
    fun legacySchemasWithTrailingBytesDoNotCreateOrderedData() = runBlocking {
        for (schema in 1..3) {
            val legacy = temporary.newFile("trailing-$schema.bin")
            writeLegacy(legacy, schema)
            legacy.appendBytes(byteArrayOf(0x00))

            assertLegacyRejectedWithoutWriting(legacy)
        }
    }

    @Test
    fun legacyResetOccursOnceAndLaterLoadsKeepNewModules() = runBlocking(block = {
        val legacy = temporary.newFile("favorites.bin")
        writeLegacy(file = legacy)
        val legacyBytes = legacy.readBytes()
        val ordered = File(temporary.root, "ordered_favorite_modules.bin")
        val store = OrderedFavoriteModuleStore(file = ordered, legacyFile = legacy)

        store.load()
        assertEquals(OrderedFavoriteReadState.Readable(aggregate = OrderedFavoriteAggregate()), store.state.value)
        assertTrue(ordered.isFile)
        assertArrayEquals(legacyBytes, legacy.readBytes())

        val saved = OrderedFavoriteAggregate(modules = listOf(element = OrderedFavoriteModule(
            id = "new-list", type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(element = identity(profile = 1)),
            iconSize = FavoriteListSize.Small,
            textSize = FavoriteListSize.Small,
            namePlacement = FavoriteNamePlacement.Below, itemsPerRow = 3,
        )))
        assertTrue(store.replaceAggregate(aggregate = saved))
        val savedBytes = ordered.readBytes()
        val restarted = OrderedFavoriteModuleStore(file = ordered, legacyFile = legacy)
        restarted.load()
        assertEquals(OrderedFavoriteReadState.Readable(aggregate = saved), restarted.state.value)
        assertArrayEquals(savedBytes, ordered.readBytes())
        assertArrayEquals(legacyBytes, legacy.readBytes())

        // Once adopted, stale or damaged legacy data cannot trigger a second reset.
        legacy.writeBytes(array = byteArrayOf(0x00))
        val later = OrderedFavoriteModuleStore(file = ordered, legacyFile = legacy)
        later.load()
        assertEquals(OrderedFavoriteReadState.Readable(aggregate = saved), later.state.value)
        assertArrayEquals(savedBytes, ordered.readBytes())
    })

    @Test
    fun unreadableNewDataNeverFallsBackToReadableLegacyOrAllowsAWrite() = runBlocking(block = {
        val legacy = temporary.newFile("favorites.bin")
        writeLegacy(file = legacy)
        val legacyBytes = legacy.readBytes()
        val ordered = temporary.newFile("ordered_favorite_modules.bin")
        val damaged = byteArrayOf(0x01, 0x02, 0x03)
        ordered.writeBytes(array = damaged)
        val store = OrderedFavoriteModuleStore(file = ordered, legacyFile = legacy)

        repeat(times = 2, action = {
            store.load()
            assertEquals(OrderedFavoriteReadState.ReadFailure, store.state.value)
            assertFalse(store.replaceAggregate(aggregate = OrderedFavoriteAggregate()))
            assertArrayEquals(damaged, ordered.readBytes())
            assertArrayEquals(legacyBytes, legacy.readBytes())
        })
    })

    private suspend fun assertLegacyRejectedWithoutWriting(legacy: File) {
        val legacyBytes = legacy.readBytes()
        val ordered = File(temporary.root, "ordered-${legacy.name}")
        val store = OrderedFavoriteModuleStore(ordered, legacy)

        store.load()

        assertEquals(legacy.name, OrderedFavoriteReadState.ReadFailure, store.state.value)
        assertFalse(store.replaceAggregate(OrderedFavoriteAggregate()))
        assertFalse(ordered.exists())
        assertArrayEquals(legacyBytes, legacy.readBytes())
    }

    private fun writeLegacy(file: File, schema: Int = 3) {
        // Schema 1 stores one identity list; schema 2 stores primary and companion lists.
        // Schema 3 stores containers with id/type/size and exact profile/component identities.
        DataOutputStream(file.outputStream()).use(block = { output ->
            output.writeInt(0x4156454E)
            output.writeInt(schema)
            if (schema < 3) {
                repeat(schema) { index ->
                    val identity = identity(profile = index.toLong() + 1)
                    output.writeInt(1)
                    output.writeLong(identity.profileSerialNumber)
                    output.writeUTF(identity.componentName.flattenToString())
                }
                return@use
            }
            output.writeInt(2)
            listOf(FavoriteContainerType.VerticalList, FavoriteContainerType.FavoriteBar)
                .forEachIndexed(action = { index, type ->
                    val identity = identity(profile = index.toLong() + 1)
                    output.writeUTF("legacy-$index")
                    output.writeInt(type.ordinal)
                    output.writeInt(FavoriteListSize.Medium.ordinal)
                    output.writeInt(1)
                    output.writeLong(identity.profileSerialNumber)
                    output.writeUTF(identity.componentName.flattenToString())
                })
        })
    }

    private fun identity(profile: Long): LaunchableIdentity = LaunchableIdentity(
        profileSerialNumber = profile,
        componentName = ComponentName("com.example.upgrade", "Main"),
    )
}
