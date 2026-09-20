package com.example.gentlenudge.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Today
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.data.events.EventCategory
import com.example.gentlenudge.data.events.HinduCalendarCalculator
import com.example.gentlenudge.data.events.NudgeCalendarEvent
import com.example.gentlenudge.data.events.NudgeEventsRepository
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.data.model.withMonthColor
import com.example.gentlenudge.notification.EventNotificationMode
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import com.example.gentlenudge.ui.theme.ImportantDot
import com.example.gentlenudge.ui.theme.NudgeBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Saffron & Warm Vedic theme accents (Light pastel with dark brown text)
private val HinduSaffronBgLight = Color(0xFFFFF8EC)
private val HinduSaffronTextLight = Color(0xFF4E2600)
private val HinduSaffronBadgeBg = Color(0xFFFFE4BF)
private val HinduSaffronHeaderAccent = Color(0xFF7A3E00)

// Hindu Dark theme counterparts (Dark warm amber/brown with soft gold text)
private val HinduSaffronBgDark = Color(0xFF2B2117)
private val HinduSaffronBorderDark = Color(0xFF523D26)
private val HinduSaffronTextDark = Color(0xFFFFD180)
private val HinduSaffronHeaderAccentDark = Color(0xFFFFD180)
private val HinduSaffronDescDark = Color(0xFFFFE0B2).copy(alpha = 0.9f)

// International & World Day accents (Light cyan with dark teal text)
private val IntlBlueBgLight = Color(0xFFF0F9FB)
private val IntlBlueTextLight = Color(0xFF053F4D)
private val IntlBlueBadgeBg = Color(0xFFD2EFF5)
private val IntlBlueHeaderAccent = Color(0xFF075567)

// International Dark theme counterparts (Dark cyan/teal with soft cyan text)
private val IntlBlueBgDark = Color(0xFF13282D)
private val IntlBlueBorderDark = Color(0xFF234B54)
private val IntlBlueTextDark = Color(0xFF80DEEA)
private val IntlBlueHeaderAccentDark = Color(0xFF80DEEA)
private val IntlBlueDescDark = Color(0xFFE0F7FA).copy(alpha = 0.9f)

// Note slate accents (Light neutral slate with dark gray text)
private val NoteSlateBgLight = Color(0xFFF6F5F2)
private val NoteSlateTextLight = Color(0xFF343A40)
private val NoteSlateBadgeBg = Color(0xFFE5E3DC)

// Note slate Dark theme counterparts
private val NoteSlateBgDark = Color(0xFF282520)
private val NoteSlateBorderDark = Color(0xFF3D3932)
private val NoteSlateTextDark = Color(0xFFF0ECE3)
private val NoteSlateHeaderAccentDark = Color(0xFF90CAF9)

// Reminder purple accents (Light lavender with dark purple text)
private val ReminderBgLight = Color(0xFFF7F2FC)
private val ReminderTextLight = Color(0xFF3B1266)
private val ReminderBadgeBg = Color(0xFFE8DBFA)
private val ReminderHeaderAccent = Color(0xFF5B21B6)

// Reminder Dark theme counterparts
private val ReminderBgDark = Color(0xFF261D30)
private val ReminderBorderDark = Color(0xFF443357)
private val ReminderTextDark = Color(0xFFCE93D8)
private val ReminderHeaderAccentDark = Color(0xFFCE93D8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOverviewSheet(
    initialCalendar: Calendar,
    allTasks: List<NudgeTask> = emptyList(),
    allTimeGoals: List<TimeGoal> = emptyList(),
    allTimeGoalRecords: List<TimeGoalRecord> = emptyList(),
    monthColorsMap: Map<Pair<Long, String>, String> = emptyMap(),
    onDismiss: () -> Unit,
    onAddNudgeForDate: ((dateLabel: String) -> Unit)? = null,
    onToggleTaskDone: ((NudgeTask) -> Unit)? = null,
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

    // Selected date state inside the overview
    var selectedCal by remember(initialCalendar.timeInMillis) {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialCalendar.timeInMillis })
    }

    // Month view state for the calendar grid
    var viewMonthCal by remember(initialCalendar.timeInMillis) {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = initialCalendar.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    val todayCal = remember { Calendar.getInstance() }

    // Date formatting
    val fullDateFormatted = remember(selectedCal.timeInMillis) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(selectedCal.time)
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Calculate Hindu Lunar Date information for the selected date
    val hinduInfo = remember(selectedCal.timeInMillis) {
        HinduCalendarCalculator.calculateForCalendar(selectedCal)
    }

    // Events for selected date
    val selYear = selectedCal.get(Calendar.YEAR)
    val selMonth = selectedCal.get(Calendar.MONTH) + 1
    val selDay = selectedCal.get(Calendar.DAY_OF_MONTH)

    val hinduEvents = remember(selYear, selMonth, selDay) {
        NudgeEventsRepository.getHinduEventsForDate(selYear, selMonth, selDay)
    }

    val intlEvents = remember(selYear, selMonth, selDay) {
        NudgeEventsRepository.getInternationalAndWorldEventsForDate(selYear, selMonth, selDay)
    }

    // Category / Time Goal notes for the selected date
    val selectedDateStr = remember(selYear, selMonth, selDay) {
        String.format(Locale.US, "%04d-%02d-%02d", selYear, selMonth, selDay)
    }
    val selYearMonthStr = remember(selYear, selMonth) {
        String.format(Locale.US, "%04d-%02d", selYear, selMonth)
    }
    val selectedCategoryNotes = remember(selectedDateStr, selYearMonthStr, allTimeGoals, allTimeGoalRecords, monthColorsMap) {
        allTimeGoalRecords.filter { it.date == selectedDateStr && !it.note.isNullOrBlank() }
            .mapNotNull { rec ->
                val goal = allTimeGoals.firstOrNull { it.id == rec.goalId }
                if (goal != null) {
                    val resolvedGoal = goal.withMonthColor(selYearMonthStr, monthColorsMap)
                    Pair(resolvedGoal, rec)
                } else null
            }
    }

    // Filter tasks for this selected date
    val dateTasks = remember(selectedCal.timeInMillis, allTasks) {
        allTasks.filter { task -> isTaskOnDateHelper(task, selectedCal, context) }
    }

    // Differentiate user notes vs scheduled nudges/reminders
    // Untimed or "Any time" tasks function as day notes; timed tasks function as scheduled reminders
    val dateNotes = remember(dateTasks) {
        dateTasks.filter { it.timeLabel.equals("Any time", ignoreCase = true) || it.timeLabel.isBlank() }
    }

    val dateReminders = remember(dateTasks) {
        dateTasks.filter { !it.timeLabel.equals("Any time", ignoreCase = true) && it.timeLabel.isNotBlank() }
    }

    // Formatted date label for Composer pre-fill
    val dateLabelForAdd = remember(selectedCal.timeInMillis) {
        if (selectedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            selectedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today"
        } else {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            if (selectedCal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
            ) {
                "Tomorrow"
            } else {
                val currentYear = todayCal.get(Calendar.YEAR)
                if (selectedCal.get(Calendar.YEAR) == currentYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(selectedCal.time)
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedCal.time)
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
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("day_overview_sheet")
        ) {
            // Sheet Header with Title & Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAY OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = NudgeBlue
                    )
                    Text(
                        text = "Calendar & Notes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp
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
                        modifier = Modifier.testTag("event_notification_settings_button")
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
                        modifier = Modifier.testTag("day_overview_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Content Area: Monthly Calendar + Day Details
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                // 1. Full Monthly Calendar Component
                item {
                    FullMonthCalendarView(
                        viewMonthCalendar = viewMonthCal,
                        selectedCalendar = selectedCal,
                        todayCalendar = todayCal,
                        allTasks = allTasks,
                        allTimeGoals = allTimeGoals,
                        allTimeGoalRecords = allTimeGoalRecords,
                        monthColorsMap = monthColorsMap,
                        onMonthChange = { newMonthCal ->
                            viewMonthCal = newMonthCal
                        },
                        onDateSelect = { newDateCal ->
                            selectedCal = newDateCal
                            // Synchronize month view if user selected date in diff month
                            val newMonth = Calendar.getInstance().apply {
                                timeInMillis = newDateCal.timeInMillis
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            viewMonthCal = newMonth
                        }
                    )
                }

                // 2. Selected Date Header Banner & Hindu Lunar Details
                item {
                    SelectedDateHeaderCard(
                        formattedDate = fullDateFormatted,
                        hinduInfo = hinduInfo,
                        isDark = isDark
                    )
                }

                // 3. Status Badges / Small Indicators Row
                item {
                    OverviewIndicatorsRow(
                        hinduEventsCount = hinduEvents.size,
                        intlEventsCount = intlEvents.size,
                        notesCount = dateNotes.size + selectedCategoryNotes.size,
                        remindersCount = dateReminders.size,
                        isDark = isDark
                    )
                }

                // 4. Hindu Festivals & Observances Section (shown only if present)
                if (hinduEvents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "HINDU FESTIVALS & OBSERVANCES",
                            icon = Icons.Outlined.SelfImprovement,
                            accentColor = if (isDark) HinduSaffronHeaderAccentDark else HinduSaffronHeaderAccent,
                            count = hinduEvents.size
                        )
                    }

                    items(hinduEvents, key = { "hindu_${it.id}" }) { event ->
                        HinduEventCard(
                            event = event,
                            isDark = isDark
                        )
                    }
                }

                // 5. International & World Days Section (shown only if present)
                if (intlEvents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "INTERNATIONAL & WORLD DAYS",
                            icon = Icons.Outlined.Public,
                            accentColor = if (isDark) IntlBlueHeaderAccentDark else IntlBlueHeaderAccent,
                            count = intlEvents.size
                        )
                    }

                    items(intlEvents, key = { "intl_${it.id}" }) { event ->
                        InternationalEventCard(
                            event = event,
                            isDark = isDark
                        )
                    }
                }

                // 6. Category / Pursuit Notes for this date (shown if present)
                if (selectedCategoryNotes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "CATEGORY NOTES",
                            icon = Icons.Outlined.EditNote,
                            accentColor = selectedCategoryNotes.firstOrNull()?.first?.parseColor() ?: (if (isDark) NoteSlateHeaderAccentDark else NudgeBlue),
                            count = selectedCategoryNotes.size
                        )
                    }

                    items(selectedCategoryNotes, key = { "cat_note_${it.second.id}_${it.first.id}" }) { (goal, record) ->
                        CategoryNoteCard(
                            goal = goal,
                            record = record,
                            isDark = isDark
                        )
                    }
                }

                // 7. User Notes for this date (shown only if present)
                if (dateNotes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "YOUR NOTES FOR THIS DATE",
                            icon = Icons.Outlined.Description,
                            accentColor = if (isDark) NoteSlateHeaderAccentDark else NudgeBlue,
                            count = dateNotes.size
                        )
                    }

                    items(dateNotes, key = { "note_${it.id}" }) { noteTask ->
                        DayNoteItemCard(
                            task = noteTask,
                            onToggleDone = { onToggleTaskDone?.invoke(noteTask) },
                            isDark = isDark
                        )
                    }
                }

                // 8. Scheduled Reminders / Nudges for this date (shown only if present)
                if (dateReminders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "SCHEDULED NUDGES & REMINDERS",
                            icon = Icons.Outlined.Alarm,
                            accentColor = if (isDark) ReminderHeaderAccentDark else ReminderHeaderAccent,
                            count = dateReminders.size
                        )
                    }

                    items(dateReminders, key = { "rem_${it.id}" }) { reminderTask ->
                        DayReminderItemCard(
                            task = reminderTask,
                            onToggleDone = { onToggleTaskDone?.invoke(reminderTask) },
                            isDark = isDark
                        )
                    }
                }

                // 9. Serene Empty Day State (if nothing is scheduled or observed)
                if (hinduEvents.isEmpty() && intlEvents.isEmpty() && dateNotes.isEmpty() && dateReminders.isEmpty() && selectedCategoryNotes.isEmpty()) {
                    item {
                        QuietDayCard(
                            formattedDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(selectedCal.time)
                        )
                    }
                }
            }

            // Bottom Action Bar: "Add Nudge for [Date]" + "Done"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.35f)
                            .height(48.dp)
                            .testTag("day_overview_dismiss_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onAddNudgeForDate?.invoke(dateLabelForAdd)
                        },
                        modifier = Modifier
                            .weight(0.65f)
                            .height(48.dp)
                            .testTag("day_overview_add_nudge_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NudgeBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Nudge for $dateLabelForAdd",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Monthly Calendar view with previous/next month navigation, Mon-Sun grid,
 * and subtle indicator multi-dots for Hindu festivals, International days, Notes, and Reminders.
 */
@Composable
fun FullMonthCalendarView(
    viewMonthCalendar: Calendar,
    selectedCalendar: Calendar,
    todayCalendar: Calendar,
    allTasks: List<NudgeTask>,
    allTimeGoals: List<TimeGoal> = emptyList(),
    allTimeGoalRecords: List<TimeGoalRecord> = emptyList(),
    monthColorsMap: Map<Pair<Long, String>, String> = emptyMap(),
    onMonthChange: (Calendar) -> Unit,
    onDateSelect: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYearText = remember(viewMonthCalendar.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(viewMonthCalendar.time)
    }

    val currentYear = viewMonthCalendar.get(Calendar.YEAR)
    val currentMonth = viewMonthCalendar.get(Calendar.MONTH) // 0-based

    val currentMonthCal by rememberUpdatedState(viewMonthCalendar)
    val currentOnMonthChange by rememberUpdatedState(onMonthChange)
    val density = LocalDensity.current
    val context = LocalContext.current
    val swipeThresholdPx = with(density) { 40.dp.toPx() }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    // Calculate grid matrix: 7 columns (Mon to Sun)
    val calendarDays = remember(currentYear, currentMonth, allTasks, allTimeGoals, allTimeGoalRecords, monthColorsMap) {
        generateMonthDays(currentYear, currentMonth, allTasks, allTimeGoals, allTimeGoalRecords, monthColorsMap, context)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("full_month_calendar_card")
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                    },
                    onDragEnd = {
                        if (totalDragX < -swipeThresholdPx) {
                            // Swipe RIGHT -> LEFT: Move to NEXT month
                            val next = Calendar.getInstance().apply {
                                timeInMillis = currentMonthCal.timeInMillis
                                add(Calendar.MONTH, 1)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            currentOnMonthChange(next)
                        } else if (totalDragX > swipeThresholdPx) {
                            // Swipe LEFT -> RIGHT: Move to PREVIOUS month
                            val prev = Calendar.getInstance().apply {
                                timeInMillis = currentMonthCal.timeInMillis
                                add(Calendar.MONTH, -1)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            currentOnMonthChange(prev)
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = {
                        totalDragX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Month Header with navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = Calendar.getInstance().apply {
                            timeInMillis = viewMonthCalendar.timeInMillis
                            add(Calendar.MONTH, -1)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        onMonthChange(prev)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cal_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = monthYearText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Jump to Today
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NudgeBlue,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val now = Calendar.getInstance()
                                onDateSelect(now)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("cal_jump_today")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            val next = Calendar.getInstance().apply {
                                timeInMillis = viewMonthCalendar.timeInMillis
                                add(Calendar.MONTH, 1)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            onMonthChange(next)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cal_next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weekday Header Row (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
            val weekDayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Month Grid Weeks (rows of 7 days)
            calendarDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { dayCell ->
                        val isSelected = dayCell.isCurrentMonth &&
                                dayCell.year == selectedCalendar.get(Calendar.YEAR) &&
                                dayCell.month == selectedCalendar.get(Calendar.MONTH) &&
                                dayCell.dayOfMonth == selectedCalendar.get(Calendar.DAY_OF_MONTH)

                        val isToday = dayCell.year == todayCalendar.get(Calendar.YEAR) &&
                                dayCell.month == todayCalendar.get(Calendar.MONTH) &&
                                dayCell.dayOfMonth == todayCalendar.get(Calendar.DAY_OF_MONTH)

                        MonthDayCell(
                            dayCell = dayCell,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = {
                                val target = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, dayCell.year)
                                    set(Calendar.MONTH, dayCell.month)
                                    set(Calendar.DAY_OF_MONTH, dayCell.dayOfMonth)
                                }
                                onDateSelect(target)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

data class MonthDayData(
    val year: Int,
    val month: Int, // 0-based
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val hasHinduEvent: Boolean,
    val hasIntlEvent: Boolean,
    val hasNotes: Boolean,
    val hasReminders: Boolean,
    val categoryNoteColors: List<Color> = emptyList()
)

private fun generateMonthDays(
    year: Int,
    month: Int,
    allTasks: List<NudgeTask>,
    allTimeGoals: List<TimeGoal> = emptyList(),
    allTimeGoalRecords: List<TimeGoalRecord> = emptyList(),
    monthColorsMap: Map<Pair<Long, String>, String> = emptyMap(),
    context: Context? = null
): List<MonthDayData> {
    val result = mutableListOf<MonthDayData>()

    val firstDayCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val maxDayInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Java Calendar: SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
    // We want Monday = 0, Tuesday = 1, ..., Sunday = 6
    val firstDayOfWeekJava = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val leadingDays = (firstDayOfWeekJava + 5) % 7 // Monday-based offset

    // 1. Previous month leading days
    val prevMonthCal = Calendar.getInstance().apply {
        timeInMillis = firstDayCal.timeInMillis
        add(Calendar.MONTH, -1)
    }
    val maxDayPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prevYear = prevMonthCal.get(Calendar.YEAR)
    val prevMonth = prevMonthCal.get(Calendar.MONTH)

    for (d in (maxDayPrevMonth - leadingDays + 1)..maxDayPrevMonth) {
        result.add(
            MonthDayData(
                year = prevYear,
                month = prevMonth,
                dayOfMonth = d,
                isCurrentMonth = false,
                hasHinduEvent = false,
                hasIntlEvent = false,
                hasNotes = false,
                hasReminders = false,
                categoryNoteColors = emptyList()
            )
        )
    }

    // 2. Current month days
    val currentYmStr = String.format(Locale.US, "%04d-%02d", year, month + 1)
    for (d in 1..maxDayInMonth) {
        val hasHindu = NudgeEventsRepository.hasHinduEventOnDate(year, month + 1, d)
        val hasIntl = NudgeEventsRepository.hasInternationalEventOnDate(year, month + 1, d)

        val checkCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, d)
        }
        val dayTasks = allTasks.filter { isTaskOnDateHelper(it, checkCal, context) }
        val hasNotes = dayTasks.any { it.timeLabel.equals("Any time", ignoreCase = true) || it.timeLabel.isBlank() }
        val hasReminders = dayTasks.any { !it.timeLabel.equals("Any time", ignoreCase = true) && it.timeLabel.isNotBlank() }

        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, d)
        val categoryColors = allTimeGoalRecords
            .filter { it.date == dateStr && !it.note.isNullOrBlank() }
            .mapNotNull { rec ->
                val goal = allTimeGoals.firstOrNull { it.id == rec.goalId }
                goal?.withMonthColor(currentYmStr, monthColorsMap)?.parseColor()
            }
            .distinct()

        result.add(
            MonthDayData(
                year = year,
                month = month,
                dayOfMonth = d,
                isCurrentMonth = true,
                hasHinduEvent = hasHindu,
                hasIntlEvent = hasIntl,
                hasNotes = hasNotes,
                hasReminders = hasReminders,
                categoryNoteColors = categoryColors
            )
        )
    }

    // 3. Trailing days to fill 5 or 6 weeks (multiple of 7)
    val trailingDays = (7 - (result.size % 7)) % 7
    val nextMonthCal = Calendar.getInstance().apply {
        timeInMillis = firstDayCal.timeInMillis
        add(Calendar.MONTH, 1)
    }
    val nextYear = nextMonthCal.get(Calendar.YEAR)
    val nextMonth = nextMonthCal.get(Calendar.MONTH)

    for (d in 1..trailingDays) {
        result.add(
            MonthDayData(
                year = nextYear,
                month = nextMonth,
                dayOfMonth = d,
                isCurrentMonth = false,
                hasHinduEvent = false,
                hasIntlEvent = false,
                hasNotes = false,
                hasReminders = false,
                categoryNoteColors = emptyList()
            )
        )
    }

    return result
}

@Composable
fun MonthDayCell(
    dayCell: MonthDayData,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellTextColor = when {
        isSelected -> Color.White
        !dayCell.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        isToday -> NudgeBlue
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NudgeBlue else Color.Transparent)
            .border(
                width = if (isToday && !isSelected) 1.2.dp else 0.dp,
                color = if (isToday && !isSelected) NudgeBlue.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${dayCell.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = cellTextColor
            )

            // Multi-dot indicators under the date number
            if (dayCell.isCurrentMonth) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dayCell.hasHinduEvent) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFD54F) else Color(0xFFE65100))
                        )
                    }
                    if (dayCell.hasIntlEvent) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF80DEEA) else Color(0xFF00838F))
                        )
                    }
                    if (dayCell.categoryNoteColors.isNotEmpty()) {
                        dayCell.categoryNoteColors.forEach { catColor ->
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                        }
                    } else if (dayCell.hasNotes) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF78909C))
                        )
                    }
                    if (dayCell.hasReminders) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFCE93D8) else Color(0xFF7E57C2))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header card showing the complete selected Gregorian date and accurate Hindu lunar date.
 */
@Composable
fun SelectedDateHeaderCard(
    formattedDate: String,
    hinduInfo: HinduCalendarCalculator.HinduDateInfo,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_date_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF24201A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.8.dp,
            brush = SolidColor(if (isDark) Color(0xFF423528) else MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "SELECTED DATE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hindu Luni-Solar Calendar detail badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF332619) else Color(0xFFFFF3E0))
                    .border(0.8.dp, if (isDark) Color(0xFF523D26) else Color(0xFFFFD180), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✦",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isDark) Color(0xFFFFD180) else Color(0xFF7A3B00)
                    )
                    Text(
                        text = "${hinduInfo.masaName} ${hinduInfo.paksha} ${hinduInfo.tithiName} • Vikram Samvat ${hinduInfo.vikramSamvat} • ${hinduInfo.rituName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = if (isDark) Color(0xFFFFE0B2) else Color(0xFF4A2500)
                    )
                }
            }
        }
    }
}

/**
 * 4 Pill Indicators showing counts for Hindu events, International days, Notes, and Reminders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewIndicatorsRow(
    hinduEventsCount: Int,
    intlEventsCount: Int,
    notesCount: Int,
    remindersCount: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IndicatorPill(
            icon = Icons.Outlined.SelfImprovement,
            label = "Hindu Event",
            count = hinduEventsCount,
            bgColor = if (isDark) Color(0xFF352718) else HinduSaffronBgLight,
            textColor = if (isDark) Color(0xFFFFD180) else HinduSaffronTextLight,
            borderColor = if (isDark) Color(0xFF523D26) else null
        )

        IndicatorPill(
            icon = Icons.Outlined.Public,
            label = "World Day",
            count = intlEventsCount,
            bgColor = if (isDark) Color(0xFF132B30) else IntlBlueBgLight,
            textColor = if (isDark) Color(0xFF80DEEA) else IntlBlueTextLight,
            borderColor = if (isDark) Color(0xFF234B54) else null
        )

        IndicatorPill(
            icon = Icons.Outlined.Description,
            label = "User Note",
            count = notesCount,
            bgColor = if (isDark) Color(0xFF282520) else NoteSlateBgLight,
            textColor = if (isDark) Color(0xFFF0ECE3) else NoteSlateTextLight,
            borderColor = if (isDark) Color(0xFF3D3932) else null
        )

        IndicatorPill(
            icon = Icons.Outlined.Alarm,
            label = "Reminder",
            count = remindersCount,
            bgColor = if (isDark) Color(0xFF261D30) else ReminderBgLight,
            textColor = if (isDark) Color(0xFFCE93D8) else ReminderTextLight,
            borderColor = if (isDark) Color(0xFF443357) else null
        )
    }
}

@Composable
fun IndicatorPill(
    icon: ImageVector,
    label: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(0.8.dp, borderColor, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "$label${if (count > 0) " ($count)" else " (0)"}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = accentColor
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = accentColor
            )
        }
    }
}

@Composable
fun EventBadge(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp
            ),
            color = textColor,
            softWrap = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventCardHeader(
    title: String,
    badgeText: String,
    titleColor: Color,
    badgeBgColor: Color,
    badgeTextColor: Color,
    titleFontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                lineHeight = 22.sp
            ),
            color = titleColor,
            modifier = Modifier.padding(end = 8.dp)
        )

        EventBadge(
            text = badgeText,
            bgColor = badgeBgColor,
            textColor = badgeTextColor
        )
    }
}

@Composable
fun HinduEventCard(
    event: NudgeCalendarEvent,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) HinduSaffronBgDark else HinduSaffronBgLight
    val borderBrush = SolidColor(if (isDark) HinduSaffronBorderDark else HinduSaffronBadgeBg)
    val titleColor = if (isDark) HinduSaffronTextDark else HinduSaffronTextLight
    val badgeBg = if (isDark) HinduSaffronBorderDark else HinduSaffronBadgeBg
    val badgeText = if (isDark) HinduSaffronTextDark else HinduSaffronTextLight
    val descColor = if (isDark) HinduSaffronDescDark else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hindu_event_card_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = borderBrush
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            EventCardHeader(
                title = event.name,
                badgeText = event.traditionOrRegion ?: "Hindu Festival",
                titleColor = titleColor,
                badgeBgColor = badgeBg,
                badgeTextColor = badgeText,
                titleFontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = descColor
            )
        }
    }
}

@Composable
fun InternationalEventCard(
    event: NudgeCalendarEvent,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) IntlBlueBgDark else event.category.lightBackgroundColor
    val titleColor = if (isDark) IntlBlueTextDark else event.category.lightTextColor
    val badgeBg = if (isDark) IntlBlueBorderDark else event.category.badgeBackgroundColor
    val badgeText = if (isDark) IntlBlueTextDark else event.category.badgeTextColor
    val descColor = if (isDark) IntlBlueDescDark else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intl_event_card_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.8.dp,
            brush = SolidColor(badgeBg)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            EventCardHeader(
                title = event.name,
                badgeText = event.category.displayName,
                titleColor = titleColor,
                badgeBgColor = badgeBg,
                badgeTextColor = badgeText,
                titleFontSize = 15.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = descColor
            )
        }
    }
}

@Composable
fun DayNoteItemCard(
    task: NudgeTask,
    onToggleDone: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) NoteSlateBgDark else NoteSlateBgLight
    val borderBrush = SolidColor(if (isDark) NoteSlateBorderDark else NoteSlateBadgeBg)
    val titleColor = if (task.isDone) {
        if (isDark) Color(0xFF8E887E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        if (isDark) NoteSlateTextDark else MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_note_item_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.8.dp,
            brush = borderBrush
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (task.isDone) "Mark not done" else "Mark done",
                    tint = if (task.isDone) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = titleColor
                )

                if (task.priority == "Important") {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ImportantDot)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayReminderItemCard(
    task: NudgeTask,
    onToggleDone: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) ReminderBgDark else ReminderBgLight
    val borderBrush = SolidColor(if (isDark) ReminderBorderDark else ReminderBadgeBg)
    val titleColor = if (task.isDone) {
        if (isDark) Color(0xFF8E887E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        if (isDark) Color(0xFFF0ECE3) else MaterialTheme.colorScheme.onSurface
    }
    val timePillBg = if (isDark) ReminderBorderDark else ReminderBadgeBg
    val timePillText = if (isDark) ReminderTextDark else ReminderTextLight

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_reminder_item_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.8.dp,
            brush = borderBrush
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (task.isDone) "Mark not done" else "Mark done",
                    tint = if (task.isDone) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = titleColor
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(timePillBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = timePillText,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = task.timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = timePillText
                            )
                        )
                    }

                    if (task.priority == "Important") {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ImportantDot)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuietDayCard(
    formattedDate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiet_day_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A calm and open day",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "No scheduled reminders, notes, or major observances for $formattedDate.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Standardized date matching helper for tasks.
 */
fun isTaskOnDateHelper(task: NudgeTask, targetCal: Calendar, context: Context? = null): Boolean {
    val targetYear = targetCal.get(Calendar.YEAR)
    val targetMonth = targetCal.get(Calendar.MONTH)
    val targetDay = targetCal.get(Calendar.DAY_OF_MONTH)

    val todayCal = Calendar.getInstance()
    val isTargetToday = targetYear == todayCal.get(Calendar.YEAR) &&
            targetMonth == todayCal.get(Calendar.MONTH) &&
            targetDay == todayCal.get(Calendar.DAY_OF_MONTH)

    // Authoritative occurrence resolution
    val occurrenceMillis = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context)
    if (occurrenceMillis > 0L) {
        val occCal = Calendar.getInstance().apply { timeInMillis = occurrenceMillis }
        if (occCal.get(Calendar.YEAR) == targetYear &&
            occCal.get(Calendar.MONTH) == targetMonth &&
            occCal.get(Calendar.DAY_OF_MONTH) == targetDay
        ) {
            return true
        }
    }

    // Overdue or pending tasks scheduled for today or earlier belong in today's view
    if (isTargetToday && !task.isDone && !task.isDeleted && occurrenceMillis > 0L) {
        if (TaskOccurrenceResolver.isOccurrenceTodayOrPast(occurrenceMillis)) {
            return true
        }
    }

    val dateFormats = listOf(
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM d", Locale.getDefault()),
        SimpleDateFormat("MMMM d", Locale.getDefault()),
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()),
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()),
        SimpleDateFormat("d MMM", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy", Locale.US),
        SimpleDateFormat("MMM d", Locale.US)
    )
    for (fmt in dateFormats) {
        try {
            val parsed = fmt.parse(task.dateLabel.trim())
            if (parsed != null) {
                val parsedCal = Calendar.getInstance().apply { time = parsed }
                // Default to current year if year wasn't specified in pattern
                val parsedYear = if (parsedCal.get(Calendar.YEAR) <= 1970) targetYear else parsedCal.get(Calendar.YEAR)
                if (parsedYear == targetYear &&
                    parsedCal.get(Calendar.MONTH) == targetMonth &&
                    parsedCal.get(Calendar.DAY_OF_MONTH) == targetDay
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}
    }

    return false
}

@Composable
fun CategoryNoteCard(
    goal: TimeGoal,
    record: TimeGoalRecord,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val goalColor = goal.parseColor()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_note_card_${record.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = goalColor.copy(alpha = if (isDark) 0.16f else 0.08f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.8.dp,
            brush = androidx.compose.ui.graphics.SolidColor(goalColor.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = goal.getIcon(),
                            contentDescription = goal.name,
                            tint = goalColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = goalColor
                    )
                }

                if (record.actualMinutes > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(goalColor.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = TimeGoalCalculations.formatMinutes(record.actualMinutes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = goalColor
                        )
                    }
                }
            }

            record.note?.let { noteText ->
                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
