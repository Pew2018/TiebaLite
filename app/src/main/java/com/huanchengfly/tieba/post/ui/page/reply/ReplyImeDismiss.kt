package com.huanchengfly.tieba.post.ui.page.reply

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.getActivityWindow
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.getDialogWindow
import com.huanchengfly.tieba.post.utils.hideKeyboard
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Shared by the navigation scrim and the reply destination hosted in that sheet. */
internal class ReplySheetDismissHandlers {
    var close: (() -> Unit)? = null
    var allowHide = false
}

internal val LocalReplySheetDismissHandlers = staticCompositionLocalOf<ReplySheetDismissHandlers?> { null }

/** Must be called inside the actual reply host, whether it is an Activity sheet or a Dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberReplyDismissRequest(): (Boolean, () -> Unit) -> Unit {
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val window = getDialogWindow() ?: getActivityWindow()
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val target = WindowInsets.imeAnimationTarget
    val scope = rememberCoroutineScope()
    val coordinator = remember { ReplyDismissCoordinator() }
    var pendingFinish by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun imeVisible(): Boolean =
        ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true ||
            ime.getBottom(density) > 0 || target.getBottom(density) > 0

    return { fullClose, finish ->
        val closeAfterHide = fullClose || !imeVisible()
        if (closeAfterHide) pendingFinish = finish
        if (coordinator.request(closeAfterHide)) {
            window?.currentFocus?.let { focused ->
                hideKeyboard(focused.context, focused)
                focused.clearFocus()
            }
            window?.let { WindowCompat.getInsetsController(it, view).hide(WindowInsetsCompat.Type.ime()) }
            keyboard?.hide()
            scope.launch {
                withTimeoutOrNull(1500) {
                    var settledFrames = 0
                    while (settledFrames < 2) {
                        withFrameNanos { }
                        settledFrames = if (imeVisible()) 0 else settledFrames + 1
                    }
                }
                if (coordinator.imeHidden()) pendingFinish?.invoke()
            }
        }
    }
}
