package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 页面角色：决定顶栏/状态栏配色来源。
 *
 * - [MAIN]：主功能页面（首页、贴吧、帖子、消息、搜索等）。
 * - [SETTINGS]：设置与管理页面。
 *
 * 两类页面都从同一顶栏偏好解析；角色只保留语义入口，便于未来在不分叉偏好模型的前提下
 * 调整层级色。当前「跟随主题色 / 自定义顶栏颜色」会同时作用于全部标准页面。
 *
 * 默认 MAIN；设置类页面在根部通过 CompositionLocalProvider 提供 SETTINGS。
 */
enum class PageRole {
    MAIN,
    SETTINGS,
}

val LocalPageRole = staticCompositionLocalOf { PageRole.MAIN }
