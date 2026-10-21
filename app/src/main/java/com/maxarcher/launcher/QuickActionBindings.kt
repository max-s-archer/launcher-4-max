package com.maxarcher.launcher

import android.content.Context
import android.util.AtomicFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class QuickAction {
    NoAction,
    EditMode,
    ScreenLock,
}

internal enum class QuickActionSlot {
    DoubleTap,
    LongPress,
}

internal data class QuickActionBindings(
    val doubleTap: QuickAction = QuickAction.NoAction,
    val longPress: QuickAction = QuickAction.NoAction,
) {
    fun actionFor(slot: QuickActionSlot): QuickAction = when (slot) {
        QuickActionSlot.DoubleTap -> doubleTap
        QuickActionSlot.LongPress -> longPress
    }

    fun withSlot(slot: QuickActionSlot, action: QuickAction): QuickActionBindings = when (slot) {
        QuickActionSlot.DoubleTap -> copy(doubleTap = action)
        QuickActionSlot.LongPress -> copy(longPress = action)
    }
}

internal interface BackupBindingsAccess {
    fun currentBindings(): QuickActionBindings?

    suspend fun restoreBindings(bindings: QuickActionBindings): Boolean
}

internal sealed interface QuickActionBindingsReadState {
    data object Loading : QuickActionBindingsReadState

    data class Readable(
        val bindings: QuickActionBindings,
    ) : QuickActionBindingsReadState

    data object ReadFailure : QuickActionBindingsReadState
}

internal class QuickActionBindingsStore internal constructor(
    private val atomicFile: AtomicFile,
) : BackupBindingsAccess {
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
    private val mutableState = MutableStateFlow<QuickActionBindingsReadState>(
        QuickActionBindingsReadState.Loading,
    )

    val state: StateFlow<QuickActionBindingsReadState> = mutableState

    suspend fun load(): Unit = mutationMutex.withLock {
        mutableState.value = QuickActionBindingsReadState.Loading
        mutableState.value = withContext(context = Dispatchers.IO) {
            if (!atomicFile.hasReadableSource()) {
                QuickActionBindingsReadState.Readable(bindings = QuickActionBindings())
            } else {
                try {
                    val document = readDocument()
                    if (document.fields.keys.containsAll(KNOWN_FIELDS).not()) {
                        try {
                            writeDocument(bindings = document.bindings)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            // The readable adopted state remains usable.
                        }
                    }
                    QuickActionBindingsReadState.Readable(bindings = document.bindings)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    QuickActionBindingsReadState.ReadFailure
                }
            }
        }
    }

    fun loadBlocking(): Unit = runBlocking { load() }

    override fun currentBindings(): QuickActionBindings? =
        (mutableState.value as? QuickActionBindingsReadState.Readable)?.bindings

    override suspend fun restoreBindings(bindings: QuickActionBindings): Boolean =
        replace(bindings = bindings)

    suspend fun replace(bindings: QuickActionBindings): Boolean = mutationMutex.withLock {
        val current = (mutableState.value as? QuickActionBindingsReadState.Readable)
            ?.bindings
            ?: return false
        if (current == bindings) return true

        val succeeded = try {
            withContext(context = Dispatchers.IO) {
                writeDocument(bindings = bindings)
                mutableState.value = QuickActionBindingsReadState.Readable(
                    bindings = bindings,
                )
                true
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        succeeded
    }

    private data class QuickActionBindingsDocument(
        val bindings: QuickActionBindings,
        val fields: Map<String, String>,
    )

    private fun readDocument(): QuickActionBindingsDocument =
        DataInputStream(BufferedInputStream(atomicFile.openRead())).use { input ->
            require(input.readInt() == MAGIC) {
                "Unrecognized quick-action-bindings document"
            }
            val schemaVersion = input.readInt()
            require(schemaVersion in MIN_READABLE_SCHEMA_VERSION..SCHEMA_VERSION) {
                "Unsupported quick-action-bindings schema"
            }
            val fieldCount = input.readInt()
            require(fieldCount in 0..MAX_FIELD_COUNT) {
                "Invalid quick-action-bindings field count"
            }
            val fields = buildMap {
                repeat(times = fieldCount) {
                    val key = input.readUTF()
                    val value = input.readUTF()
                    require(put(key, value) == null) {
                        "Duplicate quick-action-bindings field"
                    }
                }
            }
            require(input.read() == -1) { "Unexpected quick-action-bindings data" }

            val defaults = QuickActionBindings()
            QuickActionBindingsDocument(
                bindings = QuickActionBindings(
                    doubleTap = fields[FIELD_DOUBLE_TAP]?.let(::quickActionFromStorageValue)
                        ?: defaults.doubleTap,
                    longPress = fields[FIELD_LONG_PRESS]?.let(::quickActionFromStorageValue)
                        ?: defaults.longPress,
                ),
                fields = fields,
            )
        }

    private fun writeDocument(bindings: QuickActionBindings) {
        var outputStream: FileOutputStream? = atomicFile.startWrite()
        try {
            val output = DataOutputStream(BufferedOutputStream(checkNotNull(outputStream)))
            output.writeInt(MAGIC)
            output.writeInt(SCHEMA_VERSION)
            output.writeInt(KNOWN_FIELDS.size)
            output.writeField(FIELD_DOUBLE_TAP, bindings.doubleTap.storageValue)
            output.writeField(FIELD_LONG_PRESS, bindings.longPress.storageValue)
            output.flush()
            atomicFile.finishWrite(outputStream)
            outputStream = null
        } catch (exception: Exception) {
            outputStream?.let(atomicFile::failWrite)
            throw exception
        }
    }

    private fun DataOutputStream.writeField(key: String, value: String) {
        writeUTF(key)
        writeUTF(value)
    }

    private fun AtomicFile.hasReadableSource(): Boolean =
        baseFile.exists() || File(baseFile.path + BACKUP_SUFFIX).exists()

    private companion object {
        val fileMutexes = ConcurrentHashMap<String, Mutex>()
        const val FILE_NAME = "quick-action-bindings.bin"
        const val BACKUP_SUFFIX = ".bak"
        const val MAGIC = 0x51414231
        const val MIN_READABLE_SCHEMA_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAX_FIELD_COUNT = 64
        const val FIELD_DOUBLE_TAP = "double_tap"
        const val FIELD_LONG_PRESS = "long_press"
        val KNOWN_FIELDS = setOf(FIELD_DOUBLE_TAP, FIELD_LONG_PRESS)
    }
}

internal val QuickAction.storageValue: String
    get() = when (this) {
        QuickAction.NoAction -> "no_action"
        QuickAction.EditMode -> "edit_mode"
        QuickAction.ScreenLock -> "screen_lock"
    }

internal fun quickActionFromStorageValue(value: String): QuickAction = when (value) {
    "no_action" -> QuickAction.NoAction
    "edit_mode" -> QuickAction.EditMode
    "screen_lock" -> QuickAction.ScreenLock
    else -> throw IllegalArgumentException("Invalid quick action")
}
