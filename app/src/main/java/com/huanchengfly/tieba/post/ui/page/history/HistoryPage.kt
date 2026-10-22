package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ClassicPageTabs
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://history")
    ]
)
@Composable
fun HistoryPage(
    navigator: DestinationsNavigator
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
        topBar = {
            Column {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_history),
                            fontWeight = FontWeight.Medium, style = MaterialTheme.typography.h6
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                HistoryUtil.deleteAll()
                                emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                                launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.toast_clear_success
                                        )
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(id = R.string.title_history_delete),
                                tint = ExtendedTheme.colors.onTopBar
                            )
                        }
                    },
                )
                ClassicPageTabs(
                    pagerState = pagerState,
                    onTabClick = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                ) { index ->
                    Text(
                        text = stringResource(
                            if (index == 0) R.string.title_history_thread else R.string.title_history_forum
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                key = { it },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                if (it == 0) {
                    HistoryListPage(type = HistoryUtil.TYPE_THREAD)
                } else {
                    HistoryListPage(type = HistoryUtil.TYPE_FORUM)
                }
            }
        }
    }
}
