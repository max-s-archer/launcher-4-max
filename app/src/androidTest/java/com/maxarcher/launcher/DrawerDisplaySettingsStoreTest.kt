package com.maxarcher.launcher

import androidx.test.core.app.ApplicationProvider
import android.util.AtomicFile
import android.content.ContextWrapper
import java.io.FileOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.maxarcher.launcher.ui.drawer.DrawerApplicationSize
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettings
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsReadState
import com.maxarcher.launcher.ui.drawer.DrawerDisplaySettingsStore
import com.maxarcher.launcher.ui.drawer.DrawerNamePlacement
import com.maxarcher.launcher.ui.drawer.DrawerSectionAnchorPresentation

class DrawerDisplaySettingsStoreTest {
    @Test
    fun recreatedStoreWaitsForThePreviousOwnersPendingWrite(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val readerStarted = CountDownLatch(1)
        val candidate = DrawerDisplaySettings(backgroundOpacity = 0)
        val oldStore = DrawerDisplaySettingsStore(object : AtomicFile(file) {
            override fun startWrite(): FileOutputStream {
                val stream = super.startWrite()
                entered.countDown()
                if (!release.await(5, TimeUnit.SECONDS)) {
                    super.failWrite(stream)
                    throw java.io.IOException("Write gate timed out")
                }
                return stream
            }
        })
        oldStore.load()
        val saving = async(Dispatchers.IO) { oldStore.replace(candidate) }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val recreatedStore = DrawerDisplaySettingsStore(file)
            val loading = async(Dispatchers.IO) {
                readerStarted.countDown()
                recreatedStore.load()
            }
            assertTrue(readerStarted.await(5, TimeUnit.SECONDS))
            // Reading AtomicFile during startWrite can discard its unfinished .new file.
            // Releasing the writer and awaiting both must always yield the committed state.
            release.countDown()
            assertTrue(saving.await())
            loading.await()
            assertEquals(DrawerDisplaySettingsReadState.Readable(candidate), recreatedStore.state.value)
        } finally {
            release.countDown()
            saving.join()
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun cancellationAtCommitKeepsMemoryAndReloadedStateConsistent(): Unit = runBlocking {
        val file = temporarySettingsFile()
        lateinit var saving: Job
        val atomicFile = object : AtomicFile(file) {
            override fun finishWrite(stream: FileOutputStream?) {
                super.finishWrite(stream)
                saving.cancel()
            }
        }
        val store = DrawerDisplaySettingsStore(atomicFile)
        try {
            store.load()
            val candidate = DrawerDisplaySettings(
                iconSize = DrawerApplicationSize.Small,
                textSize = DrawerApplicationSize.Large,
                namePlacement = DrawerNamePlacement.Below,
                itemsPerRow = 4,
                sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
                backgroundOpacity = 0,
            )
            saving = launch(start = CoroutineStart.LAZY) { store.replace(candidate) }
            saving.start()
            saving.join()
            assertTrue(saving.isCancelled)
            val expected = DrawerDisplaySettingsReadState.Readable(candidate)
            assertEquals(expected, store.state.value)
            val reloaded = DrawerDisplaySettingsStore(file)
            reloaded.load()
            assertEquals(expected, reloaded.state.value)
        } finally {
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun failedCompleteWriteCanBeRetriedWithoutLosingOtherGroups(): Unit = runBlocking {
        val file = temporarySettingsFile()
        var fail = false
        val store = DrawerDisplaySettingsStore(object : AtomicFile(file) {
            override fun startWrite(): FileOutputStream {
                if (fail) throw java.io.IOException("Injected write failure")
                return super.startWrite()
            }
        })
        try {
            store.load()
            val original = DrawerDisplaySettings(
                iconSize = DrawerApplicationSize.Large,
                textSize = DrawerApplicationSize.Small,
                namePlacement = DrawerNamePlacement.Below,
                itemsPerRow = 3,
                sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
            )
            assertTrue(store.replace(original))
            val candidate = original.copy(backgroundOpacity = 0)
            fail = true
            assertFalse(store.replace(candidate))
            assertEquals(DrawerDisplaySettingsReadState.Readable(original), store.state.value)
            val afterFailure = DrawerDisplaySettingsStore(file)
            afterFailure.load()
            assertEquals(store.state.value, afterFailure.state.value)
            fail = false
            assertTrue(store.replace(candidate))
            val afterRetry = DrawerDisplaySettingsStore(file)
            afterRetry.load()
            assertEquals(DrawerDisplaySettingsReadState.Readable(candidate), afterRetry.state.value)
        } finally {
            deleteSettingsFiles(file)
        }
    }

    @Test
    fun freshStoreLoadsConfirmedDefaults(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        assertEquals(
            DrawerDisplaySettingsReadState.Readable(
                settings = DrawerDisplaySettings(),
            ),
            store.state.value,
        )
        assertFalse(file.exists())
        deleteSettingsFiles(file = file)
    }

    @Test
    fun completeSettingsRoundTrip(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val settings = DrawerDisplaySettings(
            iconSize = DrawerApplicationSize.Large,
            textSize = DrawerApplicationSize.Small,
            namePlacement = DrawerNamePlacement.Below,
            itemsPerRow = 4,
            sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
            backgroundOpacity = 0,
        )
        val store = DrawerDisplaySettingsStore(file = file)
        store.load()

        assertTrue(store.replace(settings = settings))
        val reloadedStore = DrawerDisplaySettingsStore(file = file)
        reloadedStore.load()

        assertEquals(
            DrawerDisplaySettingsReadState.Readable(settings = settings),
            reloadedStore.state.value,
        )
        deleteSettingsFiles(file = file)
    }

    @Test
    fun missingFieldsAdoptCurrentDefaultsWithoutDiscardingReadableFields(): Unit = runBlocking {
        val file = temporarySettingsFile()
        writeRawDocument(
            file = file,
            fields = listOf(
                "application_size" to "large",
                "name_placement" to "below",
            ),
        )
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        val expected = DrawerDisplaySettings(
            iconSize = DrawerApplicationSize.Large,
            textSize = DrawerApplicationSize.Large,
            namePlacement = DrawerNamePlacement.Below,
            itemsPerRow = 1,
            sectionAnchorPresentation = DrawerSectionAnchorPresentation.Inline,
            backgroundOpacity = DrawerDisplaySettings.DEFAULT_BACKGROUND_OPACITY,
        )
        assertEquals(
            DrawerDisplaySettingsReadState.Readable(settings = expected),
            store.state.value,
        )

        val reloadedStore = DrawerDisplaySettingsStore(file = file)
        reloadedStore.load()
        assertEquals(
            DrawerDisplaySettingsReadState.Readable(settings = expected),
            reloadedStore.state.value,
        )
        deleteSettingsFiles(file = file)
    }

    @Test
    fun unknownFieldsDoNotChangeKnownSettings(): Unit = runBlocking {
        val file = temporarySettingsFile()
        writeRawDocument(
            file = file,
            fields = completeRawFields() + ("future_field" to "future_value"),
        )
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        assertEquals(
            DrawerDisplaySettingsReadState.Readable(
                settings = DrawerDisplaySettings(),
            ),
            store.state.value,
        )
        deleteSettingsFiles(file = file)
    }

    @Test
    fun unreadableDocumentPublishesFailureWithoutReplacingSource(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val unreadableBytes = byteArrayOf(0x01, 0x02, 0x03)
        file.writeBytes(unreadableBytes)
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        assertEquals(DrawerDisplaySettingsReadState.ReadFailure, store.state.value)
        assertEquals(unreadableBytes.toList(), file.readBytes().toList())
        assertFalse(store.replace(settings = DrawerDisplaySettings()))
        deleteSettingsFiles(file = file)
    }

    @Test
    fun failedWriteKeepsLastSuccessfullyReadState(): Unit = runBlocking {
        val blockedParent = temporarySettingsFile().apply {
            writeText("not a directory")
        }
        val file = File(blockedParent, "drawer-display-settings.bin")
        val store = DrawerDisplaySettingsStore(file = file)
        store.load()

        val candidate = DrawerDisplaySettings(
            iconSize = DrawerApplicationSize.Small,
        )
        assertFalse(store.replace(settings = candidate))
        assertEquals(
            DrawerDisplaySettingsReadState.Readable(
                settings = DrawerDisplaySettings(),
            ),
            store.state.value,
        )
        blockedParent.delete()
    }

    @Test
    fun concurrentCompleteWritesRemainSerializedAndReloadable(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val store = DrawerDisplaySettingsStore(file = file)
        store.load()
        val candidates = listOf(
            DrawerDisplaySettings(iconSize = DrawerApplicationSize.Large),
            DrawerDisplaySettings(
                namePlacement = DrawerNamePlacement.Below,
                itemsPerRow = 3,
            ),
            DrawerDisplaySettings(
                sectionAnchorPresentation = DrawerSectionAnchorPresentation.LeftSide,
            ),
        )

        val results = candidates.map { settings ->
            async { store.replace(settings = settings) }
        }.awaitAll()

        assertTrue(results.all { result -> result })
        val finalState = store.state.value as DrawerDisplaySettingsReadState.Readable
        val reloadedStore = DrawerDisplaySettingsStore(file = file)
        reloadedStore.load()
        assertEquals(finalState, reloadedStore.state.value)
        deleteSettingsFiles(file = file)
    }

    @Test
    fun hiddenPlacementWithSixItemsRoundTrips(): Unit = runBlocking {
        val file = temporarySettingsFile()
        val settings = DrawerDisplaySettings(
            namePlacement = DrawerNamePlacement.Hidden,
            itemsPerRow = 6,
        )
        val store = DrawerDisplaySettingsStore(file = file)
        store.load()

        assertTrue(store.replace(settings))
        val reloadedStore = DrawerDisplaySettingsStore(file = file)
        reloadedStore.load()

        assertEquals(DrawerDisplaySettingsReadState.Readable(settings), reloadedStore.state.value)
        deleteSettingsFiles(file = file)
    }

    @Test
    fun contextStoreUsesBackupExcludedFilesDirectory(): Unit = runBlocking {
        val application = ApplicationProvider.getApplicationContext<android.content.Context>()
        val isolatedDirectory = File(application.cacheDir, "drawer-context-${UUID.randomUUID()}")
        assertTrue(isolatedDirectory.mkdirs())
        val context = object : ContextWrapper(application) {
            override fun getFilesDir(): File = isolatedDirectory
        }
        val file = context.filesDir.resolve("drawer-display-settings.bin")
        deleteSettingsFiles(file = file)
        val store = DrawerDisplaySettingsStore(context = context)

        try {
            store.load()
            assertTrue(
                store.replace(
                    settings = DrawerDisplaySettings(
                        backgroundOpacity = 0,
                    ),
                ),
            )
            assertTrue(file.isFile)
            assertEquals(context.filesDir.canonicalFile, file.parentFile?.canonicalFile)
        } finally {
            deleteSettingsFiles(file = file)
            isolatedDirectory.delete()
        }
    }

    private fun temporarySettingsFile(): File = File(
        ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
        "drawer-display-settings-${UUID.randomUUID()}.bin",
    ).also { file -> deleteSettingsFiles(file = file) }

    private fun deleteSettingsFiles(file: File) {
        file.delete()
        File(file.path + ".new").delete()
        File(file.path + ".bak").delete()
    }

    private fun writeRawDocument(
        file: File,
        fields: List<Pair<String, String>>,
    ) {
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x44525331)
            output.writeInt(1)
            output.writeInt(fields.size)
            fields.forEach { (key, value) ->
                output.writeUTF(key)
                output.writeUTF(value)
            }
        }
    }

    private fun completeRawFields(): List<Pair<String, String>> = listOf(
        "icon_size" to "medium",
        "text_size" to "medium",
        "name_placement" to "right",
        "items_per_row" to "1",
        "section_anchor" to "inline",
        "background_opacity" to "50",
    )

    @Test
    fun legacyApplicationSizeBackfillsIndependentIconAndTextSizes(): Unit = runBlocking {
        val file = temporarySettingsFile()
        writeRawDocument(file = file, fields = listOf("application_size" to "small"))
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        val expected = DrawerDisplaySettings(
            iconSize = DrawerApplicationSize.Small,
            textSize = DrawerApplicationSize.Small,
        )
        assertEquals(DrawerDisplaySettingsReadState.Readable(expected), store.state.value)
        val reloaded = DrawerDisplaySettingsStore(file = file)
        reloaded.load()
        assertEquals(DrawerDisplaySettingsReadState.Readable(expected), reloaded.state.value)
        deleteSettingsFiles(file = file)
    }

    @Test
    fun legacyBackgroundModeIsNotMappedAndMissingOpacityAdoptsTheDefault(): Unit = runBlocking {
        val file = temporarySettingsFile()
        writeRawDocument(
            file = file,
            fields = listOf(
                "application_size" to "large",
                "background" to "frosted_glass",
            ),
        )
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        assertEquals(
            DrawerDisplaySettingsReadState.Readable(
                settings = DrawerDisplaySettings(
                    iconSize = DrawerApplicationSize.Large,
                    textSize = DrawerApplicationSize.Large,
                    backgroundOpacity = DrawerDisplaySettings.DEFAULT_BACKGROUND_OPACITY,
                ),
            ),
            store.state.value,
        )

        // The readable adopted state is rewritten in the current form without the
        // legacy mode field, so a reload observes the identical state.
        val reloadedStore = DrawerDisplaySettingsStore(file = file)
        reloadedStore.load()
        assertEquals(store.state.value, reloadedStore.state.value)
        deleteSettingsFiles(file = file)
    }

    @Test
    fun outOfRangeOpacityPublishesFailure(): Unit = runBlocking {
        val file = temporarySettingsFile()
        writeRawDocument(file = file, fields = listOf("background_opacity" to "150"))
        val store = DrawerDisplaySettingsStore(file = file)

        store.load()

        assertEquals(DrawerDisplaySettingsReadState.ReadFailure, store.state.value)
        deleteSettingsFiles(file = file)
    }
}
