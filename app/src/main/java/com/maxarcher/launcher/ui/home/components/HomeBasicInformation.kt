package com.maxarcher.launcher.ui.home.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.maxarcher.launcher.AccessibilityLockController
import com.maxarcher.launcher.HomeDateTimeFormatter
import com.maxarcher.launcher.LockRequestResult
import com.maxarcher.launcher.QuickAction
import com.maxarcher.launcher.QuickActionBindings
import com.maxarcher.launcher.R
import com.maxarcher.launcher.ui.drawer.drawerForegroundShadow
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun HomeBasicInformation(
    editMode: Boolean,
    accessibilityLockController: AccessibilityLockController,
    onRequestEditMode: () -> Unit,
    modifier: Modifier = Modifier,
    bindings: QuickActionBindings = QuickActionBindings(),
    clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    // The accepted basic-information glyph shadow shares the Drawer foreground parameters.
    val foregroundShadow = drawerForegroundShadow()
    var now by remember(key1 = clock) { mutableStateOf(value = clock()) }

    LaunchedEffect(key1 = clock) {
        while (true) {
            val remainingMillis = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(duration = remainingMillis.milliseconds)
            now = clock()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .homeEditSurface(enabled = editMode),
    ) {
        Column(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.home_information_margin),
                top = dimensionResource(R.dimen.home_information_margin),
                end = dimensionResource(R.dimen.home_information_margin),
            ),
        ) {
            Text(
                text = HomeDateTimeFormatter.time(context, value = now),
                modifier = Modifier
                    .heightIn(min = dimensionResource(R.dimen.home_time_min_height))
                    .testTag("home_time")
                    .clickable(enabled = !editMode, role = Role.Button) {
                        launchClockDestination(context)
                    },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = dimensionResource(id = R.dimen.home_time_text_size).value.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = dimensionResource(id = R.dimen.home_time_line_height).value.sp,
                style = LocalTextStyle.current.copy(shadow = foregroundShadow),
                textAlign = TextAlign.Start,
            )
            Text(
                text = HomeDateTimeFormatter.dateAndWeekday(context, now),
                modifier = Modifier
                    .heightIn(min = dimensionResource(id = R.dimen.home_date_height))
                    .testTag("home_date")
                    .clickable(enabled = !editMode, role = Role.Button) {
                        val calendarUri = CalendarContract.CONTENT_URI
                            .buildUpon()
                            .appendPath("time")
                            .appendPath(now.toInstant().toEpochMilli().toString())
                            .build()
                        launchPlatformDestination(
                            context = context,
                            intent = Intent(Intent.ACTION_VIEW, calendarUri),
                            failureMessage = R.string.calendar_unavailable,
                        )
                    }
                    .padding(
                        start = dimensionResource(id = R.dimen.home_date_text_start_inset),
                    )
                    .wrapContentHeight(align = Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = dimensionResource(id = R.dimen.home_date_text_size).value.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = dimensionResource(id = R.dimen.home_date_line_height).value.sp,
                style = LocalTextStyle.current.copy(shadow = foregroundShadow),
                textAlign = TextAlign.Start,
            )
        }
        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(
                    accessibilityLockController,
                    editMode,
                    bindings,
                ) {
                    detectTapGestures(
                        onDoubleTap = {
                            performBoundAction(
                                action = bindings.doubleTap,
                                editMode = editMode,
                                accessibilityLockController = accessibilityLockController,
                                context = context,
                                hapticFeedback = hapticFeedback,
                                onRequestEditMode = onRequestEditMode,
                            )
                        },
                        onLongPress = {
                            performBoundAction(
                                action = bindings.longPress,
                                editMode = editMode,
                                accessibilityLockController = accessibilityLockController,
                                context = context,
                                hapticFeedback = hapticFeedback,
                                onRequestEditMode = onRequestEditMode,
                            )
                        },
                    )
                }
                .testTag("home_quick_action_region"),
        )
    }
}

private fun performBoundAction(
    action: QuickAction,
    editMode: Boolean,
    accessibilityLockController: AccessibilityLockController,
    context: Context,
    hapticFeedback: HapticFeedback,
    onRequestEditMode: () -> Unit,
) {
    if (editMode) return
    when (action) {
        QuickAction.NoAction -> Unit
        QuickAction.EditMode -> {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onRequestEditMode()
        }
        QuickAction.ScreenLock -> {
            if (!accessibilityLockController.isSystemEnabled()) return
            if (accessibilityLockController.requestLock() != LockRequestResult.Requested) {
                Toast.makeText(
                    context,
                    R.string.unable_to_lock_screen,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}

private fun launchClockDestination(context: Context) {
    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    val clockMainIntent = try {
        resolveDefaultActivity(context.packageManager, alarmIntent)
            ?.activityInfo
            ?.packageName
            ?.let { context.packageManager.getLaunchIntentForPackage(it) }
    } catch (_: SecurityException) {
        null
    }

    launchPlatformDestination(
        context = context,
        intent = clockMainIntent ?: alarmIntent,
        failureMessage = R.string.clock_unavailable,
    )
}

private fun resolveDefaultActivity(packageManager: PackageManager, intent: Intent) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress(names = ["DEPRECATION"])
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

private fun launchPlatformDestination(
    context: Context,
    intent: Intent,
    @StringRes failureMessage: Int,
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}
