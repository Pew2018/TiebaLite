package com.huanchengfly.tieba.post.ui.common.theme

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.huanchengfly.tieba.post.R

/**
 * 经典 Material Design 风格主题预设。
 *
 * 单一数据源：主题选择页（AppThemePage）、夜间模式列表（CustomSettingsPage）、
 * 主题值映射（ThemeUtil）与资源解析（App.ThemeDelegate）都基于这里的 id。
 *
 * 每个预设包含：
 * - 浅色样式 [lightStyle]：深色平直工具栏 #212121 + 浅色内容 #FAFAFA + 种子强调色
 * - 深色样式 [darkStyle]：深色中性面 + 提亮后的种子强调色（夜间可读性）
 */
enum class ThemeGroup(@StringRes val titleRes: Int) {
    OXYGEN(R.string.theme_group_oxygen),
    MATERIAL(R.string.theme_group_material),
}

data class ThemePreset(
    val id: String,
    val name: String,
    val group: ThemeGroup,
    @ColorInt val seedColor: Int,
    @ColorInt val onSeedColor: Int,
) {
    val darkId: String get() = "${id}_dark"
}

object ThemePresets {
    private const val OXYGEN_PREFIX = "oxygen_"
    private const val MATERIAL_PREFIX = "material_"

    val presets: List<ThemePreset> = listOf(
        // 经典配色列
        ThemePreset("oxygen_blue", "晴空蓝", ThemeGroup.OXYGEN, Color.parseColor("#FF42A5F5"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_deep_sea_blue", "深海蓝", ThemeGroup.OXYGEN, Color.parseColor("#FF0071CD"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("oxygen_golden", "流金橙", ThemeGroup.OXYGEN, Color.parseColor("#FFCC6F4E"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_lemon_yellow", "柠檬黄", ThemeGroup.OXYGEN, Color.parseColor("#FFE6A545"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_grass_green", "青草绿", ThemeGroup.OXYGEN, Color.parseColor("#FF7DC22F"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_jade_green", "碧玉绿", ThemeGroup.OXYGEN, Color.parseColor("#FF01998C"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("oxygen_charm_purple", "魅力紫", ThemeGroup.OXYGEN, Color.parseColor("#FF9575CD"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_indigo_blue", "靛青蓝", ThemeGroup.OXYGEN, Color.parseColor("#FF3F3DA2"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("oxygen_sky_blue", "天空蓝", ThemeGroup.OXYGEN, Color.parseColor("#FF26C6DA"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_vigour_red", "活力红", ThemeGroup.OXYGEN, Color.parseColor("#FFF06292"), Color.parseColor("#FF212121")),
        ThemePreset("oxygen_fashion_pink", "时尚粉", ThemeGroup.OXYGEN, Color.parseColor("#FFBA68C8"), Color.parseColor("#FF212121")),
        // Material 风格列（与经典配色列的两个 Blue 为不同色值，均为独立选项）
        ThemePreset("material_blue", "蓝", ThemeGroup.MATERIAL, Color.parseColor("#FF2196F3"), Color.parseColor("#FF212121")),
        ThemePreset("material_teal", "青绿", ThemeGroup.MATERIAL, Color.parseColor("#FF009688"), Color.parseColor("#FF212121")),
        ThemePreset("material_green", "绿", ThemeGroup.MATERIAL, Color.parseColor("#FF4CAF50"), Color.parseColor("#FF212121")),
        ThemePreset("material_red", "红", ThemeGroup.MATERIAL, Color.parseColor("#FFF44336"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("material_orange", "橙", ThemeGroup.MATERIAL, Color.parseColor("#FFFF9800"), Color.parseColor("#FF212121")),
        ThemePreset("material_purple", "紫", ThemeGroup.MATERIAL, Color.parseColor("#FF9C27B0"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("material_cyan", "青", ThemeGroup.MATERIAL, Color.parseColor("#FF00BCD4"), Color.parseColor("#FF212121")),
        ThemePreset("material_indigo", "靛蓝", ThemeGroup.MATERIAL, Color.parseColor("#FF3F51B5"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("material_pink", "粉", ThemeGroup.MATERIAL, Color.parseColor("#FFE91E63"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("material_blue_grey", "蓝灰", ThemeGroup.MATERIAL, Color.parseColor("#FF607D8B"), Color.parseColor("#FFFFFFFF")),
        ThemePreset("material_deep_orange", "深橙", ThemeGroup.MATERIAL, Color.parseColor("#FFFF5722"), Color.parseColor("#FF212121")),
        ThemePreset("material_light_green", "浅绿", ThemeGroup.MATERIAL, Color.parseColor("#FF8BC34A"), Color.parseColor("#FF212121")),
    )

    val byId: Map<String, ThemePreset> = presets.associateBy { it.id }

    /** 全部浅色主题值（顺序即界面顺序） */
    val lightIds: List<String> = presets.map { it.id }

    /** 全部深色主题值 */
    val darkIds: List<String> = presets.map { it.darkId }

    /** 全部合法预设 ID（浅色 + 深色，随上方预设表自动同步，不再手写固定数量）。 */
    val allIds: List<String> = lightIds + darkIds

    /**
     * 是否为合法预设 ID（浅色或深色）。
     * 主题 ID 解析的唯一入口：Compose、View、系统栏与偏好层都必须得到同一结果。
     */
    fun isPreset(theme: String?): Boolean =
        theme != null && (byId.containsKey(theme) || isPresetDark(theme))

    /** 是否为浅色预设 ID */
    fun isPresetLight(theme: String?): Boolean = theme != null && byId.containsKey(theme)

    /** 是否为深色预设 ID */
    fun isPresetDark(theme: String?): Boolean =
        theme != null && theme.endsWith("_dark") && byId.containsKey(theme.removeSuffix("_dark"))

    fun presetOf(theme: String?): ThemePreset? = theme?.let { byId[it.removeSuffix("_dark")] }

    /**
     * 旧版主题值 -> 新预设的稳定迁移映射。
     * 旧值仍可在 DataStore 中被已升级用户保留，读取时映射到最近的新预设。
     */
    val legacyMigration: Map<String, String> = mapOf(
        "tieba" to "material_blue",
        "blue" to "oxygen_blue",
        "black" to "material_blue_grey",
        "pink" to "oxygen_fashion_pink",
        "red" to "material_red",
        "purple" to "material_purple",
        "white" to "material_blue",
        "blue_dark" to "oxygen_blue_dark",
        "grey_dark" to "material_blue_grey_dark",
        "dark" to "oxygen_blue_dark",
    )

    /** attr 资源名（App.ThemeDelegate 的 theme_color_*_&lt;theme&gt; 名称解析机制） */
    fun attrName(attrId: Int): String? = ATTR_NAMES[attrId]

    private val ATTR_NAMES = mapOf(
        R.attr.colorPrimary to "primary",
        R.attr.colorNewPrimary to "new_primary",
        R.attr.colorAccent to "accent",
        R.attr.colorOnAccent to "on_accent",
        R.attr.colorToolbar to "toolbar",
        R.attr.colorToolbarItem to "toolbar_item",
        R.attr.colorToolbarItemActive to "toolbar_item_active",
        R.attr.colorToolbarItemSecondary to "toolbar_item_secondary",
        R.attr.colorToolbarSurface to "toolbar_surface",
        R.attr.colorOnToolbarSurface to "on_toolbar_surface",
        R.attr.colorNavBar to "nav",
        R.attr.colorNavBarSurface to "nav_bar_surface",
        R.attr.colorOnNavBarSurface to "on_nav_bar_surface",
        R.attr.colorText to "text",
        R.attr.colorTextSecondary to "text_secondary",
        R.attr.color_text_disabled to "text_disabled",
        R.attr.colorTextOnPrimary to "text_on_primary",
        R.attr.colorBackground to "background",
        R.attr.colorWindowBackground to "window_background",
        R.attr.colorChip to "chip",
        R.attr.colorOnChip to "on_chip",
        R.attr.colorUnselected to "unselected",
        R.attr.colorFloorCard to "floor_card",
        R.attr.colorCard to "card",
        R.attr.colorDivider to "divider",
        R.attr.shadow_color to "shadow",
        R.attr.colorIndicator to "indicator",
        R.attr.colorPlaceholder to "placeholder",
    )
}
