package com.huanchengfly.tieba.post.ui.page.settings.theme

import java.util.Locale

private val HEX_RGB = Regex("^#[0-9a-fA-F]{6}$")

/** Only a complete #RRGGBB value may replace the user's persisted theme. */
internal fun normalizedHexRgb(input: String): String? {
    val normalized = input.trim()
    return normalized.takeIf { HEX_RGB.matches(it) }?.uppercase(Locale.ROOT)
}
