package com.example.gentlenudge.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gentlenudge.data.model.DayCompletionState
import com.example.gentlenudge.data.model.DayProgress
import com.example.gentlenudge.data.model.MonthlyGoalProgress
import com.example.gentlenudge.data.model.MonthlyOverallProgress
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.data.model.withMonthColor
import com.example.gentlenudge.data.model.withMonthColors
import com.example.gentlenudge.export.TimeGoalsPdfExporter
import com.example.gentlenudge.export.TimeGoalsPdfOptions
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import com.example.gentlenudge.ui.theme.PaperBackgroundLight
import com.example.gentlenudge.ui.theme.PaperBorderLight
import com.example.gentlenudge.ui.theme.PaperCardLight
import com.example.gentlenudge.ui.theme.TextPrimaryLight
import com.example.gentlenudge.ui.theme.TextSecondaryLight
import com.example.gentlenudge.ui.viewmodel.NudgeViewModel
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HoursTab(val label: String) {
    DASHBOARD("Dashboard"),
    PAST_MONTHS("Past Months"),
    EXPORT("Export PDF")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeInHoursScreen(
    viewModel: NudgeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeGoals by viewModel.activeTimeGoals.collectAsStateWithLifecycle()
    val allGoals by viewModel.allTimeGoals.collectAsStateWithLifecycle()
    val allTimeGoalRecords by viewModel.allTimeGoalRecords.collectAsStateWithLifecycle()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val monthRecords by viewModel.selectedMonthRecords.collectAsStateWithLifecycle()
    val selectedGoalForDetail by viewModel.selectedGoalForDetail.collectAsStateWithLifecycle()
    val monthColorsMap by viewModel.monthColorsMap.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = HoursTab.DASHBOARD.ordinal,
        pageCount = { HoursTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val activeTab by remember {
        derivedStateOf { HoursTab.entries[pagerState.targetPage] }
    }

    var showCreateGoalSheet by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<TimeGoal?>(null) }
    var editingYearMonth by rememberSaveable { mutableStateOf(selectedYearMonth.toString()) }
    var goalForDayEntry by remember { mutableStateOf<Pair<TimeGoal, LocalDate>?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val isCurrentMonth = selectedYearMonth.year == today.year && selectedYearMonth.month == today.month

    val activeGoalsForSelectedMonth = remember(activeGoals, selectedYearMonth, monthColorsMap) {
        activeGoals.withMonthColors(selectedYearMonth, monthColorsMap)
    }
    val allGoalsForSelectedMonth = remember(allGoals, selectedYearMonth, monthColorsMap) {
        allGoals.withMonthColors(selectedYearMonth, monthColorsMap)
    }

    val overallProgress = remember(activeGoalsForSelectedMonth, selectedYearMonth, monthRecords) {
        TimeGoalCalculations.calculateMonthlyOverallProgress(
            goals = activeGoalsForSelectedMonth,
            yearMonth = selectedYearMonth,
            allRecords = monthRecords,
            today = today
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Screen Header
            LifeInHoursHeader(
                selectedYearMonth = selectedYearMonth,
                isCurrentMonth = isCurrentMonth,
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() },
                onCurrentMonth = { viewModel.goToCurrentMonth() },
                onMonthClick = { showMonthPicker = true }
            )

            // Sub-nav Tabs
            HoursSubNavTabs(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                }
            )

            // Content Area with Horizontal Swipe Navigation
            HorizontalPager(
                state = pagerState,
                key = { pageIndex -> HoursTab.entries[pageIndex].name },
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                when (HoursTab.entries[pageIndex]) {
                    HoursTab.DASHBOARD -> {
                        DashboardView(
                            overallProgress = overallProgress,
                            selectedYearMonth = selectedYearMonth,
                            today = today,
                            onGoalClick = { viewModel.setSelectedGoalForDetail(it) },
                            onAddGoalClick = {
                                editingYearMonth = selectedYearMonth.toString()
                                goalToEdit = null
                                showCreateGoalSheet = true
                            },
                            onQuickRecordToday = { goal, minutes ->
                                val dateStr = TimeGoalCalculations.formatLocalDate(today)
                                viewModel.recordDailyTime(goal.id, dateStr, minutes)
                            },
                            onOpenRecordSheet = { goal, date ->
                                goalForDayEntry = Pair(goal, date)
                            },
                            onReorderGoals = { reorderedGoals ->
                                viewModel.reorderTimeGoals(reorderedGoals)
                            }
                        )
                    }
                    HoursTab.PAST_MONTHS -> {
                        PastMonthsView(
                            selectedYearMonth = selectedYearMonth,
                            allGoals = allGoals,
                            allRecords = allTimeGoalRecords,
                            onSelectMonth = { ym ->
                                viewModel.setSelectedYearMonth(ym)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(HoursTab.DASHBOARD.ordinal)
                                }
                            }
                        )
                    }
                    HoursTab.EXPORT -> {
                        ExportPdfView(
                            selectedYearMonth = selectedYearMonth,
                            goals = activeGoalsForSelectedMonth,
                            records = monthRecords,
                            onExportPdf = { options ->
                                viewModel.showToast("Generating publication-grade PDF report…")
                                viewModel.exportTimeGoalsPdf(
                                    yearMonth = selectedYearMonth,
                                    options = options
                                ) { file ->
                                    TimeGoalsPdfExporter.sharePdf(context, file)
                                }
                            },
                            onExportMultiMonthPdf = { startMonth, endMonth, options ->
                                viewModel.showToast("Generating publication-grade multi-month PDF report…")
                                viewModel.exportMultiMonthTimeGoalsPdf(
                                    startMonth = startMonth,
                                    endMonth = endMonth,
                                    options = options
                                ) { file ->
                                    TimeGoalsPdfExporter.sharePdf(context, file)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Detail View Sheet for a Goal
        if (selectedGoalForDetail != null) {
            val rawGoal = selectedGoalForDetail!!
            var detailYearMonth by remember(rawGoal.id) { mutableStateOf(selectedYearMonth) }
            val goal = remember(rawGoal, detailYearMonth, monthColorsMap) {
                rawGoal.withMonthColor(detailYearMonth, monthColorsMap)
            }

            val detailYearMonthPrefix = detailYearMonth.toString()
            val recordsMap = remember(allTimeGoalRecords, goal.id, detailYearMonthPrefix) {
                allTimeGoalRecords.filter { record ->
                    record.goalId == goal.id && record.date.startsWith(detailYearMonthPrefix)
                }.associateBy { it.date }
            }
            val goalProgress = remember(goal, detailYearMonth, recordsMap, today) {
                TimeGoalCalculations.calculateMonthlyGoalProgress(
                    goal = goal,
                    yearMonth = detailYearMonth,
                    recordsMap = recordsMap,
                    today = today
                )
            }

            GoalDetailBottomSheet(
                goalProgress = goalProgress,
                selectedYearMonth = detailYearMonth,
                today = today,
                onDismiss = { viewModel.setSelectedGoalForDetail(null) },
                onPreviousMonth = { detailYearMonth = detailYearMonth.minusMonths(1) },
                onNextMonth = { detailYearMonth = detailYearMonth.plusMonths(1) },
                onDayClick = { dayProgress ->
                    goalForDayEntry = Pair(goal, dayProgress.date)
                },
                onEditGoal = {
                    editingYearMonth = detailYearMonth.toString()
                    goalToEdit = goal
                    showCreateGoalSheet = true
                },
                onDeleteGoal = {
                    viewModel.deleteTimeGoal(goal)
                },
                onArchiveGoal = {
                    viewModel.toggleArchiveTimeGoal(goal)
                }
            )
        }

        // Modal Sheet: Create / Edit Goal
        if (showCreateGoalSheet) {
            CreateOrEditGoalSheet(
                goalToEdit = goalToEdit,
                onDismiss = {
                    showCreateGoalSheet = false
                    goalToEdit = null
                },
                onSave = { name, dailyMinutes, colorHex, iconName, startDate ->
                    val targetYm = try {
                        YearMonth.parse(editingYearMonth)
                    } catch (e: Exception) {
                        selectedYearMonth
                    }
                    if (goalToEdit != null) {
                        viewModel.updateTimeGoal(
                            goal = goalToEdit!!,
                            name = name,
                            dailyTargetMinutes = dailyMinutes,
                            colorHex = colorHex,
                            iconName = iconName,
                            startDate = startDate,
                            yearMonth = targetYm
                        )
                    } else {
                        viewModel.addTimeGoal(
                            name = name,
                            dailyTargetMinutes = dailyMinutes,
                            colorHex = colorHex,
                            iconName = iconName,
                            startDate = startDate,
                            yearMonth = targetYm
                        )
                    }
                    showCreateGoalSheet = false
                    goalToEdit = null
                }
            )
        }

        // Modal Sheet: Record Daily Time
        if (goalForDayEntry != null) {
            val (rawGoal, date) = goalForDayEntry!!
            val dateYmStr = String.format(Locale.US, "%04d-%02d", date.year, date.monthValue)
            val goal = remember(rawGoal, dateYmStr, monthColorsMap) {
                rawGoal.withMonthColor(dateYmStr, monthColorsMap)
            }
            val dateStr = TimeGoalCalculations.formatLocalDate(date)
            val existingRecord = allTimeGoalRecords.firstOrNull { it.goalId == goal.id && it.date == dateStr }

            DailyTimeEntrySheet(
                goal = goal,
                date = date,
                existingRecord = existingRecord,
                onDismiss = { goalForDayEntry = null },
                onSave = { minutes, note ->
                    viewModel.recordDailyTime(goal.id, dateStr, minutes, note)
                    goalForDayEntry = null
                }
            )
        }

        // Dialog: Month/Year Picker
        if (showMonthPicker) {
            MonthYearPickerDialog(
                currentYearMonth = selectedYearMonth,
                onDismiss = { showMonthPicker = false },
                onSelectMonthYear = { ym ->
                    viewModel.setSelectedYearMonth(ym)
                    showMonthPicker = false
                }
            )
        }
    }
}

/**
 * Editorial Top Header for "THE LIFE WITHIN THE HOURS".
 */
@Composable
private fun LifeInHoursHeader(
    selectedYearMonth: YearMonth,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onMonthClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Tagline & Title
        Text(
            text = "The life within",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                lineHeight = 34.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "the hours",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                lineHeight = 34.sp
            ),
            color = NudgeBlue
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Choose where your hours go.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Normal,
                fontSize = 16.sp,
                letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Month Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, PaperBorderLight.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Previous month",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMonthClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = NudgeBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = TimeGoalCalculations.formatYearMonth(selectedYearMonth),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentMonth) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NudgeBlueContainer)
                            .clickable { onCurrentMonth() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = NudgeBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Next month",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Clean sub-navigation pill tabs with responsive, viewport-aware auto-scrolling.
 */
@Composable
private fun HoursSubNavTabs(
    activeTab: HoursTab,
    onTabSelected: (HoursTab) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val tabBoundsMap = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }

    val edgePaddingDp = 16.dp
    val edgePaddingPx = with(density) { edgePaddingDp.roundToPx() }

    LaunchedEffect(activeTab, viewportWidthPx, tabBoundsMap[activeTab.ordinal]) {
        if (viewportWidthPx <= 0) return@LaunchedEffect
        val bounds = tabBoundsMap[activeTab.ordinal] ?: return@LaunchedEffect
        val tabLeft = bounds.first
        val tabRight = bounds.second

        // The safe visible window in content coordinates is:
        // [scrollState.value + edgePaddingPx, scrollState.value + viewportWidthPx - edgePaddingPx]
        val visibleStart = scrollState.value + edgePaddingPx
        val visibleEnd = scrollState.value + viewportWidthPx - edgePaddingPx

        val isFullyVisible = (tabLeft >= visibleStart) && (tabRight <= visibleEnd)

        if (!isFullyVisible) {
            val targetScroll = when {
                tabLeft < visibleStart -> (tabLeft - edgePaddingPx).coerceAtLeast(0)
                tabRight > visibleEnd -> (tabRight - viewportWidthPx + edgePaddingPx).coerceIn(0, scrollState.maxValue)
                else -> scrollState.value
            }

            if (targetScroll != scrollState.value) {
                scrollState.animateScrollTo(targetScroll)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                viewportWidthPx = size.width
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoursTab.entries.forEachIndexed { index, tab ->
                val isSelected = tab == activeTab
                val bg = if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                val border = if (isSelected) NudgeBlue else PaperBorderLight.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val positionInParent = coordinates.positionInParent()
                            val left = positionInParent.x.roundToInt()
                            val right = left + coordinates.size.width
                            tabBoundsMap[index] = Pair(left, right)
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(20.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Dashboard View.
 */
@Composable
private fun DashboardView(
    overallProgress: MonthlyOverallProgress,
    selectedYearMonth: YearMonth,
    today: LocalDate,
    onGoalClick: (TimeGoal) -> Unit,
    onAddGoalClick: () -> Unit,
    onQuickRecordToday: (TimeGoal, Int) -> Unit,
    onOpenRecordSheet: (TimeGoal, LocalDate) -> Unit,
    onReorderGoals: (List<TimeGoal>) -> Unit
) {
    var isGoalsExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isGoalsExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timeGoalsChevronRotation"
    )
    val underlineProgress by animateFloatAsState(
        targetValue = if (isGoalsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timeGoalsUnderlineProgress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // 1. Overall Monthly Progress Card
        item {
            MonthlySummaryCard(overallProgress = overallProgress)
        }

        // 2. Today Quick-Entry Section (if current month)
        if (selectedYearMonth.year == today.year && selectedYearMonth.month == today.month) {
            item {
                TodayQuickEntrySection(
                    goalProgressList = overallProgress.goalProgressList,
                    today = today,
                    onQuickRecord = onQuickRecordToday,
                    onOpenRecordSheet = onOpenRecordSheet,
                    onReorder = onReorderGoals
                )
            }
        }

        // 3. Time Goals Expandable Accordion (Minimal Style matching Settings)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_goals_accordion_container")
            ) {
                // Transparent clickable header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGoalsExpanded = !isGoalsExpanded }
                        .padding(horizontal = 2.dp, vertical = 14.dp)
                        .testTag("time_goals_accordion_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NudgeBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎯",
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "YOUR TIME GOALS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${overallProgress.goalProgressList.size} Active Pursuit${if (overallProgress.goalProgressList.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isGoalsExpanded) "Collapse Time Goals" else "Expand Time Goals",
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation),
                        tint = if (isGoalsExpanded) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Underline divider: 1px thin outlineVariant divider when closed, animated subtle blue underline from LEFT to RIGHT when opened
                val goalsBaseLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    val strokeWidthPx = 1.dp.toPx()
                    val activeStrokeWidthPx = 2.dp.toPx()

                    // 1px base subtle line
                    drawLine(
                        color = goalsBaseLineColor,
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

                // Expandable Section
                AnimatedVisibility(
                    visible = isGoalsExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 280, delayMillis = 40)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (overallProgress.goalProgressList.isEmpty()) {
                            EmptyGoalsPlaceholder(onAddGoalClick = onAddGoalClick)
                        } else {
                            ReorderableGoalList(
                                goalProgressList = overallProgress.goalProgressList,
                                today = today,
                                onGoalClick = onGoalClick,
                                onOpenRecordSheet = onOpenRecordSheet,
                                onReorder = onReorderGoals
                            )

                            // At the bottom of the expanded section show: + Add Pursuit
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = onAddGoalClick,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NudgeBlue
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(NudgeBlue, NudgeBlue))
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("btn_add_time_goal")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Pursuit",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Monthly Summary Overview Card.
 */
@Composable
private fun MonthlySummaryCard(overallProgress: MonthlyOverallProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTHLY OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = NudgeBlue
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NudgeBlueContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${overallProgress.overallProgressPercent}% Achieved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = NudgeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "PLANNED",
                    value = TimeGoalCalculations.formatHoursTotal(overallProgress.totalPlannedMinutes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricColumn(
                    label = "INVESTED",
                    value = TimeGoalCalculations.formatHoursTotal(overallProgress.totalRecordedMinutes),
                    color = NudgeBlue
                )
                MetricColumn(
                    label = "REMAINING",
                    value = TimeGoalCalculations.formatHoursTotal(
                        (overallProgress.totalPlannedMinutes - overallProgress.totalRecordedMinutes).coerceAtLeast(0)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            val fraction = (overallProgress.overallProgressPercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PaperBorderLight.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF3872FF), NudgeBlue)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = color
        )
    }
}

/**
 * Today Quick Entry Row / Section.
 */
@Composable
private fun TodayQuickEntrySection(
    goalProgressList: List<MonthlyGoalProgress>,
    today: LocalDate,
    onQuickRecord: (TimeGoal, Int) -> Unit,
    onOpenRecordSheet: (TimeGoal, LocalDate) -> Unit,
    onReorder: (List<TimeGoal>) -> Unit
) {
    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)).uppercase(Locale.US)
    }

    var localList by remember(goalProgressList) { mutableStateOf(goalProgressList) }
    val currentList by rememberUpdatedState(localList)
    val currentOnReorder by rememberUpdatedState(onReorder)

    var draggingGoalId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var currentTargetIndex by remember { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeights = remember { mutableStateMapOf<Long, Float>() }

    LaunchedEffect(goalProgressList) {
        if (draggingGoalId == null) {
            localList = goalProgressList
        }
    }

    val defaultSlotHeightPx = with(density) { 68.dp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }

    val startIndex = remember(draggingGoalId, localList) {
        if (draggingGoalId != null) localList.indexOfFirst { it.goal.id == draggingGoalId } else -1
    }

    val draggedCardSlotHeight = remember(startIndex, itemHeights, density, defaultSlotHeightPx, spacingPx) {
        if (startIndex != -1 && startIndex in localList.indices) {
            val id = localList[startIndex].goal.id
            val h = itemHeights[id] ?: (defaultSlotHeightPx - spacingPx)
            h + spacingPx
        } else {
            defaultSlotHeightPx
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Today,
                    contentDescription = null,
                    tint = NudgeBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TODAY — $dateLabel",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (localList.isEmpty()) {
            Text(
                text = "Add a time goal below to begin tracking today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    localList.forEachIndexed { index, gp ->
                        key(gp.goal.id) {
                            val isDragging = gp.goal.id == draggingGoalId
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.025f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "todayReorderScale"
                            )

                            val displacementPx = when {
                                isDragging -> 0f
                                startIndex != -1 && currentTargetIndex != -1 -> {
                                    if (startIndex < currentTargetIndex && index in (startIndex + 1)..currentTargetIndex) {
                                        -draggedCardSlotHeight
                                    } else if (startIndex > currentTargetIndex && index in currentTargetIndex until startIndex) {
                                        draggedCardSlotHeight
                                    } else {
                                        0f
                                    }
                                }
                                else -> 0f
                            }

                            val animatedDisplacementY by animateFloatAsState(
                                targetValue = displacementPx,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "todayItemDisplacement"
                            )

                            val todayProg = gp.dailyProgressList.firstOrNull { it.date == today }
                            val actual = todayProg?.actualMinutes ?: 0
                            val target = gp.goal.dailyTargetMinutes
                            val isDone = actual >= target && target > 0
                            val remainingMinutes = (target - actual).coerceAtLeast(0)

                            val statusText = when {
                                isDone -> "✓ Complete"
                                actual > 0 -> "Pending · ${TimeGoalCalculations.formatDurationWords(remainingMinutes)} remaining"
                                else -> "Not Started · ${TimeGoalCalculations.formatDurationWords(target)} remaining"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isDragging) 10f else 1f)
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationY = dragOffsetY
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        } else {
                                            translationY = animatedDisplacementY
                                        }
                                    }
                                    .onGloballyPositioned { coordinates ->
                                        itemHeights[gp.goal.id] = coordinates.size.height.toFloat()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = if (isDragging) 6.dp else 0.dp,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isDragging) gp.goal.parseColor().copy(alpha = 0.5f) else PaperBorderLight.copy(alpha = 0.5f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onOpenRecordSheet(gp.goal, today) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .semantics { contentDescription = "Reorder ${gp.goal.name}" }
                                                .pointerInput(gp.goal.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            val startIdx = currentList.indexOfFirst { it.goal.id == gp.goal.id }
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            draggingGoalId = gp.goal.id
                                                            dragOffsetY = 0f
                                                            currentTargetIndex = if (startIdx != -1) startIdx else index
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y

                                                            val list = currentList
                                                            val fromIdx = list.indexOfFirst { it.goal.id == gp.goal.id }
                                                            if (fromIdx != -1 && list.size > 1) {
                                                                val heightsMap = mutableMapOf<Int, Float>()
                                                                for (i in list.indices) {
                                                                    val h = itemHeights[list[i].goal.id] ?: (defaultSlotHeightPx - spacingPx)
                                                                    heightsMap[i] = h + spacingPx
                                                                }
                                                                val newTarget = TimeGoalCalculations.calculateDropIndex(
                                                                    fromIndex = fromIdx,
                                                                    dragOffsetY = dragOffsetY,
                                                                    itemCount = list.size,
                                                                    itemHeights = heightsMap,
                                                                    defaultSlotHeightPx = defaultSlotHeightPx
                                                                )
                                                                if (newTarget != currentTargetIndex) {
                                                                    currentTargetIndex = newTarget
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val list = currentList
                                                            val fromIdx = list.indexOfFirst { it.goal.id == gp.goal.id }
                                                            val heightsMap = mutableMapOf<Int, Float>()
                                                            for (i in list.indices) {
                                                                val h = itemHeights[list[i].goal.id] ?: (defaultSlotHeightPx - spacingPx)
                                                                heightsMap[i] = h + spacingPx
                                                            }
                                                            val toIdx = if (fromIdx != -1) {
                                                                TimeGoalCalculations.calculateDropIndex(
                                                                    fromIndex = fromIdx,
                                                                    dragOffsetY = dragOffsetY,
                                                                    itemCount = list.size,
                                                                    itemHeights = heightsMap,
                                                                    defaultSlotHeightPx = defaultSlotHeightPx
                                                                )
                                                            } else {
                                                                currentTargetIndex
                                                            }

                                                            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                                                val mutable = list.toMutableList()
                                                                val item = mutable.removeAt(fromIdx)
                                                                mutable.add(toIdx, item)
                                                                localList = mutable
                                                                currentOnReorder(mutable.map { it.goal })
                                                            }
                                                            draggingGoalId = null
                                                            dragOffsetY = 0f
                                                            currentTargetIndex = -1
                                                        },
                                                        onDragCancel = {
                                                            draggingGoalId = null
                                                            dragOffsetY = 0f
                                                            currentTargetIndex = -1
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(gp.goal.parseColor().copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = gp.goal.getIcon(),
                                                contentDescription = null,
                                                tint = gp.goal.parseColor(),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = gp.goal.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Logged: ${TimeGoalCalculations.formatMinutes(actual)} / ${TimeGoalCalculations.formatMinutes(target)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isDone -> Color(0xFF2E7D32)
                                                    actual > 0 -> Color(0xFFE65100)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                }
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!isDone) {
                                            // Quick full target check button
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(gp.goal.parseColor().copy(alpha = 0.12f))
                                                    .clickable { onQuickRecord(gp.goal, target) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "+ Full Target",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    color = gp.goal.parseColor()
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "✓ Target Met",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * Reorderable vertical list of Time Goals with smooth drag-and-drop.
 */
@Composable
private fun ReorderableGoalList(
    goalProgressList: List<MonthlyGoalProgress>,
    today: LocalDate,
    onGoalClick: (TimeGoal) -> Unit,
    onOpenRecordSheet: (TimeGoal, LocalDate) -> Unit,
    onReorder: (List<TimeGoal>) -> Unit
) {
    var localList by remember(goalProgressList) { mutableStateOf(goalProgressList) }
    val currentList by rememberUpdatedState(localList)
    val currentOnReorder by rememberUpdatedState(onReorder)

    var draggingGoalId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var currentTargetIndex by remember { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeights = remember { mutableStateMapOf<Long, Float>() }

    LaunchedEffect(goalProgressList) {
        if (draggingGoalId == null) {
            localList = goalProgressList
        }
    }

    val defaultSlotHeightPx = with(density) { 172.dp.toPx() }
    val spacingPx = with(density) { 12.dp.toPx() }

    val startIndex = remember(draggingGoalId, localList) {
        if (draggingGoalId != null) localList.indexOfFirst { it.goal.id == draggingGoalId } else -1
    }

    val draggedCardSlotHeight = remember(startIndex, itemHeights, density, defaultSlotHeightPx, spacingPx) {
        if (startIndex != -1 && startIndex in localList.indices) {
            val id = localList[startIndex].goal.id
            val h = itemHeights[id] ?: (defaultSlotHeightPx - spacingPx)
            h + spacingPx
        } else {
            defaultSlotHeightPx
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        localList.forEachIndexed { index, gp ->
            key(gp.goal.id) {
                val isDragging = gp.goal.id == draggingGoalId
                val animatedScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.025f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "reorderScale"
                )

                // Calculate displacement for non-dragged items to make space for the target slot
                val displacementPx = when {
                    isDragging -> 0f
                    startIndex != -1 && currentTargetIndex != -1 -> {
                        if (startIndex < currentTargetIndex && index in (startIndex + 1)..currentTargetIndex) {
                            -draggedCardSlotHeight
                        } else if (startIndex > currentTargetIndex && index in currentTargetIndex until startIndex) {
                            draggedCardSlotHeight
                        } else {
                            0f
                        }
                    }
                    else -> 0f
                }

                val animatedDisplacementY by animateFloatAsState(
                    targetValue = displacementPx,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "itemDisplacement"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffsetY
                                scaleX = animatedScale
                                scaleY = animatedScale
                            } else {
                                translationY = animatedDisplacementY
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            itemHeights[gp.goal.id] = coordinates.size.height.toFloat()
                        }
                        .pointerInput(gp.goal.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val startIdx = currentList.indexOfFirst { it.goal.id == gp.goal.id }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingGoalId = gp.goal.id
                                    dragOffsetY = 0f
                                    currentTargetIndex = if (startIdx != -1) startIdx else index
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    val list = currentList
                                    val fromIdx = list.indexOfFirst { it.goal.id == gp.goal.id }
                                    if (fromIdx != -1 && list.size > 1) {
                                        val heightsMap = mutableMapOf<Int, Float>()
                                        for (i in list.indices) {
                                            val h = itemHeights[list[i].goal.id] ?: (defaultSlotHeightPx - spacingPx)
                                            heightsMap[i] = h + spacingPx
                                        }
                                        val newTarget = TimeGoalCalculations.calculateDropIndex(
                                            fromIndex = fromIdx,
                                            dragOffsetY = dragOffsetY,
                                            itemCount = list.size,
                                            itemHeights = heightsMap,
                                            defaultSlotHeightPx = defaultSlotHeightPx
                                        )
                                        if (newTarget != currentTargetIndex) {
                                            currentTargetIndex = newTarget
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val list = currentList
                                    val fromIdx = list.indexOfFirst { it.goal.id == gp.goal.id }
                                    val heightsMap = mutableMapOf<Int, Float>()
                                    for (i in list.indices) {
                                        val h = itemHeights[list[i].goal.id] ?: (defaultSlotHeightPx - spacingPx)
                                        heightsMap[i] = h + spacingPx
                                    }
                                    val toIdx = if (fromIdx != -1) {
                                        TimeGoalCalculations.calculateDropIndex(
                                            fromIndex = fromIdx,
                                            dragOffsetY = dragOffsetY,
                                            itemCount = list.size,
                                            itemHeights = heightsMap,
                                            defaultSlotHeightPx = defaultSlotHeightPx
                                        )
                                    } else {
                                        currentTargetIndex
                                    }

                                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                        val mutable = list.toMutableList()
                                        val item = mutable.removeAt(fromIdx)
                                        mutable.add(toIdx, item)
                                        localList = mutable
                                        currentOnReorder(mutable.map { it.goal })
                                    }
                                    draggingGoalId = null
                                    dragOffsetY = 0f
                                    currentTargetIndex = -1
                                },
                                onDragCancel = {
                                    val list = currentList
                                    val fromIdx = list.indexOfFirst { it.goal.id == gp.goal.id }
                                    val heightsMap = mutableMapOf<Int, Float>()
                                    for (i in list.indices) {
                                        val h = itemHeights[list[i].goal.id] ?: (defaultSlotHeightPx - spacingPx)
                                        heightsMap[i] = h + spacingPx
                                    }
                                    val toIdx = if (fromIdx != -1) {
                                        TimeGoalCalculations.calculateDropIndex(
                                            fromIndex = fromIdx,
                                            dragOffsetY = dragOffsetY,
                                            itemCount = list.size,
                                            itemHeights = heightsMap,
                                            defaultSlotHeightPx = defaultSlotHeightPx
                                        )
                                    } else {
                                        currentTargetIndex
                                    }

                                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                        val mutable = list.toMutableList()
                                        val item = mutable.removeAt(fromIdx)
                                        mutable.add(toIdx, item)
                                        localList = mutable
                                        currentOnReorder(mutable.map { it.goal })
                                    }
                                    draggingGoalId = null
                                    dragOffsetY = 0f
                                    currentTargetIndex = -1
                                }
                            )
                        }
                ) {
                    GoalProgressCard(
                        goalProgress = gp,
                        onClick = {
                            if (draggingGoalId == null) {
                                onGoalClick(gp.goal)
                            }
                        },
                        onRecordClick = {
                            if (draggingGoalId == null) {
                                onOpenRecordSheet(gp.goal, today)
                            }
                        },
                        isDragging = isDragging
                    )
                }
            }
        }
    }
}

/**
 * Card for an individual Goal.
 */
@Composable
private fun GoalProgressCard(
    goalProgress: MonthlyGoalProgress,
    onClick: () -> Unit,
    onRecordClick: () -> Unit,
    isDragging: Boolean = false
) {
    val goal = goalProgress.goal
    val goalColor = goal.parseColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .shadow(
                elevation = if (isDragging) 8.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (isDragging) goalColor.copy(alpha = 0.35f) else Color(0x14000000)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isDragging) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(
                if (isDragging) listOf(goalColor.copy(alpha = 0.6f), goalColor.copy(alpha = 0.6f))
                else listOf(PaperBorderLight, PaperBorderLight)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = goal.getIcon(),
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${TimeGoalCalculations.formatMinutes(goal.dailyTargetMinutes)} daily target",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // % badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(goalColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${goalProgress.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = goalColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${TimeGoalCalculations.formatHoursTotal(goalProgress.recordedMinutes)} / ${TimeGoalCalculations.formatHoursTotal(goalProgress.plannedMinutes)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Tap to view calendar",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = NudgeBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            val fraction = (goalProgress.progressPercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PaperBorderLight.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(goalColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Completed / Missed count pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✓ ${goalProgress.completedDaysCount} done",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = Color(0xFF2E7D32)
                )
                if (goalProgress.partialDaysCount > 0) {
                    Text(
                        text = "◐ ${goalProgress.partialDaysCount} partial",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = Color(0xFFE65100)
                    )
                }
                if (goalProgress.missedDaysCount > 0) {
                    Text(
                        text = "— ${goalProgress.missedDaysCount} missed",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = Color(0xFFD84315)
                    )
                }
            }
        }
    }
}

/**
 * Empty placeholder when no time goals exist.
 */
@Composable
private fun EmptyGoalsPlaceholder(onAddGoalClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(NudgeBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassEmpty,
                    contentDescription = null,
                    tint = NudgeBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Your hours are yours to shape.",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Set meaningful daily pursuits—reading, coding, deep work, or exercise—and manually record the time you invest.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddGoalClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Your First Pursuit",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Tab 2: Past Months View - Historical Access.
 */
@Composable
private fun PastMonthsView(
    selectedYearMonth: YearMonth,
    allGoals: List<TimeGoal>,
    allRecords: List<TimeGoalRecord>,
    onSelectMonth: (YearMonth) -> Unit
) {
    val current = remember { YearMonth.now() }
    val currentYear = current.year

    var additionalMonthsToLoad by rememberSaveable { mutableIntStateOf(0) }
    var showYearPickerDialog by rememberSaveable { mutableStateOf(false) }

    // Dynamic timeline grouping starting with the initial 3-year set (3 months/year) plus any loaded older months
    val timelineGroups = remember(current, additionalMonthsToLoad) {
        val initialMonths = listOf(
            Pair(currentYear, listOf(current, current.minusMonths(1), current.minusMonths(2))),
            Pair(currentYear - 1, listOf(
                YearMonth.of(currentYear - 1, Month.DECEMBER),
                YearMonth.of(currentYear - 1, Month.NOVEMBER),
                YearMonth.of(currentYear - 1, Month.OCTOBER)
            )),
            Pair(currentYear - 2, listOf(
                YearMonth.of(currentYear - 2, Month.DECEMBER),
                YearMonth.of(currentYear - 2, Month.NOVEMBER),
                YearMonth.of(currentYear - 2, Month.OCTOBER)
            ))
        )

        if (additionalMonthsToLoad <= 0) {
            initialMonths
        } else {
            // Find the earliest month currently in initialMonths
            val earliestInitialMonth = YearMonth.of(currentYear - 2, Month.OCTOBER)
            val olderMonths = (1..additionalMonthsToLoad).map { offset ->
                earliestInitialMonth.minusMonths(offset.toLong())
            }

            val allCombined = initialMonths.flatMap { it.second } + olderMonths
            allCombined
                .distinct()
                .groupBy { it.year }
                .map { (year, months) -> Pair(year, months.sortedDescending()) }
                .sortedByDescending { it.first }
        }
    }

    val allDisplayMonths = remember(timelineGroups) {
        timelineGroups.flatMap { it.second }
    }

    // Pre-calculate stats per month
    val monthStatsMap = remember(allRecords, allDisplayMonths) {
        val map = mutableMapOf<YearMonth, Pair<Int, Int>>() // YearMonth -> (totalMinutes, daysActive)
        for (ym in allDisplayMonths) {
            val prefix = ym.format(DateTimeFormatter.ofPattern("yyyy-MM-", Locale.US))
            val recordsInMonth = allRecords.filter { it.date.startsWith(prefix) && it.actualMinutes > 0 }
            val totalMins = recordsInMonth.sumOf { it.actualMinutes }
            val daysActive = recordsInMonth.map { it.date }.distinct().size
            map[ym] = Pair(totalMins, daysActive)
        }
        map
    }

    var isTimelineExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isTimelineExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timelineChevronRotation"
    )
    val underlineProgress by animateFloatAsState(
        targetValue = if (isTimelineExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timelineUnderlineProgress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Historical Timeline Expandable Accordion (Minimal style matching Settings)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("historical_timeline_accordion_container")
            ) {
                // Transparent clickable header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTimelineExpanded = !isTimelineExpanded }
                        .padding(horizontal = 2.dp, vertical = 14.dp)
                        .testTag("historical_timeline_accordion_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NudgeBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = NudgeBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Historical Timeline",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Access all historical months and pursuits data.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isTimelineExpanded) "Collapse Historical Timeline" else "Expand Historical Timeline",
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation),
                        tint = if (isTimelineExpanded) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Underline divider: 1px thin outlineVariant divider when closed, animated subtle blue underline from LEFT to RIGHT when opened
                val timelineBaseLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    val strokeWidthPx = 1.dp.toPx()
                    val activeStrokeWidthPx = 2.dp.toPx()

                    // 1px base subtle line
                    drawLine(
                        color = timelineBaseLineColor,
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

                // Expandable Section
                AnimatedVisibility(
                    visible = isTimelineExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 280, delayMillis = 40)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        timelineGroups.forEach { (year, yearMonths) ->
                            // Year section header
                            val yearTotalMinutes = yearMonths.sumOf { monthStatsMap[it]?.first ?: 0 }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$year",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (yearTotalMinutes > 0) {
                                    Text(
                                        text = "${TimeGoalCalculations.formatHoursTotal(yearTotalMinutes)} logged in $year",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = NudgeBlue
                                    )
                                }
                            }

                            yearMonths.forEach { ym ->
                                val isSelected = ym == selectedYearMonth
                                val isCurrent = ym == current
                                val (totalMins, daysActive) = monthStatsMap[ym] ?: Pair(0, 0)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSelectMonth(ym) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) NudgeBlueContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        brush = Brush.linearGradient(
                                            if (isSelected) listOf(NudgeBlue, NudgeBlue) else listOf(PaperBorderLight, PaperBorderLight)
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) NudgeBlue
                                                        else if (totalMins > 0) NudgeBlueContainer
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else if (totalMins > 0) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = TimeGoalCalculations.formatYearMonth(ym),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                    if (isCurrent) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(NudgeBlueContainer)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Current",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = NudgeBlue
                                                            )
                                                        }
                                                    } else if (isSelected) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(NudgeBlue.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Selected",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = NudgeBlue
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                val subtitle = if (totalMins > 0) {
                                                    "${TimeGoalCalculations.formatMinutes(totalMins)} logged • $daysActive active days"
                                                } else {
                                                    "${ym.lengthOfMonth()} days • No activity logged"
                                                }

                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (totalMins > 0) FontWeight.Medium else FontWeight.Normal
                                                    ),
                                                    color = if (totalMins > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = "View Month",
                                            tint = if (isSelected) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom action buttons: Load older months & Select Year
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    additionalMonthsToLoad += 12
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("load_older_months_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NudgeBlue,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Load older months (+12 mos)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    showYearPickerDialog = true
                                },
                                modifier = Modifier.testTag("select_year_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NudgeBlueContainer,
                                    contentColor = NudgeBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = NudgeBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Select Year",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = NudgeBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showYearPickerDialog) {
        MonthYearPickerDialog(
            currentYearMonth = selectedYearMonth,
            onDismiss = { showYearPickerDialog = false },
            onSelectMonthYear = { ym ->
                showYearPickerDialog = false
                onSelectMonth(ym)
            }
        )
    }
}

private enum class ExportMode {
    SINGLE_MONTH,
    MULTIPLE_MONTHS
}

/**
 * Tab 3: Export PDF View (Expandable Accordion).
 */
@Composable
private fun ExportPdfView(
    selectedYearMonth: YearMonth,
    goals: List<TimeGoal>,
    records: List<TimeGoalRecord>,
    onExportPdf: (TimeGoalsPdfOptions) -> Unit,
    onExportMultiMonthPdf: ((startMonth: YearMonth, endMonth: YearMonth, options: TimeGoalsPdfOptions) -> Unit)? = null
) {
    // Collapsed by default every time the screen/tab is opened
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "exportChevronRotation"
    )
    val underlineProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "exportUnderlineProgress"
    )

    var exportMode by remember { mutableStateOf(ExportMode.SINGLE_MONTH) }
    var rangeStartMonth by remember { mutableStateOf(selectedYearMonth) }
    var rangeEndMonth by remember { mutableStateOf(selectedYearMonth) }
    var showStartMonthPicker by remember { mutableStateOf(false) }
    var showEndMonthPicker by remember { mutableStateOf(false) }

    val isRangeInvalid = rangeStartMonth.isAfter(rangeEndMonth)
    val monthsCount = remember(rangeStartMonth, rangeEndMonth, isRangeInvalid) {
        if (isRangeInvalid) {
            0
        } else {
            var count = 0
            var ym = rangeStartMonth
            while (!ym.isAfter(rangeEndMonth)) {
                count++
                ym = ym.plusMonths(1)
            }
            count
        }
    }
    val isRangeTooLarge = !isRangeInvalid && monthsCount > 12
    val canExportMulti = !isRangeInvalid && !isRangeTooLarge && monthsCount > 0

    var includeSummary by remember { mutableStateOf(true) }
    var includeGoalsList by remember { mutableStateOf(true) }
    var includeCalendarGrid by remember { mutableStateOf(true) }
    var includeDailyRecords by remember { mutableStateOf(true) }
    var includeTimeDistribution by remember { mutableStateOf(true) }
    var includeInsights by remember { mutableStateOf(true) }

    val monthTitle = TimeGoalCalculations.formatYearMonth(selectedYearMonth)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_pdf_accordion_container")
            ) {
                // Tappable Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 2.dp, vertical = 14.dp)
                        .testTag("export_pdf_accordion_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NudgeBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "PDF Icon",
                                tint = NudgeBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Export PDF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Create and share your monthly time report.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Export PDF" else "Expand Export PDF",
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation),
                        tint = if (isExpanded) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Underline divider: 1px thin outlineVariant divider when closed, animated subtle blue underline from LEFT to RIGHT when opened
                val exportBaseLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    val strokeWidthPx = 1.dp.toPx()
                    val activeStrokeWidthPx = 2.dp.toPx()

                    // 1px base subtle line
                    drawLine(
                        color = exportBaseLineColor,
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

                // Expanded PDF Configuration Section
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 280, delayMillis = 40)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Export Mode Selector: [ Single Month ] [ Multiple Months ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .shadow(
                                        elevation = if (exportMode == ExportMode.SINGLE_MONTH) 1.dp else 0.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (exportMode == ExportMode.SINGLE_MONTH) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { exportMode = ExportMode.SINGLE_MONTH }
                                    .testTag("tab_export_single_month"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Single Month",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (exportMode == ExportMode.SINGLE_MONTH) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = if (exportMode == ExportMode.SINGLE_MONTH) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .shadow(
                                        elevation = if (exportMode == ExportMode.MULTIPLE_MONTHS) 1.dp else 0.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (exportMode == ExportMode.MULTIPLE_MONTHS) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { exportMode = ExportMode.MULTIPLE_MONTHS }
                                    .testTag("tab_export_multiple_months"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Multiple Months",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (exportMode == ExportMode.MULTIPLE_MONTHS) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = if (exportMode == ExportMode.MULTIPLE_MONTHS) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Document Info
                        if (exportMode == ExportMode.SINGLE_MONTH) {
                            Column {
                                Text(
                                    text = "EXPORT TIME REPORT (PDF)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 11.sp
                                    ),
                                    color = NudgeBlue
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Generate $monthTitle Report",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        } else {
                            Column {
                                Text(
                                    text = "EXPORT TIME REPORT (PDF)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 11.sp
                                    ),
                                    color = NudgeBlue
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Generate Multi-Month Report",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Custom Range Area
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight))
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "SELECT MONTH RANGE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 10.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Start Month Button
                                        OutlinedButton(
                                            onClick = { showStartMonthPicker = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_range_start_month"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isRangeInvalid) MaterialTheme.colorScheme.error else PaperBorderLight),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = "Start Month",
                                                    tint = NudgeBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = TimeGoalCalculations.formatYearMonth(rangeStartMonth),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Text(
                                            text = "—",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // End Month Button
                                        OutlinedButton(
                                            onClick = { showEndMonthPicker = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_range_end_month"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isRangeInvalid) MaterialTheme.colorScheme.error else PaperBorderLight),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = "End Month",
                                                    tint = NudgeBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = TimeGoalCalculations.formatYearMonth(rangeEndMonth),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Dynamic summary or validation message
                                    if (isRangeInvalid) {
                                        Text(
                                            text = "Start month must precede end month",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.testTag("txt_range_validation_error")
                                        )
                                    } else if (isRangeTooLarge) {
                                        Text(
                                            text = "You can select up to 12 months at a time.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.testTag("txt_range_validation_error")
                                        )
                                    } else {
                                        val monthsLabel = if (monthsCount == 1) "1 month selected" else "$monthsCount months selected"
                                        val rangeLabel = "${TimeGoalCalculations.formatYearMonth(rangeStartMonth)} – ${TimeGoalCalculations.formatYearMonth(rangeEndMonth)}"
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = monthsLabel,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = NudgeBlue,
                                                modifier = Modifier.testTag("txt_range_months_count")
                                            )
                                            Text(
                                                text = rangeLabel,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.testTag("txt_range_label")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                            // Document Sections To Include
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 0.5.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = "DOCUMENT SECTIONS TO INCLUDE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    ExportOptionRow(
                                        label = "Monthly Hours Summary",
                                        checked = includeSummary,
                                        onCheckedChange = { includeSummary = it },
                                        showDivider = true
                                    )

                                    ExportOptionRow(
                                        label = "Goals & Daily Targets",
                                        checked = includeGoalsList,
                                        onCheckedChange = { includeGoalsList = it },
                                        showDivider = true
                                    )

                                    ExportOptionRow(
                                        label = "Monthly Calendar Grid",
                                        checked = includeCalendarGrid,
                                        onCheckedChange = { includeCalendarGrid = it },
                                        showDivider = true
                                    )

                                    ExportOptionRow(
                                        label = "Time Distribution",
                                        checked = includeTimeDistribution,
                                        onCheckedChange = { includeTimeDistribution = it },
                                        showDivider = true
                                    )

                                    ExportOptionRow(
                                        label = "Review Insights & Streaks",
                                        checked = includeInsights,
                                        onCheckedChange = { includeInsights = it },
                                        showDivider = false
                                    )
                                }
                            }

                            // Primary Action Button (Soft Blue Tint Design)
                            if (exportMode == ExportMode.SINGLE_MONTH) {
                                val singleInteractionSource = remember { MutableInteractionSource() }
                                val isSinglePressed by singleInteractionSource.collectIsPressedAsState()

                                val singleElevation by animateDpAsState(
                                    targetValue = if (isSinglePressed) 2.dp else 6.dp,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "pdf_single_elevation"
                                )

                                val singleTranslationY by animateDpAsState(
                                    targetValue = if (isSinglePressed) 1.dp else 0.dp,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "pdf_single_translation_y"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .graphicsLayer {
                                            translationY = singleTranslationY.toPx()
                                        }
                                        .shadow(
                                            elevation = singleElevation,
                                            shape = RoundedCornerShape(20.dp),
                                            clip = false,
                                            ambientColor = Color(0x14000000),
                                            spotColor = Color(0x1F000000)
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .clickable(
                                            interactionSource = singleInteractionSource,
                                            indication = null,
                                            role = Role.Button
                                        ) {
                                            onExportPdf(
                                                TimeGoalsPdfOptions(
                                                    includeSummary = includeSummary,
                                                    includeGoalsList = includeGoalsList,
                                                    includeCalendarGrid = includeCalendarGrid,
                                                    includeDailyRecords = includeDailyRecords,
                                                    includeTimeDistribution = includeTimeDistribution,
                                                    includeInsights = includeInsights
                                                )
                                            )
                                        }
                                        .testTag("btn_generate_hours_pdf"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Generate & Share PDF",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            } else {
                                val multiInteractionSource = remember { MutableInteractionSource() }
                                val isMultiPressed by multiInteractionSource.collectIsPressedAsState()

                                val multiElevation by animateDpAsState(
                                    targetValue = when {
                                        !canExportMulti -> 0.dp
                                        isMultiPressed -> 2.dp
                                        else -> 6.dp
                                    },
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "pdf_multi_elevation"
                                )

                                val multiTranslationY by animateDpAsState(
                                    targetValue = if (canExportMulti && isMultiPressed) 1.dp else 0.dp,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "pdf_multi_translation_y"
                                )

                                val multiBgColor = if (canExportMulti) Color(0xFFEFF6FF) else Color(0xFFF3F4F6)
                                val multiTextColor = if (canExportMulti) Color(0xFF1E3A8A) else Color(0xFF9CA3AF)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .graphicsLayer {
                                            translationY = multiTranslationY.toPx()
                                        }
                                        .shadow(
                                            elevation = multiElevation,
                                            shape = RoundedCornerShape(20.dp),
                                            clip = false,
                                            ambientColor = Color(0x14000000),
                                            spotColor = Color(0x1F000000)
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(multiBgColor)
                                        .clickable(
                                            interactionSource = multiInteractionSource,
                                            indication = null,
                                            enabled = canExportMulti,
                                            role = Role.Button
                                        ) {
                                            if (canExportMulti) {
                                                val options = TimeGoalsPdfOptions(
                                                    includeSummary = includeSummary,
                                                    includeGoalsList = includeGoalsList,
                                                    includeCalendarGrid = includeCalendarGrid,
                                                    includeDailyRecords = includeDailyRecords,
                                                    includeTimeDistribution = includeTimeDistribution,
                                                    includeInsights = includeInsights
                                                )
                                                onExportMultiMonthPdf?.invoke(rangeStartMonth, rangeEndMonth, options)
                                            }
                                        }
                                        .testTag("btn_generate_hours_pdf"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (monthsCount > 1) "Generate & Share PDF ($monthsCount Months)" else "Generate & Share PDF (1 Month)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = multiTextColor
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    if (showStartMonthPicker) {
        MonthYearPickerDialog(
            currentYearMonth = rangeStartMonth,
            onDismiss = { showStartMonthPicker = false },
            onSelectMonthYear = { ym ->
                rangeStartMonth = ym
                showStartMonthPicker = false
            }
        )
    }

    if (showEndMonthPicker) {
        MonthYearPickerDialog(
            currentYearMonth = rangeEndMonth,
            onDismiss = { showEndMonthPicker = false },
            onSelectMonthYear = { ym ->
                rangeEndMonth = ym
                showEndMonthPicker = false
            }
        )
    }
}

@Composable
private fun ExportOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimal Plus / Tick Toggle Indicator (matches HTML reference)
            Box(
                modifier = Modifier
                    .size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Text(
                        text = "✓",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NudgeBlue,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Goal Detail Bottom Sheet with Full Monthly Calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailBottomSheet(
    goalProgress: MonthlyGoalProgress,
    selectedYearMonth: YearMonth,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (DayProgress) -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onArchiveGoal: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val goal = goalProgress.goal
    val goalColor = goal.parseColor()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Goal Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = goal.getIcon(),
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${TimeGoalCalculations.formatMinutes(goal.dailyTargetMinutes)} daily target",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEditGoal) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit pursuit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete pursuit",
                            tint = Color(0xFFD84315)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Target Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn(
                        label = "PLANNED",
                        value = TimeGoalCalculations.formatHoursTotal(goalProgress.plannedMinutes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MetricColumn(
                        label = "INVESTED",
                        value = TimeGoalCalculations.formatHoursTotal(goalProgress.recordedMinutes),
                        color = goalColor
                    )
                    MetricColumn(
                        label = "PROGRESS",
                        value = "${goalProgress.progressPercent}%",
                        color = goalColor
                    )
                    MetricColumn(
                        label = "COMPLETED",
                        value = "${goalProgress.completedDaysCount}d",
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Calendar Section Header with navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous month",
                            tint = NudgeBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "CALENDAR — ${TimeGoalCalculations.formatYearMonth(selectedYearMonth).uppercase(Locale.US)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        ),
                        color = NudgeBlue
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next month",
                            tint = NudgeBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = "Tap a day to log time",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Swipeable Interactive Calendar Grid
            var totalDragOffset by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val swipeThreshold = with(density) { 36.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(selectedYearMonth) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragOffset = 0f },
                            onDragEnd = {
                                if (totalDragOffset > swipeThreshold) {
                                    // Swipe LEFT -> PREVIOUS month (September -> August -> July)
                                    onPreviousMonth()
                                } else if (totalDragOffset < -swipeThreshold) {
                                    // Swipe RIGHT -> NEXT month (September -> October -> November)
                                    onNextMonth()
                                }
                                totalDragOffset = 0f
                            },
                            onDragCancel = { totalDragOffset = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragOffset += dragAmount
                            }
                        )
                    }
            ) {
                CalendarGridView(
                    goalProgress = goalProgress,
                    selectedYearMonth = selectedYearMonth,
                    goalColor = goalColor,
                    onDayClick = onDayClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            CalendarLegendRow()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Pursuit?") },
            text = { Text("This will remove \"${goal.name}\" and all of its logged daily records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteGoal()
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFD84315), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Interactive Monthly Calendar Grid Composable.
 */
@Composable
private fun CalendarGridView(
    goalProgress: MonthlyGoalProgress,
    selectedYearMonth: YearMonth,
    goalColor: Color,
    onDayClick: (DayProgress) -> Unit
) {
    val firstDay = goalProgress.dailyProgressList.firstOrNull()?.date ?: LocalDate.now()
    val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Mon, 7 = Sun
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, PaperBorderLight.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        // Day Names Header
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { name ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
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

        // Days Grid
        val totalCells = firstDayOfWeek - 1 + goalProgress.dailyProgressList.size
        val totalRows = (totalCells + 6) / 7

        var dayIdx = 0

        for (row in 0 until totalRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIdx = row * 7 + col
                    val isLeadBlank = cellIdx < firstDayOfWeek - 1
                    val isTailBlank = dayIdx >= goalProgress.dailyProgressList.size

                    if (isLeadBlank || isTailBlank) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val dayProgress = goalProgress.dailyProgressList[dayIdx]
                        dayIdx++

                        val (cellBg, cellText, symbol) = when (dayProgress.state) {
                            DayCompletionState.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "✓")
                            DayCompletionState.PARTIAL -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "◐")
                            DayCompletionState.MISSED -> Triple(Color(0xFFFBE9E7), Color(0xFFD84315), "—")
                            DayCompletionState.FUTURE -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), "○")
                            DayCompletionState.BEFORE_START -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), "·")
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .border(0.5.dp, PaperBorderLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onDayClick(dayProgress) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${dayProgress.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = cellText
                                )
                                Text(
                                    text = symbol,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = cellText
                                )
                            }

                            if (!dayProgress.note.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = "Category Note",
                                    tint = goalColor,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 1.dp, end = 1.dp)
                                        .size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(symbol = "✓", label = "Completed", color = Color(0xFF2E7D32))
        LegendItem(symbol = "◐", label = "Partial", color = Color(0xFFE65100))
        LegendItem(symbol = "—", label = "Missed", color = Color(0xFFD84315))
        LegendItem(symbol = "○", label = "Future", color = TextSecondaryLight)
    }
}

@Composable
private fun LegendItem(symbol: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = color
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Modal Sheet: Create or Edit a Time Goal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOrEditGoalSheet(
    goalToEdit: TimeGoal?,
    onDismiss: () -> Unit,
    onSave: (name: String, dailyMinutes: Int, colorHex: String, iconName: String, startDate: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presets = remember {
        listOf(
            30 to "30m",
            45 to "45m",
            60 to "1h",
            90 to "1h 30m",
            120 to "2h",
            150 to "2h 30m",
            180 to "3h",
            240 to "4h"
        )
    }

    val initialMinutes = goalToEdit?.dailyTargetMinutes ?: 120
    val matchesPreset = presets.any { it.first == initialMinutes }

    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var isCustomSelected by remember { mutableStateOf(!matchesPreset) }
    var customHours by remember { mutableIntStateOf(if (!matchesPreset) initialMinutes / 60 else (initialMinutes / 60).coerceAtLeast(1)) }
    var customMinutes by remember { mutableIntStateOf(if (!matchesPreset) initialMinutes % 60 else (initialMinutes % 60)) }
    var dailyMinutes by remember { mutableIntStateOf(initialMinutes) }
    var selectedColorHex by remember { mutableStateOf(goalToEdit?.colorHex ?: "#7C4DFF") }
    var selectedIconName by remember { mutableStateOf(goalToEdit?.iconName ?: "book") }
    var startDate by remember {
        mutableStateOf(goalToEdit?.startDate ?: TimeGoalCalculations.formatLocalDate(LocalDate.now().withDayOfMonth(1)))
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showMoreIconsDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Title
            Text(
                text = if (goalToEdit != null) "Edit Pursuit" else "New Time Goal",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Pursuit Name (e.g. Reading, Coding, Study)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_goal_name"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NudgeBlue,
                    unfocusedBorderColor = PaperBorderLight
                )
            )

            // Daily Target Duration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Daily Target: ${TimeGoalCalculations.formatMinutes(dailyMinutes)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Presets row with final "Custom" option
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (mins, label) ->
                        val isSelected = !isCustomSelected && dailyMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .clickable {
                                    isCustomSelected = false
                                    dailyMinutes = mins
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Custom pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustomSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .clickable {
                                isCustomSelected = true
                                if (customHours == 0 && customMinutes == 0) {
                                    customHours = 2
                                    customMinutes = 15
                                }
                                dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If Custom is selected, display custom time control
                if (isCustomSelected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Time Target",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%02d h %02d m", customHours, customMinutes),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NudgeBlue
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hours Stepper
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hours",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, PaperBorderLight, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (customHours > 0) {
                                                    customHours -= 1
                                                    dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(
                                                text = "−",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Text(
                                            text = "${customHours} h",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )

                                        IconButton(
                                            onClick = {
                                                if (customHours < 23) {
                                                    customHours += 1
                                                    dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(
                                                text = "+",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                // Minutes Stepper
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Minutes",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, PaperBorderLight, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = {
                                                customMinutes = if (customMinutes >= 5) (customMinutes - 5) else 55
                                                dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(
                                                text = "−",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Text(
                                            text = "${customMinutes} m",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )

                                        IconButton(
                                            onClick = {
                                                customMinutes = if (customMinutes <= 50) (customMinutes + 5) else 0
                                                dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(
                                                text = "+",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Minutes Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0, 15, 30, 45).forEach { m ->
                                    val isChipSelected = customMinutes == m
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChipSelected) NudgeBlueContainer else MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = 1.dp,
                                                color = if (isChipSelected) NudgeBlue else PaperBorderLight,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                customMinutes = m
                                                dailyMinutes = (customHours * 60 + customMinutes).coerceAtLeast(1)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${m}m",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp
                                            ),
                                            color = if (isChipSelected) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Accent Color — Custom Only (No preset circles)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category Accent Color",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                val currentColor = remember(selectedColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(selectedColorHex))
                    } catch (_: Exception) {
                        Color(0xFF7C4DFF)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, PaperBorderLight, RoundedCornerShape(14.dp))
                        .clickable { showColorPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(1.dp, PaperBorderLight, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "+  Choose Your Custom Color",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = selectedColorHex.uppercase(Locale.US),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Choose custom color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Icon Symbol — Keep Existing + More
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Icon Symbol",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Standard 10 icons
                    TimeGoal.AVAILABLE_ICONS.forEach { (iconKey, label) ->
                        val isSelected = selectedIconName.equals(iconKey, ignoreCase = true)
                        val dummyGoal = TimeGoal(name = "", dailyTargetMinutes = 0, startDate = "", iconName = iconKey)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .clickable { selectedIconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dummyGoal.getIcon(),
                                contentDescription = label,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // If selected icon is not in the top 10, display it as a selected chip
                    val isCustomIcon = !TimeGoal.AVAILABLE_ICONS.any { it.first.equals(selectedIconName, ignoreCase = true) }
                    if (isCustomIcon) {
                        val dummyGoal = TimeGoal(name = "", dailyTargetMinutes = 0, startDate = "", iconName = selectedIconName)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NudgeBlue)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dummyGoal.getIcon(),
                                contentDescription = "Selected icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // + More button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, PaperBorderLight, RoundedCornerShape(10.dp))
                            .clickable { showMoreIconsDialog = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NudgeBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "More",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = NudgeBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save Pursuit Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), dailyMinutes, selectedColorHex, selectedIconName, startDate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_time_goal"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
            ) {
                Text(
                    text = if (goalToEdit != null) "Update Pursuit" else "Save Pursuit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        CustomColorPickerDialog(
            currentColorHex = selectedColorHex,
            onDismiss = { showColorPicker = false },
            onColorSelected = { chosenHex ->
                selectedColorHex = chosenHex
                showColorPicker = false
            }
        )
    }

    // More Icons Dialog
    if (showMoreIconsDialog) {
        MoreIconsPickerDialog(
            selectedIcon = selectedIconName,
            onDismiss = { showMoreIconsDialog = false },
            onIconSelected = { chosenIcon ->
                selectedIconName = chosenIcon
                showMoreIconsDialog = false
            }
        )
    }
}

/**
 * Custom Color Picker Dialog allowing selection of ANY color with Live Preview, Sliders, Hex input, and Palette.
 */
@Composable
private fun CustomColorPickerDialog(
    currentColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val (initH, initS, initV) = remember(currentColorHex) { hexToHsv(currentColorHex) }
    var hue by remember { mutableStateOf(initH) }
    var saturation by remember { mutableStateOf(initS) }
    var value by remember { mutableStateOf(initV) }
    var hexInput by remember { mutableStateOf(currentColorHex.uppercase(Locale.US)) }

    val activeColor = remember(hue, saturation, value) { hsvToColor(hue, saturation, value) }

    val curatedPalette = listOf(
        "#7C4DFF", "#5E35B1", "#3949AB", "#1E88E5",
        "#0288D1", "#00ACC1", "#00897B", "#43A047",
        "#7CB342", "#C0CA33", "#FDD835", "#FFB300",
        "#FB8C00", "#F4511E", "#E53935", "#D81B60",
        "#8E24AA", "#6D4C41", "#546E7A", "#263238",
        "#00B0FF", "#00E676", "#FF1744", "#FF6D00"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Custom Color",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Color Preview & Hex Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeColor)
                            .border(1.dp, PaperBorderLight, RoundedCornerShape(14.dp))
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            val clean = if (input.startsWith("#")) input else "#$input"
                            if (clean.length == 7) {
                                try {
                                    val (h, s, v) = hexToHsv(clean)
                                    hue = h
                                    saturation = s
                                    value = v
                                } catch (_: Exception) {}
                            }
                        },
                        label = { Text("Hex Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Hue Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hue Spectrum",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${hue.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                    )

                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            hexInput = colorToHex(hsvToColor(hue, saturation, value))
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                // Saturation Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Saturation",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(saturation * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            hexInput = colorToHex(hsvToColor(hue, saturation, value))
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = NudgeBlue
                        )
                    )
                }

                // Brightness Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(value * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            hexInput = colorToHex(hsvToColor(hue, saturation, value))
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = NudgeBlue
                        )
                    )
                }

                // Palette Grid
                Text(
                    text = "Quick Palette Inspiration",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(curatedPalette) { hex ->
                        val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                        val isSelected = hex.equals(colorToHex(activeColor), ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable {
                                    val (h, s, v) = hexToHsv(hex)
                                    hue = h
                                    saturation = s
                                    value = v
                                    hexInput = hex.uppercase(Locale.US)
                                }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else PaperBorderLight,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(colorToHex(activeColor)) },
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * More Icons Picker Dialog showing all categorized icons.
 */
@Composable
private fun MoreIconsPickerDialog(
    selectedIcon: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Icon Symbol",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeGoal.ALL_MORE_ICONS.forEach { (categoryTitle, iconsList) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 11.sp
                            ),
                            color = NudgeBlue
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((((iconsList.size + 3) / 4) * 64).dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            items(iconsList) { (iconKey, label) ->
                                val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                                val dummyGoal = TimeGoal(name = "", dailyTargetMinutes = 0, startDate = "", iconName = iconKey)

                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable { onIconSelected(iconKey) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = dummyGoal.getIcon(),
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Helper HSV to Color conversion.
 */
private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    val argb = android.graphics.Color.HSVToColor(hsv)
    return Color(argb)
}

/**
 * Helper Color to Hex string conversion (#RRGGBB).
 */
private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

/**
 * Helper Hex string to HSV conversion.
 */
private fun hexToHsv(hex: String): Triple<Float, Float, Float> {
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        val parsed = android.graphics.Color.parseColor(clean)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(parsed, hsv)
        Triple(hsv[0], hsv[1], hsv[2])
    } catch (_: Exception) {
        Triple(260f, 0.7f, 0.9f)
    }
}

/**
 * Modal Sheet: Daily Time Entry Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimeEntrySheet(
    goal: TimeGoal,
    date: LocalDate,
    existingRecord: TimeGoalRecord?,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateStr = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US))
    }

    var minutesLogged by remember { mutableIntStateOf(existingRecord?.actualMinutes ?: 0) }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }

    var isCustomSelected by remember { mutableStateOf(false) }
    var customHours by remember { mutableIntStateOf(minutesLogged / 60) }
    var customMinutes by remember { mutableIntStateOf(minutesLogged % 60) }
    var customHoursText by remember { mutableStateOf((minutesLogged / 60).toString()) }
    var customMinutesText by remember { mutableStateOf((minutesLogged % 60).toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RECORD TIME FOR ${goal.name.uppercase(Locale.US)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = goal.parseColor()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Duration display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = TimeGoalCalculations.formatMinutes(minutesLogged),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = if (minutesLogged >= goal.dailyTargetMinutes) Color(0xFF2E7D32) else NudgeBlue
                    )
                    Text(
                        text = "Daily Target: ${TimeGoalCalculations.formatMinutes(goal.dailyTargetMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick select buttons
            val presets = listOf(
                goal.dailyTargetMinutes to "Target (${TimeGoalCalculations.formatMinutes(goal.dailyTargetMinutes)})",
                15 to "+15m",
                30 to "+30m",
                45 to "+45m",
                60 to "1 hour",
                90 to "1h 30m",
                120 to "2 hours",
                180 to "3 hours",
                0 to "Clear (0m)"
            )

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { (mins, label) ->
                    val isSelected = !isCustomSelected && minutesLogged == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                isCustomSelected = false
                                minutesLogged = mins
                                customHours = minutesLogged / 60
                                customMinutes = minutesLogged % 60
                                customHoursText = customHours.toString()
                                customMinutesText = customMinutes.toString()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Custom Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCustomSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable {
                            isCustomSelected = true
                            customHours = minutesLogged / 60
                            customMinutes = minutesLogged % 60
                            customHoursText = customHours.toString()
                            customMinutesText = customMinutes.toString()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Custom Time Input Card
            if (isCustomSelected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(PaperBorderLight, PaperBorderLight))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Exact Time",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%02d h %02d m", customHours, customMinutes),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NudgeBlue
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours Input & Stepper
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hours",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, PaperBorderLight, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (customHours > 0) {
                                                customHours -= 1
                                                customHoursText = customHours.toString()
                                                minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(
                                            text = "−",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = customHoursText,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }.take(3)
                                            customHoursText = filtered
                                            val h = filtered.toIntOrNull() ?: 0
                                            customHours = h
                                            minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.width(60.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            if (customHours < 24) {
                                                customHours += 1
                                                customHoursText = customHours.toString()
                                                minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            // Minutes Input & Stepper
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Minutes",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, PaperBorderLight, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            customMinutes = if (customMinutes >= 5) (customMinutes - 5) else 55
                                            customMinutesText = customMinutes.toString()
                                            minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(
                                            text = "−",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = customMinutesText,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }.take(2)
                                            customMinutesText = filtered
                                            val m = (filtered.toIntOrNull() ?: 0).coerceIn(0, 59)
                                            customMinutes = m
                                            minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.width(60.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            customMinutes = if (customMinutes <= 50) (customMinutes + 5) else 0
                                            customMinutesText = customMinutes.toString()
                                            minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Minutes Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 15, 20, 30, 45, 50).forEach { m ->
                                val isChipSelected = customMinutes == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isChipSelected) NudgeBlue.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isChipSelected) NudgeBlue else PaperBorderLight,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            customMinutes = m
                                            customMinutesText = m.toString()
                                            minutesLogged = (customHours * 60 + customMinutes).coerceAtLeast(0)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${m}m",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isChipSelected) NudgeBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add a note — what you did, learned, or why you couldn't complete your target...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NudgeBlue,
                    unfocusedBorderColor = PaperBorderLight
                ),
                minLines = 2,
                maxLines = 3
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(minutesLogged, note.ifBlank { null })
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
                ) {
                    Text("Save Time Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Month / Year Picker Dialog.
 */
@Composable
private fun MonthYearPickerDialog(
    currentYearMonth: YearMonth,
    onDismiss: () -> Unit,
    onSelectMonthYear: (YearMonth) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(currentYearMonth.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Year selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous year")
                    }
                    Text(
                        text = "$selectedYear",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next year")
                    }
                }

                // 12 Months Grid
                val monthNames = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0..2) {
                                val mIdx = row * 3 + col
                                val mNum = mIdx + 1
                                val isSelected = selectedYear == currentYearMonth.year && mNum == currentYearMonth.monthValue

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NudgeBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            onSelectMonthYear(YearMonth.of(selectedYear, mNum))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthNames[mIdx],
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
