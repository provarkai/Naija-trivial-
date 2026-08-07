package com.ai4biz.app.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a generated document's title + body into a simple word-wrapped,
 * multi-page PDF and exposes it via FileProvider for sharing/opening — this
 * is the MVP "PDF Export" requirement from the PRD.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_SPACING = 6f

    fun export(context: Context, title: String, body: String, fileNameHint: String): Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }

        val contentWidth = PAGE_WIDTH - MARGIN * 2
        val lines = wrapText(body, bodyPaint, contentWidth)
        val lineHeight = bodyPaint.textSize + LINE_SPACING

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        var y = MARGIN

        canvas.drawText(title, MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + LINE_SPACING * 3

        for (line in lines) {
            if (y + lineHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = MARGIN
            }
            canvas.drawText(line, MARGIN, y + bodyPaint.textSize, bodyPaint)
            y += lineHeight
        }
        document.finishPage(page)

        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val safeName = fileNameHint.ifBlank { "document" }
            .replace(Regex("[^A-Za-z0-9-_ ]"), "")
            .replace(" ", "_")
            .take(50)
        val file = File(exportsDir, "${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            if (paragraph.isEmpty()) {
                result.add("")
                return@forEach
            }
            var current = StringBuilder()
            paragraph.split(" ").forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                    result.add(current.toString())
                    current = StringBuilder(word)
                } else {
                    current = StringBuilder(candidate)
                }
            }
            result.add(current.toString())
        }
        return result
    }
}
