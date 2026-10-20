package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.utils.appPreferences

/**
 * OxygenOS / Android 7.1 时代的轻快风格动效令牌。
 * 集中定义时长与缩放，避免各页面魔法值；减少动画设置开启时调用方可直接降级为 0。
 */
object AnimTokens {
    const val DURATION_SHORT = 120
    const val DURATION_MEDIUM = 220
    const val DURATION_LONG = 300
    const val PREDICTIVE_BACK_SCALE = 0.94f
    const val PREDICTIVE_BACK_SHIFT_DP = 20f
}

/**
 * 预测返回是否启用。
 *
 * - 用户开关（持久化，默认开启）关闭时不显示应用内预览，普通返回仍正常执行；
 * - Android 14（API 34）以下的旧系统没有系统预测返回 API，自动降级为普通返回，
 *   设置页会给出明确说明，不会让用户误以为旧系统有系统级手势动画。
 */
@Composable
fun isPredictiveBackEnabled(): Boolean {
    val context = LocalContext.current
    return context.appPreferences.predictiveBack &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

/**
 * 应用内预测返回预览。
 *
 * 手势进行期间页面按进度缩小、轻微位移并降低不透明度；手势取消后回弹到原状，
 * 手势完成后执行 [onBack]。预览目标与真正到达的页面一致（onBack 即该页面的返回动作）。
 *
 * 兼容性说明：Navigation Compose 2.7.6 + Compose Destinations 1.10.0 暂不升级到
 * Navigation 2.8（官方预测返回 NavHost 转场门槛），因此采用页面级应用内预览方案，
 * 旧系统与非支持场景一律走普通返回，行为不回退。
 */
@Composable
fun Modifier.predictiveBack(
    enabled: Boolean = true,
    onBack: () -> Unit,
): Modifier {
    // 用户开关 + 系统版本双重门控：任一不满足都走普通返回
    if (!enabled || !isPredictiveBackEnabled()) return this
    val density = LocalDensity.current
    val maxShiftPx = with(density) { AnimTokens.PREDICTIVE_BACK_SHIFT_DP.dp.toPx() }
    val progress = remember { Animatable(0f) }
    PredictiveBackHandler { flow ->
        try {
            flow.collect { event ->
                progress.snapTo(event.progress)
            }
            onBack()
        } finally {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = AnimTokens.DURATION_SHORT)
            )
        }
    }
    return this.graphicsLayer {
        val p = progress.value.coerceIn(0f, 1f)
        val scale = 1f - (1f - AnimTokens.PREDICTIVE_BACK_SCALE) * p
        scaleX = scale
        scaleY = scale
        translationX = maxShiftPx * p
        alpha = 1f - 0.25f * p
    }
}
