package com.maxarcher.launcher.ui.drawer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.maxarcher.launcher.ui.drawer.DrawerIcon as Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.zIndex
import com.maxarcher.launcher.R

@Composable
internal fun DrawerNavigationTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height = dimensionResource(id = R.dimen.drawer_top_app_bar_height))
            .testTag(tag = "drawer_top_app_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(
                width = dimensionResource(id = R.dimen.drawer_search_side_reservation_width),
            ),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag(tag = "drawer_back"),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/**
 * The shared top-app-bar divider from design foundations: one full-width `1dp` line using
 * `secondaryTextColor` in every bar state, without making the bar an opaque surface.
 */
@Composable
internal fun DrawerTopBarDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun DrawerSearchTopBar(
    searchActive: Boolean,
    query: String,
    focusRequester: FocusRequester,
    onNavigateBack: () -> Unit,
    onEnterSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val animationDuration = integerResource(id = R.integer.short_property_animation_duration_ms)
    val borderWidth = animateDpAsState(
        targetValue = dimensionResource(
            id = if (searchActive) {
                R.dimen.drawer_search_active_border_width
            } else {
                R.dimen.drawer_search_border_width
            },
        ),
        animationSpec = tween(durationMillis = animationDuration),
        label = "drawerSearchBorderWidth",
    )
    val borderColor = animateColorAsState(
        targetValue = if (searchActive) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = animationDuration),
        label = "drawerSearchBorderColor",
    )
    val searchDescription = stringResource(id = R.string.drawer_search_apps)
    val searchModeDescription = stringResource(id = R.string.drawer_search_mode)

    LaunchedEffect(key1 = searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.drawer_top_app_bar_height))
            .testTag(tag = "drawer_top_app_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(
                width = dimensionResource(id = R.dimen.drawer_search_side_reservation_width),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (!searchActive) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag(tag = "drawer_back"),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .height(height = dimensionResource(id = R.dimen.drawer_top_app_bar_height)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = dimensionResource(id = R.dimen.drawer_search_field_height))
                    .border(
                        border = BorderStroke(
                            width = borderWidth.value,
                            color = borderColor.value,
                        ),
                        shape = RoundedCornerShape(
                            size = dimensionResource(id = R.dimen.drawer_search_field_radius),
                        ),
                    )
                    .then(
                        if (searchActive) {
                            Modifier
                        } else {
                            Modifier.clickable(onClick = onEnterSearch)
                        },
                    )
                    .padding(
                        start = dimensionResource(id = R.dimen.drawer_search_field_content_inset),
                        end = if (searchActive && query.isNotEmpty()) {
                            dimensionResource(id = R.dimen.drawer_search_clear_target_size)
                        } else {
                            dimensionResource(id = R.dimen.drawer_search_field_content_inset)
                        },
                    )
                    .then(
                        if (searchActive) {
                            Modifier
                        } else {
                            Modifier.semantics {
                                contentDescription = searchDescription
                            }
                        },
                    )
                    .testTag(tag = "drawer_search_field"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(
                        size = dimensionResource(id = R.dimen.drawer_search_icon_size),
                    ),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(
                    modifier = Modifier.width(
                        width = dimensionResource(id = R.dimen.drawer_search_icon_text_gap),
                    ),
                )
                if (searchActive) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(weight = 1f)
                            .focusRequester(focusRequester = focusRequester)
                            .semantics {
                                contentDescription = searchDescription
                                stateDescription = searchModeDescription
                            }
                            .testTag(tag = "drawer_search_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.onBackground),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = searchDescription,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                } else {
                    Text(
                        text = searchDescription,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (searchActive && query.isNotEmpty()) {
                IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .size(size = dimensionResource(id = R.dimen.drawer_search_clear_target_size))
                        .zIndex(zIndex = 1f)
                        .testTag(tag = "drawer_search_clear"),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.drawer_clear_search),
                        modifier = Modifier.size(
                            size = dimensionResource(id = R.dimen.drawer_search_icon_size),
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        Box(
            modifier = Modifier.width(
                width = dimensionResource(id = R.dimen.drawer_search_side_reservation_width),
            ),
            // Centering the right entry matches the Back target's position in the start
            // reservation, keeping both bar-edge icons 12dp from their safe edges.
            contentAlignment = Alignment.Center,
        ) {
            if (searchActive) {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        onCancelSearch()
                    },
                    modifier = Modifier
                        .height(height = dimensionResource(id = R.dimen.drawer_search_side_target_height))
                        .widthIn(
                            min = dimensionResource(
                                id = R.dimen.drawer_search_side_reservation_width,
                            ),
                        )
                        .testTag(tag = "drawer_search_cancel"),
                ) {
                    Text(
                        text = stringResource(id = R.string.drawer_search_cancel),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenDisplaySettings,
                    modifier = Modifier.testTag(tag = "drawer_display_settings_entry"),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_horiz),
                        contentDescription = stringResource(
                            id = R.string.drawer_display_settings,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
