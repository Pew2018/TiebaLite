package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * OxygenOS 经典主题的用途色派生与对比度工具。
 *
 * 规则：
 * - 一般文字对比度目标 4.5:1，关键图标/状态 3:1；
 * - 派生变体时优先调整明度/饱和度，不改动来源种子色；
 * - 前景色自动在白/深之间选择可读组合，不允许产出不可读配对。
 */
object ThemeColorMath {

    /** #AARRGGBB Int 的相对亮度（WCAG） */
    fun luminanceInt(argb: Int): Double {
        val r = AndroidColor.red(argb) / 255.0
        val g = AndroidColor.green(argb) / 255.0
        val b = AndroidColor.blue(argb) / 255.0
        fun ch(c: Double): Double =
            if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)
    }

    fun contrastInt(a: Int, b: Int): Double {
        val la = luminanceInt(a)
        val lb = luminanceInt(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** 在深色文字与白色之间选择对比度更高的一方 */
    fun readableOnInt(background: Int, darkText: Int = 0xFF212121.toInt(), lightText: Int = 0xFFFFFFFF.toInt()): Int =
        if (contrastInt(background, darkText) >= contrastInt(background, lightText)) darkText else lightText

    /** 与背景混合；amount=0 返回原色，1 返回 target */
    fun blendInt(color: Int, target: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        fun ch(from: Int, to: Int): Int = (from + (to - from) * a).toInt().coerceIn(0, 255)
        return AndroidColor.argb(
            ch(AndroidColor.alpha(color), AndroidColor.alpha(target)),
            ch(AndroidColor.red(color), AndroidColor.red(target)),
            ch(AndroidColor.green(color), AndroidColor.green(target)),
            ch(AndroidColor.blue(color), AndroidColor.blue(target)),
        )
    }

    /** 提亮（向白混合） */
    fun lightenInt(color: Int, amount: Float): Int = blendInt(color, 0xFFFFFFFF.toInt(), amount)

    /** 压暗（向黑混合） */
    fun darkenInt(color: Int, amount: Float): Int = blendInt(color, 0xFF000000.toInt(), amount)

    /**
     * 生成在 [background] 上可读的强调文字色：种子色对比不足时向背景反方向调整明度。
     * 小字链接/分组标题使用，避免把浅黄种子直接放在白底上。
     */
    fun readableAccentTextInt(seed: Int, background: Int, minRatio: Double = 4.5): Int {
        if (contrastInt(seed, background) >= minRatio) return seed
        val towardDark = luminanceInt(background) > 0.5
        var current = seed
        var step = 0.06f
        var guard = 0
        while (contrastInt(current, background) < minRatio && guard < 24) {
            current = if (towardDark) darkenInt(current, step) else lightenInt(current, step)
            guard++
        }
        // 仍不足时回退到中性可读色
        if (contrastInt(current, background) < minRatio) {
            current = if (towardDark) 0xFF212121.toInt() else 0xFFFFFFFF.toInt()
        }
        return current
    }

    /** 弱强调底色（选中背景等）：与内容背景同向的极淡种子色 */
    fun mutedAccentBgInt(seed: Int, background: Int): Int {
        val towardDark = luminanceInt(background) <= 0.5
        val base = if (towardDark) darkenInt(seed, 0.55f) else lightenInt(seed, 0.82f)
        return blendInt(base, background, 0.55f)
    }

    /** 弱强调底色上的可读前景 */
    fun onMutedAccentBgInt(seed: Int, background: Int): Int =
        readableAccentTextInt(seed, mutedAccentBgInt(seed, background), 4.5)

    fun Color.luminanceValue(): Double = luminanceInt(this.toArgb())
}

/** Compose Color -> #AARRGGBB Int */
fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

fun Int.toComposeColor(): Color = Color(this)
