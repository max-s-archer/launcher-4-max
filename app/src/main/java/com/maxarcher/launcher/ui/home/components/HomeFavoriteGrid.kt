package com.maxarcher.launcher.ui.home.components

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.integerResource
import com.maxarcher.launcher.R

/**
 * Keep item identity across row boundaries. Row-local composition would dispose/recreate an
 * application when a preceding cell is removed, losing both its placement animation and state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> HomeFavoriteGrid(
    items: List<T>,
    columns: Int,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    require(value = columns > 0)
    val duration = integerResource(id = R.integer.short_property_animation_duration_ms)
    // A module-local coordinate space excludes main-list scrolling and module translation.
    // Only the real cells move: no retained placeholder, outgoing hit target, or entry fade.
    LookaheadScope(content = {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = duration)),
            maxItemsInEachRow = columns,
            content = {
                items.forEach(action = { item ->
                    key(itemKey(item)) {
                        Box(
                            modifier = Modifier
                                .animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    boundsTransform = { _, _ -> tween(durationMillis = duration) },
                                    animateMotionFrameOfReference = false,
                                )
                                .weight(weight = 1f),
                            content = { content(item) },
                        )
                    }
                })
                // Preserve fixed column widths in the final partial row, including the add cell.
                repeat(times = (columns - items.size % columns) % columns, action = {
                    Spacer(modifier = Modifier.weight(weight = 1f))
                })
            },
        )
    })
}
