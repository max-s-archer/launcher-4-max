package com.maxarcher.launcher

import android.content.Context
import android.util.AtomicFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Session-scoped visibility for the Home default-launcher prompt. Foreground evaluation
 * is sticky: losing the default-launcher role later in the same session does not insert
 * the prompt. Becoming the default launcher hides it immediately and records no dismissal.
 */
internal class DefaultLauncherPromptSession {
    private var sessionAllowsPrompt = false

    fun evaluateForegroundEntry(
        isDefaultHome: Boolean,
        dismissedToday: Boolean,
    ): Boolean {
        sessionAllowsPrompt = !isDefaultHome && !dismissedToday
        return isVisible(isDefaultHome = isDefaultHome)
    }

    fun onBecameDefaultHome(): Boolean {
        sessionAllowsPrompt = false
        return false
    }

    fun onDismiss(): Boolean {
        sessionAllowsPrompt = false
        return false
    }

    fun isVisible(isDefaultHome: Boolean): Boolean =
        sessionAllowsPrompt && !isDefaultHome
}

internal class DefaultLauncherPromptStore internal constructor(
    private val atomicFile: AtomicFile,
) {
    constructor(context: Context) : this(
        atomicFile = AtomicFile(context.filesDir.resolve(FILE_NAME)),
    )

    internal constructor(file: File) : this(
        atomicFile = AtomicFile(file),
    )

    private val mutationMutex = fileMutexes.getOrPut(
        atomicFile.baseFile.absoluteFile.normalize().path,
    ) {
        Mutex()
    }
    private val mutableState = MutableStateFlow<LocalDate?>(null)

    val dismissedLocalDate: StateFlow<LocalDate?> = mutableState

    suspend fun load(): Unit = mutationMutex.withLock {
        mutableState.value = withContext(context = Dispatchers.IO) {
            if (!atomicFile.hasReadableSource()) {
                null
            } else {
                try {
                    readDismissedLocalDate()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    fun loadBlocking(): Unit = runBlocking { load() }

    suspend fun dismissForLocalDate(date: LocalDate): Boolean = mutationMutex.withLock {
        if (mutableState.value == date) return true
        val succeeded = try {
            withContext(context = Dispatchers.IO) {
                writeDismissedLocalDate(date = date)
                mutableState.value = date
                true
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        succeeded
    }

    fun isDismissedOn(date: LocalDate): Boolean = mutableState.value == date

    private fun readDismissedLocalDate(): LocalDate? =
        DataInputStream(BufferedInputStream(atomicFile.openRead())).use { input ->
            require(input.readInt() == MAGIC) {
                "Unrecognized default-launcher-prompt document"
            }
            val schemaVersion = input.readInt()
            require(schemaVersion in MIN_READABLE_SCHEMA_VERSION..SCHEMA_VERSION) {
                "Unsupported default-launcher-prompt schema"
            }
            val fieldCount = input.readInt()
            require(fieldCount in 0..MAX_FIELD_COUNT) {
                "Invalid default-launcher-prompt field count"
            }
            val fields = buildMap {
                repeat(times = fieldCount) {
                    val key = input.readUTF()
                    val value = input.readUTF()
                    require(put(key, value) == null) {
                        "Duplicate default-launcher-prompt field"
                    }
                }
            }
            require(input.read() == -1) { "Unexpected default-launcher-prompt data" }
            fields[FIELD_DISMISSED_LOCAL_DATE]?.let(LocalDate::parse)
        }

    private fun writeDismissedLocalDate(date: LocalDate) {
        var outputStream: FileOutputStream? = atomicFile.startWrite()
        try {
            val output = DataOutputStream(BufferedOutputStream(checkNotNull(outputStream)))
            output.writeInt(MAGIC)
            output.writeInt(SCHEMA_VERSION)
            output.writeInt(KNOWN_FIELDS.size)
            output.writeUTF(FIELD_DISMISSED_LOCAL_DATE)
            output.writeUTF(date.toString())
            output.flush()
            atomicFile.finishWrite(outputStream)
            outputStream = null
        } catch (exception: Exception) {
            outputStream?.let(atomicFile::failWrite)
            throw exception
        }
    }

    private fun AtomicFile.hasReadableSource(): Boolean =
        baseFile.exists() || File(baseFile.path + BACKUP_SUFFIX).exists()

    private companion object {
        val fileMutexes = ConcurrentHashMap<String, Mutex>()
        const val FILE_NAME = "default-launcher-prompt.bin"
        const val BACKUP_SUFFIX = ".bak"
        const val MAGIC = 0x444C5031
        const val MIN_READABLE_SCHEMA_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAX_FIELD_COUNT = 64
        const val FIELD_DISMISSED_LOCAL_DATE = "dismissed_local_date"
        val KNOWN_FIELDS = setOf(FIELD_DISMISSED_LOCAL_DATE)
    }
}
