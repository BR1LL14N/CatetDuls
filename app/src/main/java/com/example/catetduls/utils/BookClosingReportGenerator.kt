package com.example.catetduls.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.example.catetduls.R
import com.example.catetduls.data.Transaction
import com.example.catetduls.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BookClosingReportGenerator {

    fun generateCsv(transactions: List<Transaction>, categoryMap: Map<Int, String>, periodLabel: String): String {
        val sb = StringBuilder()
        
        // Header
        sb.append("No,Tanggal,Waktu,Kategori,Tipe,Catatan,Jumlah\n")
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        
        transactions.sortedBy { it.date }.forEachIndexed { index, transaction ->
            val date = dateFormat.format(Date(transaction.date))
            val time = timeFormat.format(Date(transaction.date))
            val category = categoryMap[transaction.categoryId] ?: "Umum"
            val type = if (transaction.type == TransactionType.PEMASUKAN) "Pemasukan" else "Pengeluaran"
            
            // Escape notes
            val escapedNotes = transaction.notes.replace("\"", "\"\"").let {
                if (it.contains(",") || it.contains("\"") || it.contains("\n")) "\"$it\"" else it
            }
            
            val amount = transaction.amount.toLong()
            
            sb.append("${index + 1},$date,$time,$category,$type,$escapedNotes,$amount\n")
        }
        
        // Summary
        val totalIncome = transactions.filter { it.type == TransactionType.PEMASUKAN }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.PENGELUARAN }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        
        sb.append("\n# RINGKASAN PERIODE $periodLabel\n")
        sb.append("# Total Pemasukan,${totalIncome.toLong()}\n")
        sb.append("# Total Pengeluaran,${totalExpense.toLong()}\n")
        sb.append("# Saldo Bersih,${balance.toLong()}\n")
        
        return sb.toString()
    }

    fun generatePdf(context: Context, transactions: List<Transaction>, categoryMap: Map<Int, String>, periodLabel: String, outputStream: java.io.OutputStream) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        
        var pageNumber = 1
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        // Dimensions and Colors
        val PAGE_WIDTH = 595f
        val PAGE_HEIGHT = 842f
        val MARGIN = 40f
        
        val COLOR_PRIMARY = ContextCompat.getColor(context, R.color.primary)
        val COLOR_INCOME = Color.parseColor("#4CAF50")
        val COLOR_EXPENSE = Color.parseColor("#F44336")
        
        fun drawHeader(canvas: Canvas) {
             // App Logo and Name
            val d = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            val bitmap = if (d is BitmapDrawable) d.bitmap else {
                val bmp = Bitmap.createBitmap(d?.intrinsicWidth ?: 100, d?.intrinsicHeight ?: 100, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                d?.setBounds(0, 0, c.width, c.height)
                d?.draw(c)
                bmp
            }
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            canvas.drawBitmap(scaledBitmap, MARGIN, MARGIN, paint)
            
            paint.color = COLOR_PRIMARY
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CatetDuls", MARGIN + 60f, MARGIN + 20f, paint)
            
            paint.textSize = 12f
            paint.color = Color.GRAY
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Laporan Tutup Buku", MARGIN + 60f, MARGIN + 40f, paint)
            
            // Title and Period
            paint.color = Color.BLACK
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val titleText = "LAPORAN KEUANGAN PERIODE"
            val titleWidth = paint.measureText(titleText)
            canvas.drawText(titleText, (PAGE_WIDTH - titleWidth) / 2, MARGIN + 80f, paint)
            
            paint.textSize = 14f
            val labelWidth = paint.measureText(periodLabel)
            canvas.drawText(periodLabel, (PAGE_WIDTH - labelWidth) / 2, MARGIN + 100f, paint)
            
            paint.color = COLOR_PRIMARY
            paint.strokeWidth = 2f
            canvas.drawLine(MARGIN, MARGIN + 110f, PAGE_WIDTH - MARGIN, MARGIN + 110f, paint)
        }

        drawHeader(canvas)
        var yPosition = MARGIN + 130f
        
        // Summary Section
        val totalIncome = transactions.filter { it.type == TransactionType.PEMASUKAN }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.PENGELUARAN }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition + 80f, paint)
        
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        
        // Columns for Summary
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 3
        
        fun drawSummaryItem(label: String, amount: Double, color: Int, xOffset: Float) {
            paint.color = Color.GRAY
            canvas.drawText(label, xOffset, yPosition + 25f, paint)
            paint.color = color
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Rp ${String.format("%,.0f", amount)}", xOffset, yPosition + 55f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
        }
        
        drawSummaryItem("Pemasukan", totalIncome, COLOR_INCOME, MARGIN + 20f)
        drawSummaryItem("Pengeluaran", totalExpense, COLOR_EXPENSE, MARGIN + 20f + colWidth)
        drawSummaryItem("Saldo Bersih", balance, if (balance >= 0) COLOR_INCOME else COLOR_EXPENSE, MARGIN + 20f + 2 * colWidth)
        
        yPosition += 100f
        
        val tableHeaderY = yPosition
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN, tableHeaderY, PAGE_WIDTH - MARGIN, tableHeaderY + 30f, paint)
        
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val colDateX = MARGIN + 10f
        val colCatX = MARGIN + 100f
        val colNoteX = MARGIN + 220f
        val colAmountX = PAGE_WIDTH - MARGIN - 10f // Right aligned
        
        canvas.drawText("Tanggal", colDateX, tableHeaderY + 20f, paint)
        canvas.drawText("Kategori", colCatX, tableHeaderY + 20f, paint)
        canvas.drawText("Catatan", colNoteX, tableHeaderY + 20f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Jumlah", colAmountX, tableHeaderY + 20f, paint)
        paint.textAlign = Paint.Align.LEFT
        
        yPosition += 30f
        
        val sdf = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.BLACK
        paint.textSize = 11f
        
        transactions.sortedByDescending { it.date }.forEach { trans ->
             if (yPosition > PAGE_HEIGHT - 50f) {
                pdfDocument.finishPage(page)
                pageNumber++
                 val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPosition = MARGIN + 40f
                
                // Draw simplified header
                // drawHeader(canvas) // Optional on new pages
            }
            
            paint.color = if (transactions.indexOf(trans) % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA")
            paint.style = Paint.Style.FILL
            canvas.drawRect(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition + 25f, paint)
            
            paint.color = Color.BLACK 
            
            // Date
            canvas.drawText(sdf.format(Date(trans.date)), colDateX, yPosition + 18f, paint)
            
            // Category
            val catName = categoryMap[trans.categoryId] ?: "Umum"
            val displayCatName = if (catName.length > 15) catName.take(12) + "..." else catName
            canvas.drawText(displayCatName, colCatX, yPosition + 18f, paint)
            
            // Note
            val note = if (trans.notes.length > 25) trans.notes.take(22) + "..." else trans.notes
            canvas.drawText(note, colNoteX, yPosition + 18f, paint)
            
            // Amount
            paint.color = if (trans.type == TransactionType.PEMASUKAN) COLOR_INCOME else COLOR_EXPENSE
            paint.textAlign = Paint.Align.RIGHT
            val amountStr = (if (trans.type == TransactionType.PENGELUARAN) "-" else "+") + " Rp " + String.format("%,.0f", trans.amount)
            canvas.drawText(amountStr, colAmountX, yPosition + 18f, paint)
            paint.textAlign = Paint.Align.LEFT
            
            yPosition += 25f
        }
        
        pdfDocument.finishPage(page)
        
        try {
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
}
