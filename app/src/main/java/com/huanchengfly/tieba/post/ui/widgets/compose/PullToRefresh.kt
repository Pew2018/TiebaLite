package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshDefaults.LoadPreUpPostDownNestedScrollConnection

/** Shared classic Material Design pull indicator used by every Material pull-refresh screen. */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ClassicPullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ExtendedTheme.colors.pullRefreshIndicator,
    contentColor: Color = ExtendedTheme.colors.progressColor,
) {
    val progress = state.progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .shadow(3.dp, CircleShape)
            .size(40.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f * progress)
            ) {
                val x = size.width / 2f
                val top = size.height * 0.2f
                val bottom = size.height * 0.78f
                val wing = size.width * 0.22f
                drawLine(contentColor, Offset(x, top), Offset(x, bottom), 2.dp.toPx(), StrokeCap.Round)
                drawLine(contentColor, Offset(x, bottom), Offset(x - wing, bottom - wing), 2.dp.toPx(), StrokeCap.Round)
                drawLine(contentColor, Offset(x, bottom), Offset(x + wing, bottom - wing), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

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
