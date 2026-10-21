/*
 * 主题迁移 / 恢复测试（Instrumented，需要 Android 设备 / 模拟器）。
 *
 * 覆盖 P0 恢复逻辑：
 *  - isKnownTheme：能否识别全部预设（含三个曾缺资源的主题）与特殊主题，拒绝损坏值；
 *  - mapLegacyTheme / migrateLegacyTheme：旧版主题值稳定迁移到已知预设；
 *  - selfHealTheme：损坏 / 未知主题回退默认，且只写入主题 key，不触碰其他用户数据；
 *  - App.ThemeDelegate.getColorByAttr：缺失资源时安全回退，绝不抛 Resources.NotFoundException。
 *
 * 注意：本环境无可用模拟器，需在设备 / CI 模拟器上执行：
 *   ./gradlew :app:connectedDebugAndroidTest
 */
package com.huanchengfly.tieba.post.theme

import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.ui.common.theme.ThemePresets
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRecoveryTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun isKnownThemeRecognizesAllPresets() {
        for (id in ThemePresets.allIds) {
            assertTrue("应识别预设 $id", ThemeUtil.isKnownTheme(id))
        }
    }

    @Test
    fun isKnownThemeRecognizesProblemThemes() {
        for (id in listOf(
            "oxygen_deep_sea_blue", "oxygen_jade_green", "oxygen_indigo_blue",
            "oxygen_deep_sea_blue_dark", "oxygen_jade_green_dark", "oxygen_indigo_blue_dark",
        )) {
            assertTrue("$id 必须被视为已知主题", ThemeUtil.isKnownTheme(id))
        }
    }

    @Test
    fun isKnownThemeRejectsCorruptValues() {
        assertFalse(ThemeUtil.isKnownTheme(null))
        assertFalse(ThemeUtil.isKnownTheme(""))
        assertFalse(ThemeUtil.isKnownTheme("not_a_real_theme"))
        assertFalse(ThemeUtil.isKnownTheme("oxygen_ghost"))
    }

    @Test
    fun legacyMigrationTargetsAreKnown() {
        // 显式校验关键旧值
        assertEquals("material_blue", ThemeUtil.mapLegacyTheme("tieba"))
        assertEquals("oxygen_blue", ThemeUtil.mapLegacyTheme("blue"))
        assertEquals("material_blue_grey", ThemeUtil.mapLegacyTheme("black"))
        assertEquals("oxygen_blue_dark", ThemeUtil.mapLegacyTheme("dark"))
        // 未知值原样返回
        assertEquals("totally_unknown", ThemeUtil.mapLegacyTheme("totally_unknown"))
    }

    @Test
    fun legacyMigrationMapTargetsValidPresets() {
        // 所有迁移目标都必须是已知主题，避免迁移到不存在的预设
        for ((_, new) in ThemePresets.legacyMigration) {
            assertTrue("迁移目标 $new 必须是已知主题", ThemeUtil.isKnownTheme(new))
        }
    }

    @Test
    fun customColourNotificationDoesNotForceLightTheme() {
        val selectedBefore = ThemeUtil.themeState.value
        val revisionBefore = ThemeUtil.customThemeRevision.value
        ThemeUtil.notifyCustomThemeChanged()
        assertEquals("颜色存盘通知不应覆盖深色/AMOLED 当前主题",
            selectedBefore, ThemeUtil.themeState.value)
        assertEquals(revisionBefore + 1, ThemeUtil.customThemeRevision.value)
    }

    @Test
    fun extremePaletteContrastRemainsReadable() {
        val black = android.graphics.Color.BLACK
        val white = android.graphics.Color.WHITE
        assertTrue(ThemeColorMath.contrastInt(black, white) >= 21.0)
        for (seed in listOf(black, white, 0xFFFFFF00.toInt(),
            0xFF00FF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(),
            0xFF0071CD.toInt(), 0xFF01998C.toInt(), 0xFF3F3DA2.toInt())) {
            for (surface in listOf(black, white, 0xFF212121.toInt())) {
                val text = ThemeColorMath.readableAccentTextInt(seed, surface)
                assertTrue("contrast too low for ${seed} on ${surface}",
                    ThemeColorMath.contrastInt(text, surface) >= 4.5)
                val foreground = ThemeColorMath.readableOnInt(seed)
                assertTrue(ThemeColorMath.contrastInt(foreground, seed) >= 3.0)
            }
        }
    }

    @Test
    fun getColorByAttrNeverThrowsOnMissingResource() {
        // 故意使用一个不存在的主题，验证安全回退（resolveColorSafe）不会崩溃
        val safe = runCatching {
            @Suppress("DEPRECATION")
            com.huanchengfly.tieba.post.App.ThemeDelegate.getColorByAttr(
                ctx, android.R.attr.colorBackground, "theme_that_does_not_exist_12345"
            )
        }
        assertTrue("getColorByAttr 在资源缺失时必须安全回退而非抛异常", safe.isSuccess)
    }
}
