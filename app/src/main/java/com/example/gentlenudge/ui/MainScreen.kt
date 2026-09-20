package com.example.gentlenudge.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gentlenudge.backup.BackupValidationResult
import com.example.gentlenudge.backup.NudgeBackupManager
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.gentlenudge.backup.NudgeBackupScheduler
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.export.HistoryPdfExporter
import com.example.gentlenudge.speech.SpeechRecognitionHelper
import com.example.gentlenudge.ui.components.ComposerSheet
import com.example.gentlenudge.ui.components.GentleNudgeLogo
import com.example.gentlenudge.ui.components.NudgeCarvedBottomBar
import com.example.gentlenudge.ui.components.ThemeToggleSwitch
import com.example.gentlenudge.ui.screens.AllTasksScreen
import com.example.gentlenudge.ui.screens.LifeInHoursScreen
import com.example.gentlenudge.ui.screens.RecentlyDeletedScreen
import com.example.gentlenudge.ui.screens.SettingsScreen
import com.example.gentlenudge.ui.screens.TodayScreen
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import com.example.gentlenudge.ui.theme.OnNudgeBlueContainer
import com.example.gentlenudge.ui.theme.ThemeMode
import com.example.gentlenudge.ui.theme.ToastBackground
import com.example.gentlenudge.ui.viewmodel.NudgeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults

enum class NudgeView(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Home),
    NOTES("Your Notes", Icons.Outlined.Inbox),
    HOURS("Hours", Icons.Outlined.HourglassEmpty),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NudgeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val laterTasks by viewModel.laterTasks.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val nudgeAgainEnabled by viewModel.nudgeAgainEnabled.collectAsStateWithLifecycle()
    val defaultSnooze by viewModel.defaultSnooze.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val allTimeGoals by viewModel.allTimeGoals.collectAsStateWithLifecycle()
    val allTimeGoalRecords by viewModel.allTimeGoalRecords.collectAsStateWithLifecycle()
    val monthColorsMap by viewModel.monthColorsMap.collectAsStateWithLifecycle()
    val deletedTasks by viewModel.deletedTasks.collectAsStateWithLifecycle()
    val deletedTasksCount by viewModel.deletedTasksCount.collectAsStateWithLifecycle()
    val currentDateMillis by viewModel.currentDateMillis.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDateBoundary()
                viewModel.checkAutoBackupCatchUp()
                viewModel.loadAutoBackupFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Backup & Data Safety state
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isResolvedDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val lastManualBackupTimestamp by viewModel.lastManualBackupTimestamp.collectAsStateWithLifecycle()
    val lastAutoBackupTimestamp by viewModel.lastAutoBackupTimestamp.collectAsStateWithLifecycle()
    val autoBackupFiles by viewModel.autoBackupFiles.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val autoBackupDay by viewModel.autoBackupDay.collectAsStateWithLifecycle()
    val autoBackupHour by viewModel.autoBackupHour.collectAsStateWithLifecycle()
    val autoBackupMinute by viewModel.autoBackupMinute.collectAsStateWithLifecycle()

    var activeView by remember { mutableStateOf(NudgeView.TODAY) }
    var showRecentlyDeleted by remember { mutableStateOf(false) }
    var composerOpen by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<NudgeTask?>(null) }
    var voiceDraftTitle by remember { mutableStateOf("") }
    var voiceDraftDate by remember { mutableStateOf("Today") }
    var voiceDraftTime by remember { mutableStateOf<String?>(null) }
    var voiceDraftCategory by remember { mutableStateOf<String?>(null) }
    var voiceDraftPriority by remember { mutableStateOf<String?>(null) }
    var voiceDraftRepeat by remember { mutableStateOf<String?>(null) }
    var voiceOriginalText by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }

    // Fallback Intent-based Speech Recognizer
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                val nlp = viewModel.parseNaturalLanguage(spoken)
                editingTask = null
                voiceOriginalText = spoken
                voiceDraftTitle = spoken
                voiceDraftDate = nlp.extractedDate
                voiceDraftTime = if (nlp.hasExplicitDateTime) nlp.extractedTime else null
                voiceDraftCategory = nlp.extractedCategory
                voiceDraftPriority = nlp.extractedPriority
                voiceDraftRepeat = nlp.extractedRepeat
                composerOpen = true
                viewModel.showToast("Heard: \"$spoken\"")
            }
        }
    }

    // Direct SpeechRecognitionHelper
    val speechHelper = remember {
        SpeechRecognitionHelper(
            context = context,
            onTextRecognized = { spoken, isFinal ->
                if (isFinal) {
                    isListening = false
                    val nlp = viewModel.parseNaturalLanguage(spoken)
                    editingTask = null
                    voiceOriginalText = spoken
                    voiceDraftTitle = spoken
                    voiceDraftDate = nlp.extractedDate
                    voiceDraftTime = if (nlp.hasExplicitDateTime) nlp.extractedTime else null
                    voiceDraftCategory = nlp.extractedCategory
                    voiceDraftPriority = nlp.extractedPriority
                    voiceDraftRepeat = nlp.extractedRepeat
                    composerOpen = true
                    viewModel.showToast("Heard: \"$spoken\"")
                }
            },
            onListeningStateChanged = { listening ->
                isListening = listening
            },
            onErrorOccurred = { errorMsg ->
                isListening = false
                viewModel.showToast(errorMsg)
            }
        )
    }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    // Permission launcher for RECORD_AUDIO
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = speechHelper.startListening()
            if (!started) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "What would you like to remember?")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        speechIntentLauncher.launch(intent)
                    } else {
                        viewModel.showToast("Voice recognition is not available on this device. You can type your thought directly.")
                    }
                } catch (e: Exception) {
                    viewModel.showToast("Voice input is not available. You can type directly.")
                }
            }
        } else {
            viewModel.showToast("Microphone permission needed to speak your nudge. You can grant it in Settings.")
        }
    }

    fun startVoiceInput() {
        if (isListening) {
            speechHelper.stopListening()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val started = speechHelper.startListening()
            if (!started) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "What would you like to remember?")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        speechIntentLauncher.launch(intent)
                    } else {
                        viewModel.showToast("Voice recognition not available on this device. You can type directly.")
                    }
                } catch (e: Exception) {
                    viewModel.showToast("Voice input not available. You can always type it in.")
                }
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Permission launcher for POST_NOTIFICATIONS on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.showToast("Notification permission is needed for reminders.")
        }
    }

    val ensureNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Backup & Restore Launchers & Dialog State
    var pendingRestoreValidation by remember { mutableStateOf<BackupValidationResult?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.createBackup(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.validateBackup(uri) { validation ->
                if (validation != null && validation.isValid) {
                    pendingRestoreValidation = validation
                    showRestoreDialog = true
                }
            }
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3500)
            viewModel.clearToast()
        }
    }

    val headerDateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    val currentDateText = headerDateFormat.format(Date())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GentleNudgeLogo(
                            size = 28.dp,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "nudge",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                                fontSize = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Light/Dark Theme Switch immediately to the left of Bloom identity badge
                    ThemeToggleSwitch(
                        isDark = isResolvedDark,
                        onToggle = {
                            val nextMode = if (isResolvedDark) ThemeMode.LIGHT else ThemeMode.DARK
                            viewModel.setThemeMode(nextMode)
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Identity badge showing user's avatar & name (tappable to edit)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showEditNameDialog = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("user_name_badge")
                    ) {
                        // Small circular Nudge logo/avatar on the LEFT
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🪴",
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        // User's currently saved badge name immediately to the RIGHT of the logo
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NudgeCarvedBottomBar(
                activeView = activeView,
                onViewSelected = {
                    activeView = it
                    showRecentlyDeleted = false
                }
            )
        },
        floatingActionButton = {
            if (activeView != NudgeView.SETTINGS && activeView != NudgeView.HOURS) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        voiceDraftTitle = ""
                        voiceDraftDate = "Today"
                        composerOpen = true
                    },
                    containerColor = NudgeBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(56.dp)
                        .testTag("fab_add_task")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New little nudge",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeView) {
                    NudgeView.TODAY -> TodayScreen(
                        todayTasks = todayTasks,
                        laterTasks = laterTasks,
                        allTasks = allTasks,
                        allTimeGoals = allTimeGoals,
                        allTimeGoalRecords = allTimeGoalRecords,
                        monthColorsMap = monthColorsMap,
                        currentDateMillis = currentDateMillis,
                        onToggleDone = { viewModel.toggleDone(it) },
                        onSnooze = { task, label -> viewModel.snoozeTask(task, label) },
                        onDelete = { viewModel.deleteTask(it) },
                        onEdit = { task ->
                            editingTask = task
                            composerOpen = true
                        },
                        onOpenComposer = { draft, date ->
                            val nlp = if (draft.isNotBlank()) viewModel.parseNaturalLanguage(draft) else null
                            editingTask = null
                            voiceDraftTitle = nlp?.cleanTitle ?: draft
                            voiceDraftDate = if (nlp?.hasExplicitDateTime == true) nlp.extractedDate else (date ?: "Today")
                            voiceDraftTime = if (nlp?.hasExplicitDateTime == true) nlp.extractedTime else null
                            voiceDraftCategory = if (nlp?.hasExplicitDateTime == true) nlp.extractedCategory else null
                            voiceDraftPriority = if (nlp?.hasExplicitDateTime == true) nlp.extractedPriority else null
                            voiceDraftRepeat = if (nlp?.hasExplicitDateTime == true) nlp.extractedRepeat else null
                            composerOpen = true
                        },
                        onQuickAdd = { input, dateLabel ->
                            val nlp = viewModel.parseNaturalLanguage(input)
                            val finalDate = if (nlp.hasExplicitDateTime) nlp.extractedDate else (dateLabel ?: "Today")
                            val finalTime = if (nlp.hasExplicitDateTime) nlp.extractedTime else "Evening"
                            viewModel.addTask(
                                title = nlp.cleanTitle,
                                dateLabel = finalDate,
                                timeLabel = finalTime,
                                category = nlp.extractedCategory,
                                priority = nlp.extractedPriority,
                                repeat = nlp.extractedRepeat,
                                attachments = emptyList()
                            )
                            if (nlp.hasExplicitDateTime) {
                                viewModel.showToast("Set reminder: \"${nlp.cleanTitle}\" for $finalDate at $finalTime")
                            }
                        },
                        onStartVoice = { startVoiceInput() },
                        isListening = isListening
                    )
                    NudgeView.NOTES -> AllTasksScreen(
                        tasks = filteredTasks,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onToggleDone = { viewModel.toggleDone(it) },
                        onSnooze = { task, label -> viewModel.snoozeTask(task, label) },
                        onDelete = { viewModel.deleteTask(it) },
                        onEdit = { task ->
                            editingTask = task
                            composerOpen = true
                        },
                        onOpenComposer = {
                            editingTask = null
                            voiceDraftTitle = ""
                            voiceDraftDate = "Today"
                            voiceDraftTime = null
                            voiceDraftCategory = null
                            voiceDraftPriority = null
                            voiceDraftRepeat = null
                            composerOpen = true
                        },
                        onDeleteMultiple = { tasks ->
                            viewModel.permanentlyDeleteTasks(tasks)
                        }
                    )
                    NudgeView.HOURS -> LifeInHoursScreen(
                        viewModel = viewModel
                    )
                    NudgeView.SETTINGS -> {
                        if (showRecentlyDeleted) {
                            BackHandler { showRecentlyDeleted = false }
                            RecentlyDeletedScreen(
                                deletedTasks = deletedTasks,
                                onRestore = { viewModel.restoreTask(it) },
                                onPermanentDelete = { viewModel.permanentlyDeleteTask(it) },
                                onEmptyAll = { viewModel.emptyRecentlyDeleted() },
                                onNavigateBack = { showRecentlyDeleted = false }
                            )
                        } else {
                            SettingsScreen(
                                notificationsEnabled = notificationsEnabled,
                                onToggleNotifications = {
                                    ensureNotificationPermission()
                                    viewModel.toggleNotifications()
                                },
                                nudgeAgainEnabled = nudgeAgainEnabled,
                                onToggleNudgeAgain = { viewModel.toggleNudgeAgain() },
                                defaultSnooze = defaultSnooze,
                                onSelectSnooze = { viewModel.setDefaultSnooze(it) },
                                deletedTasksCount = deletedTasksCount,
                                onOpenRecentlyDeleted = { showRecentlyDeleted = true },
                                onExportPdf = {
                                    viewModel.showToast("Preparing your offline PDF history…")
                                    viewModel.exportHistoryPdf { file ->
                                        HistoryPdfExporter.sharePdf(context, file)
                                    }
                                },
                                onCreateBackup = {
                                    val defaultName = NudgeBackupManager.generateDefaultBackupFileName()
                                    createBackupLauncher.launch(defaultName)
                                },
                                onRestoreBackup = {
                                    restoreBackupLauncher.launch(arrayOf("*/*"))
                                },
                                lastManualBackupTimestamp = lastManualBackupTimestamp,
                                lastAutoBackupTimestamp = lastAutoBackupTimestamp,
                                autoBackupFiles = autoBackupFiles,
                                onShareAutoBackup = { file ->
                                    com.example.gentlenudge.backup.NudgeBackupManager.shareAutoBackupFile(context, file)
                                },
                                autoBackupEnabled = autoBackupEnabled,
                                onToggleAutoBackup = { viewModel.toggleAutoBackup() },
                                autoBackupDay = autoBackupDay,
                                autoBackupHour = autoBackupHour,
                                autoBackupMinute = autoBackupMinute,
                                onSetAutoBackupSchedule = { day, hour, minute ->
                                    viewModel.setAutoBackupSchedule(day, hour, minute)
                                },
                                onResetSampleData = { viewModel.resetSampleData() },
                                onShowToast = { viewModel.showToast(it) }
                            )
                        }
                    }
                }
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreDialog && pendingRestoreValidation != null) {
        val validation = pendingRestoreValidation!!

        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                pendingRestoreValidation = null
            },
            title = {
                Text(
                    text = "Restore Local Backup?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Source: Local Backup File",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NudgeBlue
                        )
                    )

                    Text(
                        text = "This backup contains ${validation.tasks.size} notes/reminders, ${validation.timeGoals.size} pursuits, and ${validation.attachmentEntries.size} attachments.\n\nRestoring will safely replace your current local notes and pursuits with the backup contents. Your local database schema remains completely intact.\n\nDo you want to proceed?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(validation)
                        showRestoreDialog = false
                        pendingRestoreValidation = null
                    }
                ) {
                    Text(
                        text = "Restore Data",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        pendingRestoreValidation = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var inputName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Your Name",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter the name you'd like to display on your badge:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        singleLine = true,
                        placeholder = { Text("Bloom") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NudgeBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            cursorColor = NudgeBlue
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = inputName.trim()
                        val finalName = if (trimmed.isBlank()) "Bloom" else trimmed
                        viewModel.updateUserName(finalName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("btn_save_name")
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        color = NudgeBlue
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    modifier = Modifier.testTag("btn_cancel_name")
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Modal Composer Sheet
    if (composerOpen) {
        ComposerSheet(
            existingTask = editingTask,
            initialTitle = voiceDraftTitle,
            initialDate = voiceDraftDate,
            initialTime = voiceDraftTime,
            initialCategory = voiceDraftCategory,
            initialPriority = voiceDraftPriority,
            initialRepeat = voiceDraftRepeat,
            initialOriginalVoiceText = voiceOriginalText,
            onDismiss = {
                composerOpen = false
                editingTask = null
                voiceDraftTitle = ""
                voiceDraftDate = "Today"
                voiceDraftTime = null
                voiceDraftCategory = null
                voiceDraftPriority = null
                voiceDraftRepeat = null
                voiceOriginalText = null
            },
            onSave = { title, date, time, category, priority, repeat, soundType, ringtoneUri, ringtoneTitle, attachments ->
                ensureNotificationPermission()
                val currentEditing = editingTask
                if (currentEditing != null) {
                    val isRepeatingOption = repeat.isNotBlank() && !repeat.equals("Does not repeat", ignoreCase = true) && !repeat.equals("Once", ignoreCase = true)
                    val newIsDone = if (currentEditing.isDone && isRepeatingOption) false else currentEditing.isDone
                    val newCompletedAt = if (!newIsDone) null else currentEditing.completedAt
                    viewModel.updateTask(
                        currentEditing.copy(
                            title = title,
                            dateLabel = date,
                            timeLabel = time,
                            category = category,
                            priority = priority,
                            repeat = repeat,
                            soundType = soundType,
                            ringtoneUri = ringtoneUri,
                            ringtoneTitle = ringtoneTitle,
                            attachmentsJson = TaskAttachment.listToJson(attachments),
                            isDone = newIsDone,
                            completedAt = newCompletedAt
                        )
                    )
                } else {
                    viewModel.addTask(
                        title = title,
                        dateLabel = date,
                        timeLabel = time,
                        category = category,
                        priority = priority,
                        repeat = repeat,
                        soundType = soundType,
                        ringtoneUri = ringtoneUri,
                        ringtoneTitle = ringtoneTitle,
                        attachments = attachments
                    )
                }
                composerOpen = false
                editingTask = null
                voiceDraftTitle = ""
                voiceDraftDate = "Today"
                voiceDraftTime = null
                voiceDraftCategory = null
                voiceDraftPriority = null
                voiceDraftRepeat = null
            },
            onStartVoice = { startVoiceInput() }
        )
    }
}
