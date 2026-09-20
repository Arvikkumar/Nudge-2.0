package com.example.gentlenudge.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.backup.NudgeBackupScheduler
import com.example.gentlenudge.ui.components.MinimalAnalogClock
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import com.example.gentlenudge.widget.NudgeWidgetPinningHelper
import java.util.Calendar

@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    nudgeAgainEnabled: Boolean,
    onToggleNudgeAgain: () -> Unit,
    defaultSnooze: String,
    onSelectSnooze: (String) -> Unit,
    deletedTasksCount: Int = 0,
    onOpenRecentlyDeleted: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    // Backup & Data Safety state & actions
    lastBackupTimestamp: Long = 0L,
    lastManualBackupTimestamp: Long = lastBackupTimestamp,
    lastAutoBackupTimestamp: Long = 0L,
    autoBackupFiles: List<com.example.gentlenudge.backup.NudgeBackupManager.AutoBackupFileInfo> = emptyList(),
    onShareAutoBackup: (java.io.File) -> Unit = {},
    autoBackupEnabled: Boolean = false,
    onToggleAutoBackup: () -> Unit = {},
    autoBackupDay: Int = Calendar.SUNDAY,
    autoBackupHour: Int = 23,
    autoBackupMinute: Int = 0,
    onSetAutoBackupSchedule: (dayOfWeek: Int, hour: Int, minute: Int) -> Unit = { _, _, _ -> },
    onResetSampleData: () -> Unit = {},
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showAutoScheduleDialog by remember { mutableStateOf(false) }
    var isAlertsExpanded by rememberSaveable { mutableStateOf(false) }
    var isBackupExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    val lastManualBackupRelative = remember(lastManualBackupTimestamp) {
        NudgeBackupScheduler.formatLastBackupRelative(lastManualBackupTimestamp)
    }

    val lastAutoBackupRelative = remember(lastAutoBackupTimestamp) {
        NudgeBackupScheduler.formatLastBackupRelative(lastAutoBackupTimestamp)
    }

    val scheduleSummary = remember(autoBackupDay, autoBackupHour, autoBackupMinute) {
        NudgeBackupScheduler.formatScheduleSummary(autoBackupDay, autoBackupHour, autoBackupMinute)
    }

    val nextAutoBackupFormatted = remember(autoBackupDay, autoBackupHour, autoBackupMinute) {
        NudgeBackupScheduler.formatNextAutoBackup(autoBackupDay, autoBackupHour, autoBackupMinute)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Heading
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Settings that",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "fit your rhythm.",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp
                    ),
                    color = NudgeBlue
                )
            }
        }

        // Clean, minimal, full-width accordion sections: Alerts, Backup, History
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("minimal_accordion_container")
            ) {
                // 1. Alerts Section
                MinimalAccordionSection(
                    title = "Alerts",
                    isExpanded = isAlertsExpanded,
                    onToggle = { isAlertsExpanded = !isAlertsExpanded },
                    testTag = "alerts_accordion_section"
                ) {
                    SettingToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        description = "A little tap when something matters",
                        checked = notificationsEnabled,
                        onCheckedChange = { onToggleNotifications() }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingActionRow(
                        icon = Icons.Outlined.Schedule,
                        title = "Default snooze",
                        description = "A little breathing room",
                        actionText = "$defaultSnooze ⌵",
                        onClick = {
                            val next = when (defaultSnooze) {
                                "15 minutes" -> "30 minutes"
                                "30 minutes" -> "1 hour"
                                "1 hour" -> "Tomorrow"
                                else -> "15 minutes"
                            }
                            onSelectSnooze(next)
                            onShowToast("Default snooze set to $next.")
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingToggleRow(
                        icon = Icons.Outlined.Tune,
                        title = "Nudge again",
                        description = "Ask again, softly, if I miss it",
                        checked = nudgeAgainEnabled,
                        onCheckedChange = { onToggleNudgeAgain() }
                    )
                }

                // 2. Backup Section
                MinimalAccordionSection(
                    title = "Backup",
                    isExpanded = isBackupExpanded,
                    onToggle = { isBackupExpanded = !isBackupExpanded },
                    testTag = "backup_accordion_section"
                ) {
                    SettingInfoRow(
                        icon = Icons.Outlined.FileUpload,
                        title = "Last manual backup",
                        description = "Confirmed timestamp of your latest manual export",
                        valueText = lastManualBackupRelative
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingInfoRow(
                        icon = Icons.Outlined.History,
                        title = "Last automatic backup",
                        description = "Confirmed timestamp of your latest automatic backup",
                        valueText = lastAutoBackupRelative
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingToggleRow(
                        icon = Icons.Outlined.Autorenew,
                        title = "Automatic Backup",
                        description = "Automatically create a local backup of my Nudge data",
                        checked = autoBackupEnabled,
                        onCheckedChange = { onToggleAutoBackup() }
                    )

                    if (autoBackupEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                        SettingActionRow(
                            icon = Icons.Outlined.Event,
                            title = "Backup schedule",
                            description = "Recurring local backup schedule",
                            actionText = "$scheduleSummary ⌵",
                            onClick = { showAutoScheduleDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                        SettingInfoRow(
                            icon = Icons.Outlined.Today,
                            title = "Next backup",
                            description = "Scheduled automatic local backup",
                            valueText = nextAutoBackupFormatted
                        )
                    }

                    if (autoBackupFiles.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Saved Automatic Backups",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Latest 3 backups retained safely in internal storage. Tap to share or save externally.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            autoBackupFiles.forEach { backupItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backupItem.formattedDate,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${backupItem.formattedSize} • ${backupItem.name}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    TextButton(
                                        onClick = { onShareAutoBackup(backupItem.file) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = "Share ${backupItem.name}",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingActionRow(
                        icon = Icons.Outlined.FileUpload,
                        title = "Create Local Backup",
                        description = "Export all your notes, pursuits, settings, and attachments to a safe backup file",
                        actionText = "Backup",
                        onClick = onCreateBackup
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingActionRow(
                        icon = Icons.Outlined.FileDownload,
                        title = "Restore Local Backup",
                        description = "Recover notes, pursuits, and attachments from a previously saved backup file",
                        actionText = "Restore",
                        onClick = onRestoreBackup
                    )
                }

                // 3. History Section
                MinimalAccordionSection(
                    title = "History",
                    isExpanded = isHistoryExpanded,
                    onToggle = { isHistoryExpanded = !isHistoryExpanded },
                    testTag = "history_accordion_section"
                ) {
                    SettingActionRow(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "Recently Deleted",
                        description = "Recover deleted reminders within 30 days",
                        actionText = if (deletedTasksCount > 0) "$deletedTasksCount items ›" else "View ›",
                        onClick = onOpenRecentlyDeleted
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.8.dp)

                    SettingActionRow(
                        icon = Icons.Outlined.PictureAsPdf,
                        title = "Export History (PDF)",
                        description = "Download a complete, offline PDF copy of all your completed & pending notes",
                        actionText = "Export PDF",
                        onClick = onExportPdf
                    )
                }
            }
        }

        // Brand signature footer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 35.dp, bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A jack of all trades is master of none,\nbut often better than a master of one.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                MinimalAnalogClock(
                    modifier = Modifier
                        .size(210.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            NudgeWidgetPinningHelper.requestPinClockWidget(context) { msg ->
                                onShowToast(msg)
                            }
                        }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Dialog: Select Automatic Backup Schedule (Day & Time)
    if (showAutoScheduleDialog) {
        val daysOfWeek = listOf(
            0 to "Every day",
            Calendar.SUNDAY to "Every Sunday",
            Calendar.MONDAY to "Every Monday",
            Calendar.TUESDAY to "Every Tuesday",
            Calendar.WEDNESDAY to "Every Wednesday",
            Calendar.THURSDAY to "Every Thursday",
            Calendar.FRIDAY to "Every Friday",
            Calendar.SATURDAY to "Every Saturday"
        )
        var selectedDay by remember { mutableStateOf(autoBackupDay) }
        var selectedHour by remember { mutableStateOf(autoBackupHour) }
        var selectedMinute by remember { mutableStateOf(autoBackupMinute) }

        AlertDialog(
            onDismissRequest = { showAutoScheduleDialog = false },
            title = {
                Text(
                    text = "Automatic Backup Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select recurring day and time to safely generate a local backup:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Day of Week selection
                    Text(
                        text = "Frequency / Day:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NudgeBlue
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        daysOfWeek.forEach { (dayVal, dayLabel) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .selectable(
                                        selected = (selectedDay == dayVal),
                                        onClick = { selectedDay = dayVal },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedDay == dayVal),
                                    onClick = { selectedDay = dayVal },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NudgeBlue,
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedDay == dayVal) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Time Picker Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NudgeBlueContainer)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        selectedHour = h
                                        selectedMinute = m
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    false
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = NudgeBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Backup Time",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = NudgeBackupScheduler.formatTime(selectedHour, selectedMinute) + " ✎",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NudgeBlue
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetAutoBackupSchedule(selectedDay, selectedHour, selectedMinute)
                        showAutoScheduleDialog = false
                    }
                ) {
                    Text("Save Schedule", color = NudgeBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoScheduleDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun SettingInfoRow(
    icon: ImageVector,
    title: String,
    description: String,
    valueText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NudgeBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NudgeBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NudgeBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NudgeBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        NudgeIconToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = title
        )
    }
}

@Composable
fun NudgeIconToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val trackPadding = 3.dp

    // Animation values
    val trackColor by animateColorAsState(
        targetValue = if (checked) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nudge_toggle_track_color"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (trackWidth - thumbSize - trackPadding) else trackPadding,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nudge_toggle_thumb_offset"
    )

    // Touch target wrapper to ensure >= 48dp x 48dp accessibility standard
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        // Pill track
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(trackColor)
        ) {
            // ON icon: subtle white checkmark on the left side of track
            Box(
                modifier = Modifier
                    .size(width = trackWidth - thumbSize - trackPadding, height = trackHeight)
                    .align(Alignment.CenterStart)
                    .padding(start = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // OFF icon: subtle soft gray cross on the right side of track
            Box(
                modifier = Modifier
                    .size(width = trackWidth - thumbSize - trackPadding, height = trackHeight)
                    .align(Alignment.CenterEnd)
                    .padding(end = 7.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!checked) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Circular white thumb
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset)
                    .align(Alignment.CenterStart)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun SettingActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NudgeBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NudgeBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(NudgeBlueContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = NudgeBlue
            )
        }
    }
}

@Composable
fun MinimalAccordionSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevronRotation_$title"
    )

    val underlineProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "underlineProgress_$title"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        // Transparent clickable header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 2.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation),
                tint = if (isExpanded) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Underline divider: 1px thin outlineVariant divider when closed, animated subtle blue underline from LEFT to RIGHT when opened
        val baseLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            val strokeWidthPx = 1.dp.toPx()
            val activeStrokeWidthPx = 2.dp.toPx()

            // 1px base subtle line
            drawLine(
                color = baseLineColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = strokeWidthPx
            )

            // Animated blue underline expanding from left to right
            if (underlineProgress > 0.001f) {
                drawLine(
                    color = NudgeBlue,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width * underlineProgress, size.height / 2),
                    strokeWidth = activeStrokeWidthPx
                )
            }
        }

        // Expanded settings content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                content()
            }
        }
    }
}

