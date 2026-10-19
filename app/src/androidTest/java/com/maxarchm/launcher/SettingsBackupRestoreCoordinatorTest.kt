package com.maxarchm.launcher

import android.content.ComponentName
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.maxarchm.launcher.ui.drawer.DrawerDisplaySettings

class SettingsBackupRestoreCoordinatorTest {
    @Test
    fun successfulRestoreReplacesFavoritesAndDisplaySettings() = runBlocking {
        val favorites = FakeFavoritesAccess(initial = aggregateWithIdentity(serial = 1))
        val settings = FakeSettingsAccess()
        val bindings = FakeBindingsAccess()
        val iconShape = FakeIconShapeAccess()
        val coordinator = SettingsBackupRestoreCoordinator(
            favorites = favorites,
            settings = settings,
            bindings = bindings,
            iconShape = iconShape,
        )
        val backup = SettingsBackupState(
            aggregate = aggregateWithIdentity(serial = 2),
            settings = DrawerDisplaySettings(
                backgroundOpacity = 100,
            ),
            bindings = QuickActionBindings(doubleTap = QuickAction.ScreenLock),
            iconShape = IconShapeSettings(IconShapeClipMode.Freeform, 73),
        )

        val succeeded = coordinator.restore(backup)

        assertTrue(succeeded)
        assertEquals(backup.aggregate, favorites.aggregate)
        assertEquals(backup.settings, settings.settings)
        assertEquals(backup.bindings, bindings.bindings)
        assertEquals(backup.iconShape, iconShape.settings)
    }

    @Test
    fun failedSettingsRestoreRollsFavoritesBackToPreviousState() = runBlocking {
        val previous = aggregateWithIdentity(serial = 1)
        val favorites = FakeFavoritesAccess(initial = previous)
        val settings = FakeSettingsAccess()
        settings.restoreFails = true
        val coordinator = SettingsBackupRestoreCoordinator(
            favorites = favorites,
            settings = settings,
            bindings = FakeBindingsAccess(),
            iconShape = FakeIconShapeAccess(),
        )
        val backup = SettingsBackupState(
            aggregate = aggregateWithIdentity(serial = 2),
            settings = DrawerDisplaySettings(
                backgroundOpacity = 100,
            ),
        )

        val succeeded = coordinator.restore(backup)

        assertFalse(succeeded)
        assertEquals(previous, favorites.aggregate)
        assertEquals(
            listOf(backup.aggregate, previous),
            favorites.restoreCalls,
        )
        assertTrue(settings.restoreCalls.isNotEmpty())
    }

    @Test
    fun restoreWithoutReadableFavoriteStateChangesNothing() = runBlocking {
        val favorites = FakeFavoritesAccess(initial = null)
        val settings = FakeSettingsAccess()
        val coordinator = SettingsBackupRestoreCoordinator(
            favorites = favorites,
            settings = settings,
            bindings = FakeBindingsAccess(),
            iconShape = FakeIconShapeAccess(),
        )
        val backup = SettingsBackupState(
            aggregate = aggregateWithIdentity(serial = 2),
            settings = DrawerDisplaySettings(),
        )

        val succeeded = coordinator.restore(backup)

        assertFalse(succeeded)
        assertTrue(favorites.restoreCalls.isEmpty())
        assertTrue(settings.restoreCalls.isEmpty())
    }

    @Test
    fun restoreWithoutReadableDisplaySettingsChangesNothing() = runBlocking {
        val favorites = FakeFavoritesAccess(aggregateWithIdentity(serial = 1))
        val settings = FakeSettingsAccess(initial = null)
        val coordinator = SettingsBackupRestoreCoordinator(
            favorites = favorites,
            settings = settings,
            bindings = FakeBindingsAccess(),
            iconShape = FakeIconShapeAccess(),
        )
        val backup = SettingsBackupState(
            aggregate = aggregateWithIdentity(serial = 2),
            settings = DrawerDisplaySettings(),
        )

        val succeeded = coordinator.restore(backup)

        assertFalse(succeeded)
        assertTrue(favorites.restoreCalls.isEmpty())
        assertTrue(settings.restoreCalls.isEmpty())
    }

    @Test
    fun shapeFailureCompensatesAllPreviousStages() = runBlocking {
        val previousFavorites = aggregateWithIdentity(serial = 1)
        val favorites = FakeFavoritesAccess(previousFavorites)
        val settings = FakeSettingsAccess()
        val bindings = FakeBindingsAccess()
        val shape = FakeIconShapeAccess().apply { restoreFails = true }
        val coordinator = SettingsBackupRestoreCoordinator(favorites, settings, bindings, shape)
        val backup = changedBackup()

        assertFalse(coordinator.restore(backup))

        assertEquals(previousFavorites, favorites.aggregate)
        assertEquals(DrawerDisplaySettings(), settings.settings)
        assertEquals(QuickActionBindings(), bindings.bindings)
        assertEquals(IconShapeSettings(), shape.settings)
        assertEquals(listOf(backup.aggregate, previousFavorites), favorites.restoreCalls)
        assertEquals(listOf(backup.settings, DrawerDisplaySettings()), settings.restoreCalls)
        assertEquals(listOf(backup.bindings, QuickActionBindings()), bindings.restoreCalls)
    }

    @Test
    fun shapeExceptionAfterWriteCompensatesShapeAndEarlierStages() = runBlocking {
        listOf(IllegalStateException("write failed"), CancellationException("cancelled"))
            .forEach { failure ->
                val previous = aggregateWithIdentity(serial = 1)
                val favorites = FakeFavoritesAccess(previous)
                val settings = FakeSettingsAccess()
                val bindings = FakeBindingsAccess()
                val shape = FakeIconShapeAccess().apply { failureAfterWrite = failure }
                val coordinator = SettingsBackupRestoreCoordinator(
                    favorites, settings, bindings, shape,
                )
                var cancelled = false
                try {
                    assertFalse(coordinator.restore(changedBackup()))
                } catch (_: CancellationException) {
                    cancelled = true
                }
                assertEquals(failure is CancellationException, cancelled)
                assertEquals(previous, favorites.aggregate)
                assertEquals(DrawerDisplaySettings(), settings.settings)
                assertEquals(QuickActionBindings(), bindings.bindings)
                assertEquals(IconShapeSettings(), shape.settings)
            }
    }

    @Test
    fun unreadableShapePreventsAnyRestoreWrites() = runBlocking {
        val favorites = FakeFavoritesAccess(aggregateWithIdentity(serial = 1))
        val settings = FakeSettingsAccess()
        val bindings = FakeBindingsAccess()
        val shape = FakeIconShapeAccess(initial = null)
        val coordinator = SettingsBackupRestoreCoordinator(favorites, settings, bindings, shape)

        assertFalse(coordinator.restore(changedBackup()))
        assertTrue(favorites.restoreCalls.isEmpty())
        assertTrue(settings.restoreCalls.isEmpty())
        assertTrue(bindings.restoreCalls.isEmpty())
        assertTrue(shape.restoreCalls.isEmpty())
    }

    @Test
    fun earlierFailureDoesNotWriteShape() = runBlocking {
        val favorites = FakeFavoritesAccess(aggregateWithIdentity(serial = 1))
        val bindings = FakeBindingsAccess().apply { restoreFails = true }
        val shape = FakeIconShapeAccess()
        val coordinator = SettingsBackupRestoreCoordinator(
            favorites, FakeSettingsAccess(), bindings, shape,
        )

        assertFalse(coordinator.restore(changedBackup()))
        assertTrue(shape.restoreCalls.isEmpty())
    }

    @Test
    fun failedBindingsCompensationDoesNotPreventOtherCompensations() = runBlocking {
        val previous = aggregateWithIdentity(serial = 1)
        val favorites = FakeFavoritesAccess(previous)
        val settings = FakeSettingsAccess()
        val bindings = FakeBindingsAccess().apply { compensationThrows = true }
        val shape = FakeIconShapeAccess().apply { restoreFails = true }
        val coordinator = SettingsBackupRestoreCoordinator(favorites, settings, bindings, shape)

        assertFalse(coordinator.restore(changedBackup()))
        assertEquals(previous, favorites.aggregate)
        assertEquals(DrawerDisplaySettings(), settings.settings)
        assertEquals(3, bindings.restoreCalls.size)
    }

    @Test
    fun unchangedShapeDoesNotWriteAndOldBackupResetsSavedShape() = runBlocking {
        val shape = FakeIconShapeAccess()
        val coordinator = SettingsBackupRestoreCoordinator(
            FakeFavoritesAccess(OrderedFavoriteAggregate()),
            FakeSettingsAccess(), FakeBindingsAccess(), shape,
        )
        val backup = SettingsBackupState(OrderedFavoriteAggregate(), DrawerDisplaySettings())
        assertTrue(coordinator.restore(backup))
        assertTrue(shape.restoreCalls.isEmpty())

        shape.settings = IconShapeSettings(IconShapeClipMode.Freeform, 19)
        assertTrue(coordinator.restore(backup))
        assertEquals(IconShapeSettings(), shape.settings)
    }

    @Test
    fun realShapeStoreExportsSavedStateAndPublishesRestoredState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "backup-shape-${UUID.randomUUID()}.bin")
        val backupFile = File(file.path + ".json")
        try {
            val store = IconShapeSettingsStore(file)
            assertEquals(null, store.currentIconShapeSettings())
            store.load()
            val saved = IconShapeSettings(IconShapeClipMode.Oem, 29)
            assertTrue(store.replace(saved))
            val controller = SettingsBackupController(
                context,
                FakeFavoritesAccess(OrderedFavoriteAggregate()),
                FakeSettingsAccess(), FakeBindingsAccess(), store,
                homeSafeWidthDp = { 360f },
            )
            val uri = Uri.fromFile(backupFile)
            assertTrue(controller.writeBackup(uri))
            val backup = controller.readBackup(uri)
            assertEquals(saved, backup?.iconShape)
            assertTrue(store.replace(IconShapeSettings(IconShapeClipMode.Freeform, 88)))
            assertTrue(controller.restore(checkNotNull(backup)))
            assertEquals(IconShapeSettingsReadState.Readable(saved), store.state.value)
            assertEquals(saved, store.lastSuccessfulSettings)
            val reloaded = IconShapeSettingsStore(file)
            reloaded.load()
            assertEquals(saved, reloaded.currentIconShapeSettings())
        } finally {
            backupFile.delete()
            file.delete()
            File(file.path + ".bak").delete()
            File(file.path + ".new").delete()
        }
    }

    private fun changedBackup() = SettingsBackupState(
        aggregate = aggregateWithIdentity(serial = 2),
        settings = DrawerDisplaySettings(backgroundOpacity = 100),
        bindings = QuickActionBindings(doubleTap = QuickAction.ScreenLock),
        iconShape = IconShapeSettings(IconShapeClipMode.Freeform, 73),
    )

    private fun aggregateWithIdentity(serial: Long) = OrderedFavoriteAggregate(
        modules = listOf(
            OrderedFavoriteModule(
                id = "vertical-list-1",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(
                    LaunchableIdentity(
                        serial,
                        ComponentName("com.example", "Main"),
                    ),
                ),
            ),
        ),
    )

    private class FakeFavoritesAccess(
        initial: OrderedFavoriteAggregate?,
    ) : BackupFavoritesAccess {
        var aggregate: OrderedFavoriteAggregate? = initial
        var restoreFails = false
        val restoreCalls = mutableListOf<OrderedFavoriteAggregate>()

        override fun currentOrderedAggregate(): OrderedFavoriteAggregate? = aggregate

        override suspend fun restoreAggregate(
            aggregate: OrderedFavoriteAggregate,
        ): Boolean {
            restoreCalls.add(aggregate)
            if (restoreFails) return false
            this.aggregate = aggregate
            return true
        }
    }

    private class FakeSettingsAccess(
        initial: DrawerDisplaySettings? = DrawerDisplaySettings(),
    ) : BackupSettingsAccess {
        var settings: DrawerDisplaySettings? = initial
        var restoreFails = false
        val restoreCalls = mutableListOf<DrawerDisplaySettings>()

        override fun currentSettings(): DrawerDisplaySettings? = settings

        override suspend fun restoreSettings(settings: DrawerDisplaySettings): Boolean {
            restoreCalls.add(settings)
            if (restoreFails) return false
            this.settings = settings
            return true
        }
    }

    private class FakeIconShapeAccess(
        initial: IconShapeSettings? = IconShapeSettings(),
    ) : BackupIconShapeAccess {
        var settings = initial
        var restoreFails = false
        var failureAfterWrite: Exception? = null
        val restoreCalls = mutableListOf<IconShapeSettings>()

        override fun currentIconShapeSettings(): IconShapeSettings? = settings

        override suspend fun restoreIconShapeSettings(settings: IconShapeSettings): Boolean {
            restoreCalls.add(settings)
            if (restoreFails) return false
            this.settings = settings
            failureAfterWrite?.let { failure ->
                failureAfterWrite = null
                throw failure
            }
            return true
        }
    }

    private class FakeBindingsAccess(
        initial: QuickActionBindings? = QuickActionBindings(),
    ) : BackupBindingsAccess {
        var bindings: QuickActionBindings? = initial
        var restoreFails = false
        var compensationThrows = false
        val restoreCalls = mutableListOf<QuickActionBindings>()

        override fun currentBindings(): QuickActionBindings? = bindings

        override suspend fun restoreBindings(bindings: QuickActionBindings): Boolean {
            restoreCalls.add(bindings)
            if (compensationThrows && restoreCalls.size > 1) {
                throw IllegalStateException("compensation failed")
            }
            if (restoreFails) return false
            this.bindings = bindings
            return true
        }
    }
}
