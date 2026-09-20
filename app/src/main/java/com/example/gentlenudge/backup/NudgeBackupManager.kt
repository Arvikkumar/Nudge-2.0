package com.example.gentlenudge.backup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.gentlenudge.attachment.AttachmentManager
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalMonthColor
import com.example.gentlenudge.data.model.TimeGoalRecord
import org.json.JSONArray
import org.json.JSONObject
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val formatVersion: Int,
    val app: String,
    val schemaVersion: Int,
    val timestamp: Long,
    val createdAtIso: String,
    val payloadChecksum: String,
    val tasksCount: Int,
    val timeGoalsCount: Int,
    val timeGoalRecordsCount: Int,
    val attachmentsCount: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("formatVersion", formatVersion)
            put("app", app)
            put("schemaVersion", schemaVersion)
            put("timestamp", timestamp)
            put("createdAtIso", createdAtIso)
            put("payloadChecksum", payloadChecksum)
            put("tasksCount", tasksCount)
            put("timeGoalsCount", timeGoalsCount)
            put("timeGoalRecordsCount", timeGoalRecordsCount)
            put("attachmentsCount", attachmentsCount)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BackupManifest {
            return BackupManifest(
                formatVersion = json.getInt("formatVersion"),
                app = json.getString("app"),
                schemaVersion = json.getInt("schemaVersion"),
                timestamp = json.getLong("timestamp"),
                createdAtIso = json.optString("createdAtIso", ""),
                payloadChecksum = json.getString("payloadChecksum"),
                tasksCount = json.optInt("tasksCount", 0),
                timeGoalsCount = json.optInt("timeGoalsCount", 0),
                timeGoalRecordsCount = json.optInt("timeGoalRecordsCount", 0),
                attachmentsCount = json.optInt("attachmentsCount", 0)
            )
        }
    }
}

data class BackupValidationResult(
    val isValid: Boolean,
    val manifest: BackupManifest? = null,
    val errorMessage: String? = null,
    val tasks: List<NudgeTask> = emptyList(),
    val timeGoals: List<TimeGoal> = emptyList(),
    val timeGoalRecords: List<TimeGoalRecord> = emptyList(),
    val monthColors: List<TimeGoalMonthColor> = emptyList(),
    val preferences: Map<String, Any?> = emptyMap(),
    val attachmentEntries: Map<String, ByteArray> = emptyMap()
)

object NudgeBackupManager {

    const val APP_IDENTIFIER = "Nudge"
    const val BACKUP_FORMAT_VERSION = 1
    const val CURRENT_SCHEMA_VERSION = 7

    const val MANIFEST_ENTRY_NAME = "backup_manifest.json"
    const val PAYLOAD_ENTRY_NAME = "database_payload.json"
    const val ATTACHMENTS_DIR_PREFIX = "attachments/"

    fun generateDefaultBackupFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "nudge_backup_${timeStamp}.nudgebackup"
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    // ==========================================
    // BACKUP CREATION
    // ==========================================

    suspend fun createBackupZipBytes(
        context: Context,
        database: AppDatabase,
        prefs: SharedPreferences
    ): Pair<ByteArray, BackupManifest> {
        val tasks = database.nudgeTaskDao().getAllTasksIncludingDeletedList()
        val timeGoals = database.timeGoalDao().getAllGoalsList()
        val records = database.timeGoalDao().getAllRecordsList()
        val monthColors = database.timeGoalDao().getAllMonthColorsList()

        // Gather preferences
        val prefMap = mutableMapOf<String, Any?>()
        if (prefs.contains("dark_theme")) {
            prefMap["dark_theme"] = prefs.getBoolean("dark_theme", true)
        }
        if (prefs.contains("notifications_enabled")) {
            prefMap["notifications_enabled"] = prefs.getBoolean("notifications_enabled", true)
        }
        if (prefs.contains("nudge_again_enabled")) {
            prefMap["nudge_again_enabled"] = prefs.getBoolean("nudge_again_enabled", true)
        }
        if (prefs.contains("default_snooze")) {
            prefMap["default_snooze"] = prefs.getString("default_snooze", "30 minutes")
        }
        if (prefs.contains("user_name")) {
            prefMap["user_name"] = prefs.getString("user_name", "Bloom")
        }
        if (prefs.contains(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED)) {
            prefMap[NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED] = prefs.getBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, false)
        }
        if (prefs.contains(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY)) {
            prefMap[NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY] = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, Calendar.SUNDAY)
        }
        if (prefs.contains(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR)) {
            prefMap[NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR] = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, 23)
        }
        if (prefs.contains(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE)) {
            prefMap[NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE] = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, 0)
        }
        if (prefs.contains(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE)) {
            prefMap[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE] = prefs.getString(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE, "OFF")
        }
        if (prefs.contains(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS)) {
            prefMap[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS] = prefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS, 1)
        }
        if (prefs.contains(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR)) {
            prefMap[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR] = prefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR, 9)
        }
        if (prefs.contains(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE)) {
            prefMap[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE] = prefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE, 0)
        }

        // Serialize Payload JSON
        val payloadJson = JSONObject().apply {
            val tasksArray = JSONArray()
            for (t in tasks) {
                tasksArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("timeLabel", t.timeLabel)
                    put("dateLabel", t.dateLabel)
                    put("category", t.category)
                    put("priority", t.priority)
                    put("repeat", t.repeat)
                    put("soundType", t.soundType)
                    if (t.ringtoneUri != null) put("ringtoneUri", t.ringtoneUri)
                    if (t.ringtoneTitle != null) put("ringtoneTitle", t.ringtoneTitle)
                    if (t.attachmentsJson != null) put("attachmentsJson", t.attachmentsJson)
                    put("isDone", t.isDone)
                    put("section", t.section)
                    put("createdAt", t.createdAt)
                    if (t.completedAt != null) put("completedAt", t.completedAt)
                    put("isDeleted", t.isDeleted)
                    if (t.deletedAt != null) put("deletedAt", t.deletedAt)
                })
            }
            put("tasks", tasksArray)

            val goalsArray = JSONArray()
            for (g in timeGoals) {
                goalsArray.put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("dailyTargetMinutes", g.dailyTargetMinutes)
                    put("colorHex", g.colorHex)
                    put("iconName", g.iconName)
                    put("startDate", g.startDate)
                    put("isArchived", g.isArchived)
                    put("createdAt", g.createdAt)
                    put("orderIndex", g.orderIndex)
                })
            }
            put("timeGoals", goalsArray)

            val recordsArray = JSONArray()
            for (r in records) {
                recordsArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("goalId", r.goalId)
                    put("date", r.date)
                    put("actualMinutes", r.actualMinutes)
                    if (r.note != null) put("note", r.note)
                    put("updatedAt", r.updatedAt)
                })
            }
            put("timeGoalRecords", recordsArray)

            val monthColorsArray = JSONArray()
            for (mc in monthColors) {
                monthColorsArray.put(JSONObject().apply {
                    put("goalId", mc.goalId)
                    put("yearMonth", mc.yearMonth)
                    put("colorHex", mc.colorHex)
                    put("updatedAt", mc.updatedAt)
                })
            }
            put("timeGoalMonthColors", monthColorsArray)

            val prefsJson = JSONObject()
            for ((k, v) in prefMap) {
                prefsJson.put(k, v)
            }
            put("preferences", prefsJson)
        }

        val payloadBytes = payloadJson.toString(2).toByteArray(Charsets.UTF_8)
        val payloadChecksum = sha256(payloadBytes)

        // Find attachment files
        val attachmentFiles = mutableMapOf<String, File>()
        for (task in tasks) {
            val attachments = TaskAttachment.listFromJson(task.attachmentsJson)
            for (att in attachments) {
                if (att.filePath.isNotBlank()) {
                    val file = File(att.filePath)
                    if (file.exists() && file.isFile) {
                        attachmentFiles[file.name] = file
                    }
                }
            }
        }

        val now = System.currentTimeMillis()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val manifest = BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            app = APP_IDENTIFIER,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            timestamp = now,
            createdAtIso = isoFormat.format(Date(now)),
            payloadChecksum = payloadChecksum,
            tasksCount = tasks.size,
            timeGoalsCount = timeGoals.size,
            timeGoalRecordsCount = records.size,
            attachmentsCount = attachmentFiles.size
        )

        val manifestBytes = manifest.toJson().toString(2).toByteArray(Charsets.UTF_8)

        // Create Zip Output
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            // 1. Manifest
            val manifestEntry = ZipEntry(MANIFEST_ENTRY_NAME)
            zos.putNextEntry(manifestEntry)
            zos.write(manifestBytes)
            zos.closeEntry()

            // 2. Payload
            val payloadEntry = ZipEntry(PAYLOAD_ENTRY_NAME)
            zos.putNextEntry(payloadEntry)
            zos.write(payloadBytes)
            zos.closeEntry()

            // 3. Attachments
            for ((fileName, file) in attachmentFiles) {
                val attEntry = ZipEntry("$ATTACHMENTS_DIR_PREFIX$fileName")
                zos.putNextEntry(attEntry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        val zipBytes = byteOut.toByteArray()

        // Validate created bytes immediately before returning
        val validation = validateBackupBytes(zipBytes)
        if (!validation.isValid) {
            throw IllegalStateException("Generated backup failed self-validation: ${validation.errorMessage}")
        }

        return Pair(zipBytes, manifest)
    }

    suspend fun writeBackupToUri(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        prefs: SharedPreferences
    ): Result<BackupManifest> {
        return try {
            val (zipBytes, manifest) = createBackupZipBytes(context, database, prefs)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(zipBytes)
                outputStream.flush()
            } ?: return Result.failure(IllegalStateException("Could not open destination storage for writing."))

            // Update confirmed last manual backup timestamp on successful manual export
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(NudgeBackupScheduler.KEY_LAST_MANUAL_BACKUP_TIMESTAMP, now)
                .putLong(NudgeBackupScheduler.KEY_LAST_BACKUP_TIMESTAMP, now)
                .apply()

            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // AUTOMATIC LOCAL BACKUP EXECUTION & RETENTION
    // ==========================================

    data class AutoBackupFileInfo(
        val file: File,
        val name: String,
        val formattedDate: String,
        val sizeBytes: Long,
        val formattedSize: String
    )

    fun getAutoBackupsDirectory(context: Context): File {
        val dir = File(context.filesDir, "auto_backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAutoBackupFiles(context: Context): List<File> {
        val dir = getAutoBackupsDirectory(context)
        return dir.listFiles { file ->
            file.isFile && file.name.startsWith("Nudge_Backup_") && file.name.endsWith(".nudgebackup")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getAutoBackupFilesInfo(context: Context): List<AutoBackupFileInfo> {
        val files = getAutoBackupFiles(context)
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return files.map { file ->
            val dateStr = sdf.format(Date(file.lastModified()))
            val sizeKb = (file.length() + 1023) / 1024
            val sizeStr = if (sizeKb >= 1024) String.format(Locale.US, "%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"
            AutoBackupFileInfo(
                file = file,
                name = file.name,
                formattedDate = dateStr,
                sizeBytes = file.length(),
                formattedSize = sizeStr
            )
        }
    }

    fun shareAutoBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Nudge Backup - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Automatic Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("NudgeBackupManager", "Error sharing auto backup: ${e.message}", e)
        }
    }

    fun cleanOldAutoBackups(context: Context, retainCount: Int = 3) {
        val backups = getAutoBackupFiles(context)
        if (backups.size > retainCount) {
            backups.drop(retainCount).forEach { fileToDelete ->
                try {
                    fileToDelete.delete()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    suspend fun performAutomaticBackup(
        context: Context,
        database: AppDatabase,
        prefs: SharedPreferences
    ): Result<File> {
        val dir = getAutoBackupsDirectory(context)
        val tempFile = File(dir, "auto_backup_in_progress.tmp")

        return try {
            // 1. Generate standard, fully-compatible .nudgebackup bytes
            val (zipBytes, manifest) = createBackupZipBytes(context, database, prefs)

            // 2. Validate created bytes before committing to disk
            val validation = validateBackupBytes(zipBytes)
            if (!validation.isValid) {
                tempFile.delete()
                return Result.failure(IllegalStateException("Automatic backup self-validation failed: ${validation.errorMessage}"))
            }

            // 3. Write safely to temp file
            FileOutputStream(tempFile).use { fos ->
                fos.write(zipBytes)
                fos.flush()
            }

            // 4. Generate target timestamped filename e.g. Nudge_Backup_2026-08-28_23-00.nudgebackup
            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val finalFile = File(dir, "Nudge_Backup_${timeStamp}.nudgebackup")

            // Atomic rename to final file
            if (tempFile.renameTo(finalFile) || (finalFile.delete() && tempFile.renameTo(finalFile))) {
                // 5. Retain latest 3 automatic backups safely inside auto_backups/
                cleanOldAutoBackups(context, retainCount = 3)

                // 6. Update Last Automatic Backup timestamp only after confirmed successful completion
                val now = System.currentTimeMillis()
                prefs.edit().putLong(NudgeBackupScheduler.KEY_LAST_AUTO_BACKUP_TIMESTAMP, now).apply()

                Result.success(finalFile)
            } else {
                // Fallback copy if rename fails on some file systems
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
                cleanOldAutoBackups(context, retainCount = 3)
                val now = System.currentTimeMillis()
                prefs.edit().putLong(NudgeBackupScheduler.KEY_LAST_AUTO_BACKUP_TIMESTAMP, now).apply()
                Result.success(finalFile)
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        }
    }

    // ==========================================
    // BACKUP VALIDATION
    // ==========================================

    fun validateBackupBytes(zipBytes: ByteArray): BackupValidationResult {
        return validateBackupStream(ByteArrayInputStream(zipBytes))
    }

    fun validateBackupStream(inputStream: InputStream): BackupValidationResult {
        return try {
            var manifestBytes: ByteArray? = null
            var payloadBytes: ByteArray? = null
            val attachmentEntries = mutableMapOf<String, ByteArray>()

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == MANIFEST_ENTRY_NAME) {
                        manifestBytes = zis.readBytes()
                    } else if (name == PAYLOAD_ENTRY_NAME) {
                        payloadBytes = zis.readBytes()
                    } else if (name.startsWith(ATTACHMENTS_DIR_PREFIX) && !entry.isDirectory) {
                        val fileName = name.removePrefix(ATTACHMENTS_DIR_PREFIX)
                        if (fileName.isNotBlank()) {
                            attachmentEntries[fileName] = zis.readBytes()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestBytes == null) {
                return BackupValidationResult(isValid = false, errorMessage = "Backup is missing manifest header ($MANIFEST_ENTRY_NAME).")
            }
            if (payloadBytes == null) {
                return BackupValidationResult(isValid = false, errorMessage = "Backup is missing database payload ($PAYLOAD_ENTRY_NAME).")
            }

            val manifestJson = try {
                JSONObject(String(manifestBytes!!, Charsets.UTF_8))
            } catch (e: Exception) {
                return BackupValidationResult(isValid = false, errorMessage = "Manifest JSON is corrupted: ${e.message}")
            }

            val manifest = try {
                BackupManifest.fromJson(manifestJson)
            } catch (e: Exception) {
                return BackupValidationResult(isValid = false, errorMessage = "Manifest format is invalid: ${e.message}")
            }

            if (manifest.app != APP_IDENTIFIER) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "This backup belongs to '${manifest.app}', not '$APP_IDENTIFIER'."
                )
            }

            if (manifest.formatVersion > BACKUP_FORMAT_VERSION) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Unsupported backup format version (${manifest.formatVersion}). Please update the app."
                )
            }

            if (manifest.schemaVersion > CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Unsupported database schema version (${manifest.schemaVersion})."
                )
            }

            // Checksum verification
            val actualChecksum = sha256(payloadBytes!!)
            if (!actualChecksum.equals(manifest.payloadChecksum, ignoreCase = true)) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Backup payload integrity check failed (SHA-256 mismatch). File may be corrupted."
                )
            }

            // Parse and validate payload
            val payloadJson = try {
                JSONObject(String(payloadBytes!!, Charsets.UTF_8))
            } catch (e: Exception) {
                return BackupValidationResult(isValid = false, errorMessage = "Payload JSON is malformed: ${e.message}")
            }

            val tasksArray = payloadJson.optJSONArray("tasks")
                ?: return BackupValidationResult(isValid = false, errorMessage = "Payload missing 'tasks' array.")
            val parsedTasks = mutableListOf<NudgeTask>()
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val title = obj.optString("title", "")
                if (title.isBlank()) {
                    return BackupValidationResult(isValid = false, errorMessage = "Task at index $i is missing title.")
                }
                parsedTasks.add(
                    NudgeTask(
                        id = obj.optLong("id", 0L),
                        title = title,
                        timeLabel = obj.optString("timeLabel", "Any time"),
                        dateLabel = obj.optString("dateLabel", "Today"),
                        category = obj.optString("category", "Personal"),
                        priority = obj.optString("priority", "Normal"),
                        repeat = obj.optString("repeat", "Does not repeat"),
                        soundType = obj.optString("soundType", "Small nudge"),
                        ringtoneUri = if (obj.has("ringtoneUri") && !obj.isNull("ringtoneUri")) obj.getString("ringtoneUri") else null,
                        ringtoneTitle = if (obj.has("ringtoneTitle") && !obj.isNull("ringtoneTitle")) obj.getString("ringtoneTitle") else null,
                        attachmentsJson = if (obj.has("attachmentsJson") && !obj.isNull("attachmentsJson")) obj.getString("attachmentsJson") else null,
                        isDone = obj.optBoolean("isDone", false),
                        section = obj.optString("section", "today"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        completedAt = if (obj.has("completedAt") && !obj.isNull("completedAt")) obj.getLong("completedAt") else null,
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null
                    )
                )
            }

            val goalsArray = payloadJson.optJSONArray("timeGoals") ?: JSONArray()
            val parsedGoals = mutableListOf<TimeGoal>()
            for (i in 0 until goalsArray.length()) {
                val obj = goalsArray.getJSONObject(i)
                val name = obj.optString("name", "")
                if (name.isBlank()) {
                    return BackupValidationResult(isValid = false, errorMessage = "Time goal at index $i is missing name.")
                }
                val dailyTarget = obj.optInt("dailyTargetMinutes", 0)
                if (dailyTarget < 0) {
                    return BackupValidationResult(isValid = false, errorMessage = "Time goal '$name' has invalid target minutes.")
                }
                parsedGoals.add(
                    TimeGoal(
                        id = obj.optLong("id", 0L),
                        name = name,
                        dailyTargetMinutes = dailyTarget,
                        colorHex = obj.optString("colorHex", "#7C4DFF"),
                        iconName = obj.optString("iconName", "book"),
                        startDate = obj.optString("startDate", ""),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        orderIndex = obj.optInt("orderIndex", 0)
                    )
                )
            }

            val recordsArray = payloadJson.optJSONArray("timeGoalRecords") ?: JSONArray()
            val parsedRecords = mutableListOf<TimeGoalRecord>()
            for (i in 0 until recordsArray.length()) {
                val obj = recordsArray.getJSONObject(i)
                parsedRecords.add(
                    TimeGoalRecord(
                        id = obj.optLong("id", 0L),
                        goalId = obj.optLong("goalId", 0L),
                        date = obj.optString("date", ""),
                        actualMinutes = obj.optInt("actualMinutes", 0),
                        note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val monthColorsArray = payloadJson.optJSONArray("timeGoalMonthColors") ?: JSONArray()
            val parsedMonthColors = mutableListOf<TimeGoalMonthColor>()
            for (i in 0 until monthColorsArray.length()) {
                val obj = monthColorsArray.getJSONObject(i)
                parsedMonthColors.add(
                    TimeGoalMonthColor(
                        goalId = obj.optLong("goalId", 0L),
                        yearMonth = obj.optString("yearMonth", ""),
                        colorHex = obj.optString("colorHex", "#7C4DFF"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val prefsMap = mutableMapOf<String, Any?>()
            val prefsObj = payloadJson.optJSONObject("preferences")
            if (prefsObj != null) {
                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    prefsMap[key] = prefsObj.get(key)
                }
            }

            BackupValidationResult(
                isValid = true,
                manifest = manifest,
                tasks = parsedTasks,
                timeGoals = parsedGoals,
                timeGoalRecords = parsedRecords,
                monthColors = parsedMonthColors,
                preferences = prefsMap,
                attachmentEntries = attachmentEntries
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Failed to parse backup archive: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    fun validateBackupFromUri(context: Context, uri: Uri): Result<BackupValidationResult> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Could not open selected backup file."))
            val result = inputStream.use { validateBackupStream(it) }
            if (result.isValid) {
                Result.success(result)
            } else {
                Result.failure(IllegalStateException(result.errorMessage ?: "Invalid backup file."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // RESTORE EXECUTION (ATOMIC)
    // ==========================================

    suspend fun restoreBackupFromUri(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        prefs: SharedPreferences
    ): Result<BackupValidationResult> {
        return try {
            val validation = validateBackupFromUri(context, uri).getOrThrow()
            restoreValidatedBackup(context, validation, database, prefs)
            Result.success(validation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreValidatedBackup(
        context: Context,
        validation: BackupValidationResult,
        database: AppDatabase,
        prefs: SharedPreferences
    ) {
        if (!validation.isValid) {
            throw IllegalArgumentException("Cannot restore invalid backup: ${validation.errorMessage}")
        }

        val attachmentsDir = AttachmentManager.getAttachmentsDir(context)

        // 1. First extract attachment files safely to attachments directory
        val restoredFileMap = mutableMapOf<String, File>() // fileName -> destination File
        for ((fileName, fileBytes) in validation.attachmentEntries) {
            val destFile = File(attachmentsDir, fileName)
            FileOutputStream(destFile).use { fos ->
                fos.write(fileBytes)
                fos.flush()
            }
            restoredFileMap[fileName] = destFile
        }

        // 2. Normalize attachment filePaths in tasks to point to current device path
        val normalizedTasks = validation.tasks.map { task ->
            if (task.attachmentsJson.isNullOrBlank()) {
                task
            } else {
                val attachments = TaskAttachment.listFromJson(task.attachmentsJson)
                val updatedAttachments = attachments.map { att ->
                    val fileName = File(att.filePath).name
                    val localFile = restoredFileMap[fileName] ?: File(attachmentsDir, fileName)
                    if (localFile.exists()) {
                        att.copy(filePath = localFile.absolutePath)
                    } else {
                        att
                    }
                }
                task.copy(attachmentsJson = TaskAttachment.listToJson(updatedAttachments))
            }
        }

        // 3. Cancel alarms for existing tasks before clearing database
        val existingTasks = database.nudgeTaskDao().getAllTasksIncludingDeletedList()
        for (task in existingTasks) {
            com.example.gentlenudge.notification.NudgeAlarmScheduler.cancelTask(context, task.id)
        }

        // 4. Atomically perform Room database updates inside a transaction
        database.withTransaction {
            val taskDao = database.nudgeTaskDao()
            val goalDao = database.timeGoalDao()

            // Clear existing tables
            taskDao.clearAll()
            goalDao.clearAllRecords()
            goalDao.clearAllMonthColors()
            goalDao.clearAllGoals()

            // Insert restored entities preserving original IDs
            if (normalizedTasks.isNotEmpty()) {
                taskDao.insertAll(normalizedTasks)
            }
            if (validation.timeGoals.isNotEmpty()) {
                goalDao.insertAllGoals(validation.timeGoals)
            }
            if (validation.monthColors.isNotEmpty()) {
                goalDao.insertAllMonthColors(validation.monthColors)
            }
            if (validation.timeGoalRecords.isNotEmpty()) {
                goalDao.insertAllRecords(validation.timeGoalRecords)
            }
        }

        // 4. Restore preferences safely
        val prefEdit = prefs.edit()
        val p = validation.preferences
        if (p.containsKey("dark_theme")) {
            prefEdit.putBoolean("dark_theme", p["dark_theme"] as? Boolean ?: true)
        }
        if (p.containsKey("notifications_enabled")) {
            prefEdit.putBoolean("notifications_enabled", p["notifications_enabled"] as? Boolean ?: true)
        }
        if (p.containsKey("nudge_again_enabled")) {
            prefEdit.putBoolean("nudge_again_enabled", p["nudge_again_enabled"] as? Boolean ?: true)
        }
        if (p.containsKey("default_snooze")) {
            prefEdit.putString("default_snooze", p["default_snooze"] as? String ?: "30 minutes")
        }
        if (p.containsKey("user_name")) {
            prefEdit.putString("user_name", p["user_name"] as? String ?: "Bloom")
        }
        if (p.containsKey(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED)) {
            val v = p[NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED] as? Boolean
                ?: p[NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED]?.toString()?.toBooleanStrictOrNull()
                ?: false
            prefEdit.putBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, v)
        }
        if (p.containsKey(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY)) {
            val v = (p[NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY] as? Number)?.toInt()
                ?: p[NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY]?.toString()?.toIntOrNull()
                ?: Calendar.SUNDAY
            prefEdit.putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, v)
        }
        if (p.containsKey(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR)) {
            val v = (p[NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR] as? Number)?.toInt()
                ?: p[NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR]?.toString()?.toIntOrNull()
                ?: 23
            prefEdit.putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, v)
        }
        if (p.containsKey(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE)) {
            val v = (p[NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE] as? Number)?.toInt()
                ?: p[NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE]?.toString()?.toIntOrNull()
                ?: 0
            prefEdit.putInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, v)
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE)) {
            val v = p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE] as? String
                ?: p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE]?.toString()
                ?: "OFF"
            prefEdit.putString(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE, v)
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS)) {
            val v = (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS] as? Number)?.toInt()
                ?: p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS]?.toString()?.toIntOrNull()
                ?: 1
            prefEdit.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS, v)
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR)) {
            val v = (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR] as? Number)?.toInt()
                ?: p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR]?.toString()?.toIntOrNull()
                ?: 9
            prefEdit.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR, v)
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE)) {
            val v = (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE] as? Number)?.toInt()
                ?: p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE]?.toString()?.toIntOrNull()
                ?: 0
            prefEdit.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE, v)
        }
        prefEdit.commit()
    }
}
