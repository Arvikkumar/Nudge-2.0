package com.example.gentlenudge

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gentlenudge.backup.NudgeBackupManager
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.db.TimeGoalDao
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.export.MonthReportData
import com.example.gentlenudge.export.TimeGoalsPdfExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowTestPdfDocument::class])
class TimeGoalMonthScopingTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dao: TimeGoalDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.timeGoalDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddPursuitInOctober2026_belongsOnlyToOctober2026() = runBlocking {
        // Create "Grammar Course" in October 2026
        val octGoal = TimeGoal(
            name = "Grammar Course",
            dailyTargetMinutes = 60,
            colorHex = "#7C4DFF",
            iconName = "book",
            startDate = "2026-10-01",
            yearMonth = "2026-10",
            orderIndex = 0
        )
        val octGoalId = dao.insertGoal(octGoal)
        assertTrue(octGoalId > 0)

        // Query October 2026 -> Appears
        val octGoals = dao.getActiveGoalsForMonth("2026-10").first()
        assertEquals(1, octGoals.size)
        assertEquals("Grammar Course", octGoals[0].name)
        assertEquals("2026-10", octGoals[0].yearMonth)

        // Query September 2026 -> Must NOT appear
        val sepGoals = dao.getActiveGoalsForMonth("2026-09").first()
        assertTrue(sepGoals.isEmpty())

        // Query November 2026 -> Must NOT appear
        val novGoals = dao.getActiveGoalsForMonth("2026-11").first()
        assertTrue(novGoals.isEmpty())
    }

    @Test
    fun testIndependentPursuitsPerMonth() = runBlocking {
        // October: Grammar Course
        dao.insertGoal(
            TimeGoal(
                name = "Grammar Course",
                dailyTargetMinutes = 60,
                startDate = "2026-10-01",
                yearMonth = "2026-10"
            )
        )

        // November: Advanced Vocabulary
        dao.insertGoal(
            TimeGoal(
                name = "Advanced Vocabulary",
                dailyTargetMinutes = 45,
                startDate = "2026-11-01",
                yearMonth = "2026-11"
            )
        )

        val octGoals = dao.getActiveGoalsForMonth("2026-10").first()
        assertEquals(1, octGoals.size)
        assertEquals("Grammar Course", octGoals[0].name)

        val novGoals = dao.getActiveGoalsForMonth("2026-11").first()
        assertEquals(1, novGoals.size)
        assertEquals("Advanced Vocabulary", novGoals[0].name)

        // Neither leaks to August 2026
        val augGoals = dao.getActiveGoalsForMonth("2026-08").first()
        assertTrue(augGoals.isEmpty())
    }

    @Test
    fun testMonthlyProgressCalculations_isolatedByMonth() {
        val octGoal = TimeGoal(
            id = 10,
            name = "Grammar Course",
            dailyTargetMinutes = 60,
            startDate = "2026-10-01",
            yearMonth = "2026-10"
        )

        val novGoal = TimeGoal(
            id = 20,
            name = "Advanced Vocabulary",
            dailyTargetMinutes = 90,
            startDate = "2026-11-01",
            yearMonth = "2026-11"
        )

        val octYm = YearMonth.of(2026, 10)
        val novYm = YearMonth.of(2026, 11)

        // October progress uses only October goals
        val octProgress = TimeGoalCalculations.calculateMonthlyOverallProgress(
            goals = listOf(octGoal),
            yearMonth = octYm,
            allRecords = emptyList(),
            today = LocalDate.of(2026, 10, 15)
        )
        // October has 31 days * 60 = 1860 planned minutes
        assertEquals(1860, octProgress.totalPlannedMinutes)

        // November progress uses only November goals
        val novProgress = TimeGoalCalculations.calculateMonthlyOverallProgress(
            goals = listOf(novGoal),
            yearMonth = novYm,
            allRecords = emptyList(),
            today = LocalDate.of(2026, 11, 15)
        )
        // November has 30 days * 90 = 2700 planned minutes
        assertEquals(2700, novProgress.totalPlannedMinutes)
    }

    @Test
    fun testHistoricalRecordsPreservation_accessibleInMonthWithRecords() = runBlocking {
        // A pursuit created with yearMonth "2026-08", but with records logged in "2026-09"
        val goalId = dao.insertGoal(
            TimeGoal(
                name = "Reading Marathon",
                dailyTargetMinutes = 60,
                startDate = "2026-08-01",
                yearMonth = "2026-08"
            )
        )

        // Record logged in September 2026
        dao.insertOrUpdateRecord(
            TimeGoalRecord(
                goalId = goalId,
                date = "2026-09-12",
                actualMinutes = 60,
                note = "Chapter 4"
            )
        )

        // August sees it because yearMonth == "2026-08"
        val augGoals = dao.getActiveGoalsForMonth("2026-08").first()
        assertEquals(1, augGoals.size)
        assertEquals("Reading Marathon", augGoals[0].name)

        // September sees it because a record exists in September
        val sepGoals = dao.getActiveGoalsForMonth("2026-09").first()
        assertEquals(1, sepGoals.size)
        assertEquals("Reading Marathon", sepGoals[0].name)

        // October has no records and yearMonth is not October -> Does NOT appear
        val octGoals = dao.getActiveGoalsForMonth("2026-10").first()
        assertTrue(octGoals.isEmpty())
    }

    @Test
    fun testBackupAndRestore_preservesYearMonthAndHandlesLegacy() {
        val backupJson = JSONObject().apply {
            put("version", 2)
            put("appVersion", "2.0")
            put("exportTimestamp", System.currentTimeMillis())
            put("tasks", JSONArray())

            val goalsArr = JSONArray()
            // Modern goal with explicit yearMonth
            goalsArr.put(JSONObject().apply {
                put("id", 101L)
                put("name", "Grammar Course")
                put("dailyTargetMinutes", 60)
                put("colorHex", "#7C4DFF")
                put("iconName", "book")
                put("startDate", "2026-10-01")
                put("yearMonth", "2026-10")
                put("isArchived", false)
                put("createdAt", 1600000000000L)
                put("orderIndex", 0)
            })
            // Legacy goal without yearMonth (must fallback to startDate substring)
            goalsArr.put(JSONObject().apply {
                put("id", 102L)
                put("name", "Legacy Piano")
                put("dailyTargetMinutes", 45)
                put("colorHex", "#1E88E5")
                put("iconName", "music")
                put("startDate", "2026-07-01")
                put("isArchived", false)
                put("createdAt", 1600000000000L)
                put("orderIndex", 1)
            })
            put("timeGoals", goalsArr)
            put("timeGoalRecords", JSONArray())
            put("timeGoalMonthColors", JSONArray())
            put("preferences", JSONObject())
        }

        val payloadBytes = backupJson.toString().toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val checksum = digest.digest(payloadBytes).joinToString("") { "%02x".format(it) }

        val manifest = JSONObject().apply {
            put("formatVersion", NudgeBackupManager.BACKUP_FORMAT_VERSION)
            put("app", NudgeBackupManager.APP_IDENTIFIER)
            put("schemaVersion", NudgeBackupManager.CURRENT_SCHEMA_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("createdAtIso", "2026-10-15T00:00:00Z")
            put("payloadChecksum", checksum)
            put("tasksCount", 0)
            put("timeGoalsCount", 2)
            put("timeGoalRecordsCount", 0)
            put("attachmentsCount", 0)
        }

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry(NudgeBackupManager.MANIFEST_ENTRY_NAME))
            zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(NudgeBackupManager.PAYLOAD_ENTRY_NAME))
            zos.write(payloadBytes)
            zos.closeEntry()
        }

        val validation = NudgeBackupManager.validateBackupBytes(byteOut.toByteArray())
        assertTrue(validation.isValid)

        val parsedGoals = validation.timeGoals
        assertEquals(2, parsedGoals.size)

        val modernGoal = parsedGoals.first { it.id == 101L }
        assertEquals("2026-10", modernGoal.yearMonth)

        val legacyGoal = parsedGoals.first { it.id == 102L }
        assertEquals("2026-07", legacyGoal.yearMonth)
    }

    @Test
    fun testMultiMonthPdfExport_eachMonthHasOwnGoals() {
        val octGoal = TimeGoal(
            id = 1,
            name = "Grammar Course",
            dailyTargetMinutes = 60,
            startDate = "2026-10-01",
            yearMonth = "2026-10"
        )
        val novGoal = TimeGoal(
            id = 2,
            name = "Advanced Vocabulary",
            dailyTargetMinutes = 45,
            startDate = "2026-11-01",
            yearMonth = "2026-11"
        )

        val octData = MonthReportData(
            yearMonth = YearMonth.of(2026, 10),
            goals = listOf(octGoal),
            records = emptyList()
        )
        val novData = MonthReportData(
            yearMonth = YearMonth.of(2026, 11),
            goals = listOf(novGoal),
            records = emptyList()
        )

        assertEquals("Grammar Course", octData.goals.single().name)
        assertEquals("Advanced Vocabulary", novData.goals.single().name)

        val pdfFile = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = listOf(octData, novData)
        )
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }
}
