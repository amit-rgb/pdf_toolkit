package com.example

import com.example.engine.DrawingPoint
import com.example.engine.DrawingStroke
import com.example.engine.PageAnnotations
import com.example.engine.PageSizePreset
import com.example.engine.TextAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfEngineUnitTest {

    @Test
    fun testPageSizePresets() {
        assertEquals(595, PageSizePreset.A4.width)
        assertEquals(842, PageSizePreset.A4.height)

        assertEquals(420, PageSizePreset.A5.width)
        assertEquals(595, PageSizePreset.A5.height)

        assertEquals(612, PageSizePreset.LETTER.width)
        assertEquals(792, PageSizePreset.LETTER.height)
    }

    @Test
    fun testAnnotationDataModel() {
        val points = listOf(
            DrawingPoint(0.1f, 0.2f),
            DrawingPoint(0.3f, 0.4f)
        )
        val stroke = DrawingStroke(
            points = points,
            colorArgb = -65536, // Red
            strokeWidthPt = 4.0f,
            isHighlighter = false
        )
        assertFalse(stroke.isHighlighter)
        assertEquals(2, stroke.points.size)

        val textAnno = TextAnnotation(
            text = "Approved by Reviewer",
            xRatio = 0.2f,
            yRatio = 0.5f,
            colorArgb = -16777216, // Black
            fontSizePt = 16.0f
        )
        assertEquals("Approved by Reviewer", textAnno.text)

        val pageAnno = PageAnnotations(
            strokes = listOf(stroke),
            textItems = listOf(textAnno)
        )
        assertEquals(1, pageAnno.strokes.size)
        assertEquals(1, pageAnno.textItems.size)
    }

    @Test
    fun testCoordinateRatioMapping() {
        val widthPt = 595
        val heightPt = 842

        val pt = DrawingPoint(0.5f, 0.25f)
        val mappedX = pt.xRatio * widthPt
        val mappedY = pt.yRatio * heightPt

        assertEquals(297.5f, mappedX, 0.01f)
        assertEquals(210.5f, mappedY, 0.01f)
    }
}
