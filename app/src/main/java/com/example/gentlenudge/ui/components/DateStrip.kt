package com.example.gentlenudge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.data.events.NudgeCalendarEvent
import com.example.gentlenudge.data.events.NudgeEventsRepository
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StripDate(
    val year: Int,
    val month: Int, // 0-based Calendar.MONTH
    val dayOfMonth: Int,
    val dayOfWeek: String, // e.g. "Mon", "Tue"
    val isToday: Boolean,
    val dateInMillis: Long,
    val events: List<NudgeCalendarEvent> = emptyList()
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
}

@Composable
fun DateStrip(
    selectedCalendar: Calendar,
    onDateSelected: (Calendar) -> Unit,
    datesWithTasks: Set<String> = emptySet(), // formatted "yyyy-MM-dd"
    onEventDateSelected: ((events: List<NudgeCalendarEvent>, date: Calendar) -> Unit)? = null,
    onOpenDayOverview: ((date: Calendar) -> Unit)? = null,
    currentDateMillis: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Generate date sequence: 14 days before today up to 45 days after today (total 60 days)
    val todayCal = remember(currentDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val pastDays = 14
    val futureDays = 45

    val dateList = remember(todayCal.timeInMillis) {
        val list = mutableListOf<StripDate>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayCal.timeInMillis
            add(Calendar.DAY_OF_YEAR, -pastDays)
        }

        for (i in 0..(pastDays + futureDays)) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) // 0-based
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val isToday = y == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            val dayEvents = NudgeEventsRepository.getEventsForDate(y, m + 1, d)

            list.add(
                StripDate(
                    year = y,
                    month = m,
                    dayOfMonth = d,
                    dayOfWeek = dayFormat.format(cal.time),
                    isToday = isToday,
                    dateInMillis = cal.timeInMillis,
                    events = dayEvents
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val todayIndex = pastDays

    // Scroll to today's date on initial composition or when day rolls over
    LaunchedEffect(todayCal.timeInMillis) {
        val targetIndex = (todayIndex - 2).coerceAtLeast(0)
        listState.scrollToItem(targetIndex)
    }

    // Is selected date today?
    val isSelectedToday = selectedCalendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            selectedCalendar.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

    // Current month/year heading based on selected date
    val currentMonthYearText = remember(selectedCalendar.timeInMillis) {
        monthYearFormat.format(selectedCalendar.time)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("date_strip_container")
    ) {
        // Header: Month & Year + "Today" quick jump button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onOpenDayOverview?.invoke(selectedCalendar)
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("date_strip_month_title"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentMonthYearText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Today,
                    contentDescription = "Open Calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(
                visible = !isSelectedToday,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NudgeBlueContainer)
                        .clickable {
                            val resetCal = Calendar.getInstance()
                            onDateSelected(resetCal)
                            coroutineScope.launch {
                                val target = (todayIndex - 2).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("date_strip_jump_today_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Today,
                        contentDescription = "Jump to Today",
                        tint = NudgeBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = NudgeBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal scrollable dates strip
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("date_strip_row"),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(dateList, key = { _, item -> "${item.year}_${item.month}_${item.dayOfMonth}" }) { index, item ->
                val isSelected = item.year == selectedCalendar.get(Calendar.YEAR) &&
                        item.month == selectedCalendar.get(Calendar.MONTH) &&
                        item.dayOfMonth == selectedCalendar.get(Calendar.DAY_OF_MONTH)

                val dateKey = String.format(Locale.US, "%04d-%02d-%02d", item.year, item.month + 1, item.dayOfMonth)
                val hasTasks = datesWithTasks.contains(dateKey)

                DateStripItem(
                    date = item,
                    isSelected = isSelected,
                    hasTasks = hasTasks,
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = item.dateInMillis
                        }
                        onDateSelected(newCal)

                        // Open Day Overview when user taps any date
                        onOpenDayOverview?.invoke(newCal)

                        // Backward compatibility for event callback
                        if (item.hasEvents) {
                            onEventDateSelected?.invoke(item.events, newCal)
                        }

                        coroutineScope.launch {
                            val target = (index - 2).coerceAtLeast(0)
                            listState.animateScrollToItem(target)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DateStripItem(
    date: StripDate,
    isSelected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        NudgeBlue
    } else {
        MaterialTheme.colorScheme.surface
    }

    val dayNameColor = if (isSelected) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val dayNumberColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        NudgeBlue
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    // Subtle warm amber color for event indicator dot
    val eventDotColor = if (isSelected) {
        Color(0xFFFFD54F) // Warm golden white on blue
    } else {
        Color(0xFFE67E22) // Subtle warm amber dot on clean surface
    }

    val taskDotColor = if (isSelected) {
        Color.White.copy(alpha = 0.9f)
    } else {
        NudgeBlue
    }

    Box(
        modifier = modifier
            .width(50.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("date_strip_item_${date.dayOfMonth}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            // Day of week abbreviation (Mon, Tue, etc.)
            Text(
                text = date.dayOfWeek,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                color = dayNameColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Day number (21, 22, 23, etc.)
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = dayNumberColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Tiny subtle indicator underneath date
            // Requirements:
            // - Date with events: display ONLY a tiny subtle indicator
            // - Do not display event name on calendar
            // - Dates with no event / tasks: clean space
            if (date.hasEvents && hasTasks) {
                // Both Event & Task: two tiny subtle dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("date_indicator_both_${date.dayOfMonth}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(eventDotColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(taskDotColor)
                    )
                }
            } else if (date.hasEvents) {
                // Event only: single tiny subtle event indicator dot
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(eventDotColor)
                        .testTag("date_indicator_event_${date.dayOfMonth}")
                )
            } else if (hasTasks || (date.isToday && !isSelected)) {
                // Task only or Today dot
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(taskDotColor)
                        .testTag("date_indicator_task_${date.dayOfMonth}")
                )
            } else {
                // Clean date: no indicator
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}
