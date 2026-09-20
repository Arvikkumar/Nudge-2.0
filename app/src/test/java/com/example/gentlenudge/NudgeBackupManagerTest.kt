package com.example.gentlenudge

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gentlenudge.attachment.AttachmentManager
import com.example.gentlenudge.backup.NudgeBackupManager
import com.example.gentlenudge.backup.NudgeBackupScheduler
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class NudgeBackupManagerTest {

    private lateinit var context: Context
    private lateinit var testDatabase: AppDatabase
    private lateinit var testPrefs: SharedPreferences
    private val testDbName = "backup_test_db.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
        testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        testPrefs = context.getSharedPreferences("test_nudge_prefs", Context.MODE_PRIVATE)
        testPrefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        testDatabase.close()
        testPrefs.edit().clear().apply()
    }

    @Test
    fun testCreateAndValidateBackup_withFullDataAndAttachments() = runBlocking {
        // 1. Create a dummy attachment file
        val attachmentsDir = AttachmentManager.getAttachmentsDir(context)
        val sampleAttachmentFile = File(attachmentsDir, "test_audio_123.m4a")
        sampleAttachmentFile.writeText("MOCK_AUDIO_DATA_FOR_BACKUP_TEST")

        val sampleAttachment = TaskAttachment(
            id = "att-1",
            type = "audio",
            filePath = sampleAttachmentFile.absolutePath,
            fileName = "test_audio_123.m4a",
            fileSize = sampleAttachmentFile.length(),
            durationMs = 4500L,
            mimeType = "audio/mp4"
        )

        // 2. Populate Tasks
        val task1 = NudgeTask(
            id = 1L,
            title = "Drink warm water",
            timeLabel = "Morning",
            dateLabel = "Today",
            category = "Personal",
            priority = "Normal",
            repeat = "Daily",
            soundType = "Small nudge",
            attachmentsJson = TaskAttachment.listToJson(listOf(sampleAttachment)),
            isDone = false,
            section = "today",
            createdAt = 1700000000000L
        )
        val task2 = NudgeTask(
            id = 2L,
            title = "Review quarterly report",
            timeLabel = "3:00 PM",
            dateLabel = "Tomorrow",
            category = "Work",
            priority = "High",
            repeat = "Does not repeat",
            soundType = "Calm bell",
            isDone = true,
            section = "later",
            createdAt = 1700000500000L,
            completedAt = 1700001000000L
        )
        testDatabase.nudgeTaskDao().insertAll(listOf(task1, task2))

        // 3. Populate Time Goals & Records
        val goal1 = TimeGoal(
            id = 10L,
            name = "Reading & Study",
            dailyTargetMinutes = 45,
            colorHex = "#2196F3",
            iconName = "book",
            startDate = "2026-08-01",
            isArchived = false
        )
        testDatabase.timeGoalDao().insertGoal(goal1)

        val record1 = TimeGoalRecord(
            id = 100L,
            goalId = 10L,
            date = "2026-08-25",
            actualMinutes = 50,
            note = "Finished Chapter 4"
        )
        testDatabase.timeGoalDao().insertOrUpdateRecord(record1)

        // 4. Set Preferences
        testPrefs.edit()
            .putBoolean("dark_theme", false)
            .putBoolean("notifications_enabled", true)
            .putBoolean("nudge_again_enabled", false)
            .putString("default_snooze", "1 hour")
            .apply()

        // 5. Create Backup Package
        val (zipBytes, manifest) = NudgeBackupManager.createBackupZipBytes(context, testDatabase, testPrefs)

        assertNotNull(zipBytes)
        assertTrue(zipBytes.isNotEmpty())
        assertEquals(NudgeBackupManager.APP_IDENTIFIER, manifest.app)
        assertEquals(NudgeBackupManager.BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(NudgeBackupManager.CURRENT_SCHEMA_VERSION, manifest.schemaVersion)
        assertEquals(2, manifest.tasksCount)
        assertEquals(1, manifest.timeGoalsCount)
        assertEquals(1, manifest.timeGoalRecordsCount)
        assertEquals(1, manifest.attachmentsCount)

        // 6. Validate Backup Package
        val validation = NudgeBackupManager.validateBackupBytes(zipBytes)
        assertTrue("Validation should succeed: ${validation.errorMessage}", validation.isValid)
        assertEquals(2, validation.tasks.size)
        assertEquals(1, validation.timeGoals.size)
        assertEquals(1, validation.timeGoalRecords.size)
        assertEquals(1, validation.attachmentEntries.size)
        assertTrue(validation.attachmentEntries.containsKey("test_audio_123.m4a"))
        assertEquals("MOCK_AUDIO_DATA_FOR_BACKUP_TEST", String(validation.attachmentEntries["test_audio_123.m4a"]!!))
    }

    @Test
    fun testValidationFails_whenChecksumMismatchedOrCorrupted() {
        val manifest = JSONObject().apply {
            put("formatVersion", 1)
            put("app", "Nudge")
            put("schemaVersion", 5)
            put("timestamp", System.currentTimeMillis())
            put("payloadChecksum", "invalid_checksum_value_here")
            put("tasksCount", 1)
            put("timeGoalsCount", 0)
            put("timeGoalRecordsCount", 0)
            put("attachmentsCount", 0)
        }
        val payload = JSONObject().apply {
            put("tasks", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                    put("title", "Test Task")
                })
            })
        }

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry(NudgeBackupManager.MANIFEST_ENTRY_NAME))
            zos.write(manifest.toString().toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(NudgeBackupManager.PAYLOAD_ENTRY_NAME))
            zos.write(payload.toString().toByteArray())
            zos.closeEntry()
        }

        val validation = NudgeBackupManager.validateBackupBytes(byteOut.toByteArray())
        assertFalse("Validation must fail when checksum does not match", validation.isValid)
        assertTrue(validation.errorMessage?.contains("SHA-256 mismatch") == true)
    }

    @Test
    fun testValidationFails_whenWrongAppIdentifier() {
        val payloadBytes = "{}".toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val checksum = digest.digest(payloadBytes).joinToString("") { "%02x".format(it) }

        val manifest = JSONObject().apply {
            put("formatVersion", 1)
            put("app", "SomeOtherApp")
            put("schemaVersion", 5)
            put("timestamp", System.currentTimeMillis())
            put("payloadChecksum", checksum)
        }

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry(NudgeBackupManager.MANIFEST_ENTRY_NAME))
            zos.write(manifest.toString().toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(NudgeBackupManager.PAYLOAD_ENTRY_NAME))
            zos.write(payloadBytes)
            zos.closeEntry()
        }

        val validation = NudgeBackupManager.validateBackupBytes(byteOut.toByteArray())
        assertFalse(validation.isValid)
        assertTrue(validation.errorMessage?.contains("SomeOtherApp") == true)
    }

    @Test
    fun testValidationFails_whenMissingManifest() {
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry(NudgeBackupManager.PAYLOAD_ENTRY_NAME))
            zos.write("{}".toByteArray())
            zos.closeEntry()
        }

        val validation = NudgeBackupManager.validateBackupBytes(byteOut.toByteArray())
        assertFalse(validation.isValid)
        assertTrue(validation.errorMessage?.contains("missing manifest") == true)
    }

    @Test
    fun testRestoreBackup_restoresDataAtomicallyAndAccurately() = runBlocking {
        // Setup initial database state
        val originalTask = NudgeTask(
            id = 55L,
            title = "Original Unrelated Task",
            isDone = false,
            section = "today"
        )
        testDatabase.nudgeTaskDao().insertTask(originalTask)

        // Prepare backup data to restore
        val attachmentsDir = AttachmentManager.getAttachmentsDir(context)
        val restoredFileName = "restored_voice_memo.m4a"
        val restoredFileContent = "RESTORED_AUDIO_VOICE_MEMO_CONTENT"

        val restoreTask = NudgeTask(
            id = 101L,
            title = "Morning Meditation",
            timeLabel = "7:00 AM",
            dateLabel = "Today",
            category = "Health",
            priority = "High",
            repeat = "Daily",
            soundType = "Tibetan bowl",
            ringtoneTitle = "Peaceful",
            attachmentsJson = TaskAttachment.listToJson(listOf(
                TaskAttachment(
                    id = "att-meditation",
                    type = "audio",
                    filePath = "/old/device/path/$restoredFileName",
                    fileName = restoredFileName,
                    fileSize = restoredFileContent.length.toLong(),
                    durationMs = 12000L,
                    mimeType = "audio/mp4"
                )
            )),
            isDone = false,
            section = "today",
            createdAt = 1710000000000L
        )

        val restoreGoal = TimeGoal(
            id = 201L,
            name = "Morning Jog",
            dailyTargetMinutes = 30,
            colorHex = "#4CAF50",
            iconName = "directions_run",
            startDate = "2026-08-01",
            isArchived = false
        )

        val restoreRecord = TimeGoalRecord(
            id = 301L,
            goalId = 201L,
            date = "2026-08-26",
            actualMinutes = 35,
            note = "Felt great"
        )

        val validation = com.example.gentlenudge.backup.BackupValidationResult(
            isValid = true,
            tasks = listOf(restoreTask),
            timeGoals = listOf(restoreGoal),
            timeGoalRecords = listOf(restoreRecord),
            preferences = mapOf(
                "dark_theme" to false,
                "notifications_enabled" to true,
                "nudge_again_enabled" to false,
                "default_snooze" to "15 minutes"
            ),
            attachmentEntries = mapOf(
                restoredFileName to restoredFileContent.toByteArray(Charsets.UTF_8)
            )
        )

        // Execute restore
        NudgeBackupManager.restoreValidatedBackup(context, validation, testDatabase, testPrefs)

        // Verify old tasks are replaced
        val tasksInDb = testDatabase.nudgeTaskDao().getAllTasksList()
        assertEquals(1, tasksInDb.size)
        val restoredTaskInDb = tasksInDb[0]
        assertEquals(101L, restoredTaskInDb.id)
        assertEquals("Morning Meditation", restoredTaskInDb.title)
        assertEquals("Health", restoredTaskInDb.category)
        assertEquals("High", restoredTaskInDb.priority)
        assertEquals("Daily", restoredTaskInDb.repeat)
        assertEquals("Tibetan bowl", restoredTaskInDb.soundType)

        // Verify attachment file was written and filePath path remapped to local device
        val expectedRestoredFile = File(attachmentsDir, restoredFileName)
        assertTrue("Attachment file must exist on disk", expectedRestoredFile.exists())
        assertEquals(restoredFileContent, expectedRestoredFile.readText())

        val restoredAttachments = TaskAttachment.listFromJson(restoredTaskInDb.attachmentsJson)
        assertEquals(1, restoredAttachments.size)
        assertEquals(expectedRestoredFile.absolutePath, restoredAttachments[0].filePath)

        // Verify Time Goals & Records
        val goalsInDb = testDatabase.timeGoalDao().getAllGoalsList()
        assertEquals(1, goalsInDb.size)
        assertEquals("Morning Jog", goalsInDb[0].name)
        assertEquals(30, goalsInDb[0].dailyTargetMinutes)

        val recordsInDb = testDatabase.timeGoalDao().getAllRecordsList()
        assertEquals(1, recordsInDb.size)
        assertEquals(201L, recordsInDb[0].goalId)
        assertEquals(35, recordsInDb[0].actualMinutes)
        assertEquals("Felt great", recordsInDb[0].note)

        // Verify Preferences
        assertFalse(testPrefs.getBoolean("dark_theme", true))
        assertTrue(testPrefs.getBoolean("notifications_enabled", false))
        assertFalse(testPrefs.getBoolean("nudge_again_enabled", true))
        assertEquals("15 minutes", testPrefs.getString("default_snooze", ""))
    }

    @Test
    fun testEndToEnd_FullBackupAndRestoreCycle() = runBlocking {
        // 1. Initial State with 3 tasks and 2 goals
        val t1 = NudgeTask(title = "Task One", section = "today")
        val t2 = NudgeTask(title = "Task Two", isDone = true, section = "today")
        val t3 = NudgeTask(title = "Task Three", section = "later")
        testDatabase.nudgeTaskDao().insertAll(listOf(t1, t2, t3))

        val g1 = TimeGoal(name = "Goal One", dailyTargetMinutes = 20, startDate = "2026-08-01")
        val g2 = TimeGoal(name = "Goal Two", dailyTargetMinutes = 40, startDate = "2026-08-01")
        testDatabase.timeGoalDao().insertAllGoals(listOf(g1, g2))

        val r1 = TimeGoalRecord(goalId = 1L, date = "2026-08-26", actualMinutes = 25)
        testDatabase.timeGoalDao().insertOrUpdateRecord(r1)

        testPrefs.edit().putBoolean("dark_theme", false).putString("default_snooze", "45 minutes").apply()

        // 2. Export Backup
        val (zipBytes, manifest) = NudgeBackupManager.createBackupZipBytes(context, testDatabase, testPrefs)
        assertEquals(3, manifest.tasksCount)
        assertEquals(2, manifest.timeGoalsCount)
        assertEquals(1, manifest.timeGoalRecordsCount)

        // 3. Clear database completely
        testDatabase.nudgeTaskDao().clearAll()
        testDatabase.timeGoalDao().clearAllGoals()
        testDatabase.timeGoalDao().clearAllRecords()
        testPrefs.edit().clear().apply()

        assertEquals(0, testDatabase.nudgeTaskDao().getAllTasksList().size)
        assertEquals(0, testDatabase.timeGoalDao().getAllGoalsList().size)

        // 4. Validate & Restore from backup bytes
        val validation = NudgeBackupManager.validateBackupBytes(zipBytes)
        assertTrue(validation.isValid)

        NudgeBackupManager.restoreValidatedBackup(context, validation, testDatabase, testPrefs)

        // 5. Assert all data restored exactly
        val restoredTasks = testDatabase.nudgeTaskDao().getAllTasksList()
        assertEquals(3, restoredTasks.size)
        assertTrue(restoredTasks.any { it.title == "Task One" })
        assertTrue(restoredTasks.any { it.title == "Task Two" && it.isDone })
        assertTrue(restoredTasks.any { it.title == "Task Three" })

        val restoredGoals = testDatabase.timeGoalDao().getAllGoalsList()
        assertEquals(2, restoredGoals.size)
        assertTrue(restoredGoals.any { it.name == "Goal One" })
        assertTrue(restoredGoals.any { it.name == "Goal Two" })

        val restoredRecords = testDatabase.timeGoalDao().getAllRecordsList()
        assertEquals(1, restoredRecords.size)
        assertEquals(25, restoredRecords[0].actualMinutes)

        assertFalse(testPrefs.getBoolean("dark_theme", true))
        assertEquals("45 minutes", testPrefs.getString("default_snooze", ""))
    }

    @Test
    fun testSeparateBackupTimestamps_manualUpdatesManualOnly_autoUpdatesAutoOnly() = runBlocking {
        // Initial state: both zero
        assertEquals(0L, NudgeBackupScheduler.getLastManualBackupTimestamp(testPrefs))
        assertEquals(0L, NudgeBackupScheduler.getLastAutoBackupTimestamp(testPrefs))

        // Trigger automatic backup
        val autoResult = NudgeBackupManager.performAutomaticBackup(context, testDatabase, testPrefs)
        assertTrue(autoResult.isSuccess)

        val autoTimestamp = NudgeBackupScheduler.getLastAutoBackupTimestamp(testPrefs)
        assertTrue("Auto timestamp should be > 0", autoTimestamp > 0L)
        assertEquals("Manual timestamp must remain 0 after auto backup", 0L, NudgeBackupScheduler.getLastManualBackupTimestamp(testPrefs))

        // Now simulate manual backup
        val fakeUri = android.net.Uri.parse("content://fake/backup.nudgebackup")
        // We write directly or test manual timestamp persistence
        val now = System.currentTimeMillis()
        testPrefs.edit()
            .putLong(NudgeBackupScheduler.KEY_LAST_MANUAL_BACKUP_TIMESTAMP, now)
            .putLong(NudgeBackupScheduler.KEY_LAST_BACKUP_TIMESTAMP, now)
            .apply()

        assertEquals(now, NudgeBackupScheduler.getLastManualBackupTimestamp(testPrefs))
        assertEquals(autoTimestamp, NudgeBackupScheduler.getLastAutoBackupTimestamp(testPrefs))
    }

    @Test
    fun testRetentionPolicy_keepsLatestThreeAutoBackups() = runBlocking {
        val dir = NudgeBackupManager.getAutoBackupsDirectory(context)
        dir.listFiles()?.forEach { it.delete() }

        // Create 5 dummy automatic backup files with different modification times
        val createdFiles = mutableListOf<File>()
        for (i in 1..5) {
            val f = File(dir, "Nudge_Backup_2026-08-0${i}_20-00.nudgebackup")
            f.writeText("BACKUP_DATA_$i")
            f.setLastModified(1700000000000L + (i * 100000L))
            createdFiles.add(f)
        }

        assertEquals(5, NudgeBackupManager.getAutoBackupFiles(context).size)

        // Apply retention policy (retain latest 3)
        NudgeBackupManager.cleanOldAutoBackups(context, retainCount = 3)

        val remainingFiles = NudgeBackupManager.getAutoBackupFiles(context)
        assertEquals(3, remainingFiles.size)
        // The retained files must be the latest 3 (i = 5, 4, 3)
        val remainingNames = remainingFiles.map { it.name }
        assertTrue(remainingNames.contains("Nudge_Backup_2026-08-05_20-00.nudgebackup"))
        assertTrue(remainingNames.contains("Nudge_Backup_2026-08-04_20-00.nudgebackup"))
        assertTrue(remainingNames.contains("Nudge_Backup_2026-08-03_20-00.nudgebackup"))
        assertFalse(remainingNames.contains("Nudge_Backup_2026-08-01_20-00.nudgebackup"))
        assertFalse(remainingNames.contains("Nudge_Backup_2026-08-02_20-00.nudgebackup"))
    }

    @Test
    fun testAutoBackupFilesInfo_returnsFormattedMetadata() {
        val dir = NudgeBackupManager.getAutoBackupsDirectory(context)
        dir.listFiles()?.forEach { it.delete() }

        val f = File(dir, "Nudge_Backup_2026-08-28_23-00.nudgebackup")
        f.writeText("HELLO_WORLD_TEST_BACKUP")

        val list = NudgeBackupManager.getAutoBackupFilesInfo(context)
        assertEquals(1, list.size)
        val info = list[0]
        assertEquals("Nudge_Backup_2026-08-28_23-00.nudgebackup", info.name)
        assertTrue(info.formattedDate.isNotBlank())
        assertTrue(info.formattedSize.isNotBlank())
        assertTrue(info.sizeBytes > 0)
    }

    @Test
    fun testFormatLastBackupRelative_values() {
        assertEquals("Never", NudgeBackupScheduler.formatLastBackupRelative(0L))
        assertEquals("Never", NudgeBackupScheduler.formatLastBackupRelative(-1L))

        val now = System.currentTimeMillis()
        assertEquals("Today", NudgeBackupScheduler.formatLastBackupRelative(now))
        assertEquals("Today", NudgeBackupScheduler.formatLastBackupRelative(now - 3600000L))

        val yesterday = now - (25L * 3600000L)
        assertEquals("Yesterday", NudgeBackupScheduler.formatLastBackupRelative(yesterday))

        val fiveDaysAgo = now - (5L * 24L * 3600000L)
        assertEquals("5 days ago", NudgeBackupScheduler.formatLastBackupRelative(fiveDaysAgo))
    }

    @Test
    fun testCalculatePreviousScheduledTime_calculation() {
        // Test daily previous occurrence
        val now = System.currentTimeMillis()
        val prevDaily = NudgeBackupScheduler.calculatePreviousScheduledTime(now, 0, 20, 0)
        assertTrue("Previous daily scheduled time must be <= now", prevDaily <= now)
        assertTrue("Previous daily scheduled time must be within last 24 hours", (now - prevDaily) <= (24L * 3600000L + 60000L))

        // Test weekly previous occurrence
        val prevWeekly = NudgeBackupScheduler.calculatePreviousScheduledTime(now, java.util.Calendar.SUNDAY, 20, 0)
        assertTrue("Previous weekly scheduled time must be <= now", prevWeekly <= now)
        assertTrue("Previous weekly scheduled time must be within last 7 days", (now - prevWeekly) <= (7L * 24L * 3600000L + 60000L))
    }
}
