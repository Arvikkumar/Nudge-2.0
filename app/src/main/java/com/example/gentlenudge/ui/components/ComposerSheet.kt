package com.example.gentlenudge.ui.components

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.gentlenudge.attachment.AttachmentManager
import com.example.gentlenudge.attachment.AudioMemoManager
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.nlp.NudgeNlpParser
import com.example.gentlenudge.nlp.ParsedNudge
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.speech.SpeechRecognitionHelper
import com.example.gentlenudge.speech.VoiceInputState
import com.example.gentlenudge.ui.theme.ImportantDot
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer
import com.example.gentlenudge.ui.theme.OnNudgeBlueContainer
import com.example.gentlenudge.ui.theme.SoftPeach
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun calculateTimeRemainingText(
    dateLabel: String,
    timeLabel: String,
    repeatRule: String? = null,
    task: NudgeTask? = null,
    context: Context? = null
): String? {
    if (timeLabel.isBlank() || timeLabel.equals("Any time", ignoreCase = true)) return null

    val now = System.currentTimeMillis()
    val targetMillis = if (task != null &&
        task.dateLabel.equals(dateLabel, ignoreCase = true) &&
        task.timeLabel.equals(timeLabel, ignoreCase = true)
    ) {
        TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, now)
    } else {
        val baseRef = if (task != null && task.createdAt > 0L) task.createdAt else now
        NudgeAlarmScheduler.calculateNextOccurrenceMillis(dateLabel, timeLabel, repeatRule, now, baseRef = baseRef)
    }

    val diffMillis = targetMillis - now
    if (diffMillis <= 0) return null

    val diffSeconds = diffMillis / 1000
    val totalMinutes = (diffSeconds + 59) / 60
    if (totalMinutes <= 0) return "in less than a minute"

    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    val parts = mutableListOf<String>()
    if (days > 0) {
        parts.add("$days ${if (days == 1L) "day" else "days"}")
    }
    if (hours > 0) {
        parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
    }
    if (minutes > 0) {
        parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
    }

    return if (parts.isNotEmpty()) "in ${parts.joinToString(" ")}" else "in less than a minute"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposerSheet(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        date: String,
        time: String,
        category: String,
        priority: String,
        repeat: String,
        soundType: String,
        ringtoneUri: String?,
        ringtoneTitle: String?,
        attachments: List<TaskAttachment>
    ) -> Unit,
    onStartVoice: () -> Unit,
    existingTask: NudgeTask? = null,
    initialTitle: String = "",
    initialDate: String = "Today",
    initialTime: String? = null,
    initialCategory: String? = null,
    initialPriority: String? = null,
    initialRepeat: String? = null,
    initialAttachments: List<TaskAttachment> = emptyList(),
    initialOriginalVoiceText: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    fun getTimeAfterMinutes(minutes: Int): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutes)
        }
        return timeFormatter.format(cal.time)
    }

    val standardDateOptions = listOf("Today", "Tonight", "Tomorrow")
    val standardTimeOptions = listOf("30 minutes later", "1 hour later")
    val standardRepeatOptions = listOf("Does not repeat", "Every day")
    val soundOptions = listOf("Small nudge", "Full ringtone")

    var title by remember { mutableStateOf(existingTask?.title ?: initialTitle) }
    var selectedDate by remember { mutableStateOf(existingTask?.dateLabel ?: initialDate) }
    var selectedTimeOption by remember {
        mutableStateOf(
            if (existingTask != null && existingTask.timeLabel !in standardTimeOptions) "custom"
            else if (initialTime != null) "custom"
            else "30 minutes later"
        )
    }
    var selectedTime by remember {
        mutableStateOf(
            existingTask?.timeLabel ?: initialTime ?: getTimeAfterMinutes(30)
        )
    }
    var customDateText by remember {
        mutableStateOf<String?>(
            if (existingTask != null && existingTask.dateLabel !in standardDateOptions) existingTask.dateLabel
            else if (initialDate.isNotBlank() && initialDate !in standardDateOptions) initialDate
            else null
        )
    }
    var customTimeText by remember {
        mutableStateOf<String?>(
            if (existingTask != null && existingTask.timeLabel !in standardTimeOptions) existingTask.timeLabel
            else initialTime
        )
    }
    var selectedCategory by remember { mutableStateOf(existingTask?.category ?: initialCategory ?: "Personal") }
    var isImportant by remember { mutableStateOf(existingTask?.priority == "Important" || initialPriority == "Important") }
    var selectedRepeat by remember { mutableStateOf(existingTask?.repeat ?: initialRepeat ?: "Does not repeat") }
    var customRepeatText by remember {
        mutableStateOf<String?>(
            if (existingTask != null && existingTask.repeat !in listOf("Does not repeat", "Every day")) existingTask.repeat
            else if (initialRepeat != null && initialRepeat !in listOf("Does not repeat", "Every day")) initialRepeat
            else null
        )
    }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }
    var selectedSoundType by remember { mutableStateOf(existingTask?.soundType ?: "Small nudge") }
    var selectedRingtoneUri by remember { mutableStateOf(existingTask?.ringtoneUri) }
    var selectedRingtoneTitle by remember { mutableStateOf(existingTask?.ringtoneTitle) }
    var attachments by remember { mutableStateOf(existingTask?.attachments ?: initialAttachments) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var previewImageAttachment by remember { mutableStateOf<TaskAttachment?>(null) }

    // Speech Recognition & Voice UX State
    var lastRecognizedVoiceText by remember { mutableStateOf(initialOriginalVoiceText) }
    var parsedNudgeInfo by remember {
        mutableStateOf(
            if (initialOriginalVoiceText != null) NudgeNlpParser.parse(initialOriginalVoiceText)
            else if (initialTitle.isNotBlank()) NudgeNlpParser.parse(initialTitle)
            else null
        )
    }
    var voiceState by remember { mutableStateOf(VoiceInputState.IDLE) }
    var voiceRmsLevel by remember { mutableFloatStateOf(0f) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }

    var isInitialComposition by remember { mutableStateOf(true) }
    var lastKnownTitle by remember { mutableStateOf(title) }

    // Dynamic interpretation update when user types or edits transcription manually
    LaunchedEffect(title) {
        if (isInitialComposition) {
            isInitialComposition = false
            return@LaunchedEffect
        }
        if (title != lastKnownTitle) {
            lastKnownTitle = title
            if (title.isNotBlank()) {
                val parsed = NudgeNlpParser.parse(title)
                parsedNudgeInfo = parsed
                if (parsed.hasExplicitDateTime) {
                    selectedDate = parsed.extractedDate
                    if (parsed.extractedDate !in standardDateOptions) {
                        customDateText = parsed.extractedDate
                    } else {
                        customDateText = null
                    }
                    selectedTime = parsed.extractedTime
                    customTimeText = parsed.extractedTime
                    selectedTimeOption = "custom"
                }
            }
        }
    }

    val infiniteMicTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale1 by infiniteMicTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_1"
    )
    val pulseAlpha1 by infiniteMicTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_1"
    )
    val micPulseScale by infiniteMicTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    // Speech Recognition Helper for Composer Input
    val speechHelper = remember {
        SpeechRecognitionHelper(
            context = context,
            onTextRecognized = { recognizedText, isFinal ->
                if (recognizedText.isNotBlank()) {
                    if (isFinal) {
                        voiceState = VoiceInputState.IDLE
                        val parsed = NudgeNlpParser.parse(recognizedText)
                        title = recognizedText
                        lastRecognizedVoiceText = recognizedText
                        parsedNudgeInfo = parsed
                        validationError = null
                        voiceErrorMessage = null
                        if (parsed.hasExplicitDateTime) {
                            selectedDate = parsed.extractedDate
                            if (parsed.extractedDate !in standardDateOptions) {
                                customDateText = parsed.extractedDate
                            } else {
                                customDateText = null
                            }
                            selectedTime = parsed.extractedTime
                            customTimeText = parsed.extractedTime
                            selectedTimeOption = "custom"
                        }
                        selectedCategory = parsed.extractedCategory
                        if (parsed.extractedPriority == "Important") {
                            isImportant = true
                        }
                        if (parsed.extractedRepeat != "Does not repeat") {
                            selectedRepeat = parsed.extractedRepeat
                        }
                    } else {
                        title = recognizedText
                        validationError = null
                    }
                }
            },
            onStateChanged = { newState ->
                voiceState = newState
            },
            onRmsChangedState = { rms ->
                voiceRmsLevel = rms.coerceIn(0f, 10f)
            },
            onErrorOccurred = { errorMsg ->
                voiceState = VoiceInputState.IDLE
                voiceErrorMessage = errorMsg
            }
        )
    }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    // Fallback Intent Launcher for Speech
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenMatches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val parsed = NudgeNlpParser.parse(spoken)
                title = spoken
                lastRecognizedVoiceText = spoken
                parsedNudgeInfo = parsed
                validationError = null
                voiceErrorMessage = null
                if (parsed.hasExplicitDateTime) {
                    selectedDate = parsed.extractedDate
                    if (parsed.extractedDate !in standardDateOptions) {
                        customDateText = parsed.extractedDate
                    } else {
                        customDateText = null
                    }
                    selectedTime = parsed.extractedTime
                    customTimeText = parsed.extractedTime
                    selectedTimeOption = "custom"
                }
                selectedCategory = parsed.extractedCategory
                if (parsed.extractedPriority == "Important") {
                    isImportant = true
                }
                if (parsed.extractedRepeat != "Does not repeat") {
                    selectedRepeat = parsed.extractedRepeat
                }
            }
        }
        voiceState = VoiceInputState.IDLE
    }

    // Permission launcher for Speech Recognition
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceErrorMessage = null
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
                        voiceErrorMessage = "Speech recognition is not available on this device."
                    }
                } catch (e: Exception) {
                    voiceErrorMessage = "Speech recognition is not available."
                }
            }
        } else {
            voiceState = VoiceInputState.IDLE
            voiceErrorMessage = "Microphone permission is needed for voice input."
            Toast.makeText(context, "Microphone permission is needed for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    // Natural Language Detection State
    val nlpParsed = remember(title) {
        if (title.length > 5) NudgeNlpParser.parse(title) else null
    }

    // Audio Memo & Attachment Management
    val audioMemoManager = remember { AudioMemoManager(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var playingAttachmentId by remember { mutableStateOf<String?>(null) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    // Recording duration timer
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDurationSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingDurationSeconds++
                if (recordingDurationSeconds >= 300) { // 5 min auto stop
                    val memo = audioMemoManager.stopRecording()
                    if (memo != null) {
                        attachments = attachments + memo
                    }
                    isRecordingAudio = false
                }
            }
        }
    }

    DisposableEffect(audioMemoManager) {
        onDispose {
            audioMemoManager.cleanup()
        }
    }

    // Photo Gallery Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val att = AttachmentManager.saveAttachmentFromUri(context, uri, TaskAttachment.TYPE_IMAGE)
            if (att != null) {
                attachments = attachments + att
            }
        }
    }

    // Document/File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val att = AttachmentManager.saveAttachmentFromUri(context, uri, TaskAttachment.TYPE_FILE)
            if (att != null) {
                attachments = attachments + att
            }
        }
    }

    // Record Audio Permission Launcher for Voice Memo
    val audioRecordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = audioMemoManager.startRecording()
            if (started) {
                isRecordingAudio = true
            } else {
                Toast.makeText(context, "Unable to start voice recording.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission needed to record voice note.", Toast.LENGTH_SHORT).show()
        }
    }

    // Ringtone Picker
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val titleFound = try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: "Custom Ringtone"
                } catch (_: Exception) {
                    "Custom Ringtone"
                }
                selectedRingtoneUri = uri.toString()
                selectedRingtoneTitle = titleFound
            }
        }
    }

    fun openRingtonePicker() {
        val currentUri = selectedRingtoneUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Reminder Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }
        try {
            ringtonePickerLauncher.launch(intent)
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    LaunchedEffect(existingTask) {
        if (existingTask != null) {
            title = existingTask.title
            selectedDate = existingTask.dateLabel
            selectedTime = existingTask.timeLabel
            selectedCategory = existingTask.category
            isImportant = existingTask.priority == "Important"
            selectedRepeat = existingTask.repeat
            selectedSoundType = existingTask.soundType
            selectedRingtoneUri = existingTask.ringtoneUri
            selectedRingtoneTitle = existingTask.ringtoneTitle
            attachments = existingTask.attachments
            if (existingTask.dateLabel !in standardDateOptions) {
                customDateText = existingTask.dateLabel
            }
            if (existingTask.repeat !in listOf("Does not repeat", "Every day")) {
                customRepeatText = existingTask.repeat
            }
            if (existingTask.timeLabel !in standardTimeOptions) {
                customTimeText = existingTask.timeLabel
                selectedTimeOption = "custom"
            }
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        if (customDateText != null) {
            val dateFormats = listOf(
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
                SimpleDateFormat("MMM d", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("MMM d, yyyy", Locale.US),
                SimpleDateFormat("MMM d", Locale.US)
            )
            for (fmt in dateFormats) {
                try {
                    val parsed = fmt.parse(customDateText!!)
                    if (parsed != null) {
                        val tempCal = Calendar.getInstance().apply { time = parsed }
                        cal.set(Calendar.MONTH, tempCal.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH))
                        if (customDateText!!.contains(Regex("\\b20\\d{2}\\b"))) {
                            cal.set(Calendar.YEAR, tempCal.get(Calendar.YEAR))
                        }
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val formatted = if (year == currentYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(pickedCal.time)
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(pickedCal.time)
                }
                customDateText = formatted
                selectedDate = formatted
                validationError = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        val minCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        datePickerDialog.datePicker.minDate = minCal.timeInMillis
        datePickerDialog.show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance()
        if (customTimeText != null) {
            val timeFormats = listOf(
                SimpleDateFormat("h:mm a", Locale.getDefault()),
                SimpleDateFormat("HH:mm", Locale.getDefault()),
                SimpleDateFormat("h:mm a", Locale.US),
                SimpleDateFormat("HH:mm", Locale.US)
            )
            for (fmt in timeFormats) {
                try {
                    val parsed = fmt.parse(customTimeText!!)
                    if (parsed != null) {
                        val tempCal = Calendar.getInstance().apply { time = parsed }
                        cal.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE))
                        break
                    }
                } catch (_: Exception) {}
            }
        } else {
            cal.add(Calendar.MINUTE, 30)
        }

        val is24Hour = DateFormat.is24HourFormat(context)
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val formatted = if (is24Hour) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(pickedCal.time)
                } else {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(pickedCal.time)
                }
                customTimeText = formatted
                selectedTime = formatted
                selectedTimeOption = "custom"
                validationError = null
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            is24Hour
        )
        timePickerDialog.show()
    }

    // Recording pulsing transition
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    if (showCustomRepeatDialog) {
        CustomRepeatDialog(
            initialRepeat = customRepeatText ?: (if (selectedRepeat !in standardRepeatOptions) selectedRepeat else null),
            onDismissRequest = { showCustomRepeatDialog = false },
            onConfirm = { chosenRepeat ->
                customRepeatText = chosenRepeat
                selectedRepeat = chosenRepeat
                showCustomRepeatDialog = false
                validationError = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (existingTask != null) "EDIT LITTLE NUDGE" else "NEW LITTLE NUDGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NudgeBlue
                    )
                    Text(
                        text = if (existingTask != null) "Refine this thought." else "Hold this thought.",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close composer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Error Message Banner (if any)
            if (voiceErrorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Voice error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = voiceErrorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { voiceErrorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Recognized Speech Preview & Verification Banner
            if (!lastRecognizedVoiceText.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NudgeBlueContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, NudgeBlue.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("composer_voice_interpretation_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header: 🎙 Heard by Nudge + Dismiss 'X'
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Heard voice",
                                    tint = NudgeBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Heard by Nudge",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp
                                    ),
                                    color = OnNudgeBlueContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    lastRecognizedVoiceText = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss preview",
                                    tint = OnNudgeBlueContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Original spoken transcription quotation
                        Text(
                            text = "\"${lastRecognizedVoiceText}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        )

                        // Extracted info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Extracted:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "$selectedDate at $selectedTime",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = NudgeBlue
                                )
                            )
                        }

                        // Interpreted as badge/pill with clear clock icon and distinct visual hierarchy
                        val parsedInfo = parsedNudgeInfo
                        val isAmbiguous = parsedInfo != null && parsedInfo.isAmbiguousAmPm && parsedInfo.alternativeTime != null
                        val altTarget = if (parsedInfo != null && isAmbiguous) {
                            if (selectedTime == parsedInfo.extractedTime) parsedInfo.alternativeTime else parsedInfo.extractedTime
                        } else null
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, NudgeBlue.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isAmbiguous && altTarget != null) {
                                        Modifier.clickable {
                                            selectedTime = altTarget
                                            customTimeText = altTarget
                                            selectedTimeOption = "custom"
                                            validationError = null
                                        }
                                    } else Modifier
                                )
                                .testTag("interpreted_as_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Interpretation clock",
                                    tint = NudgeBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isAmbiguous && altTarget != null) {
                                        "Interpreted as $selectedTime  •  Tap to switch to $altTarget"
                                    } else {
                                        "Interpreted as $selectedTime"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Task title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (validationError != null) validationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("composer_title_input"),
                placeholder = {
                    Text(
                        text = "e.g. Call dentist tomorrow at 3pm, Buy groceries tonight…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.titleMedium,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (voiceState == VoiceInputState.LISTENING) {
                            VoiceWaveformIndicator(
                                rmsLevel = voiceRmsLevel,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        } else if (voiceState == VoiceInputState.PROCESSING) {
                            Text(
                                text = "Processing…",
                                style = MaterialTheme.typography.labelSmall,
                                color = NudgeBlue,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (voiceState == VoiceInputState.LISTENING) {
                                // Subtle continuous pulsing ripple
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(pulseScale1)
                                        .alpha(pulseAlpha1)
                                        .clip(CircleShape)
                                        .background(NudgeBlue.copy(alpha = 0.35f))
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (voiceState == VoiceInputState.LISTENING) {
                                        speechHelper.stopListening()
                                    } else {
                                        val hasMic = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) {
                                            voiceErrorMessage = null
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
                                                        voiceErrorMessage = "Speech recognition is not supported on this device."
                                                    }
                                                } catch (e: Exception) {
                                                    voiceErrorMessage = "Speech recognition unavailable."
                                                }
                                            }
                                        } else {
                                            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(if (voiceState == VoiceInputState.LISTENING) micPulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (voiceState == VoiceInputState.LISTENING) NudgeBlue
                                        else Color.Transparent
                                    )
                                    .testTag("composer_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = if (voiceState == VoiceInputState.LISTENING) "Stop listening" else "Voice input",
                                    tint = if (voiceState == VoiceInputState.LISTENING) Color.White else NudgeBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NudgeBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                maxLines = 3
            )

            // Voice Listening State Indicator & Error Notice
            if (voiceState == VoiceInputState.LISTENING) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(micPulseScale)
                            .clip(CircleShape)
                            .background(NudgeBlue)
                    )
                    Text(
                        text = "Listening… speak naturally (e.g. \"Read tonight at 9 PM\")",
                        style = MaterialTheme.typography.bodySmall,
                        color = NudgeBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (voiceState == VoiceInputState.PROCESSING) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = NudgeBlue
                    )
                    Text(
                        text = "Processing speech…",
                        style = MaterialTheme.typography.bodySmall,
                        color = NudgeBlue
                    )
                }
            } else if (voiceErrorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = voiceErrorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Ambiguity Confirmation Card
            if (nlpParsed != null && nlpParsed.isAmbiguousAmPm && nlpParsed.cleanTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isAltSelected = (selectedTime == nlpParsed.alternativeTime)
                val isPrimarySelected = (selectedTime == nlpParsed.extractedTime || !isAltSelected)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nlp_ambiguity_confirmation_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NudgeBlueContainer.copy(alpha = 0.85f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(NudgeBlue.copy(alpha = 0.4f)),
                        width = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = NudgeBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isAltSelected && nlpParsed.alternativeTime != null) {
                                    "Did you mean ${nlpParsed.extractedDate} at ${nlpParsed.alternativeTime}?"
                                } else {
                                    nlpParsed.confirmationPrompt ?: "Did you mean ${nlpParsed.extractedDate} at ${nlpParsed.extractedTime}?"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnNudgeBlueContainer,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Primary extracted time (e.g. 5:30 PM)
                            if (isPrimarySelected) {
                                Button(
                                    onClick = {
                                        selectedDate = nlpParsed.extractedDate
                                        if (nlpParsed.extractedDate !in standardDateOptions) {
                                            customDateText = nlpParsed.extractedDate
                                        }
                                        selectedTime = nlpParsed.extractedTime
                                        customTimeText = nlpParsed.extractedTime
                                        selectedTimeOption = "custom"
                                        selectedCategory = nlpParsed.extractedCategory
                                        isImportant = (nlpParsed.extractedPriority == "Important")
                                        selectedRepeat = nlpParsed.extractedRepeat
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nlp_confirm_yes_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "✓ Yes, ${nlpParsed.extractedTime}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedDate = nlpParsed.extractedDate
                                        if (nlpParsed.extractedDate !in standardDateOptions) {
                                            customDateText = nlpParsed.extractedDate
                                        }
                                        selectedTime = nlpParsed.extractedTime
                                        customTimeText = nlpParsed.extractedTime
                                        selectedTimeOption = "custom"
                                        selectedCategory = nlpParsed.extractedCategory
                                        isImportant = (nlpParsed.extractedPriority == "Important")
                                        selectedRepeat = nlpParsed.extractedRepeat
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nlp_confirm_yes_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "✎ ${nlpParsed.extractedTime}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = NudgeBlue
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Alternate time (e.g. 5:30 AM)
                            if (nlpParsed.alternativeTime != null) {
                                if (isAltSelected) {
                                    Button(
                                        onClick = {
                                            selectedDate = nlpParsed.extractedDate
                                            if (nlpParsed.extractedDate !in standardDateOptions) {
                                                customDateText = nlpParsed.extractedDate
                                            }
                                            selectedTime = nlpParsed.alternativeTime
                                            customTimeText = nlpParsed.alternativeTime
                                            selectedTimeOption = "custom"
                                            selectedCategory = nlpParsed.extractedCategory
                                            isImportant = (nlpParsed.extractedPriority == "Important")
                                            selectedRepeat = nlpParsed.extractedRepeat
                                            validationError = null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("nlp_change_time_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "✓ Yes, ${nlpParsed.alternativeTime}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            selectedDate = nlpParsed.extractedDate
                                            if (nlpParsed.extractedDate !in standardDateOptions) {
                                                customDateText = nlpParsed.extractedDate
                                            }
                                            selectedTime = nlpParsed.alternativeTime
                                            customTimeText = nlpParsed.alternativeTime
                                            selectedTimeOption = "custom"
                                            selectedCategory = nlpParsed.extractedCategory
                                            isImportant = (nlpParsed.extractedPriority == "Important")
                                            selectedRepeat = nlpParsed.extractedRepeat
                                            validationError = null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("nlp_change_time_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "✎ ${nlpParsed.alternativeTime}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = NudgeBlue
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { openTimePicker() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nlp_change_time_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "✎ Change time",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = NudgeBlue
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (nlpParsed != null && nlpParsed.hasExplicitDateTime && nlpParsed.cleanTitle.isNotBlank()) {
                // Dynamic NLP Detected Chip
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NudgeBlueContainer)
                        .clickable {
                            // Apply parsed NLP values smoothly while preserving transcription
                            selectedDate = nlpParsed.extractedDate
                            if (nlpParsed.extractedDate !in standardDateOptions) {
                                customDateText = nlpParsed.extractedDate
                            }
                            selectedTime = nlpParsed.extractedTime
                            customTimeText = nlpParsed.extractedTime
                            selectedTimeOption = "custom"
                            selectedCategory = nlpParsed.extractedCategory
                            isImportant = nlpParsed.extractedPriority == "Important"
                            selectedRepeat = nlpParsed.extractedRepeat
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("nlp_suggestion_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = NudgeBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Interpreted as ${nlpParsed.extractedDate} at ${nlpParsed.extractedTime}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnNudgeBlueContainer,
                                fontSize = 12.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Tap to apply",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NudgeBlue,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // ATTACHMENTS SECTION (Photos, Files, Voice)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Attachments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (attachments.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NudgeBlueContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${attachments.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = NudgeBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attachment Trigger Buttons Row (Photos, Files, Voice Memo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Photos Gallery
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .testTag("composer_attach_photo"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        width = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Photo,
                            contentDescription = "Add Photo",
                            tint = NudgeBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Photos",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. Document / File
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { filePickerLauncher.launch("*/*") }
                        .testTag("composer_attach_file"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        width = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Add File",
                            tint = NudgeBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Files",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 3. Voice Memo Recorder
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (isRecordingAudio) {
                                val memo = audioMemoManager.stopRecording()
                                if (memo != null) attachments = attachments + memo
                                isRecordingAudio = false
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val started = audioMemoManager.startRecording()
                                    if (started) isRecordingAudio = true
                                } else {
                                    audioRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("composer_attach_voice"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecordingAudio) SoftPeach else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(if (isRecordingAudio) ImportantDot else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        width = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Record Voice Memo",
                            tint = if (isRecordingAudio) ImportantDot else NudgeBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isRecordingAudio) "Stop" else "Voice",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isRecordingAudio) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRecordingAudio) ImportantDot else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Live Voice Recording Banner
            AnimatedVisibility(
                visible = isRecordingAudio,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftPeach),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(ImportantDot),
                        width = 1.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(ImportantDot)
                            )
                            val minutes = recordingDurationSeconds / 60
                            val seconds = recordingDurationSeconds % 60
                            Text(
                                text = "Recording: %d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB33600),
                                    fontSize = 13.5.sp
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel button
                            Text(
                                text = "Discard",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier
                                    .clickable {
                                        audioMemoManager.cancelRecording()
                                        isRecordingAudio = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )

                            // Stop & Attach button
                            Button(
                                onClick = {
                                    val memo = audioMemoManager.stopRecording()
                                    if (memo != null) {
                                        attachments = attachments + memo
                                    }
                                    isRecordingAudio = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImportantDot,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Finish and attach",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Attach", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp))
                            }
                        }
                    }
                }
            }

            // Attached Items Display List
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { att ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                width = 0.8.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left side: Icon or Thumbnail + Info
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    when (att.type) {
                                        TaskAttachment.TYPE_IMAGE -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { previewImageAttachment = att }
                                            ) {
                                                AsyncImage(
                                                    model = File(att.filePath),
                                                    contentDescription = att.fileName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        TaskAttachment.TYPE_AUDIO -> {
                                            val isThisPlaying = playingAttachmentId == att.id
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isThisPlaying) NudgeBlue else NudgeBlueContainer)
                                                    .clickable {
                                                        if (isThisPlaying) {
                                                            audioMemoManager.stopPlayback()
                                                            playingAttachmentId = null
                                                            playbackProgress = 0f
                                                        } else {
                                                            playingAttachmentId = att.id
                                                            audioMemoManager.startPlayback(
                                                                audioFile = File(att.filePath),
                                                                onProgress = { curMs, totMs ->
                                                                    playbackProgress = if (totMs > 0) curMs.toFloat() / totMs else 0f
                                                                },
                                                                onCompletion = {
                                                                    playingAttachmentId = null
                                                                    playbackProgress = 0f
                                                                }
                                                            )
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isThisPlaying) "Pause" else "Play",
                                                    tint = if (isThisPlaying) Color.White else NudgeBlue,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Description,
                                                    contentDescription = null,
                                                    tint = NudgeBlue,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (att.type) {
                                                TaskAttachment.TYPE_AUDIO -> "Voice Memo (${att.formattedDuration()})"
                                                else -> att.fileName
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${att.formattedSize()} • ${att.type.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Right side: Delete button
                                IconButton(
                                    onClick = {
                                        if (playingAttachmentId == att.id) {
                                            audioMemoManager.stopPlayback()
                                            playingAttachmentId = null
                                        }
                                        AttachmentManager.deleteAttachmentFile(att.filePath)
                                        attachments = attachments.filter { it.id != att.id }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Image Preview Dialog
            if (previewImageAttachment != null) {
                Dialog(onDismissRequest = { previewImageAttachment = null }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = previewImageAttachment!!.fileName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { previewImageAttachment = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close preview")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = File(previewImageAttachment!!.filePath),
                                contentDescription = previewImageAttachment!!.fileName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // When / Date Section
            Text(
                text = "When?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                standardDateOptions.forEach { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = {
                            selectedDate = date
                            validationError = null
                        },
                        label = { Text(date, style = MaterialTheme.typography.bodyMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeBlueContainer,
                            selectedLabelColor = OnNudgeBlueContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedDate == date,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = NudgeBlue
                        )
                    )
                }

                // Custom date chip
                val isCustomDateSelected = customDateText != null && selectedDate == customDateText
                FilterChip(
                    selected = isCustomDateSelected,
                    onClick = {
                        if (isCustomDateSelected && customDateText != null) {
                            openDatePicker()
                        } else if (customDateText != null) {
                            selectedDate = customDateText!!
                            validationError = null
                        } else {
                            openDatePicker()
                        }
                    },
                    label = {
                        Text(
                            text = customDateText ?: "Custom date",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeBlueContainer,
                        selectedLabelColor = OnNudgeBlueContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isCustomDateSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = NudgeBlue
                    ),
                    modifier = Modifier.testTag("composer_custom_date_chip")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time section
            Text(
                text = "At what time?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                standardTimeOptions.forEach { option ->
                    val isSelected = selectedTimeOption == option
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTimeOption = option
                            if (option == "30 minutes later") {
                                selectedTime = getTimeAfterMinutes(30)
                            } else if (option == "1 hour later") {
                                selectedTime = getTimeAfterMinutes(60)
                            }
                            validationError = null
                        },
                        label = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeBlueContainer,
                            selectedLabelColor = OnNudgeBlueContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = NudgeBlue
                        )
                    )
                }

                // Custom time chip
                val isCustomTimeSelected = selectedTimeOption == "custom"
                FilterChip(
                    selected = isCustomTimeSelected,
                    onClick = {
                        openTimePicker()
                    },
                    label = {
                        Text(
                            text = customTimeText ?: "Custom time",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeBlueContainer,
                        selectedLabelColor = OnNudgeBlueContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isCustomTimeSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = NudgeBlue
                    ),
                    modifier = Modifier.testTag("composer_custom_time_chip")
                )
            }

            // Custom time distance indicator
            if (selectedTimeOption == "custom" && customTimeText != null) {
                val remainingText = calculateTimeRemainingText(
                    dateLabel = selectedDate,
                    timeLabel = customTimeText!!,
                    repeatRule = selectedRepeat,
                    task = existingTask,
                    context = context
                )
                if (!remainingText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = NudgeBlue,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("composer_custom_time_remaining")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat Section
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                standardRepeatOptions.forEach { rep ->
                    FilterChip(
                        selected = selectedRepeat == rep,
                        onClick = {
                            selectedRepeat = rep
                            validationError = null
                        },
                        label = {
                            Text(
                                if (rep == "Does not repeat") "Once" else rep,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeBlueContainer,
                            selectedLabelColor = OnNudgeBlueContainer
                        )
                    )
                }

                // Custom repeat chip
                val isCustomRepeatSelected = selectedRepeat !in standardRepeatOptions
                FilterChip(
                    selected = isCustomRepeatSelected,
                    onClick = {
                        showCustomRepeatDialog = true
                    },
                    label = {
                        Text(
                            text = if (isCustomRepeatSelected && !customRepeatText.isNullOrBlank()) customRepeatText!! else if (isCustomRepeatSelected && selectedRepeat.isNotBlank()) selectedRepeat else "Custom",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeBlueContainer,
                        selectedLabelColor = OnNudgeBlueContainer
                    ),
                    modifier = Modifier.testTag("composer_custom_repeat_chip")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder sound Section
            Text(
                text = "Reminder sound",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                soundOptions.forEach { sound ->
                    val isSelected = selectedSoundType == sound
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSoundType = sound },
                        label = {
                            Text(sound, style = MaterialTheme.typography.bodyMedium)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeBlueContainer,
                            selectedLabelColor = OnNudgeBlueContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = NudgeBlue
                        ),
                        modifier = Modifier.testTag(
                            if (sound == "Small nudge") "composer_sound_small_nudge" else "composer_sound_full_ringtone"
                        )
                    )
                }
            }

            // Custom Ringtone Selection row when Full ringtone is selected
            if (selectedSoundType == "Full ringtone") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { openRingtonePicker() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("composer_choose_ringtone_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = NudgeBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedRingtoneTitle ?: "Default alarm tone",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (selectedRingtoneTitle != null) "Change" else "Choose ringtone",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = NudgeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Important switch pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isImportant = !isImportant }
                    .background(
                        if (isImportant) SoftPeach else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isImportant) ImportantDot else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("composer_priority_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isImportant) ImportantDot else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Text(
                        text = if (isImportant) "Marked as Important" else "Mark as Important",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isImportant) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isImportant) Color(0xFFB33600) else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inline Validation Error message if any
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("composer_validation_error")
                )
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = NudgeBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Saved privately on device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank()) {
                            validationError = "Give this little nudge a few words first."
                            return@Button
                        }
                        val now = System.currentTimeMillis()
                        val triggerMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(selectedDate, selectedTime, selectedRepeat, now)
                        // Validate past time for specific date/time (allow Today/Tonight and repeating tasks to be scheduled for next occurrence)
                        val isPastForToday = (selectedDate.equals("Today", ignoreCase = true) || selectedDate.equals("Tonight", ignoreCase = true))
                        val isRepeating = selectedRepeat.isNotBlank() && !selectedRepeat.equals("Does not repeat", ignoreCase = true) && !selectedRepeat.equals("Once", ignoreCase = true)
                        if (triggerMillis < now - 60000L && !isPastForToday && !isRepeating) {
                            validationError = "Please choose a future time for this nudge."
                            return@Button
                        }
                        validationError = null
                        onSave(
                            trimmedTitle,
                            selectedDate,
                            selectedTime,
                            selectedCategory,
                            if (isImportant) "Important" else "Normal",
                            selectedRepeat,
                            selectedSoundType,
                            if (selectedSoundType == "Full ringtone") selectedRingtoneUri else null,
                            if (selectedSoundType == "Full ringtone") selectedRingtoneTitle else null,
                            attachments
                        )
                    },
                    modifier = Modifier.testTag("composer_save_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NudgeBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (existingTask != null) "Update nudge" else "Save nudge",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceWaveformIndicator(
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .height(24.dp)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 4
        for (i in 0 until bars) {
            val offset = i * 1.3f
            val wave = ((kotlin.math.sin(phase + offset) + 1f) / 2f) // 0..1
            val rmsNorm = (rmsLevel / 10f).coerceIn(0f, 1f)
            val barHeight = 4.dp + (14.dp * (0.35f * wave + 0.65f * rmsNorm))

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NudgeBlue)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomRepeatDialog(
    initialRepeat: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val quickPresets = listOf(
        "Every week",
        "Every 2 weeks",
        "Every month",
        "Every 3 months",
        "Every 6 months",
        "Every year",
        "Weekdays (Mon–Fri)",
        "Weekends (Sat–Sun)"
    )

    var selectedPreset by remember {
        mutableStateOf(
            if (initialRepeat in quickPresets) initialRepeat else null
        )
    }
    var intervalCount by remember { mutableIntStateOf(2) }
    var intervalUnit by remember { mutableStateOf("weeks") }

    val customIntervalString = remember(intervalCount, intervalUnit) {
        val unitLabel = when (intervalUnit) {
            "days" -> if (intervalCount == 1) "day" else "days"
            "weeks" -> if (intervalCount == 1) "week" else "weeks"
            "months" -> if (intervalCount == 1) "month" else "months"
            "years" -> if (intervalCount == 1) "year" else "years"
            else -> intervalUnit
        }
        "Every $intervalCount $unitLabel"
    }

    var isCustomIntervalMode by remember {
        mutableStateOf(initialRepeat != null && initialRepeat !in quickPresets && initialRepeat.startsWith("Every "))
    }

    val activeRepeatRule = if (isCustomIntervalMode) customIntervalString else (selectedPreset ?: "Every week")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "REPEAT SCHEDULE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = NudgeBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Custom recurrence",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPresets.forEach { preset ->
                        val isSelected = !isCustomIntervalMode && selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                isCustomIntervalMode = false
                            },
                            label = {
                                Text(preset, style = MaterialTheme.typography.bodySmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NudgeBlueContainer,
                                selectedLabelColor = OnNudgeBlueContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Custom Interval",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCustomIntervalMode) NudgeBlueContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCustomIntervalMode) NudgeBlue else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustomIntervalMode = true }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat every:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        isCustomIntervalMode = true
                                        if (intervalCount > 1) intervalCount--
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease count",
                                        modifier = Modifier.size(16.dp),
                                        tint = NudgeBlue
                                    )
                                }

                                Text(
                                    text = "$intervalCount",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = {
                                        isCustomIntervalMode = true
                                        if (intervalCount < 99) intervalCount++
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase count",
                                        modifier = Modifier.size(16.dp),
                                        tint = NudgeBlue
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("days", "weeks", "months", "years").forEach { unit ->
                                val isUnitSelected = isCustomIntervalMode && intervalUnit == unit
                                val label = unit.replaceFirstChar { it.uppercase() }
                                Surface(
                                    onClick = {
                                        isCustomIntervalMode = true
                                        intervalUnit = unit
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isUnitSelected) NudgeBlueContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isUnitSelected) OnNudgeBlueContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isUnitSelected) NudgeBlue else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isUnitSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NudgeBlueContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = NudgeBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Repeats: $activeRepeatRule",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = OnNudgeBlueContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(activeRepeatRule) },
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
            ) {
                Text("Set Repeat")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

