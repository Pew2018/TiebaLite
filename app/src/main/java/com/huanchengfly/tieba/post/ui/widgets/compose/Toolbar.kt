package com.huanchengfly.tieba.post.ui.widgets.compose

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalPageRole
import com.huanchengfly.tieba.post.ui.common.theme.compose.PageRole
import com.huanchengfly.tieba.post.ui.common.theme.compose.ReportStatusBarSurface
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass.Companion.Compact
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

val LocalToolbarCollapseFraction = androidx.compose.runtime.staticCompositionLocalOf { 0f }
val LocalToolbarCollapseConnection = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.input.nestedscroll.NestedScrollConnection?> { null }

internal fun toolbarCollapseFraction(offsetPx: Float, maxOffsetPx: Float): Float =
    if (maxOffsetPx <= 0f) 0f else (offsetPx / maxOffsetPx).coerceIn(0f, 1f)

@Composable
fun rememberRoleColors(): RoleColors {
    val colors = ExtendedTheme.colors
    return when (LocalPageRole.current) {
        PageRole.SETTINGS -> RoleColors(
            topBar = colors.settingsTopBar,
            statusBar = colors.settingsStatusBar,
            onTopBar = colors.onSettingsTopBar,
            onTopBarSecondary = colors.onSettingsTopBarSecondary,
        )

        PageRole.MAIN -> RoleColors(
            topBar = colors.topBar,
            statusBar = colors.statusBar,
            onTopBar = colors.onTopBar,
            onTopBarSecondary = colors.onTopBarSecondary,
        )
    }
}

data class RoleColors(
    val topBar: Color,
    val statusBar: Color,
    val onTopBar: Color,
    val onTopBarSecondary: Color,
)

@Composable
fun accountNavIconIfCompact(): (@Composable () -> Unit)? =
    if (LocalWindowSizeClass.current.widthSizeClass == Compact) (@Composable { AccountNavIcon() })
    else null

@Composable
fun AccountNavIcon(
    onClick: (() -> Unit)? = null,
    spacer: Boolean = true,
    size: Dp = Sizes.Small
) {
    val navigator = LocalNavigator.current
    val currentAccount = LocalAccount.current
    if (spacer) Spacer(modifier = Modifier.width(12.dp))
    if (currentAccount == null) {
        Image(
            painter = rememberDrawablePainter(
                drawable = ContextCompat.getDrawable(
                    LocalContext.current,
                    R.drawable.ic_launcher_new_round
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(size)
        )
    } else {
        val context = LocalContext.current
        val menuState = rememberMenuState()
        LongClickMenu(
            menuContent = {
                val allAccounts = AccountUtil.allAccounts
                allAccounts.forEach {
                    DropdownMenuItem(onClick = { AccountUtil.switchAccount(context, it.id) }) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(Sizes.Small)
                        ) {
                            Avatar(
                                data = StringUtil.getAvatarUrl(it.portrait),
                                contentDescription = stringResource(id = R.string.title_switch_account_long_press),
                                size = Sizes.Small,
                            )
                            if (currentAccount.id == it.id) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(id = R.string.desc_current_account),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = Color.Black.copy(0.35f))
                                        .padding(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = it.nameShow ?: it.name)
                    }
                }
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DropdownMenuItem(
                    onClick = {
                        navigator.navigate(LoginPageDestination)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.title_new_account),
                        tint = ExtendedTheme.colors.onChip,
                        modifier = Modifier
                            .size(Sizes.Small)
                            .clip(CircleShape)
                            .background(color = ExtendedTheme.colors.chip)
                            .padding(8.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(id = R.string.title_new_account))
                }
            },
            menuState = menuState,
            onClick = onClick,
            shape = CircleShape
        ) {
            Avatar(
                data = StringUtil.getAvatarUrl(currentAccount.portrait),
                size = size,
                contentDescription = stringResource(id = R.string.title_switch_account_long_press)
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    ProvideContentColor(color = rememberRoleColors().onTopBar) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun BackNavigationIcon(onBackPressed: () -> Unit) {
    ProvideContentColor(color = rememberRoleColors().onTopBar) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_arrow_back),
                contentDescription = stringResource(id = R.string.button_back),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Deprecated(
    "Use the non deprecated overload",
    ReplaceWith(
        """TitleCentredToolbar(
                title = { Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6) },
                modifier = modifier,
                insets = insets,
                navigationIcon = navigationIcon,
                actions = actions,
                content = content
            )""",
        "androidx.compose.ui.text.font.FontWeight",
        "androidx.compose.material.MaterialTheme",
        "androidx.compose.material.Text"
    )
)
@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    expandedTitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TitleCentredToolbar(
        title = {
            Text(text = title)
        },
        modifier = modifier,
        insets = insets,
        expandedTitle = expandedTitle,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    expandedTitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val roleColors = rememberRoleColors()
    TopAppBarContainer(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(56.dp),
                backgroundColor = roleColors.topBar,
                contentColor = roleColors.onTopBar,
                elevation = 0.dp
            ) {
                ProvideContentColor(color = roleColors.onTopBar) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navigationIcon?.invoke()
                        Spacer(Modifier.width(16.dp))
                        // weight/actions 必须作为 Row 的直接子项（RowScope 由 Row 提供），
                        // 不能包在 Provide* 的普通 () -> Unit lambda 内，否则拿不到 RowScope。
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val collapse = LocalToolbarCollapseFraction.current
                            Box(modifier = Modifier.graphicsLayer {
                                alpha = if (expandedTitle == null) 1f else collapse
                            }) {
                                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                                    ProvideContentColor(color = roleColors.onTopBar) {
                                        title()
                                    }
                                }
                            }
                        }
                        actions()
                    }
                }
            }
        },
        modifier = modifier,
        insets = insets,
        expandedTitle = expandedTitle,
        content = content
    )
}

@Composable
fun Toolbar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    expandedTitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        title = {
            Text(text = title)
        },
        expandedTitle = expandedTitle,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@Composable
fun Toolbar(
    title: @Composable (() -> Unit),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = ExtendedTheme.colors.topBar,
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    expandedTitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val roleColors = rememberRoleColors()
    val collapse = LocalToolbarCollapseFraction.current
    val resolvedBackground = backgroundColor.takeIf { it != ExtendedTheme.colors.topBar } ?: roleColors.topBar
    val resolvedContent = contentColor.takeIf { it != ExtendedTheme.colors.onTopBar } ?: roleColors.onTopBar
    TopAppBarContainer(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(56.dp),
                title = {
                    ProvideTextStyle(value = MaterialTheme.typography.h6) {
                        Box(modifier = Modifier.graphicsLayer {
                            alpha = if (expandedTitle == null) 1f else collapse
                        }) {
                            ProvideContentColor(color = resolvedContent, content = title)
                        }
                    }
                },
                actions = {
                    ProvideContentColor(color = resolvedContent) {
                        actions()
                    }
                },
                navigationIcon = (@Composable {
                    ProvideContentColor(color = resolvedContent) {
                        navigationIcon?.invoke()
                    }
                }).takeIf { navigationIcon != null },
                backgroundColor = resolvedBackground,
                contentColor = resolvedContent,
                elevation = 0.dp
            )
        },
        expandedTitle = expandedTitle,
        content = content
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopAppBarContainer(
    topBar: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    expandedTitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val statusBarModifier = if (insets) {
        Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
    } else {
        Modifier
    }
    val coroutineScope = rememberCoroutineScope()
    val roleColors = rememberRoleColors()
    val collapse = LocalToolbarCollapseFraction.current
    // 系统栏图标明暗由实际绘制在状态栏后方的表面决定
    ReportStatusBarSurface(surface = roleColors.statusBar)
    Column(modifier) {
        Spacer(
            modifier = statusBarModifier
                .fillMaxWidth()
                .background(color = roleColors.statusBar)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onDoubleClick = {
                        Log.i("TopAppBarContainer", "TopAppBarContainer: onDoubleClick")
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop)
                    },
                    onClick = {},
                ),
            content = topBar
        )
        if (expandedTitle != null) {
            val expandedHeight = 96.dp * (1f - collapse)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedHeight)
                    .background(roleColors.topBar)
                    .graphicsLayer { alpha = 1f - collapse }
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = expandedTitle,
                    color = roleColors.onTopBar,
                    fontSize = (32f - 12f * collapse).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
        content?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = roleColors.topBar),
            ) {
                content()
            }
        }
    }
}
