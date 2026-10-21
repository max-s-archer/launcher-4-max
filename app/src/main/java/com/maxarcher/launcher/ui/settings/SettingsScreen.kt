package com.maxarcher.launcher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxarcher.launcher.AccessibilityLockController
import com.maxarcher.launcher.EmptyAccessibilityLockController
import com.maxarcher.launcher.IconShapeSettings
import com.maxarcher.launcher.HomeBottomAppBarState
import com.maxarcher.launcher.QuickAction
import com.maxarcher.launcher.QuickActionBindings
import com.maxarcher.launcher.QuickActionSlot
import com.maxarcher.launcher.R
import com.maxarcher.launcher.SettingsBackupControl
import com.maxarcher.launcher.SettingsBackupState
import com.maxarcher.launcher.storageValue
import com.maxarcher.launcher.ui.style.styleSettingsPanelSurface
import kotlinx.coroutines.launch

internal enum class SettingsPage {
    Root,
    QuickActions,
    IconShape,
    HomeBottomAppBar,
}

@Composable
internal fun SettingsScreen(
    platform: SettingsPlatform,
    licenseText: String,
    thirdPartyNoticesText: String = "",
    accessibilityLockController: AccessibilityLockController = EmptyAccessibilityLockController,
    backupController: SettingsBackupControl? = null,
    bindings: QuickActionBindings = QuickActionBindings(),
    onBindQuickAction: (QuickActionSlot, QuickAction) -> Unit = { _, _ -> },
    iconShapeSettings: IconShapeSettings = IconShapeSettings(),
    iconShapeSaveInFlight: Boolean = false,
    iconShapeSaveFailureToken: Int = 0,
    onCommitIconShapeSettings: (IconShapeSettings) -> Unit = {},
    homeBottomAppBarState: HomeBottomAppBarState = HomeBottomAppBarState(),
    homeBottomAppBarSaveInFlight: Boolean = false,
    homeBottomAppBarSafeWidthDp: Float? = null,
    onHomeBottomAppBarStateChange: (HomeBottomAppBarState) -> Unit = {},
    page: SettingsPage = SettingsPage.Root,
    onPageChange: (SettingsPage) -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isDefaultHome by remember(platform) { mutableStateOf(platform.isDefaultHome()) }
    var showLicense by remember { mutableStateOf(false) }
    var showThirdPartyNotices by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showScreenLockExplanation by remember { mutableStateOf(false) }
    var popupSlot by remember { mutableStateOf<QuickActionSlot?>(null) }
    var popupAnchorInWindow by remember { mutableStateOf<IntOffset?>(null) }
    var showProminentDisclosure by remember { mutableStateOf(false) }
    var backupInFlight by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<SettingsBackupState?>(null) }
    val rootListState = rememberLazyListState()
    var isAccessibilitySystemEnabled by remember(accessibilityLockController) {
        mutableStateOf(accessibilityLockController.isSystemEnabled())
    }
    val isAccessibilityConnected by accessibilityLockController.connectionState
        .collectAsStateWithLifecycle()

    val backupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
    ) { uri ->
        if (uri != null && backupController != null) {
            scope.launch {
                backupInFlight = true
                val succeeded = backupController.writeBackup(uri = uri)
                backupInFlight = false
                Toast.makeText(
                    context,
                    if (succeeded) {
                        R.string.settings_data_backup_success
                    } else {
                        R.string.settings_data_backup_failure
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && backupController != null) {
            scope.launch {
                backupInFlight = true
                val backup = backupController.readBackup(uri = uri)
                backupInFlight = false
                if (backup == null) {
                    Toast.makeText(
                        context,
                        R.string.settings_data_restore_failure,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    pendingRestore = backup
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, platform, accessibilityLockController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultHome = platform.isDefaultHome()
                isAccessibilitySystemEnabled = accessibilityLockController.isSystemEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = if (page == SettingsPage.IconShape) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
            }
            .testTag("settings_surface"),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            SettingsTopBar(
                title = stringResource(
                    when (page) {
                        SettingsPage.Root -> R.string.settings
                        SettingsPage.QuickActions -> R.string.quick_action_settings
                        SettingsPage.IconShape -> R.string.icon_shape
                        SettingsPage.HomeBottomAppBar -> R.string.home_bottom_app_bar
                    },
                ),
                onBack = onBack,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (page == SettingsPage.IconShape) {
                IconShapeSettingsContent(
                    settings = iconShapeSettings,
                    saveInFlight = iconShapeSaveInFlight,
                    saveFailureToken = iconShapeSaveFailureToken,
                    onCommit = onCommitIconShapeSettings,
                    modifier = Modifier.weight(1f),
                )
            } else if (page == SettingsPage.QuickActions) {
                QuickActionSettingsContent(
                    bindings = bindings,
                    serviceEnabled = isAccessibilitySystemEnabled && isAccessibilityConnected,
                    onOpenSlot = { slot, anchor ->
                        popupSlot = slot
                        popupAnchorInWindow = anchor
                    },
                    onOpenServiceState = { showScreenLockExplanation = true },
                    modifier = Modifier.weight(1f),
                )
            } else if (page == SettingsPage.HomeBottomAppBar) {
                HomeBottomAppBarSettingsContent(
                    state = homeBottomAppBarState,
                    saveInFlight = homeBottomAppBarSaveInFlight,
                    homeSafeWidthDp = homeBottomAppBarSafeWidthDp,
                    onStateChange = onHomeBottomAppBarStateChange,
                    modifier = Modifier.weight(1f),
                )
            } else {
            LazyColumn(
                state = rootListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("settings_list"),
            ) {
                item(key = "default-home") {
                    PrimarySettingsItem(
                        title = stringResource(R.string.default_home_application),
                        supportingText = stringResource(
                            if (isDefaultHome) {
                                R.string.launcher4max_is_default_launcher
                            } else {
                                R.string.launcher4max_is_not_default_launcher
                            },
                        ),
                        onClick = { platform.openDefaultHomeSettings() },
                        testTag = "settings_default_home",
                    )
                }
                item(key = "quick-action-settings") {
                    PrimarySettingsItem(
                        title = stringResource(R.string.quick_action_settings),
                        supportingText = null,
                        onClick = { onPageChange(SettingsPage.QuickActions) },
                        testTag = "settings_quick_action_settings",
                    )
                }
                item(key = "icon-shape") {
                    PrimarySettingsItem(
                        title = stringResource(R.string.icon_shape),
                        supportingText = null,
                        onClick = { onPageChange(SettingsPage.IconShape) },
                        testTag = "settings_icon_shape",
                    )
                }
                item(key = "home-bottom-app-bar") {
                    PrimarySettingsItem(
                        title = stringResource(R.string.home_bottom_app_bar),
                        supportingText = null,
                        onClick = { onPageChange(SettingsPage.HomeBottomAppBar) },
                        testTag = "settings_home_bottom_app_bar",
                    )
                }
                if (backupController != null) {
                    item(key = "data-backup") {
                        PrimarySettingsItem(
                            title = stringResource(R.string.settings_data_backup),
                            supportingText = null,
                            onClick = {
                                backupDocumentLauncher.launch(
                                    backupController.suggestedFileName(),
                                )
                            },
                            enabled = !backupInFlight,
                            testTag = "settings_data_backup",
                        )
                    }
                    item(key = "data-restore") {
                        PrimarySettingsItem(
                            title = stringResource(R.string.settings_data_restore),
                            supportingText = null,
                            onClick = {
                                restoreDocumentLauncher.launch(RESTORE_MIME_TYPES)
                            },
                            enabled = !backupInFlight,
                            testTag = "settings_data_restore",
                        )
                    }
                }
                item(key = "privacy") {
                    SecondarySettingsItem(
                        text = stringResource(R.string.privacy),
                        onClick = { showPrivacy = true },
                        testTag = "settings_privacy",
                    )
                }
                item(key = "license") {
                    SecondarySettingsItem(
                        text = stringResource(R.string.launcher4max_license),
                        onClick = { showLicense = true },
                        testTag = "settings_license",
                    )
                }
                item(key = "third-party-notices") {
                    SecondarySettingsItem(
                        text = stringResource(R.string.third_party_notices),
                        onClick = { showThirdPartyNotices = true },
                        testTag = "settings_third_party_notices",
                    )
                }
                item(key = "repository") {
                    SecondarySettingsItem(
                        text = stringResource(R.string.project_repository),
                        onClick = {
                            if (!platform.openProjectRepository()) {
                                Toast.makeText(
                                    context,
                                    R.string.unable_to_open_project_link,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        testTag = "settings_project_repository",
                    )
                }
                item(key = "version") {
                    SecondarySettingsItem(
                        text = platform.versionText(),
                        onClick = null,
                        testTag = "settings_version",
                    )
                }
            }
            }
        }
    }

    popupSlot?.let { slot ->
        val currentBinding = bindings.actionFor(slot)
        QuickActionSelectionPopup(
            current = currentBinding,
            anchorInWindow = popupAnchorInWindow,
            onSelect = { action ->
                popupSlot = null
                popupAnchorInWindow = null
                onBindQuickAction(slot, action)
                if (action == QuickAction.ScreenLock &&
                    !accessibilityLockController.isSystemEnabled()
                ) {
                    showScreenLockExplanation = true
                }
            },
            onDismiss = {
                popupSlot = null
                popupAnchorInWindow = null
            },
        )
    }

    if (showLicense) {
        LicenseBottomSheet(
            title = stringResource(R.string.launcher4max_license_title),
            body = licenseText,
            sheetTag = "launcher4max_license_sheet",
            bodyTag = "launcher4max_license_text",
            onDismiss = { showLicense = false },
        )
    }

    if (showThirdPartyNotices) {
        LicenseBottomSheet(
            title = stringResource(R.string.third_party_notices_title),
            body = thirdPartyNoticesText,
            sheetTag = "third_party_notices_sheet",
            bodyTag = "third_party_notices_text",
            onDismiss = { showThirdPartyNotices = false },
        )
    }

    if (showPrivacy) {
        PrivacyBottomSheet(
            onOpenContact = {
                if (!platform.openPrivacyContact()) {
                    Toast.makeText(
                        context,
                        R.string.unable_to_open_privacy_contact,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onDismiss = { showPrivacy = false },
        )
    }

    if (showScreenLockExplanation) {
        ScreenLockExplanationSheet(
            enabled = isAccessibilitySystemEnabled && isAccessibilityConnected,
            onOpenAccessibilitySettings = {
                val systemEnabled = accessibilityLockController.isSystemEnabled()
                isAccessibilitySystemEnabled = systemEnabled
                if (systemEnabled) {
                    if (!accessibilityLockController.openAccessibilitySettings()) {
                        Toast.makeText(
                            context,
                            R.string.unable_to_open_accessibility_settings,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else {
                    showScreenLockExplanation = false
                    showProminentDisclosure = true
                }
            },
            onDismiss = {
                showScreenLockExplanation = false
            },
        )
    }

    if (showProminentDisclosure) {
        AccessibilityProminentDisclosure(
            onCancel = { showProminentDisclosure = false },
            onContinue = {
                showProminentDisclosure = false
                if (!accessibilityLockController.openAccessibilitySettings()) {
                    Toast.makeText(
                        context,
                        R.string.unable_to_open_accessibility_settings,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
    }

    pendingRestore?.let { backup ->
        RestoreConfirmationDialog(
            onConfirm = {
                pendingRestore = null
                if (backupController != null) {
                    scope.launch {
                        backupInFlight = true
                        val succeeded = backupController.restore(backup = backup)
                        backupInFlight = false
                        Toast.makeText(
                            context,
                            if (succeeded) {
                                R.string.settings_data_restore_success
                            } else {
                                R.string.settings_data_restore_failure
                            },
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onCancel = { pendingRestore = null },
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.settings_top_bar_min_height)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The Back target starts 12dp from the safe start edge so its 24dp artwork begins
        // 24dp out, matching the Drawer top app bar.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.settings_back_target_start))
                .testTag("settings_back"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.settings_horizontal_padding))
                .testTag("settings_title"),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun PrimarySettingsItem(
    title: String,
    supportingText: String?,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(
                min = dimensionResource(
                    if (supportingText == null) {
                        R.dimen.settings_primary_one_line_min_height
                    } else {
                        R.dimen.settings_primary_two_line_min_height
                    },
                ),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.settings_horizontal_padding))
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            supportingText?.let { text ->
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.settings_trailing_icon_size)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SecondarySettingsItem(
    text: String,
    onClick: (() -> Unit)?,
    testTag: String,
) {
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.settings_secondary_item_min_height))
            .then(interactionModifier)
            .padding(horizontal = dimensionResource(R.dimen.settings_horizontal_padding))
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScreenLockExplanationSheet(
    enabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
        dragHandle = { SettingsModalDragHandle() },
        modifier = Modifier.testTag("screen_lock_explanation_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.settings_horizontal_padding)),
        ) {
            Text(
                text = stringResource(R.string.quick_action_screen_lock),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    if (enabled) R.string.capability_on else R.string.capability_off,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.screen_lock_explanation),
                modifier = Modifier.padding(
                    vertical = dimensionResource(R.dimen.settings_modal_content_spacing),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("open_accessibility_settings"),
            ) {
                Text(stringResource(R.string.open_accessibility_settings))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PrivacyBottomSheet(
    onOpenContact: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
        dragHandle = { SettingsModalDragHandle() },
        modifier = Modifier.testTag("privacy_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.settings_horizontal_padding)),
        ) {
            Text(
                text = stringResource(R.string.privacy),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.privacy_statement),
                modifier = Modifier.padding(
                    vertical = dimensionResource(R.dimen.settings_modal_content_spacing),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenContact,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("privacy_contact"),
            ) {
                Text(stringResource(R.string.privacy_contact_url))
            }
            Text(
                text = stringResource(R.string.privacy_contact_behavior),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AccessibilityProminentDisclosure(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.accessibility_disclosure_title)) },
        text = { Text(stringResource(R.string.accessibility_disclosure_body)) },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("accessibility_disclosure_cancel"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.testTag("accessibility_disclosure_continue"),
            ) {
                Text(stringResource(R.string.agree_and_continue))
            }
        },
        modifier = Modifier.testTag("accessibility_prominent_disclosure"),
    )
}

@Composable
private fun SettingsModalDragHandle() {
    androidx.compose.foundation.layout.Box(
        Modifier
            .padding(vertical = dimensionResource(R.dimen.action_sheet_handle_padding))
            .size(
                width = dimensionResource(R.dimen.action_sheet_handle_width),
                height = dimensionResource(R.dimen.action_sheet_handle_height),
            )
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    dimensionResource(R.dimen.action_sheet_handle_height),
                ),
            ),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LicenseBottomSheet(
    title: String,
    body: String,
    sheetTag: String,
    bodyTag: String,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(vertical = dimensionResource(R.dimen.action_sheet_handle_padding))
                    .size(
                        width = dimensionResource(R.dimen.action_sheet_handle_width),
                        height = dimensionResource(R.dimen.action_sheet_handle_height),
                    )
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            dimensionResource(R.dimen.action_sheet_handle_height),
                        ),
                    ),
            )
        },
        modifier = Modifier.testTag(sheetTag),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.settings_horizontal_padding),
                vertical = dimensionResource(R.dimen.settings_license_title_vertical_padding),
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = body,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.settings_horizontal_padding))
                .testTag(bodyTag),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun RestoreConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_data_restore_dialog_title)) },
        text = { Text(stringResource(R.string.settings_data_restore_dialog_body)) },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("settings_restore_cancel"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("settings_restore_confirm"),
            ) {
                Text(stringResource(R.string.settings_data_restore_confirm))
            }
        },
        modifier = Modifier.testTag("settings_restore_dialog"),
    )
}

@Composable
private fun QuickActionSettingsContent(
    bindings: QuickActionBindings,
    serviceEnabled: Boolean,
    onOpenSlot: (QuickActionSlot, IntOffset) -> Unit,
    onOpenServiceState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_action_settings_page"),
    ) {
        QuickActionSlotRow(
            slot = QuickActionSlot.DoubleTap,
            title = stringResource(R.string.quick_action_double_tap_slot),
            action = bindings.doubleTap,
            onOpen = onOpenSlot,
            testTag = "quick_action_slot_double_tap",
        )
        QuickActionSlotRow(
            slot = QuickActionSlot.LongPress,
            title = stringResource(R.string.quick_action_long_press_slot),
            action = bindings.longPress,
            onOpen = onOpenSlot,
            testTag = "quick_action_slot_long_press",
        )
        PrimarySettingsItem(
            title = stringResource(R.string.quick_action_screen_lock),
            supportingText = stringResource(
                if (serviceEnabled) R.string.capability_on else R.string.capability_off,
            ),
            onClick = onOpenServiceState,
            testTag = "quick_action_service_state",
        )
    }
}

@Composable
private fun QuickActionSlotRow(
    slot: QuickActionSlot,
    title: String,
    action: QuickAction,
    onOpen: (QuickActionSlot, IntOffset) -> Unit,
    testTag: String,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    PrimarySettingsItem(
        title = title,
        supportingText = stringResource(action.labelRes()),
        onClick = {
            val position = coordinates?.positionInWindow()?.round() ?: IntOffset.Zero
            val size = coordinates?.size ?: IntSize.Zero
            onOpen(
                slot,
                IntOffset(x = position.x, y = position.y + size.height / 2),
            )
        },
        testTag = testTag,
        modifier = Modifier.onGloballyPositioned { coordinates = it },
    )
}

@Composable
private fun QuickActionSelectionPopup(
    current: QuickAction,
    anchorInWindow: IntOffset?,
    onSelect: (QuickAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val trailingInsetPx = with(density) {
        dimensionResource(R.dimen.settings_popup_trailing_inset).roundToPx()
    }
    val positionProvider = remember(anchorInWindow, trailingInsetPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val anchor = anchorInWindow ?: IntOffset(
                    x = anchorBounds.right,
                    y = anchorBounds.top + anchorBounds.height / 2,
                )
                val x = (windowSize.width - trailingInsetPx - popupContentSize.width)
                    .coerceAtLeast(0)
                return IntOffset(x = x, y = anchor.y)
            }
        }
    }
    val cornerRadius = dimensionResource(R.dimen.settings_popup_corner_radius)
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .styleSettingsPanelSurface(cornerRadius = cornerRadius)
                .testTag("quick_action_selection_popup"),
        ) {
            QuickAction.entries.forEach { action ->
                QuickActionOptionRow(
                    action = action,
                    selected = action == current,
                    onSelect = { onSelect(action) },
                )
            }
        }
    }
}

@Composable
private fun QuickActionOptionRow(
    action: QuickAction,
    selected: Boolean,
    onSelect: () -> Unit,
) {
        Row(
            modifier = Modifier
                .height(dimensionResource(R.dimen.settings_popup_option_height))
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = dimensionResource(R.dimen.settings_horizontal_padding))
            .testTag("quick_action_option_${action.storageValue}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.onSurface,
                unselectedColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.size(dimensionResource(R.dimen.settings_popup_radio_size)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.settings_popup_radio_label_gap)))
        Text(
            text = stringResource(action.labelRes()),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = dimensionResource(R.dimen.settings_popup_primary_text_size).value.sp,
        )
    }
}

private fun QuickAction.labelRes(): Int = when (this) {
    QuickAction.NoAction -> R.string.quick_action_no_action
    QuickAction.EditMode -> R.string.quick_action_edit_mode
    QuickAction.ScreenLock -> R.string.quick_action_screen_lock
}

private const val JSON_MIME_TYPE = "application/json"
private val RESTORE_MIME_TYPES = arrayOf(JSON_MIME_TYPE, "*/*")
