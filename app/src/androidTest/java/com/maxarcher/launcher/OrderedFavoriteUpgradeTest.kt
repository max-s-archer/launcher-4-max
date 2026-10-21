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

/** Isolated schema-3 fixtures, not evidence of APK installation or Android settings retention. */
class OrderedFavoriteUpgradeTest {
    @get:Rule
    val temporary = TemporaryFolder()

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

    private fun writeLegacy(file: File) {
        // The accepted 1.3.0 format: magic, schema 3, container count, then each
        // container's id/type/size and exact profile/component identities.
        DataOutputStream(file.outputStream()).use(block = { output ->
            output.writeInt(0x4156454E)
            output.writeInt(3)
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
