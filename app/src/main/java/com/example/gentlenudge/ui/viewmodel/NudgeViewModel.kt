package com.example.gentlenudge.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gentlenudge.GentleNudgeApp
import com.example.gentlenudge.backup.BackupManifest
import com.example.gentlenudge.backup.BackupValidationResult
import com.example.gentlenudge.backup.NudgeBackupManager
import com.example.gentlenudge.backup.NudgeBackupScheduler
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalMonthColor
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.data.model.withMonthColor
import com.example.gentlenudge.data.model.withMonthColors
import com.example.gentlenudge.data.repository.NudgeRepository
import com.example.gentlenudge.export.HistoryPdfExporter
import com.example.gentlenudge.export.MonthReportData
import com.example.gentlenudge.export.TimeGoalsPdfExporter
import com.example.gentlenudge.export.TimeGoalsPdfOptions
import com.example.gentlenudge.nlp.NudgeNlpParser
import com.example.gentlenudge.nlp.ParsedNudge
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.ui.theme.ThemeMode
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import com.example.gentlenudge.ui.components.TaskOccurrenceResolver
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NudgeViewModel(
    application: Application,
    private val repository: NudgeRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)

    private val _dateTick = MutableStateFlow(System.currentTimeMillis())
    val currentDateMillis: StateFlow<Long> = _dateTick.asStateFlow()

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _dateTick.value = System.currentTimeMillis()
        }
    }

    val allTasks: StateFlow<List<NudgeTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayTasks: StateFlow<List<NudgeTask>> = combine(allTasks, _dateTick) { tasks, now ->
        val pending = tasks.filter { !it.isDone && !it.isDeleted }
        pending.filter { task ->
            val occurrence = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, application, now)
            TaskOccurrenceResolver.isOccurrenceTodayOrPast(occurrence, now)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val laterTasks: StateFlow<List<NudgeTask>> = combine(allTasks, _dateTick) { tasks, now ->
        val pending = tasks.filter { !it.isDone && !it.isDeleted }
        pending.filter { task ->
            val occurrence = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, application, now)
            !TaskOccurrenceResolver.isOccurrenceTodayOrPast(occurrence, now)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedTasks: StateFlow<List<NudgeTask>> = repository.completedTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTasks: StateFlow<List<NudgeTask>> = repository.deletedTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTasksCount: StateFlow<Int> = repository.deletedTasksCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Preferences
    private val _themeMode = MutableStateFlow(
        run {
            val saved = prefs.getString("app_theme_mode", null)
            if (saved != null) {
                try {
                    ThemeMode.valueOf(saved)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
            } else {
                ThemeMode.LIGHT
            }
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _nudgeAgainEnabled = MutableStateFlow(
        prefs.getBoolean("nudge_again_enabled", true)
    )
    val nudgeAgainEnabled: StateFlow<Boolean> = _nudgeAgainEnabled.asStateFlow()

    private val _defaultSnooze = MutableStateFlow(
        prefs.getString("default_snooze", "30 minutes") ?: "30 minutes"
    )
    val defaultSnooze: StateFlow<String> = _defaultSnooze.asStateFlow()

    private val _userName = MutableStateFlow(
        prefs.getString("user_name", "Bloom")?.ifBlank { "Bloom" } ?: "Bloom"
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Backup & Data Safety State Flows
    private val _lastManualBackupTimestamp = MutableStateFlow(
        NudgeBackupScheduler.getLastManualBackupTimestamp(prefs)
    )
    val lastManualBackupTimestamp: StateFlow<Long> = _lastManualBackupTimestamp.asStateFlow()

    private val _lastAutoBackupTimestamp = MutableStateFlow(
        NudgeBackupScheduler.getLastAutoBackupTimestamp(prefs)
    )
    val lastAutoBackupTimestamp: StateFlow<Long> = _lastAutoBackupTimestamp.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(
        NudgeBackupScheduler.getLastManualBackupTimestamp(prefs)
    )
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _autoBackupFiles = MutableStateFlow<List<NudgeBackupManager.AutoBackupFileInfo>>(emptyList())
    val autoBackupFiles: StateFlow<List<NudgeBackupManager.AutoBackupFileInfo>> = _autoBackupFiles.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(
        prefs.getBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, false)
    )
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _autoBackupDay = MutableStateFlow(
        prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, Calendar.SUNDAY)
    )
    val autoBackupDay: StateFlow<Int> = _autoBackupDay.asStateFlow()

    private val _autoBackupHour = MutableStateFlow(
        prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, 23)
    )
    val autoBackupHour: StateFlow<Int> = _autoBackupHour.asStateFlow()

    private val _autoBackupMinute = MutableStateFlow(
        prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, 0)
    )
    val autoBackupMinute: StateFlow<Int> = _autoBackupMinute.asStateFlow()

    // Editing state
    private val _taskToEdit = MutableStateFlow<NudgeTask?>(null)
    val taskToEdit: StateFlow<NudgeTask?> = _taskToEdit.asStateFlow()

    // The Life Within the Hours - State Flows
    val allTimeGoals: StateFlow<List<TimeGoal>> = repository.allTimeGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTimeGoals: StateFlow<List<TimeGoal>> = repository.activeTimeGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTimeGoalRecords: StateFlow<List<TimeGoalRecord>> = repository.allTimeGoalRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecordedMonths: StateFlow<List<String>> = repository.allRecordedMonths
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTimeGoalMonthColors: StateFlow<List<TimeGoalMonthColor>> = repository.allMonthColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthColorsMap: StateFlow<Map<Pair<Long, String>, String>> = repository.allMonthColors
        .map { list -> list.associate { (it.goalId to it.yearMonth) to it.colorHex } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedMonthRecords: StateFlow<List<TimeGoalRecord>> = _selectedYearMonth
        .flatMapLatest { ym ->
            val prefix = ym.format(DateTimeFormatter.ofPattern("yyyy-MM-", Locale.US))
            repository.getRecordsForMonth(prefix)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedGoalForDetail = MutableStateFlow<TimeGoal?>(null)
    val selectedGoalForDetail: StateFlow<TimeGoal?> = _selectedGoalForDetail.asStateFlow()

    val filteredTasks: StateFlow<List<NudgeTask>> = combine(allTasks, _searchQuery) { tasks, query ->
        if (query.isBlank()) {
            tasks
        } else {
            tasks.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true) ||
                task.dateLabel.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val dateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        try {
            application.registerReceiver(dateChangeReceiver, dateFilter)
        } catch (_: Exception) {}

        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val nextMidnight = TaskOccurrenceResolver.getNextMidnightMillis(now)
                val delayMs = maxOf(500L, nextMidnight - now + 200L)
                delay(delayMs)
                _dateTick.value = System.currentTimeMillis()
            }
        }

        // Schedule all active pending tasks initially and cleanup expired deleted items (>30 days)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If tasks only contain old default tasks or need updating to the new default tasks
                if (!prefs.getBoolean("default_sample_tasks_v2", false)) {
                    val currentTasks = repository.getAllTasksList()
                    val oldTitles = setOf("Call Rahul", "Buy toothpaste", "Check the washing machine", "Recharge phone", "Send documents", "Reply to Meera")
                    val isOnlyOldDefaults = currentTasks.isEmpty() || currentTasks.all { it.title in oldTitles }
                    if (isOnlyOldDefaults) {
                        repository.clearAll()
                        val newSeedTasks = listOf(
                            NudgeTask(
                                title = "Pick up the laundry",
                                timeLabel = "10:00 AM",
                                dateLabel = "Today",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "today"
                            ),
                            NudgeTask(
                                title = "Call mummy",
                                timeLabel = "1:30 PM",
                                dateLabel = "Today",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "today"
                            ),
                            NudgeTask(
                                title = "Take notes",
                                timeLabel = "4:00 PM",
                                dateLabel = "Today",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "today"
                            ),
                            NudgeTask(
                                title = "Pay the electricity bill",
                                timeLabel = "7:00 PM",
                                dateLabel = "Today",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "today"
                            ),
                            NudgeTask(
                                title = "Book the train ticket",
                                timeLabel = "Tomorrow, 10:00 AM",
                                dateLabel = "Tomorrow",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "later"
                            ),
                            NudgeTask(
                                title = "Cancel the subscription",
                                timeLabel = "Fri, 2:00 PM",
                                dateLabel = "Friday",
                                category = "",
                                priority = "Normal",
                                repeat = "Does not repeat",
                                isDone = false,
                                section = "later"
                            )
                        )
                        repository.insertAll(newSeedTasks)
                    }
                    prefs.edit().putBoolean("default_sample_tasks_v2", true).apply()
                }

                if (!prefs.getBoolean("default_sample_goals_v2", false)) {
                    val currentGoals = repository.getAllTimeGoalsList()
                    val oldGoalNames = setOf("Reading", "Coding", "Watching Course", "Exercise")
                    val isOnlyOldDefaults = currentGoals.isEmpty() || (currentGoals.size <= 4 && currentGoals.all { it.name in oldGoalNames })
                    if (isOnlyOldDefaults) {
                        val now = java.time.LocalDate.now()
                        val currentYearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-", java.util.Locale.US).format(now)
                        val startOfMonth = "${currentYearMonth}01"

                        repository.clearAllGoals()
                        val newStarterGoals = listOf(
                            TimeGoal(
                                id = 1,
                                name = "Reading",
                                dailyTargetMinutes = 120, // 2 hours
                                colorHex = "#7C4DFF",
                                iconName = "book",
                                startDate = startOfMonth,
                                orderIndex = 0
                            ),
                            TimeGoal(
                                id = 2,
                                name = "Exercise",
                                dailyTargetMinutes = 60, // 1 hour
                                colorHex = "#1E88E5",
                                iconName = "fitness",
                                startDate = startOfMonth,
                                orderIndex = 1
                            ),
                            TimeGoal(
                                id = 3,
                                name = "Family Time",
                                dailyTargetMinutes = 60, // 1 hour
                                colorHex = "#E65100",
                                iconName = "community",
                                startDate = startOfMonth,
                                orderIndex = 2
                            ),
                            TimeGoal(
                                id = 4,
                                name = "Personal Projects",
                                dailyTargetMinutes = 120, // 2 hours
                                colorHex = "#2E7D32",
                                iconName = "work",
                                startDate = startOfMonth,
                                orderIndex = 3
                            )
                        )
                        repository.insertAllGoals(newStarterGoals)
                    }
                    prefs.edit().putBoolean("default_sample_goals_v2", true).apply()
                }

                repository.cleanupExpiredDeletedTasks()
            } catch (_: Exception) {}

            if (_notificationsEnabled.value) {
                try {
                    val pendingTasks = repository.getAllTasksList().filter { !it.isDone && !it.isDeleted }
                    for (task in pendingTasks) {
                        NudgeAlarmScheduler.scheduleTask(getApplication(), task, forceRecalculate = false)
                    }
                } catch (_: Exception) {}
            }
        }

        checkAutoBackupCatchUp()
        loadAutoBackupFiles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setTaskToEdit(task: NudgeTask?) {
        _taskToEdit.value = task
    }

    fun toggleNotifications() {
        val newState = !_notificationsEnabled.value
        _notificationsEnabled.value = newState
        prefs.edit().putBoolean("notifications_enabled", newState).apply()
        if (newState) {
            viewModelScope.launch {
                allTasks.value.filter { !it.isDone }.forEach { task ->
                    NudgeAlarmScheduler.scheduleTask(getApplication(), task, forceRecalculate = false)
                }
            }
            showToast("Quiet notifications turned on.")
        } else {
            allTasks.value.forEach { task ->
                NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            }
            showToast("Notifications silenced.")
        }
    }

    fun toggleNudgeAgain() {
        val newState = !_nudgeAgainEnabled.value
        _nudgeAgainEnabled.value = newState
        prefs.edit().putBoolean("nudge_again_enabled", newState).apply()
    }

    fun setDefaultSnooze(snooze: String) {
        _defaultSnooze.value = snooze
        prefs.edit().putString("default_snooze", snooze).apply()
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isBlank()) "Bloom" else trimmed
        _userName.value = finalName
        prefs.edit().putString("user_name", finalName).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit()
            .putString("app_theme_mode", mode.name)
            .putBoolean("dark_theme", mode == ThemeMode.DARK)
            .apply()
    }

    private fun isTodayDate(dateLabel: String): Boolean {
        if (dateLabel.equals("Today", ignoreCase = true) || dateLabel.equals("Tonight", ignoreCase = true)) {
            return true
        }
        val formats = listOf(
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            SimpleDateFormat("MMM d", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMM d", Locale.US)
        )
        val now = Calendar.getInstance()
        for (fmt in formats) {
            try {
                val parsed = fmt.parse(dateLabel)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply { time = parsed }
                    val currentYear = now.get(Calendar.YEAR)
                    val matchesYear = !dateLabel.contains(Regex("\\b20\\d{2}\\b")) || (cal.get(Calendar.YEAR) == currentYear)
                    if (matchesYear &&
                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                        cal.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
                    ) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun parseNaturalLanguage(input: String): ParsedNudge {
        return NudgeNlpParser.parse(input)
    }

    fun addTask(
        title: String,
        dateLabel: String = "Today",
        timeLabel: String = "",
        category: String = "Personal",
        priority: String = "Normal",
        repeat: String = "Does not repeat",
        soundType: String = "Small nudge",
        ringtoneUri: String? = null,
        ringtoneTitle: String? = null,
        attachments: List<TaskAttachment> = emptyList()
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            showToast("Give this little nudge a few words first.")
            return
        }

        val isToday = isTodayDate(dateLabel)
        val finalTime = if (timeLabel.isNotBlank()) timeLabel else if (isToday) "Any time" else dateLabel
        val section = if (isToday) "today" else "later"
        val attachmentsJson = if (attachments.isNotEmpty()) TaskAttachment.listToJson(attachments) else null

        val task = NudgeTask(
            title = trimmed,
            timeLabel = finalTime,
            dateLabel = dateLabel,
            category = category,
            priority = priority,
            repeat = repeat,
            soundType = soundType,
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle,
            attachmentsJson = attachmentsJson,
            isDone = false,
            section = section
        )

        viewModelScope.launch {
            val generatedId = repository.insert(task)
            val savedTask = task.copy(id = generatedId)
            if (_notificationsEnabled.value) {
                NudgeAlarmScheduler.scheduleTask(getApplication(), savedTask, forceRecalculate = true)
            }
            showToast("Saved. You can forget about it now.")
        }
    }

    private fun isRepeatingRule(repeat: String?): Boolean {
        if (repeat.isNullOrBlank()) return false
        val trimmed = repeat.trim()
        return !trimmed.equals("Does not repeat", ignoreCase = true) && !trimmed.equals("Once", ignoreCase = true)
    }

    fun updateTask(
        task: NudgeTask,
        title: String,
        dateLabel: String,
        timeLabel: String,
        category: String,
        priority: String,
        repeat: String,
        soundType: String = task.soundType,
        ringtoneUri: String? = task.ringtoneUri,
        ringtoneTitle: String? = task.ringtoneTitle,
        attachments: List<TaskAttachment> = task.attachments
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            showToast("Title cannot be empty.")
            return
        }

        val isToday = isTodayDate(dateLabel)
        val finalTime = if (timeLabel.isNotBlank()) timeLabel else if (isToday) "Any time" else dateLabel
        val section = if (isToday) "today" else "later"
        val attachmentsJson = if (attachments.isNotEmpty()) TaskAttachment.listToJson(attachments) else null

        val isRepeatingOption = isRepeatingRule(repeat)
        val wasDoneAndNowRepeating = task.isDone && isRepeatingOption
        val newIsDone = if (wasDoneAndNowRepeating) false else task.isDone
        val newCompletedAt = if (!newIsDone) null else task.completedAt

        val scheduleChanged = (dateLabel != task.dateLabel || finalTime != task.timeLabel || repeat != task.repeat)
        val dateChanged = (dateLabel != task.dateLabel)
        val newCreatedAt = if (dateChanged) System.currentTimeMillis() else task.createdAt

        val updated = task.copy(
            title = trimmed,
            dateLabel = dateLabel,
            timeLabel = finalTime,
            category = category,
            priority = priority,
            repeat = repeat,
            soundType = soundType,
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle,
            attachmentsJson = attachmentsJson,
            isDone = newIsDone,
            completedAt = newCompletedAt,
            section = section,
            createdAt = newCreatedAt
        )

        viewModelScope.launch {
            repository.update(updated)
            if (_notificationsEnabled.value && !updated.isDone) {
                NudgeAlarmScheduler.scheduleTask(getApplication(), updated, forceRecalculate = scheduleChanged)
            } else if (updated.isDone) {
                NudgeAlarmScheduler.cancelTask(getApplication(), updated.id)
            }
            showToast("Nudge updated.")
        }
    }

    fun updateTask(task: NudgeTask) {
        val isToday = isTodayDate(task.dateLabel)
        val section = if (isToday) "today" else "later"
        val isRepeatingOption = isRepeatingRule(task.repeat)
        val wasDoneAndNowRepeating = task.isDone && isRepeatingOption
        val newIsDone = if (wasDoneAndNowRepeating) false else task.isDone
        val newCompletedAt = if (!newIsDone) null else task.completedAt

        viewModelScope.launch {
            val existing = repository.getTaskById(task.id)
            val scheduleChanged = existing == null ||
                existing.dateLabel != task.dateLabel ||
                existing.timeLabel != task.timeLabel ||
                existing.repeat != task.repeat
            val dateChanged = existing != null && existing.dateLabel != task.dateLabel
            val newCreatedAt = if (dateChanged) System.currentTimeMillis() else (existing?.createdAt ?: task.createdAt)

            val updated = task.copy(
                isDone = newIsDone,
                completedAt = newCompletedAt,
                section = section,
                createdAt = newCreatedAt
            )

            repository.update(updated)
            if (_notificationsEnabled.value && !updated.isDone) {
                NudgeAlarmScheduler.scheduleTask(getApplication(), updated, forceRecalculate = scheduleChanged)
            } else if (updated.isDone) {
                NudgeAlarmScheduler.cancelTask(getApplication(), updated.id)
            }
            showToast("Nudge updated.")
        }
    }

    fun toggleDone(task: NudgeTask) {
        viewModelScope.launch {
            repository.toggleDone(task)
            if (!task.isDone) {
                NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
                showToast("Done — one less thing to hold in your head.")
            } else {
                if (_notificationsEnabled.value) {
                    NudgeAlarmScheduler.scheduleTask(getApplication(), task.copy(isDone = false, completedAt = null), forceRecalculate = true)
                }
                showToast("Moved back to your active nudges.")
            }
        }
    }

    fun snoozeTask(task: NudgeTask, snoozeLabel: String) {
        viewModelScope.launch {
            val (newTimeLabel, newDateLabel) = calculateSnoozeLabels(snoozeLabel)
            val updated = task.copy(
                timeLabel = newTimeLabel,
                dateLabel = newDateLabel,
                section = if (newDateLabel == "Today" || newDateLabel == "Tonight") "today" else "later",
                isDone = false,
                completedAt = null
            )
            repository.update(updated)
            if (_notificationsEnabled.value) {
                NudgeAlarmScheduler.scheduleTask(getApplication(), updated, forceRecalculate = true)
            }
            showToast("Okay. I’ll nudge you ${snoozeLabel.lowercase()}.")
        }
    }

    private fun calculateSnoozeLabels(snoozeOption: String): Pair<String, String> {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val now = System.currentTimeMillis()
        return when (snoozeOption.lowercase().trim()) {
            "15m", "15 minutes" -> {
                Pair(timeFormat.format(Date(now + 15 * 60 * 1000L)), "Today")
            }
            "30m", "30 minutes" -> {
                Pair(timeFormat.format(Date(now + 30 * 60 * 1000L)), "Today")
            }
            "1h", "1 hour" -> {
                Pair(timeFormat.format(Date(now + 60 * 60 * 1000L)), "Today")
            }
            "tonight" -> {
                Pair("8:00 PM", "Tonight")
            }
            "tomorrow" -> {
                Pair("9:00 AM", "Tomorrow")
            }
            else -> {
                Pair(timeFormat.format(Date(now + 30 * 60 * 1000L)), "Today")
            }
        }
    }

    fun deleteTask(task: NudgeTask) {
        viewModelScope.launch {
            repository.softDelete(task)
            NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            showToast("Moved to Recently Deleted.")
        }
    }

    fun restoreTask(task: NudgeTask) {
        viewModelScope.launch {
            repository.restoreTask(task)
            if (!task.isDone) {
                val restored = task.copy(isDeleted = false, deletedAt = null)
                if (_notificationsEnabled.value) {
                    NudgeAlarmScheduler.scheduleTask(getApplication(), restored)
                }
            }
            showToast("Reminder restored.")
        }
    }

    fun permanentlyDeleteTask(task: NudgeTask) {
        viewModelScope.launch {
            repository.permanentlyDelete(task)
            NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            showToast("Permanently deleted.")
        }
    }

    fun permanentlyDeleteTasks(tasks: List<NudgeTask>) {
        if (tasks.isEmpty()) return
        viewModelScope.launch {
            tasks.forEach { task ->
                repository.permanentlyDelete(task)
                NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            }
            val count = tasks.size
            showToast(if (count == 1) "1 completed nudge deleted." else "$count completed nudges deleted.")
        }
    }

    fun emptyRecentlyDeleted() {
        viewModelScope.launch {
            val deleted = repository.getDeletedTasksList()
            deleted.forEach { task ->
                NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            }
            repository.permanentlyDeleteAllRecentlyDeleted()
            showToast("Recently Deleted cleared.")
        }
    }

    fun cleanupRecentlyDeleted() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupExpiredDeletedTasks()
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            val existingTasks = repository.getAllTasksList()
            existingTasks.forEach { task ->
                NudgeAlarmScheduler.cancelTask(getApplication(), task.id)
            }
            repository.clearAll()
            val seedTasks = listOf(
                NudgeTask(
                    title = "Pick up the laundry",
                    timeLabel = "10:00 AM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Call mummy",
                    timeLabel = "1:30 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Take notes",
                    timeLabel = "4:00 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Pay the electricity bill",
                    timeLabel = "7:00 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Book the train ticket",
                    timeLabel = "Tomorrow, 10:00 AM",
                    dateLabel = "Tomorrow",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "later"
                ),
                NudgeTask(
                    title = "Cancel the subscription",
                    timeLabel = "Fri, 2:00 PM",
                    dateLabel = "Friday",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "later"
                )
            )
            repository.insertAll(seedTasks)
            seedTasks.filter { !it.isDone }.forEach {
                if (_notificationsEnabled.value) {
                    NudgeAlarmScheduler.scheduleTask(getApplication(), it)
                }
            }

            val now = java.time.LocalDate.now()
            val currentYearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-", java.util.Locale.US).format(now)
            val startOfMonth = "${currentYearMonth}01"
            repository.clearAllGoals()
            val newStarterGoals = listOf(
                TimeGoal(
                    id = 1,
                    name = "Reading",
                    dailyTargetMinutes = 120, // 2 hours
                    colorHex = "#7C4DFF",
                    iconName = "book",
                    startDate = startOfMonth,
                    orderIndex = 0
                ),
                TimeGoal(
                    id = 2,
                    name = "Exercise",
                    dailyTargetMinutes = 60, // 1 hour
                    colorHex = "#1E88E5",
                    iconName = "fitness",
                    startDate = startOfMonth,
                    orderIndex = 1
                ),
                TimeGoal(
                    id = 3,
                    name = "Family Time",
                    dailyTargetMinutes = 60, // 1 hour
                    colorHex = "#E65100",
                    iconName = "community",
                    startDate = startOfMonth,
                    orderIndex = 2
                ),
                TimeGoal(
                    id = 4,
                    name = "Personal Projects",
                    dailyTargetMinutes = 120, // 2 hours
                    colorHex = "#2E7D32",
                    iconName = "work",
                    startDate = startOfMonth,
                    orderIndex = 3
                )
            )
            repository.insertAllGoals(newStarterGoals)

            showToast("Reset to calm starter nudges.")
        }
    }

    fun createBackup(uri: Uri, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<GentleNudgeApp>()
            val result = withContext(Dispatchers.IO) {
                NudgeBackupManager.writeBackupToUri(app, uri, app.database, prefs)
            }
            result.onSuccess { manifest ->
                val now = System.currentTimeMillis()
                _lastManualBackupTimestamp.value = now
                _lastBackupTimestamp.value = now

                val msg = "Backup complete: ${manifest.tasksCount} notes, ${manifest.timeGoalsCount} pursuits saved."
                showToast(msg)
                onComplete?.invoke(true, msg)
            }.onFailure { e ->
                val msg = "Could not create backup: ${e.localizedMessage ?: e.message}"
                showToast(msg)
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun toggleAutoBackup() {
        val next = !_autoBackupEnabled.value
        _autoBackupEnabled.value = next
        val now = System.currentTimeMillis()
        val day = _autoBackupDay.value
        val hour = _autoBackupHour.value
        val minute = _autoBackupMinute.value
        val editor = prefs.edit().putBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, next)
        if (next) {
            editor.putLong(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED_AT, now)
            val prevOccurrence = NudgeBackupScheduler.calculatePreviousScheduledTime(now, day, hour, minute)
            editor.putLong(NudgeBackupScheduler.KEY_LAST_PROCESSED_OCCURRENCE, prevOccurrence)
        }
        editor.apply()
        NudgeBackupScheduler.scheduleAutoBackup(getApplication())
        if (next) {
            val nextFormatted = NudgeBackupScheduler.formatNextAutoBackup(day, hour, minute)
            showToast("Automatic backup on. Next: $nextFormatted")
        } else {
            showToast("Automatic backup turned off.")
        }
        loadAutoBackupFiles()
    }

    fun setAutoBackupSchedule(dayOfWeek: Int, hour: Int, minute: Int) {
        _autoBackupDay.value = dayOfWeek
        _autoBackupHour.value = hour
        _autoBackupMinute.value = minute
        val now = System.currentTimeMillis()
        val prevOccurrence = NudgeBackupScheduler.calculatePreviousScheduledTime(now, dayOfWeek, hour, minute)
        prefs.edit()
            .putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, dayOfWeek)
            .putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, hour)
            .putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, minute)
            .putLong(NudgeBackupScheduler.KEY_LAST_PROCESSED_OCCURRENCE, prevOccurrence)
            .apply()
        NudgeBackupScheduler.scheduleAutoBackup(getApplication())
        val nextFormatted = NudgeBackupScheduler.formatNextAutoBackup(dayOfWeek, hour, minute)
        showToast("Schedule updated. Next backup: $nextFormatted")
    }

    fun refreshLastBackupTimestamp() {
        _lastManualBackupTimestamp.value = NudgeBackupScheduler.getLastManualBackupTimestamp(prefs)
        _lastAutoBackupTimestamp.value = NudgeBackupScheduler.getLastAutoBackupTimestamp(prefs)
        _lastBackupTimestamp.value = NudgeBackupScheduler.getLastManualBackupTimestamp(prefs)
        loadAutoBackupFiles()
    }

    fun loadAutoBackupFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = NudgeBackupManager.getAutoBackupFilesInfo(getApplication())
            _autoBackupFiles.value = files
        }
    }

    fun checkAutoBackupCatchUp() {
        NudgeBackupScheduler.checkAndPerformCatchUpIfMissed(getApplication())
        refreshLastBackupTimestamp()
    }

    fun triggerAutoBackupNow(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<GentleNudgeApp>()
            val result = withContext(Dispatchers.IO) {
                NudgeBackupManager.performAutomaticBackup(app, app.database, prefs)
            }
            result.onSuccess { file ->
                val now = System.currentTimeMillis()
                _lastAutoBackupTimestamp.value = now
                val dayOfWeek = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, Calendar.SUNDAY)
                val hour = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, 23)
                val minute = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, 0)
                val occurrence = NudgeBackupScheduler.calculatePreviousScheduledTime(now, dayOfWeek, hour, minute)
                prefs.edit().putLong(NudgeBackupScheduler.KEY_LAST_PROCESSED_OCCURRENCE, occurrence).apply()
                loadAutoBackupFiles()
                val msg = "Auto backup created safely: ${file.name}"
                showToast(msg)
                onComplete?.invoke(true, msg)
            }.onFailure { error ->
                val msg = "Auto backup failed: ${error.localizedMessage ?: error.message}"
                showToast(msg)
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun validateBackup(uri: Uri, onResult: (BackupValidationResult?) -> Unit) {
        viewModelScope.launch {
            val app = getApplication<GentleNudgeApp>()
            val result = withContext(Dispatchers.IO) {
                NudgeBackupManager.validateBackupFromUri(app, uri)
            }
            result.onSuccess { validation ->
                onResult(validation)
            }.onFailure { e ->
                val msg = "Invalid backup: ${e.localizedMessage ?: e.message}"
                showToast(msg)
                onResult(null)
            }
        }
    }

    fun restoreBackup(validation: BackupValidationResult, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val app = getApplication<GentleNudgeApp>()
                withContext(Dispatchers.IO) {
                    NudgeBackupManager.restoreValidatedBackup(app, validation, app.database, prefs)
                }

                // Sync UI state flows with restored preferences
                val restoredTheme = prefs.getString("app_theme_mode", null)
                _themeMode.value = if (restoredTheme != null) {
                    try { ThemeMode.valueOf(restoredTheme) } catch (e: Exception) { ThemeMode.SYSTEM }
                } else {
                    ThemeMode.SYSTEM
                }
                _notificationsEnabled.value = prefs.getBoolean("notifications_enabled", true)
                _nudgeAgainEnabled.value = prefs.getBoolean("nudge_again_enabled", true)
                _defaultSnooze.value = prefs.getString("default_snooze", "30 minutes") ?: "30 minutes"
                _userName.value = prefs.getString("user_name", "Bloom")?.ifBlank { "Bloom" } ?: "Bloom"

                _autoBackupEnabled.value = prefs.getBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, false)
                _autoBackupDay.value = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, Calendar.SUNDAY)
                _autoBackupHour.value = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, 23)
                _autoBackupMinute.value = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, 0)

                // Reschedule notifications for active restored tasks
                if (_notificationsEnabled.value) {
                    withContext(Dispatchers.IO) {
                        val restoredPendingTasks = repository.getAllTasksList().filter { !it.isDone && !it.isDeleted }
                        for (task in restoredPendingTasks) {
                            NudgeAlarmScheduler.scheduleTask(app, task)
                        }
                    }
                }

                // Restore background scheduling state for Automatic Backup and Calendar Events
                NudgeBackupScheduler.scheduleAutoBackup(app)
                NudgeEventNotificationScheduler.rescheduleIfEnabled(app)

                val msg = "Restored: ${validation.tasks.size} notes and ${validation.timeGoals.size} pursuits."
                showToast(msg)
                onComplete?.invoke(true, msg)
            } catch (e: Exception) {
                val msg = "Could not restore backup: ${e.localizedMessage ?: e.message}"
                showToast(msg)
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun exportBackupJson(): String {
        val tasks = allTasks.value
        val jsonArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("timeLabel", t.timeLabel)
                put("dateLabel", t.dateLabel)
                put("category", t.category)
                put("priority", t.priority)
                put("repeat", t.repeat)
                put("soundType", t.soundType)
                if (t.ringtoneUri != null) {
                    put("ringtoneUri", t.ringtoneUri)
                }
                if (t.ringtoneTitle != null) {
                    put("ringtoneTitle", t.ringtoneTitle)
                }
                if (t.attachmentsJson != null) {
                    put("attachmentsJson", t.attachmentsJson)
                }
                put("isDone", t.isDone)
                put("section", t.section)
                put("createdAt", t.createdAt)
                if (t.completedAt != null) {
                    put("completedAt", t.completedAt)
                }
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun importBackupJson(jsonString: String): Result<Int> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val parsedTasks = mutableListOf<NudgeTask>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                parsedTasks.add(
                    NudgeTask(
                        title = obj.getString("title"),
                        timeLabel = obj.optString("timeLabel", "Any time"),
                        dateLabel = obj.optString("dateLabel", "Today"),
                        category = obj.optString("category", "Personal"),
                        priority = obj.optString("priority", "Normal"),
                        repeat = obj.optString("repeat", "Does not repeat"),
                        soundType = obj.optString("soundType", "Small nudge"),
                        ringtoneUri = if (obj.has("ringtoneUri")) obj.getString("ringtoneUri") else null,
                        ringtoneTitle = if (obj.has("ringtoneTitle")) obj.getString("ringtoneTitle") else null,
                        attachmentsJson = if (obj.has("attachmentsJson")) obj.getString("attachmentsJson") else null,
                        isDone = obj.optBoolean("isDone", false),
                        section = obj.optString("section", "today"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        completedAt = if (obj.has("completedAt")) obj.getLong("completedAt") else null
                    )
                )
            }
            viewModelScope.launch {
                repository.clearAll()
                repository.insertAll(parsedTasks)
                parsedTasks.filter { !it.isDone }.forEach {
                    if (_notificationsEnabled.value) {
                        NudgeAlarmScheduler.scheduleTask(getApplication(), it)
                    }
                }
                showToast("Your saved nudges are back where they belong.")
            }
            Result.success(parsedTasks.size)
        } catch (e: Exception) {
            showToast("That file doesn’t look like a Nudge backup.")
            Result.failure(e)
        }
    }

    fun exportHistoryPdf(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    val tasks = repository.getAllTasksList()
                    HistoryPdfExporter.generateHistoryPdf(
                        context = getApplication(),
                        tasks = tasks
                    )
                }
                onFileReady(pdfFile)
            } catch (e: Exception) {
                showToast("Could not generate history PDF: ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // THE LIFE WITHIN THE HOURS — Actions & Logic
    // ==========================================

    fun setSelectedYearMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
    }

    fun goToPreviousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun goToCurrentMonth() {
        _selectedYearMonth.value = YearMonth.now()
    }

    fun setSelectedGoalForDetail(goal: TimeGoal?) {
        _selectedGoalForDetail.value = goal
    }

    fun addTimeGoal(
        name: String,
        dailyTargetMinutes: Int,
        colorHex: String = "#7C4DFF",
        iconName: String = "book",
        startDate: String = TimeGoalCalculations.formatLocalDate(LocalDate.now().withDayOfMonth(1)),
        yearMonth: YearMonth = _selectedYearMonth.value
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            showToast("Please enter a name for your pursuit.")
            return
        }
        if (dailyTargetMinutes <= 0) {
            showToast("Daily target must be at least 1 minute.")
            return
        }

        viewModelScope.launch {
            val currentGoals = repository.getAllTimeGoalsList()
            val maxOrder = currentGoals.maxOfOrNull { it.orderIndex } ?: -1
            val goal = TimeGoal(
                name = trimmed,
                dailyTargetMinutes = dailyTargetMinutes,
                colorHex = colorHex,
                iconName = iconName,
                startDate = startDate,
                orderIndex = maxOrder + 1
            )
            val insertedId = repository.insertTimeGoal(goal)
            val ymStr = yearMonth.toString()
            repository.insertOrUpdateMonthColor(
                TimeGoalMonthColor(
                    goalId = insertedId,
                    yearMonth = ymStr,
                    colorHex = colorHex
                )
            )
            showToast("Pursuit added. Shape your hours.")
        }
    }

    fun reorderTimeGoals(newOrder: List<TimeGoal>) {
        viewModelScope.launch {
            val allGoals = repository.getAllTimeGoalsList()
            val activeIds = newOrder.map { it.id }.toSet()
            val activeUpdated = newOrder.mapIndexed { index, goal ->
                goal.copy(orderIndex = index)
            }
            val remainingGoals = allGoals.filter { it.id !in activeIds }
            val remainingUpdated = remainingGoals.mapIndexed { index, goal ->
                goal.copy(orderIndex = newOrder.size + index)
            }
            val allUpdated = activeUpdated + remainingUpdated
            repository.insertAllGoals(allUpdated)
        }
    }

    fun updateTimeGoal(
        goal: TimeGoal,
        name: String,
        dailyTargetMinutes: Int,
        colorHex: String,
        iconName: String,
        startDate: String,
        yearMonth: YearMonth = _selectedYearMonth.value,
        isArchived: Boolean = goal.isArchived
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            showToast("Goal name cannot be empty.")
            return
        }

        viewModelScope.launch {
            val updated = goal.copy(
                name = trimmed,
                dailyTargetMinutes = dailyTargetMinutes,
                iconName = iconName,
                startDate = startDate,
                isArchived = isArchived
            )
            repository.updateTimeGoal(updated)
            val ymStr = yearMonth.toString()
            repository.insertOrUpdateMonthColor(
                TimeGoalMonthColor(
                    goalId = goal.id,
                    yearMonth = ymStr,
                    colorHex = colorHex
                )
            )
            if (_selectedGoalForDetail.value?.id == goal.id) {
                _selectedGoalForDetail.value = updated.copy(colorHex = colorHex)
            }
            showToast("Pursuit updated.")
        }
    }

    fun setCategoryMonthColor(goalId: Long, yearMonth: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertOrUpdateMonthColor(
                TimeGoalMonthColor(
                    goalId = goalId,
                    yearMonth = yearMonth,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteTimeGoal(goal: TimeGoal) {
        viewModelScope.launch {
            repository.deleteTimeGoal(goal)
            if (_selectedGoalForDetail.value?.id == goal.id) {
                _selectedGoalForDetail.value = null
            }
            showToast("Pursuit and its records removed.")
        }
    }

    fun toggleArchiveTimeGoal(goal: TimeGoal) {
        viewModelScope.launch {
            val updated = goal.copy(isArchived = !goal.isArchived)
            repository.updateTimeGoal(updated)
            if (_selectedGoalForDetail.value?.id == goal.id) {
                _selectedGoalForDetail.value = updated
            }
            val msg = if (updated.isArchived) "Pursuit archived." else "Pursuit restored."
            showToast(msg)
        }
    }

    fun recordDailyTime(goalId: Long, date: String, minutes: Int, note: String? = null) {
        viewModelScope.launch {
            repository.recordDailyTime(goalId, date, minutes, note)
            val timeStr = TimeGoalCalculations.formatMinutes(minutes)
            showToast("Recorded $timeStr for $date.")
        }
    }

    fun exportTimeGoalsPdf(
        yearMonth: YearMonth = _selectedYearMonth.value,
        options: TimeGoalsPdfOptions = TimeGoalsPdfOptions(),
        onFileReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    val goals = repository.getAllTimeGoalsList()
                    val monthColorsList = repository.getAllMonthColorsList()
                    val monthColors = monthColorsList.associate { (it.goalId to it.yearMonth) to it.colorHex }
                    val goalsForMonth = goals.withMonthColors(yearMonth, monthColors)
                    val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-", Locale.US))
                    val records = repository.getRecordsListForMonth(prefix)
                    TimeGoalsPdfExporter.generatePdf(
                        context = getApplication(),
                        yearMonth = yearMonth,
                        goals = goalsForMonth,
                        records = records,
                        options = options
                    )
                }
                onFileReady(pdfFile)
            } catch (e: Exception) {
                showToast("Could not generate report: ${e.localizedMessage}")
            }
        }
    }

    fun exportMultiMonthTimeGoalsPdf(
        startMonth: YearMonth,
        endMonth: YearMonth,
        options: TimeGoalsPdfOptions = TimeGoalsPdfOptions(),
        onFileReady: (File) -> Unit
    ) {
        if (startMonth.isAfter(endMonth)) {
            showToast("Start month must precede end month.")
            return
        }

        val monthsList = mutableListOf<YearMonth>()
        var curr = startMonth
        while (!curr.isAfter(endMonth)) {
            monthsList.add(curr)
            curr = curr.plusMonths(1)
        }

        if (monthsList.size > 12) {
            showToast("Maximum selectable range is 12 months.")
            return
        }

        viewModelScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    val goals = repository.getAllTimeGoalsList()
                    val monthColorsList = repository.getAllMonthColorsList()
                    val monthColors = monthColorsList.associate { (it.goalId to it.yearMonth) to it.colorHex }

                    val monthsData = monthsList.map { ym ->
                        val goalsForMonth = goals.withMonthColors(ym, monthColors)
                        val prefix = ym.format(DateTimeFormatter.ofPattern("yyyy-MM-", Locale.US))
                        val records = repository.getRecordsListForMonth(prefix)
                        MonthReportData(
                            yearMonth = ym,
                            goals = goalsForMonth,
                            records = records
                        )
                    }

                    TimeGoalsPdfExporter.generateMultiMonthPdf(
                        context = getApplication(),
                        monthsData = monthsData,
                        options = options
                    )
                }
                onFileReady(pdfFile)
            } catch (e: Exception) {
                showToast("Could not generate multi-month report: ${e.localizedMessage}")
            }
        }
    }

    fun refreshDateBoundary() {
        val now = System.currentTimeMillis()
        if (!TaskOccurrenceResolver.isSameDay(_dateTick.value, now)) {
            _dateTick.value = now
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(dateChangeReceiver)
        } catch (_: Exception) {}
    }
}

class NudgeViewModelFactory(
    private val application: Application,
    private val repository: NudgeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NudgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NudgeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
