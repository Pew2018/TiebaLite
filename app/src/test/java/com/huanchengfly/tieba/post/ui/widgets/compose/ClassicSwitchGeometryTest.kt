package com.huanchengfly.tieba.post.ui.widgets.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ClassicSwitchGeometryTest {
    private fun geometry(density: Float) = ClassicSwitchGeometry(
        width = 48f * density,
        height = 28f * density,
        trackWidth = 36f * density,
        trackHeight = 16f * density,
        strokeWidth = 2f * density,
        thumbDiameter = 24f * density,
    )

    @Test fun thumbCentersStayOnDrawnEndCapsAtFractionalDensities() {
        listOf(1f, 1.5f, 2.625f, 2.75f, 3f, 4f).forEach { density ->
            val g = geometry(density)
            val leftCap = g.trackLeft + g.trackPathHeight / 2f
            val rightCap = g.trackLeft + g.trackPathWidth - g.trackPathHeight / 2f
            // Layout rounds thumb offsets to pixels; allow at most half a pixel of error.
            assertEquals(leftCap, g.thumbStart.roundToInt() + g.thumbDiameter / 2f, 0.5f)
            assertEquals(rightCap, g.thumbEnd.roundToInt() + g.thumbDiameter / 2f, 0.5f)
            assertEquals(g.height / 2f, g.trackTop + g.trackRadius, 0.001f)
        }
    }

    @Test fun thumbCoversTrackEndsAndFitsTheVisualBounds() {
        val g = geometry(1f)
        val outerTrackLeft = g.trackLeft - g.strokeWidth / 2f
        val outerTrackRight = g.trackLeft + g.trackPathWidth + g.strokeWidth / 2f
        assertTrue(g.thumbStart >= 0f)
        assertTrue(g.thumbStart < outerTrackLeft)
        assertTrue(g.thumbEnd + g.thumbDiameter > outerTrackRight)
        assertTrue(g.thumbEnd + g.thumbDiameter <= g.width)
        assertTrue(g.thumbDiameter > g.trackPathHeight + g.strokeWidth)
        assertEquals(g.trackPathHeight / 2f, g.trackRadius, 0f)
    }

    @Test fun relativeOffsetsMirrorAcrossTheSameTrackInRtl() {
        val g = geometry(2.625f)
        assertEquals(g.thumbEnd, g.width - g.thumbDiameter - g.thumbStart, 0.001f)
        assertEquals(g.thumbStart, g.width - g.thumbDiameter - g.thumbEnd, 0.001f)
        assertTrue(g.thumbEnd > g.thumbStart)
    }
}
