package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowHeightSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.isPredictiveBackEnabled
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun NavigationWrapper(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == MainNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            NavigationDrawerContent(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        AnimatedVisibility(visible = navigationType == MainNavigationType.NAVIGATION_RAIL) {
            NavigationRail(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MainPage(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val windowHeightSizeClass by rememberUpdatedState(newValue = windowSizeClass.heightSizeClass)
    val windowWidthSizeClass by rememberUpdatedState(newValue = windowSizeClass.widthSizeClass)
    val foldingDevicePosture by LocalDevicePosture.current

    val messageCount by viewModel.uiState.collectPartialAsState(
        prop1 = MainUiState::messageCount,
        initial = 0
    )

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val hideExplore by rememberPreferenceAsState(
        key = booleanPreferencesKey("hideExplore"),
        defaultValue = LocalContext.current.appPreferences.hideExplore
    )
    val pageCount by remember {
        derivedStateOf {
            if (hideExplore) 3 else 4
        }
    }
    val pagerState = rememberPagerState { pageCount }
    LaunchedEffect(hideExplore) {
        if (pagerState.currentPage == 3 && hideExplore) {
            pagerState.scrollToPage(2)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val themeColors = ExtendedTheme.colors
    val navigationItems by remember {
        derivedStateOf {
            listOfNotNull(
                NavigationItem(
                    id = "home",
                    icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        HomePage(
                            canOpenExplore = !LocalContext.current.appPreferences.hideExplore
                        ) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(1)
                            }
                        }
                    }
                ),
                if (hideExplore) null
                else NavigationItem(
                    id = "explore",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_toy_fans)
                    },
                    title = { stringResource(id = R.string.title_explore) },
                    content = {
                        ExplorePage()
                    }
                ),
                NavigationItem(
                    id = "notification",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
                    },
                    title = { stringResource(id = R.string.title_notifications) },
                    badge = messageCount > 0,
                    badgeText = "$messageCount",
                    onClick = {
                        viewModel.send(MainUiIntent.NewMessage.Clear)
                    },
                    content = {
                        NotificationsPage()
                    }
                ),
                NavigationItem(
                    id = "user",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person)
                    },
                    title = { stringResource(id = R.string.title_user) },
                    content = {
                        UserPage()
                    }
                ),
            ).toImmutableList()
        }
    }
    val navigationType by remember {
        derivedStateOf {
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }

                WindowWidthSizeClass.Medium -> {
                    MainNavigationType.NAVIGATION_RAIL
                }

                WindowWidthSizeClass.Expanded -> {
                    if (foldingDevicePosture is DevicePosture.BookPosture) {
                        MainNavigationType.NAVIGATION_RAIL
                    } else {
                        MainNavigationType.PERMANENT_NAVIGATION_DRAWER
                    }
                }

                else -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }
            }
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition by remember {
        derivedStateOf {
            when (windowHeightSizeClass) {
                WindowHeightSizeClass.Compact -> {
                    MainNavigationContentPosition.TOP
                }

                WindowHeightSizeClass.Medium,
                WindowHeightSizeClass.Expanded -> {
                    MainNavigationContentPosition.CENTER
                }

                else -> {
                    MainNavigationContentPosition.TOP
                }
            }
        }
    }
    val onReselected: (Int) -> Unit = {
        coroutineScope.emitGlobalEvent(
            GlobalEvent.Refresh(navigationItems[it].id)
        )
    }
    ProvideNavigator(navigator = navigator) {
        NavigationWrapper(
            currentPosition = pagerState.currentPage,
            onChangePosition = { coroutineScope.launch { pagerState.scrollToPage(it) } },
            onReselected = onReselected,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            MyScaffold(
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AnimatedVisibility(visible = navigationType == MainNavigationType.BOTTOM_NAVIGATION) {
                        BottomNavigation(
                            currentPosition = pagerState.currentPage,
                            onChangePosition = {
                                coroutineScope.launch { pagerState.scrollToPage(it) }
                            },
                            onReselected = onReselected,
                            navigationItems = navigationItems,
                            themeColors = themeColors,
                        )
                    }
                }
            ) { paddingValues ->
                LazyLoadHorizontalPager(
                    contentPadding = paddingValues,
                    state = pagerState,
                    key = { navigationItems[it].id },
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = false
                ) {
                    navigationItems[it].content()
                }
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }
    // 预测返回（Android 14+）：非首页页签返回时，首页即手势中的连续目标。
    // 手势进度直接驱动 pager 向首页滚动（真实首页内容，非占位），
    // 取消后原页签完整恢复（滚动位置与加载状态不变），完成后仅提交一次。
    PredictiveBackHandler(
        enabled = isPredictiveBackEnabled() && pagerState.currentPage != 0
    ) { flow ->
        val startPage = pagerState.currentPage
        var applied = 0f
        var pageWidth = 0
        var committed = false
        try {
            flow.collect { event ->
                if (pageWidth <= 0) {
                    pageWidth = pagerState.layoutInfo.visibleItemsInfo.firstOrNull()?.size
                        ?: pagerState.layoutInfo.viewportSize
                }
                if (pageWidth > 0 && startPage > 0) {
                    val target = -event.progress * startPage * pageWidth
                    val delta = target - applied
                    if (delta != 0f) {
                        pagerState.scrollBy(delta)
                        applied += delta
                    }
                }
            }
            committed = true
            pagerState.scrollToPage(0)
        } finally {
            withContext(NonCancellable) {
                pagerState.scrollToPage(if (committed) 0 else startPage)
            }
        }
    }
}

