package com.huanchengfly.tieba.post.ui.common.theme

/**
 * Separates the chosen colour family from the dark surface preference.
 * Historical dark_theme values (e.g. material_blue_dark) still mean classic dark.
 * No Android state or persistence is read by this policy.
 */
object ThemeAppearance {
    fun resolveDarkTheme(darkPreference: String, presetDarkId: String?): String =
        if (darkPreference == "amoled_dark") "amoled_dark"
        else presetDarkId ?: "material_blue_grey_dark"
}
