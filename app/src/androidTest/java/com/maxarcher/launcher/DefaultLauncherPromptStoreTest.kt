package com.maxarcher.launcher

import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLauncherPromptStoreTest {
    @Test
    fun missingFileResolvesToNoDismissal(): Unit = runBlocking {
        val file = temporaryPromptFile()
        val store = DefaultLauncherPromptStore(file)
        try {
            store.load()
            assertNull(store.dismissedLocalDate.value)
            assertFalse(store.isDismissedOn(date = LocalDate.parse("2026-09-20")))
        } finally {
            deletePromptFiles(file)
        }
    }

    @Test
    fun dismissPersistsAcrossReload(): Unit = runBlocking {
        val file = temporaryPromptFile()
        val store = DefaultLauncherPromptStore(file)
        val date = LocalDate.parse("2026-09-20")
        try {
            store.load()
            assertTrue(store.dismissForLocalDate(date = date))
            assertEquals(date, store.dismissedLocalDate.value)
            assertTrue(store.isDismissedOn(date = date))
            assertFalse(store.isDismissedOn(date = date.plusDays(1)))
            val reloaded = DefaultLauncherPromptStore(file)
            reloaded.load()
            assertEquals(date, reloaded.dismissedLocalDate.value)
            assertTrue(reloaded.isDismissedOn(date = date))
        } finally {
            deletePromptFiles(file)
        }
    }

    private fun temporaryPromptFile(): File = File(
        ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
        "default-launcher-prompt-${UUID.randomUUID()}.bin",
    ).also { file -> deletePromptFiles(file) }

    private fun deletePromptFiles(file: File) {
        file.delete()
        File(file.path + ".new").delete()
        File(file.path + ".bak").delete()
    }
}

class DefaultLauncherPromptSessionTest {
    @Test
    fun evaluateAllowsPromptWhenNotDefaultAndNotDismissed() {
        val session = DefaultLauncherPromptSession()
        assertTrue(
            session.evaluateForegroundEntry(isDefaultHome = false, dismissedToday = false),
        )
    }

    @Test
    fun evaluateHidesPromptWhenAlreadyDefault() {
        val session = DefaultLauncherPromptSession()
        assertFalse(
            session.evaluateForegroundEntry(isDefaultHome = true, dismissedToday = false),
        )
    }

    @Test
    fun evaluateHidesPromptWhenDismissedToday() {
        val session = DefaultLauncherPromptSession()
        assertFalse(
            session.evaluateForegroundEntry(isDefaultHome = false, dismissedToday = true),
        )
    }

    @Test
    fun laterLossOfDefaultDoesNotInsertPrompt() {
        val session = DefaultLauncherPromptSession()
        session.evaluateForegroundEntry(isDefaultHome = true, dismissedToday = false)
        assertFalse(session.isVisible(isDefaultHome = false))
    }

    @Test
    fun becomingDefaultHidesPromptWithoutChangingDismissal() {
        val session = DefaultLauncherPromptSession()
        session.evaluateForegroundEntry(isDefaultHome = false, dismissedToday = false)
        assertFalse(session.onBecameDefaultHome())
        assertFalse(session.isVisible(isDefaultHome = false))
    }

    @Test
    fun dismissHidesPromptForTheSession() {
        val session = DefaultLauncherPromptSession()
        session.evaluateForegroundEntry(isDefaultHome = false, dismissedToday = false)
        assertFalse(session.onDismiss())
        assertFalse(session.isVisible(isDefaultHome = false))
    }
}
