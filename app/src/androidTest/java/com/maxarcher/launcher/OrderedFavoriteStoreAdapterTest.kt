package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OrderedFavoriteStoreAdapterTest {
    @get:Rule
    val files = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @Test
    fun contextStoreWritesFavoritesInsideTheExcludedFilesDirectory() = runBlocking {
        val directory = files.newFolder()
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(baseContext) {
            override fun getFilesDir(): File = directory
        }
        val store = OrderedFavoriteStoreAdapter(context)
        store.load()
        assertTrue(store.add(identity(1)))

        val file = context.filesDir.resolve("ordered_favorite_modules.bin")
        assertTrue(file.isFile)
        assertEquals(context.filesDir.canonicalFile, file.parentFile?.canonicalFile)
        val restarted = OrderedFavoriteStoreAdapter(context)
        restarted.load()
        assertEquals(listOf(identity(1)), restarted.readableIdentities())
    }

    @Test
    fun addDeduplicatesExactIdentityAndPersistsProfileAndInsertionOrder() = runBlocking {
        val file = files.newFolder().resolve("ordered.bin")
        val store = OrderedFavoriteStoreAdapter(file)
        val first = identity(1)
        val second = LaunchableIdentity(1, ComponentName("com.example.second", "Main"))
        val clone = identity(2)
        store.load()

        assertTrue(store.add(first))
        assertTrue(store.add(second))
        assertTrue(store.add(first))
        assertTrue(store.add(clone))
        assertEquals(listOf(first, second, clone), store.readableIdentities())

        val restarted = OrderedFavoriteStoreAdapter(file)
        restarted.load()
        assertEquals(listOf(first, second, clone), restarted.readableIdentities())
    }

    @Test
    fun damagedDocumentDisablesAddWithoutReplacingBytes() = runBlocking {
        val file = files.newFile()
        val damaged = byteArrayOf(0x00, 0x01, 0x02)
        file.writeBytes(damaged)
        val store = OrderedFavoriteStoreAdapter(file)
        store.load()

        assertEquals(FavoriteReadState.ReadFailure, store.state.value)
        assertFalse(store.add(identity(1)))
        assertArrayEquals(damaged, file.readBytes())
    }

    @Test
    fun failedWritesKeepLastReadableState() = runBlocking {
        val directory = files.newFolder()
        val file = directory.resolve("ordered.bin")
        val store = OrderedFavoriteStoreAdapter(file)
        store.load()
        assertTrue(store.add(identity(1)))
        assertTrue(store.add(identity(2)))
        val before = store.state.value
        val aggregate = store.currentOrderedAggregate()
        assertTrue(file.delete())
        assertTrue(directory.delete())
        directory.writeText("not a directory")

        assertFalse(store.add(identity(3)))
        assertEquals(before, store.state.value)
        assertNull(store.updateOrderedAggregate { current ->
            current.copy(modules = current.modules.map {
                it.copy(identities = it.identities.reversed())
            })
        })
        assertEquals(before, store.state.value)
        assertEquals(aggregate, store.currentOrderedAggregate())
        assertFalse(file.exists())
    }

    @Test
    fun duplicateIdentityReplacementKeepsReadableStateAndSavedBytes() = runBlocking {
        val file = files.newFolder().resolve("ordered.bin")
        val store = OrderedFavoriteStoreAdapter(file)
        store.load()
        val before = store.state.value
        val bytes = file.readBytes()
        val module = OrderedFavoriteModule(
            id = "first",
            type = OrderedFavoriteModuleType.Vertical,
            identities = listOf(identity(1)),
        )

        assertFalse(
            store.restoreAggregate(
                OrderedFavoriteAggregate(listOf(module, module.copy(id = "second"))),
            ),
        )
        assertEquals(before, store.state.value)
        assertArrayEquals(bytes, file.readBytes())
    }

    private fun identity(serial: Long) =
        LaunchableIdentity(serial, ComponentName("com.example", "Main"))

    private fun OrderedFavoriteStoreAdapter.readableIdentities(): List<LaunchableIdentity> =
        (state.value as FavoriteReadState.Readable).identities
}
