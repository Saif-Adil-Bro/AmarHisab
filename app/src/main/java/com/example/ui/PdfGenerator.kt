package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.ExpenseEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generatePdf(
        context: Context,
        expenses: List<ExpenseEntity>,
        profileName: String,
        startDate: Long,
        endDate: Long,
        outputStream: OutputStream
    ): Boolean {
        val pdfDocument = PdfDocument()

        val bnLocale = Locale("bn", "BD")
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", bnLocale)
        val footerDateFormatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", bnLocale)

        val dateRangeString = "${dateFormatter.format(Date(startDate))} হতে ${dateFormatter.format(Date(endDate))}"
        val totalAmount = expenses.sumOf { it.price }

        // Load Bangla fonts
        val regularTypeface = try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali_regular)
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
        val boldTypeface = try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali_bold)
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT_BOLD
        }

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = regularTypeface
            isAntiAlias = true
        }

        val boldTextPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = boldTypeface
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val lightGrayPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        // Standard A4 Size: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        val leftMargin = 36f
        val rightMargin = 559f // 595 - 36
        val availableWidth = (rightMargin - leftMargin).toInt() // 523

        // Column Coordinates
        val col0X = leftMargin                   // Date starts
        val col1X = leftMargin + 85f            // Item description starts
        val col2X = leftMargin + 275f           // Category starts
        val col3X = leftMargin + 400f           // Amount starts

        // Header and layout parameters
        var currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas

        // Draw Cover / First Page Headers
        var currentY = 50f

        // App Header: "আমার হিসাব"
        boldTextPaint.textSize = 24f
        boldTextPaint.color = Color.parseColor("#1565C0") // Elegant Primary Accent
        drawTextAlignedCenter(canvas, "আমার হিসাব", pageWidth / 2f, currentY, boldTextPaint)
        currentY += 32f

        // Document Title
        boldTextPaint.textSize = 14f
        boldTextPaint.color = Color.BLACK
        drawTextAlignedCenter(canvas, "ব্যক্তিগত খরচের হিসাব বিবরণী (PDF রিপোর্ট)", pageWidth / 2f, currentY, boldTextPaint)
        currentY += 20f

        // Profile and Date Range Information
        textPaint.textSize = 11f
        textPaint.color = Color.DKGRAY
        drawTextAlignedCenter(canvas, "প্রোফাইল: $profileName | সময়কাল: $dateRangeString", pageWidth / 2f, currentY, textPaint)
        currentY += 30f

        // Highlight Summary Card
        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 50f, lightGrayPaint)
        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 50f, borderPaint)

        boldTextPaint.textSize = 13f
        boldTextPaint.color = Color.parseColor("#2E7D32") // Accent Green for Success status
        drawText(canvas, "মোট খরচ:", leftMargin + 16f, currentY + 18f, boldTextPaint)
        
        boldTextPaint.textSize = 16f
        val formattedTotal = "৳" + String.format(bnLocale, "%,.2f", totalAmount)
        drawText(canvas, formattedTotal, rightMargin - 150f, currentY + 16f, boldTextPaint)

        textPaint.textSize = 10f
        textPaint.color = Color.GRAY
        drawText(canvas, "মোট খরচের খতিয়ান ও বিবরণী নিচে সারণীভুক্ত করা হলো।", leftMargin + 16f, currentY + 38f, textPaint)
        currentY += 75f

        // Draw Table Helper Function
        fun drawTableHeaders(canvas: Canvas, y: Float) {
            // Header Background
            val headerHeight = 25f
            canvas.drawRect(leftMargin, y, rightMargin, y + headerHeight, lightGrayPaint)
            canvas.drawRect(leftMargin, y, rightMargin, y + headerHeight, borderPaint)

            boldTextPaint.textSize = 11f
            boldTextPaint.color = Color.BLACK

            // Column Header Labels
            drawText(canvas, "তারিখ", col0X + 8f, y + 6f, boldTextPaint)
            drawText(canvas, "ব্যয় বিবরণী / আইটেম", col1X + 8f, y + 6f, boldTextPaint)
            drawText(canvas, "ক্যাটাগরি", col2X + 8f, y + 6f, boldTextPaint)
            drawText(canvas, "পরিমাণ", col3X + 8f, y + 6f, boldTextPaint)
        }

        // Draw Footer on current page
        fun drawFooter(canvas: Canvas, pageNum: Int) {
            val footerY = pageHeight - 45f
            canvas.drawLine(leftMargin, footerY, rightMargin, footerY, borderPaint)

            textPaint.textSize = 9f
            textPaint.color = Color.GRAY
            val footerText = "রিপোর্ট তৈরির তারিখ: ${footerDateFormatter.format(Date())}"
            drawText(canvas, footerText, leftMargin, footerY + 16f, textPaint)

            val pageStr = "পৃষ্ঠা নং - $pageNum"
            drawText(canvas, pageStr, rightMargin - 70f, footerY + 16f, textPaint)
        }

        // Draw first page table headers
        drawTableHeaders(canvas, currentY)
        val headerRowHeight = 25f
        currentY += headerRowHeight

        val rowHeight = 32f

        for (i in expenses.indices) {
            val expense = expenses[i]

            // Check if drawing this row exceeds safety margin. Limit Y to pageHeight - 75f to allow footer
            if (currentY + rowHeight > pageHeight - 75f) {
                // Finish page
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)

                // Start new page
                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas

                // Small continuous header
                currentY = 45f
                boldTextPaint.textSize = 12f
                boldTextPaint.color = Color.GRAY
                drawText(canvas, "মোট খতিয়ান (চলমান...)", leftMargin, currentY, boldTextPaint)
                currentY += 15f

                drawTableHeaders(canvas, currentY)
                currentY += headerRowHeight
            }

            // Draw Row border/background (alternating stripes)
            if (i % 2 == 1) {
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, lightGrayPaint)
            }
            // Draw thin row dividers
            canvas.drawLine(leftMargin, currentY + rowHeight, rightMargin, currentY + rowHeight, borderPaint)
            canvas.drawLine(leftMargin, currentY, leftMargin, currentY + rowHeight, borderPaint)
            canvas.drawLine(col1X, currentY, col1X, currentY + rowHeight, borderPaint)
            canvas.drawLine(col2X, currentY, col2X, currentY + rowHeight, borderPaint)
            canvas.drawLine(col3X, currentY, col3X, currentY + rowHeight, borderPaint)
            canvas.drawLine(rightMargin, currentY, rightMargin, currentY + rowHeight, borderPaint)

            // Setup styling for items
            textPaint.textSize = 10f
            textPaint.color = Color.BLACK

            // Format date for row
            val rowDate = SimpleDateFormat("dd/MM/yyyy", bnLocale).format(Date(expense.date))
            drawText(canvas, rowDate, col0X + 8f, currentY + 10f, textPaint)

            // Draw Item Name with static layout to handle wrap if needed
            val itemWidthLimit = (col2X - col1X - 16f).toInt()
            drawBanglaText(
                canvas = canvas,
                text = expense.itemName,
                x = col1X + 8f,
                y = currentY + 9f,
                width = itemWidthLimit,
                paint = textPaint
            )

            // Draw Category with static layout to handle emoji and category name wrap
            val catWidthLimit = (col3X - col2X - 16f).toInt()
            drawBanglaText(
                canvas = canvas,
                text = expense.category,
                x = col2X + 8f,
                y = currentY + 9f,
                width = catWidthLimit,
                paint = textPaint
            )

            // Format amount
            val formatPrice = "৳" + String.format(bnLocale, "%,.2f", expense.price)
            drawText(canvas, formatPrice, col3X + 8f, currentY + 10f, textPaint)

            currentY += rowHeight
        }

        // Draw last page footer
        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        return try {
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            false
        }
    }

    // Helper functions for canvas text layout with StaticLayout
    private fun drawBanglaText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ) {
        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, alignment, 1.0f, 0.0f, false)
        }
        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        canvas.drawText(text, x, y + 10f, paint) // standard offset for baseline
    }

    private fun drawTextAlignedCenter(canvas: Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        val originalAlign = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y + 10f, paint)
        paint.textAlign = originalAlign
    }
}
