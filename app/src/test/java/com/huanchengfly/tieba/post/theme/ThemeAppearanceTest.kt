package com.huanchengfly.tieba.post.theme

import com.huanchengfly.tieba.post.ui.common.theme.ThemeAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeAppearanceTest {
    @Test fun amoledDoesNotReplaceTheSelectedPalette() {
        assertEquals("amoled_dark", ThemeAppearance.resolveDarkTheme("amoled_dark", "oxygen_jade_green_dark"))
        assertEquals("amoled_dark", ThemeAppearance.resolveDarkTheme("amoled_dark", "oxygen_indigo_blue_dark"))
    }

    @Test fun classicUsesTheCurrentlySelectedSeedNotAnOldSavedDarkPreset() {
        assertEquals("oxygen_deep_sea_blue_dark",
            ThemeAppearance.resolveDarkTheme("material_blue_grey_dark", "oxygen_deep_sea_blue_dark"))
        assertEquals("oxygen_indigo_blue_dark",
            ThemeAppearance.resolveDarkTheme("oxygen_jade_green_dark", "oxygen_indigo_blue_dark"))
    }

    @Test fun customClassicHasStableKnownFallback() {
        assertEquals("material_blue_grey_dark", ThemeAppearance.resolveDarkTheme("oxygen_blue_dark", null))
    }
}
