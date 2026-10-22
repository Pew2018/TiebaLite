package com.huanchengfly.tieba.post.ui.page.settings.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalPageRole
import com.huanchengfly.tieba.post.ui.common.theme.compose.PageRole
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.dependNot
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun MoreSettingsPage(
    navigator: DestinationsNavigator,
) {
    // 设置类页面角色：顶栏/状态栏使用 经典深色
    CompositionLocalProvider(LocalPageRole provides PageRole.SETTINGS) {

    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        collapsibleToolbar = true,
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                expandedTitle = stringResource(id = R.string.title_settings_more),
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_more),
                        fontWeight = FontWeight.Medium, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current
        var cacheSize by remember { mutableStateOf("0.0B") }
        LaunchedEffect(Unit) {
            thread {
                cacheSize = ImageCacheUtil.getCacheSize(context)
            }
        }
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            dividerThickness = 1.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (context.appPreferences.showExperimentalFeatures) {
                prefsItem {
                    SwitchPref(
                        key = "checkCIUpdate",
                        title = stringResource(id = R.string.title_check_ci_update),
                        defaultChecked = false,
                        summary = stringResource(id = R.string.tip_check_ci_update)
                    )
                }
            }
            prefsItem {
                SwitchPref(
                    key = "use_webview",
                    title = stringResource(id = R.string.title_use_webview),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_webview_on)
                        else stringResource(id = R.string.tip_use_webview)
                    },
                )
            }
            prefsItem {
                SwitchPref(
                    enabled = dependNot(key = "use_webview", defaultValue = true),
                    key = "use_custom_tabs",
                    title = stringResource(id = R.string.title_use_custom_tabs),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_custom_tab_on)
                        else stringResource(id = R.string.tip_use_custom_tab)
                    },
                )
            }
            prefsItem {
                TextPref(
                    enabled = true,
                    title = stringResource(id = R.string.title_clear_picture_cache),
                    onClick = {
                        coroutineScope.launch {
                            ImageCacheUtil.clearImageAllCache(context)
                            cacheSize = "0.0B"
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        }
                    },
                    summary = stringResource(id = R.string.tip_cache, cacheSize)
                )
            }
            prefsItem {
                TextPref(
                    enabled = true,
                    title = stringResource(id = R.string.title_about),
                    onClick = {
                        navigator.navigate(AboutPageDestination)
                    },
                    summary = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME)
                )
            }
        }
    }

    }
}
