package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.DiscordLoginState
import com.maxrave.simpmusic.viewModel.LogInViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.discord_login_failed
import simpmusic.composeapp.generated.resources.discord_login_step_1
import simpmusic.composeapp.generated.resources.discord_login_step_2
import simpmusic.composeapp.generated.resources.discord_open_authorize_page
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_confirm
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_hint
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_title
import simpmusic.composeapp.generated.resources.log_in_to_discord
import simpmusic.composeapp.generated.resources.login_success
import simpmusic.composeapp.generated.resources.rich_presence_info

/** Desktop is far wider than any reading measure — the column stops here and centres. */
private val CONTENT_MAX_WIDTH = 420.dp

/**
 * Discord login for Full builds, laid out like [LastfmLoginScreen]: no WebView and no token, the
 * user approves SimpMusic on Discord's own page in their browser, and the redirect brings them back
 * through `simpmusic://discord-auth`, which SharedViewModel completes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordOAuthLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val state by viewModel.discordLoginState.collectAsStateWithLifecycle()
    val loggedIn by viewModel.discordLoggedIn.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var callbackInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetDiscordLoginState()
            showBottomNavigation()
        }
    }

    // Closes on the stored login rather than on this screen's own state, because the redirect is
    // handled by SharedViewModel and never reaches this screen.
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            navController.navigateUp()
        }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            is DiscordLoginState.AwaitingApproval -> uriHandler.openUri(current.authorizeUrl)
            is DiscordLoginState.LoggedIn -> viewModel.makeToast(getString(Res.string.login_success))
            is DiscordLoginState.Failed -> viewModel.makeToast(getString(Res.string.discord_login_failed))
            else -> Unit
        }
    }

    val busy = state is DiscordLoginState.CompletingLogin

    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding() + 96.dp))

            Column(
                modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.rich_presence_info),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                if (busy) {
                    CircularProgressIndicator()
                } else {
                    NumberedStep(
                        number = "1",
                        text = stringResource(Res.string.discord_login_step_1),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startDiscordLogin() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                    ) {
                        Text(
                            text = stringResource(Res.string.discord_open_authorize_page),
                            // typo() bakes a colour into the style, so a Text inside a filled
                            // button keeps the body colour unless the colour is set here.
                            style = typo().labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    // Everything below only matters once the browser is open.
                    if (state is DiscordLoginState.AwaitingApproval) {
                        Spacer(Modifier.height(28.dp))
                        NumberedStep(
                            number = "2",
                            text = stringResource(Res.string.discord_login_step_2),
                        )

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        // Last resort when the redirect never reaches the app: no handler for the
                        // scheme (common on Linux), or a browser that will not hand off to an app.
                        Text(
                            text = stringResource(Res.string.lastfm_paste_callback_title),
                            style = typo().labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = callbackInput,
                            onValueChange = { callbackInput = it },
                            singleLine = true,
                            textStyle = typo().bodySmall,
                            placeholder = {
                                Text(
                                    text = stringResource(Res.string.lastfm_paste_callback_hint),
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.completeDiscordLoginFromCallback(callbackInput) },
                            enabled = callbackInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = CircleShape,
                        ) {
                            Text(
                                text = stringResource(Res.string.lastfm_paste_callback_confirm),
                                style = typo().labelSmall,
                                color =
                                    if (callbackInput.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
        VerticalScrollBar(
            scrollState = scrollState,
            modifier = Modifier.padding(bottom = DesktopMiniPlayerClearance),
        )

        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            title = {
                Text(
                    text = stringResource(Res.string.log_in_to_discord),
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
        )
    }
}
