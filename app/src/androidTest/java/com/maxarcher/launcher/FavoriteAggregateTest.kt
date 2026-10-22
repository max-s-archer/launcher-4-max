package com.maxarcher.launcher

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteAggregateTest {
    @Test
    fun invalidAggregateRejectsDuplicateIdentityAndEmptyContainer() {
        val identity = identity(1, "com.example", "Main")
        val first = FavoriteContainer(
            id = "vertical-list-1",
            type = FavoriteContainerType.VerticalList,
            identities = listOf(identity),
        )
        assertFalse(
            isValidAggregate(
                FavoriteAggregate(
                    verticalLists = listOf(first, first.copy(id = "vertical-list-2")),
                ),
            ),
        )
        assertFalse(
            isValidAggregate(
                FavoriteAggregate(
                    favoriteBars = listOf(
                        FavoriteContainer(
                            id = "favorite-bar-1",
                            type = FavoriteContainerType.FavoriteBar,
                            identities = emptyList(),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun invalidAggregateRejectsContainersInTheWrongGroup() {
        val identity = identity(1, "com.example", "Main")
        assertFalse(
            isValidAggregate(
                FavoriteAggregate(
                    verticalLists = listOf(
                        FavoriteContainer(
                            id = "favorite-bar-1",
                            type = FavoriteContainerType.FavoriteBar,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(
            isValidAggregate(
                FavoriteAggregate(
                    favoriteBars = listOf(
                        FavoriteContainer(
                            id = PRIMARY_LIST_ID,
                            type = FavoriteContainerType.VerticalList,
                            identities = listOf(identity),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun removingPrimaryListDoesNotPromoteCompanion() {
        val primary = identity(1, "com.example.primary", "Main")
        val companion = identity(2, "com.example.companion", "Main")
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(
                FavoriteContainer(
                    id = PRIMARY_LIST_ID,
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(primary),
                ),
                FavoriteContainer(
                    id = "vertical-list-2",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(companion),
                ),
            ),
        )

        val readable = FavoriteReadState.Readable(aggregate.removeIdentity(primary))

        assertTrue(readable.primaryIdentities.isEmpty())
        assertEquals(listOf(companion), readable.companionIdentities)
        assertEquals("vertical-list-2", readable.aggregate.verticalLists.single().id)
    }

    @Test
    fun moveFavoriteSupportsEveryContainerTypePair() {
        val moved = identity(1, "com.example.moved", "Main")
        val sourceTail = identity(1, "com.example.source.tail", "Main")
        val targetFirst = identity(1, "com.example.target.first", "Main")
        val targetLast = identity(1, "com.example.target.last", "Main")

        fun verifyMove(
            sourceType: FavoriteContainerType,
            targetType: FavoriteContainerType,
        ) {
            val source = FavoriteContainer(
                id = "source",
                type = sourceType,
                identities = listOf(moved, sourceTail),
            )
            val target = FavoriteContainer(
                id = "target",
                type = targetType,
                identities = listOf(targetFirst, targetLast),
            )
            val aggregate = FavoriteAggregate(
                verticalLists = listOf(source, target)
                    .filter { it.type == FavoriteContainerType.VerticalList },
                favoriteBars = listOf(source, target)
                    .filter { it.type == FavoriteContainerType.FavoriteBar },
            )

            val updated = aggregate.moveFavorite(
                sourceContainerId = source.id,
                targetContainerId = target.id,
                identity = moved,
                targetIndex = 1,
                exchangeIdentity = null,
            )

            assertEquals(listOf(sourceTail), updated.container(source.id)?.identities)
            assertEquals(
                listOf(targetFirst, moved, targetLast),
                updated.container(target.id)?.identities,
            )
            assertTrue(isValidAggregate(updated))
        }

        verifyMove(FavoriteContainerType.VerticalList, FavoriteContainerType.VerticalList)
        verifyMove(FavoriteContainerType.VerticalList, FavoriteContainerType.FavoriteBar)
        verifyMove(FavoriteContainerType.FavoriteBar, FavoriteContainerType.VerticalList)
        verifyMove(FavoriteContainerType.FavoriteBar, FavoriteContainerType.FavoriteBar)
    }

    @Test
    fun moveFavoriteExchangePreservesBothContainerSlotsAndIdentityUniqueness() {
        val moved = identity(1, "com.example.moved", "Main")
        val sourceTail = identity(1, "com.example.source.tail", "Main")
        val exchanged = identity(1, "com.example.exchanged", "Main")
        val targetTail = identity(1, "com.example.target.tail", "Main")
        val source = FavoriteContainer(
            id = "source-list",
            type = FavoriteContainerType.VerticalList,
            identities = listOf(moved, sourceTail),
        )
        val target = FavoriteContainer(
            id = "target-bar",
            type = FavoriteContainerType.FavoriteBar,
            identities = listOf(exchanged, targetTail),
        )
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(source),
            favoriteBars = listOf(target),
        )

        val updated = aggregate.moveFavorite(
            sourceContainerId = source.id,
            targetContainerId = target.id,
            identity = moved,
            targetIndex = null,
            exchangeIdentity = exchanged,
        )

        assertEquals(listOf(exchanged, sourceTail), updated.verticalLists.single().identities)
        assertEquals(listOf(moved, targetTail), updated.favoriteBars.single().identities)
        assertEquals(aggregate.identities.toSet(), updated.identities.toSet())
        assertTrue(isValidAggregate(updated))
    }

    @Test
    fun moveFavoriteDeletesAnEmptySourceAndIgnoresInvalidMoves() {
        val moved = identity(1, "com.example.moved", "Main")
        val targetEntry = identity(1, "com.example.target", "Main")
        val aggregate = FavoriteAggregate(
            verticalLists = listOf(
                FavoriteContainer(
                    id = "source-list",
                    type = FavoriteContainerType.VerticalList,
                    identities = listOf(moved),
                ),
            ),
            favoriteBars = listOf(
                FavoriteContainer(
                    id = "target-bar",
                    type = FavoriteContainerType.FavoriteBar,
                    identities = listOf(targetEntry),
                ),
            ),
        )

        val updated = aggregate.moveFavorite(
            sourceContainerId = "source-list",
            targetContainerId = "target-bar",
            identity = moved,
            targetIndex = 0,
            exchangeIdentity = null,
        )

        assertTrue(updated.verticalLists.isEmpty())
        assertEquals(listOf(moved, targetEntry), updated.favoriteBars.single().identities)
        assertEquals(
            aggregate,
            aggregate.moveFavorite(
                sourceContainerId = "missing",
                targetContainerId = "target-bar",
                identity = moved,
                targetIndex = 0,
                exchangeIdentity = null,
            ),
        )
    }

    private fun FavoriteAggregate.container(id: String): FavoriteContainer? =
        (verticalLists + favoriteBars).firstOrNull { it.id == id }

    private fun identity(serial: Long, packageName: String, className: String) =
        LaunchableIdentity(serial, ComponentName(packageName, className))
}
