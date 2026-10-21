package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * OxygenOS 7.1.1 / Material Design 1 统一动效令牌。
 *
 * 曲线参数对应附录 4.1：
 * - FastOutSlowIn  (0.4, 0, 0.2, 1)     常规页面与控件状态变化
 * - LinearOutSlowIn (0, 0, 0.2, 1)      进入屏幕的元素
 * - FastOutLinearIn (0.4, 0, 1, 1)      离开屏幕的元素
 *
 * 时长对应附录 4.2（初版目标值，单位 ms）：
 * 列表点击 Ripple 150–250 / 页面进入 220–280 / 页面返回 180–250 /
 * PopupMenu 120–180 / Dialog 打开 180–220 / Dialog 关闭 120–170 /
 * Tab 切换 200–250 / Switch 120–180 / Checkbox 100–150 / FAB 180–240 /
 * 底部操作面板 220–280 / Snackbar 180–250 / 图片加载 120–180。
 *
 * 禁止弹簧/回弹/Overshoot；尊重系统动画缩放与“减少动画”。
 */
object AnimTokens {
    // 插值器（使用 Compose 内置实现，Bézier 参数与附录一致）
    val FastOutSlowIn: Easing = FastOutSlowInEasing
    val LinearOutSlowIn: Easing = LinearOutSlowInEasing
    val FastOutLinearIn: Easing = FastOutLinearInEasing

    // 时长（ms）——取各区间中值，作为统一令牌
    const val DURATION_RIPPLE = 200
    const val DURATION_PAGE_ENTER = 250
    const val DURATION_PAGE_EXIT = 220
    const val DURATION_POPUP = 150
    const val DURATION_DIALOG_OPEN = 200
    const val DURATION_DIALOG_CLOSE = 150
    const val DURATION_TAB = 220
    const val DURATION_SWITCH = 150
    const val DURATION_CHECKBOX = 120
    const val DURATION_FAB = 200
    const val DURATION_SHEET = 250
    const val DURATION_SNACKBAR = 220
    const val DURATION_IMAGE = 150

    /** 短动画（如预测返回取消复位） */
    const val DURATION_SHORT = 200
}
