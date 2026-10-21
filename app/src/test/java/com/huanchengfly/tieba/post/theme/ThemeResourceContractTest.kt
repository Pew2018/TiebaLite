/*
 * 主题资源契约测试（JVM 单元测试，无需 Android 设备）。
 *
 * 直接解析 res/values 下的 colors.xml / styles.xml / arrays.xml，
 * 校验每一个主题 ID 都拥有完整的 theme_color_* 颜色资源与对应 style，
 * 防止 P0 类崩溃（getColorByAttr 在 getIdentifier()==0 时崩溃）。
 *
 * 该测试可在本地 `./gradlew :app:testDebugUnitTest` 直接执行。
 */
package com.huanchengfly.tieba.post.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.text.Regex

class ThemeResourceContractTest {

    private fun moduleDir(): File {
        // Gradle 运行单元测试时工作目录为 :app 模块根；兜底尝试仓库根下的 app/。
        val candidates = listOf(
            File("src/main/res/values"),
            File("app/src/main/res/values"),
            File(System.getProperty("user.dir"), "app/src/main/res/values"),
        )
        return candidates.firstNotNullOfOrNull { if (it.exists()) it else null }
            ?: error("无法定位 res/values 目录")
    }

    private val resDir = moduleDir()

    private fun read(name: String): String =
        File(resDir, name).readText(Charsets.UTF_8)

    private val colorsXml = read("colors.xml")
    private val stylesXml = read("styles.xml")
    private val arraysXml = read("arrays.xml")

    private val colorNames: Set<String> by lazy {
        Regex("""<color\s+name="([^"]+)"""").findAll(colorsXml)
            .map { it.groupValues[1] }
            .toSet()
    }

    private val styleNames: Set<String> by lazy {
        Regex("""<style\s+name="([^"]+)"""").findAll(stylesXml)
            .map { it.groupValues[1].replace('.', '_') } // R 类名以 _ 分隔，XML 中以 . 分隔
            .toSet()
    }

    /**
     * getColorByAttr 实际以 theme_color_<attr>_<theme> 命名的语义色后缀（取自 App.kt）。
     * 其余（color_text_*、shadow、on_nav_bar_surface 等）为回退 / 命名差异，不计入契约。
     */
    private val NON_CONTRACT_SUFFIXES = setOf(
        "text", "text_disabled", "text_secondary", "text_on_primary", "shadow", "on_nav_bar_surface"
    )

    private fun arrayItems(arrayName: String): List<String> {
        val block = Regex("""<string-array\s+name="$arrayName">(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
            .find(arraysXml)?.groupValues?.get(1)
            ?: error("未找到 string-array: $arrayName")
        return Regex("""<item>(.*?)</item>""").findAll(block)
            .map { it.groupValues[1].trim() }
            .toList()
    }

    private val lightThemes: List<String> by lazy { arrayItems("theme_values") }
    private val darkThemes: List<String> by lazy { arrayItems("dark_theme_values") }

    /** 以 material_blue（浅色基准）推导 theme_color_* 语义 attr 后缀，剔除非契约后缀。 */
    private val referenceAttrs: Set<String> by lazy {
        colorNames.filter { it.startsWith("theme_color_") && it.endsWith("_material_blue") }
            .map { it.removePrefix("theme_color_").removeSuffix("_material_blue") }
            .toSet() - NON_CONTRACT_SUFFIXES
    }

    private fun titleCase(themeId: String): String =
        themeId.split("_").joinToString("_") { it.replaceFirstChar(Char::uppercase) }

    private fun expectedStyle(themeId: String): String =
        if (themeId == "amoled_dark") "TiebaLite_Dark_Amoled"
        else "TiebaLite_Preset_${titleCase(themeId)}"

    @Test
    fun referenceThemeHasFullColorBlock() {
        assertTrue("基准主题 material_blue 应拥有完整 theme_color 颜色块", referenceAttrs.isNotEmpty())
        // 关键的背景与顶栏色必须存在
        for (must in listOf("background", "toolbar", "primary", "on_toolbar_surface")) {
            assertTrue("material_blue 缺少 theme_color_${must}_material_blue", must in referenceAttrs)
        }
    }

    @Test
    fun everyLightThemeHasFullColorCoverage() {
        for (theme in lightThemes) {
            for (attr in referenceAttrs) {
                val name = "theme_color_${attr}_$theme"
                assertTrue("浅色主题 $theme 缺少颜色资源 $name", name in colorNames)
            }
        }
    }

    @Test
    fun everyDarkThemeHasFullColorCoverage() {
        for (theme in darkThemes) {
            for (attr in referenceAttrs) {
                val name = "theme_color_${attr}_$theme"
                assertTrue("深色主题 $theme 缺少颜色资源 $name", name in colorNames)
            }
        }
    }

    @Test
    fun everyLightThemeHasStyle() {
        for (theme in lightThemes) {
            val style = expectedStyle(theme)
            assertTrue("浅色主题 $theme 缺少 style $style", style in styleNames)
        }
    }

    @Test
    fun everyDarkThemeHasStyle() {
        for (theme in darkThemes) {
            val style = expectedStyle(theme)
            assertTrue("深色主题 $theme 缺少 style $style", style in styleNames)
        }
    }

    @Test
    fun amoledWindowAndNavigationSurfacesAreTrulyBlack() {
        for (token in listOf("theme_color_window_background_amoled_dark",
            "theme_color_background_amoled_dark", "theme_color_nav_amoled_dark")) {
            val match = Regex("""<color\s+name="$token">([^<]+)</color>""").find(colorsXml)
            assertTrue("缺少 AMOLED 表面资源 $token", match != null)
            assertEquals("$token 必须纯黑", "#FF000000", match!!.groupValues[1].trim())
        }
    }

    @Test
    fun problemThemesAreFullyCovered() {
        val problem = listOf(
            "oxygen_deep_sea_blue", "oxygen_jade_green", "oxygen_indigo_blue",
            "oxygen_deep_sea_blue_dark", "oxygen_jade_green_dark", "oxygen_indigo_blue_dark",
        )
        for (theme in problem) {
            assertTrue("$theme 必须出现在主题列表中", (theme in lightThemes) || (theme in darkThemes))
            for (attr in referenceAttrs) {
                assertTrue("$theme 缺少颜色资源 theme_color_${attr}_$theme",
                    "theme_color_${attr}_$theme" in colorNames)
            }
            assertTrue("$theme 缺少 style ${expectedStyle(theme)}", expectedStyle(theme) in styleNames)
        }
    }

    @Test
    fun arrayNameCountsMatchValueCounts() {
        val lightNames = arrayItems("themeNames")
        val darkNames = arrayItems("dark_theme_names")
        assertEquals("theme_values 与 themeNames 数量不一致", lightThemes.size, lightNames.size)
        assertEquals("dark_theme_values 与 dark_theme_names 数量不一致", darkThemes.size, darkNames.size)
        assertFalse("theme_values 不应为空", lightThemes.isEmpty())
        assertEquals("浅色主题 ID 必须唯一", lightThemes.size, lightThemes.distinct().size)
        assertEquals("深色主题 ID 必须唯一", darkThemes.size, darkThemes.distinct().size)
        assertTrue("浅色主题显示名不得为空", lightNames.all { it.isNotBlank() })
        assertTrue("深色主题显示名不得为空", darkNames.all { it.isNotBlank() })
        assertTrue(
            "每个浅色预设必须有同 ID 的深色变体",
            lightThemes.all { "${it}_dark" in darkThemes }
        )
    }
}
