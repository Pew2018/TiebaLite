#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 OxygenOS 7.1.1 经典风格主题资源：
- colors.xml 追加块（20 个预设种子色 + 深色变体 + onAccent 变体 + 基础令牌）
- styles.xml 追加块（浅色基座 TiebaLite.Light、深色基座 TiebaLite.Dark.Oxygen、
  纯黑 TiebaLite.Dark.Amoled、20 个浅色预设 + 20 个深色预设）
"""

PRESETS = [
    # (id, 中文名, 种子色)
    ("oxygen_blue", "氧气蓝", "#42A5F5"),
    ("oxygen_golden", "流金橙", "#CC6F4E"),
    ("oxygen_lemon_yellow", "柠檬黄", "#E6A545"),
    ("oxygen_grass_green", "青草绿", "#7DC22F"),
    ("oxygen_charm_purple", "魅力紫", "#9575CD"),
    ("oxygen_sky_blue", "天空蓝", "#26C6DA"),
    ("oxygen_vigour_red", "活力红", "#F06292"),
    ("oxygen_fashion_pink", "时尚粉", "#BA68C8"),
    ("material_blue", "M 蓝", "#2196F3"),
    ("material_teal", "M 青绿", "#009688"),
    ("material_green", "M 绿", "#4CAF50"),
    ("material_red", "M 红", "#F44336"),
    ("material_orange", "M 橙", "#FF9800"),
    ("material_purple", "M 紫", "#9C27B0"),
    ("material_cyan", "M 青", "#00BCD4"),
    ("material_indigo", "M 靛蓝", "#3F51B5"),
    ("material_pink", "M 粉", "#E91E63"),
    ("material_blue_grey", "M 蓝灰", "#607D8B"),
    ("material_deep_orange", "M 深橙", "#FF5722"),
    ("material_light_green", "M 浅绿", "#8BC34A"),
]

WHITE = (255, 255, 255)
DARK_TEXT = (33, 33, 33)  # #212121


def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def rgb_to_hex(rgb):
    return "#%02X%02X%02X" % rgb


def luminance(rgb):
    def chan(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (chan(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def lighten(rgb, amount):
    return tuple(int(round(c + (255 - c) * amount)) for c in rgb)


def style_name(preset_id):
    parts = preset_id.split("_", 1)
    if parts[0] == "oxygen":
        rest = ".".join(w.capitalize() for w in parts[1].split("_"))
        return "TiebaLite.Preset.Oxygen." + rest
    rest = ".".join(w.capitalize() for w in parts[1].split("_"))
    return "TiebaLite.Preset.Material." + rest


def color_res(preset_id, suffix=""):
    return "preset_" + preset_id + suffix


# ---------- colors.xml ----------
color_lines = []
color_lines.append("\n    <!-- ===== OxygenOS 7.1.1 经典主题：基础令牌（浅色） ===== -->")
light_tokens = [
    ("oxygen_toolbar", "#FF212121"),
    ("oxygen_background", "#FFFAFAFA"),
    ("oxygen_card", "#FFFFFFFF"),
    ("oxygen_floor_card", "#FFFFFFFF"),
    ("oxygen_divider", "#FFE6E6E6"),
    ("oxygen_text_primary", "#FF212121"),
    ("oxygen_text_secondary", "#FF757575"),
    ("oxygen_text_disabled", "#FFBDBDBD"),
    ("oxygen_unselected", "#FFBDBDBD"),
    ("oxygen_chip", "#FFEFEFEF"),
    ("oxygen_on_chip", "#FF757575"),
    ("oxygen_placeholder", "#FFF0F0F0"),
    ("oxygen_nav_bar", "#FFFAFAFA"),
    ("oxygen_shadow", "#1A000000"),
]
for name, val in light_tokens:
    color_lines.append('    <color name="%s">%s</color>' % (name, val))
color_lines.append("\n    <!-- ===== OxygenOS 7.1.1 经典主题：基础令牌（深色） ===== -->")
dark_tokens = [
    ("oxygen_dark_toolbar", "#FF212121"),
    ("oxygen_dark_background", "#FF121212"),
    ("oxygen_dark_card", "#FF1E1E1E"),
    ("oxygen_dark_floor_card", "#FF1E1E1E"),
    ("oxygen_dark_divider", "#FF2A2A2A"),
    ("oxygen_dark_text_primary", "#FFE6E6E6"),
    ("oxygen_dark_text_secondary", "#FF9E9E9E"),
    ("oxygen_dark_text_disabled", "#FF616161"),
    ("oxygen_dark_unselected", "#FF616161"),
    ("oxygen_dark_chip", "#FF242424"),
    ("oxygen_dark_on_chip", "#FF9E9E9E"),
    ("oxygen_dark_placeholder", "#FF242424"),
    ("oxygen_dark_nav_bar", "#FF121212"),
]
for name, val in dark_tokens:
    color_lines.append('    <color name="%s">%s</color>' % (name, val))

color_lines.append("\n    <!-- ===== OxygenOS 7.1.1 经典主题：20 个预设（种子色 / 深色变体 / onAccent） ===== -->")
preset_meta = []
for pid, name, seed in PRESETS:
    seed_rgb = hex_to_rgb(seed)
    on_light = rgb_to_hex(DARK_TEXT) if contrast(seed_rgb, DARK_TEXT) >= contrast(seed_rgb, WHITE) else rgb_to_hex(WHITE)
    dark_tint = lighten(seed_rgb, 0.34)
    on_dark = rgb_to_hex(DARK_TEXT) if contrast(dark_tint, DARK_TEXT) >= contrast(dark_tint, WHITE) else rgb_to_hex(WHITE)
    preset_meta.append((pid, name, seed, on_light, dark_tint, on_dark))
    color_lines.append('    <color name="%s">%s</color>' % (color_res(pid), "#FF" + seed.lstrip("#").upper()))
    color_lines.append('    <color name="%s">%s</color>' % (color_res(pid, "_on"), "#FF" + on_light.lstrip("#").upper()))
    color_lines.append('    <color name="%s">%s</color>' % (color_res(pid, "_dark"), "#FF" + rgb_to_hex(dark_tint).lstrip("#").upper()))
    color_lines.append('    <color name="%s">%s</color>' % (color_res(pid, "_dark_on"), "#FF" + on_dark.lstrip("#").upper()))

# ---------- styles.xml ----------
style_lines = []
style_lines.append("\n    <!-- ================= OxygenOS 7.1.1 经典主题 ================= -->")
style_lines.append("""
    <style name="TiebaLite.Light" parent="AppTheme">
        <item name="colorPrimary">@color/oxygen_blue</item>
        <item name="colorNewPrimary">@color/oxygen_blue</item>
        <item name="colorAccent">@color/oxygen_blue</item>
        <item name="colorOnAccent">@color/white</item>
        <item name="colorTextOnPrimary">@color/white</item>
        <item name="colorToolbar">@color/oxygen_toolbar</item>
        <item name="colorToolbarItem">@color/white</item>
        <item name="colorToolbarItemSecondary">#B3FFFFFF</item>
        <item name="colorToolbarItemActive">@color/white</item>
        <item name="colorToolbarSurface">@color/white</item>
        <item name="colorOnToolbarSurface">@color/oxygen_text_primary</item>
        <item name="colorNavBar">@color/oxygen_nav_bar</item>
        <item name="colorNavBarSurface">@color/white</item>
        <item name="colorOnNavBarSurface">@color/oxygen_text_secondary</item>
        <item name="colorText">@color/oxygen_text_primary</item>
        <item name="colorTextSecondary">@color/oxygen_text_secondary</item>
        <item name="color_text_disabled">@color/oxygen_text_disabled</item>
        <item name="colorBackground">@color/oxygen_background</item>
        <item name="colorWindowBackground">@color/oxygen_background</item>
        <item name="colorChip">@color/oxygen_chip</item>
        <item name="colorOnChip">@color/oxygen_on_chip</item>
        <item name="colorUnselected">@color/oxygen_unselected</item>
        <item name="colorFloorCard">@color/oxygen_floor_card</item>
        <item name="colorCard">@color/oxygen_card</item>
        <item name="colorDivider">@color/oxygen_divider</item>
        <item name="shadow_color">@color/oxygen_shadow</item>
        <item name="colorIndicator">@color/oxygen_blue</item>
        <item name="colorPlaceholder">@color/oxygen_placeholder</item>
    </style>

    <style name="TiebaLite.Dark.Oxygen" parent="AppTheme.Dark">
        <item name="colorPrimary">@color/oxygen_blue_dark</item>
        <item name="colorNewPrimary">@color/oxygen_blue_dark</item>
        <item name="colorAccent">@color/oxygen_blue_dark</item>
        <item name="colorOnAccent">@color/oxygen_blue_dark_on</item>
        <item name="colorTextOnPrimary">@color/oxygen_blue_dark_on</item>
        <item name="colorToolbar">@color/oxygen_dark_toolbar</item>
        <item name="colorToolbarItem">@color/oxygen_dark_text_primary</item>
        <item name="colorToolbarItemSecondary">#99E6E7EE</item>
        <item name="colorToolbarItemActive">@color/white</item>
        <item name="colorToolbarSurface">@color/oxygen_dark_card</item>
        <item name="colorOnToolbarSurface">@color/oxygen_dark_text_primary</item>
        <item name="colorNavBar">@color/oxygen_dark_nav_bar</item>
        <item name="colorNavBarSurface">@color/oxygen_dark_card</item>
        <item name="colorOnNavBarSurface">@color/oxygen_dark_text_secondary</item>
        <item name="colorText">@color/oxygen_dark_text_primary</item>
        <item name="colorTextSecondary">@color/oxygen_dark_text_secondary</item>
        <item name="color_text_disabled">@color/oxygen_dark_text_disabled</item>
        <item name="colorBackground">@color/oxygen_dark_background</item>
        <item name="colorWindowBackground">@color/oxygen_dark_background</item>
        <item name="colorChip">@color/oxygen_dark_chip</item>
        <item name="colorOnChip">@color/oxygen_dark_on_chip</item>
        <item name="colorUnselected">@color/oxygen_dark_unselected</item>
        <item name="colorFloorCard">@color/oxygen_dark_floor_card</item>
        <item name="colorCard">@color/oxygen_dark_card</item>
        <item name="colorDivider">@color/oxygen_dark_divider</item>
        <item name="shadow_color">#00000000</item>
        <item name="colorIndicator">@color/oxygen_blue_dark</item>
        <item name="colorPlaceholder">@color/oxygen_dark_placeholder</item>
    </style>
""")

for pid, name, seed, on_light, dark_tint, on_dark in preset_meta:
    sn = style_name(pid)
    style_lines.append("""
    <style name="%s" parent="TiebaLite.Light">
        <item name="colorPrimary">@color/%s</item>
        <item name="colorNewPrimary">@color/%s</item>
        <item name="colorAccent">@color/%s</item>
        <item name="colorOnAccent">@color/%s</item>
        <item name="colorTextOnPrimary">@color/%s</item>
        <item name="colorIndicator">@color/%s</item>
    </style>
""" % (sn, color_res(pid), color_res(pid), color_res(pid), color_res(pid, "_on"), color_res(pid, "_on"), color_res(pid)))
    style_lines.append("""
    <style name="%s.Dark" parent="TiebaLite.Dark.Oxygen">
        <item name="colorPrimary">@color/%s</item>
        <item name="colorNewPrimary">@color/%s</item>
        <item name="colorAccent">@color/%s</item>
        <item name="colorOnAccent">@color/%s</item>
        <item name="colorTextOnPrimary">@color/%s</item>
        <item name="colorIndicator">@color/%s</item>
    </style>
""" % (sn, color_res(pid, "_dark"), color_res(pid, "_dark"), color_res(pid, "_dark"), color_res(pid, "_dark_on"), color_res(pid, "_dark_on"), color_res(pid, "_dark")))

with open("tools/gen_colors_block.xml", "w", encoding="utf-8") as f:
    f.write("\n".join(color_lines) + "\n")
with open("tools/gen_styles_block.xml", "w", encoding="utf-8") as f:
    f.write("\n".join(style_lines) + "\n")

# 输出预设元数据供 Kotlin 端使用
print("PRESETS = [")
for pid, name, seed, on_light, dark_tint, on_dark in preset_meta:
    print('    ThemePreset("%s", "%s", "%s", "%s"),' % (pid, name, seed.upper().replace("#", "FF"), on_light.upper().replace("#", "FF")))
print("]")
