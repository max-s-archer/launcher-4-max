package com.maxarcher.launcher.ui.drawer

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.R

internal fun drawerPinnedAnchorTop(
    sectionTop: Int,
    sectionBottom: Int,
    anchorHeight: Int,
    topInset: Int,
): Int = minOf(maxOf(sectionTop, 0) + topInset, sectionBottom - anchorHeight)

private data class VisibleDrawerSection(
    val range: DrawerSectionRange,
    val top: Int,
    val bottom: Int,
)

/** Read-only gutter; the application rows stay lazy and own scrolling and input. */
@SuppressLint("FrequentlyChangingValue")
@Composable
internal fun DrawerLeftSectionAnchors(
    ranges: List<DrawerSectionRange>,
    listState: LazyListState,
) {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo.filter {
        it.offset + it.size > layoutInfo.viewportStartOffset &&
            it.offset < layoutInfo.viewportEndOffset
    }
    val visibleSections = ranges.mapNotNull { range ->
        if (visibleItems.none { it.index in range.startIndex until range.endIndex }) {
            null
        } else {
            VisibleDrawerSection(
                range = range,
                // If the first row has left the viewport, this section is already pinned.
                top = visibleItems.firstOrNull { it.index == range.startIndex }?.offset ?: 0,
                // Until the last row is visible, its bottom cannot push the anchor away.
                bottom = visibleItems.firstOrNull { it.index == range.endIndex - 1 }
                    ?.let { it.offset + it.size } ?: Int.MAX_VALUE,
            )
        }
    }
    val topInset = dimensionResource(R.dimen.drawer_section_anchor_top_inset)
    Layout(
        modifier = Modifier
            .padding(start = dimensionResource(R.dimen.drawer_application_grid_boundary))
            .width(dimensionResource(R.dimen.drawer_section_anchor_column_width))
            .fillMaxHeight()
            .clipToBounds()
            .testTag("drawer_left_section_anchors"),
        content = {
            visibleSections.forEach { section ->
                Box(
                    modifier = Modifier.fillMaxWidth().testTag(
                        if (section.range.isSettings) "drawer_settings_anchor"
                        else "drawer_section_${section.range.label}",
                    ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (section.range.isSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.drawer_settings_anchor_icon_size)),
                        )
                    } else {
                        Text(
                            text = section.range.label,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = dimensionResource(R.dimen.shared_large_app_name_text_size).value.sp,
                                lineHeight = dimensionResource(R.dimen.shared_large_app_name_line_height).value.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val anchors = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            anchors.forEachIndexed { index, anchor ->
                val section = visibleSections[index]
                anchor.placeRelative(
                    x = 0,
                    y = drawerPinnedAnchorTop(section.top, section.bottom, anchor.height, topInset.roundToPx()),
                )
            }
        }
    }
}
