package com.maxarcher.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

@Composable
internal fun Launcher4MaxTheme(content: @Composable () -> Unit) {
    val transparent = colorResource(R.color.launcher4max_transparent)
    val foreground = colorResource(R.color.launcher4max_foreground)
    val secondaryForeground = colorResource(R.color.launcher4max_secondary_foreground)
    val sheetSurface = colorResource(R.color.launcher4max_sheet_surface)
    val sheetScrim = colorResource(R.color.launcher4max_sheet_scrim)
    val error = colorResource(R.color.launcher4max_error)
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = transparent,
            error = error,
            surface = sheetSurface,
            onBackground = foreground,
            onSurface = foreground,
            onSurfaceVariant = secondaryForeground,
            scrim = sheetScrim,
        ),
        content = content,
    )
}
