#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为 40 个预设生成 theme_color_*_<preset> 命名颜色资源（App.ThemeDelegate.getColorByAttr 的名称解析机制）。
浅色预设与深色预设分别引用对应的基础令牌颜色，避免重复字面量。"""

LIGHT_ATTRS = [
    ("primary", "preset_{id}"),
    ("new_primary", "preset_{id}"),
    ("accent", "preset_{id}"),
    ("on_accent", "preset_{id}_on"),
    ("text_on_primary", "preset_{id}_on"),
    ("toolbar", "oxygen_toolbar"),
    ("toolbar_item", "white"),
    ("toolbar_item_active", "white"),
    ("toolbar_item_secondary", "#B3FFFFFF"),
    ("toolbar_surface", "white"),
    ("on_toolbar_surface", "oxygen_text_primary"),
    ("nav", "oxygen_nav_bar"),
    ("nav_bar_surface", "white"),
    ("on_nav_bar_surface", "oxygen_text_secondary"),
    ("text", "oxygen_text_primary"),
    ("text_secondary", "oxygen_text_secondary"),
    ("text_disabled", "oxygen_text_disabled"),
    ("background", "oxygen_background"),
    ("window_background", "oxygen_background"),
    ("chip", "oxygen_chip"),
    ("on_chip", "oxygen_on_chip"),
    ("unselected", "oxygen_unselected"),
    ("floor_card", "oxygen_floor_card"),
    ("card", "oxygen_card"),
    ("divider", "oxygen_divider"),
    ("shadow", "oxygen_shadow"),
    ("indicator", "preset_{id}"),
    ("placeholder", "oxygen_placeholder"),
]

DARK_ATTRS = [
    ("primary", "preset_{id}_dark"),
    ("new_primary", "preset_{id}_dark"),
    ("accent", "preset_{id}_dark"),
    ("on_accent", "preset_{id}_dark_on"),
    ("text_on_primary", "preset_{id}_dark_on"),
    ("toolbar", "oxygen_dark_toolbar"),
    ("toolbar_item", "oxygen_dark_text_primary"),
    ("toolbar_item_active", "white"),
    ("toolbar_item_secondary", "#99E6E7EE"),
    ("toolbar_surface", "oxygen_dark_card"),
    ("on_toolbar_surface", "oxygen_dark_text_primary"),
    ("nav", "oxygen_dark_nav_bar"),
    ("nav_bar_surface", "oxygen_dark_card"),
    ("on_nav_bar_surface", "oxygen_dark_text_secondary"),
    ("text", "oxygen_dark_text_primary"),
    ("text_secondary", "oxygen_dark_text_secondary"),
    ("text_disabled", "oxygen_dark_text_disabled"),
    ("background", "oxygen_dark_background"),
    ("window_background", "oxygen_dark_background"),
    ("chip", "oxygen_dark_chip"),
    ("on_chip", "oxygen_dark_on_chip"),
    ("unselected", "oxygen_dark_unselected"),
    ("floor_card", "oxygen_dark_floor_card"),
    ("card", "oxygen_dark_card"),
    ("divider", "oxygen_dark_divider"),
    ("shadow", "#00000000"),
    ("indicator", "preset_{id}_dark"),
    ("placeholder", "oxygen_dark_placeholder"),
]

IDS = [
    "oxygen_blue", "oxygen_golden", "oxygen_lemon_yellow", "oxygen_grass_green",
    "oxygen_charm_purple", "oxygen_sky_blue", "oxygen_vigour_red", "oxygen_fashion_pink",
    "material_blue", "material_teal", "material_green", "material_red",
    "material_orange", "material_purple", "material_cyan", "material_indigo",
    "material_pink", "material_blue_grey", "material_deep_orange", "material_light_green",
]

lines = ["\n    <!-- ===== OxygenOS 预设：theme_color_*_<preset> 命名资源（ThemeDelegate 名称解析） ===== -->"]
for pid in IDS:
    lines.append("\n    <!-- %s -->" % pid)
    for attr, ref in LIGHT_ATTRS:
        lines.append('    <color name="theme_color_%s_%s">@color/%s</color>' % (attr, pid, ref.format(id=pid)))
    for attr, ref in DARK_ATTRS:
        lines.append('    <color name="theme_color_%s_%s_dark">@color/%s</color>' % (attr, pid, ref.format(id=pid)))

with open("tools/gen_theme_colors_block.xml", "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("generated", len(lines), "lines")
