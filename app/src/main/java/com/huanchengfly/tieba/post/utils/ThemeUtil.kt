package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.google.android.material.appbar.AppBarLayout
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.interfaces.BackgroundTintable
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.common.theme.ThemePresets
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.theme.TintSwipeRefreshLayout
import kotlinx.coroutines.launch
import java.util.Locale

object ThemeUtil {
    val themeState: MutableState<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) migrateLegacyTheme(
                dataStore.getString(KEY_THEME, THEME_DEFAULT),
                KEY_THEME
            )
            else THEME_DEFAULT
        )
    }

    /**
     * 旧版主题值 -> 新预设的纯映射（无任何写入副作用）。
     * 未知 ID 原样返回，由调用方决定稳定回退。
     */
    fun mapLegacyTheme(theme: String): String = LEGACY_THEME_MIGRATION[theme] ?: theme

    /**
     * 旧版主题值 -> 新预设的一次性迁移。
     * 已安装升级用户存储的 tieba/blue/black/pink/red/purple/blue_dark/grey_dark 等值
     * 会被映射到最近的 OxygenOS 预设并写回，避免升级后主题失效或崩溃。
     *
     * 重要：迁移结果只写回来源 key（[key]），不得在读取 dark_theme/old_theme 时
     * 改写活动主题（KEY_THEME）。
     */
    fun migrateLegacyTheme(theme: String, key: String = KEY_THEME): String {
        val mapped = mapLegacyTheme(theme)
        if (mapped != theme) {
            dataStore.putString(key, mapped)
        }
        return mapped
    }

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"
    const val KEY_USE_DYNAMIC_THEME = "useDynamicColorTheme"
    const val KEY_SWITCH_REASON = "switch_reason"
    const val KEY_TRANSLUCENT_PRIMARY_COLOR = "translucent_primary_color"
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color"
    const val KEY_TRANSLUCENT_THEME_BACKGROUND_PATH = "translucent_theme_background_path"
    const val KEY_USE_DYNAMIC_COLOR_THEME = "useDynamicColorTheme"
    const val KEY_MAIN_TOOLBAR_STYLE = "main_toolbar_style"

    /** 主功能页面顶栏风格（设置类页面固定 OxygenOS 深灰，不随此偏好变化） */
    const val MAIN_TOOLBAR_APP_BLUE = "app_blue"
    const val MAIN_TOOLBAR_TEAL = "teal"
    const val MAIN_TOOLBAR_PURPLE = "purple"
    const val MAIN_TOOLBAR_NEUTRAL_DARK = "neutral_dark"
    const val MAIN_TOOLBAR_FOLLOW_SEED = "follow_seed"

    val MAIN_TOOLBAR_STYLES = listOf(
        MAIN_TOOLBAR_APP_BLUE,
        MAIN_TOOLBAR_TEAL,
        MAIN_TOOLBAR_PURPLE,
        MAIN_TOOLBAR_NEUTRAL_DARK,
        MAIN_TOOLBAR_FOLLOW_SEED,
    )

    /** 当前主功能顶栏风格（持久化，默认经典一加应用蓝） */
    fun mainToolbarStyle(): String = dataStore.getString(KEY_MAIN_TOOLBAR_STYLE, MAIN_TOOLBAR_APP_BLUE)

    fun setMainToolbarStyle(style: String) {
        dataStore.putString(KEY_MAIN_TOOLBAR_STYLE, style)
    }

    /**
     * 主功能页面顶栏色。默认经典一加应用蓝 #0071CD（状态栏由其压暗得到 #0067BC 一档），
     * 另提供青绿/紫/中性深灰/跟随种子派生，均经过前景对比度核查。
     */
    @ColorInt
    fun mainToolbarColor(theme: String): Int {
        if (isTranslucentTheme(theme)) return android.graphics.Color.TRANSPARENT
        val night = isNightMode(theme)
        return when (mainToolbarStyle()) {
            MAIN_TOOLBAR_TEAL -> if (night) 0xFF016B61.toInt() else 0xFF01998C.toInt()
            MAIN_TOOLBAR_PURPLE -> if (night) 0xFF37368F.toInt() else 0xFF3F3DA2.toInt()
            MAIN_TOOLBAR_NEUTRAL_DARK -> if (night) 0xFF1E1E1E.toInt() else 0xFF212121.toInt()
            MAIN_TOOLBAR_FOLLOW_SEED -> {
                val seed = App.ThemeDelegate.getColorByAttr(INSTANCE, R.attr.colorPrimary, theme)
                if (night) com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
                    .darkenInt(seed, 0.25f) else seed
            }

            else -> if (night) 0xFF005FA3.toInt() else 0xFF0071CD.toInt()
        }
    }

    /** 主功能页面状态栏色（比顶栏略深一档，还原 2017 年 OxygenOS 的上下层次） */
    @ColorInt
    fun mainStatusBarColor(theme: String): Int =
        com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
            .darkenInt(mainToolbarColor(theme), 0.08f)

    /** 设置类页面顶栏色：OxygenOS 深灰 */
    @ColorInt
    fun settingsToolbarColor(theme: String): Int =
        if (isNightMode(theme)) 0xFF1E1E1E.toInt() else 0xFF212121.toInt()

    @ColorInt
    fun settingsStatusBarColor(theme: String): Int =
        com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
            .darkenInt(settingsToolbarColor(theme), 0.08f)

    /** 顶栏前景（标题/图标）：按对比度自动选择白字或深字 */
    @ColorInt
    fun onToolbarColor(@ColorInt toolbarColor: Int): Int =
        com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
            .readableOnInt(toolbarColor)

    const val THEME_TRANSLUCENT = "translucent"
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"
    const val THEME_CUSTOM = "custom"
    const val THEME_DEFAULT = "material_blue"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    // OxygenOS 7.1.1 经典风格预设（与 ThemePresets.kt 单一数据源对应）
    const val THEME_OXYGEN_BLUE = "oxygen_blue"
    const val THEME_OXYGEN_GOLDEN = "oxygen_golden"
    const val THEME_OXYGEN_LEMON_YELLOW = "oxygen_lemon_yellow"
    const val THEME_OXYGEN_GRASS_GREEN = "oxygen_grass_green"
    const val THEME_OXYGEN_CHARM_PURPLE = "oxygen_charm_purple"
    const val THEME_OXYGEN_SKY_BLUE = "oxygen_sky_blue"
    const val THEME_OXYGEN_VIGOUR_RED = "oxygen_vigour_red"
    const val THEME_OXYGEN_FASHION_PINK = "oxygen_fashion_pink"
    const val THEME_MATERIAL_BLUE = "material_blue"
    const val THEME_MATERIAL_TEAL = "material_teal"
    const val THEME_MATERIAL_GREEN = "material_green"
    const val THEME_MATERIAL_RED = "material_red"
    const val THEME_MATERIAL_ORANGE = "material_orange"
    const val THEME_MATERIAL_PURPLE = "material_purple"
    const val THEME_MATERIAL_CYAN = "material_cyan"
    const val THEME_MATERIAL_INDIGO = "material_indigo"
    const val THEME_MATERIAL_PINK = "material_pink"
    const val THEME_MATERIAL_BLUE_GREY = "material_blue_grey"
    const val THEME_MATERIAL_DEEP_ORANGE = "material_deep_orange"
    const val THEME_MATERIAL_LIGHT_GREEN = "material_light_green"
    const val THEME_OXYGEN_BLUE_DARK = "oxygen_blue_dark"
    const val THEME_OXYGEN_GOLDEN_DARK = "oxygen_golden_dark"
    const val THEME_OXYGEN_LEMON_YELLOW_DARK = "oxygen_lemon_yellow_dark"
    const val THEME_OXYGEN_GRASS_GREEN_DARK = "oxygen_grass_green_dark"
    const val THEME_OXYGEN_CHARM_PURPLE_DARK = "oxygen_charm_purple_dark"
    const val THEME_OXYGEN_SKY_BLUE_DARK = "oxygen_sky_blue_dark"
    const val THEME_OXYGEN_VIGOUR_RED_DARK = "oxygen_vigour_red_dark"
    const val THEME_OXYGEN_FASHION_PINK_DARK = "oxygen_fashion_pink_dark"
    const val THEME_MATERIAL_BLUE_DARK = "material_blue_dark"
    const val THEME_MATERIAL_TEAL_DARK = "material_teal_dark"
    const val THEME_MATERIAL_GREEN_DARK = "material_green_dark"
    const val THEME_MATERIAL_RED_DARK = "material_red_dark"
    const val THEME_MATERIAL_ORANGE_DARK = "material_orange_dark"
    const val THEME_MATERIAL_PURPLE_DARK = "material_purple_dark"
    const val THEME_MATERIAL_CYAN_DARK = "material_cyan_dark"
    const val THEME_MATERIAL_INDIGO_DARK = "material_indigo_dark"
    const val THEME_MATERIAL_PINK_DARK = "material_pink_dark"
    const val THEME_MATERIAL_BLUE_GREY_DARK = "material_blue_grey_dark"
    const val THEME_MATERIAL_DEEP_ORANGE_DARK = "material_deep_orange_dark"
    const val THEME_MATERIAL_LIGHT_GREEN_DARK = "material_light_green_dark"

    /** 旧版主题值迁移映射（读取时一次性迁移，见 [migrateLegacyTheme]） */
    val LEGACY_THEME_MIGRATION: Map<String, String> = ThemePresets.legacyMigration

    const val TRANSLUCENT_THEME_LIGHT = 0
    const val TRANSLUCENT_THEME_DARK = 1

    private val context: Context
        get() = INSTANCE

    val dataStore: DataStore<Preferences>
        get() = INSTANCE.dataStore

    fun getTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorText)
    }

    @JvmStatic
    fun getSecondaryTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorTextSecondary)
    }

    fun switchToNightMode(context: Activity) {
        switchToNightMode(context, true)
    }

    private fun refreshUI(activity: Activity?) {
        if (activity is BaseActivity) {
            activity.refreshUIIfNeed()
            return
        }
        ThemeUtils.refreshUI(activity)
    }

    private fun getOldTheme(): String {
        val oldTheme =
            migrateLegacyTheme(
                dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT),
                KEY_OLD_THEME
            ).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        if (recordOldTheme) {
            val oldTheme = getRawTheme()
            if (!isNightMode(oldTheme)) {
                dataStore.putString(KEY_OLD_THEME, oldTheme)
            }
        }
        dataStore.putString(KEY_THEME, newTheme)
        themeState.value = newTheme
    }

    fun switchDynamicTheme() {
        val currentUseDynamicTheme = dataStore.getBoolean(KEY_USE_DYNAMIC_THEME, false)
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, !currentUseDynamicTheme)
    }

    fun setUseDynamicTheme(useDynamicTheme: Boolean) {
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, useDynamicTheme)
    }

    fun isUsingDynamicTheme(): Boolean {
        return context.appPreferences.useDynamicColorTheme
    }

    fun switchNightMode() {
        if (isNightMode()) {
            switchTheme(getOldTheme(), recordOldTheme = false)
        } else {
            switchTheme(
                migrateLegacyTheme(
                    dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK),
                    KEY_DARK_THEME
                )
            )
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(
            migrateLegacyTheme(
                dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK),
                KEY_DARK_THEME
            )
        )
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmStatic
    fun setChipThemeByLevel(level: String?, parent: View, vararg textViews: TextView) {
        setChipTheme(Util.getIconColorByLevel(level), parent, *textViews)
    }

    fun setChipTheme(@ColorInt color: Int, parent: View, vararg textViews: TextView) {
        parent.backgroundTintList = ColorStateList.valueOf(color)
        for (textView in textViews) {
            textView.setTextColor(ThemeUtils.getColorByAttr(parent.context, R.attr.colorOnAccent))
        }
    }

    @JvmStatic
    fun setThemeForSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        if (swipeRefreshLayout is TintSwipeRefreshLayout) {
            swipeRefreshLayout.tint()
            return
        }
        val context = swipeRefreshLayout.context
        val resources = context.resources
        if (resources != null) {
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.color_swipe_refresh_bg))
            swipeRefreshLayout.setColorSchemeColors(
                ThemeUtils.getColorByAttr(
                    context,
                    R.attr.colorAccent
                )
            )
        }
    }

    @JvmStatic
    fun isNightMode(): Boolean {
        return isNightMode(getRawTheme())
    }

    @JvmStatic
    fun isNightMode(theme: String): Boolean {
        return theme.lowercase(Locale.getDefault()).contains("dark") && !theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isTranslucentTheme(): Boolean {
        return isTranslucentTheme(getRawTheme())
    }

    @JvmStatic
    fun isTranslucentTheme(theme: String): Boolean {
        return theme.equals(
            THEME_TRANSLUCENT,
            ignoreCase = true
        ) || theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isDynamicTheme(theme: String): Boolean {
        return theme.endsWith("_dynamic")
    }

    fun isStatusBarFontDark(): Boolean {
        val theme = getRawTheme()
        val isToolbarPrimaryColor: Boolean = INSTANCE.appPreferences.toolbarPrimaryColor
        if (ThemePresets.isPreset(theme)) {
            // 预设：状态栏文字明暗由主功能顶栏实际颜色的对比度决定
            // （深色/彩色顶栏 → 浅色文字；浅色顶栏 → 深色文字）
            return onToolbarColor(mainToolbarColor(theme)) != 0xFFFFFFFF.toInt()
        }
        return if (theme == THEME_CUSTOM) {
            INSTANCE.appPreferences.customStatusBarFontDark
        } else if (isTranslucentTheme(theme)) {
            theme.contains("dark", ignoreCase = true)
        } else if (!isToolbarPrimaryColor) {
            !isNightMode(theme)
        } else {
            false
        }
    }

    fun isNavigationBarFontDark(): Boolean {
        return !isNightMode()
    }

    fun setTheme(context: Activity) {
        val nowTheme = getCurrentTheme()
        context.setTheme(getThemeByName(nowTheme))
    }

    @JvmOverloads
    fun getCurrentTheme(
        theme: String = getRawTheme(),
        checkDynamic: Boolean = false,
    ): String {
        var nowTheme = theme
        if (isTranslucentTheme(nowTheme)) {
            val colorTheme =
                dataStore.getInt("translucent_background_theme", TRANSLUCENT_THEME_LIGHT)
            nowTheme = if (colorTheme == TRANSLUCENT_THEME_DARK) {
                THEME_TRANSLUCENT_DARK
            } else {
                THEME_TRANSLUCENT_LIGHT
            }
        } else if (checkDynamic && isUsingDynamicTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nowTheme = "${nowTheme}_dynamic"
        }
        return nowTheme
    }

    @JvmStatic
    fun setTranslucentThemeWebViewBackground(webView: WebView?) {
        if (webView == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        webView.setBackgroundColor(Color.WHITE)
    }

    private fun setAppBarFitsSystemWindow(view: View?, appBarFitsSystemWindow: Boolean) {
        if (view == null) return
        if (view is AppBarLayout) {
            view.setFitsSystemWindows(appBarFitsSystemWindow)
            view.clipToPadding = !appBarFitsSystemWindow
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setAppBarFitsSystemWindow(view.getChildAt(i), appBarFitsSystemWindow)
            }
        }
    }

    fun setTranslucentBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(Color.TRANSPARENT)
    }

    @JvmStatic
    fun setTranslucentDialogBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(
            ThemeUtils.getColorById(
                view.context,
                R.color.theme_color_card_grey_dark
            )
        )
    }

    fun setTranslucentThemeBackground(
        activity: BaseActivity,
        view: View?,
        setFitsSystemWindow: Boolean = true,
        useCache: Boolean = false,
    ) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            if (setFitsSystemWindow) {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = false
                (view as ViewGroup).clipToPadding = true
            }
            return
        }
        if (setFitsSystemWindow) {
            if (view is CoordinatorLayout) {
                setAppBarFitsSystemWindow(view, true)
                view.setFitsSystemWindows(false)
                (view as ViewGroup).clipToPadding = true
            } else {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = true
                (view as ViewGroup).clipToPadding = false
            }
        }
        view.backgroundTintList = null
        if (view is BackgroundTintable) {
            (view as BackgroundTintable).setBackgroundTintResId(0)
        }
        val backgroundFilePath = dataStore.getString(KEY_TRANSLUCENT_THEME_BACKGROUND_PATH)
        if (backgroundFilePath == null) {
            view.setBackgroundColor(Color.BLACK)
            return
        }
        if (useCache && translucentBackground != null &&
            (translucentBackground !is BitmapDrawable || !(translucentBackground as BitmapDrawable).bitmap.isRecycled)
        ) {
            view.background = translucentBackground
            return
        }
        activity.launch {
            val result = DisplayRequest(activity, newFileUri(backgroundFilePath)) {
                resizeScale(Scale.CENTER_CROP)
            }.execute()
            if (result is DisplayResult.Success) {
                if (useCache) {
                    translucentBackground = result.drawable
                }
                view.background = result.drawable
            } else {
                view.setBackgroundColor(Color.BLACK)
            }
        }
    }

    @StyleRes
    private fun getThemeByName(themeName: String): Int {
        return when (themeName.lowercase(Locale.getDefault())) {
            THEME_OXYGEN_BLUE, -> R.style.TiebaLite_Preset_Oxygen_Blue
            THEME_OXYGEN_GOLDEN, -> R.style.TiebaLite_Preset_Oxygen_Golden
            THEME_OXYGEN_LEMON_YELLOW, -> R.style.TiebaLite_Preset_Oxygen_Lemon_Yellow
            THEME_OXYGEN_GRASS_GREEN, -> R.style.TiebaLite_Preset_Oxygen_Grass_Green
            THEME_OXYGEN_CHARM_PURPLE, -> R.style.TiebaLite_Preset_Oxygen_Charm_Purple
            THEME_OXYGEN_SKY_BLUE, -> R.style.TiebaLite_Preset_Oxygen_Sky_Blue
            THEME_OXYGEN_VIGOUR_RED, -> R.style.TiebaLite_Preset_Oxygen_Vigour_Red
            THEME_OXYGEN_FASHION_PINK, -> R.style.TiebaLite_Preset_Oxygen_Fashion_Pink
            THEME_OXYGEN_BLUE_DARK, -> R.style.TiebaLite_Preset_Oxygen_Blue_Dark
            THEME_OXYGEN_GOLDEN_DARK, -> R.style.TiebaLite_Preset_Oxygen_Golden_Dark
            THEME_OXYGEN_LEMON_YELLOW_DARK, -> R.style.TiebaLite_Preset_Oxygen_Lemon_Yellow_Dark
            THEME_OXYGEN_GRASS_GREEN_DARK, -> R.style.TiebaLite_Preset_Oxygen_Grass_Green_Dark
            THEME_OXYGEN_CHARM_PURPLE_DARK, -> R.style.TiebaLite_Preset_Oxygen_Charm_Purple_Dark
            THEME_OXYGEN_SKY_BLUE_DARK, -> R.style.TiebaLite_Preset_Oxygen_Sky_Blue_Dark
            THEME_OXYGEN_VIGOUR_RED_DARK, -> R.style.TiebaLite_Preset_Oxygen_Vigour_Red_Dark
            THEME_OXYGEN_FASHION_PINK_DARK, -> R.style.TiebaLite_Preset_Oxygen_Fashion_Pink_Dark
            THEME_MATERIAL_BLUE, -> R.style.TiebaLite_Preset_Material_Blue
            THEME_MATERIAL_TEAL, -> R.style.TiebaLite_Preset_Material_Teal
            THEME_MATERIAL_GREEN, -> R.style.TiebaLite_Preset_Material_Green
            THEME_MATERIAL_RED, -> R.style.TiebaLite_Preset_Material_Red
            THEME_MATERIAL_ORANGE, -> R.style.TiebaLite_Preset_Material_Orange
            THEME_MATERIAL_PURPLE, -> R.style.TiebaLite_Preset_Material_Purple
            THEME_MATERIAL_CYAN, -> R.style.TiebaLite_Preset_Material_Cyan
            THEME_MATERIAL_INDIGO, -> R.style.TiebaLite_Preset_Material_Indigo
            THEME_MATERIAL_PINK, -> R.style.TiebaLite_Preset_Material_Pink
            THEME_MATERIAL_BLUE_GREY, -> R.style.TiebaLite_Preset_Material_Blue_Grey
            THEME_MATERIAL_DEEP_ORANGE, -> R.style.TiebaLite_Preset_Material_Deep_Orange
            THEME_MATERIAL_LIGHT_GREEN, -> R.style.TiebaLite_Preset_Material_Light_Green
            THEME_MATERIAL_BLUE_DARK, -> R.style.TiebaLite_Preset_Material_Blue_Dark
            THEME_MATERIAL_TEAL_DARK, -> R.style.TiebaLite_Preset_Material_Teal_Dark
            THEME_MATERIAL_GREEN_DARK, -> R.style.TiebaLite_Preset_Material_Green_Dark
            THEME_MATERIAL_RED_DARK, -> R.style.TiebaLite_Preset_Material_Red_Dark
            THEME_MATERIAL_ORANGE_DARK, -> R.style.TiebaLite_Preset_Material_Orange_Dark
            THEME_MATERIAL_PURPLE_DARK, -> R.style.TiebaLite_Preset_Material_Purple_Dark
            THEME_MATERIAL_CYAN_DARK, -> R.style.TiebaLite_Preset_Material_Cyan_Dark
            THEME_MATERIAL_INDIGO_DARK, -> R.style.TiebaLite_Preset_Material_Indigo_Dark
            THEME_MATERIAL_PINK_DARK, -> R.style.TiebaLite_Preset_Material_Pink_Dark
            THEME_MATERIAL_BLUE_GREY_DARK, -> R.style.TiebaLite_Preset_Material_Blue_Grey_Dark
            THEME_MATERIAL_DEEP_ORANGE_DARK, -> R.style.TiebaLite_Preset_Material_Deep_Orange_Dark
            THEME_MATERIAL_LIGHT_GREEN_DARK, -> R.style.TiebaLite_Preset_Material_Light_Green_Dark
            THEME_TRANSLUCENT, THEME_TRANSLUCENT_LIGHT -> R.style.TiebaLite_Translucent_Light
            THEME_TRANSLUCENT_DARK -> R.style.TiebaLite_Translucent_Dark
            THEME_DEFAULT -> R.style.TiebaLite_Tieba
            THEME_BLACK -> R.style.TiebaLite_Black
            THEME_PURPLE -> R.style.TiebaLite_Purple
            THEME_PINK -> R.style.TiebaLite_Pink
            THEME_RED -> R.style.TiebaLite_Red
            THEME_BLUE_DARK -> R.style.TiebaLite_Dark_Blue
            THEME_GREY_DARK -> R.style.TiebaLite_Dark_Grey
            THEME_AMOLED_DARK -> R.style.TiebaLite_Dark_Amoled
            THEME_CUSTOM -> R.style.TiebaLite_Custom
            else -> R.style.TiebaLite_Tieba
        }
    }

    @JvmStatic
    fun getRawTheme(): String {
        val theme = themeState.value
        return when (theme.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT,
            THEME_TRANSLUCENT_LIGHT,
            THEME_TRANSLUCENT_DARK,
            THEME_CUSTOM,
            THEME_AMOLED_DARK,
            -> theme.lowercase(Locale.getDefault())

            else -> if (ThemePresets.isPreset(theme)) theme.lowercase(Locale.getDefault())
            else THEME_DEFAULT
        }
    }
}