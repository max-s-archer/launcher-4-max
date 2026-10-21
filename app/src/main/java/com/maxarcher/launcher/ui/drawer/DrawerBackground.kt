package com.maxarcher.launcher.ui.drawer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.maxarcher.launcher.R

private val LocalDrawerForegroundShadow = staticCompositionLocalOf { false }

/**
 * The single background treatment on every device: `darkSurfaceBaseColor` composited
 * over the wallpaper at the persisted percentage — `0` presents the wallpaper clear
 * and `100` presents the solid surface. The foreground text and artwork shadow stays
 * in effect at every percentage.
 */
@Composable
internal fun DrawerBackgroundSurface(
    opacity: Int,
    content: @Composable () -> Unit,
) {
    val shadow = drawerForegroundShadow()
    val typography = MaterialTheme.typography
    val textStyle = LocalTextStyle.current
    val shadowedTypography = remember(typography, shadow) { typography.withShadow(shadow) }
    val shadowedTextStyle = remember(textStyle, shadow) { textStyle.copy(shadow = shadow) }
    MaterialTheme(typography = shadowedTypography) {
        CompositionLocalProvider(LocalTextStyle provides shadowedTextStyle) {
            CompositionLocalProvider(LocalDrawerForegroundShadow provides true) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = colorResource(R.color.launcher4max_sheet_surface).copy(
                                alpha = opacity / 100f,
                            ),
                        )
                        .testTag("drawer_background_surface"),
                ) {
                    content()
                }
            }
        }
    }
}

/** Opaque panels retain their own contrast treatment; Home never enters the Drawer provider. */
@Composable
internal fun DrawerPanelAppearance(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography
    val textStyle = LocalTextStyle.current
    val panelTypography = remember(typography) { typography.withShadow(null) }
    val panelTextStyle = remember(textStyle) { textStyle.copy(shadow = null) }
    MaterialTheme(typography = panelTypography) {
        CompositionLocalProvider(LocalTextStyle provides panelTextStyle) {
            CompositionLocalProvider(LocalDrawerForegroundShadow provides false, content = content)
        }
    }
}

/**
 * The one shared foreground shadow construction from the drawer shadow resources.
 * Every surface that renders text or artwork over the wallpaper without the drawer
 * provider must reuse this instead of re-assembling the values.
 */
@Composable
internal fun drawerForegroundShadow(): Shadow {
    val color = colorResource(R.color.drawer_foreground_shadow)
    val x = dimensionResource(R.dimen.drawer_foreground_shadow_offset_x)
    val y = dimensionResource(R.dimen.drawer_foreground_shadow_offset_y)
    val radius = dimensionResource(R.dimen.drawer_foreground_shadow_radius)
    val density = LocalDensity.current
    return remember(color, x, y, radius, density) {
        with(density) {
            Shadow(color, Offset(x.toPx(), y.toPx()), radius.toPx())
        }
    }
}

@Composable
internal fun DrawerIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    if (!LocalDrawerForegroundShadow.current) {
        Icon(painter, contentDescription, modifier, tint)
        return
    }
    Box(modifier, propagateMinConstraints = true) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = colorResource(R.color.drawer_foreground_shadow),
            modifier = Modifier.matchParentSize()
                .offset(
                    x = dimensionResource(R.dimen.drawer_foreground_shadow_offset_x),
                    y = dimensionResource(R.dimen.drawer_foreground_shadow_offset_y),
                )
                .blur(
                    radius = dimensionResource(R.dimen.drawer_foreground_shadow_radius),
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                ),
        )
        Icon(painter = painter, contentDescription = contentDescription, tint = tint)
    }
}

private fun Typography.withShadow(shadow: Shadow?) = copy(
    displayLarge = displayLarge.copy(shadow = shadow),
    displayMedium = displayMedium.copy(shadow = shadow),
    displaySmall = displaySmall.copy(shadow = shadow),
    headlineLarge = headlineLarge.copy(shadow = shadow),
    headlineMedium = headlineMedium.copy(shadow = shadow),
    headlineSmall = headlineSmall.copy(shadow = shadow),
    titleLarge = titleLarge.copy(shadow = shadow),
    titleMedium = titleMedium.copy(shadow = shadow),
    titleSmall = titleSmall.copy(shadow = shadow),
    bodyLarge = bodyLarge.copy(shadow = shadow),
    bodyMedium = bodyMedium.copy(shadow = shadow),
    bodySmall = bodySmall.copy(shadow = shadow),
    labelLarge = labelLarge.copy(shadow = shadow),
    labelMedium = labelMedium.copy(shadow = shadow),
    labelSmall = labelSmall.copy(shadow = shadow),
)
