package com.maxarcher.launcher

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
    fun barRemovalRestoresFormerPlaceOnlyWhenCurrentCapacityHasRoom() {
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
            restoreOrderedFavorite(aggregate = removed, snapshot = snapshot, barCapacity = 2)
                ?.homeBottomAppBar
                ?.identities,
        )
        val filled = removed.copy(
            homeBottomAppBar = removed.homeBottomAppBar.copy(
                identities = listOf(identity(serial = 3), identity(serial = 4)),
            ),
        )
        assertNull(
            restoreOrderedFavorite(aggregate = filled, snapshot = snapshot, barCapacity = 2),
        )
        assertEquals(
            listOf(identity(1), identity(3), identity(4)),
            restoreOrderedFavorite(aggregate = filled, snapshot = snapshot, barCapacity = 4)
                ?.homeBottomAppBar?.identities,
        )
        assertNull(
            restoreOrderedFavorite(aggregate = removed, snapshot = snapshot, barCapacity = 1),
        )
        assertNull(restoreOrderedFavorite(aggregate = removed, snapshot = snapshot))
        assertNull(
            restoreOrderedFavorite(
                aggregate = removed.copy(
                    homeBottomAppBar = removed.homeBottomAppBar.copy(enabled = false),
                ),
                snapshot = snapshot,
                barCapacity = 4,
            ),
        )
    }

    @Test
    fun barUndoUsesWidthAndIconSizeAtTransactionTime() = runBlocking {
        for (width in listOf(100f, 200f)) {
            val store = storeWith(emptyList())
            store.updateOrderedAggregate {
                it.copy(
                    homeBottomAppBar = HomeBottomAppBarState(
                        enabled = true,
                        identities = listOf(identity(1), identity(2)),
                    ),
                )
            }
            val controlled = ControlledStore(store)
            val editor = HomeFavoriteEditor(controlled)
            assertTrue(editor.remove(identity(1)))
            val snapshot = checkNotNull(editor.undoRemoval)
            var currentWidth = 300f
            val gate = CompletableDeferred<Unit>()
            controlled.gate = gate
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                editor.undo(
                    snapshot = snapshot,
                    currentBarCapacity = { bar ->
                        homeBottomAppBarCapacity(
                            safeWidthDp = currentWidth,
                            outerMarginDp = 16f,
                            iconSizeDp = if (bar.iconSize == FavoriteListSize.Large) 80f else 40f,
                            horizontalInsetDp = 0f,
                            iconGapDp = 0f,
                        )
                    },
                )
            }
            currentWidth = width
            store.updateOrderedAggregate {
                it.copy(
                    homeBottomAppBar = it.homeBottomAppBar.copy(
                        iconSize = FavoriteListSize.Large,
                    ),
                )
            }
            val before = store.currentOrderedAggregate()
            gate.complete(Unit)
            val expectedSuccess = width == 200f
            assertEquals(expectedSuccess, pending.await())
            assertNull(editor.undoRemoval)
            assertFalse(editor.isSaving)
            store.load()
            if (expectedSuccess) {
                assertEquals(
                    listOf(identity(1), identity(2)),
                    store.currentOrderedAggregate()?.homeBottomAppBar?.identities,
                )
            } else {
                assertEquals(before, store.currentOrderedAggregate())
            }
            assertFalse(editor.undo(snapshot, currentBarCapacity = { 10 }))
        }
    }

    @Test
    fun barUndoRejectsUnknownZeroAndDisabledCapacityWithoutChangingData() = runBlocking {
        for ((enabled, capacity) in listOf(true to null, true to 0, true to 1, false to 4)) {
            val store = storeWith(emptyList())
            store.updateOrderedAggregate {
                it.copy(
                    homeBottomAppBar = HomeBottomAppBarState(
                        enabled = enabled,
                        identities = listOf(identity(1), identity(2)),
                    ),
                )
            }
            val editor = HomeFavoriteEditor(store)
            assertTrue(editor.remove(identity(1)))
            val snapshot = checkNotNull(editor.undoRemoval)
            val before = store.currentOrderedAggregate()
            assertFalse(editor.undo(snapshot, currentBarCapacity = { capacity }))
            assertNull(editor.undoRemoval)
            store.load()
            assertEquals(before, store.currentOrderedAggregate())
        }
    }

    @Test
    fun ordinaryModuleUndoDoesNotReadBarCapacity() = runBlocking {
        val store = storeWith(listOf(module("list", listOf(identity(1)))))
        val editor = HomeFavoriteEditor(store)
        assertTrue(editor.remove(identity(1)))
        assertTrue(
            editor.undo(
                snapshot = checkNotNull(editor.undoRemoval),
                currentBarCapacity = { error("Ordinary modules do not need bar capacity") },
            ),
        )
        store.load()
        assertEquals(listOf(identity(1)), store.currentOrderedAggregate()?.identities)
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

    @Test
    fun compositionPreservesMixedOrderStylesAndBarAcrossReload() = runBlocking {
        val ribbon = module("ribbon", listOf(identity(1)), OrderedFavoriteModuleType.Ribbon)
        val vertical = module("vertical", listOf(identity(2)))
        val last = module("last", listOf(identity(3)), OrderedFavoriteModuleType.Ribbon)
        val store = storeWith(listOf(ribbon, vertical, last))
        val bar = HomeBottomAppBarState(enabled = true, identities = listOf(identity(4)))
        checkNotNull(store.updateOrderedAggregate { it.copy(homeBottomAppBar = bar) })
        val editor = HomeFavoriteEditor(store)

        val saved = editor.updateComposition { aggregate ->
            aggregate.copy(
                modules = aggregate.modules.map {
                    if (it.id == vertical.id) {
                        it.copy(
                            identities = it.identities + identity(5),
                            iconSize = FavoriteListSize.Small,
                        )
                    } else it
                },
            )
        }

        val expected = listOf(
            ribbon,
            vertical.copy(
                identities = listOf(identity(2), identity(5)),
                iconSize = FavoriteListSize.Small,
            ),
            last,
        )
        assertEquals(OrderedFavoriteAggregate(expected, bar), saved)
        store.load()
        assertEquals(saved, store.currentOrderedAggregate())
        assertFalse(editor.isSaving)
    }

    @Test
    fun failedAndUnchangedCompositionKeepUndoButSuccessfulEditInvalidatesIt() = runBlocking {
        val store = storeWith(listOf(module("list", listOf(identity(1), identity(2)))))
        val controlled = ControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        assertTrue(editor.remove(identity(1)))
        val undo = checkNotNull(editor.undoRemoval)
        val before = store.currentOrderedAggregate()
        controlled.failWrites = true

        assertNull(editor.updateComposition { it.copy(modules = emptyList()) })
        assertEquals(before, store.currentOrderedAggregate())
        assertSame(undo, editor.undoRemoval)
        assertFalse(editor.isSaving)

        controlled.failWrites = false
        assertEquals(before, editor.updateComposition { it })
        assertSame(undo, editor.undoRemoval)
        assertNull(editor.updateComposition {
            it.copy(modules = it.modules + it.modules.single())
        })
        assertSame(undo, editor.undoRemoval)
        assertEquals(before, store.currentOrderedAggregate())
        assertEquals(OrderedFavoriteAggregate(), editor.updateComposition {
            it.copy(modules = emptyList())
        })
        assertNull(editor.undoRemoval)
    }

    @Test
    fun compositionUsesLatestStateAfterConcurrentReconciliationAndRejectsOverlap() = runBlocking {
        val store = storeWith(
            listOf(
                module("first", listOf(identity(1))),
                module("target", listOf(identity(2), identity(3))),
            ),
        )
        val controlled = ControlledStore(store)
        val gate = CompletableDeferred<Unit>()
        controlled.gate = gate
        val editor = HomeFavoriteEditor(controlled)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            editor.updateComposition { aggregate ->
                aggregate.copy(
                    modules = aggregate.modules.map {
                        if (it.id == "target") it.copy(textSize = FavoriteListSize.Small) else it
                    },
                )
            }
        }
        assertTrue(editor.isSaving)
        assertNull(editor.updateComposition { it.copy(modules = emptyList()) })
        assertFalse(editor.remove(identity(2)))
        assertTrue(store.removeAll(setOf(identity(1), identity(3))))
        gate.complete(Unit)

        val saved = checkNotNull(pending.await())
        assertEquals(listOf(identity(2)), saved.identities)
        assertEquals(FavoriteListSize.Small, saved.modules.single().textSize)
        assertEquals(1, controlled.writeAttempts)
        assertFalse(editor.isSaving)
        store.load()
        assertEquals(saved, store.currentOrderedAggregate())
    }

    @Test
    fun wholeUndoRestoresEveryFieldWithoutRollingBackOtherModulesOrTheBar() = runBlocking {
        for (type in OrderedFavoriteModuleType.entries) {
            val first = module("first", listOf(identity(1)))
            val removed = module("removed", listOf(identity(2), identity(3)), type)
                .copy(name = "Work", nameVisible = true)
            val last = module("last", listOf(identity(4)))
            val store = storeWith(listOf(first, removed, last))
            val bar = HomeBottomAppBarState(
                enabled = true,
                identities = listOf(identity(5), identity(6)),
            )
            store.updateOrderedAggregate { it.copy(homeBottomAppBar = bar) }
            val editor = HomeFavoriteEditor(store)
            assertTrue(editor.removeModule(removed.id))
            val snapshot = checkNotNull(editor.undoRemoval)
            assertTrue(snapshot.wholeModule)
            val updatedFirst = first.copy(name = "Updated", textSize = FavoriteListSize.Small)
            val updatedBar = bar.copy(identities = bar.identities.reversed())
            assertEquals(
                OrderedFavoriteAggregate(listOf(updatedFirst, last), updatedBar),
                editor.updateComposition {
                    it.copy(modules = listOf(updatedFirst, last), homeBottomAppBar = updatedBar)
                },
            )
            assertTrue(editor.reorderModules(listOf(last.id, first.id)))
            editor.invalidateApplicationUndo()
            assertSame(snapshot, editor.undoRemoval)

            assertTrue(editor.undo(snapshot))
            val expected = OrderedFavoriteAggregate(
                modules = listOf(removed, last, updatedFirst),
                homeBottomAppBar = updatedBar,
            )
            assertEquals(expected, store.currentOrderedAggregate())
            assertFalse(editor.undo(snapshot))
            store.load()
            assertEquals(expected, store.currentOrderedAggregate())
        }
    }

    @Test
    fun applicationAndWholeRemovalsShareOnlyTheLatestSnapshot() = runBlocking {
        val first = module("first", listOf(identity(1), identity(2)))
        val second = module("second", listOf(identity(3), identity(4)))
        val store = storeWith(listOf(first, second))
        val editor = HomeFavoriteEditor(store)
        assertTrue(editor.remove(identity(1)))
        val application = checkNotNull(editor.undoRemoval)
        assertTrue(editor.removeModule(second.id))
        val whole = checkNotNull(editor.undoRemoval)
        assertTrue(whole.wholeModule)
        editor.dismissUndo(application)
        assertSame(whole, editor.undoRemoval)
        assertFalse(editor.undo(application))
        assertTrue(editor.remove(identity(2)))
        val latest = checkNotNull(editor.undoRemoval)
        assertFalse(latest.wholeModule)
        editor.dismissUndo(whole)
        assertSame(latest, editor.undoRemoval)
        assertFalse(editor.undo(whole))
        assertTrue(editor.undo(latest))
        assertEquals(listOf(first.copy(identities = listOf(identity(2)))), modules(store))
    }

    @Test
    fun wholeUndoSurvivesUnrelatedMovementAndOrdinaryInventoryRefresh() = runBlocking {
        val removed = module("removed", listOf(identity(1), identity(2)))
        val retained = module("retained", listOf(identity(3), identity(4)))
        val store = storeWith(listOf(removed, retained))
        val editor = HomeFavoriteEditor(store)
        assertTrue(editor.removeModule(removed.id))
        val snapshot = checkNotNull(editor.undoRemoval)
        assertTrue(editor.reorderApplication(
            ApplicationOrderChange(
                moduleId = retained.id,
                identity = identity(3),
                originalOrder = retained.identities,
                boundary = 1,
            ),
        ))
        editor.reconcileAvailability(
            mapOf(
                identity(1) to FavoriteAvailability.Unknown(presentationEntry = null),
                identity(2) to FavoriteAvailability.Disabled(presentationEntry = null),
            ),
        )
        editor.observeComposition(checkNotNull(store.currentOrderedAggregate()))
        assertSame(snapshot, editor.undoRemoval)
        editor.reconcileAvailability(mapOf(identity(2) to FavoriteAvailability.ConfirmedRemoved))
        assertNull(editor.undoRemoval)
        assertFalse(editor.undo(snapshot))
        store.load()
        assertEquals(
            listOf(retained.copy(identities = listOf(identity(4), identity(3)))),
            modules(store),
        )
    }

    @Test
    fun wholeUndoInvalidatesOnAnySuccessfulAdditionButNotFailedAddition() = runBlocking {
        for (toBar in listOf(false, true)) {
            val removed = module("removed", listOf(identity(1), identity(2)))
            val target = module("target", listOf(identity(3)))
            val store = storeWith(listOf(removed, target))
            store.updateOrderedAggregate {
                it.copy(homeBottomAppBar = HomeBottomAppBarState(enabled = true))
            }
            val controlled = ControlledStore(store)
            val editor = HomeFavoriteEditor(controlled)
            assertTrue(editor.removeModule(removed.id))
            val snapshot = checkNotNull(editor.undoRemoval)
            controlled.failWrites = true
            val destination = if (toBar) HOME_BOTTOM_APP_BAR_DESTINATION_ID else target.id
            assertFalse(editor.insertExternalFavorite(identity(4), destination, 0, null, 4))
            assertSame(snapshot, editor.undoRemoval)
            controlled.failWrites = false
            assertTrue(editor.insertExternalFavorite(identity(4), destination, 0, null, 4))
            assertNull(editor.undoRemoval)
            assertFalse(editor.undo(snapshot))
            store.load()
            assertEquals(
                setOf(identity(3), identity(4)),
                store.currentOrderedAggregate()?.identities?.toSet(),
            )
        }
    }

    @Test
    fun wholeUndoObservesExternalAdditionsButPreservesUnrelatedChanges() = runBlocking {
        for (toBar in listOf(false, true)) {
            val store = storeWith(listOf(module("removed", listOf(identity(1)))))
            val editor = HomeFavoriteEditor(store)
            assertTrue(editor.removeModule("removed"))
            val snapshot = checkNotNull(editor.undoRemoval)
            val changed = checkNotNull(store.updateOrderedAggregate {
                it.copy(homeBottomAppBar = it.homeBottomAppBar.copy(enabled = true))
            })
            editor.observeComposition(changed)
            assertSame(snapshot, editor.undoRemoval)
            val added = checkNotNull(store.updateOrderedAggregate {
                if (toBar) {
                    it.copy(homeBottomAppBar = it.homeBottomAppBar.copy(
                        identities = listOf(identity(2)),
                    ))
                } else {
                    it.copy(modules = listOf(module("new", listOf(identity(2)))))
                }
            })
            editor.observeComposition(added)
            assertNull(editor.undoRemoval)
            assertFalse(editor.undo(snapshot))
            assertEquals(added, store.currentOrderedAggregate())
        }
    }

    @Test
    fun wholeUndoNeverPartiallyRestoresARefavoritedOrRecreatedModule() = runBlocking {
        for (reuseModuleId in listOf(false, true)) {
            val removed = module("removed", listOf(identity(1), identity(2)))
            val store = storeWith(listOf(removed))
            val editor = HomeFavoriteEditor(store)
            assertTrue(editor.removeModule(removed.id))
            val snapshot = checkNotNull(editor.undoRemoval)
            val replacement = if (reuseModuleId) {
                module(removed.id, listOf(identity(3)))
            } else {
                module("new", listOf(identity(2)))
            }
            val current = checkNotNull(store.updateOrderedAggregate {
                it.copy(modules = listOf(replacement))
            })
            // Undo must check the latest store state even before its Flow is observed.
            assertFalse(editor.undo(snapshot))
            assertNull(editor.undoRemoval)
            store.load()
            assertEquals(current, store.currentOrderedAggregate())
        }
    }

    @Test
    fun wholeUndoFailureAndDismissalKeepTheDurableRemovalWithoutRetry() = runBlocking {
        val removed = module("removed", listOf(identity(1), identity(2)))
        val store = storeWith(listOf(removed))
        val controlled = ControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        assertTrue(editor.removeModule(removed.id))
        val snapshot = checkNotNull(editor.undoRemoval)
        assertNull(HomeFavoriteEditor(store).undoRemoval)
        controlled.failWrites = true
        assertFalse(editor.undo(snapshot))
        assertNull(editor.undoRemoval)
        assertFalse(editor.isSaving)
        store.load()
        assertTrue(modules(store).isEmpty())
        controlled.failWrites = false
        store.updateOrderedAggregate { it.copy(modules = listOf(removed)) }
        assertTrue(editor.removeModule(removed.id))
        val next = checkNotNull(editor.undoRemoval)
        editor.dismissUndo(next)
        assertNull(editor.undoRemoval)
        assertFalse(editor.undo(next))
        store.load()
        assertTrue(modules(store).isEmpty())
    }

    @Test
    fun wholeRemovalSaveSurvivesEditExitButNotAHomeInterruption() = runBlocking {
        for (leaveHome in listOf(false, true)) {
            val store = storeWith(listOf(module("removed", listOf(identity(1), identity(2)))))
            val controlled = ControlledStore(store)
            val gate = CompletableDeferred<Unit>()
            controlled.gate = gate
            val editor = HomeFavoriteEditor(controlled)
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                editor.removeModule("removed")
            }
            assertTrue(editor.isSaving)
            assertFalse(editor.remove(identity(1)))
            if (leaveHome) editor.invalidateUndo() else editor.invalidateApplicationUndo()
            gate.complete(Unit)
            assertTrue(pending.await())
            assertEquals(!leaveHome, editor.undoRemoval?.wholeModule == true)
            assertFalse(editor.isSaving)
            store.load()
            assertTrue(modules(store).isEmpty())
        }
    }

    @Test
    fun wholeUndoRechecksAdditionAndPermanentDisappearanceWhileWaitingForTheStore() = runBlocking {
        for (addition in listOf(false, true)) {
            val store = storeWith(listOf(module("removed", listOf(identity(1), identity(2)))))
            val controlled = ControlledStore(store)
            val editor = HomeFavoriteEditor(controlled)
            assertTrue(editor.removeModule("removed"))
            val snapshot = checkNotNull(editor.undoRemoval)
            val gate = CompletableDeferred<Unit>()
            controlled.gate = gate
            val pending = async(start = CoroutineStart.UNDISPATCHED) { editor.undo(snapshot) }
            if (addition) {
                store.updateOrderedAggregate {
                    it.copy(modules = listOf(module("new", listOf(identity(3)))))
                }
            } else {
                editor.reconcileAvailability(
                    mapOf(identity(2) to FavoriteAvailability.ConfirmedRemoved),
                )
            }
            val current = store.currentOrderedAggregate()
            gate.complete(Unit)
            assertFalse(pending.await())
            assertNull(editor.undoRemoval)
            store.load()
            assertEquals(current, store.currentOrderedAggregate())
        }
    }

    @Test
    fun staleCompositionEmissionCannotDiscardTheLatestWholeSnapshot() = runBlocking {
        val store = storeWith(listOf(module("removed", listOf(identity(1), identity(2)))))
        val editor = HomeFavoriteEditor(store)
        val stale = store.currentOrderedAggregate()
        editor.observeComposition(stale)
        assertTrue(editor.removeModule("removed"))
        val snapshot = checkNotNull(editor.undoRemoval)
        editor.observeComposition(stale)
        editor.observeComposition(store.currentOrderedAggregate())
        assertSame(snapshot, editor.undoRemoval)
        assertTrue(editor.undo(snapshot))
    }

    @Test
    fun applicationRemovalReplacesWholeUndoEvenIfEditExitPreventsItsOwnSnapshot() = runBlocking {
        val store = storeWith(listOf(
            module("removed", listOf(identity(1), identity(2))),
            module("retained", listOf(identity(3))),
        ))
        val controlled = ControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        assertTrue(editor.removeModule("removed"))
        val snapshot = checkNotNull(editor.undoRemoval)
        val gate = CompletableDeferred<Unit>()
        controlled.gate = gate
        val pending = async(start = CoroutineStart.UNDISPATCHED) { editor.remove(identity(3)) }
        editor.invalidateApplicationUndo()
        assertSame(snapshot, editor.undoRemoval)
        gate.complete(Unit)
        assertTrue(pending.await())
        assertNull(editor.undoRemoval)
        assertFalse(editor.undo(snapshot))
        store.load()
        assertTrue(modules(store).isEmpty())
    }

    @Test
    fun dismissingAnOlderSnackbarDuringTheNextSaveDoesNotSuppressTheNewSnapshot() = runBlocking {
        val store = storeWith(listOf(
            module("first", listOf(identity(1))),
            module("second", listOf(identity(2))),
        ))
        val controlled = ControlledStore(store)
        val editor = HomeFavoriteEditor(controlled)
        assertTrue(editor.remove(identity(1)))
        val first = checkNotNull(editor.undoRemoval)
        val gate = CompletableDeferred<Unit>()
        controlled.gate = gate
        val pending = async(start = CoroutineStart.UNDISPATCHED) { editor.removeModule("second") }
        editor.dismissUndo(first)
        gate.complete(Unit)
        assertTrue(pending.await())
        val latest = checkNotNull(editor.undoRemoval)
        assertTrue(latest.wholeModule)
        assertEquals("second", latest.module?.id)
        editor.dismissUndo(first)
        assertSame(latest, editor.undoRemoval)
        assertTrue(editor.undo(latest))
        assertEquals(listOf(identity(2)), store.currentOrderedAggregate().identities)
    }

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
