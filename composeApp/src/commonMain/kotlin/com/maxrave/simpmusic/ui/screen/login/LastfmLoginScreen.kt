package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
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
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LastfmLoginState
import com.maxrave.simpmusic.viewModel.LogInViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lastfm_login_failed
import simpmusic.composeapp.generated.resources.lastfm_login_step_1
import simpmusic.composeapp.generated.resources.lastfm_login_step_2
import simpmusic.composeapp.generated.resources.lastfm_open_authorize_page
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_confirm
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_hint
import simpmusic.composeapp.generated.resources.lastfm_paste_callback_title
import simpmusic.composeapp.generated.resources.log_in_to_lastfm
import simpmusic.composeapp.generated.resources.login_success
import simpmusic.composeapp.generated.resources.scrobbling_info

/** Desktop is far wider than any reading measure — the column stops here and centres. */
private val CONTENT_MAX_WIDTH = 420.dp

/**
 * Last.fm's desktop auth flow, which is what SimpMusic uses on every platform.
 *
 * There is no WebView here on purpose — unlike the Discord and Spotify screens, this never sees the
 * user's password. SimpMusic asks Last.fm for a request token, sends the user to Last.fm's own page
 * in their browser, and trades the approved token for a session key when they come back.
 *
 * @param token supplied when the user returns through the `wordbyword://lastfm-auth` callback; the
 * login then finishes on its own and the screen closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastfmLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val state by viewModel.lastfmState.collectAsStateWithLifecycle()
    val loggedIn by viewModel.lastfmLoggedIn.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var callbackInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetLastfmState()
            showBottomNavigation()
        }
    }

    // Closes on a stored session key rather than on this screen's own state, because the callback
    // is handled by SharedViewModel — the browser redirect never reaches this screen.
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            navController.navigateUp()
        }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            is LastfmLoginState.AwaitingApproval -> uriHandler.openUri(current.authorizeUrl)
            is LastfmLoginState.LoggedIn -> viewModel.makeToast(getString(Res.string.login_success))
            is LastfmLoginState.Failed -> viewModel.makeToast(getString(Res.string.lastfm_login_failed))
            else -> Unit
        }
    }

    val busy = state is LastfmLoginState.CompletingLogin

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding() + 96.dp))

            Column(
                modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.scrobbling_info),
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
                        text = stringResource(Res.string.lastfm_login_step_1),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startLastfmLogin() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                    ) {
                        Text(
                            text = stringResource(Res.string.lastfm_open_authorize_page),
                            // typo() bakes a colour into the style, so a Text inside a filled
                            // button keeps the body colour and disappears into the container
                            // unless the colour is set here.
                            style = typo().labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    // Everything below only matters once the browser is open.
                    if (state is LastfmLoginState.AwaitingApproval) {
                        Spacer(Modifier.height(28.dp))
                        NumberedStep(
                            number = "2",
                            text = stringResource(Res.string.lastfm_login_step_2),
                        )

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        // Last resort when the redirect never reaches the app at all: no handler
                        // registered for the scheme (common on Linux), or a browser that will not
                        // hand off to a local app. The address bar still shows the callback, so let
                        // the user bring it over by hand.
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
                            onClick = { viewModel.completeLastfmLoginFromCallback(callbackInput) },
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

        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            title = {
                Text(
                    text = stringResource(Res.string.log_in_to_lastfm),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        SimpIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
        )
    }
}

/** A step marker plus its instruction, so the two actions read as an ordered pair. */
@Composable
private fun NumberedStep(
    number: String,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = text,
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
