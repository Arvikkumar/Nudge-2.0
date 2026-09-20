package com.example.gentlenudge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.export.MonthReportData
import com.example.gentlenudge.export.TimeGoalsPdfExporter
import com.example.gentlenudge.export.TimeGoalsPdfOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTestPdfDocument::class])
class TimeGoalsPdfExporterTest {

    private lateinit var context: Context

    private val sampleGoals = listOf(
        TimeGoal(
            id = 1L,
            name = "Deep Work",
            dailyTargetMinutes = 120,
            colorHex = "#3D70FF",
            startDate = "2026-08-01"
        ),
        TimeGoal(
            id = 2L,
            name = "Reading",
            dailyTargetMinutes = 45,
            colorHex = "#2E7D32",
            startDate = "2026-08-01"
        )
    )

    private val sampleRecords = listOf(
        TimeGoalRecord(
            id = 101L,
            goalId = 1L,
            date = "2026-08-01",
            actualMinutes = 120
        ),
        TimeGoalRecord(
            id = 102L,
            goalId = 1L,
            date = "2026-08-02",
            actualMinutes = 60
        ),
        TimeGoalRecord(
            id = 103L,
            goalId = 2L,
            date = "2026-08-01",
            actualMinutes = 45
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowTestPdfDocument.pageCount = 0
    }

    @Test
    fun `test generatePdf with Visual Monthly Calendar Grid ONLY`() {
        val aug2026 = YearMonth.of(2026, 8)
        val options = TimeGoalsPdfOptions(
            includeSummary = false,
            includeGoalsList = false,
            includeCalendarGrid = true,
            includeDailyRecords = false,
            includeInsights = false
        )

        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.AUGUST, 30, 20, 27, 0)
        }.time
        val file = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = options,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        org.junit.Assert.assertEquals("Time Investment Report - 30 August 2026 — 08:27 PM.pdf", file.name)
        assertTrue(ShadowTestPdfDocument.pageCount >= 1)
    }

    @Test
    fun `test generatePdf with Visual Monthly Calendar Grid and Monthly Summary`() {
        val aug2026 = YearMonth.of(2026, 8)
        val options = TimeGoalsPdfOptions(
            includeSummary = true,
            includeGoalsList = false,
            includeCalendarGrid = true,
            includeDailyRecords = false,
            includeInsights = false
        )

        val file = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = options
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(ShadowTestPdfDocument.pageCount >= 1)
    }

    @Test
    fun `test generatePdf with all 4 document sections selected`() {
        val aug2026 = YearMonth.of(2026, 8)
        val options = TimeGoalsPdfOptions(
            includeSummary = true,
            includeGoalsList = true,
            includeCalendarGrid = true,
            includeDailyRecords = true,
            includeInsights = true
        )

        val file = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = options
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(ShadowTestPdfDocument.pageCount >= 1)
    }

    @Test
    fun `test generatePdf dynamically across different months`() {
        val monthsToTest = listOf(
            YearMonth.of(2026, 7),  // July 2026 (31 days)
            YearMonth.of(2025, 12), // December 2025 (31 days)
            YearMonth.of(2024, 2),  // February 2024 (Leap year, 29 days)
            YearMonth.of(2024, 11)  // November 2024 (30 days)
        )

        for (ym in monthsToTest) {
            val file = TimeGoalsPdfExporter.generatePdf(
                context = context,
                yearMonth = ym,
                goals = sampleGoals,
                records = emptyList(),
                options = TimeGoalsPdfOptions(
                    includeSummary = false,
                    includeGoalsList = false,
                    includeCalendarGrid = true,
                    includeDailyRecords = false,
                    includeInsights = false
                ),
                exportDate = java.util.Calendar.getInstance().apply {
                    set(2026, java.util.Calendar.AUGUST, 30, 20, 27, 0)
                }.time
            )
            org.junit.Assert.assertEquals("Time Investment Report - 30 August 2026 — 08:27 PM.pdf", file.name)
        }
    }

    @Test
    fun `test generatePdf with large list of goals triggering multiple pages`() {
        val aug2026 = YearMonth.of(2026, 8)
        val manyGoals = (1..15).map { i ->
            TimeGoal(
                id = i.toLong(),
                name = "Goal #$i — Extended Practice and Development Routine",
                dailyTargetMinutes = 30 + (i * 10),
                colorHex = if (i % 2 == 0) "#3D70FF" else "#2E7D32",
                startDate = "2026-08-01"
            )
        }
        val options = TimeGoalsPdfOptions(
            includeSummary = true,
            includeGoalsList = true,
            includeCalendarGrid = true,
            includeDailyRecords = true,
            includeInsights = true
        )

        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.AUGUST, 30, 20, 27, 0)
        }.time
        val file = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = manyGoals,
            records = sampleRecords,
            options = options,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        org.junit.Assert.assertEquals("Time Investment Report - 30 August 2026 — 08:27 PM.pdf", file.name)
        assertTrue("Multi-page generation should trigger page count > 1", ShadowTestPdfDocument.pageCount > 1)
    }

    @Test
    fun `test generatePdf with default options`() {
        val aug2026 = YearMonth.of(2026, 8)
        val file = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = TimeGoalsPdfOptions()
        )
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun `test buildMultiMonthFilename format`() {
        val jul2026 = YearMonth.of(2026, 7)
        val sep2026 = YearMonth.of(2026, 9)
        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 9, 10, 0, 0)
        }.time

        val filename = TimeGoalsPdfExporter.buildMultiMonthFilename(jul2026, sep2026, fixedDate)
        assertEquals("Time Investment Report - July 2026 to September 2026 — 09 September 2026.pdf", filename)

        // Same start and end month
        val singleRangeFilename = TimeGoalsPdfExporter.buildMultiMonthFilename(jul2026, jul2026, fixedDate)
        assertEquals("Time Investment Report - July 2026 — 09 September 2026.pdf", singleRangeFilename)
    }

    @Test
    fun `test generateMultiMonthPdf with 1-month custom range`() {
        val jul2026 = YearMonth.of(2026, 7)
        val monthsData = listOf(
            MonthReportData(
                yearMonth = jul2026,
                goals = sampleGoals,
                records = sampleRecords
            )
        )
        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 9, 12, 0, 0)
        }.time

        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("Time Investment Report - July 2026 — 09 September 2026.pdf", file.name)
        assertTrue(ShadowTestPdfDocument.pageCount >= 1)
    }

    @Test
    fun `test generateMultiMonthPdf with 2-month custom range`() {
        val jul2026 = YearMonth.of(2026, 7)
        val aug2026 = YearMonth.of(2026, 8)
        val monthsData = listOf(
            MonthReportData(yearMonth = jul2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = aug2026, goals = sampleGoals, records = sampleRecords)
        )
        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 9, 12, 0, 0)
        }.time

        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("Time Investment Report - July 2026 to August 2026 — 09 September 2026.pdf", file.name)
        // Each month must start on a brand new page, so 2 months guarantees at least 2 pages
        assertTrue("2 months must generate at least 2 pages", ShadowTestPdfDocument.pageCount >= 2)
    }

    @Test
    fun `test generateMultiMonthPdf with 3-month custom range`() {
        val jul2026 = YearMonth.of(2026, 7)
        val aug2026 = YearMonth.of(2026, 8)
        val sep2026 = YearMonth.of(2026, 9)
        val monthsData = listOf(
            MonthReportData(yearMonth = jul2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = aug2026, goals = sampleGoals, records = sampleRecords),
            MonthReportData(yearMonth = sep2026, goals = sampleGoals, records = emptyList())
        )
        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 9, 12, 0, 0)
        }.time

        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("Time Investment Report - July 2026 to September 2026 — 09 September 2026.pdf", file.name)
        // 3 months guarantees at least 3 pages
        assertTrue("3 months must generate at least 3 pages", ShadowTestPdfDocument.pageCount >= 3)
    }

    @Test
    fun `test generateMultiMonthPdf crossing year boundary`() {
        val nov2026 = YearMonth.of(2026, 11)
        val dec2026 = YearMonth.of(2026, 12)
        val jan2027 = YearMonth.of(2027, 1)
        val feb2027 = YearMonth.of(2027, 2)
        val monthsData = listOf(
            MonthReportData(yearMonth = nov2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = dec2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = jan2027, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = feb2027, goals = sampleGoals, records = emptyList())
        )
        val fixedDate = java.util.Calendar.getInstance().apply {
            set(2027, java.util.Calendar.FEBRUARY, 28, 12, 0, 0)
        }.time

        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData,
            exportDate = fixedDate
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("Time Investment Report - November 2026 to February 2027 — 28 February 2027.pdf", file.name)
        // 4 months guarantees at least 4 pages
        assertTrue("4 months must generate at least 4 pages", ShadowTestPdfDocument.pageCount >= 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test generateMultiMonthPdf throws on invalid range`() {
        val jul2026 = YearMonth.of(2026, 7)
        val aug2026 = YearMonth.of(2026, 8)
        // Inverted order: start is after end
        val monthsData = listOf(
            MonthReportData(yearMonth = aug2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = jul2026, goals = sampleGoals, records = emptyList())
        )
        TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test generateMultiMonthPdf throws on empty data`() {
        TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = emptyList()
        )
    }

    @Test
    fun `test generateMultiMonthPdf with exactly 12 months is valid`() {
        val monthsData = (1..12).map { month ->
            MonthReportData(
                yearMonth = YearMonth.of(2026, month),
                goals = sampleGoals,
                records = emptyList()
            )
        }
        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData
        )
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue("12 months must generate at least 12 pages", ShadowTestPdfDocument.pageCount >= 12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test generateMultiMonthPdf throws on more than 12 months`() {
        val monthsData = (1..13).map { month ->
            MonthReportData(
                yearMonth = YearMonth.of(2026, 1).plusMonths((month - 1).toLong()),
                goals = sampleGoals,
                records = emptyList()
            )
        }
        TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test generateMultiMonthPdf throws on skipped non-sequential month`() {
        val jul2026 = YearMonth.of(2026, 7)
        val sep2026 = YearMonth.of(2026, 9)
        // August is skipped!
        val monthsData = listOf(
            MonthReportData(yearMonth = jul2026, goals = sampleGoals, records = emptyList()),
            MonthReportData(yearMonth = sep2026, goals = sampleGoals, records = emptyList())
        )
        TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData
        )
    }

    @Test
    fun `test generateMultiMonthPdf with custom checkboxes disabled`() {
        val jul2026 = YearMonth.of(2026, 7)
        val aug2026 = YearMonth.of(2026, 8)
        val monthsData = listOf(
            MonthReportData(yearMonth = jul2026, goals = sampleGoals, records = sampleRecords),
            MonthReportData(yearMonth = aug2026, goals = sampleGoals, records = emptyList())
        )
        val file = TimeGoalsPdfExporter.generateMultiMonthPdf(
            context = context,
            monthsData = monthsData,
            options = TimeGoalsPdfOptions(
                includeSummary = false,
                includeGoalsList = false,
                includeCalendarGrid = false,
                includeInsights = false
            )
        )
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun `test month range sequence calculation logic`() {
        // 1 month: July -> July = 1
        var count = 0
        var curr = YearMonth.of(2026, 7)
        val end1 = YearMonth.of(2026, 7)
        while (!curr.isAfter(end1)) {
            count++
            curr = curr.plusMonths(1)
        }
        assertEquals(1, count)

        // 3 months: July -> September = 3
        count = 0
        curr = YearMonth.of(2026, 7)
        val end3 = YearMonth.of(2026, 9)
        val sequence3 = mutableListOf<YearMonth>()
        while (!curr.isAfter(end3)) {
            sequence3.add(curr)
            count++
            curr = curr.plusMonths(1)
        }
        assertEquals(3, count)
        assertEquals(listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9)), sequence3)

        // Cross-year 4 months: November 2026 -> February 2027
        count = 0
        curr = YearMonth.of(2026, 11)
        val endCross = YearMonth.of(2027, 2)
        val sequenceCross = mutableListOf<YearMonth>()
        while (!curr.isAfter(endCross)) {
            sequenceCross.add(curr)
            count++
            curr = curr.plusMonths(1)
        }
        assertEquals(4, count)
        assertEquals(
            listOf(
                YearMonth.of(2026, 11),
                YearMonth.of(2026, 12),
                YearMonth.of(2027, 1),
                YearMonth.of(2027, 2)
            ),
            sequenceCross
        )
    }

    @Test
    fun `test generatePdf with includeTimeDistribution enabled and disabled`() {
        val aug2026 = YearMonth.of(2026, 8)
        
        // Enabled (default)
        val optionsEnabled = TimeGoalsPdfOptions(includeTimeDistribution = true)
        val fileEnabled = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = optionsEnabled
        )
        assertNotNull(fileEnabled)
        assertTrue(fileEnabled.exists())

        // Disabled
        val optionsDisabled = TimeGoalsPdfOptions(includeTimeDistribution = false)
        val fileDisabled = TimeGoalsPdfExporter.generatePdf(
            context = context,
            yearMonth = aug2026,
            goals = sampleGoals,
            records = sampleRecords,
            options = optionsDisabled
        )
        assertNotNull(fileDisabled)
        assertTrue(fileDisabled.exists())
    }
}
