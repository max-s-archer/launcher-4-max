package com.maxarcher.launcher

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFavoriteEditorInsertExternalFavoriteTest {
    @Test
    fun creationInsertsNewVerticalModuleWithIdentityAsFirstFavorite() = runBlocking {
        val (editor, store) = newSetup()
        val external = identity(serial = 1)

        val saved = editor.insertExternalFavorite(
            identity = external,
            destinationModuleId = null,
            boundary = 0,
            newModuleType = OrderedFavoriteModuleType.Vertical,
        )

        assertTrue(saved)
        val modules = currentModules(store)
        assertEquals(1, modules.size)
        assertEquals(OrderedFavoriteModuleType.Vertical, modules.single().type)
        assertEquals(listOf(external), modules.single().identities)
    }

    @Test
    fun insertionPlacesExternalIdentityAtTheRequestedBoundary() = runBlocking {
        val (editor, store) = newSetup()
        val first = identity(serial = 1)
        editor.insertExternalFavorite(
            identity = first,
            destinationModuleId = null,
            boundary = 0,
            newModuleType = OrderedFavoriteModuleType.Vertical,
        )
        val moduleId = currentModules(store).single().id
        val external = identity(serial = 2)

        val saved = editor.insertExternalFavorite(
            identity = external,
            destinationModuleId = moduleId,
            boundary = 0,
            newModuleType = null,
        )

        assertTrue(saved)
        assertEquals(
            listOf(external, first),
            currentModules(store).single().identities,
        )
    }

    @Test
    fun duplicateIdentityIsRejectedWithoutChangingState() = runBlocking {
        val (editor, store) = newSetup()
        val external = identity(serial = 1)
        editor.insertExternalFavorite(
            identity = external,
            destinationModuleId = null,
            boundary = 0,
            newModuleType = OrderedFavoriteModuleType.Vertical,
        )
        val before = currentModules(store)

        val saved = editor.insertExternalFavorite(
            identity = external,
            destinationModuleId = null,
            boundary = 0,
            newModuleType = OrderedFavoriteModuleType.Ribbon,
        )

        assertFalse(saved)
        assertEquals(before, currentModules(store))
    }

    @Test
    fun invalidBoundaryIsRejectedWithoutChangingState() = runBlocking {
        val (editor, store) = newSetup()
        editor.insertExternalFavorite(
            identity = identity(serial = 1),
            destinationModuleId = null,
            boundary = 0,
            newModuleType = OrderedFavoriteModuleType.Vertical,
        )
        val moduleId = currentModules(store).single().id
        val before = currentModules(store)

        val saved = editor.insertExternalFavorite(
            identity = identity(serial = 2),
            destinationModuleId = moduleId,
            boundary = 5,
            newModuleType = null,
        )

        assertFalse(saved)
        assertEquals(before, currentModules(store))
    }

    @Test
    fun unknownDestinationModuleIsRejectedWithoutChangingState() = runBlocking {
        val (editor, store) = newSetup()

        val saved = editor.insertExternalFavorite(
            identity = identity(serial = 1),
            destinationModuleId = "missing-module",
            boundary = 0,
            newModuleType = null,
        )

        assertFalse(saved)
        assertEquals(0, currentModules(store).size)
    }

    private fun newSetup(): Pair<HomeFavoriteEditor, FavoriteStore> {
        val store = OrderedFavoriteStoreAdapter(
            file = temporaryFile(),
            legacyFile = null,
        )
        runBlocking { store.load() }
        return HomeFavoriteEditor(store = store) to store
    }

    private fun currentModules(store: FavoriteStore): List<OrderedFavoriteModule> =
        (store.state.value as FavoriteReadState.Readable).orderedModules.orEmpty()

    private fun identity(serial: Long) = LaunchableIdentity(
        profileSerialNumber = serial,
        componentName = ComponentName("com.example", "Main"),
    )

    private fun temporaryFile(): File {
        val file = File(
            ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
            "insert-external-${UUID.randomUUID()}.bin",
        )
        file.deleteOnExit()
        return file
    }
}
