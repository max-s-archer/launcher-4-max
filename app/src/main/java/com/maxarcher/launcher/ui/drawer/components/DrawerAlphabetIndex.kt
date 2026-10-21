package com.maxarcher.launcher.ui.drawer.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.maxarcher.launcher.ui.drawer.DrawerIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.R

private const val SETTINGS_INDEX_ENTRY = "settings"

@Composable
internal fun DrawerAlphabetIndex(
    labels: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String, Boolean) -> Unit,
    onActiveLabelChange: (String?) -> Unit,
    onSelectSettings: (Boolean) -> Unit,
    includeSettings: Boolean = true,
) {
    val slotHeight = dimensionResource(id = R.dimen.drawer_index_slot_height)
    val density = LocalDensity.current
    val indexState = rememberLazyListState()
    val indexEntries = remember(
        key1 = labels,
        key2 = includeSettings,
        calculation = {
            if (includeSettings) labels + SETTINGS_INDEX_ENTRY else labels
        },
    )
    LazyColumn(
        modifier = modifier
            .height(
                height = minOf(
                    slotHeight * indexEntries.size,
                    dimensionResource(id = R.dimen.drawer_index_complete_height),
                ),
            )
            .width(width = dimensionResource(id = R.dimen.drawer_index_width))
            .pointerInput(
                key1 = indexEntries,
                key2 = indexState,
                block = {
                    val edgeThresholdPx = with(
                        receiver = density,
                        block = { slotHeight.toPx() },
                    )

                    fun entryAt(y: Float): String? = indexState.layoutInfo.visibleItemsInfo
                        .firstOrNull(
                            predicate = { item -> y >= item.offset && y < item.offset + item.size },
                        )
                        ?.let(
                            block = { item -> indexEntries.getOrNull(index = item.index) },
                        )

                    try {
                        awaitEachGesture(
                            block = {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var selectedLabel: String? = null
                                var multiplePointersDetected = false

                                fun select(entry: String?, immediate: Boolean) {
                                    if (entry == null || entry == selectedLabel) return
                                    selectedLabel = entry
                                    if (entry == SETTINGS_INDEX_ENTRY) {
                                        onActiveLabelChange(SETTINGS_INDEX_ENTRY)
                                        onSelectSettings(immediate)
                                    } else {
                                        onActiveLabelChange(entry)
                                        onSelect(entry, immediate)
                                    }
                                }

                                down.consume()
                                select(
                                    entry = entryAt(y = down.position.y),
                                    immediate = true,
                                )
                                var pointersRemainPressed: Boolean
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count(predicate = { it.pressed }) > 1) {
                                        multiplePointersDetected = true
                                        selectedLabel = null
                                        onActiveLabelChange(null)
                                    }
                                    event.changes.forEach(action = { change -> change.consume() })
                                    val activeChange = event.changes.firstOrNull(
                                        predicate = { it.id == down.id },
                                    )
                                    if (!multiplePointersDetected && activeChange?.pressed == true) {
                                        val viewportEnd = indexState.layoutInfo.viewportEndOffset
                                            .toFloat()
                                        when {
                                            activeChange.position.y < edgeThresholdPx -> {
                                                indexState.dispatchRawDelta(delta = -edgeThresholdPx)
                                            }

                                            activeChange.position.y > viewportEnd - edgeThresholdPx -> {
                                                indexState.dispatchRawDelta(delta = edgeThresholdPx)
                                            }
                                        }
                                        select(
                                            entry = entryAt(y = activeChange.position.y),
                                            immediate = false,
                                        )
                                    }
                                    pointersRemainPressed = event.changes.any(
                                        predicate = { it.pressed },
                                    )
                                } while (pointersRemainPressed)
                                onActiveLabelChange(null)
                            },
                        )
                    } finally {
                        onActiveLabelChange(null)
                    }
                },
            )
            .testTag(tag = "drawer_alphabet_index"),
        state = indexState,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            items(
                items = labels,
                key = { label -> "index:$label" },
                itemContent = { label ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = slotHeight)
                            .semantics(
                                properties = {
                                    role = Role.Button
                                    onClick(
                                        action = {
                                            onSelect(label, true)
                                            true
                                        },
                                    )
                                },
                            )
                            .testTag(tag = "drawer_index_$label"),
                        contentAlignment = Alignment.Center,
                        content = {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = dimensionResource(
                                    id = R.dimen.drawer_index_text_size,
                                ).value.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                    )
                },
            )
            if (includeSettings) {
                item(
                    key = "index:settings",
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = slotHeight)
                                .semantics(
                                    properties = {
                                        role = Role.Button
                                        onClick(
                                            action = {
                                                onSelectSettings(true)
                                                true
                                            },
                                        )
                                    },
                                )
                                .testTag(tag = "drawer_index_settings"),
                            contentAlignment = Alignment.Center,
                            content = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = stringResource(id = R.string.settings),
                                    modifier = Modifier.size(
                                        size = dimensionResource(
                                            id = R.dimen.drawer_index_settings_icon_size,
                                        ),
                                    ),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                        )
                    },
                )
            }
        },
    )
}

@Composable
internal fun DrawerIndexBubble(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size = dimensionResource(id = R.dimen.drawer_index_bubble_size))
            .testTag(tag = "drawer_index_bubble"),
        contentAlignment = Alignment.Center,
        content = {
            if (label == SETTINGS_INDEX_ENTRY) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = stringResource(id = R.string.settings),
                    modifier = Modifier.size(
                        size = dimensionResource(
                            id = R.dimen.drawer_index_bubble_settings_icon_size,
                        ),
                    ),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimensionResource(
                        id = R.dimen.drawer_index_bubble_text_size,
                    ).value.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}
