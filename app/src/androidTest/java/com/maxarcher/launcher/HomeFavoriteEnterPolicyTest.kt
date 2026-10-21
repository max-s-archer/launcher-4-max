package com.maxarcher.launcher

import android.content.ComponentName
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterBatch
import com.maxarcher.launcher.ui.home.components.HomeFavoriteEnterKey
import com.maxarcher.launcher.ui.home.components.homeFavoriteEnterKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HomeFavoriteEnterPolicyTest {
    private val a = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.a", "Main"))
    private val b = LaunchableIdentity(profileSerialNumber = 1, componentName = ComponentName("test.b", "Main"))

    @Test
    fun initialReadAndUnchangedMembershipDoNotEnter() {
        val current = mapOf("one" to listOf(element = a))
        assertEquals(emptySet<HomeFavoriteEnterKey>(), homeFavoriteEnterKeys(previous = null, current = current))
        assertEquals(emptySet<HomeFavoriteEnterKey>(), homeFavoriteEnterKeys(previous = current, current = current))
    }

    @Test
    fun addingOrUndoingAnApplicationFadesOnlyThatIdentity() {
        assertEquals(
            setOf(element = HomeFavoriteEnterKey(moduleId = "one", identity = b)),
            homeFavoriteEnterKeys(previous = mapOf("one" to listOf(element = a)), current = mapOf("one" to listOf(a, b))),
        )
    }

    @Test
    fun creatingOrRestoringAModuleFadesOnlyTheModule() {
        assertEquals(
            setOf(element = HomeFavoriteEnterKey(moduleId = "one")),
            homeFavoriteEnterKeys(previous = emptyMap(), current = mapOf("one" to listOf(a, b))),
        )
    }

    @Test
    fun reorderingCrossModuleMovementAndDropToCreateDoNotEnter() {
        val previous = mapOf("one" to listOf(a, b))
        listOf(
            mapOf("one" to listOf(b, a)),
            mapOf("one" to listOf(element = a), "new" to listOf(element = b)),
            mapOf("new" to listOf(a, b)),
        ).forEach(action = { current ->
            assertEquals(emptySet<HomeFavoriteEnterKey>(), homeFavoriteEnterKeys(previous = previous, current = current))
        })
    }

    @Test
    fun reverseMembershipSelectsOneRemovalLevelAndExcludesMovedIdentities() {
        val before = mapOf("one" to listOf(a, b))
        assertEquals(
            setOf(element = HomeFavoriteEnterKey(moduleId = "one", identity = b)),
            homeFavoriteEnterKeys(previous = mapOf("one" to listOf(element = a)), current = before),
        )
        assertEquals(
            setOf(element = HomeFavoriteEnterKey(moduleId = "one")),
            homeFavoriteEnterKeys(previous = emptyMap(), current = before),
        )
        assertEquals(
            emptySet<HomeFavoriteEnterKey>(),
            homeFavoriteEnterKeys(previous = mapOf("destination" to listOf(a, b)), current = before),
        )
    }

    @Test
    fun aTicketCannotReplayAndExpiredAdditionsCannotFadeOnScroll() {
        val key = HomeFavoriteEnterKey(moduleId = "one", identity = a)
        val batch = HomeFavoriteEnterBatch(keys = setOf(element = key), moduleFades = mutableMapOf())
        val ticket = checkNotNull(value = batch.claim(key = key))
        batch.attach(ticket = ticket)
        assertNull(batch.claim(key = key))
        val expired = HomeFavoriteEnterBatch(keys = setOf(element = key), moduleFades = mutableMapOf())
        expired.close()
        assertNull(expired.claim(key = key))
    }

    @Test
    fun aNewMutationDoesNotAddAChildFadeWhileItsParentIsStillEntering() {
        val active = mutableMapOf<String, Any>()
        val parentKey = HomeFavoriteEnterKey(moduleId = "one")
        val parentBatch = HomeFavoriteEnterBatch(keys = setOf(element = parentKey), moduleFades = active)
        val parent = checkNotNull(value = parentBatch.claim(key = parentKey))
        parentBatch.attach(ticket = parent)
        parentBatch.begin(ticket = parent)
        val childKey = HomeFavoriteEnterKey(moduleId = "one", identity = b)
        val childBatch = HomeFavoriteEnterBatch(keys = setOf(element = childKey), moduleFades = active)
        assertNull(childBatch.claim(key = childKey))
        parentBatch.finish(ticket = parent)
        val later = HomeFavoriteEnterBatch(keys = setOf(element = childKey), moduleFades = active)
        assertNotNull(later.claim(key = childKey))
    }
}
