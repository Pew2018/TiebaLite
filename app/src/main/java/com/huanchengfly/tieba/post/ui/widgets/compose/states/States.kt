package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen

/** Initial loading never drives the independently managed pull-refresh indicator. */
val DefaultLoadingScreen: @Composable StateScreenScope.() -> Unit = {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 2.dp,
            color = ExtendedTheme.colors.primary,
        )
    }
}

val DefaultEmptyScreen: @Composable StateScreenScope.() -> Unit = {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        actions = {
            if (canReload) {
                Button(onClick = { reload() }) {
                    Text(text = stringResource(id = R.string.btn_refresh))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

val DefaultErrorScreen: @Composable StateScreenScope.() -> Unit = {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.error_tip)) },
        actions = {
            if (canReload) {
                Button(onClick = { reload() }) {
                    Text(text = stringResource(id = R.string.btn_reload))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun StateScreen(
    isEmpty: Boolean,
    isError: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onReload: (() -> Unit)? = null,
    clickToReload: Boolean = false,
    emptyScreen: @Composable StateScreenScope.() -> Unit = DefaultEmptyScreen,
    errorScreen: @Composable StateScreenScope.() -> Unit = DefaultErrorScreen,
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    content: @Composable StateScreenScope.() -> Unit,
) {
    val stateScreenScope = remember(key1 = onReload) { StateScreenScope(onReload) }
    val clickableModifier = if (onReload != null && clickToReload) Modifier.clickable(
        enabled = isEmpty && !isLoading,
        onClick = onReload
    ) else Modifier
    Box(
        modifier = modifier.then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            stateScreenScope.content()
        } else {
            if (isLoading) {
                stateScreenScope.loadingScreen()
            } else if (isError) {
                stateScreenScope.errorScreen()
            } else {
                stateScreenScope.emptyScreen()
            }
        }
    }
}

class StateScreenScope(
    private val onReload: (() -> Unit)? = null
) {
    val canReload: Boolean
        get() = onReload != null

    fun reload() {
        onReload?.invoke()
    }
}