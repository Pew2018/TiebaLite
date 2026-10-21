package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 页面角色：决定顶栏/状态栏配色来源。
 *
 * - [MAIN]：主功能页面（首页、贴吧、帖子、消息、搜索等）。
 * - [SETTINGS]：设置与管理页面。
 *
 * MAIN 使用「跟随主题色 / 独立顶栏颜色」偏好。
 * SETTINGS 使用固定经典深色顶栏（AMOLED 模式为纯黑），不受独立顶栏颜色影响。
 *
 * 默认 MAIN；设置类页面在根部通过 CompositionLocalProvider 提供 SETTINGS。
 */
enum class PageRole {
    MAIN,
    SETTINGS,
}

val LocalPageRole = staticCompositionLocalOf { PageRole.MAIN }
