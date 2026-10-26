package com.maxarcher.launcher

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

/** One feedback owner for both removal types; cancellation removes the superseded Snackbar. */
@Composable
internal fun HomeRemovalFeedback(
    editor: HomeFavoriteEditor,
    hostState: SnackbarHostState,
    currentBarCapacity: (HomeBottomAppBarState) -> Int? = { null },
    revalidate: suspend (FavoriteRemovalSnapshot) -> Boolean = { true },
    onRestored: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestCapacity by rememberUpdatedState(currentBarCapacity)
    val latestRevalidate by rememberUpdatedState(revalidate)
    val latestRestored by rememberUpdatedState(onRestored)
    val snapshot = editor.undoRemoval
    val removedMessage = stringResource(
        if (snapshot?.wholeModule == true) {
            R.string.home_module_removed
        } else {
            R.string.favorite_removed
        },
    )
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(snapshot) {
        if (snapshot == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = removedMessage,
            actionLabel = undoLabel,
            withDismissAction = snapshot.wholeModule,
            duration = if (snapshot.wholeModule) {
                SnackbarDuration.Indefinite
            } else {
                SnackbarDuration.Long
            },
        )
        if (result == SnackbarResult.ActionPerformed) {
            // The write outlives the keyed Snackbar effect, but not the application composition.
            scope.launch {
                val restored = editor.undo(
                    snapshot = snapshot,
                    currentBarCapacity = latestCapacity,
                    revalidate = { latestRevalidate(snapshot) },
                )
                latestRestored()
                if (!restored) {
                    Toast.makeText(context, R.string.favorite_undo_unavailable, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            editor.dismissUndo(snapshot)
        }
    }
}
