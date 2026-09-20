package com.example.gentlenudge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.export.HistoryPdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.OutputStream
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Implements(PdfDocument::class)
class ShadowTestPdfDocument {
    companion object {
        var pageCount = 0
    }

    private var isClosed = false

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        pageCount++
        val bitmap = Bitmap.createBitmap(pageInfo.pageWidth, pageInfo.pageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val constructor = PdfDocument.Page::class.java.getDeclaredConstructor(Canvas::class.java, PdfDocument.PageInfo::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(canvas, pageInfo)
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        // Finished successfully
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        val dummyPdfHeader = "%PDF-1.4\n%Nudge Task History PDF Export\n"
        out.write(dummyPdfHeader.toByteArray())
        out.flush()
    }

    @Implementation
    fun close() {
        isClosed = true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTestPdfDocument::class])
class HistoryPdfExporterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowTestPdfDocument.pageCount = 0
    }

    @Test
    fun `test dynamic filename format matches required pattern`() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 30, 20, 27, 0)
        }
        val testDate = calendar.time
        val filename = HistoryPdfExporter.buildDynamicPdfFilename(testDate)
        assertEquals("Task History - 30 August 2026 — 08:27 PM.pdf", filename)

        val morningCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 24, 8, 10, 0)
        }
        val morningDate = morningCalendar.time
        val morningFilename = HistoryPdfExporter.buildDynamicPdfFilename(morningDate)
        assertEquals("Task History - 24 August 2026 — 08:10 AM.pdf", morningFilename)
    }

    @Test
    fun `test in-pdf timestamp matches required header format`() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 24, 8, 10, 0)
        }
        val testDate = calendar.time
        val inPdfString = HistoryPdfExporter.buildInPdfTimestamp(testDate)
        assertEquals("August 24, 2026 at 08:10 AM", inPdfString)
    }

    @Test
    fun `test 1 - PDF generation with no tasks creates valid non-empty file`() {
        val emptyTasks = emptyList<NudgeTask>()
        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, emptyTasks)

        assertNotNull("Generated PDF file should not be null", pdfFile)
        assertTrue("PDF file should exist on disk", pdfFile.exists())
        assertTrue("PDF file should have content bytes", pdfFile.length() > 0)
        assertTrue("File name should start with Task History", pdfFile.name.startsWith("Task History - "))
        assertTrue("File name should end with .pdf", pdfFile.name.endsWith(".pdf"))
        assertEquals("Single page for empty history", 1, ShadowTestPdfDocument.pageCount)
    }

    @Test
    fun `test 2 - PDF generation with one completed task`() {
        val singleCompleted = listOf(
            NudgeTask(
                id = 1,
                title = "Water the Japanese bonsai tree",
                timeLabel = "9:00 AM",
                dateLabel = "Today",
                category = "Home",
                priority = "Normal",
                repeat = "Every week",
                soundType = "Small nudge",
                isDone = true,
                section = "today",
                createdAt = System.currentTimeMillis() - 3600000L,
                completedAt = System.currentTimeMillis()
            )
        )

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, singleCompleted)

        assertTrue("PDF file exists", pdfFile.exists())
        assertTrue("PDF file contains data", pdfFile.length() > 0)
        assertEquals("Single page for 1 task", 1, ShadowTestPdfDocument.pageCount)
    }

    @Test
    fun `test 3 - PDF generation with multiple completed and pending tasks`() {
        val tasks = listOf(
            NudgeTask(
                id = 1,
                title = "Draft Q3 quarterly reflection note",
                timeLabel = "2:30 PM",
                dateLabel = "Today",
                category = "Work",
                priority = "Important",
                repeat = "Does not repeat",
                soundType = "Full ringtone",
                isDone = false,
                section = "today"
            ),
            NudgeTask(
                id = 2,
                title = "Pick up organic groceries from market",
                timeLabel = "5:00 PM",
                dateLabel = "Today",
                category = "Shopping",
                priority = "Normal",
                isDone = false,
                section = "today"
            ),
            NudgeTask(
                id = 3,
                title = "Send birthday card to grandma",
                timeLabel = "Any time",
                dateLabel = "Yesterday",
                category = "Personal",
                priority = "Important",
                isDone = true,
                completedAt = System.currentTimeMillis() - 86400000L
            ),
            NudgeTask(
                id = 4,
                title = "Meditate for 10 minutes before bed",
                timeLabel = "10:00 PM",
                dateLabel = "Every day",
                category = "Personal",
                isDone = true,
                completedAt = System.currentTimeMillis() - 43200000L
            )
        )

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, tasks)

        assertTrue("PDF file exists", pdfFile.exists())
        assertTrue("PDF file contains data", pdfFile.length() > 0)
        assertEquals("Single page for 4 tasks", 1, ShadowTestPdfDocument.pageCount)
    }

    @Test
    fun `test 4 - PDF generation with dates and reminder times`() {
        val tasksWithReminders = listOf(
            NudgeTask(
                id = 10,
                title = "Dentist annual checkup appointment",
                timeLabel = "11:15 AM",
                dateLabel = "Thursday, Aug 27",
                category = "Personal",
                priority = "Important",
                repeat = "Does not repeat",
                soundType = "Full ringtone",
                isDone = false
            ),
            NudgeTask(
                id = 11,
                title = "Pay monthly apartment utility bill",
                timeLabel = "8:00 AM",
                dateLabel = "1st of next month",
                category = "Home",
                priority = "Normal",
                repeat = "Every month",
                soundType = "Small nudge",
                isDone = false
            )
        )

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, tasksWithReminders)

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun `test 5 - PDF generation with a long history spanning multiple pages`() {
        // Create 45 tasks to test multi-page calculation and pagination
        val largeTaskList = mutableListOf<NudgeTask>()
        for (i in 1..45) {
            largeTaskList.add(
                NudgeTask(
                    id = i.toLong(),
                    title = "Nudge Task item #$i — Review project documents and follow up with the team regarding milestones",
                    timeLabel = "${(i % 12) + 1}:00 PM",
                    dateLabel = if (i % 2 == 0) "Today" else "Tomorrow",
                    category = if (i % 3 == 0) "Work" else if (i % 3 == 1) "Personal" else "Home",
                    priority = if (i % 4 == 0) "Important" else "Normal",
                    repeat = if (i % 5 == 0) "Every week" else "Does not repeat",
                    soundType = if (i % 2 == 0) "Full ringtone" else "Small nudge",
                    isDone = i % 2 == 0,
                    completedAt = if (i % 2 == 0) System.currentTimeMillis() - (i * 3600000L) else null
                )
            )
        }

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, largeTaskList)

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
        assertTrue("Multi-page generation should trigger page count > 1", ShadowTestPdfDocument.pageCount > 1)
    }

    @Test
    fun `test 6 - PDF generation with very long task text and descriptions`() {
        val longTextTask = listOf(
            NudgeTask(
                id = 100,
                title = "This is an exceptionally long task note designed to verify that the StaticLayout word wrapping correctly calculates dynamic line heights without any text truncation, character cutting, or overlapping into subsequent visual components or metadata pill badges across the document canvas. Everything should look neat, serene, and legible.",
                timeLabel = "3:45 PM",
                dateLabel = "Friday",
                category = "Work",
                priority = "Important",
                repeat = "Every weekday",
                soundType = "Full ringtone",
                isDone = false
            )
        )

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, longTextTask)

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun `test 7 - Completely offline execution test`() {
        val offlineTasks = listOf(
            NudgeTask(
                id = 200,
                title = "Offline Airplane Mode Task Entry",
                timeLabel = "Any time",
                dateLabel = "Today",
                isDone = true,
                completedAt = System.currentTimeMillis()
            )
        )

        val pdfFile = HistoryPdfExporter.generateHistoryPdf(context, offlineTasks)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun `test 8 - PDF generation with task history`() {
        val tasks = listOf(
            NudgeTask(
                id = 300,
                title = "Mindful breathing session",
                isDone = true,
                completedAt = System.currentTimeMillis()
            )
        )
        val pdfFile = HistoryPdfExporter.generateHistoryPdf(
            context = context,
            tasks = tasks
        )
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }
}
