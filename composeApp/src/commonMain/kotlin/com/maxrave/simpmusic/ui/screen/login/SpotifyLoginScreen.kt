package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.LogoDev
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.Config
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.ui.component.DevCookieLogInBottomSheet
import com.maxrave.simpmusic.ui.component.DevLogInBottomSheet
import com.maxrave.simpmusic.ui.component.DevLogInType
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.multiplatform.webview.cookie.WebViewCookieManager
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.log_in_to_spotify
import simpmusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun SpotifyLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val spotifyStatus by viewModel.spotifyStatus.collectAsStateWithLifecycle()

    val fullSpotifyCookies by viewModel.fullSpotifyCookies.collectAsStateWithLifecycle()

    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showCookiesBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val state = rememberWebViewState(Config.SPOTIFY_LOG_IN_URL)
    val navigator = rememberWebViewNavigator()

    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
        WebViewCookieManager().removeAllCookies()
        state.webSettings.apply {
            isJavaScriptEnabled = true
            androidWebSettings.apply {
                domStorageEnabled = true
            }
        }
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    // Handle login success
    LaunchedEffect(spotifyStatus) {
        if (spotifyStatus) {
            settingsViewModel.setSpotifyLogIn(true)
            viewModel.makeToast(
                getString(Res.string.login_success),
            )
            navController.navigateUp()
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.loadingState }.collect { loadingState ->
            if (loadingState is LoadingState.Finished) {
                state.lastLoadedUrl?.let { WebViewCookieManager().getCookies(it) }?.let { cookie ->
                    val cookies =
                        cookie.map {
                            it.name to it.value
                        }
                    viewModel.setFullSpotifyCookies(cookies)
                }
                if (state.lastLoadedUrl == Config.SPOTIFY_ACCOUNT_URL) {
                    WebViewCookieManager()
                        .getCookies(
                            Config.SPOTIFY_ACCOUNT_URL,
                        ).let {
                            viewModel.saveSpotifySpdc(
                                it.joinToString("; ") {
                                    "${it.name}=${it.value}"
                                },
                            )
                        }
                    // Clear all the cookies
                    WebViewCookieManager().removeAllCookies()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            WebView(
                state = state,
                navigator = navigator,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = stringResource(Res.string.log_in_to_spotify),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        Res.drawable.baseline_arrow_back_ios_new_24,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        devLoginSheet = true
                    },
                ) {
                    Icon(
                        Icons.Default.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )

        FloatingActionButton(
            onClick = {
                showCookiesBottomSheet = true
            },
            containerColor = Color(0xFF40D96A),
            modifier =
                Modifier
                    .align(
                        Alignment.BottomStart,
                    ).padding(innerPadding)
                    .padding(
                        25.dp,
                    ),
        ) {
            Icon(
                Icons.Default.Cookie,
                "Cookies",
            )
        }
    }
    if (devLoginSheet) {
        DevLogInBottomSheet(
            onDismiss = {
                devLoginSheet = false
            },
            onDone = { spdc ->
                devLoginSheet = false
                val spdcText = "sp_dc=$spdc"
                viewModel.saveSpotifySpdc(spdcText)
                viewModel.makeToast(
                    getStringBlocking(Res.string.login_success),
                )
                navController.navigateUp()
            },
            type = DevLogInType.Spotify,
        )
    }

    if (showCookiesBottomSheet) {
        DevCookieLogInBottomSheet(
            onDismiss = {
                showCookiesBottomSheet = false
            },
            type = DevLogInType.Spotify,
            cookies = fullSpotifyCookies,
        )
    }
}