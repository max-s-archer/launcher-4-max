package com.maxarcher.launcher.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.R
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow

/**
 * The edit-mode operation header of one module. From the physical left it holds the
 * whole-module unfavorite action, the editable name, the name-visibility toggle, a flexible
 * space, the style affordance, and the module-reorder handle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeModuleOperationHeader(
    module: OrderedFavoriteModule,
    actionsEnabled: Boolean,
    nameEditable: Boolean,
    styleOpen: Boolean,
    nameRestoreKey: Int,
    onRename: (String) -> Unit,
    onNameFieldBoundsChange: (Rect?) -> Unit,
    onToggleNameVisible: () -> Unit,
    onToggleStyle: () -> Unit,
    onRemoveModule: (() -> Unit)?,
    reorderHandleModifier: Modifier,
) {
    val focusManager = LocalFocusManager.current
    var nameFocused by remember { mutableStateOf(false) }
    var nameBounds by remember { mutableStateOf<Rect?>(null) }
    // Keyed by the restore count so a failed save reloads the last saved name.
    var nameText by remember(module.id, nameRestoreKey) { mutableStateOf(module.name) }
    LaunchedEffect(module.name) {
        if (!nameFocused) nameText = module.name
    }
    BackHandler(enabled = nameFocused) { focusManager.clearFocus() }
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, nameFocused) {
        // System Back may hide the keyboard before Home receives it; end editing with it.
        if (nameFocused && imeWasVisible && !imeVisible) focusManager.clearFocus()
        imeWasVisible = imeVisible && nameFocused
    }
    val defaultName = module.copy(name = "").displayName()
    val textStyle = LocalTextStyle.current.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = dimensionResource(R.dimen.style_settings_secondary_text_size).value.sp,
        lineHeight = dimensionResource(R.dimen.style_settings_secondary_line_height).value.sp,
        shadow = drawerForegroundShadow(),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.home_module_header_height))
            .testTag("home_module_header_${module.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeModuleHeaderAction(
            icon = R.drawable.ic_cancel_favorite,
            label = stringResource(R.string.home_module_remove_group),
            enabled = actionsEnabled && onRemoveModule != null,
            testTag = "home_module_remove_${module.id}",
            onClick = {
                focusManager.clearFocus()
                onRemoveModule?.invoke()
            },
        )
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(dimensionResource(R.dimen.home_module_header_height))
                .padding(horizontal = dimensionResource(R.dimen.home_module_header_content_inset)),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = nameText,
                onValueChange = { value ->
                    val singleLine = value.replace("\n", "")
                    nameText = singleLine
                    if (singleLine != module.name) onRename(singleLine)
                },
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val bounds = Rect(coordinates.positionInWindow(), coordinates.size.toSize())
                        nameBounds = bounds
                        if (nameFocused) onNameFieldBoundsChange(bounds)
                    }
                    .onFocusChanged { state ->
                        nameFocused = state.isFocused
                        onNameFieldBoundsChange(nameBounds.takeIf { state.isFocused })
                    }
                    .then(
                        if (nameText.isEmpty()) {
                            Modifier.semantics { contentDescription = defaultName }
                        } else {
                            Modifier
                        },
                    )
                    .testTag("home_module_name_${module.id}"),
                readOnly = !nameEditable,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (nameFocused) {
                            innerTextField()
                        } else {
                            // Unfocused, the name shows end ellipsis instead of scrolling.
                            Box(modifier = Modifier.alpha(0f)) { innerTextField() }
                            Text(
                                text = nameText.ifEmpty { defaultName },
                                style = textStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
            )
        }
        HomeModuleHeaderAction(
            icon = if (module.nameVisible) {
                R.drawable.ic_visibility
            } else {
                R.drawable.ic_visibility_off
            },
            label = stringResource(
                if (module.nameVisible) {
                    R.string.home_module_hide_name
                } else {
                    R.string.home_module_show_name
                },
            ),
            enabled = actionsEnabled,
            testTag = "home_module_name_visibility_${module.id}",
            onClick = {
                focusManager.clearFocus()
                onToggleNameVisible()
            },
        )
        Spacer(modifier = Modifier.weight(1f))
        HomeModuleHeaderAction(
            icon = R.drawable.ic_tune,
            label = stringResource(
                if (styleOpen) {
                    R.string.home_collapse_style_panel
                } else {
                    R.string.home_expand_style_panel
                },
            ),
            enabled = actionsEnabled,
            testTag = "home_module_style_${module.id}",
            onClick = {
                focusManager.clearFocus()
                onToggleStyle()
            },
        )
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.home_module_header_height))
                .then(reorderHandleModifier)
                .alpha(headerContentAlpha(actionsEnabled))
                .testTag("home_module_reorder_${module.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.home_module_reorder_handle),
                modifier = Modifier.size(dimensionResource(R.dimen.home_add_favorite_icon_size)),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun HomeModuleHeaderAction(
    icon: Int,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.home_module_header_height))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(headerContentAlpha(enabled))
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(dimensionResource(R.dimen.home_add_favorite_icon_size)),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun headerContentAlpha(enabled: Boolean): Float =
    if (enabled) 1f else integerResource(R.integer.disabled_content_alpha_percent) / 100f
