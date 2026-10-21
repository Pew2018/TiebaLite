package com.huanchengfly.tieba.post.ui.page.settings.theme

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
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
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.CustomThemeDialog
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
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch
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
    // 设置类页面角色：顶栏/状态栏使用 经典深色
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
    val customPrimaryColorDialogState = rememberDialogState()
    val customTopBarDialogState = rememberDialogState()
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
    // Read the real persisted value up front. The generic preference state helper starts from its
    // default and can briefly write that default back before its collector receives stored data.
    var topBarFollowsTheme by remember {
        mutableStateOf(context.appPreferences.toolbarPrimaryColor)
    }
    var selectedDarkTheme by remember {
        mutableStateOf(context.appPreferences.darkTheme ?: ThemeUtil.THEME_AMOLED_DARK)
    }
    var customTopBarHex by remember {
        mutableStateOf(context.appPreferences.customTopBarColor ?: "#212121")
    }
    val customTopBarHexValid = remember(customTopBarHex) {
        "^#([0-9a-fA-F]{6})$".toRegex().matches(customTopBarHex)
    }

    Dialog(
        dialogState = customPrimaryColorDialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_theme)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_finish),
                onClick = {
                    val primary = CustomThemeDialog.toString(customPrimaryColor.toArgb())
                    // Keep the legacy booleans readable, but publish all visible roles together.
                    context.appPreferences.applyCustomThemeColors(
                        primary = primary,
                        topBar = if (topBarFollowsTheme) primary else customTopBarHex,
                        topBarContent = String.format("#%06X", 0xFFFFFF and
                            com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
                                .readableOnInt(customPrimaryColor.toArgb())),
                        accent = primary,
                    )
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
        )
    }

    Dialog(
        dialogState = customTopBarDialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_top_bar_color)) },
        buttons = {
            DialogNegativeButton(
                text = stringResource(id = R.string.button_restore_default),
                onClick = {
                    customTopBarHex = "#212121"
                    context.appPreferences.customTopBarColor = customTopBarHex
                    context.appPreferences.customTopBarContentColor = "#FFFFFF"
                    ThemeUtil.notifyThemeSettingsChanged()
                }
            )
            DialogPositiveButton(
                text = stringResource(id = R.string.button_save),
                onClick = {
                    if (customTopBarHexValid) {
                        val normalized = customTopBarHex.uppercase()
                        val color = android.graphics.Color.parseColor(normalized)
                        context.appPreferences.customTopBarColor = normalized
                        context.appPreferences.customTopBarContentColor = String.format(
                            "#%06X",
                            0xFFFFFF and com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
                                .readableOnInt(color)
                        )
                        ThemeUtil.notifyThemeSettingsChanged()
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (customTopBarHexValid) Color(android.graphics.Color.parseColor(customTopBarHex))
                            else ExtendedTheme.colors.divider
                        )
                )
                OutlinedTextField(
                    value = customTopBarHex,
                    onValueChange = { customTopBarHex = it.trim() },
                    label = { Text(stringResource(id = R.string.label_hex_color)) },
                    isError = customTopBarHex.isNotEmpty() && !customTopBarHexValid,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (customTopBarHex.isNotEmpty() && !customTopBarHexValid) {
                Text(
                    text = stringResource(id = R.string.error_hex_color),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }
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
            // 预设采用普通 56dp 单选行；选中态由稳定主题 ID 唯一决定。
            ThemeGroup.entries.forEach { group ->
                item(key = "header_preset_${group.name}") {
                    SectionHeader(text = stringResource(id = group.titleRes))
                }
                itemsIndexed(
                    items = ThemePresets.presets.filter { it.group == group },
                    key = { _, preset -> preset.id }
                ) { _, preset ->
                    ThemePresetRow(
                        preset = preset,
                        selected = ThemePresets.presetOf(currentTheme)?.id == preset.id,
                        onClick = {
                            ThemeUtil.switchTheme(if (ThemeUtil.isNightMode(currentTheme)) preset.darkId else preset.id)
                        }
                    )
                }
            }

            item(key = "header_dark_surface") {
                SectionHeader(text = stringResource(id = R.string.title_dark_surface_mode))
            }
            item(key = "dark_surface_normal") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.dark_surface_normal),
                    summary = stringResource(id = R.string.dark_surface_normal_summary),
                    selected = selectedDarkTheme != ThemeUtil.THEME_AMOLED_DARK,
                    onClick = {
                        val presetDark = ThemePresets.presetOf(currentTheme)?.darkId
                            ?: "material_blue_grey_dark"
                        selectedDarkTheme = presetDark
                        context.appPreferences.darkTheme = presetDark
                        if (ThemeUtil.isNightMode(currentTheme)) {
                            ThemeUtil.switchTheme(presetDark, recordOldTheme = false)
                        }
                        ThemeUtil.notifyThemeSettingsChanged()
                    }
                )
            }
            item(key = "dark_surface_amoled") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.dark_surface_amoled),
                    summary = stringResource(id = R.string.dark_surface_amoled_summary),
                    selected = selectedDarkTheme == ThemeUtil.THEME_AMOLED_DARK,
                    onClick = {
                        selectedDarkTheme = ThemeUtil.THEME_AMOLED_DARK
                        context.appPreferences.darkTheme = ThemeUtil.THEME_AMOLED_DARK
                        if (ThemeUtil.isNightMode(currentTheme)) {
                            ThemeUtil.switchTheme(ThemeUtil.THEME_AMOLED_DARK, recordOldTheme = false)
                        }
                        ThemeUtil.notifyThemeSettingsChanged()
                    }
                )
            }

            item(key = "header_custom") {
                SectionHeader(text = stringResource(id = R.string.title_custom_color_section))
            }
            item(key = "top_bar_follow") {
                ToggleSettingRow(
                    title = stringResource(id = R.string.title_top_bar_follow_theme),
                    summary = stringResource(id = R.string.summary_top_bar_follow_theme),
                    checked = topBarFollowsTheme,
                    onCheckedChange = { checked ->
                        topBarFollowsTheme = checked
                        context.appPreferences.toolbarPrimaryColor = checked
                        ThemeUtil.notifyThemeSettingsChanged()
                    }
                )
            }
            if (!topBarFollowsTheme) {
                item(key = "custom_top_bar") {
                    ThemeEntryRow(
                        title = stringResource(id = R.string.title_custom_top_bar_color),
                        summary = stringResource(id = R.string.summary_custom_top_bar_color),
                        selected = false,
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (customTopBarHexValid) Color(android.graphics.Color.parseColor(customTopBarHex))
                                        else ExtendedTheme.colors.divider
                                    )
                            )
                        },
                        onClick = { customTopBarDialogState.show() }
                    )
                }
            }
            item(key = "entry_custom") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.title_custom_accent_color),
                    selected = false,
                    summary = stringResource(id = R.string.summary_custom_theme),
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
            item {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1, color = ExtendedTheme.colors.text)
            Text(text = summary, style = MaterialTheme.typography.caption, color = ExtendedTheme.colors.textSecondary)
        }
        Box(modifier = Modifier.size(width = 48.dp, height = 48.dp), contentAlignment = Alignment.Center) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/** Compact custom editor.  It intentionally avoids the former full-screen colour wheel. */
@Composable
private fun CustomColorPickerContent(
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    var inputHexColor by remember(color) { mutableStateOf(color.toHex()) }
    val validHex = "^#([0-9a-fA-F]{6})$".toRegex().matches(inputHexColor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            OutlinedTextField(
                value = inputHexColor,
                onValueChange = {
                    inputHexColor = it.trim()
                    if ("^#([0-9a-fA-F]{6})$".toRegex().matches(inputHexColor)) {
                        onColorChange(Color(android.graphics.Color.parseColor(inputHexColor)))
                    }
                },
                label = { Text(stringResource(id = R.string.label_hex_color)) },
                isError = inputHexColor.isNotEmpty() && !validHex,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (inputHexColor.isNotEmpty() && !validHex) {
            Text(
                text = stringResource(id = R.string.error_hex_color),
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
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
