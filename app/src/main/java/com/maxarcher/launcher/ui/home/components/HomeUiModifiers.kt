package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.maxarcher.launcher.R

@Composable
internal fun Modifier.homeEditSurface(enabled: Boolean): Modifier = if (!enabled) {
    this
} else {
    background(
        colorResource(id = R.color.home_edit_surface),
        shape = RoundedCornerShape(size = dimensionResource(R.dimen.home_edit_surface_radius)),
    )
}
