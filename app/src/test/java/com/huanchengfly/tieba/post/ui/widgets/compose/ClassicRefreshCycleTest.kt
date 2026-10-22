package com.huanchengfly.tieba.post.ui.widgets.compose

import com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicRefreshCycleTest {
    @Test fun classicToolbarPreservesWhiteInkAcrossBrightAndDarkPalettes() {
        listOf(0xFF009688, 0xFF03A9F4, 0xFFFFEB3B, 0xFFE91E63, 0xFF212121,
            0xFF000000, 0xFFFFFFFF, 0xFF9C27B0, 0xFF0071CD,
            0xFF8BC34A).forEach { seed ->
            val background = ThemeColorMath.classicToolbarBackgroundInt(seed.toInt())
            assertTrue(ThemeColorMath.contrastInt(background, 0xFFFFFFFF.toInt()) >= 4.5)
            assertEquals(0xFFFFFFFF.toInt(), ThemeColorMath.toolbarForegroundInt(background))
        }
    }

    @Test fun fixedBackgroundsKeepReadableInkAtMidtoneBoundary() {
        for (channel in 0..255) {
            val background = (0xFF shl 24) or (channel shl 16) or (channel shl 8) or channel
            assertTrue(ThemeColorMath.contrastInt(background,
                ThemeColorMath.toolbarForegroundInt(background)) >= 4.5)
        }
    }
    @Test fun duplicateGesturesCannotStartSecondRequest() {
        val cycle = ClassicRefreshCycle()
        assertTrue(cycle.begin(false))
        assertFalse(cycle.begin(false))
        assertTrue(cycle.pending)
        assertEquals(ClassicRefreshPhase.Armed, cycle.phase)
    }

    @Test fun successfulOrFailedRequestReturnsToIdle() {
        val cycle = ClassicRefreshCycle()
        cycle.begin(false)
        cycle.observe(true)
        assertEquals(ClassicRefreshPhase.Refreshing, cycle.phase)
        cycle.observe(false)
        assertFalse(cycle.pending)
        assertEquals(ClassicRefreshPhase.Settling, cycle.phase)
        cycle.settle()
        assertEquals(ClassicRefreshPhase.Idle, cycle.phase)
        assertTrue(cycle.begin(false))
    }

    @Test fun initialLoadingAndMissingStartAreSafe() {
        val cycle = ClassicRefreshCycle()
        assertFalse(cycle.begin(true))
        cycle.observe(false)
        assertFalse(cycle.pending)
        assertTrue(cycle.begin(false))
        cycle.expireIfNeverStarted()
        assertFalse(cycle.pending)
        assertEquals(ClassicRefreshPhase.Settling, cycle.phase)
    }

    @Test fun refreshGlyphHasReadableContrastOnLightDarkAndAccentSurfaces() {
        val surfaces = listOf(
            0xFF0071CD.toInt(), // classic blue
            0xFFFFFFFF.toInt(), // light / translucent fallback
            0xFF000000.toInt(), // AMOLED
            0xFFFFFF00.toInt(), // high-luminance custom accent
        )
        surfaces.forEach { surface ->
            val foreground = ThemeColorMath.readableOnInt(surface)
            assertTrue(
                "contrast ${ThemeColorMath.contrastInt(surface, foreground)} for #${surface.toUInt().toString(16)}",
                ThemeColorMath.contrastInt(surface, foreground) >= 3.0,
            )
        }
    }

    @Test fun automaticToolbarForegroundMeetsTextContrastOnCommonThemeSurfaces() {
        val surfaces = listOf(
            0xFF212121.toInt(), // classic toolbar
            0xFF0071CD.toInt(), // Oxygen blue
            0xFFFFFFFF.toInt(), // light custom toolbar
            0xFF000000.toInt(), // AMOLED
            0xFFFFFF00.toInt(), // high-luminance custom colour
        )
        surfaces.forEach { surface ->
            val foreground = ThemeColorMath.readableOnInt(surface)
            assertTrue(
                "contrast ${ThemeColorMath.contrastInt(surface, foreground)} for #${surface.toUInt().toString(16)}",
                ThemeColorMath.contrastInt(surface, foreground) >= 4.5,
            )
        }
    }

    @Test fun transparentSecondaryForegroundIsMeasuredAgainstItsActualToolbar() {
        val toolbar = 0xFF0071CD.toInt()
        val secondaryWhite = 0xB3FFFFFF.toInt()
        val composited = ThemeColorMath.compositeOnInt(secondaryWhite, toolbar)
        assertTrue(ThemeColorMath.contrastInt(composited, toolbar) >= 3.0)
    }

    @Test fun manualForegroundStillWinsOverTheAutomaticCandidate() {
        val toolbar = 0xFFFFE082.toInt()
        val automatic = ThemeColorMath.readableOnInt(toolbar)
        val manual = 0xFF5D4037.toInt()
        assertTrue(ThemeColorMath.contrastInt(toolbar, automatic) >= 4.5)
        assertTrue(ThemeColorMath.contrastInt(toolbar, manual) >= 4.5)
        assertFalse(manual == automatic)
    }
}
