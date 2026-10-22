package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.DismissValue
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberDismissState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value != DismissValue.Default) {
                hostState.currentSnackbarData?.dismiss()
                true
            } else {
                false
            }
        }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != DismissValue.Default) {
            dismissState.reset()
        }
    }
    SwipeToDismiss(state = dismissState, background = {}) {
        SnackbarHost(hostState = hostState)
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("no scaffold here!") }

@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    collapsibleToolbar: Boolean = false,
    collapseOnRoot: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val maxCollapsePx = with(density) { 96.dp.toPx() }
    val collapseOffset = remember(collapsibleToolbar) { mutableStateOf(0f) }
    val nestedConnection = remember(collapsibleToolbar, maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!collapsibleToolbar || available.y >= 0f) return Offset.Zero
                val before = collapseOffset.value
                val after = (before - available.y).coerceIn(0f, maxCollapsePx)
                collapseOffset.value = after
                return Offset(0f, -(after - before))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!collapsibleToolbar || available.y <= 0f) return Offset.Zero
                val before = collapseOffset.value
                val after = (before - available.y).coerceAtLeast(0f)
                collapseOffset.value = after
                return Offset(0f, after - before)
            }
        }
    }
    val collapseFraction = if (collapsibleToolbar) {
        toolbarCollapseFraction(collapseOffset.value, maxCollapsePx)
    } else 0f
    val scaffoldModifier = if (collapsibleToolbar && collapseOnRoot) modifier.nestedScroll(nestedConnection) else modifier
    CompositionLocalProvider(LocalSnackbarHostState provides scaffoldState.snackbarHostState) {
        CompositionLocalProvider(LocalToolbarCollapseFraction provides collapseFraction) {
        CompositionLocalProvider(LocalToolbarCollapseConnection provides nestedConnection.takeIf { collapsibleToolbar }) {
        Scaffold(
            scaffoldModifier,
            scaffoldState,
            topBar,
            bottomBar,
            snackbarHost,
            floatingActionButton,
            floatingActionButtonPosition,
            isFloatingActionButtonDocked,
            drawerContent,
            drawerGesturesEnabled,
            drawerShape,
            drawerElevation,
            drawerBackgroundColor,
            drawerContentColor,
            drawerScrimColor,
            backgroundColor,
            contentColor,
            content
        )
        }
        }
    }
}
