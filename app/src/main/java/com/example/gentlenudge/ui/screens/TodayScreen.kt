package com.example.gentlenudge.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.R
import com.example.gentlenudge.data.events.NudgeCalendarEvent
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.ui.components.CalendarEventSheet
import com.example.gentlenudge.ui.components.DateStrip
import com.example.gentlenudge.ui.components.DayOverviewSheet
import com.example.gentlenudge.ui.components.TaskOccurrenceResolver
import com.example.gentlenudge.ui.components.TaskSlipItem
import com.example.gentlenudge.ui.components.isTaskOnDateHelper
import com.example.gentlenudge.ui.theme.ImportantDot
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import com.example.gentlenudge.ui.theme.TagHomeBg
import com.example.gentlenudge.ui.theme.TagHomeText
import com.example.gentlenudge.ui.theme.TagOtherBg
import com.example.gentlenudge.ui.theme.TagOtherText
import com.example.gentlenudge.ui.theme.TagPersonalBg
import com.example.gentlenudge.ui.theme.TagPersonalText
import com.example.gentlenudge.ui.theme.TagShoppingBg
import com.example.gentlenudge.ui.theme.TagShoppingText
import com.example.gentlenudge.ui.theme.TagWorkBg
import com.example.gentlenudge.ui.theme.TagWorkText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    todayTasks: List<NudgeTask>,
    laterTasks: List<NudgeTask>,
    allTasks: List<NudgeTask> = emptyList(),
    allTimeGoals: List<TimeGoal> = emptyList(),
    allTimeGoalRecords: List<TimeGoalRecord> = emptyList(),
    monthColorsMap: Map<Pair<Long, String>, String> = emptyMap(),
    currentDateMillis: Long = System.currentTimeMillis(),
    onToggleDone: (NudgeTask) -> Unit,
    onSnooze: (NudgeTask, String) -> Unit,
    onDelete: (NudgeTask) -> Unit,
    onEdit: (NudgeTask) -> Unit,
    onOpenComposer: (draftText: String, dateLabel: String?) -> Unit,
    onQuickAdd: (title: String, dateLabel: String) -> Unit,
    onStartVoice: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val currentDate = remember(currentDateMillis) { Date(currentDateMillis) }
    val currentDay = remember(currentDateMillis) { dayFormat.format(currentDate) }
    val currentMonth = remember(currentDateMillis) { monthFormat.format(currentDate).uppercase() }

    var selectedCalendar by remember(currentDateMillis) {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = currentDateMillis })
    }
    var showDayOverviewCalendar by remember { mutableStateOf<Calendar?>(null) }

    val todayCal = remember(currentDateMillis) {
        Calendar.getInstance().apply { timeInMillis = currentDateMillis }
    }

    // Build dates with active tasks to show subtle dots
    val datesWithTasks = remember(allTasks, currentDateMillis) {
        val set = mutableSetOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        allTasks.filter { !it.isDone }.forEach { task ->
            val trigger = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, currentDateMillis)
            val cal = Calendar.getInstance().apply { timeInMillis = trigger }
            set.add(sdf.format(cal.time))
        }
        set
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Parchment Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    width = 1.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg_hero_leaf_shadow),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Content Column
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "What deserves",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "your attention?",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 34.sp
                                ),
                                color = NudgeBlue
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "A little reminder can go a long way.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Date circular badge on the top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NudgeBlue)
                                    .clickable {
                                        showDayOverviewCalendar = todayCal
                                    }
                                    .testTag("today_screen_date_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentDay,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentMonth,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.8.sp,
                                            lineHeight = 10.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Date Strip (Month-aware, compact 7-day calendar selector with event indicators)
        item {
            DateStrip(
                selectedCalendar = selectedCalendar,
                currentDateMillis = currentDateMillis,
                onDateSelected = { newCal ->
                    selectedCalendar = newCal
                    showDayOverviewCalendar = newCal
                },
                datesWithTasks = datesWithTasks,
                onOpenDayOverview = { targetCal ->
                    selectedCalendar = targetCal
                    showDayOverviewCalendar = targetCal
                }
            )
        }

        // Quick Capture Bar - Main Focus
        item {
            QuickCaptureBar(
                onOpenComposer = { draft -> onOpenComposer(draft, "Today") },
                onQuickAdd = { title -> onQuickAdd(title, "Today") },
                onStartVoice = onStartVoice,
                isListening = isListening
            )
        }

        // Reminders Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S NUDGES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Keep these close",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (todayTasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NudgeBlueContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${todayTasks.size} to do",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = NudgeBlue
                        )
                    }
                }
            }
        }

        // Reminders List or Calm Empty State
        if (todayTasks.isEmpty()) {
            item {
                EmptyNudgeCard(
                    message = "Nothing to remember right now."
                )
            }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                TaskSlipItem(
                    task = task,
                    onToggleDone = { onToggleDone(task) },
                    onSnooze = { label -> onSnooze(task, label) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) }
                )
            }
        }

        // Later Section (Shown when there are later tasks)
        if (laterTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                    Text(
                        text = "LATER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Coming up",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(laterTasks, key = { it.id }) { task ->
                TaskSlipItem(
                    task = task,
                    onToggleDone = { onToggleDone(task) },
                    onSnooze = { label -> onSnooze(task, label) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) }
                )
            }
        }

        // Standard bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Day Overview Screen / Sheet
    val overviewCal = showDayOverviewCalendar
    if (overviewCal != null) {
        DayOverviewSheet(
            initialCalendar = overviewCal,
            allTasks = allTasks,
            allTimeGoals = allTimeGoals,
            allTimeGoalRecords = allTimeGoalRecords,
            monthColorsMap = monthColorsMap,
            onDismiss = {
                showDayOverviewCalendar = null
            },
            onAddNudgeForDate = { dateLabel ->
                showDayOverviewCalendar = null
                onOpenComposer("", dateLabel)
            },
            onToggleTaskDone = onToggleDone
        )
    }
}

private fun isTaskOnDate(task: NudgeTask, targetCal: Calendar): Boolean {
    return isTaskOnDateHelper(task, targetCal)
}

@Composable
fun EmptyNudgeCard(
    message: String,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outline),
            width = 1.dp
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickCaptureBar(
    onOpenComposer: (draftText: String) -> Unit,
    onQuickAdd: (title: String) -> Unit,
    onStartVoice: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // LEFT: Microphone / voice recording button
        IconButton(
            onClick = onStartVoice,
            modifier = Modifier
                .size(40.dp)
                .testTag("quick_capture_mic_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening..." else "Speak reminder",
                tint = if (isListening) ImportantDot else NudgeBlue
            )
        }

        // MIDDLE: Text / input area
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .testTag("quick_capture_input"),
            placeholder = {
                Text(
                    text = "Add a quick thought...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onQuickAdd(text.trim())
                        text = ""
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        // RIGHT: Plus button
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onQuickAdd(text.trim())
                    text = ""
                } else {
                    onOpenComposer("")
                }
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NudgeBlue)
                .testTag("quick_capture_submit_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add thought",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


