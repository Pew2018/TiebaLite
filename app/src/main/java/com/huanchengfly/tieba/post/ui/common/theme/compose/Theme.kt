package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
import com.huanchengfly.tieba.post.ui.common.theme.compose.toArgbInt
import com.huanchengfly.tieba.post.utils.compose.darken

@Stable
data class ExtendedColors(
    val theme: String,
    val isNightMode: Boolean,
    val primary: Color = Color.Unspecified,
    val onPrimary: Color = Color.Unspecified,
    val accent: Color = Color.Unspecified,
    val onAccent: Color = Color.Unspecified,
    val topBar: Color = Color.Unspecified,
    val onTopBar: Color = Color.Unspecified,
    val onTopBarSecondary: Color = Color.Unspecified,
    val onTopBarActive: Color = Color.Unspecified,
    val topBarSurface: Color = Color.Unspecified,
    val onTopBarSurface: Color = Color.Unspecified,
    val bottomBar: Color = Color.Unspecified,
    val bottomBarSurface: Color = Color.Unspecified,
    val onBottomBarSurface: Color = Color.Unspecified,
    val text: Color = Color.Unspecified,
    val textSecondary: Color = Color.Unspecified,
    val textDisabled: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val chip: Color = Color.Unspecified,
    val onChip: Color = Color.Unspecified,
    val unselected: Color = Color.Unspecified,
    val card: Color = Color.Unspecified,
    val floorCard: Color = Color.Unspecified,
    val divider: Color = Color.Unspecified,
    val shadow: Color = Color.Unspecified,
    val indicator: Color = Color.Unspecified,
    val windowBackground: Color = Color.Unspecified,
    val placeholder: Color = Color.Unspecified,
    val statusBar: Color = Color.Unspecified,
    val switchOn: Color = Color.Unspecified,
    val switchOff: Color = Color.Unspecified,
    val fab: Color = Color.Unspecified,
    val selected: Color = Color.Unspecified,
    // 页面角色：设置类顶栏（OxygenOS 深灰，固定）
    val settingsTopBar: Color = Color.Unspecified,
    val settingsStatusBar: Color = Color.Unspecified,
    val onSettingsTopBar: Color = Color.Unspecified,
    val onSettingsTopBarSecondary: Color = Color.Unspecified,
    // 用途色：从种子派生的不同强调用途
    val accentFill: Color = Color.Unspecified,
    val onAccentFill: Color = Color.Unspecified,
    val accentText: Color = Color.Unspecified,
    val accentMuted: Color = Color.Unspecified,
    val onAccentMuted: Color = Color.Unspecified,
    val progressColor: Color = Color.Unspecified,
    val selectedBg: Color = Color.Unspecified,
    val onSelectedBg: Color = Color.Unspecified,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        ThemeUtil.THEME_DEFAULT,
        false,
    )
}


@SuppressLint("ConflictingOnColor")
fun getColorPalette(
    darkTheme: Boolean,
    extendedColors: ExtendedColors
): Colors {
    val primaryColor = extendedColors.accentFill
    val secondaryColor = extendedColors.accentText
    return if (darkTheme) {
        darkColors(
            primary = primaryColor,
            onPrimary = extendedColors.onAccentFill,
            primaryVariant = primaryColor.darken(),
            secondary = secondaryColor,
            onSecondary = extendedColors.onSelectedBg,
            secondaryVariant = extendedColors.accentMuted,
            background = extendedColors.background,
            onBackground = extendedColors.text,
            surface = extendedColors.card,
            onSurface = extendedColors.text,
        )
    } else {
        lightColors(
            primary = primaryColor,
            onPrimary = extendedColors.onAccentFill,
            primaryVariant = primaryColor.darken(),
            secondary = secondaryColor,
            onSecondary = extendedColors.onSelectedBg,
            secondaryVariant = extendedColors.accentMuted,
            background = extendedColors.background,
            onBackground = extendedColors.text,
            surface = extendedColors.card,
            onSurface = extendedColors.text,
        )
    }
}

@Composable
private fun getDynamicColor(
    theme: String,
    tonalPalette: TonalPalette,
): ExtendedColors {
    val isDarkColorPalette = ThemeUtil.isNightMode(theme)
    return if (isDarkColorPalette) {
        if (theme == ThemeUtil.THEME_AMOLED_DARK) {
            getBlackDarkDynamicColor(tonalPalette)
        } else {
            getDarkDynamicColor(tonalPalette)
        }
    } else {
        getLightDynamicColor(tonalPalette)
    }
}

@Composable
private fun getDynamicTopBarColor(
    tonalPalette: TonalPalette,
    isNightMode: Boolean = false,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val primaryColor = tonalPalette.primary40
    val backgroundColor = tonalPalette.neutralVariant99
    return if (topBarUsePrimaryColor) {
        primaryColor
    } else {
        backgroundColor
    }
}

@Composable
private fun getDynamicOnTopBarColor(
    tonalPalette: TonalPalette,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val onPrimaryColor = tonalPalette.primary100
    val onBackgroundColor = tonalPalette.neutralVariant10
    return if (topBarUsePrimaryColor) {
        onPrimaryColor
    } else {
        onBackgroundColor
    }
}

@Composable
private fun getDynamicOnTopBarSecondaryColor(
    tonalPalette: TonalPalette,
    isNightMode: Boolean = false,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val primaryColor = tonalPalette.primary80
    val backgroundColor = tonalPalette.neutralVariant40
    return if (topBarUsePrimaryColor) {
        primaryColor
    } else {
        backgroundColor
    }
}

@Composable
private fun getDynamicOnTopBarActiveColor(
    tonalPalette: TonalPalette,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val primaryColor = tonalPalette.primary100
    val backgroundColor = tonalPalette.neutralVariant0
    return if (topBarUsePrimaryColor) {
        primaryColor
    } else {
        backgroundColor
    }
}

@Composable
private fun getDynamicTopBarSurfaceColor(
    tonalPalette: TonalPalette,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val primaryColor = tonalPalette.primary90
    val backgroundColor = tonalPalette.neutralVariant95
    return if (topBarUsePrimaryColor) {
        primaryColor
    } else {
        backgroundColor
    }
}

@Composable
private fun getDynamicOnTopBarSurfaceColor(
    tonalPalette: TonalPalette,
): Color {
    val topBarUsePrimaryColor =
        LocalContext.current.appPreferences.toolbarPrimaryColor
    val primaryColor = tonalPalette.primary10
    val backgroundColor = tonalPalette.neutralVariant30
    return if (topBarUsePrimaryColor) {
        primaryColor
    } else {
        backgroundColor
    }
}

@Composable
private fun getLightDynamicColor(tonalPalette: TonalPalette): ExtendedColors {
    return ExtendedColors(
        theme = "dynamic",
        isNightMode = false,
        primary = tonalPalette.primary40,
        onPrimary = tonalPalette.primary100,
        accent = tonalPalette.secondary40,
        onAccent = tonalPalette.secondary100,
        topBar = getDynamicTopBarColor(tonalPalette),
        onTopBar = getDynamicOnTopBarColor(tonalPalette),
        onTopBarSecondary = getDynamicOnTopBarSecondaryColor(tonalPalette),
        onTopBarActive = getDynamicOnTopBarActiveColor(tonalPalette),
        topBarSurface = getDynamicTopBarSurfaceColor(tonalPalette),
        onTopBarSurface = getDynamicOnTopBarSurfaceColor(tonalPalette),
        bottomBar = tonalPalette.neutralVariant99,
        bottomBarSurface = tonalPalette.neutralVariant95,
        onBottomBarSurface = tonalPalette.neutralVariant30,
        text = tonalPalette.neutralVariant10,
        textSecondary = tonalPalette.neutralVariant40,
        textDisabled = tonalPalette.neutralVariant70,
        background = tonalPalette.neutralVariant99,
        chip = tonalPalette.neutralVariant95,
        onChip = tonalPalette.neutralVariant40,
        unselected = tonalPalette.neutralVariant60,
        card = tonalPalette.neutralVariant99,
        floorCard = tonalPalette.neutralVariant95,
        divider = tonalPalette.neutralVariant95,
        shadow = tonalPalette.neutralVariant90,
        indicator = tonalPalette.neutralVariant95,
        windowBackground = tonalPalette.neutralVariant99,
        placeholder = tonalPalette.neutralVariant100,
        statusBar = tonalPalette.neutralVariant95,
        settingsTopBar = tonalPalette.neutralVariant95,
        settingsStatusBar = tonalPalette.neutralVariant90,
        onSettingsTopBar = tonalPalette.neutralVariant10,
        onSettingsTopBarSecondary = tonalPalette.neutralVariant30,
        accentFill = tonalPalette.secondary40,
        onAccentFill = tonalPalette.secondary100,
        accentText = tonalPalette.secondary40,
        accentMuted = tonalPalette.secondary90,
        onAccentMuted = tonalPalette.secondary30,
        progressColor = tonalPalette.secondary40,
        selectedBg = tonalPalette.secondary90,
        onSelectedBg = tonalPalette.secondary30,
        switchOn = tonalPalette.secondary40,
        switchOff = if (false) Color(0xFF5A5A5A) else Color(0xFFC4C4C4),
        fab = tonalPalette.secondary40,
        selected = tonalPalette.secondary40,
    )
}

@Composable
private fun getDarkDynamicColor(tonalPalette: TonalPalette): ExtendedColors {
    return ExtendedColors(
        theme = "dynamic",
        isNightMode = true,
        primary = tonalPalette.primary80,
        onPrimary = tonalPalette.primary10,
        accent = tonalPalette.secondary80,
        onAccent = tonalPalette.secondary20,
        topBar = tonalPalette.neutralVariant10,
        onTopBar = tonalPalette.neutralVariant90,
        onTopBarSecondary = tonalPalette.neutralVariant70,
        onTopBarActive = tonalPalette.neutralVariant100,
        topBarSurface = tonalPalette.neutralVariant20,
        onTopBarSurface = tonalPalette.neutralVariant70,
        bottomBar = tonalPalette.neutralVariant10,
        bottomBarSurface = tonalPalette.neutralVariant20,
        onBottomBarSurface = tonalPalette.neutralVariant70,
        text = tonalPalette.neutralVariant90,
        textSecondary = tonalPalette.neutralVariant70,
        textDisabled = tonalPalette.neutralVariant50,
        background = tonalPalette.neutralVariant10,
        chip = tonalPalette.neutralVariant20,
        onChip = tonalPalette.neutralVariant60,
        unselected = tonalPalette.neutralVariant40,
        card = tonalPalette.neutralVariant20,
        floorCard = tonalPalette.neutralVariant20,
        divider = tonalPalette.neutralVariant20,
        shadow = tonalPalette.neutralVariant20,
        indicator = tonalPalette.neutralVariant10,
        windowBackground = tonalPalette.neutralVariant10,
        placeholder = tonalPalette.neutralVariant50,
        statusBar = tonalPalette.neutralVariant0,
        settingsTopBar = tonalPalette.neutralVariant0,
        settingsStatusBar = tonalPalette.neutralVariant0,
        onSettingsTopBar = tonalPalette.neutralVariant90,
        onSettingsTopBarSecondary = tonalPalette.neutralVariant70,
        accentFill = tonalPalette.secondary80,
        onAccentFill = tonalPalette.secondary20,
        accentText = tonalPalette.secondary80,
        accentMuted = tonalPalette.secondary30,
        onAccentMuted = tonalPalette.secondary90,
        progressColor = tonalPalette.secondary80,
        selectedBg = tonalPalette.secondary30,
        onSelectedBg = tonalPalette.secondary90,
        switchOn = tonalPalette.secondary80,
        switchOff = if (true) Color(0xFF5A5A5A) else Color(0xFFC4C4C4),
        fab = tonalPalette.secondary80,
        selected = tonalPalette.secondary80,
    )
}

@Composable
private fun getBlackDarkDynamicColor(tonalPalette: TonalPalette): ExtendedColors {
    return ExtendedColors(
        theme = "dynamic",
        isNightMode = true,
        primary = tonalPalette.primary80,
        onPrimary = tonalPalette.primary10,
        accent = tonalPalette.secondary80,
        onAccent = tonalPalette.secondary20,
        topBar = tonalPalette.neutralVariant0,
        onTopBar = tonalPalette.neutralVariant90,
        onTopBarSecondary = tonalPalette.neutralVariant70,
        onTopBarActive = tonalPalette.neutralVariant100,
        topBarSurface = tonalPalette.neutralVariant10,
        onTopBarSurface = tonalPalette.neutralVariant70,
        bottomBar = tonalPalette.neutralVariant0,
        bottomBarSurface = tonalPalette.neutralVariant10,
        onBottomBarSurface = tonalPalette.neutralVariant70,
        text = tonalPalette.neutralVariant90,
        textSecondary = tonalPalette.neutralVariant70,
        textDisabled = tonalPalette.neutralVariant50,
        background = tonalPalette.neutralVariant0,
        chip = tonalPalette.neutralVariant10,
        onChip = tonalPalette.neutralVariant50,
        unselected = tonalPalette.neutralVariant40,
        card = tonalPalette.neutralVariant10,
        floorCard = tonalPalette.neutralVariant10,
        divider = tonalPalette.neutralVariant10,
        shadow = tonalPalette.neutralVariant10,
        indicator = tonalPalette.neutralVariant10,
        windowBackground = tonalPalette.neutralVariant0,
        placeholder = tonalPalette.neutralVariant50,
        statusBar = tonalPalette.neutralVariant0,
        settingsTopBar = tonalPalette.neutralVariant0,
        settingsStatusBar = tonalPalette.neutralVariant0,
        onSettingsTopBar = tonalPalette.neutralVariant90,
        onSettingsTopBarSecondary = tonalPalette.neutralVariant70,
        accentFill = tonalPalette.secondary80,
        onAccentFill = tonalPalette.secondary20,
        accentText = tonalPalette.secondary80,
        accentMuted = tonalPalette.secondary30,
        onAccentMuted = tonalPalette.secondary90,
        progressColor = tonalPalette.secondary80,
        selectedBg = tonalPalette.secondary30,
        onSelectedBg = tonalPalette.secondary90,
        switchOn = tonalPalette.secondary80,
        switchOff = if (true) Color(0xFF5A5A5A) else Color(0xFFC4C4C4),
        fab = tonalPalette.secondary80,
        selected = tonalPalette.secondary80,
    )
}

@Composable
private fun getThemeColorForTheme(theme: String): ExtendedColors {
    val context = LocalContext.current
    val nowTheme = ThemeUtil.getCurrentTheme(theme)
    val textColor =
        Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorText, nowTheme))
    val bottomBarColor =
        Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorNavBar, nowTheme))
    return ExtendedColors(
        theme = nowTheme,
        isNightMode = ThemeUtil.isNightMode(nowTheme),
        primary = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorNewPrimary,
                nowTheme
            )
        ),
        onPrimary = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorOnAccent,
                nowTheme
            )
        ),
        accent = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorAccent, nowTheme)),
        onAccent = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorOnAccent, nowTheme)),
        topBar = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorToolbar, nowTheme)),
        onTopBar = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorToolbarItem,
                nowTheme
            )
        ),
        onTopBarSecondary = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorToolbarItemSecondary,
                nowTheme
            )
        ),
        onTopBarActive = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorToolbarItemActive,
                nowTheme
            )
        ),
        topBarSurface = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorToolbarSurface,
                nowTheme
            )
        ),
        onTopBarSurface = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorOnToolbarSurface,
                nowTheme
            )
        ),
        bottomBar = bottomBarColor,
        bottomBarSurface = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorNavBarSurface,
                nowTheme
            )
        ),
        onBottomBarSurface = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorOnNavBarSurface,
                nowTheme
            )
        ),
        text = textColor.copy(alpha = ContentAlpha.high),
        textSecondary = textColor.copy(alpha = ContentAlpha.medium),
        textDisabled = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.color_text_disabled,
                nowTheme
            )
        ),
        background = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorBackground,
                nowTheme
            )
        ),
        chip = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorChip, nowTheme)),
        onChip = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorOnChip, nowTheme)),
        unselected = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorUnselected,
                nowTheme
            )
        ),
        card = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorCard, nowTheme)),
        floorCard = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorFloorCard,
                nowTheme
            )
        ),
        divider = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorDivider, nowTheme)),
        shadow = Color(App.ThemeDelegate.getColorByAttr(context, R.attr.shadow_color, nowTheme)),
        indicator = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorIndicator,
                nowTheme
            )
        ),
        windowBackground = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorWindowBackground,
                nowTheme
            )
        ),
        placeholder = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorPlaceholder,
                nowTheme
            )
        ),
        // OxygenOS 派生语义角色：状态栏比工具栏略深，控件色由强调色派生
        statusBar = Color(ThemeUtil.mainStatusBarColor(nowTheme)),
        switchOn = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorAccent,
                nowTheme
            )
        ),
        switchOff = if (ThemeUtil.isNightMode(nowTheme)) Color(0xFF5A5A5A) else Color(0xFFC4C4C4),
        fab = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorAccent,
                nowTheme
            )
        ),
        selected = Color(
            App.ThemeDelegate.getColorByAttr(
                context,
                R.attr.colorIndicator,
                nowTheme
            )
        ),
        // 页面角色：设置类顶栏固定 OxygenOS 深灰
        settingsTopBar = Color(ThemeUtil.settingsToolbarColor(nowTheme)),
        settingsStatusBar = Color(ThemeUtil.settingsStatusBarColor(nowTheme)),
        onSettingsTopBar = Color(ThemeUtil.onToolbarColor(ThemeUtil.settingsToolbarColor(nowTheme))),
        onSettingsTopBarSecondary = Color(ThemeUtil.onToolbarColor(ThemeUtil.settingsToolbarColor(nowTheme)))
            .copy(alpha = 0.7f),
        // 用途色：从种子派生，全部经过对比度核查
        accentFill = fab,
        onAccentFill = Color(ThemeColorMath.readableOnInt(fab.toArgbInt())),
        accentText = Color(
            ThemeColorMath.readableAccentTextInt(
                fab.toArgbInt(),
                Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorBackground,
                        nowTheme
                    )
                ).toArgbInt()
            )
        ),
        accentMuted = Color(
            ThemeColorMath.mutedAccentBgInt(
                fab.toArgbInt(),
                Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorBackground,
                        nowTheme
                    )
                ).toArgbInt()
            )
        ),
        onAccentMuted = Color(
            ThemeColorMath.onMutedAccentBgInt(
                fab.toArgbInt(),
                Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorBackground,
                        nowTheme
                    )
                ).toArgbInt()
            )
        ),
        progressColor = fab,
        selectedBg = Color(
            ThemeColorMath.mutedAccentBgInt(
                fab.toArgbInt(),
                Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorBackground,
                        nowTheme
                    )
                ).toArgbInt()
            )
        ),
        onSelectedBg = Color(
            ThemeColorMath.onMutedAccentBgInt(
                fab.toArgbInt(),
                Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorBackground,
                        nowTheme
                    )
                ).toArgbInt()
            )
        ),
    )
}

@Composable
fun TiebaLiteTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val theme by remember { ThemeUtil.themeState }
    val isDynamicTheme by rememberPreferenceAsState(
        key = booleanPreferencesKey("useDynamicColorTheme"),
        defaultValue = false
    )
    val isDarkColorPalette by remember {
        derivedStateOf {
            ThemeUtil.isNightMode(theme)
                    || (ThemeUtil.isTranslucentTheme(theme) && theme.contains("light"))
        }
    }

    val useDynamicTheme = isDynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val extendedColors = if (!useDynamicTheme || ThemeUtil.isTranslucentTheme(theme)) {
        getThemeColorForTheme(theme)
    } else {
        getDynamicColor(theme, dynamicTonalPalette(context))
    }

    val colors = getColorPalette(isDarkColorPalette, extendedColors)

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object ExtendedTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
