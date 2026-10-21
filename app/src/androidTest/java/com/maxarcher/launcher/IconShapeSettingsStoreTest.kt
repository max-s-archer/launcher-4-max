package com.maxarcher.launcher

import androidx.test.core.app.ApplicationProvider
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconShapeSettingsStoreTest {
    @Test
    fun missingFileResolvesToDefaultSettings(): Unit = runBlocking {
        val file = temporarySettingsFile()
        try {
            val store = IconShapeSettingsStore(file)

            store.load()

            assertEquals(
                IconShapeSettingsReadState.Readable(IconShapeSettings()),
                store.state.value,
            )
        } finally {
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun replacePersistsAndReloadsSettings(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val candidate = IconShapeSettings(
            mode = IconShapeClipMode.Freeform,
            freeformClip = 0,
        )
        try {
            val store = IconShapeSettingsStore(file)
            store.load()

            assertTrue(store.replace(candidate))
            assertEquals(
                IconShapeSettingsReadState.Readable(candidate),
                store.state.value,
            )

            val reloaded = IconShapeSettingsStore(file)
            reloaded.load()

            assertEquals(
                IconShapeSettingsReadState.Readable(candidate),
                reloaded.state.value,
            )
        } finally {
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun invalidStoredValuesFallBackIndependently(): Unit = runBlocking {
        val file = temporarySettingsFile()
        try {
            val cases = listOf(
                mapOf("mode" to "unknown", "freeform_clip" to "73") to
                    IconShapeSettings(freeformClip = 73),
                mapOf("mode" to "freeform") to
                    IconShapeSettings(mode = IconShapeClipMode.Freeform),
            ) + listOf("-1", "101", "2.5", "", "invalid").map { invalid ->
                mapOf("mode" to "freeform", "freeform_clip" to invalid) to
                    IconShapeSettings(mode = IconShapeClipMode.Freeform)
            }
            cases.forEach { (fields, expected) ->
                writeSettingsDocument(file, fields)
                val store = IconShapeSettingsStore(file)
                store.load()
                assertEquals(
                    fields.toString(),
                    IconShapeSettingsReadState.Readable(expected),
                    store.state.value,
                )
            }
        } finally {
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun failedWritePreservesSuccessfulSettingsAndAllowsRetry(): Unit = runBlocking {
        val parent = temporarySettingsFile()
        val file = File(parent, "settings.bin")
        val saved = IconShapeSettings(IconShapeClipMode.Freeform, 73)
        val candidate = saved.copy(mode = IconShapeClipMode.Oem)
        try {
            assertTrue(parent.mkdir())
            val store = IconShapeSettingsStore(file)
            store.load()
            assertTrue(store.replace(saved))
            // A regular file at the parent path deterministically prevents AtomicFile writes.
            deleteSettingsFiles(file)
            assertTrue(parent.delete())
            assertTrue(parent.createNewFile())
            assertFalse(store.replace(candidate))
            assertEquals(saved, store.lastSuccessfulSettings)
            assertEquals(IconShapeSettingsReadState.Readable(saved), store.state.value)
            assertTrue(parent.delete())
            assertTrue(parent.mkdir())
            assertTrue(store.replace(candidate))
            val reloaded = IconShapeSettingsStore(file)
            reloaded.load()
            assertEquals(IconShapeSettingsReadState.Readable(candidate), reloaded.state.value)
        } finally {
            deleteSettingsFiles(file)
            parent.delete()
        }
    }

    @Test
    fun malformedDocumentBlocksReplacementUntilReload(): Unit = runBlocking {
        val file = temporarySettingsFile()
        try {
            file.writeBytes(byteArrayOf(0))
            val store = IconShapeSettingsStore(file)
            assertFalse(store.replace(IconShapeSettings(IconShapeClipMode.Freeform)))
            store.load()
            assertEquals(IconShapeSettingsReadState.ReadFailure, store.state.value)
            assertFalse(store.replace(IconShapeSettings(IconShapeClipMode.Freeform)))
            assertEquals(1L, file.length())
            writeSettingsDocument(file, emptyMap())
            store.load()
            assertTrue(store.replace(IconShapeSettings(IconShapeClipMode.Freeform, 100)))
        } finally {
            deleteSettingsFiles(file)
        }
    }

    private fun temporarySettingsFile(): File = File(
        ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
        "icon-shape-settings-${UUID.randomUUID()}.bin",
    ).also { file -> deleteSettingsFiles(file) }

    private fun writeSettingsDocument(
        file: File,
        fields: Map<String, String>,
    ) {
        DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
            output.writeInt(0x49534831)
            output.writeInt(1)
            output.writeInt(fields.size)
            fields.forEach { (key, value) ->
                output.writeUTF(key)
                output.writeUTF(value)
            }
        }
    }

    private fun deleteSettingsFiles(file: File) {
        file.delete()
        File(file.path + ".new").delete()
        File(file.path + ".bak").delete()
    }
}
