package com.maxarcher.launcher.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.maxarcher.launcher.FavoriteAggregate
import com.maxarcher.launcher.FavoriteAvailability
import com.maxarcher.launcher.FavoriteContainer
import com.maxarcher.launcher.FavoriteReadState
import com.maxarcher.launcher.HomeApplicationMovement
import com.maxarcher.launcher.HomeEditTransaction
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.ModuleDragSession
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.R
import com.maxarcher.launcher.isValidAggregate
import com.maxarcher.launcher.updateVerticalList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Module movement, style edits, and their serialized edit transaction.
 * HomeScreen supplies the current state, localized messages, and persistence callbacks.
 */
internal class HomeFavoriteEditOrchestration(
    val orderedApplicationMovement: HomeApplicationMovement,
    val snackbarHostState: SnackbarHostState,
    val editScope: CoroutineScope,
    val context: Context,
) {
    var editMutationJob by mutableStateOf<Job?>(null)
    var moduleDragSession by mutableStateOf<ModuleDragSession?>(null)
    val moduleBoundsInWindow = mutableStateMapOf<String, Rect>()
    var moduleListBoundsInWindow by mutableStateOf(Rect.Zero)
    val editTransaction = HomeEditTransaction()

    var editMode by mutableStateOf(false)
    var applicationEditingSaving by mutableStateOf(false)
    var selectedModuleId by mutableStateOf<String?>(null)
    var favoriteState by mutableStateOf<FavoriteReadState>(FavoriteReadState.Loading)
    var favoriteAvailability by mutableStateOf<Map<LaunchableIdentity, FavoriteAvailability>>(
        emptyMap(),
    )
    var undoLabel by mutableStateOf("")
    var undoUnavailableMessage by mutableStateOf("")
    var moduleStyleSaveFailureMessage by mutableStateOf("")
    var moduleOrderSaveFailureMessage by mutableStateOf("")
    var onCommitFavoriteComposition by mutableStateOf<
        suspend ((FavoriteAggregate) -> FavoriteAggregate) -> FavoriteAggregate?
    >(
        { transform -> transform(FavoriteAggregate()) },
    )
    var onCommitModuleOrder by mutableStateOf<suspend (List<String>) -> Boolean>({ false })

    fun cancelActiveDragSessions() {
        moduleDragSession = null
        orderedApplicationMovement.cancel()
    }

    fun startModuleDrag(
        module: OrderedFavoriteModule,
        modules: List<OrderedFavoriteModule>,
        touchInWindow: Offset,
    ): Boolean {
        if (modules.size < 2 || editMutationJob?.isActive == true || applicationEditingSaving) {
            return false
        }
        val sourceIndex = modules.indexOfFirst { it.id == module.id }
        val sourceBounds = moduleBoundsInWindow[module.id] ?: return false
        if (sourceIndex < 0) return false
        moduleDragSession = ModuleDragSession(
            sourceModule = module,
            sourceSelected = module.id == selectedModuleId,
            sourceAvailability = module.identities.associateWith { identity ->
                favoriteAvailability[identity] ?: FavoriteAvailability.Unknown(null)
            },
            initialModules = modules,
            remainingModules = modules.filterNot { it.id == module.id },
            insertionIndex = sourceIndex,
            originInWindow = sourceBounds.topLeft,
            size = IntSize(
                sourceBounds.width.roundToInt(),
                sourceBounds.height.roundToInt(),
            ),
            touchStartInWindow = touchInWindow,
        )
        return true
    }

    fun advanceModuleDrag(amount: Offset) {
        val previous = moduleDragSession ?: return
        moduleDragSession = previous.advanced(
            amount = amount,
            listBoundsInWindow = moduleListBoundsInWindow,
            moduleBoundsInWindow = moduleBoundsInWindow,
        )
    }

    fun enqueueEditMutation(mutation: suspend () -> Unit) {
        val previousJob = editMutationJob
        val mutationJob = editScope.launch(start = CoroutineStart.LAZY) {
            previousJob?.join()
            mutation()
        }
        editMutationJob = mutationJob
        mutationJob.invokeOnCompletion {
            editScope.launch {
                if (editMutationJob === mutationJob) editMutationJob = null
            }
        }
        mutationJob.start()
    }

    fun finishModuleDrag() {
        val session = moduleDragSession ?: return
        moduleDragSession = null
        val reordered = session.completedModules() ?: return

        val editSession = editTransaction.sessionId
        editTransaction.beginModuleOrder(reordered)
        enqueueEditMutation moduleOrderMutation@{
            val persisted = onCommitModuleOrder(reordered.map { it.id })
            if (editSession != editTransaction.sessionId || !editMode) {
                return@moduleOrderMutation
            }
            editTransaction.completeModuleOrder()
            if (!persisted) {
                Toast.makeText(
                    context,
                    moduleOrderSaveFailureMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun commitEditAggregate(
        transform: (FavoriteAggregate) -> FavoriteAggregate,
        message: String = "",
        recordUndo: Boolean = false,
        onCommitted: () -> Unit = {},
        onFailed: () -> Unit = {
            Toast.makeText(
                context,
                R.string.favorite_reorder_unavailable,
                Toast.LENGTH_SHORT,
            ).show()
        },
    ) {
        if (favoriteState !is FavoriteReadState.Readable) return
        val session = editTransaction.sessionId
        enqueueEditMutation mutation@{
            val base = editTransaction.baseAggregate(
                (favoriteState as? FavoriteReadState.Readable)?.aggregate,
            )
                ?: return@mutation
            val updated = transform(base)
            if (!isValidAggregate(updated)) return@mutation
            editTransaction.beginMutation(updated)
            val persisted = onCommitFavoriteComposition(transform)
            if (session != editTransaction.sessionId || !editMode) return@mutation
            if (persisted == null) {
                editTransaction.discardPending(updated)
                withFrameNanos { }
                if (session != editTransaction.sessionId || !editMode) return@mutation
                editTransaction.restoreCommitted(
                    (favoriteState as? FavoriteReadState.Readable)?.aggregate,
                )
                cancelActiveDragSessions()
                onFailed()
                return@mutation
            }
            editTransaction.completeMutation(persisted)
            onCommitted()
            if (!recordUndo) {
                editTransaction.clearUndo()
                snackbarHostState.currentSnackbarData?.dismiss()
                return@mutation
            }
            val sequence = editTransaction.recordUndo(base)
            snackbarHostState.currentSnackbarData?.dismiss()
            editScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed &&
                    session == editTransaction.sessionId
                ) {
                    val snapshot = editTransaction.consumeUndo(sequence) ?: return@launch
                    commitEditAggregate(
                        transform = { snapshot },
                        onFailed = {
                            editScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = undoUnavailableMessage,
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    fun commitVerticalModuleStyle(
        moduleId: String,
        transform: (FavoriteContainer) -> FavoriteContainer,
    ) {
        if (editMutationJob?.isActive == true || applicationEditingSaving) return
        commitEditAggregate(
            transform = { aggregate ->
                aggregate.updateVerticalList(moduleId, transform)
            },
            onFailed = {
                Toast.makeText(
                    context,
                    moduleStyleSaveFailureMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
