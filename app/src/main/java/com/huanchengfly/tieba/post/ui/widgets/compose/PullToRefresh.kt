package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import kotlinx.coroutines.delay

enum class ClassicRefreshPhase { Idle, Pulling, Armed, Refreshing, Settling }

/** Coordinates request lifetime and duplicate gestures; AndroidX owns drag and animation. */
class ClassicRefreshCycle {
    var pending by mutableStateOf(false)
        private set
    var phase by mutableStateOf(ClassicRefreshPhase.Idle)
        private set
    private var sawLoading = false

    fun begin(alreadyLoading: Boolean): Boolean {
        if (pending || alreadyLoading) return false
        pending = true
        sawLoading = false
        phase = ClassicRefreshPhase.Armed
        return true
    }

    fun observe(loading: Boolean) {
        if (!pending) return
        if (loading) {
            sawLoading = true
            phase = ClassicRefreshPhase.Refreshing
        } else if (sawLoading) finish()
    }

    fun expireIfNeverStarted() {
        if (pending && !sawLoading) finish()
    }

    fun finish() {
        pending = false
        sawLoading = false
        phase = ClassicRefreshPhase.Settling
    }

    fun updatePull(progress: Float) {
        if (pending || phase == ClassicRefreshPhase.Settling) return
        phase = when {
            progress >= 1f -> ClassicRefreshPhase.Armed
            progress > 0f -> ClassicRefreshPhase.Pulling
            else -> ClassicRefreshPhase.Idle
        }
    }

    fun settle() {
        if (!pending) phase = ClassicRefreshPhase.Idle
    }
}

@OptIn(ExperimentalMaterialApi::class)
class ClassicRefreshState(
    val state: PullRefreshState,
    val isActive: Boolean,
    val isRefreshing: Boolean,
    val phase: ClassicRefreshPhase,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberClassicPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
): ClassicRefreshState {
    val cycle = remember { ClassicRefreshCycle() }
    val latestRefresh by rememberUpdatedState(onRefresh)
    val latestLoading by rememberUpdatedState(refreshing)

    SideEffect { cycle.observe(refreshing) }
    LaunchedEffect(cycle.pending, refreshing) {
        if (cycle.pending && !refreshing) {
            // Immediate failures must not strand the official indicator at its refresh offset.
            delay(2500)
            cycle.expireIfNeverStarted()
        }
    }
    val state = rememberPullRefreshState(
        // Keep the AndroidX indicator docked from release until request completion.
        refreshing = cycle.pending,
        onRefresh = {
            if (cycle.begin(latestLoading)) {
                try {
                    latestRefresh()
                } catch (throwable: Throwable) {
                    cycle.finish()
                    throw throwable
                }
            }
        },
    )
    SideEffect { cycle.updatePull(state.progress) }
    LaunchedEffect(cycle.phase) {
        if (cycle.phase == ClassicRefreshPhase.Settling) {
            delay(190)
            cycle.settle()
        }
    }
    return ClassicRefreshState(state, cycle.pending, cycle.pending && refreshing, cycle.phase)
}

/**
 * The official Compose Material 2 indicator, based on SwipeRefreshLayout. This removes the
 * competing hand-written offset/alpha/conditional-composition state that caused mid-pull loss.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ClassicPullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ExtendedTheme.colors.pullRefreshIndicator,
    contentColor: Color = ExtendedTheme.colors.progressColor,
    holdAtRest: Boolean = refreshing,
) {
    PullRefreshIndicator(
        refreshing = refreshing || holdAtRest,
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        scale = false,
    )
}
