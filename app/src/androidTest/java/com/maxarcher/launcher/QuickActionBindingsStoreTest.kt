package com.maxarcher.launcher

import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionBindingsStoreTest {
    @Test
    fun missingFileResolvesToNoActionDefaults(): Unit = runBlocking {
        val file = temporaryBindingsFile()
        val store = QuickActionBindingsStore(file)
        try {
            store.load()
            assertEquals(
                QuickActionBindingsReadState.Readable(QuickActionBindings()),
                store.state.value,
            )
        } finally {
            deleteBindingsFiles(file)
        }
    }

    @Test
    fun replacePersistsAndReloadsBindings(): Unit = runBlocking {
        val file = temporaryBindingsFile()
        val store = QuickActionBindingsStore(file)
        val candidate = QuickActionBindings(
            doubleTap = QuickAction.ScreenLock,
            longPress = QuickAction.EditMode,
        )
        try {
            store.load()
            assertTrue(store.replace(candidate))
            assertEquals(
                QuickActionBindingsReadState.Readable(candidate),
                store.state.value,
            )
            val reloaded = QuickActionBindingsStore(file)
            reloaded.load()
            assertEquals(
                QuickActionBindingsReadState.Readable(candidate),
                reloaded.state.value,
            )
        } finally {
            deleteBindingsFiles(file)
        }
    }

    private fun temporaryBindingsFile(): File = File(
        ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
        "quick-action-bindings-${UUID.randomUUID()}.bin",
    ).also { file -> deleteBindingsFiles(file) }

    private fun deleteBindingsFiles(file: File) {
        file.delete()
        File(file.path + ".new").delete()
        File(file.path + ".bak").delete()
    }
}
