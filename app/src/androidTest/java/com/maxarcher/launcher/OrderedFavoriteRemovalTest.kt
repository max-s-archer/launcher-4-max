package com.maxarcher.launcher

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OrderedFavoriteRemovalTest {
    @get:Rule
    val files = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @Test
    fun removingLastApplicationDeletesEitherModuleTypeAndPersistsEmptyHome() = runBlocking(
        block = {
            for (type in OrderedFavoriteModuleType.entries) {
                val file = favoriteFile()
                val removed = identity(serial = 1)
                val adapter = seed(
                    file = file,
                    modules = listOf(
                        element = OrderedFavoriteModule(
                            id = "only-module",
                            type = type,
                            identities = listOf(element = removed),
                        ),
                    ),
                )

                assertTrue(adapter.remove(identity = removed))

                assertEquals(emptyList<OrderedFavoriteModule>(), modules(adapter = adapter))
                assertPersisted(file = file, expected = emptyList())
            }
        },
    )

    @Test
    fun removalPreservesExactIdentityStyleAndMixedModuleOrder() = runBlocking(
        block = {
            val file = favoriteFile()
            val removed = identity(serial = 1)
            // Same component, different profile: removing one must not remove the other.
            val retained = identity(serial = 2)
            val first = OrderedFavoriteModule(
                id = "first-ribbon",
                type = OrderedFavoriteModuleType.Ribbon,
                identities = listOf(element = identity(serial = 3)),
            )
            val middle = OrderedFavoriteModule(
                id = "middle-list",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(removed, retained),
                iconSize = FavoriteListSize.Large,
                textSize = FavoriteListSize.Large,
                namePlacement = FavoriteNamePlacement.Below,
                itemsPerRow = 2,
            )
            val last = OrderedFavoriteModule(
                id = "last-ribbon",
                type = OrderedFavoriteModuleType.Ribbon,
                identities = listOf(element = identity(serial = 4)),
            )
            val adapter = seed(file = file, modules = listOf(first, middle, last))
            val initial = modules(adapter = adapter)

            assertTrue(adapter.remove(identity = removed))

            val expected = listOf(
                first,
                middle.copy(identities = listOf(element = retained)),
                last,
            )
            assertEquals(expected, modules(adapter = adapter))
            assertSame(initial[0], modules(adapter = adapter)[0])
            assertSame(initial[2], modules(adapter = adapter)[2])
            assertPersisted(file = file, expected = expected)

            assertTrue(adapter.remove(identity = retained))

            assertEquals(listOf(first, last), modules(adapter = adapter))
            assertPersisted(file = file, expected = listOf(first, last))
        },
    )

    @Test
    fun inventoryRemovalDeletesEmptyModulesAndKeepsUnaffectedApplicationOrder() = runBlocking(
        block = {
            val file = favoriteFile()
            val first = OrderedFavoriteModule(
                id = "first-ribbon",
                type = OrderedFavoriteModuleType.Ribbon,
                identities = listOf(element = identity(serial = 1)),
            )
            val middle = OrderedFavoriteModule(
                id = "middle-list",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(identity(serial = 2), identity(serial = 3), identity(serial = 4)),
                iconSize = FavoriteListSize.Small,
                textSize = FavoriteListSize.Small,
                namePlacement = FavoriteNamePlacement.Below,
                itemsPerRow = 3,
            )
            val last = OrderedFavoriteModule(
                id = "last-list",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(element = identity(serial = 5)),
            )
            val adapter = seed(file = file, modules = listOf(first, middle, last))

            assertTrue(
                adapter.removeAll(
                    identities = setOf(identity(serial = 1), identity(serial = 3), identity(serial = 5)),
                ),
            )

            val expected = listOf(
                element = middle.copy(
                    identities = listOf(identity(serial = 2), identity(serial = 4)),
                ),
            )
            assertEquals(expected, modules(adapter = adapter))
            assertPersisted(file = file, expected = expected)

            assertTrue(adapter.removeAll(identities = setOf(identity(serial = 2), identity(serial = 4))))
            assertEquals(emptyList<OrderedFavoriteModule>(), modules(adapter = adapter))
            assertPersisted(file = file, expected = emptyList())
        },
    )

    @Test
    fun absentAndEmptyRemovalsPreserveModulesAndSavedBytes() = runBlocking(
        block = {
            val file = favoriteFile()
            val module = OrderedFavoriteModule(
                id = "unchanged-list",
                type = OrderedFavoriteModuleType.Vertical,
                identities = listOf(element = identity(serial = 1)),
            )
            val adapter = seed(file = file, modules = listOf(element = module))
            val initial = modules(adapter = adapter)
            val initialBytes = file.readBytes()

            assertTrue(adapter.remove(identity = identity(serial = 99)))
            assertTrue(adapter.removeAll(identities = emptySet()))

            assertEquals(initial, modules(adapter = adapter))
            assertSame(initial[0], modules(adapter = adapter)[0])
            assertArrayEquals(initialBytes, file.readBytes())
        },
    )

    @Test
    fun removalsBeforeSuccessfulLoadDoNotModifyUnreadableData() = runBlocking(
        block = {
            val file = favoriteFile()
            val unreadableBytes = byteArrayOf(0x01, 0x02, 0x03)
            file.writeBytes(array = unreadableBytes)
            val adapter = OrderedFavoriteStoreAdapter(file = file)
            val removed = identity(serial = 1)

            assertFalse(adapter.remove(identity = removed))
            assertFalse(adapter.removeAll(identities = setOf(element = removed)))
            adapter.load()
            assertEquals(FavoriteReadState.ReadFailure, adapter.state.value)
            assertFalse(adapter.remove(identity = removed))
            assertFalse(adapter.removeAll(identities = setOf(element = removed)))

            assertArrayEquals(unreadableBytes, file.readBytes())
        },
    )

    private fun favoriteFile(): File = files.newFolder().resolve(relative = "favorites.bin")

    private fun identity(serial: Long): LaunchableIdentity = LaunchableIdentity(
        profileSerialNumber = serial,
        componentName = ComponentName("com.example", "Main"),
    )

    private suspend fun seed(
        file: File,
        modules: List<OrderedFavoriteModule>,
    ): OrderedFavoriteStoreAdapter {
        val store = OrderedFavoriteModuleStore(file = file)
        store.load()
        check(value = store.replaceAggregate(aggregate = OrderedFavoriteAggregate(modules = modules)))
        return OrderedFavoriteStoreAdapter(file = file).also(
            block = { adapter -> adapter.load() },
        )
    }

    private fun modules(adapter: OrderedFavoriteStoreAdapter): List<OrderedFavoriteModule> =
        checkNotNull(value = (adapter.state.value as? FavoriteReadState.Readable)?.orderedModules)

    private suspend fun assertPersisted(
        file: File,
        expected: List<OrderedFavoriteModule>,
    ) {
        val store = OrderedFavoriteModuleStore(file = file)
        store.load()
        assertEquals(
            OrderedFavoriteReadState.Readable(aggregate = OrderedFavoriteAggregate(modules = expected)),
            store.state.value,
        )
    }
}
