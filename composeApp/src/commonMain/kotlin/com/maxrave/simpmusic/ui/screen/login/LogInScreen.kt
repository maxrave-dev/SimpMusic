package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.maxrave.common.Config
import com.maxrave.domain.repository.BrowserLoginFailure
import com.maxrave.domain.repository.BrowserLoginTarget
import com.maxrave.domain.repository.BrowserProfile
import com.maxrave.domain.repository.BrowserProfileStatus
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.expect.ui.PlatformWebView
import com.maxrave.simpmusic.expect.ui.createWebViewCookieManager
import com.maxrave.simpmusic.expect.ui.rememberWebViewState
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.ui.component.DevLogInBottomSheet
import com.maxrave.simpmusic.ui.component.DevLogInType
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SettingGroup
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.LogoDev
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.BrowserLoginViewModel
import com.maxrave.simpmusic.viewModel.BrowserLoginUiState
import com.maxrave.simpmusic.viewModel.requiresFileAccess
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.browser_login_busy
import simpmusic.composeapp.generated.resources.browser_login_description
import simpmusic.composeapp.generated.resources.browser_login_found
import simpmusic.composeapp.generated.resources.browser_login_full_disk_access
import simpmusic.composeapp.generated.resources.browser_login_importing
import simpmusic.composeapp.generated.resources.browser_login_key_unavailable
import simpmusic.composeapp.generated.resources.browser_login_missing
import simpmusic.composeapp.generated.resources.browser_login_permission
import simpmusic.composeapp.generated.resources.browser_login_open_settings
import simpmusic.composeapp.generated.resources.browser_login_refresh
import simpmusic.composeapp.generated.resources.browser_login_rejected
import simpmusic.composeapp.generated.resources.browser_login_unreadable
import simpmusic.composeapp.generated.resources.browser_login_unsupported
import simpmusic.composeapp.generated.resources.browser_login_youtube
import simpmusic.composeapp.generated.resources.spotify
import simpmusic.composeapp.generated.resources.loading
import simpmusic.composeapp.generated.resources.log_in
import simpmusic.composeapp.generated.resources.login_failed
import simpmusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val coroutineScope = rememberCoroutineScope()
    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val state = rememberWebViewState()
    // Resolve the Desktop-only dependency only on Desktop. Android keeps its existing WebView.
    val browserViewModel = if (getPlatform() == Platform.Desktop) koinViewModel<BrowserLoginViewModel>() else null
    val browserState = browserViewModel?.state?.collectAsStateWithLifecycle()?.value
    val isMacOS = browserViewModel != null && getPlatform().osName().lowercase().let { "mac" in it || "darwin" in it }

    if (browserViewModel != null && isMacOS) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            browserViewModel.onResumeAfterFileAccess()
        }
    }

    LaunchedEffect(browserViewModel) {
        browserViewModel?.refresh(BrowserLoginTarget.YOUTUBE)
    }
    LaunchedEffect(browserState?.completed) {
        if (browserState?.completed == true) {
            viewModel.makeToast(getString(Res.string.login_success))
            navController.navigateUp()
        }
    }

    // Android still hosts this in aboveContent (above its WebView). Desktop hosts it outside the
    // browser panel, so replacing PlatformWebView does not strand the existing developer button.
    val manualLogin: @Composable () -> Unit = {
        if (devLoginSheet) {
            DevLogInBottomSheet(
                onDismiss = { devLoginSheet = false },
                onDone = { cookie ->
                    coroutineScope.launch {
                        val success = settingsViewModel.addAccount(cookie)
                        if (success) {
                            viewModel.makeToast(getString(Res.string.login_success))
                            navController.navigateUp()
                        } else {
                            viewModel.makeToast(getString(Res.string.login_failed))
                        }
                    }
                },
                type = DevLogInType.YouTube,
            )
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.value }.collect {
            Logger.d(
                "LogInScreen",
                "WebViewState: ${
                    when (it) {
                        is com.maxrave.simpmusic.expect.ui.WebViewState.Finished -> "Finished"
                        is com.maxrave.simpmusic.expect.ui.WebViewState.Loading -> "Loading ${it.progress}%"
                    }
                }",
            )
        }
    }

    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column(if (browserViewModel != null) Modifier.fillMaxSize() else Modifier) {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            val desktopState = browserState
            if (browserViewModel != null && desktopState != null) {
                if (!desktopState.loading && desktopState.profiles.isEmpty() && desktopState.error == null) {
                    // No browser found: retain the exact existing guide text and blog button.
                    PlatformWebView(state, Config.LOG_IN_URL, aboveContent = {}, clearCookies = false, onPageFinished = {})
                } else {
                    BrowserLoginPanel(
                        state = desktopState,
                        target = BrowserLoginTarget.YOUTUBE,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        isMacOS = isMacOS,
                        onRefresh = browserViewModel::refresh,
                        onOpenFileAccess = {
                            browserViewModel.onFileAccessRequested()
                            openUrl("x-apple.systempreferences:com.apple.preference.security?Privacy_AllFiles")
                        },
                        onSelect = { profile ->
                            browserViewModel.importProfile(profile, BrowserLoginTarget.YOUTUBE) { cookies ->
                                settingsViewModel.addAccount(cookies.cookie, cookies.netscapeCookie)
                            }
                        },
                    )
                }
            } else {
                // WebView for YouTube Music login, unchanged on Android.
                PlatformWebView(
                    state,
                    Config.LOG_IN_URL,
                    aboveContent = {
                        manualLogin()
                    },
                    clearCookies = true,
                ) { url ->
                    Logger.d("LogInScreen", "Current URL: $url")
                    if (url == Config.YOUTUBE_MUSIC_MAIN_URL) {
                        coroutineScope.launch {
                            val success =
                                createWebViewCookieManager()
                                    .getCookie(url)
                                    .takeIf {
                                        it.isNotEmpty()
                                    }?.let {
                                        settingsViewModel.addAccount(it)
                                    } ?: false

                            createWebViewCookieManager().removeAllCookies()

                            if (success) {
                                viewModel.makeToast(getString(Res.string.login_success))
                                navController.navigateUp()
                            } else {
                                viewModel.makeToast(getString(Res.string.login_failed))
                            }
                        }
                    }
                }
            }
        }

        if (browserViewModel != null) manualLogin()

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
            title = {
                Text(
                    text = stringResource(Res.string.log_in),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        SimpIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                IconButton(
                    enabled = browserState?.importingId == null,
                    onClick = {
                        devLoginSheet = true
                    },
                ) {
                    Icon(
                        SimpIcons.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}

@Composable
internal fun BrowserLoginPanel(
    state: BrowserLoginUiState,
    target: BrowserLoginTarget,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isMacOS: Boolean,
    onRefresh: () -> Unit,
    onOpenFileAccess: () -> Unit,
    onSelect: (BrowserProfile) -> Unit,
) {
    val busy = state.loading || state.importingId != null
    val serviceName = stringResource(if (target == BrowserLoginTarget.YOUTUBE) Res.string.browser_login_youtube else Res.string.spotify)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding + 24.dp),
    ) {
        item(key = "description") {
            Text(
                stringResource(Res.string.browser_login_description, serviceName),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (busy) {
            item(key = "progress") {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(
                        stringResource(if (state.loading) Res.string.loading else Res.string.browser_login_importing),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        state.profiles.groupBy { it.browser }.forEach { (browser, profiles) ->
            item(key = "browser:$browser") {
                SettingGroup(title = browser) {
                    profiles.forEach { profile ->
                        val needsFileAccess = state.requiresFileAccess(profile, isMacOS)
                        val subtitle = when (profile.status) {
                            BrowserProfileStatus.COOKIES_FOUND -> Res.string.browser_login_found
                            BrowserProfileStatus.NO_COOKIES -> Res.string.browser_login_missing
                            BrowserProfileStatus.PERMISSION_REQUIRED -> if (isMacOS) Res.string.browser_login_full_disk_access else Res.string.browser_login_permission
                            BrowserProfileStatus.UNREADABLE -> Res.string.browser_login_unreadable
                            BrowserProfileStatus.UNSUPPORTED -> Res.string.browser_login_unsupported
                        }
                        SettingItem(
                            title = profile.name,
                            subtitle = if (profile.status == BrowserProfileStatus.COOKIES_FOUND || profile.status == BrowserProfileStatus.NO_COOKIES) {
                                stringResource(subtitle, serviceName)
                            } else {
                                stringResource(subtitle)
                            },
                            isEnable = !busy && profile.status != BrowserProfileStatus.UNSUPPORTED,
                            onClick = { if (needsFileAccess) onOpenFileAccess() else onSelect(profile) },
                            otherView = if (needsFileAccess) {
                                {
                                    TextButton(onClick = onOpenFileAccess, enabled = !busy) {
                                        Text(stringResource(Res.string.browser_login_open_settings), color = LocalContentColor.current)
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
        val error = state.error
        if (error != null || state.rejectedSession) {
            item(key = "error") {
                val message = when {
                    state.rejectedSession -> Res.string.browser_login_rejected
                    error == BrowserLoginFailure.FILE_ACCESS_DENIED -> if (isMacOS) Res.string.browser_login_full_disk_access else Res.string.browser_login_permission
                    error == BrowserLoginFailure.ACCESS_DENIED -> Res.string.browser_login_permission
                    error == BrowserLoginFailure.KEY_UNAVAILABLE -> Res.string.browser_login_key_unavailable
                    error == BrowserLoginFailure.UNSUPPORTED_ENCRYPTION -> Res.string.browser_login_unsupported
                    error == BrowserLoginFailure.DATABASE_BUSY -> Res.string.browser_login_busy
                    error == BrowserLoginFailure.NO_COOKIES -> Res.string.browser_login_missing
                    else -> Res.string.browser_login_unreadable
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (error == BrowserLoginFailure.NO_COOKIES) stringResource(message, serviceName) else stringResource(message),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item(key = "refresh") {
            TextButton(onClick = onRefresh, enabled = !busy) {
                Text(stringResource(Res.string.browser_login_refresh), color = LocalContentColor.current)
            }
        }
        item(key = "end_of_list") {
            EndOfPage()
        }
    }
}
