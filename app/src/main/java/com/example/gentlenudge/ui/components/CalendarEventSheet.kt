package com.example.gentlenudge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.data.events.NudgeCalendarEvent
import com.example.gentlenudge.notification.EventNotificationMode
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import com.example.gentlenudge.ui.theme.NudgeBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventSheet(
    events: List<NudgeCalendarEvent>,
    dateCalendar: Calendar,
    onDismiss: () -> Unit,
    onAddNudgeForDate: ((dateLabel: String) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val context = LocalContext.current

    var showEventNotificationDialog by remember { mutableStateOf(false) }
    var eventNotifMode by remember {
        mutableStateOf(NudgeEventNotificationScheduler.getEventNotificationMode(context))
    }
    var eventNotifDays by remember {
        mutableStateOf(NudgeEventNotificationScheduler.getEventNotificationDays(context))
    }
    var eventNotifHour by remember {
        mutableStateOf(NudgeEventNotificationScheduler.getCustomNotificationHour(context))
    }
    var eventNotifMinute by remember {
        mutableStateOf(NudgeEventNotificationScheduler.getCustomNotificationMinute(context))
    }

    if (showEventNotificationDialog) {
        EventNotificationSettingDialog(
            currentMode = eventNotifMode,
            currentDays = eventNotifDays,
            currentHour = eventNotifHour,
            currentMinute = eventNotifMinute,
            onDismiss = { showEventNotificationDialog = false },
            onSettingSaved = { newMode, newDays ->
                eventNotifMode = newMode
                eventNotifDays = newDays
                eventNotifHour = NudgeEventNotificationScheduler.getCustomNotificationHour(context)
                eventNotifMinute = NudgeEventNotificationScheduler.getCustomNotificationMinute(context)
            }
        )
    }

    val fullDateFormatted = remember(dateCalendar.timeInMillis) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(dateCalendar.time)
    }
    val dateLabelForNudge = remember(dateCalendar.timeInMillis) {
        val today = Calendar.getInstance()
        if (today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today"
        } else {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            if (tomorrow.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
            ) {
                "Tomorrow"
            } else {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (dateCalendar.get(Calendar.YEAR) == currentYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(dateCalendar.time)
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(dateCalendar.time)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        modifier = Modifier.testTag("calendar_event_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Row with Date and Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (events.size > 1) "EVENTS & OBSERVANCES" else "EVENT & OBSERVANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = NudgeBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fullDateFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showEventNotificationDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("event_notification_settings_button")
                    ) {
                        Icon(
                            imageVector = if (eventNotifMode != EventNotificationMode.OFF) {
                                Icons.Filled.Notifications
                            } else {
                                Icons.Outlined.Notifications
                            },
                            contentDescription = "Event Notification Settings",
                            tint = if (eventNotifMode != EventNotificationMode.OFF) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("calendar_event_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close event sheet",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of event cards (Scrollable if multiple events or on smaller screens)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
            ) {
                events.forEachIndexed { index, event ->
                    val cardBg = if (isDark) event.category.darkBackgroundColor else event.category.lightBackgroundColor
                    val titleColor = if (isDark) event.category.darkTextColor else event.category.lightTextColor
                    val badgeBg = if (isDark) event.category.darkBadgeBackgroundColor else event.category.badgeBackgroundColor
                    val badgeText = if (isDark) event.category.darkBadgeTextColor else event.category.badgeTextColor

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_event_card_$index"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBg
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 0.8.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(badgeBg)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Category Pill Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = event.category.displayName.uppercase(Locale.US),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = badgeText
                                    )
                                }

                                if (!event.traditionOrRegion.isNullOrBlank()) {
                                    Text(
                                        text = "• ${event.traditionOrRegion}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Event Title
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    lineHeight = 21.sp
                                ),
                                color = titleColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Short Description
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.5.sp
                                ),
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Always visible and anchored with comfortable spacing and insets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAddNudgeForDate != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddNudgeForDate(dateLabelForNudge)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("calendar_event_add_nudge_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NudgeBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add nudge for date",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("calendar_event_got_it_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NudgeBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Got it",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
