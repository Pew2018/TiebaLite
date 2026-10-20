package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 页面角色：决定顶栏/状态栏配色来源。
 *
 * - [MAIN]：主功能页面（首页、贴吧、帖子、消息、搜索等），使用主功能顶栏风格偏好
 *   （默认经典一加应用蓝 #0071CD，可切青绿/紫/中性深灰/跟随种子）。
 * - [SETTINGS]：设置与管理页面，固定 OxygenOS 深灰 #212121。
 *
 * 默认 MAIN；设置类页面在根部通过 CompositionLocalProvider 提供 SETTINGS。
 */
enum class PageRole {
    MAIN,
    SETTINGS,
}

val LocalPageRole = staticCompositionLocalOf { PageRole.MAIN }
