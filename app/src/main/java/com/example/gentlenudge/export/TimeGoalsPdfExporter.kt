package com.example.gentlenudge.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.gentlenudge.R
import com.example.gentlenudge.data.model.DayCompletionState
import com.example.gentlenudge.data.model.MonthlyGoalProgress
import com.example.gentlenudge.data.model.MonthlyOverallProgress
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class TimeGoalsPdfOptions(
    val includeSummary: Boolean = true,
    val includeGoalsList: Boolean = true,
    val includeCalendarGrid: Boolean = true,
    val includeDailyRecords: Boolean = true,
    val includeTimeDistribution: Boolean = true,
    val includeInsights: Boolean = true
)

data class MonthReportData(
    val yearMonth: YearMonth,
    val goals: List<TimeGoal>,
    val records: List<TimeGoalRecord>
)

/**
 * Generates 100% offline, publication-grade PDF documents for "The Life Within the Hours"
 * time investment reports using native Android PdfDocument APIs.
 */
object TimeGoalsPdfExporter {

    // Standard A4 dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 42f
    private const val MARGIN_RIGHT = 42f
    private const val MARGIN_TOP = 42f
    private const val MARGIN_BOTTOM = 45f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    // Palette matching Nudge design system
    private const val COLOR_PRIMARY = 0xFF3D70FF.toInt() // Nudge Signature Blue
    private const val COLOR_ON_PRIMARY = 0xFFFFFFFF.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFF1C1A17.toInt() // Deep Charcoal
    private const val COLOR_TEXT_SECONDARY = 0xFF5F5E5A.toInt() // Warm Slate
    private const val COLOR_TEXT_MUTED = 0xFF8C8881.toInt()
    private const val COLOR_BORDER = 0xFFE5E2DC.toInt()
    private const val COLOR_CARD_BG = 0xFFFAF9F6.toInt()
    private const val COLOR_HEADER_BG = 0xFFF5F7FF.toInt()

    // Status colors
    private const val COLOR_COMPLETED_BG = 0xFFE8F5E9.toInt()
    private const val COLOR_COMPLETED_TEXT = 0xFF2E7D32.toInt()
    private const val COLOR_PARTIAL_BG = 0xFFFFF3E0.toInt()
    private const val COLOR_PARTIAL_TEXT = 0xFFE65100.toInt()
    private const val COLOR_MISSED_BG = 0xFFFBE9E7.toInt()
    private const val COLOR_MISSED_TEXT = 0xFFD84315.toInt()
    private const val COLOR_FUTURE_BG = 0xFFF3F1EC.toInt()
    private const val COLOR_FUTURE_TEXT = 0xFF757575.toInt()

    fun buildFilename(exportDate: Date = Date()): String {
        val datePart = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(exportDate)
        val timePart = SimpleDateFormat("hh:mm a", Locale.US).format(exportDate)
        return "Time Investment Report - $datePart — $timePart.pdf"
    }

    fun buildMultiMonthFilename(
        startMonth: YearMonth,
        endMonth: YearMonth,
        exportDate: Date = Date()
    ): String {
        val startStr = TimeGoalCalculations.formatYearMonth(startMonth)
        val endStr = TimeGoalCalculations.formatYearMonth(endMonth)
        val datePart = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(exportDate)
        val rangePart = if (startMonth == endMonth) startStr else "$startStr to $endStr"
        return "Time Investment Report - $rangePart — $datePart.pdf"
    }

    /**
     * Generates a complete PDF report for the selected month.
     */
    fun generatePdf(
        context: Context,
        yearMonth: YearMonth,
        goals: List<TimeGoal>,
        records: List<TimeGoalRecord>,
        options: TimeGoalsPdfOptions = TimeGoalsPdfOptions(),
        exportDate: Date = Date()
    ): File {
        val exportDir = File(context.filesDir, "hours_exports").apply { mkdirs() }
        val filename = buildFilename(exportDate)
        val pdfFile = File(exportDir, filename)

        val overallProgress = TimeGoalCalculations.calculateMonthlyOverallProgress(
            goals = goals,
            yearMonth = yearMonth,
            allRecords = records,
            today = LocalDate.now()
        )

        val monthTitle = TimeGoalCalculations.formatYearMonth(yearMonth)
        val exportedAtStr = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm a", Locale.US).format(exportDate)

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var currentY = MARGIN_TOP

        fun startNextPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = MARGIN_TOP
        }

        // 1. Cover Header
        currentY = drawDocumentHeader(context, canvas, monthTitle, exportedAtStr)

        // 2. Summary Card
        if (options.includeSummary) {
            currentY = drawSummaryCard(canvas, currentY, overallProgress)
        }

        // 3. Visual Monthly Calendar Grid (Dedicated Section)
        if (options.includeCalendarGrid) {
            val calendarNeededHeight = calculateCalendarHeight(yearMonth)
            if (currentY + calendarNeededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNextPage()
            }
            currentY = drawVisualMonthlyCalendarGrid(canvas, currentY, yearMonth, overallProgress, records)
        }

        // 4. Goals Breakdown (All Time Goals & Daily Targets)
        if (options.includeGoalsList && overallProgress.goalProgressList.isNotEmpty()) {
            val sectionHeaderHeight = 30f
            if (currentY + sectionHeaderHeight + 75f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNextPage()
            }
            currentY = drawGoalsSectionHeader(canvas, currentY)

            for (goalProgress in overallProgress.goalProgressList) {
                val neededHeight = 72f
                if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawGoalSection(canvas, currentY, goalProgress)
            }
        }

        // 5. Time Distribution & Streak / Day Statistics
        if (options.includeTimeDistribution && overallProgress.goalProgressList.isNotEmpty()) {
            val neededHeight = calculateTimeDistributionHeight(overallProgress.goalProgressList.size)
            if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNextPage()
            }
            currentY = drawTimeDistributionSection(
                canvas = canvas,
                startY = currentY,
                yearMonth = yearMonth,
                overallProgress = overallProgress,
                records = records
            )
        }

        // 6. Monthly Review & Insights
        if (options.includeInsights && overallProgress.goalProgressList.isNotEmpty()) {
            if (currentY + 160f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNextPage()
            }
            currentY = drawInsightsSection(canvas, currentY, overallProgress)
        }

        // Finish last page
        pdfDocument.finishPage(currentPage)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Generates a combined multi-month PDF report containing all months in monthsData sequentially in ONE document.
     * Every month starts on a brand new page and uses the existing monthly visual structure.
     */
    fun generateMultiMonthPdf(
        context: Context,
        monthsData: List<MonthReportData>,
        options: TimeGoalsPdfOptions = TimeGoalsPdfOptions(),
        exportDate: Date = Date()
    ): File {
        require(monthsData.isNotEmpty()) { "monthsData cannot be empty" }
        val startMonth = monthsData.first().yearMonth
        val endMonth = monthsData.last().yearMonth
        require(!startMonth.isAfter(endMonth)) { "Start month cannot be after end month" }
        require(monthsData.size <= 12) { "Range cannot exceed 12 months" }
        for (i in 0 until monthsData.size - 1) {
            require(monthsData[i].yearMonth.plusMonths(1) == monthsData[i + 1].yearMonth) {
                "Months in monthsData must be continuous sequential months"
            }
        }

        val exportDir = File(context.filesDir, "hours_exports").apply { mkdirs() }
        val filename = buildMultiMonthFilename(startMonth, endMonth, exportDate)
        val pdfFile = File(exportDir, filename)

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var currentY = MARGIN_TOP

        fun startNextPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = MARGIN_TOP
        }

        val exportedAtStr = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm a", Locale.US).format(exportDate)

        for ((index, monthData) in monthsData.withIndex()) {
            // CRITICAL: Every month MUST start on a brand new page!
            if (index > 0) {
                startNextPage()
            }

            val ym = monthData.yearMonth
            val goals = monthData.goals
            val records = monthData.records

            val overallProgress = TimeGoalCalculations.calculateMonthlyOverallProgress(
                goals = goals,
                yearMonth = ym,
                allRecords = records,
                today = LocalDate.now()
            )

            val monthTitle = TimeGoalCalculations.formatYearMonth(ym)

            // 1. Cover Header for this month
            currentY = drawDocumentHeader(context, canvas, monthTitle, exportedAtStr)

            // 2. Summary Card
            if (options.includeSummary) {
                currentY = drawSummaryCard(canvas, currentY, overallProgress)
            }

            // 3. Visual Monthly Calendar Grid (Dedicated Section)
            if (options.includeCalendarGrid) {
                val calendarNeededHeight = calculateCalendarHeight(ym)
                if (currentY + calendarNeededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawVisualMonthlyCalendarGrid(canvas, currentY, ym, overallProgress, records)
            }

            // 4. Goals Breakdown (All Time Goals & Daily Targets)
            if (options.includeGoalsList && overallProgress.goalProgressList.isNotEmpty()) {
                val sectionHeaderHeight = 30f
                if (currentY + sectionHeaderHeight + 75f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawGoalsSectionHeader(canvas, currentY)

                for (goalProgress in overallProgress.goalProgressList) {
                    val neededHeight = 72f
                    if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        startNextPage()
                    }
                    currentY = drawGoalSection(canvas, currentY, goalProgress)
                }
            }

            // 5. Time Distribution & Streak / Day Statistics
            if (options.includeTimeDistribution && overallProgress.goalProgressList.isNotEmpty()) {
                val neededHeight = calculateTimeDistributionHeight(overallProgress.goalProgressList.size)
                if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawTimeDistributionSection(
                    canvas = canvas,
                    startY = currentY,
                    yearMonth = ym,
                    overallProgress = overallProgress,
                    records = records
                )
            }

            // 6. Monthly Review & Insights
            if (options.includeInsights && overallProgress.goalProgressList.isNotEmpty()) {
                if (currentY + 160f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawInsightsSection(canvas, currentY, overallProgress)
            }
        }

        // Finish last page
        pdfDocument.finishPage(currentPage)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun drawDocumentHeader(
        context: Context,
        canvas: Canvas,
        monthTitle: String,
        exportedAt: String
    ): Float {
        val y = MARGIN_TOP
        val logoSize = 42f

        drawGentleNudgeLogo(context, canvas, MARGIN_LEFT, y, logoSize)

        val textStartX = MARGIN_LEFT + logoSize + 14f

        val brandPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.14f
            isAntiAlias = true
        }
        canvas.drawText("NUDGE", textStartX, y + 10f, brandPaint)

        val titlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 19f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Time Investment Report — $monthTitle", textStartX, y + 31f, titlePaint)

        val metaPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Generated: $exportedAt", textStartX, y + 48f, metaPaint)

        val dividerY = y + 60f
        val rulePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN_LEFT, dividerY, MARGIN_LEFT + CONTENT_WIDTH, dividerY, rulePaint)

        return dividerY + 16f
    }

    private fun drawGentleNudgeLogo(
        context: Context,
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float
    ) {
        var drawn = false
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_gentle_nudge_logo)
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(
                    size.toInt().coerceAtLeast(1),
                    size.toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val bmpCanvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size.toInt(), size.toInt())
                drawable.draw(bmpCanvas)
                canvas.drawBitmap(bitmap, left, top, null)
                drawn = true
            }
        } catch (_: Throwable) {
            drawn = false
        }

        if (!drawn) {
            drawGentleNudgeVectorLogo(canvas, left, top, size)
        }
    }

    /**
     * High-fidelity vector rendering of the authentic Gentle Nudge clock logo:
     * Sweeping crescent arrow, 12 tick marks, hour & minute hands, and center hub.
     */
    private fun drawGentleNudgeVectorLogo(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float
    ) {
        val centerX = left + size * 0.5f
        val centerY = top + size * 0.5f
        val radius = size * 0.40f

        val logoPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // 1. Draw outer crescent arrow
        val outerRadius = radius * 1.08f
        val innerRadius = radius * 0.88f
        val crescentPath = android.graphics.Path().apply {
            val outerRect = RectF(
                centerX - outerRadius,
                centerY - outerRadius,
                centerX + outerRadius,
                centerY + outerRadius
            )
            arcTo(outerRect, 35f, 238f, true)

            // Arrow head at top pointing right
            val arrowX = centerX - 2f * (size / 100f)
            val arrowY = centerY - outerRadius - 3f * (size / 100f)
            val tipX = centerX + 10f * (size / 100f)
            val tipY = centerY - outerRadius + 2f * (size / 100f)
            val baseBottomX = centerX - 2f * (size / 100f)
            val baseBottomY = centerY - outerRadius + 8f * (size / 100f)

            lineTo(arrowX, arrowY)
            lineTo(tipX, tipY)
            lineTo(baseBottomX, baseBottomY)

            val innerRect = RectF(
                centerX - innerRadius,
                centerY - innerRadius,
                centerX + innerRadius,
                centerY + innerRadius
            )
            arcTo(innerRect, 270f, -235f, false)
            close()
        }
        canvas.drawPath(crescentPath, logoPaint)

        // 2. 12 Hour tick marks
        for (i in 0 until 12) {
            val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
            val isCardinal = (i % 3 == 0)
            val tickLen = if (isCardinal) radius * 0.20f else radius * 0.12f
            val outerX = centerX + (radius * cos(angleRad)).toFloat()
            val outerY = centerY + (radius * sin(angleRad)).toFloat()
            val innerX = centerX + ((radius - tickLen) * cos(angleRad)).toFloat()
            val innerY = centerY + ((radius - tickLen) * sin(angleRad)).toFloat()

            strokePaint.strokeWidth = if (isCardinal) size * 0.045f else size * 0.03f
            canvas.drawLine(innerX, innerY, outerX, outerY, strokePaint)
        }

        // 3. Center pivot hub
        canvas.drawCircle(centerX, centerY, size * 0.045f, logoPaint)

        // 4. Hour Hand (pointing toward ~10:15 / top-left)
        val hourAngleRad = (-145.0) * (PI / 180.0)
        val hourLen = radius * 0.48f
        strokePaint.strokeWidth = size * 0.055f
        val hourX = centerX + (hourLen * cos(hourAngleRad)).toFloat()
        val hourY = centerY + (hourLen * sin(hourAngleRad)).toFloat()
        canvas.drawLine(centerX, centerY, hourX, hourY, strokePaint)

        // 5. Minute Hand (pointing toward ~3:00 / right)
        val minAngleRad = (5.0) * (PI / 180.0)
        val minLen = radius * 0.68f
        strokePaint.strokeWidth = size * 0.045f
        val minX = centerX + (minLen * cos(minAngleRad)).toFloat()
        val minY = centerY + (minLen * sin(minAngleRad)).toFloat()
        canvas.drawLine(centerX, centerY, minX, minY, strokePaint)

        // 6. Thin Hand (pointing toward ~1:30 / top-right)
        val secAngleRad = (-45.0) * (PI / 180.0)
        val secLen = radius * 0.72f
        strokePaint.strokeWidth = size * 0.025f
        val secX = centerX + (secLen * cos(secAngleRad)).toFloat()
        val secY = centerY + (secLen * sin(secAngleRad)).toFloat()
        canvas.drawLine(centerX, centerY, secX, secY, strokePaint)
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        startY: Float,
        progress: MonthlyOverallProgress
    ): Float {
        val cardHeight = 64f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        val bgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        val colWidth = CONTENT_WIDTH / 4f
        val stats = listOf(
            "TOTAL PLANNED" to TimeGoalCalculations.formatHoursTotal(progress.totalPlannedMinutes),
            "TOTAL INVESTED" to TimeGoalCalculations.formatHoursTotal(progress.totalRecordedMinutes),
            "OVERALL PROGRESS" to "${progress.overallProgressPercent}%",
            "ACTIVE PURSUITS" to "${progress.goalProgressList.size}"
        )

        val labelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            isAntiAlias = true
        }

        val valPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        for (i in stats.indices) {
            val colX = MARGIN_LEFT + (i * colWidth) + 14f
            val (lbl, v) = stats[i]
            canvas.drawText(lbl, colX, startY + 20f, labelPaint)
            canvas.drawText(v, colX, startY + 42f, valPaint)
        }

        // Progress bar inside summary card
        val barY = startY + 52f
        val barLeft = MARGIN_LEFT + 14f
        val barWidth = CONTENT_WIDTH - 28f
        val barHeight = 4f

        val barBgPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(barLeft, barY, barLeft + barWidth, barY + barHeight), 2f, 2f, barBgPaint)

        val fillWidth = (barWidth * (progress.overallProgressPercent / 100f)).coerceIn(0f, barWidth)
        if (fillWidth > 0f) {
            val barFillPaint = Paint().apply {
                color = COLOR_PRIMARY
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(barLeft, barY, barLeft + fillWidth, barY + barHeight), 2f, 2f, barFillPaint)
        }

        return startY + cardHeight + 16f
    }

    private fun calculateCalendarHeight(yearMonth: YearMonth): Float {
        val firstDay = yearMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Mon, 7 = Sun
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalWeeks = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7
        // Header (38f) + Weekdays (20f) + Rows (totalWeeks * 36f) + Spacing (8f) + Legend (22f) + Padding (20f)
        return 108f + (totalWeeks * 36f)
    }

    private fun drawVisualMonthlyCalendarGrid(
        canvas: Canvas,
        startY: Float,
        yearMonth: YearMonth,
        overallProgress: MonthlyOverallProgress,
        records: List<TimeGoalRecord>
    ): Float {
        val firstDay = yearMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Mon, 7 = Sun
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalWeeks = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7

        val cellHeight = 36f
        val gridHeight = totalWeeks * cellHeight
        val cardHeight = 108f + gridHeight

        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        // Card background
        val bgPaint = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        // Card border
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Section Title & Info
        var curY = startY + 16f

        val sectionLabelPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        canvas.drawText("VISUAL MONTHLY CALENDAR", MARGIN_LEFT + 14f, curY, sectionLabelPaint)

        val monthNameStr = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        val monthTitlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(monthNameStr, MARGIN_LEFT + 14f, curY + 16f, monthTitlePaint)

        val totalMinutesRecorded = overallProgress.totalRecordedMinutes
        val activeDaysCount = (1..daysInMonth).count { day ->
            val dateStr = TimeGoalCalculations.formatLocalDate(yearMonth.atDay(day))
            records.any { it.date == dateStr && it.actualMinutes > 0 }
        }
        val metaPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val summaryMeta = "$activeDaysCount active days logged  •  ${TimeGoalCalculations.formatHoursTotal(totalMinutesRecorded)} invested"
        canvas.drawText(summaryMeta, MARGIN_LEFT + CONTENT_WIDTH - 14f, curY + 16f, metaPaint)

        curY += 26f

        // Divider
        val rulePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN_LEFT + 14f, curY, MARGIN_LEFT + CONTENT_WIDTH - 14f, curY, rulePaint)
        curY += 10f

        // Weekday Headers
        val innerPadding = 12f
        val innerWidth = CONTENT_WIDTH - (innerPadding * 2)
        val colWidth = innerWidth / 7f
        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        val weekdayHeaderRect = RectF(MARGIN_LEFT + innerPadding, curY, MARGIN_LEFT + CONTENT_WIDTH - innerPadding, curY + 18f)
        val weekdayBgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(weekdayHeaderRect, 4f, 4f, weekdayBgPaint)

        val weekdayPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        for (i in 0 until 7) {
            val cx = MARGIN_LEFT + innerPadding + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(dayNames[i], cx, curY + 12f, weekdayPaint)
        }
        curY += 22f

        // Calendar Day Cells
        val today = LocalDate.now()
        val recordsByDate = records.groupBy { it.date }

        val cellBorderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }

        val dateNumPaint = Paint().apply {
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val symbolPaint = Paint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val timeSubPaint = Paint().apply {
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val cellFillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var col = firstDayOfWeek - 1
        var rowY = curY

        // Draw empty leading cells if month doesn't start on Monday
        for (emptyCol in 0 until col) {
            val cellLeft = MARGIN_LEFT + innerPadding + (emptyCol * colWidth)
            val cellRect = RectF(cellLeft + 1f, rowY + 1f, cellLeft + colWidth - 1f, rowY + cellHeight - 1f)
            cellFillPaint.color = 0xFFF7F6F3.toInt()
            canvas.drawRoundRect(cellRect, 3f, 3f, cellFillPaint)
        }

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = TimeGoalCalculations.formatLocalDate(date)
            val dayRecords = recordsByDate[dateStr] ?: emptyList()
            val totalMins = dayRecords.sumOf { it.actualMinutes }

            // Target for day across all active goals
            val targetForDay = overallProgress.goalProgressList.sumOf { gp ->
                gp.dailyProgressList.getOrNull(day - 1)?.targetMinutes ?: 0
            }

            val state: DayCompletionState = when {
                date.isAfter(today) -> DayCompletionState.FUTURE
                targetForDay > 0 -> {
                    if (totalMins >= targetForDay) DayCompletionState.COMPLETED
                    else if (totalMins > 0) DayCompletionState.PARTIAL
                    else DayCompletionState.MISSED
                }
                totalMins > 0 -> DayCompletionState.COMPLETED
                else -> DayCompletionState.MISSED
            }

            val (bgColor, textColor, symbol) = when (state) {
                DayCompletionState.COMPLETED -> Triple(COLOR_COMPLETED_BG, COLOR_COMPLETED_TEXT, "✓")
                DayCompletionState.PARTIAL -> Triple(COLOR_PARTIAL_BG, COLOR_PARTIAL_TEXT, "◐")
                DayCompletionState.MISSED -> Triple(COLOR_MISSED_BG, COLOR_MISSED_TEXT, "—")
                DayCompletionState.FUTURE -> Triple(COLOR_FUTURE_BG, COLOR_FUTURE_TEXT, "○")
                DayCompletionState.BEFORE_START -> Triple(COLOR_CARD_BG, COLOR_TEXT_MUTED, "·")
            }

            val cellLeft = MARGIN_LEFT + innerPadding + (col * colWidth)
            val cellRect = RectF(cellLeft + 1.5f, rowY + 1.5f, cellLeft + colWidth - 1.5f, rowY + cellHeight - 1.5f)

            cellFillPaint.color = bgColor
            canvas.drawRoundRect(cellRect, 4f, 4f, cellFillPaint)
            canvas.drawRoundRect(cellRect, 4f, 4f, cellBorderPaint)

            // Day number (top left)
            dateNumPaint.color = if (state == DayCompletionState.FUTURE) COLOR_FUTURE_TEXT else COLOR_TEXT_PRIMARY
            canvas.drawText("$day", cellLeft + 5f, rowY + 12f, dateNumPaint)

            // Status symbol (top right)
            symbolPaint.color = textColor
            canvas.drawText(symbol, cellLeft + colWidth - 5f, rowY + 12f, symbolPaint)

            // Bottom text: logged time (or subtle marker)
            timeSubPaint.color = textColor
            val subText = if (totalMins > 0) {
                TimeGoalCalculations.formatMinutes(totalMins)
            } else if (state == DayCompletionState.FUTURE) {
                "·"
            } else {
                "0m"
            }
            canvas.drawText(subText, cellLeft + 5f, rowY + 28f, timeSubPaint)

            col++
            if (col >= 7) {
                col = 0
                rowY += cellHeight
            }
        }

        // Draw trailing empty cells if month doesn't end on Sunday
        if (col != 0) {
            for (emptyCol in col until 7) {
                val cellLeft = MARGIN_LEFT + innerPadding + (emptyCol * colWidth)
                val cellRect = RectF(cellLeft + 1f, rowY + 1f, cellLeft + colWidth - 1f, rowY + cellHeight - 1f)
                cellFillPaint.color = 0xFFF7F6F3.toInt()
                canvas.drawRoundRect(cellRect, 3f, 3f, cellFillPaint)
            }
            rowY += cellHeight
        }

        // Calendar Legend at bottom
        val legendY = rowY + 14f
        val legendItems = listOf(
            Triple("✓", "Completed", COLOR_COMPLETED_TEXT),
            Triple("◐", "Partial", COLOR_PARTIAL_TEXT),
            Triple("—", "No Log / Missed", COLOR_MISSED_TEXT),
            Triple("○", "Future / Remaining", COLOR_FUTURE_TEXT)
        )
        val legendColWidth = innerWidth / 4f
        val legendSymbolPaint = Paint().apply {
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val legendTextPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        for (i in legendItems.indices) {
            val (sym, lbl, clr) = legendItems[i]
            val lx = MARGIN_LEFT + innerPadding + (i * legendColWidth)
            legendSymbolPaint.color = clr
            canvas.drawText(sym, lx, legendY, legendSymbolPaint)
            canvas.drawText(lbl, lx + 11f, legendY, legendTextPaint)
        }

        return startY + cardHeight + 16f
    }

    private fun drawGoalsSectionHeader(canvas: Canvas, startY: Float): Float {
        val labelPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        canvas.drawText("ALL TIME GOALS & DAILY TARGETS", MARGIN_LEFT, startY + 12f, labelPaint)

        val rulePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN_LEFT, startY + 20f, MARGIN_LEFT + CONTENT_WIDTH, startY + 20f, rulePaint)
        return startY + 28f
    }

    private fun drawGoalSection(
        canvas: Canvas,
        startY: Float,
        goalProgress: MonthlyGoalProgress
    ): Float {
        val goal = goalProgress.goal
        val goalColor = try {
            android.graphics.Color.parseColor(goal.colorHex)
        } catch (_: Exception) {
            COLOR_PRIMARY
        }

        val cardHeight = 64f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        val headerBgPaint = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, headerBgPaint)

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        // Accent indicator left bar
        val accentPaint = Paint().apply {
            color = goalColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, startY, MARGIN_LEFT + 4f, startY + cardHeight), 2f, 2f, accentPaint)

        val titlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(goal.name, MARGIN_LEFT + 12f, startY + 18f, titlePaint)

        val metaPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val targetDaily = TimeGoalCalculations.formatMinutes(goal.dailyTargetMinutes)
        val plannedTotal = TimeGoalCalculations.formatHoursTotal(goalProgress.plannedMinutes)
        val recordedTotal = TimeGoalCalculations.formatHoursTotal(goalProgress.recordedMinutes)
        val statsStr = "Target: $targetDaily/day  •  Planned: $plannedTotal  •  Invested: $recordedTotal (${goalProgress.progressPercent}%)"
        canvas.drawText(statsStr, MARGIN_LEFT + CONTENT_WIDTH - 12f, startY + 18f, metaPaint)

        // Progress bar inside goal card
        val barY = startY + 28f
        val barLeft = MARGIN_LEFT + 12f
        val barWidth = CONTENT_WIDTH - 24f
        val barHeight = 4f

        val barBgPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(barLeft, barY, barLeft + barWidth, barY + barHeight), 2f, 2f, barBgPaint)

        val fillWidth = (barWidth * (goalProgress.progressPercent / 100f)).coerceIn(0f, barWidth)
        if (fillWidth > 0f) {
            val barFillPaint = Paint().apply {
                color = goalColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(barLeft, barY, barLeft + fillWidth, barY + barHeight), 2f, 2f, barFillPaint)
        }

        // Completion status summary pill line
        val summaryLine = "Completed: ${goalProgress.completedDaysCount}d (✓)  •  Partial: ${goalProgress.partialDaysCount}d (◐)  •  Missed: ${goalProgress.missedDaysCount}d (—)  •  Remaining: ${goalProgress.remainingDaysCount}d (○)"
        val summaryPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(summaryLine, MARGIN_LEFT + 12f, startY + 48f, summaryPaint)

        return startY + cardHeight + 10f
    }

    data class MonthlyStreakStats(
        val currentStreak: Int,
        val longestStreak: Int,
        val activeDays: Int,
        val missedDays: Int
    )

    private fun calculateMonthlyStreakStats(
        yearMonth: YearMonth,
        records: List<TimeGoalRecord>,
        today: LocalDate = LocalDate.now()
    ): MonthlyStreakStats {
        val daysInMonth = yearMonth.lengthOfMonth()
        val lastEvaluatedDay = when {
            yearMonth.isBefore(YearMonth.from(today)) -> daysInMonth
            yearMonth == YearMonth.from(today) -> minOf(today.dayOfMonth, daysInMonth)
            else -> 0
        }

        val isDayActive = BooleanArray(daysInMonth + 1)
        for (day in 1..daysInMonth) {
            val dateStr = TimeGoalCalculations.formatLocalDate(yearMonth.atDay(day))
            isDayActive[day] = records.any { it.date == dateStr && it.actualMinutes > 0 }
        }

        val activeDays = (1..daysInMonth).count { isDayActive[it] }

        val missedDays = if (lastEvaluatedDay > 0) {
            (1..lastEvaluatedDay).count { !isDayActive[it] }
        } else {
            0
        }

        var longestStreak = 0
        var run = 0
        for (day in 1..daysInMonth) {
            if (isDayActive[day]) {
                run++
                if (run > longestStreak) longestStreak = run
            } else {
                run = 0
            }
        }

        var currentStreak = 0
        if (lastEvaluatedDay > 0) {
            val startDay = if (yearMonth == YearMonth.from(today) && !isDayActive[lastEvaluatedDay] && lastEvaluatedDay > 1 && isDayActive[lastEvaluatedDay - 1]) {
                lastEvaluatedDay - 1
            } else {
                lastEvaluatedDay
            }
            for (day in startDay downTo 1) {
                if (isDayActive[day]) currentStreak++ else break
            }
        }

        return MonthlyStreakStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            activeDays = activeDays,
            missedDays = missedDays
        )
    }

    private fun calculateTimeDistributionHeight(goalsCount: Int): Float {
        val tableHeight = (goalsCount * 16.5f) + 24f
        val donutCenterYOffset = 34f + maxOf(43f, tableHeight / 2f)
        val upperEndYOffset = maxOf(donutCenterYOffset + 43f, 34f + tableHeight)
        val midDividerYOffset = upperEndYOffset + 12f
        val statsStartYOffset = midDividerYOffset + 12f
        val cardHeight = statsStartYOffset + 38f
        return cardHeight + 16f
    }

    private fun drawTimeDistributionSection(
        canvas: Canvas,
        startY: Float,
        yearMonth: YearMonth,
        overallProgress: MonthlyOverallProgress,
        records: List<TimeGoalRecord>
    ): Float {
        val sortedGoals = overallProgress.goalProgressList.sortedByDescending { it.recordedMinutes }
        val totalRecordedMinutes = overallProgress.totalRecordedMinutes

        // Layout measurements
        val tableHeight = (sortedGoals.size * 16.5f) + 24f
        val donutCenterYOffset = 34f + maxOf(43f, tableHeight / 2f)
        val donutCenterX = MARGIN_LEFT + 75f
        val donutCenterY = startY + donutCenterYOffset

        val upperEndYOffset = maxOf(donutCenterYOffset + 43f, 34f + tableHeight)
        val midDividerY = startY + upperEndYOffset + 12f
        val statsStartY = midDividerY + 12f
        val cardHeight = statsStartY + 38f - startY

        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        // Card background & border
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Section Title: "TIME DISTRIBUTION"
        val headingPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        canvas.drawText("TIME DISTRIBUTION", MARGIN_LEFT + 14f, startY + 18f, headingPaint)

        // 1. Donut Chart (Left)
        val midR = 34f
        val strokeW = 16f
        val oval = RectF(
            donutCenterX - midR,
            donutCenterY - midR,
            donutCenterX + midR,
            donutCenterY + midR
        )

        val arcPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            isAntiAlias = true
        }

        if (totalRecordedMinutes > 0) {
            var currentAngle = -90f
            for (gp in sortedGoals) {
                if (gp.recordedMinutes <= 0) continue
                val sweepAngle = (gp.recordedMinutes.toFloat() / totalRecordedMinutes.toFloat()) * 360f
                val goalColor = try {
                    android.graphics.Color.parseColor(gp.goal.colorHex)
                } catch (_: Exception) {
                    COLOR_PRIMARY
                }
                arcPaint.color = goalColor
                canvas.drawArc(oval, currentAngle, sweepAngle, false, arcPaint)
                currentAngle += sweepAngle
            }
        } else {
            arcPaint.color = COLOR_BORDER
            canvas.drawArc(oval, -90f, 360f, false, arcPaint)
        }

        // Donut Center Text: Total hours & label
        val centerHoursPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val centerLabelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val totalHoursStr = TimeGoalCalculations.formatHoursTotal(totalRecordedMinutes)
        canvas.drawText(totalHoursStr, donutCenterX, donutCenterY - 1f, centerHoursPaint)
        canvas.drawText("Total Invested", donutCenterX, donutCenterY + 11f, centerLabelPaint)

        // 2. Pursuit Breakdown List (Right)
        val legendStartX = MARGIN_LEFT + 155f
        val pctColX = MARGIN_LEFT + CONTENT_WIDTH - 14f
        val hoursColX = pctColX - 52f

        val namePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val hoursPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val pctPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val indicatorPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var rowY = startY + 36f
        val rowHeight = 16.5f

        for (gp in sortedGoals) {
            val goalColor = try {
                android.graphics.Color.parseColor(gp.goal.colorHex)
            } catch (_: Exception) {
                COLOR_PRIMARY
            }
            indicatorPaint.color = goalColor
            canvas.drawRoundRect(
                RectF(legendStartX, rowY - 6.5f, legendStartX + 7.5f, rowY + 1f),
                1.5f,
                1.5f,
                indicatorPaint
            )

            canvas.drawText(gp.goal.name, legendStartX + 14f, rowY, namePaint)

            val hStr = TimeGoalCalculations.formatHoursTotal(gp.recordedMinutes)
            canvas.drawText(hStr, hoursColX, rowY, hoursPaint)

            val pct = if (totalRecordedMinutes > 0) {
                (gp.recordedMinutes.toDouble() / totalRecordedMinutes.toDouble()) * 100.0
            } else {
                0.0
            }
            val pctStr = String.format(Locale.US, "%.1f%%", pct)
            canvas.drawText(pctStr, pctColX, rowY, pctPaint)

            rowY += rowHeight
        }

        // Subtle divider before Total
        val rulePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(legendStartX, rowY - 3f, pctColX, rowY - 3f, rulePaint)
        rowY += 10f

        // Total Row
        val totalLabelPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val totalHoursPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Total", legendStartX, rowY, totalLabelPaint)
        canvas.drawText(totalHoursStr, hoursColX, rowY, totalHoursPaint)
        val totalPctStr = if (totalRecordedMinutes > 0) "100%" else "0%"
        canvas.drawText(totalPctStr, pctColX, rowY, pctPaint)

        // 3. Horizontal divider between upper section and four stats
        canvas.drawLine(
            MARGIN_LEFT + 14f,
            midDividerY,
            MARGIN_LEFT + CONTENT_WIDTH - 14f,
            midDividerY,
            rulePaint
        )

        // 4. Four Statistics (Current Streak, Longest Streak, Active Days, Missed Days)
        val streakStats = calculateMonthlyStreakStats(yearMonth, records)

        val statLabelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val statValuePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val stats = listOf(
            "CURRENT STREAK" to "${streakStats.currentStreak} ${if (streakStats.currentStreak == 1) "day" else "days"}",
            "LONGEST STREAK" to "${streakStats.longestStreak} ${if (streakStats.longestStreak == 1) "day" else "days"}",
            "ACTIVE DAYS" to "${streakStats.activeDays} ${if (streakStats.activeDays == 1) "day" else "days"}",
            "MISSED DAYS" to "${streakStats.missedDays} ${if (streakStats.missedDays == 1) "day" else "days"}"
        )

        val colWidth = CONTENT_WIDTH / 4f
        for ((i, stat) in stats.withIndex()) {
            val cx = MARGIN_LEFT + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(stat.first, cx, statsStartY + 8f, statLabelPaint)
            canvas.drawText(stat.second, cx, statsStartY + 25f, statValuePaint)

            if (i > 0) {
                val vx = MARGIN_LEFT + (i * colWidth)
                canvas.drawLine(vx, statsStartY + 2f, vx, statsStartY + 28f, rulePaint)
            }
        }

        return startY + cardHeight + 16f
    }

    private fun drawInsightsSection(
        canvas: Canvas,
        startY: Float,
        progress: MonthlyOverallProgress
    ): Float {
        val cardHeight = 100f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        val bgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        val headingPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            isAntiAlias = true
        }
        canvas.drawText("MONTHLY INSIGHTS & REFLECTION", MARGIN_LEFT + 14f, startY + 18f, headingPaint)

        val bodyPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        var lineY = startY + 36f

        val mostTimeStr = progress.mostTimeSpentGoal?.let {
            "• Most Time Spent: ${it.goal.name} — ${TimeGoalCalculations.formatHoursTotal(it.recordedMinutes)} invested"
        } ?: "• Most Time Spent: No recorded hours yet"
        canvas.drawText(mostTimeStr, MARGIN_LEFT + 14f, lineY, bodyPaint)
        lineY += 16f

        val mostConsistentStr = progress.mostConsistentGoal?.let {
            "• Most Consistent Pursuit: ${it.goal.name} — ${it.completedDaysCount} days completed"
        } ?: "• Most Consistent Pursuit: Record daily to build consistency"
        canvas.drawText(mostConsistentStr, MARGIN_LEFT + 14f, lineY, bodyPaint)
        lineY += 16f

        val largestGapStr = progress.largestGapGoal?.let {
            val gap = (it.plannedMinutes - it.recordedMinutes).coerceAtLeast(0)
            "• Largest Opportunity: ${it.goal.name} — ${TimeGoalCalculations.formatHoursTotal(gap)} remaining to reach monthly target"
        } ?: "• Largest Opportunity: All goals met or on track"
        canvas.drawText(largestGapStr, MARGIN_LEFT + 14f, lineY, bodyPaint)
        lineY += 16f

        val totalHours = TimeGoalCalculations.formatHoursTotal(progress.totalRecordedMinutes)
        val plannedHours = TimeGoalCalculations.formatHoursTotal(progress.totalPlannedMinutes)
        canvas.drawText("• Total monthly time invested: $totalHours out of $plannedHours planned (${progress.overallProgressPercent}% achieved).", MARGIN_LEFT + 14f, lineY, boldPaint)

        return startY + cardHeight + 16f
    }

    /**
     * Shares the generated PDF file using the Android system share sheet.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Here is my Time Goals Report (${pdfFile.name}).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Time Goals (PDF)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
