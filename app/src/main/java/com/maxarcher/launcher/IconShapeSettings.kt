package com.maxarcher.launcher

import android.content.Context
import android.util.AtomicFile
import com.maxarcher.launcher.platform.icons.LauncherIconAppearance
import com.maxarcher.launcher.platform.icons.LauncherIconShape
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

internal enum class IconShapeClipMode(
    val storageValue: String,
) {
    Oem("oem"),
    Freeform("freeform"),
}

internal data class IconShapeSettings(
    val mode: IconShapeClipMode = IconShapeClipMode.Oem,
    val freeformClip: Int = DEFAULT_FREEFORM_CLIP,
) {
    init {
        require(freeformClip in FREEFORM_CLIP_RANGE)
    }

    companion object {
        const val DEFAULT_FREEFORM_CLIP = 50
        val FREEFORM_CLIP_RANGE = 0..100
    }
}

internal sealed interface IconShapeSettingsReadState {
    data object Loading : IconShapeSettingsReadState

    data class Readable(
        val settings: IconShapeSettings,
    ) : IconShapeSettingsReadState

    data object ReadFailure : IconShapeSettingsReadState
}

internal fun IconShapeSettings.iconAppearance(): LauncherIconAppearance =
    LauncherIconAppearance(
        shape = when (mode) {
            IconShapeClipMode.Oem -> LauncherIconShape.SystemAdaptive
            IconShapeClipMode.Freeform -> LauncherIconShape.Freeform(freeformClip)
        },
    )

internal class IconShapeSettingsStore internal constructor(
    private val atomicFile: AtomicFile,
) : BackupIconShapeAccess {
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
    private val mutableState = MutableStateFlow<IconShapeSettingsReadState>(
        IconShapeSettingsReadState.Loading,
    )

    val state: StateFlow<IconShapeSettingsReadState> = mutableState

    @Volatile
    var lastSuccessfulSettings: IconShapeSettings = IconShapeSettings()
        private set

    suspend fun load(): Unit = mutationMutex.withLock {
        mutableState.value = IconShapeSettingsReadState.Loading
        val result = withContext(Dispatchers.IO) {
            if (!atomicFile.hasReadableSource()) {
                IconShapeSettingsReadState.Readable(IconShapeSettings())
            } else {
                try {
                    IconShapeSettingsReadState.Readable(readDocument())
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    IconShapeSettingsReadState.ReadFailure
                }
            }
        }
        if (result is IconShapeSettingsReadState.Readable) {
            lastSuccessfulSettings = result.settings
        }
        mutableState.value = result
    }

    fun loadBlocking(): Unit = runBlocking { load() }

    override fun currentIconShapeSettings(): IconShapeSettings? =
        (mutableState.value as? IconShapeSettingsReadState.Readable)?.settings

    override suspend fun restoreIconShapeSettings(settings: IconShapeSettings): Boolean =
        replace(settings)

    suspend fun replace(settings: IconShapeSettings): Boolean = mutationMutex.withLock {
        val current = (mutableState.value as? IconShapeSettingsReadState.Readable)?.settings
            ?: return false
        if (current == settings) return true

        try {
            withContext(Dispatchers.IO) {
                writeDocument(settings = settings)
                lastSuccessfulSettings = settings
                mutableState.value = IconShapeSettingsReadState.Readable(settings)
            }
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
    }

    private fun readDocument(): IconShapeSettings =
        DataInputStream(BufferedInputStream(atomicFile.openRead())).use { input ->
            require(input.readInt() == MAGIC) {
                "Unrecognized icon-shape-settings document"
            }
            require(input.readInt() in MIN_READABLE_SCHEMA_VERSION..SCHEMA_VERSION) {
                "Unsupported icon-shape-settings schema"
            }
            val fieldCount = input.readInt()
            require(fieldCount in 0..MAX_FIELD_COUNT) {
                "Invalid icon-shape-settings field count"
            }
            val fields = buildMap {
                repeat(fieldCount) {
                    val key = input.readUTF()
                    val value = input.readUTF()
                    require(put(key, value) == null) {
                        "Duplicate icon-shape-settings field"
                    }
                }
            }
            require(input.read() == -1) {
                "Unexpected icon-shape-settings data"
            }

            val mode = fields[FIELD_MODE]?.let(::modeFromStorageValue)
                ?: IconShapeClipMode.Oem
            val freeformClip = fields[FIELD_FREEFORM_CLIP]?.toIntOrNull()
                ?.takeIf { it in IconShapeSettings.FREEFORM_CLIP_RANGE }
                ?: IconShapeSettings.DEFAULT_FREEFORM_CLIP
            IconShapeSettings(mode = mode, freeformClip = freeformClip)
        }

    private fun writeDocument(settings: IconShapeSettings) {
        var outputStream: FileOutputStream? = atomicFile.startWrite()
        try {
            val output = DataOutputStream(BufferedOutputStream(checkNotNull(outputStream)))
            output.writeInt(MAGIC)
            output.writeInt(SCHEMA_VERSION)
            output.writeInt(KNOWN_FIELDS.size)
            output.writeField(FIELD_MODE, settings.mode.storageValue)
            output.writeField(FIELD_FREEFORM_CLIP, settings.freeformClip.toString())
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

    private fun modeFromStorageValue(value: String): IconShapeClipMode =
        IconShapeClipMode.entries.firstOrNull { it.storageValue == value }
            ?: IconShapeClipMode.Oem

    private fun AtomicFile.hasReadableSource(): Boolean =
        baseFile.exists() || File(baseFile.path + BACKUP_SUFFIX).exists()

    private companion object {
        val fileMutexes = ConcurrentHashMap<String, Mutex>()
        const val FILE_NAME = "icon-shape-settings.bin"
        const val BACKUP_SUFFIX = ".bak"
        const val MAGIC = 0x49534831
        const val MIN_READABLE_SCHEMA_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAX_FIELD_COUNT = 16
        const val FIELD_MODE = "mode"
        const val FIELD_FREEFORM_CLIP = "freeform_clip"
        val KNOWN_FIELDS = setOf(FIELD_MODE, FIELD_FREEFORM_CLIP)
    }
}
