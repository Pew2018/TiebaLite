package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.components.dialogs.CustomThemeDialog
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalPageRole
import com.huanchengfly.tieba.post.ui.common.theme.compose.PageRole
import com.huanchengfly.tieba.post.ui.common.theme.ThemeGroup
import com.huanchengfly.tieba.post.ui.common.theme.ThemePreset
import com.huanchengfly.tieba.post.ui.common.theme.ThemePresets
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun AppThemePage(
    navigator: DestinationsNavigator,
) {
    // 设置类页面角色：顶栏/状态栏使用 OxygenOS 深灰
    CompositionLocalProvider(LocalPageRole provides PageRole.SETTINGS) {
        AppThemeContent(navigator = navigator)
    }
}

@Composable
private fun AppThemeContent(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val currentTheme by remember { ThemeUtil.themeState }
    val isDynamicTheme by rememberPreferenceAsState(
        key = booleanPreferencesKey("useDynamicColorTheme"),
        defaultValue = false
    )
    val mainToolbarStyle by rememberPreferenceAsState(
        key = stringPreferencesKey(ThemeUtil.KEY_MAIN_TOOLBAR_STYLE),
        defaultValue = ThemeUtil.MAIN_TOOLBAR_APP_BLUE
    )
    val customPrimaryColorDialogState = rememberDialogState()
    var customPrimaryColor by remember {
        mutableStateOf(
            Color(
                App.ThemeDelegate.getColorByAttr(
                    context,
                    R.attr.colorPrimary,
                    ThemeUtil.THEME_CUSTOM
                )
            )
        )
    }
    var customToolbarPrimaryColor by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR),
        defaultValue = false
    )
    var customStatusBarFontDark by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_STATUS_BAR_FONT_DARK),
        defaultValue = false
    )

    Dialog(
        dialogState = customPrimaryColorDialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_theme)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_finish),
                onClick = {
                    customStatusBarFontDark = customStatusBarFontDark || !customToolbarPrimaryColor
                    context.appPreferences.customPrimaryColor =
                        CustomThemeDialog.toString(customPrimaryColor.toArgb())
                    context.appPreferences.toolbarPrimaryColor = customToolbarPrimaryColor
                    context.appPreferences.customStatusBarFontDark = customStatusBarFontDark
                    ThemeUtil.setUseDynamicTheme(false)
                    ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.button_cancel),
                onClick = {
                    customPrimaryColor = Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorPrimary,
                            ThemeUtil.THEME_CUSTOM
                        )
                    )
                }
            )
        }
    ) {
        CustomColorPickerContent(
            color = customPrimaryColor,
            onColorChange = { customPrimaryColor = it },
            toolbarPrimaryColor = customToolbarPrimaryColor,
            onToolbarPrimaryColorChange = { customToolbarPrimaryColor = it },
            statusBarFontDark = customStatusBarFontDark,
            onStatusBarFontDarkChange = { customStatusBarFontDark = it },
        )
    }

    val isNight = ThemeUtil.isNightMode(currentTheme)
    val currentPreset = ThemePresets.presetOf(currentTheme)
    val modeText = when {
        isDynamicTheme -> stringResource(id = R.string.title_dynamic_theme)
        ThemeUtil.isTranslucentTheme(currentTheme) -> stringResource(id = R.string.title_theme_translucent)
        currentTheme == ThemeUtil.THEME_CUSTOM -> stringResource(id = R.string.title_custom_color)
        currentPreset != null -> currentPreset.name
        else -> stringResource(id = R.string.title_theme)
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_theme),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 当前状态摘要：明确"当前主题是什么、用在哪"
            item(key = "current_summary") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.title_theme_current),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary
                    )
                    Text(
                        text = modeText + " · " + if (isNight) {
                            stringResource(id = R.string.title_theme_mode_dark)
                        } else {
                            stringResource(id = R.string.title_theme_mode_light)
                        },
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium,
                        color = ExtendedTheme.colors.text
                    )
                    Text(
                        text = stringResource(id = R.string.summary_theme_roles),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary
                    )
                }
            }

            // 示意图：真实反映当前解析结果的配色关系
            item(key = "theme_schematic") {
                ThemeColorSchematic()
            }

            // 特殊模式：动态取色 / 自定义颜色 / 透明主题（普通设置入口，功能保留）
            item(key = "header_special") {
                SectionHeader(text = stringResource(id = R.string.title_theme_special))
            }
            item(key = "entry_dynamic") {
                val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ThemeEntryRow(
                    title = stringResource(id = R.string.title_dynamic_theme),
                    summary = stringResource(
                        id = if (supported) R.string.summary_dynamic_theme
                        else R.string.summary_dynamic_theme_unsupported
                    ),
                    selected = isDynamicTheme,
                    enabled = supported,
                    leading = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Rounded.Colorize,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { ThemeUtil.setUseDynamicTheme(true) }
                )
            }
            item(key = "entry_custom") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.title_custom_color),
                    summary = stringResource(id = R.string.summary_custom_theme),
                    selected = !isDynamicTheme && currentTheme == ThemeUtil.THEME_CUSTOM,
                    leading = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Rounded.ColorLens,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { customPrimaryColorDialogState.show() }
                )
            }
            item(key = "entry_translucent") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.title_theme_translucent),
                    summary = stringResource(id = R.string.summary_translucent_theme),
                    selected = ThemeUtil.isTranslucentTheme(currentTheme),
                    leading = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Rounded.PhotoSizeSelectActual,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { context.goToActivity<TranslucentThemeActivity>() }
                )
            }

            // 主功能顶栏风格（设置页面顶栏固定深灰，不随此变化）
            item(key = "header_toolbar_style") {
                SectionHeader(text = stringResource(id = R.string.title_main_toolbar_style))
            }
            item(key = "toolbar_style_note") {
                Text(
                    text = stringResource(id = R.string.summary_main_toolbar_style),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            itemsIndexed(
                items = ThemeUtil.MAIN_TOOLBAR_STYLES,
                key = { _, style -> style }
            ) { _, style ->
                val labelRes = when (style) {
                    ThemeUtil.MAIN_TOOLBAR_TEAL -> R.string.main_toolbar_teal
                    ThemeUtil.MAIN_TOOLBAR_PURPLE -> R.string.main_toolbar_purple
                    ThemeUtil.MAIN_TOOLBAR_NEUTRAL_DARK -> R.string.main_toolbar_neutral_dark
                    ThemeUtil.MAIN_TOOLBAR_FOLLOW_SEED -> R.string.main_toolbar_follow_seed
                    else -> R.string.main_toolbar_app_blue
                }
                ThemeEntryRow(
                    title = stringResource(id = labelRes),
                    selected = mainToolbarStyle == style,
                    onClick = { ThemeUtil.setMainToolbarStyle(style) }
                )
            }

            // 预设主题：两列分组，一处单选状态
            ThemeGroup.entries.forEach { group ->
                item(key = "header_preset_${group.name}") {
                    SectionHeader(text = stringResource(id = group.titleRes))
                }
                itemsIndexed(
                    items = ThemePresets.presets.filter { it.group == group },
                    key = { _, preset -> preset.id }
                ) { _, preset ->
                    val selected = !isDynamicTheme &&
                            !ThemeUtil.isTranslucentTheme(currentTheme) &&
                            currentTheme != ThemeUtil.THEME_CUSTOM &&
                            ThemePresets.presetOf(currentTheme)?.id == preset.id &&
                            !ThemePresets.isPresetDark(currentTheme)
                    ThemePresetRow(
                        preset = preset,
                        selected = selected,
                        onClick = {
                            ThemeUtil.switchTheme(preset.id)
                            ThemeUtil.setUseDynamicTheme(false)
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** 自定义颜色选择器内容（色轮 + Hex 输入，保留原有能力） */
@Composable
private fun CustomColorPickerContent(
    color: Color,
    onColorChange: (Color) -> Unit,
    toolbarPrimaryColor: Boolean,
    onToolbarPrimaryColorChange: (Boolean) -> Unit,
    statusBarFontDark: Boolean,
    onStatusBarFontDarkChange: (Boolean) -> Unit,
) {
    var useInput by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (useInput) {
            var inputHexColor by remember { mutableStateOf(color.toHex()) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                IconButton(
                    onClick = {
                        if ("^#([0-9a-fA-F]{6})$".toRegex().matches(inputHexColor)) {
                            onColorChange(Color(android.graphics.Color.parseColor(inputHexColor)))
                        }
                        useInput = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(id = R.string.button_sure_default)
                    )
                }
            }
        } else {
            Box {
                HarmonyColorPicker(
                    harmonyMode = ColorHarmonyMode.ANALOGOUS,
                    color = HsvColor.from(color),
                    onColorChanged = { onColorChange(it.toColor()) },
                    modifier = Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                )
                IconButton(
                    onClick = { useInput = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BorderColor,
                        contentDescription = stringResource(id = R.string.desc_input_color)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { onToolbarPrimaryColorChange(!toolbarPrimaryColor) }
                )
        ) {
            Checkbox(
                checked = toolbarPrimaryColor,
                onCheckedChange = onToolbarPrimaryColorChange,
            )
            Text(text = stringResource(id = R.string.tip_toolbar_primary_color))
        }
        if (toolbarPrimaryColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onStatusBarFontDarkChange(!statusBarFontDark) }
                    )
            ) {
                Checkbox(
                    checked = statusBarFontDark,
                    onCheckedChange = onStatusBarFontDarkChange,
                )
                Text(text = stringResource(id = R.string.tip_status_bar_font))
            }
        }
    }
}

private fun Color.toHex(): String {
    val argb = android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Medium,
        color = ExtendedTheme.colors.accentText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

/** 普通设置入口行（≥56dp，行末单选状态只有一处） */
@Composable
private fun ThemeEntryRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        leading?.invoke()
        if (leading != null) Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = if (enabled) ExtendedTheme.colors.text else ExtendedTheme.colors.textDisabled
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
        RadioButton(selected = selected, onClick = null, enabled = enabled)
    }
}

/** 预设行：28dp 色块 + 名称 + 行末一处选中状态 */
@Composable
private fun ThemePresetRow(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                onClickLabel = preset.name,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color = Color(preset.seedColor))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = preset.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f),
            color = ExtendedTheme.colors.text
        )
        RadioButton(selected = selected, onClick = null)
    }
}

/**
 * 主题配色关系示意图：随真实解析结果更新，带语义标签。
 */
@Composable
private fun ThemeColorSchematic() {
    val colors = ExtendedTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SchematicRow(stringResource(id = R.string.schematic_main_toolbar), colors.topBar)
        SchematicRow(stringResource(id = R.string.schematic_settings_toolbar), colors.settingsTopBar)
        SchematicRow(stringResource(id = R.string.schematic_status_bar), colors.statusBar)
        SchematicRow(stringResource(id = R.string.schematic_text), colors.text)
        SchematicRow(stringResource(id = R.string.schematic_accent_fill), colors.accentFill)
        SchematicRow(stringResource(id = R.string.schematic_accent_muted), colors.accentMuted)
    }
}

@Composable
private fun SchematicRow(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
