package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
    // 设置类页面共享当前顶栏语义色，保证预览与实际页面一致。
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
    val themeRevision by remember { ThemeUtil.customThemeRevision }
    @Suppress("UNUSED_VARIABLE") val observedRevision = themeRevision
    val selectedPalette = ThemeUtil.selectedPaletteTheme()
    val listState = rememberLazyListState()
    val customPrimaryColorDialogState = rememberDialogState()
    val customTopBarDialogState = rememberDialogState()
    val customTopBarTextDialogState = rememberDialogState()
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
    var customPrimaryHex by remember { mutableStateOf(customPrimaryColor.toHex()) }
    val customPrimaryHexValid = normalizedHexRgb(customPrimaryHex) != null
    // Read the real persisted value up front. The generic preference state helper starts from its
    // default and can briefly write that default back before its collector receives stored data.
    var topBarFollowsTheme by remember {
        mutableStateOf(context.appPreferences.toolbarPrimaryColor)
    }
    var selectedDarkTheme by remember {
        mutableStateOf(context.appPreferences.darkTheme ?: ThemeUtil.THEME_AMOLED_DARK)
    }
    var followsSystemNight by remember {
        mutableStateOf(context.appPreferences.followSystemNight)
    }
    var customTopBarHex by remember {
        mutableStateOf(context.appPreferences.customTopBarColor ?: "#212121")
    }
    var customTopBarTextAuto by remember {
        mutableStateOf(context.appPreferences.customTopBarContentAuto)
    }
    var customTopBarTextHex by remember {
        mutableStateOf(context.appPreferences.customTopBarContentColor ?: "#FFFFFF")
    }
    val customTopBarHexValid = normalizedHexRgb(customTopBarHex) != null
    val customTopBarTextHexValid = normalizedHexRgb(customTopBarTextHex) != null
    LaunchedEffect(customPrimaryColorDialogState.show) {
        if (customPrimaryColorDialogState.show) customPrimaryHex = customPrimaryColor.toHex()
    }
    LaunchedEffect(customTopBarDialogState.show) {
        if (customTopBarDialogState.show) {
            customTopBarHex = context.appPreferences.customTopBarColor ?: "#212121"
        }
    }
    LaunchedEffect(customTopBarTextDialogState.show) {
        if (customTopBarTextDialogState.show) {
            customTopBarTextHex = context.appPreferences.customTopBarContentColor
                ?: String.format(
                    "#%06X",
                    0xFFFFFF and com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath
                        .readableOnInt(ThemeUtil.mainToolbarColor(currentTheme))
                )
        }
    }

    Dialog(
        dialogState = customPrimaryColorDialogState,
        onDismiss = { customPrimaryHex = customPrimaryColor.toHex() },
        title = { Text(text = stringResource(id = R.string.title_custom_theme)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
            DialogPositiveButton(
                text = stringResource(id = R.string.button_finish),
                enabled = customPrimaryHexValid,
                onClick = {
                    val hex = normalizedHexRgb(customPrimaryHex) ?: return@DialogPositiveButton
                    val confirmed = Color(android.graphics.Color.parseColor(hex))
                    val primary = CustomThemeDialog.toString(confirmed.toArgb())
                    context.appPreferences.applyCustomThemeColors(
                        primary = primary,
                        // Changing the seed never overwrites an independent toolbar colour.
                        topBar = context.appPreferences.customTopBarColor ?: "#212121",
                        topBarContent = context.appPreferences.customTopBarContentColor ?: "#FFFFFF",
                        accent = primary,
                    )
                    customPrimaryColor = confirmed
                    ThemeUtil.selectPalette(ThemeUtil.THEME_CUSTOM)
                }
            )
        }
    ) {
        CustomColorPickerContent(
            inputHexColor = customPrimaryHex,
            onHexChange = { customPrimaryHex = it },
            previewColor = normalizedHexRgb(customPrimaryHex)?.let {
                Color(android.graphics.Color.parseColor(it))
            } ?: customPrimaryColor,
        )
    }

    Dialog(
        dialogState = customTopBarDialogState,
        onDismiss = {
            customTopBarHex = context.appPreferences.customTopBarColor ?: "#212121"
        },
        title = { Text(text = stringResource(id = R.string.title_custom_top_bar_color)) },
        buttons = {
            DialogNegativeButton(
                text = stringResource(id = R.string.button_restore_default),
                onClick = {
                    customTopBarHex = "#212121"
                    context.appPreferences.customTopBarColor = customTopBarHex
                    context.appPreferences.customTopBarContentAuto = true
                    customTopBarTextAuto = true
                    context.appPreferences.customTopBarContentColor = "#FFFFFF"
                    ThemeUtil.notifyThemeSettingsChanged()
                }
            )
            DialogPositiveButton(
                text = stringResource(id = R.string.button_save),
                enabled = customTopBarHexValid,
                onClick = {
                    val normalized = normalizedHexRgb(customTopBarHex) ?: return@DialogPositiveButton
                    val color = android.graphics.Color.parseColor(normalized)
                    context.appPreferences.customTopBarColor = normalized
                    // Changing the background must not overwrite the user's saved manual ink.
                    customTopBarHex = normalized
                    ThemeUtil.notifyThemeSettingsChanged()
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
                            if (customTopBarHexValid) Color(android.graphics.Color.parseColor(normalizedHexRgb(customTopBarHex)!!))
                            else ExtendedTheme.colors.divider
                        )
                )
                OutlinedTextField(
                    value = customTopBarHex,
                    onValueChange = { customTopBarHex = it },
                    label = { Text(stringResource(id = R.string.label_hex_color)) },
                    isError = !customTopBarHexValid,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!customTopBarHexValid) {
                Text(
                    text = stringResource(id = R.string.error_hex_color),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }

    Dialog(
        dialogState = customTopBarTextDialogState,
        onDismiss = {
            if (context.appPreferences.customTopBarContentAuto) {
                customTopBarTextAuto = true
            }
            customTopBarTextHex = context.appPreferences.customTopBarContentColor ?: "#FFFFFF"
        },
        title = { Text(text = stringResource(id = R.string.title_custom_top_bar_content_color)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
            DialogPositiveButton(
                text = stringResource(id = R.string.button_save),
                enabled = customTopBarTextHexValid,
                onClick = {
                    val normalized = normalizedHexRgb(customTopBarTextHex) ?: return@DialogPositiveButton
                    context.appPreferences.customTopBarContentColor = normalized
                    context.appPreferences.customTopBarContentAuto = false
                    customTopBarTextAuto = false
                    ThemeUtil.notifyThemeSettingsChanged()
                }
            )
        }
    ) {
        val foreground = normalizedHexRgb(customTopBarTextHex)?.let(android.graphics.Color::parseColor)
        val previewBackground = if (!topBarFollowsTheme) {
            normalizedHexRgb(context.appPreferences.customTopBarColor ?: "#212121")
                ?.let(android.graphics.Color::parseColor) ?: ThemeUtil.mainToolbarColor(currentTheme)
        } else {
            ThemeUtil.mainToolbarColor(currentTheme)
        }
        val contrast = foreground?.let {
            com.huanchengfly.tieba.post.ui.common.theme.compose.ThemeColorMath.contrastInt(previewBackground, it)
        }
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
                        .background(foreground?.let { Color(it) } ?: ExtendedTheme.colors.text)
                )
                OutlinedTextField(
                    value = customTopBarTextHex,
                    onValueChange = { customTopBarTextHex = it },
                    label = { Text(stringResource(id = R.string.label_hex_color)) },
                    isError = !customTopBarTextHexValid,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!customTopBarTextHexValid) {
                Text(
                    text = stringResource(id = R.string.error_hex_color),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            } else if (contrast != null && contrast < 4.5) {
                Text(
                    text = stringResource(id = R.string.warning_low_toolbar_contrast, String.format("%.1f", contrast)),
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
                        fontWeight = FontWeight.Medium, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "header_display_mode") {
                SectionHeader(text = stringResource(id = R.string.title_display_mode))
            }
            item(key = "display_mode_system") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.display_mode_system),
                    selected = followsSystemNight,
                    onClick = {
                        followsSystemNight = true
                        context.appPreferences.followSystemNight = true
                        if (App.isSystemNight != ThemeUtil.isNightMode(currentTheme)) {
                            ThemeUtil.switchNightMode()
                        }
                    }
                )
            }
            item(key = "display_mode_light") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.display_mode_light),
                    selected = !followsSystemNight && !ThemeUtil.isNightMode(currentTheme),
                    onClick = {
                        followsSystemNight = false
                        context.appPreferences.followSystemNight = false
                        if (ThemeUtil.isNightMode(currentTheme)) ThemeUtil.switchNightMode()
                    }
                )
            }
            item(key = "display_mode_dark") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.display_mode_dark),
                    selected = !followsSystemNight && ThemeUtil.isNightMode(currentTheme),
                    onClick = {
                        followsSystemNight = false
                        context.appPreferences.followSystemNight = false
                        if (!ThemeUtil.isNightMode(currentTheme)) ThemeUtil.switchNightMode()
                    }
                )
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
                        val presetDark = ThemePresets.presetOf(selectedPalette)?.darkId
                            ?: "material_blue_grey_dark"
                        selectedDarkTheme = presetDark
                        ThemeUtil.setDarkSurface(amoled = false)
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
                        ThemeUtil.setDarkSurface(amoled = true)
                    }
                )
            }


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
                        selected = ThemePresets.presetOf(selectedPalette)?.id == preset.id,
                        onClick = { ThemeUtil.selectPalette(preset.id) }
                    )
                }
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
                        showRadio = false,
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (customTopBarHexValid) Color(android.graphics.Color.parseColor(normalizedHexRgb(customTopBarHex)!!))
                                        else ExtendedTheme.colors.divider
                                    )
                            )
                        },
                        onClick = { customTopBarDialogState.show() }
                    )
                }
            }
            item(key = "header_top_bar_content") {
                SectionHeader(text = stringResource(id = R.string.title_top_bar_content_section))
            }
            item(key = "top_bar_text_auto") {
                ToggleSettingRow(
                    title = stringResource(id = R.string.title_top_bar_content_auto),
                    summary = stringResource(id = R.string.summary_top_bar_content_auto),
                    checked = customTopBarTextAuto,
                    onCheckedChange = { checked ->
                        if (checked) {
                            customTopBarTextAuto = true
                            context.appPreferences.customTopBarContentAuto = true
                            ThemeUtil.notifyThemeSettingsChanged()
                        } else {
                            customTopBarTextDialogState.show()
                        }
                    }
                )
            }
            if (!customTopBarTextAuto) {
                item(key = "custom_top_bar_content") {
                    ThemeEntryRow(
                        title = stringResource(id = R.string.title_custom_top_bar_content_color),
                    summary = stringResource(
                        id = R.string.summary_custom_top_bar_content_color,
                        context.appPreferences.customTopBarContentColor ?: "#FFFFFF"
                    ),
                        selected = false,
                        showRadio = false,
                        onClick = { customTopBarTextDialogState.show() }
                    )
                }
            }
            item(key = "entry_custom") {
                ThemeEntryRow(
                    title = stringResource(id = R.string.title_custom_accent_color),
                    selected = selectedPalette == ThemeUtil.THEME_CUSTOM,
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
    inputHexColor: String,
    onHexChange: (String) -> Unit,
    previewColor: Color,
) {
    val validHex = normalizedHexRgb(inputHexColor) != null
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
                     .background(previewColor)
            )
            OutlinedTextField(
                value = inputHexColor,
                onValueChange = onHexChange,
                label = { Text(stringResource(id = R.string.label_hex_color)) },
                isError = !validHex,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (!validHex) {
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
    showRadio: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (summary == null) 0.dp else 8.dp),
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
        if (showRadio) RadioButton(
            selected = selected, onClick = null, enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = ExtendedTheme.colors.accentText,
                unselectedColor = ExtendedTheme.colors.textSecondary
            )
        )
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
        RadioButton(
            selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = ExtendedTheme.colors.accentText,
                unselectedColor = ExtendedTheme.colors.textSecondary
            )
        )
    }
}
