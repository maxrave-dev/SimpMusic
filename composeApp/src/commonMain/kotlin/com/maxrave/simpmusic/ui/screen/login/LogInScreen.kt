package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LogoDev
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.common.Config
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformWebView
import com.maxrave.simpmusic.expect.ui.createWebViewCookieManager
import com.maxrave.simpmusic.expect.ui.rememberWebViewState
import com.maxrave.simpmusic.ui.component.DevLogInBottomSheet
import com.maxrave.simpmusic.ui.component.DevLogInType
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.log_in
import simpmusic.composeapp.generated.resources.login_failed
import simpmusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
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
        createWebViewCookieManager().removeAllCookies()
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
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
            // WebView for YouTube Music login
            PlatformWebView(
                state,
                Config.LOG_IN_URL,
                aboveContent = {
                    if (devLoginSheet) {
                        DevLogInBottomSheet(
                            onDismiss = {
                                devLoginSheet = false
                            },
                            onDone = { cookie, netscapeCookie ->
                                coroutineScope.launch {
                                    val success = settingsViewModel.addAccount(cookie, netscapeCookie)
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
                    text = stringResource(Res.string.log_in),
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
    }
}