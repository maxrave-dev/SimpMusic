package com.maxrave.simpmusic.ui.screen.login

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@SuppressLint("SetJavaScriptEnabled")
@UnstableApi
@Composable
fun SpotifyLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val context = LocalContext.current
    val hazeState = rememberHazeState()
    val spotifyStatus by viewModel.spotifyStatus.collectAsState()
    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
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

    // Handle login success
    LaunchedEffect(spotifyStatus) {
        if (spotifyStatus) {
            settingsViewModel.setSpotifyLogIn(true)
            Toast
                .makeText(
                    context,
                    R.string.login_success,
                    Toast.LENGTH_SHORT,
                ).show()
            navController.navigateUp()
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
            // WebView for Spotify login
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    if (url == Config.SPOTIFY_ACCOUNT_URL) {
                                        CookieManager.getInstance().getCookie(url)?.let {
                                            viewModel.saveSpotifySpdc(it)
                                        }
                                        WebStorage.getInstance().deleteAllData()

                                        // Clear all the cookies
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()

                                        clearCache(true)
                                        clearFormData()
                                        clearHistory()
                                        clearSslPreferences()
                                    }
                                }
                            }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(Config.SPOTIFY_LOG_IN_URL)
                    }
                },
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
                    text = stringResource(id = R.string.log_in_to_spotify),
                    style = typo.titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        R.drawable.baseline_arrow_back_ios_new_24,
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
    if (devLoginSheet) {
        DevLogInBottomSheet(
            onDismiss = {
                devLoginSheet = false
            },
            onDone = { spdc ->
                devLoginSheet = false
                val spdcText = "sp_dc=$spdc"
                viewModel.saveSpotifySpdc(spdcText)
                Toast
                    .makeText(
                        context,
                        R.string.login_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                navController.navigateUp()
            },
            type = DevLogInType.Spotify,
        )
    }
}