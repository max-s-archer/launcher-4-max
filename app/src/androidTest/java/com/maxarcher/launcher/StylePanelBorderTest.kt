package com.maxarcher.launcher

import android.content.Context
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class StylePanelBorderTest {
    @Test
    fun resourcesCarryTheAcceptedDeliveryValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = Density(context.resources.displayMetrics.density)
        assertEquals(
            with(density) { 1.dp.roundToPx() },
            context.resources.getDimensionPixelSize(R.dimen.style_settings_panel_border_width),
        )
        assertEquals(
            0xFF5A5A60.toInt(),
            ContextCompat.getColor(context, R.color.style_settings_panel_border),
        )
    }
}
