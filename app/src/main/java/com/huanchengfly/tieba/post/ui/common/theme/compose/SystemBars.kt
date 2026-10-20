package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.graphics.Color as AndroidColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.systemuicontroller.SystemUiController

/**
 * 页面表面驱动的系统栏样式。
 *
 * 状态栏/导航栏图标明暗由“当前页面实际绘制在系统栏后方的表面”决定，
 * 而不是按主题名全局决定：有彩色/深色顶栏的页面用浅色图标，
 * 浅色背景无顶栏页面（如个人中心）用深色图标。
 */
data class SystemBarStyle(
    val statusBarColor: Color = Color.Transparent,
    val darkStatusBarIcons: Boolean = false,
    val navigationBarColor: Color = Color.Transparent,
    val darkNavigationBarIcons: Boolean = false,
)

class SystemBarController {
    private val state = mutableStateOf(SystemBarStyle())
    val style: SystemBarStyle get() = state.value

    fun setStatusBar(color: Color, darkIcons: Boolean) {
        state.value = state.value.copy(statusBarColor = color, darkStatusBarIcons = darkIcons)
    }

    fun setNavigationBar(color: Color, darkIcons: Boolean) {
        state.value = state.value.copy(navigationBarColor = color, darkNavigationBarIcons = darkIcons)
    }

    /** 浅色表面 → 深色图标；深色表面 → 浅色图标 */
    fun darkIconsFor(surface: Color): Boolean =
        AndroidColor.luminance(surface.toArgb()) > 0.5
}

val LocalSystemBarController = staticCompositionLocalOf<SystemBarController?> { null }

/** 宿主（Activity）应用系统栏样式；随状态变化即时生效 */
@Composable
fun ApplySystemBars(controller: SystemBarController, systemUiController: SystemUiController) {
    val style = controller.style
    SideEffect {
        systemUiController.setStatusBarColor(
            color = style.statusBarColor,
            darkIcons = style.darkStatusBarIcons
        )
        systemUiController.setNavigationBarColor(
            color = style.navigationBarColor,
            darkIcons = style.darkNavigationBarIcons,
            navigationBarContrastEnforced = false
        )
    }
}

/**
 * 页面向系统栏报告“状态栏后方的实际表面色”，自动选择可读的图标明暗。
 * 顶栏页面由 Toolbar 自动调用；无顶栏页面（如个人中心）在根部调用一次。
 */
@Composable
fun ReportStatusBarSurface(surface: Color) {
    val controller = LocalSystemBarController.current ?: return
    val darkIcons = controller.darkIconsFor(surface)
    SideEffect {
        controller.setStatusBar(Color.Transparent, darkIcons)
    }
}

/**
 * 页面向系统栏报告“导航栏后方的实际表面色”（通常是页面背景）。
 */
@Composable
fun ReportNavigationBarSurface(surface: Color) {
    val controller = LocalSystemBarController.current ?: return
    val darkIcons = controller.darkIconsFor(surface)
    SideEffect {
        controller.setNavigationBar(Color.Transparent, darkIcons)
    }
}

/**
 * 默认导航栏样式：浅色主题用深色图标，深色主题用浅色图标。
 */
@Composable
fun DefaultNavigationBarStyle() {
    val controller = LocalSystemBarController.current ?: return
    val isNight = MaterialTheme.colors.isLight.not()
    SideEffect {
        controller.setNavigationBar(Color.Transparent, darkIcons = !isNight)
    }
}
