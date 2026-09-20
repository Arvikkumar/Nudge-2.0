package com.example.gentlenudge.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gentlenudge.notification.EventNotificationMode
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import com.example.gentlenudge.ui.theme.NudgeBlue

@Composable
fun EventNotificationSettingDialog(
    currentMode: EventNotificationMode,
    currentDays: Int,
    currentHour: Int = NudgeEventNotificationScheduler.getCustomNotificationHour(LocalContext.current),
    currentMinute: Int = NudgeEventNotificationScheduler.getCustomNotificationMinute(LocalContext.current),
    onDismiss: () -> Unit,
    onSettingSaved: (mode: EventNotificationMode, days: Int) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }
    var customDays by remember(currentDays, currentMode) {
        val initialDays = if (currentMode == EventNotificationMode.CUSTOM && currentDays in 1..30) {
            currentDays
        } else {
            val savedCustom = NudgeEventNotificationScheduler.getCustomNotificationDays(context)
            if (savedCustom in 1..30) savedCustom else 3
        }
        mutableIntStateOf(initialDays)
    }
    var customHour by remember(currentHour) {
        mutableIntStateOf(if (currentHour in 0..23) currentHour else 9)
    }
    var customMinute by remember(currentMinute) {
        mutableIntStateOf(if (currentMinute in 0..59) currentMinute else 0)
    }

    // Permission request launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Result callback; permission state will be handled automatically by system
    }

    fun checkAndRequestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedMode != EventNotificationMode.OFF) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.NotificationsOff
                        },
                        contentDescription = null,
                        tint = NudgeBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "EVENT NOTIFICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = NudgeBlue
                    )
                    Text(
                        text = "Global Event Reminder",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Applies automatically to all festivals, world days, and calendar observances at 9:00 AM.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: OFF
                EventOptionCard(
                    title = "Off",
                    description = "No event notifications",
                    isSelected = selectedMode == EventNotificationMode.OFF,
                    onClick = {
                        selectedMode = EventNotificationMode.OFF
                    },
                    testTag = "event_reminder_opt_off"
                )

                // Option 2: 1 DAY BEFORE
                EventOptionCard(
                    title = "1 day before",
                    description = "Notify 1 day before every upcoming event",
                    isSelected = selectedMode == EventNotificationMode.ONE_DAY_BEFORE,
                    onClick = {
                        selectedMode = EventNotificationMode.ONE_DAY_BEFORE
                        checkAndRequestPermissionIfNeeded()
                    },
                    testTag = "event_reminder_opt_1day"
                )

                // Option 3: 2 DAYS BEFORE
                EventOptionCard(
                    title = "2 days before",
                    description = "Notify 2 days before every upcoming event",
                    isSelected = selectedMode == EventNotificationMode.TWO_DAYS_BEFORE,
                    onClick = {
                        selectedMode = EventNotificationMode.TWO_DAYS_BEFORE
                        checkAndRequestPermissionIfNeeded()
                    },
                    testTag = "event_reminder_opt_2days"
                )

                // Option 4: CUSTOM
                EventOptionCard(
                    title = "Custom",
                    description = "Choose number of days before every event",
                    isSelected = selectedMode == EventNotificationMode.CUSTOM,
                    onClick = {
                        selectedMode = EventNotificationMode.CUSTOM
                        checkAndRequestPermissionIfNeeded()
                    },
                    testTag = "event_reminder_opt_custom"
                )

                // Custom Days Stepper
                AnimatedVisibility(
                    visible = selectedMode == EventNotificationMode.CUSTOM,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF3F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NudgeBlue.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Remind in advance:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (customDays > 1) NudgeBlue.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable(
                                                enabled = customDays > 1,
                                                onClick = {
                                                    if (customDays > 1) {
                                                        customDays = customDays - 1
                                                    }
                                                }
                                            )
                                            .testTag("custom_days_minus"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease days",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (customDays > 1) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.padding(horizontal = 1.dp)
                                    ) {
                                        Text(
                                            text = "$customDays ${if (customDays == 1) "day" else "days"}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = NudgeBlue,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("custom_days_text")
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (customDays < 30) NudgeBlue.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable(
                                                enabled = customDays < 30,
                                                onClick = {
                                                    if (customDays < 30) {
                                                        customDays = customDays + 1
                                                    }
                                                }
                                            )
                                            .testTag("custom_days_plus"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase days",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (customDays < 30) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Notification time:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    customHour = h
                                                    customMinute = m
                                                },
                                                customHour,
                                                customMinute,
                                                false
                                            ).show()
                                        }
                                        .testTag("custom_notif_time_btn")
                                ) {
                                    Text(
                                        text = NudgeEventNotificationScheduler.formatTime(customHour, customMinute),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = NudgeBlue,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("custom_notif_time_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMode != EventNotificationMode.OFF) {
                        checkAndRequestPermissionIfNeeded()
                    }
                    val finalDays = when (selectedMode) {
                        EventNotificationMode.OFF -> 0
                        EventNotificationMode.ONE_DAY_BEFORE -> 1
                        EventNotificationMode.TWO_DAYS_BEFORE -> 2
                        EventNotificationMode.CUSTOM -> customDays
                    }
                    NudgeEventNotificationScheduler.setEventNotificationSetting(
                        context = context,
                        mode = selectedMode,
                        customDays = customDays,
                        customHour = customHour,
                        customMinute = customMinute
                    )
                    onSettingSaved(selectedMode, finalDays)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NudgeBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("event_setting_save_btn")
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("event_setting_cancel_btn")
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun EventOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.5f else 0.6f)
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFFAF9F6)
    }

    val borderColor = if (isSelected) {
        NudgeBlue.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = if (isSelected) NudgeBlue else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NudgeBlue,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
