package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshDefaults.LoadPreUpPostDownNestedScrollConnection
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class ClassicRefreshPhase { Idle, Pulling, Armed, Refreshing, Settling }

/**
 * A user-initiated refresh cycle. Initial page loads and pagination do not create a cycle.
 * One gesture starts one request; completion/failure/cancellation is observed through the
 * ViewModel's refreshing flag. The short fallback also releases requests that never emit a
 * loading state (e.g. an immediate synchronous failure).
 */
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
            // If no ViewModel loading transition is ever emitted, do not strand the circle.
            delay(2500)
            cycle.expireIfNeverStarted()
        }
    }
    val state = rememberPullRefreshState(
        refreshing = refreshing && cycle.pending,
        onRefresh = {
            if (cycle.begin(latestLoading)) {
                try { latestRefresh() } catch (e: Throwable) {
                    cycle.finish()
                    throw e
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
 * Classic 40dp pull-to-refresh indicator.  At rest its complete circle is above the
 * content (y = -40dp, alpha = 0). Drag position follows Material 2 progress without
 * spring lag, while release and completion settle using a short classic easing.
 * Arrow/arc and rotating spinner crossfade rather than swapping abruptly.
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
    val progress = state.progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val hiddenPx = with(density) { (-40).dp.toPx() }
    val dockPx = with(density) { PullToRefreshDefaults.RefreshingOffset.toPx() }
    val offsetTarget = if (holdAtRest) dockPx else hiddenPx + (dockPx - hiddenPx) * progress
    val offset by animateFloatAsState(
        targetValue = offsetTarget,
        animationSpec = if (!holdAtRest && progress > 0f) snap() else
            tween(durationMillis = 190, easing = LinearOutSlowInEasing),
        label = "classicRefreshCirclePosition",
    )
    val visibility = ((offset - hiddenPx) / (dockPx - hiddenPx)).coerceIn(0f, 1f)
    // Keeping a fully hidden static circle is harmless; never run an idle spinner.
    if (!holdAtRest && progress <= 0f && visibility <= 0f) return
    Box(
        modifier = modifier
            .alpha(visibility)
            .offset { IntOffset(0, offset.roundToInt()) }
            .shadow(3.dp, CircleShape)
            .size(40.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = refreshing, animationSpec = tween(140), label = "classicRefreshGlyph") { spinning ->
            if (spinning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Canvas(modifier = Modifier.size(22.dp).rotate(progress * 180f)) {
                    val stroke = 2.dp.toPx()
                    val arcProgress = if (holdAtRest) 1f else progress
                    drawArc(
                        color = contentColor,
                        startAngle = -90f,
                        sweepAngle = (arcProgress * 270f).coerceAtLeast(1f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (arcProgress > 0.3f) {
                        val center = size.center
                        val arrowLength = size.width * 0.24f
                        val end = Offset(center.x, size.height * 0.91f)
                        drawLine(contentColor, Offset(end.x, end.y - arrowLength),
                            end, stroke, StrokeCap.Round)
                        drawLine(contentColor, end,
                            Offset(end.x - arrowLength * 0.45f, end.y - arrowLength * 0.45f),
                            stroke, StrokeCap.Round)
                        drawLine(contentColor, end,
                            Offset(end.x + arrowLength * 0.45f, end.y - arrowLength * 0.45f),
                            stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

// Compatibility surface for older callers. The Forum page has migrated to the sole Material 2
// pullRefresh gesture owner; don't mount this legacy swipeable container together with it.
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToRefreshLayout(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicator: @Composable BoxScope.(isRefreshing: Boolean, willLoad: Boolean) -> Unit = { isRefreshing, willLoad ->
        val indicatorColor = ExtendedTheme.colors.progressColor
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .shadow(3.dp, CircleShape)
                .size(40.dp)
                .background(ExtendedTheme.colors.windowBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = indicatorColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (willLoad) 180f else 0f)
                ) {
                    val x = size.width / 2f
                    val top = size.height * 0.2f
                    val bottom = size.height * 0.78f
                    val wing = size.width * 0.22f
                    drawLine(indicatorColor, Offset(x, top), Offset(x, bottom), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(indicatorColor, Offset(x, bottom), Offset(x - wing, bottom - wing), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(indicatorColor, Offset(x, bottom), Offset(x + wing, bottom - wing), 2.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    },
    refreshDistance: Dp = PullToRefreshDefaults.RefreshDistance,
    refreshingOffset: Dp = PullToRefreshDefaults.RefreshingOffset,
    threshold: Float = 0.75f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    val curThreshold by rememberUpdatedState(newValue = threshold)

    val refreshDistancePx = remember(refreshDistance) {
        with(density) { refreshDistance.toPx() }
    }
    val refreshingOffsetPx = remember(refreshingOffset) {
        with(density) { refreshingOffset.toPx() }
    }

    val swipeableState = rememberSwipeableState(false) {
        if (it && !refreshing) {
            onRefresh()
        }
        false
    }
    val showIndicator by remember {
        derivedStateOf { swipeableState.offset.value > (-refreshDistancePx + refreshingOffsetPx) }
    }
    val layoutOffsetY by remember {
        derivedStateOf {
            swipeableState.offset.value + refreshDistancePx
        }
    }
    Box {
        Box(
            modifier = Modifier
                .nestedScroll(swipeableState.LoadPreUpPostDownNestedScrollConnection)
                .swipeable(
                    state = swipeableState,
                    anchors = mapOf(
                        -refreshDistancePx to false,
                        refreshDistancePx to true,
                    ),
                    thresholds = { _, _ -> FractionalThreshold(curThreshold) },
                    orientation = Orientation.Vertical,
                ),
        ) {
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        x = 0,
                        y = layoutOffsetY.toInt()
                    )
                }
            ) {
                content()
            }
        }

        AnimatedVisibility(
            visible = showIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            indicator(refreshing, swipeableState.targetValue)
        }
    }
}

object PullToRefreshDefaults {
    val RefreshingOffset = 36.dp

    val RefreshDistance = 72.dp

    internal val <T> SwipeableState<T>.LoadPreUpPostDownNestedScrollConnection: NestedScrollConnection
        get() = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.toFloat()
                return if (delta < 0 && source == NestedScrollSource.Drag) {
                    performDrag(delta).toOffset()
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return if (source == NestedScrollSource.Drag) {
                    performDrag(available.toFloat()).toOffset()
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val toFling = Offset(available.x, available.y).toFloat()
                return if (toFling < 0 && offset.value > minBound) {
                    performFling(velocity = toFling)
                    // since we go to the anchor with tween settling, consume all for the best UX
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                performFling(velocity = Offset(available.x, available.y).toFloat())
                return available
            }

            private fun Float.toOffset(): Offset = Offset(0f, this)

            private fun Offset.toFloat(): Float = this.y
        }
}
