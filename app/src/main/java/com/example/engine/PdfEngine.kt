package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class PageSizePreset(val width: Int, val height: Int, val label: String) {
    A4(595, 842, "A4 (210 × 297 mm)"),
    A5(420, 595, "A5 (148 × 210 mm)"),
    LETTER(612, 792, "Letter (8.5 × 11 in)"),
    FIT_IMAGE(0, 0, "Fit to Image Size")
}

data class DrawingPoint(val xRatio: Float, val yRatio: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val colorArgb: Int,
    val strokeWidthPt: Float,
    val isHighlighter: Boolean = false
)

data class TextAnnotation(
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val colorArgb: Int,
    val fontSizePt: Float,
    val isBold: Boolean = false
)

data class PageAnnotations(
    val strokes: List<DrawingStroke> = emptyList(),
    val textItems: List<TextAnnotation> = emptyList()
)

object PdfEngine {

    /**
     * Copy URI content to a temporary local File so ParcelFileDescriptor / File APIs work seamlessly.
     */
    suspend fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "pdf_tmp"): File = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Could not open input stream for URI: $uri")
        tempFile
    }

    /**
     * Creates a blank 1-page PDF document to draw or write notes on.
     */
    suspend fun createBlankPdf(context: Context, pageSize: PageSizePreset = PageSizePreset.A4): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val width = if (pageSize.width > 0) pageSize.width else 595
        val height = if (pageSize.height > 0) pageSize.height else 842

        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)
        document.finishPage(page)

        val outputFile = File(context.filesDir, "Blank_Document_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    /**
     * Converts multiple images (JPG, PNG) into a single PDF locally.
     */
    suspend fun createPdfFromImages(
        context: Context,
        imageUris: List<Uri>,
        pageSize: PageSizePreset = PageSizePreset.A4,
        isLandscape: Boolean = false,
        marginPt: Int = 18,
        rotations: List<Int> = emptyList()
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        for (i in imageUris.indices) {
            val uri = imageUris[i]
            val rotation = if (i < rotations.size) rotations[i] else 0

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: continue
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) continue

            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                bitmap = rotated
            }

            var targetWidth = pageSize.width
            var targetHeight = pageSize.height
            if (pageSize == PageSizePreset.FIT_IMAGE) {
                targetWidth = bitmap.width
                targetHeight = bitmap.height
            } else if (isLandscape) {
                val temp = targetWidth
                targetWidth = targetHeight
                targetHeight = temp
            }

            val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val printableWidth = (targetWidth - (marginPt * 2)).toFloat().coerceAtLeast(10f)
            val printableHeight = (targetHeight - (marginPt * 2)).toFloat().coerceAtLeast(10f)

            val scale = minOf(printableWidth / bitmap.width, printableHeight / bitmap.height)
            val destWidth = bitmap.width * scale
            val destHeight = bitmap.height * scale

            val left = marginPt + (printableWidth - destWidth) / 2f
            val top = marginPt + (printableHeight - destHeight) / 2f

            val destRect = RectF(left, top, left + destWidth, top + destHeight)
            canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

            document.finishPage(page)
            bitmap.recycle()
        }

        val outputFile = File(context.filesDir, "Images_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    /**
     * Merge multiple PDF files into one single PDF locally.
     */
    suspend fun mergePdfs(
        context: Context,
        pdfFiles: List<File>,
        outputName: String = "Merged_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        var totalPageCounter = 1

        for (file in pdfFiles) {
            if (!file.exists() || file.length() == 0L) continue
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val widthPt = page.width
                val heightPt = page.height

                // Render page at 2x density for clear quality with ARGB_8888
                val renderScale = 2f
                val bitmap = Bitmap.createBitmap(
                    (widthPt * renderScale).toInt().coerceAtLeast(1),
                    (heightPt * renderScale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, totalPageCounter++).create()
                val newPage = document.startPage(pageInfo)
                val canvas = newPage.canvas

                canvas.drawBitmap(bitmap, null, Rect(0, 0, widthPt, heightPt), Paint(Paint.FILTER_BITMAP_FLAG))
                document.finishPage(newPage)
                bitmap.recycle()
            }
            renderer.close()
            pfd.close()
        }

        val outputFile = File(context.filesDir, outputName)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    /**
     * Bakes pencil drawings, colored pencil annotations, and text items onto the PDF document.
     */
    suspend fun saveAnnotatedPdf(
        context: Context,
        sourcePdf: File,
        annotationsMap: Map<Int, PageAnnotations>,
        outputName: String = "Edited_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()

        for (pageIndex in 0 until renderer.pageCount) {
            val page = renderer.openPage(pageIndex)
            val widthPt = page.width
            val heightPt = page.height

            // Render original page in ARGB_8888 at 2.0x scale for crisp quality
            val renderScale = 2.0f
            val pageBitmap = Bitmap.createBitmap(
                (widthPt * renderScale).toInt().coerceAtLeast(1),
                (heightPt * renderScale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            pageBitmap.eraseColor(Color.WHITE)
            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, pageIndex + 1).create()
            val newPage = document.startPage(pageInfo)
            val canvas = newPage.canvas

            // 1. Draw base page bitmap
            canvas.drawBitmap(pageBitmap, null, Rect(0, 0, widthPt, heightPt), Paint(Paint.FILTER_BITMAP_FLAG))
            pageBitmap.recycle()

            // 2. Overlay annotations if present for this page
            val pageAnno = annotationsMap[pageIndex]
            if (pageAnno != null) {
                // Draw pencil & highlighter strokes
                for (stroke in pageAnno.strokes) {
                    if (stroke.points.size < 2) continue
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = stroke.colorArgb
                        style = Paint.Style.STROKE
                        strokeWidth = stroke.strokeWidthPt
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        if (stroke.isHighlighter) {
                            alpha = 110 // Semi-transparent highlighter effect
                        }
                    }

                    val path = Path()
                    val first = stroke.points[0]
                    path.moveTo(first.xRatio * widthPt, first.yRatio * heightPt)
                    for (p in 1 until stroke.points.size) {
                        val pt = stroke.points[p]
                        path.lineTo(pt.xRatio * widthPt, pt.yRatio * heightPt)
                    }
                    canvas.drawPath(path, strokePaint)
                }

                // Draw text annotations
                for (txt in pageAnno.textItems) {
                    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = txt.colorArgb
                        textSize = txt.fontSizePt
                        typeface = if (txt.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    }
                    canvas.drawText(
                        txt.text,
                        txt.xRatio * widthPt,
                        txt.yRatio * heightPt,
                        textPaint
                    )
                }
            }

            document.finishPage(newPage)
        }

        renderer.close()
        pfd.close()

        val outputFile = File(context.filesDir, outputName)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    /**
     * Renders a high-resolution ARGB_8888 bitmap of a single page for the viewer/editor.
     */
    suspend fun renderPageBitmap(
        context: Context,
        sourcePdf: File,
        pageIndex: Int,
        renderScale: Float = 2.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex !in 0 until renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val width = (page.width * renderScale).toInt().coerceAtLeast(1)
            val height = (page.height * renderScale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders lightweight thumbnail bitmaps (ARGB_8888) for fast filmstrip scrolling.
     */
    suspend fun getPageThumbnails(
        context: Context,
        sourcePdf: File,
        maxPages: Int = 50
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<Bitmap>()
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = minOf(renderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height

                val thumbW = 200
                val thumbH = ((h.toFloat() / w.toFloat()) * thumbW).toInt().coerceAtLeast(100)

                val thumbBitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
                thumbBitmap.eraseColor(Color.WHITE)
                page.render(thumbBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                thumbnails.add(thumbBitmap)
            }

            renderer.close()
            pfd.close()
        } catch (_: Exception) {}
        thumbnails
    }

    /**
     * Returns total page count of a PDF file.
     */
    suspend fun getPageCount(sourcePdf: File): Int = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            0
        }
    }
}
