package com.example.gentlenudge.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.example.gentlenudge.data.model.NudgeTask
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates high-quality, professional, 100% offline PDF documents of the user's
 * complete task and reminder history using native Android PdfDocument APIs.
 */
object HistoryPdfExporter {

    // Standard A4 dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 42f
    private const val MARGIN_RIGHT = 42f
    private const val MARGIN_TOP = 42f
    private const val MARGIN_BOTTOM = 45f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    // Nudge Palette
    private const val COLOR_PRIMARY = 0xFF3D70FF.toInt() // Nudge Signature Blue
    private const val COLOR_ON_PRIMARY = 0xFFFFFFFF.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFF1C1A17.toInt() // Deep Charcoal
    private const val COLOR_TEXT_SECONDARY = 0xFF5F5E5A.toInt() // Warm Slate
    private const val COLOR_TEXT_MUTED = 0xFF8C8881.toInt()
    private const val COLOR_BORDER = 0xFFE5E2DC.toInt()
    private const val COLOR_CARD_BG = 0xFFFAF9F6.toInt()
    private const val COLOR_HEADER_BG = 0xFFF5F7FF.toInt()

    // Status colors
    private const val COLOR_DONE_BG = 0xFFEBF3FF.toInt()
    private const val COLOR_DONE_TEXT = 0xFF2B5CD6.toInt()
    private const val COLOR_PENDING_BG = 0xFFF3F1EC.toInt()
    private const val COLOR_PENDING_TEXT = 0xFF55524B.toInt()
    private const val COLOR_IMPORTANT_BG = 0xFFFEECEB.toInt()
    private const val COLOR_IMPORTANT_TEXT = 0xFFC5221F.toInt()

    /**
     * Helper to compute dynamic filename matching format:
     * Task History - DD Month YYYY — HH:MM AM/PM.pdf
     */
    fun buildDynamicPdfFilename(exportDate: Date): String {
        val datePart = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(exportDate)
        val timePart = SimpleDateFormat("hh:mm a", Locale.US).format(exportDate)
        return "Task History - $datePart — $timePart.pdf"
    }

    /**
     * Helper to compute in-document export timestamp string:
     * August 24, 2026 at 08:10 AM
     */
    fun buildInPdfTimestamp(exportDate: Date): String {
        val datePart = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(exportDate)
        val timePart = SimpleDateFormat("hh:mm a", Locale.US).format(exportDate).uppercase(Locale.US)
        return "$datePart at $timePart"
    }

    /**
     * Generates a complete task history PDF and writes it to app storage.
     * Guaranteed to work 100% offline without network access.
     */
    fun generateHistoryPdf(
        context: Context,
        tasks: List<NudgeTask>,
        exportDate: Date = Date()
    ): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val filename = buildDynamicPdfFilename(exportDate)
        val pdfFile = File(exportDir, filename)

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        // Date formatter for header and task cards
        val generatedAtString = buildInPdfTimestamp(exportDate)
        val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

        var currentY = MARGIN_TOP

        // Helper to finish current page and start a new one (Page 2+ naturally continues without repeating headers)
        fun startNextPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = MARGIN_TOP
        }

        // Draw First Page Header with actual Nudge Logo and balanced spacing
        currentY = drawDocumentHeader(context, canvas, generatedAtString, exportDate)

        // Draw Summary Stats Card
        currentY = drawSummaryStats(canvas, currentY, tasks)

        if (tasks.isEmpty()) {
            // Graceful Empty State
            drawEmptyState(canvas, currentY)
        } else {
            // Partition tasks into Active and Completed
            val pendingTasks = tasks.filter { !it.isDone }
            val completedTasks = tasks.filter { it.isDone }

            if (pendingTasks.isNotEmpty()) {
                // Section Header: Active & Scheduled Tasks
                if (currentY + 50f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                currentY = drawSectionHeader(canvas, currentY, "ACTIVE & SCHEDULED NUDGES", pendingTasks.size)

                for (task in pendingTasks) {
                    val requiredHeight = measureTaskCardHeight(task)
                    if (currentY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        startNextPage()
                    }
                    currentY = drawTaskCard(canvas, currentY, task, shortDateFormat)
                }
            }

            if (completedTasks.isNotEmpty()) {
                // Section Header: Completed History
                if (currentY + 50f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNextPage()
                }
                if (pendingTasks.isNotEmpty()) {
                    currentY += 12f
                }
                currentY = drawSectionHeader(canvas, currentY, "COMPLETED HISTORY", completedTasks.size)

                for (task in completedTasks) {
                    val requiredHeight = measureTaskCardHeight(task)
                    if (currentY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        startNextPage()
                    }
                    currentY = drawTaskCard(canvas, currentY, task, shortDateFormat)
                }
            }
        }

        // Finish final page with clean blank bottom (no footer)
        pdfDocument.finishPage(currentPage)

        // Write PDF to output file
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Measures the dynamic height of a task card based on wrapped title and metadata lines.
     */
    private fun measureTaskCardHeight(task: NudgeTask): Float {
        val titlePaint = TextPaint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardInnerWidth = CONTENT_WIDTH - 24f
        val staticLayout = StaticLayout.Builder
            .obtain(task.title, 0, task.title.length, titlePaint, cardInnerWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .setIncludePad(false)
            .build()

        val titleHeight = staticLayout.height.toFloat()

        var metadataHeight = 16f
        if (task.timeLabel != "Any time" || task.repeat != "Does not repeat" || task.category != "Personal") {
            metadataHeight += 15f
        }

        return 12f + 18f + 6f + titleHeight + 8f + metadataHeight + 12f + 10f
    }

    /**
     * Draws the main editorial header on page 1 with the actual Nudge brand logo
     * and clean, balanced vertical hierarchy.
     */
    private fun drawDocumentHeader(
        context: Context,
        canvas: Canvas,
        generatedAt: String,
        exportDate: Date = Date()
    ): Float {
        val y = MARGIN_TOP
        val logoSize = 42f

        // Draw the exact official Nudge logo
        drawActualNudgeLogo(context, canvas, MARGIN_LEFT, y, logoSize)

        val textStartX = MARGIN_LEFT + logoSize + 14f

        // 1. Brand Sub-label ("NUDGE")
        val brandPaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.14f
            isAntiAlias = true
        }
        canvas.drawText("NUDGE", textStartX, y + 10f, brandPaint)

        // 2. Main Title ("Task History — [Month Year]")
        val titlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 19f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val monthYearStr = SimpleDateFormat("MMMM yyyy", Locale.US).format(exportDate)
        canvas.drawText("Task History — $monthYearStr", textStartX, y + 32f, titlePaint)

        // 3. Generation Metadata Line
        val metaPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Generated: $generatedAt", textStartX, y + 49f, metaPaint)

        val dividerY = y + 62f

        // 4. Horizontal Separator Rule
        val rulePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN_LEFT, dividerY, MARGIN_LEFT + CONTENT_WIDTH, dividerY, rulePaint)

        return dividerY + 16f
    }

    /**
     * Renders the actual Gentle Nudge clock logo with full vector fidelity.
     */
    private fun drawActualNudgeLogo(
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
     * Vector drawing of the Gentle Nudge brand mark:
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

    /**
     * Draws the summary statistics card.
     */
    private fun drawSummaryStats(canvas: Canvas, startY: Float, tasks: List<NudgeTask>): Float {
        val cardHeight = 44f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        // Background
        val bgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        val total = tasks.size
        val completed = tasks.count { it.isDone }
        val pending = total - completed
        val completionRate = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

        val columnWidth = CONTENT_WIDTH / 4f
        val stats = listOf(
            "TOTAL TASKS" to "$total",
            "COMPLETED" to "$completed",
            "PENDING" to "$pending",
            "COMPLETION RATE" to "$completionRate%"
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
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        for (i in stats.indices) {
            val colX = MARGIN_LEFT + (i * columnWidth) + 14f
            val (label, value) = stats[i]
            canvas.drawText(label, colX, startY + 16f, labelPaint)
            canvas.drawText(value, colX, startY + 34f, valPaint)
        }

        return startY + cardHeight + 16f
    }

    /**
     * Draws a section header bar.
     */
    private fun drawSectionHeader(canvas: Canvas, startY: Float, title: String, count: Int): Float {
        var y = startY

        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.1f
            isAntiAlias = true
        }
        canvas.drawText("$title ($count)", MARGIN_LEFT, y + 10f, titlePaint)

        y += 18f
        return y
    }

    /**
     * Draws an individual task card with badges, multi-line wrapped text, and metadata.
     */
    private fun drawTaskCard(
        canvas: Canvas,
        startY: Float,
        task: NudgeTask,
        dateFormat: SimpleDateFormat
    ): Float {
        val totalCardHeight = measureTaskCardHeight(task) - 10f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + totalCardHeight)

        // Background
        val bgPaint = Paint().apply {
            color = if (task.isDone) COLOR_CARD_BG else 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = if (task.priority == "Important") COLOR_IMPORTANT_TEXT else COLOR_BORDER
            strokeWidth = if (task.priority == "Important") 1f else 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        var contentY = startY + 12f
        val contentX = MARGIN_LEFT + 12f

        // 1. Status Pill Badge
        val statusText = if (task.isDone) "✓ COMPLETED" else "⏳ PENDING"
        val statusBgColor = if (task.isDone) COLOR_DONE_BG else COLOR_PENDING_BG
        val statusTextColor = if (task.isDone) COLOR_DONE_TEXT else COLOR_PENDING_TEXT

        drawBadge(canvas, contentX, contentY, statusText, statusBgColor, statusTextColor)

        var badgeOffset = contentX + measureBadgeWidth(statusText) + 8f

        // 2. Priority Badge if Important
        if (task.priority == "Important") {
            drawBadge(canvas, badgeOffset, contentY, "★ IMPORTANT", COLOR_IMPORTANT_BG, COLOR_IMPORTANT_TEXT)
            badgeOffset += measureBadgeWidth("★ IMPORTANT") + 8f
        }

        // 3. Category Badge
        if (task.category.isNotBlank() && task.category != "Personal") {
            drawBadge(canvas, badgeOffset, contentY, task.category.uppercase(Locale.US), COLOR_PENDING_BG, COLOR_TEXT_SECONDARY)
        }

        contentY += 18f + 6f

        // 4. Task Title (Preserved exactly, wrapped via StaticLayout)
        val titlePaint = TextPaint().apply {
            color = if (task.isDone) COLOR_TEXT_SECONDARY else COLOR_TEXT_PRIMARY
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardInnerWidth = CONTENT_WIDTH - 24f
        val staticLayout = StaticLayout.Builder
            .obtain(task.title, 0, task.title.length, titlePaint, cardInnerWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(contentX, contentY)
        staticLayout.draw(canvas)
        canvas.restore()

        contentY += staticLayout.height + 8f

        // 5. Metadata Row 1: Creation and Completion Dates
        val metaPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val createdStr = "Created: ${dateFormat.format(Date(task.createdAt))}"
        val completedStr = if (task.isDone && task.completedAt != null) {
            " • Completed: ${dateFormat.format(Date(task.completedAt))}"
        } else ""

        canvas.drawText("$createdStr$completedStr", contentX, contentY + 7f, metaPaint)

        // 6. Metadata Row 2: Reminder Time / Repeat / Sound if configured
        val reminderParts = mutableListOf<String>()
        if (task.timeLabel != "Any time" || task.dateLabel != "Today") {
            reminderParts.add("Nudge: ${task.dateLabel} at ${task.timeLabel}")
        }
        if (task.repeat != "Does not repeat") {
            reminderParts.add("Repeats: ${task.repeat}")
        }
        if (task.soundType != "Small nudge") {
            reminderParts.add("Sound: ${task.soundType}")
        }

        if (reminderParts.isNotEmpty()) {
            contentY += 14f
            val reminderPaint = Paint().apply {
                color = COLOR_DONE_TEXT
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(reminderParts.joinToString(" • "), contentX, contentY + 7f, reminderPaint)
        }

        return startY + totalCardHeight + 10f
    }

    /**
     * Draws a rounded pill badge for status and metadata.
     */
    private fun drawBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        text: String,
        bgColor: Int,
        textColor: Int
    ) {
        val badgeHeight = 16f
        val textPaint = Paint().apply {
            color = textColor
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.04f
            isAntiAlias = true
        }
        val textWidth = textPaint.measureText(text)
        val badgeWidth = textWidth + 12f
        val badgeRect = RectF(x, y, x + badgeWidth, y + badgeHeight)

        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 4f, 4f, bgPaint)
        canvas.drawText(text, x + 6f, y + 11.5f, textPaint)
    }

    /**
     * Measures badge width for horizontal layout offsets.
     */
    private fun measureBadgeWidth(text: String): Float {
        val textPaint = Paint().apply {
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.04f
            isAntiAlias = true
        }
        return textPaint.measureText(text) + 12f
    }

    /**
     * Draws a graceful empty state message when history is empty.
     */
    private fun drawEmptyState(canvas: Canvas, startY: Float) {
        val cardHeight = 90f
        val cardRect = RectF(MARGIN_LEFT, startY, MARGIN_LEFT + CONTENT_WIDTH, startY + cardHeight)

        val bgPaint = Paint().apply {
            color = COLOR_CARD_BG
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

        val mainTextPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subTextPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val centerX = MARGIN_LEFT + (CONTENT_WIDTH / 2f)
        canvas.drawText("No task history available yet.", centerX, startY + 38f, mainTextPaint)
        canvas.drawText("Active and completed nudges will be organized and exported here.", centerX, startY + 56f, subTextPaint)
    }

    /**
     * Triggers the Android system share/save chooser for the exported PDF.
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
            putExtra(
                Intent.EXTRA_SUBJECT,
                pdfFile.nameWithoutExtension
            )
            putExtra(Intent.EXTRA_TEXT, "Here is my Nudge Task History export (${pdfFile.name}).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Task History (PDF)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
