package com.maxarcher.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.maxarcher.launcher.R

@Composable
internal fun HomeDefaultLauncherPrompt(
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val promptShape = RoundedCornerShape(
        size = dimensionResource(id = R.dimen.home_default_launcher_prompt_corner_radius),
    )
    val borderAlpha = integerResource(
        id = R.integer.home_favorite_bar_border_alpha_percent,
    ) / 100f
    val dismissLabel = stringResource(id = R.string.home_default_launcher_prompt_dismiss)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(
                min = dimensionResource(id = R.dimen.home_default_launcher_prompt_min_height),
            )
            .clip(shape = promptShape)
            .background(color = MaterialTheme.colorScheme.surface)
            .border(
                width = dimensionResource(id = R.dimen.home_favorite_bar_border_width),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = borderAlpha),
                shape = promptShape,
            )
            .testTag(tag = "home_default_launcher_prompt"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(weight = 1f)
                .clickable(role = Role.Button, onClick = onSelect)
                .padding(
                    start = dimensionResource(
                        id = R.dimen.home_default_launcher_prompt_content_padding,
                    ),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = stringResource(id = R.string.home_default_launcher_prompt_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimensionResource(id = R.dimen.home_date_text_size).value.sp,
                    lineHeight = dimensionResource(id = R.dimen.home_date_line_height).value.sp,
                )
                Text(
                    text = stringResource(
                        id = R.string.home_default_launcher_prompt_supporting,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = dimensionResource(
                        id = R.dimen.style_settings_secondary_text_size,
                    ).value.sp,
                    lineHeight = dimensionResource(
                        id = R.dimen.style_settings_secondary_line_height,
                    ).value.sp,
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier
                    .size(
                        size = dimensionResource(
                            id = R.dimen.home_default_launcher_prompt_icon_size,
                        ),
                    )
                    .clearAndSetSemantics { },
                tint = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(
                modifier = Modifier.width(
                    width = dimensionResource(
                        id = R.dimen.home_default_launcher_prompt_trailing_gap,
                    ),
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(
                    size = dimensionResource(
                        id = R.dimen.home_default_launcher_prompt_dismiss_target,
                    ),
                )
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics {
                    contentDescription = dismissLabel
                    role = Role.Button
                }
                .testTag(tag = "home_default_launcher_prompt_dismiss"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(
                    size = dimensionResource(
                        id = R.dimen.home_default_launcher_prompt_icon_size,
                    ),
                ),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
