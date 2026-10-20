package com.maxarchm.launcher

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeFavoriteEditorTest {
    @get:Rule
    val files = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @Test
    fun undoRestoresDeletedModuleAtItsMixedOrderPositionWithStyle() = runBlocking(
        block = {
            for (type in OrderedFavoriteModuleType.entries) {
                val first = module(id = "first", identities = listOf(element = identity(serial = 1)))
                val removed = module(
                    id = "removed",
                    identities = listOf(element = identity(serial = 2)),
                    type = type,
                )
                val last = module(id = "last", identities = listOf(element = identity(serial = 3)))
                val original = listOf(first, removed, last)
                val store = storeWith(modules = original)
                val editor = HomeFavoriteEditor(store = store)

                assertTrue(editor.remove(identity = identity(serial = 2)))
                assertEquals(listOf(first, last), modules(store = store))
                val snapshot = checkNotNull(value = editor.undoRemoval)
                assertTrue(editor.undo(snapshot = snapshot))
                store.load()
                assertEquals(original, modules(store = store))
                assertNull(editor.undoRemoval)
            }
        },
    )

    @Test
    fun latestRemovalReplacesUndoAndOldSnackbarCannotDismissIt() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(
                    element = module(
                        id = "list",
                        identities = listOf(identity(serial = 1), identity(serial = 2), identity(serial = 3)),
                    ),
                ),
            )
            val editor = HomeFavoriteEditor(store = store)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val first = checkNotNull(value = editor.undoRemoval)
            assertTrue(editor.remove(identity = identity(serial = 2)))
            val second = checkNotNull(value = editor.undoRemoval)
            editor.dismissUndo(snapshot = first)
            assertSame(second, editor.undoRemoval)
            assertFalse(editor.undo(snapshot = first))
            assertTrue(editor.remove(identity = identity(serial = 99)))
            assertSame(second, editor.undoRemoval)
            assertTrue(editor.undo(snapshot = second))
            assertEquals(listOf(identity(serial = 2), identity(serial = 3)), modules(store = store)[0].identities)
        },
    )

    @Test
    fun inventoryCleanupDoesNotResurrectOtherApplicationsOrMisplaceRestoredModule() = runBlocking(
        block = {
            val first = module(id = "first", identities = listOf(element = identity(serial = 1)))
            val middle = module(id = "middle", identities = listOf(identity(serial = 2), identity(serial = 3)))
            val last = module(id = "last", identities = listOf(element = identity(serial = 4)))
            val store = storeWith(modules = listOf(first, middle, last))
            val editor = HomeFavoriteEditor(store = store)
            assertTrue(editor.remove(identity = identity(serial = 2)))
            val snapshot = checkNotNull(value = editor.undoRemoval)

            assertTrue(store.removeAll(identities = setOf(identity(serial = 1), identity(serial = 3))))
            editor.reconcileAvailability(
                availability = mapOf(
                    identity(serial = 1) to FavoriteAvailability.ConfirmedRemoved,
                    identity(serial = 3) to FavoriteAvailability.ConfirmedRemoved,
                    identity(serial = 2) to FavoriteAvailability.Disabled(presentationEntry = null),
                ),
            )
            assertSame(snapshot, editor.undoRemoval)
            assertTrue(editor.undo(snapshot = snapshot))
            store.load()
            assertEquals(
                listOf(middle.copy(identities = listOf(element = identity(serial = 2))), last),
                modules(store = store),
            )
        },
    )

    @Test
    fun restoreUsesSurvivingApplicationNeighborsAfterInventoryCleanup() {
        val original = OrderedFavoriteAggregate(
            modules = listOf(
                element = module(
                    id = "list",
                    identities = listOf(identity(serial = 1), identity(serial = 2), identity(serial = 3)),
                ),
            ),
        )
        val snapshot = checkNotNull(
            value = captureFavoriteRemoval(aggregate = original, identity = identity(serial = 2)),
        )
        val current = removeOrderedFavorite(
            aggregate = removeOrderedFavorite(aggregate = original, identity = identity(serial = 2)),
            identity = identity(serial = 1),
        )
        assertEquals(
            listOf(identity(serial = 2), identity(serial = 3)),
            restoreOrderedFavorite(aggregate = current, snapshot = snapshot)?.identities,
        )
    }

    @Test
    fun barRemovalRestoresFormerPlaceUntilTheCapturedCapacityIsFull() {
        val original = OrderedFavoriteAggregate(
            homeBottomAppBar = HomeBottomAppBarState(
                enabled = true,
                identities = listOf(identity(serial = 1), identity(serial = 2)),
            ),
        )
        val snapshot = checkNotNull(
            value = captureFavoriteRemoval(aggregate = original, identity = identity(serial = 1)),
        )
        val removed = removeOrderedFavorite(aggregate = original, identity = identity(serial = 1))
        assertEquals(
            listOf(identity(serial = 1), identity(serial = 2)),
            restoreOrderedFavorite(aggregate = removed, snapshot = snapshot)
                ?.homeBottomAppBar
                ?.identities,
        )
        val filled = removed.copy(
            homeBottomAppBar = removed.homeBottomAppBar.copy(
                identities = listOf(identity(serial = 3), identity(serial = 4)),
            ),
        )
        assertNull(restoreOrderedFavorite(aggregate = filled, snapshot = snapshot))
    }

    @Test
    fun permanentDisappearanceInvalidatesUndoButUncertainRefreshDoesNot() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(element = module(id = "list", identities = listOf(element = identity(serial = 1)))),
            )
            val editor = HomeFavoriteEditor(store = store)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            editor.reconcileAvailability(
                availability = mapOf(identity(serial = 1) to FavoriteAvailability.Unknown(presentationEntry = null)),
            )
            assertSame(snapshot, editor.undoRemoval)
            editor.reconcileAvailability(
                availability = mapOf(identity(serial = 1) to FavoriteAvailability.ConfirmedRemoved),
            )
            assertNull(editor.undoRemoval)
            assertFalse(editor.undo(snapshot = snapshot))
            assertTrue(modules(store = store).isEmpty())
        },
    )

    @Test
    fun failedUndoKeepsDurableStateAndDoesNotOfferAutomaticRetry() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(element = module(id = "list", identities = listOf(element = identity(serial = 1)))),
            )
            val controlled = ControlledStore(delegate = store)
            val editor = HomeFavoriteEditor(store = controlled)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            controlled.failWrites = true
            assertFalse(editor.undo(snapshot = snapshot))
            assertNull(editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            assertTrue(modules(store = store).isEmpty())
        },
    )

    @Test
    fun failedRemovalPreservesThePreviousUndoAndDurableMembership() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(
                    element = module(id = "list", identities = listOf(identity(serial = 1), identity(serial = 2))),
                ),
            )
            val controlled = ControlledStore(delegate = store)
            val editor = HomeFavoriteEditor(store = controlled)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            controlled.failWrites = true
            assertFalse(editor.remove(identity = identity(serial = 2)))
            assertSame(snapshot, editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            assertEquals(listOf(element = identity(serial = 2)), modules(store = store)[0].identities)
        },
    )

    @Test
    fun undoRevalidationCanRejectRestorationWithoutWritingOrRetrying() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(element = module(id = "list", identities = listOf(element = identity(serial = 1)))),
            )
            val editor = HomeFavoriteEditor(store = store)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            assertFalse(editor.undo(snapshot = snapshot, revalidate = { false }))
            assertNull(editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            assertTrue(modules(store = store).isEmpty())
        },
    )

    @Test
    fun successfulOtherMutationInvalidatesUndoButFailedMutationDoesNot() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(
                    module(id = "first", identities = listOf(identity(serial = 1), identity(serial = 2))),
                    module(id = "second", identities = listOf(element = identity(serial = 3))),
                ),
            )
            val editor = HomeFavoriteEditor(store = store)
            assertTrue(editor.remove(identity = identity(serial = 1)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            assertFalse(editor.reorderModules(moduleIds = listOf(element = "unknown")))
            assertSame(snapshot, editor.undoRemoval)
            assertTrue(editor.reorderModules(moduleIds = listOf("second", "first")))
            assertNull(editor.undoRemoval)
        },
    )

    @Test
    fun interruptionDuringSaveDoesNotRecreateUndoAndConcurrentRemovalIsRejected() = runBlocking(
        block = {
            val store = storeWith(
                modules = listOf(
                    element = module(id = "list", identities = listOf(identity(serial = 1), identity(serial = 2))),
                ),
            )
            val gate = CompletableDeferred<Unit>()
            val controlled = ControlledStore(delegate = store)
            controlled.gate = gate
            val editor = HomeFavoriteEditor(store = controlled)
            val saving = async(
                start = CoroutineStart.UNDISPATCHED,
                block = { editor.remove(identity = identity(serial = 1)) },
            )
            assertTrue(editor.isSaving)
            assertFalse(editor.remove(identity = identity(serial = 2)))
            editor.invalidateUndo()
            gate.complete(value = Unit)
            assertTrue(saving.await())
            assertNull(editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            assertEquals(listOf(element = identity(serial = 2)), modules(store = store)[0].identities)
        },
    )

    @Test
    fun applicationInsertionPersistsOnlyTheSourceOrderAndInvalidatesUndo() = runBlocking(
        block = {
            for (type in OrderedFavoriteModuleType.entries) {
                val source = module(
                    id = "source", type = type,
                    identities = listOf(identity(serial = 1), identity(serial = 2), identity(serial = 3), identity(serial = 4)),
                )
                val other = module(id = "other", identities = listOf(element = identity(serial = 5)))
                val store = storeWith(modules = listOf(source, other))
                val editor = HomeFavoriteEditor(store = store)
                assertTrue(editor.remove(identity = identity(serial = 4)))
                assertTrue(editor.reorderApplication(
                    change = ApplicationOrderChange(
                        moduleId = source.id, identity = identity(serial = 1),
                        originalOrder = source.identities.take(n = 3), boundary = 2,
                    ),
                ))
                store.load()
                assertEquals(
                    listOf(source.copy(identities = listOf(identity(serial = 2), identity(serial = 3), identity(serial = 1))), other),
                    modules(store = store),
                )
                assertNull(editor.undoRemoval)
            }
        },
    )

    @Test
    fun applicationNoChangeDoesNotWriteAndSaveFailurePreservesOrderAndUndo() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(identity(serial = 1), identity(serial = 2), identity(serial = 3)))
            val store = storeWith(modules = listOf(element = source))
            val controlled = ControlledStore(delegate = store)
            val editor = HomeFavoriteEditor(store = controlled)
            assertTrue(editor.remove(identity = identity(serial = 3)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            val change = ApplicationOrderChange(
                moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities.take(n = 2), boundary = 0,
            )
            val writes = controlled.writeAttempts
            assertTrue(editor.reorderApplication(change = change))
            assertEquals(writes, controlled.writeAttempts)
            assertSame(snapshot, editor.undoRemoval)
            controlled.failWrites = true
            assertFalse(editor.reorderApplication(change = change.copy(boundary = 1)))
            assertSame(snapshot, editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            assertEquals(source.identities.take(n = 2), modules(store = store)[0].identities)
        },
    )

    @Test
    fun inventoryChangeWhileMovementSaveWaitsCannotResurrectOrOverwriteIdentities() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(identity(serial = 1), identity(serial = 2), identity(serial = 3)))
            val store = storeWith(modules = listOf(element = source))
            val gate = CompletableDeferred<Unit>()
            val controlled = ControlledStore(delegate = store)
            controlled.gate = gate
            val editor = HomeFavoriteEditor(store = controlled)
            val saving = async(
                start = CoroutineStart.UNDISPATCHED,
                block = {
                    editor.reorderApplication(
                        change = ApplicationOrderChange(
                            moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities, boundary = 2,
                        ),
                    )
                },
            )
            assertTrue(editor.isSaving)
            assertFalse(editor.remove(identity = identity(serial = 2)))
            assertTrue(store.remove(identity = identity(serial = 2)))
            gate.complete(value = Unit)
            assertFalse(saving.await())
            store.load()
            assertEquals(listOf(identity(serial = 1), identity(serial = 3)), modules(store = store)[0].identities)
        },
    )

    @Test
    fun crossModuleMovementPersistsEveryTypePairAndDeletesOnlyAnEmptiedSource() = runBlocking(
        block = {
            for (sourceType in OrderedFavoriteModuleType.entries) {
                for (destinationType in OrderedFavoriteModuleType.entries) {
                    for (onlyMember in listOf(true, false)) {
                        val source = module(
                            id = "source", type = sourceType,
                            identities = if (onlyMember) listOf(element = identity(serial = 1)) else listOf(identity(serial = 1), identity(serial = 2)),
                        )
                        val target = module(id = "target", type = destinationType, identities = listOf(identity(serial = 3), identity(serial = 4)))
                        val untouched = module(id = "untouched", identities = listOf(element = identity(serial = 5)))
                        val store = storeWith(modules = listOf(source, untouched, target))
                        val editor = HomeFavoriteEditor(store = store)
                        assertTrue(editor.reorderApplication(
                            change = ApplicationOrderChange(
                                moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities,
                                destinationModuleId = target.id, destinationOrder = target.identities, boundary = 1,
                            ),
                        ))
                        store.load()
                        val expectedSource = if (onlyMember) emptyList() else listOf(element = source.copy(identities = listOf(element = identity(serial = 2))))
                        assertEquals(
                            expectedSource + listOf(untouched, target.copy(identities = listOf(identity(serial = 3), identity(serial = 1), identity(serial = 4)))),
                            modules(store = store),
                        )
                        assertNull(editor.undoRemoval)
                    }
                }
            }
        },
    )

    @Test
    fun failedCrossModuleSaveRetainsTheSourceAndDoesNotPartiallyChangeTheTarget() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(element = identity(serial = 1)))
            val target = module(id = "target", type = OrderedFavoriteModuleType.Ribbon, identities = listOf(element = identity(serial = 2)))
            val original = listOf(source, target)
            val store = storeWith(modules = original)
            val controlled = ControlledStore(delegate = store)
            controlled.failWrites = true
            val editor = HomeFavoriteEditor(store = controlled)
            assertFalse(editor.reorderApplication(
                change = ApplicationOrderChange(
                    moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities,
                    destinationModuleId = target.id, destinationOrder = target.identities, boundary = 0,
                ),
            ))
            store.load()
            assertEquals(original, modules(store = store))
            assertFalse(editor.isSaving)
        },
    )

    @Test
    fun destinationInventoryChangeWhileSaveWaitsIsNotOverwritten() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(element = identity(serial = 1)))
            val target = module(id = "target", identities = listOf(identity(serial = 2), identity(serial = 3)))
            val store = storeWith(modules = listOf(source, target))
            val gate = CompletableDeferred<Unit>()
            val controlled = ControlledStore(delegate = store)
            controlled.gate = gate
            val editor = HomeFavoriteEditor(store = controlled)
            val saving = async(
                start = CoroutineStart.UNDISPATCHED,
                block = {
                    editor.reorderApplication(
                        change = ApplicationOrderChange(
                            moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities,
                            destinationModuleId = target.id, destinationOrder = target.identities, boundary = 1,
                        ),
                    )
                },
            )
            assertTrue(store.remove(identity = identity(serial = 2)))
            gate.complete(value = Unit)
            assertFalse(saving.await())
            store.load()
            assertEquals(listOf(source, target.copy(identities = listOf(element = identity(serial = 3)))), modules(store = store))
        },
    )

    @Test
    fun dropToCreateAppendsDefaultModuleAndAtomicallyRemovesOnlyAnEmptiedSource() = runBlocking(
        block = {
            for (sourceType in OrderedFavoriteModuleType.entries) {
                for (newType in OrderedFavoriteModuleType.entries) {
                    for (onlyMember in listOf(true, false)) {
                        val source = module(
                            id = "source", type = sourceType,
                            identities = if (onlyMember) listOf(element = identity(serial = 1)) else listOf(identity(serial = 1), identity(serial = 2)),
                        )
                        val other = module(id = "other", identities = listOf(element = identity(serial = 3)))
                        val store = storeWith(modules = listOf(source, other))
                        val editor = HomeFavoriteEditor(store = store)
                        assertTrue(editor.reorderApplication(
                            change = ApplicationOrderChange(
                                moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities,
                                boundary = 0, newModuleType = newType,
                            ),
                        ))
                        store.load()
                        val actual = modules(store = store)
                        val created = actual.last()
                        assertEquals(newType, created.type)
                        assertEquals(listOf(element = identity(serial = 1)), created.identities)
                        assertEquals(FavoriteListSize.Medium, created.iconSize)
                        assertEquals(FavoriteListSize.Medium, created.textSize)
                        assertEquals(FavoriteNamePlacement.Right, created.namePlacement)
                        assertEquals(1, created.itemsPerRow)
                        assertTrue(created.id != source.id && created.id != other.id)
                        val preceding = if (onlyMember) listOf(element = other) else listOf(source.copy(identities = listOf(element = identity(serial = 2))), other)
                        assertEquals(preceding, actual.dropLast(n = 1))
                        assertNull(editor.undoRemoval)
                    }
                }
            }
        },
    )

    @Test
    fun failedCreationPreservesDurableStateAndPreviousUndoThenSuccessInvalidatesUndo() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(identity(serial = 1), identity(serial = 2)))
            val store = storeWith(modules = listOf(element = source))
            val controlled = ControlledStore(delegate = store)
            val editor = HomeFavoriteEditor(store = controlled)
            assertTrue(editor.remove(identity = identity(serial = 2)))
            val snapshot = checkNotNull(value = editor.undoRemoval)
            val change = ApplicationOrderChange(
                moduleId = source.id, identity = identity(serial = 1), originalOrder = listOf(element = identity(serial = 1)),
                boundary = 0, newModuleType = OrderedFavoriteModuleType.Ribbon,
            )
            controlled.failWrites = true
            assertFalse(editor.reorderApplication(change = change))
            assertSame(snapshot, editor.undoRemoval)
            store.load()
            assertEquals(listOf(element = source.copy(identities = change.originalOrder)), modules(store = store))
            controlled.failWrites = false
            assertTrue(editor.reorderApplication(change = change))
            assertNull(editor.undoRemoval)
        },
    )

    @Test
    fun sourceDisappearanceWhileCreationWaitsCannotCreateAStaleModule() = runBlocking(
        block = {
            val source = module(id = "source", identities = listOf(element = identity(serial = 1)))
            val store = storeWith(modules = listOf(element = source))
            val gate = CompletableDeferred<Unit>()
            val controlled = ControlledStore(delegate = store)
            controlled.gate = gate
            val editor = HomeFavoriteEditor(store = controlled)
            val saving = async(
                start = CoroutineStart.UNDISPATCHED,
                block = {
                    editor.reorderApplication(
                        change = ApplicationOrderChange(
                            moduleId = source.id, identity = identity(serial = 1), originalOrder = source.identities,
                            boundary = 0, newModuleType = OrderedFavoriteModuleType.Vertical,
                        ),
                    )
                },
            )
            assertTrue(store.remove(identity = identity(serial = 1)))
            gate.complete(value = Unit)
            assertFalse(saving.await())
            store.load()
            assertTrue(modules(store = store).isEmpty())
        },
    )

    private class ControlledStore(private val delegate: FavoriteStore) : FavoriteStore by delegate {
        var failWrites = false
        var gate: CompletableDeferred<Unit>? = null
        var writeAttempts = 0

        override suspend fun updateOrderedAggregate(
            transform: (OrderedFavoriteAggregate) -> OrderedFavoriteAggregate,
        ): OrderedFavoriteAggregate? {
            writeAttempts += 1
            gate?.await()
            if (failWrites) return null
            return delegate.updateOrderedAggregate(transform = transform)
        }
    }

    private suspend fun storeWith(modules: List<OrderedFavoriteModule>): OrderedFavoriteStoreAdapter {
        val file = files.newFolder().resolve(relative = "favorites.bin")
        val store = OrderedFavoriteModuleStore(file = file)
        store.load()
        check(value = store.replaceAggregate(aggregate = OrderedFavoriteAggregate(modules = modules)))
        return OrderedFavoriteStoreAdapter(file = file).also(block = { it.load() })
    }

    private fun modules(store: FavoriteStore): List<OrderedFavoriteModule> =
        checkNotNull(value = (store.state.value as? FavoriteReadState.Readable)?.orderedModules)

    private fun identity(serial: Long): LaunchableIdentity = LaunchableIdentity(
        profileSerialNumber = serial,
        componentName = ComponentName("com.example", "Main"),
    )

    private fun module(
        id: String,
        identities: List<LaunchableIdentity>,
        type: OrderedFavoriteModuleType = OrderedFavoriteModuleType.Vertical,
    ): OrderedFavoriteModule = OrderedFavoriteModule(
        id = id,
        type = type,
        identities = identities,
        iconSize = if (type == OrderedFavoriteModuleType.Vertical) {
            FavoriteListSize.Large
        } else {
            FavoriteListSize.Medium
        },
        textSize = if (type == OrderedFavoriteModuleType.Vertical) {
            FavoriteListSize.Large
        } else {
            FavoriteListSize.Medium
        },
        namePlacement = if (type == OrderedFavoriteModuleType.Vertical) FavoriteNamePlacement.Below else FavoriteNamePlacement.Right,
        itemsPerRow = if (type == OrderedFavoriteModuleType.Vertical) 2 else 1,
    )
}
