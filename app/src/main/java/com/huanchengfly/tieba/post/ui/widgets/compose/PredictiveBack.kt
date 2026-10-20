package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * OxygenOS / Android 7 时代的轻快动效令牌。
 * 集中定义时长与缩放，避免各页面魔法值；系统“减少动画”时调用方可降级。
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
 * - 用户开关（持久化，默认开启）以 Compose State 订阅，关闭后当前页面立即生效；
 * - Android 14（API 34）以下旧系统没有系统预测返回 API，自动降级为普通返回，
 *   设置页会给出明确说明，不会让用户误以为旧系统有系统级手势动画。
 */
@Composable
fun isPredictiveBackEnabled(): Boolean {
    val prefEnabled by rememberPreferenceAsState(
        key = booleanPreferencesKey("predictive_back"),
        defaultValue = true
    )
    return prefEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

/**
 * 应用内预测返回预览（页面级局部状态专用）。
 *
 * 行为约定：
 * - handler 稳定注册，用 `enabled` 控制，不随条件增删；
 * - 手势进行期间页面按进度缩放、位移并降低不透明度；
 * - 手势取消后在不受取消影响的作用域中可靠复位，零业务副作用；
 * - 手势完成后执行 [onBack]（与普通返回完全相同的动作）。
 *
 * 导航栈级别的返回预览由 Navigation 组件负责（依赖版本见 sweet-dependency 配置）；
 * 本 Modifier 只用于需要先消费局部状态的页面（页签、搜索词、底部面板等）。
 */
@Composable
fun Modifier.predictiveBack(
    enabled: Boolean = true,
    onBack: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val maxShiftPx = with(density) { AnimTokens.PREDICTIVE_BACK_SHIFT_DP.dp.toPx() }
    val progress = remember { Animatable(0f) }
    val active = enabled && isPredictiveBackEnabled()

    PredictiveBackHandler(enabled = active) { flow ->
        try {
            flow.collect { event ->
                progress.snapTo(event.progress)
            }
            onBack()
        } finally {
            // 手势取消：在不受取消影响的作用域中完成复位动画
            withContext(NonCancellable) {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = AnimTokens.DURATION_SHORT)
                )
            }
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
