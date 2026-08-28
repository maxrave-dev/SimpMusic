package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.data.model.listentogether.RoomJoinRequest
import com.maxrave.domain.data.model.listentogether.RoomMember
import com.maxrave.domain.data.model.listentogether.RoomSuggestion
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Logout
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherSettingsDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.lt_background_warning
import simpmusic.composeapp.generated.resources.lt_block
import simpmusic.composeapp.generated.resources.lt_block_desc
import simpmusic.composeapp.generated.resources.lt_cancel_join
import simpmusic.composeapp.generated.resources.lt_connect
import simpmusic.composeapp.generated.resources.lt_connected
import simpmusic.composeapp.generated.resources.lt_connecting
import simpmusic.composeapp.generated.resources.lt_create_room
import simpmusic.composeapp.generated.resources.lt_credit_compatible
import simpmusic.composeapp.generated.resources.lt_credit_protocol
import simpmusic.composeapp.generated.resources.lt_disconnect
import simpmusic.composeapp.generated.resources.lt_display_name
import simpmusic.composeapp.generated.resources.lt_display_name_hint
import simpmusic.composeapp.generated.resources.lt_host_badge
import simpmusic.composeapp.generated.resources.lt_in_room
import simpmusic.composeapp.generated.resources.lt_join_requests
import simpmusic.composeapp.generated.resources.lt_join_room
import simpmusic.composeapp.generated.resources.lt_just_asked
import simpmusic.composeapp.generated.resources.lt_kick
import simpmusic.composeapp.generated.resources.lt_kick_desc
import simpmusic.composeapp.generated.resources.lt_leave_room
import simpmusic.composeapp.generated.resources.lt_not_connected
import simpmusic.composeapp.generated.resources.lt_or_join_with_code
import simpmusic.composeapp.generated.resources.lt_room_code
import simpmusic.composeapp.generated.resources.lt_suggestions
import simpmusic.composeapp.generated.resources.lt_tagline
import simpmusic.composeapp.generated.resources.lt_transfer_host
import simpmusic.composeapp.generated.resources.lt_transfer_host_desc
import simpmusic.composeapp.generated.resources.lt_waiting_approval
import simpmusic.composeapp.generated.resources.lt_waiting_approval_desc
import simpmusic.composeapp.generated.resources.lt_you
import simpmusic.composeapp.generated.resources.settings

/**
 * Listen Together.
 *
 * Colour, type and surface all come from the app's own system — `MaterialTheme.colorScheme`,
 * `typo()`, the colour scheme, and the liquid-glass treatment the Analytics screen uses — rather than a
 * palette of hard-coded hexes. The first version of this screen invented its own #0F1319 dark and
 * its own accent, which is precisely why it read as pasted in from somewhere else.
 */
private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val ROW_SHAPE = RoundedCornerShape(18.dp)

/**
 * Below this the screen is one column. Two columns need room for two REAL columns — splitting a
 * 700dp window just makes two cramped ones.
 */
private const val TWO_COLUMN_MIN_DP = 900

/** How long the copy button shows a tick before going back to the copy glyph. */
private const val COPIED_FEEDBACK_MS = 1800L
private const val ROOM_CODE_LENGTH = ListenTogetherViewModel.ROOM_CODE_LENGTH

/** Avatar tints derived from the theme, so they move with it instead of fighting it. */
@Composable
private fun tintFor(id: String): Color {
    val palette =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        )
    val index = id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) } % palette.size
    return palette[index]
}

private fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"

@Composable
fun ListenTogetherScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val codeInput by viewModel.roomCodeInput.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var managing by remember { mutableStateOf<RoomMember?>(null) }

    // The back button refracts whatever the page has drawn, so the CONTENT is the backdrop source
    // and the button is its SIBLING — the same arrangement Album/Playlist/Analytics use. Nesting the
    // button inside the source is the render-feedback loop that kills the RuntimeShader.
    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        // Measured from BoxWithConstraints, not the window: this is the space left after the
        // navigation rail. Two columns only once there is room for two REAL columns — a 560dp
        // ribbon centred in a 2000dp window is what made this read as a form rather than a page.
        val wide = maxWidth >= TWO_COLUMN_MIN_DP.dp

        val copyCode: () -> Unit = { state.roomCode?.let { clipboard.setText(AnnotatedString(it)) } }
        val shareTitle = stringResource(Res.string.listen_together)
        val shareCode: () -> Unit = {
            state.roomCode?.let { shareUrl(title = shareTitle, url = SHARE_PREFIX + it) }
        }
        val openSettings: () -> Unit = { navController.navigate(ListenTogetherSettingsDestination) }

        // Home's ambient top glow, tinted from the THEME rather than artwork — this page has none.
        // Same recipe as HomeScreen's first item: an angled two-stop gradient with a scrim easing
        // its tail into the background so there is no seam. Static, since the theme colour does not
        // move. Drawn first, so both layout branches scroll over it; deliberately NOT inside the
        // glass backdrop source — the back button is a sibling of that source and nesting order is
        // what keeps the RuntimeShader from feeding back into itself.
        val bg = MaterialTheme.colorScheme.background
        val glow =
            if (bg.luminance() > 0.5f) {
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
            } else {
                MaterialTheme.colorScheme.primary.rgbFactor(0.3f)
            }
        // The backdrop SOURCE is this ground box — page colour plus the glow — not the content
        // columns: glass shows whatever its source recorded, and with the source on the (mostly
        // transparent) content the back button rendered as a solid black coin over a tinted page.
        // Same arrangement as AlbumScreen's landscape header: a matchParentSize sibling that takes
        // part in no measurement, with the button kept OUTSIDE it — nesting the button inside the
        // source is the render-feedback loop that kills the RuntimeShader.
        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            Box(Modifier.matchParentSize().background(bg))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .angledGradientBackground(listOf(glow, bg), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(bg)),
                )
            }
        }

        if (wide) {
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // The identity side scrolls separately and normally does not move at all: the room
                // code is what people read aloud, so it stays on screen while the list scrolls.
                Column(
                    modifier =
                        Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 28.dp, top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Reserves the strip the floating back button is drawn over — it is a sibling of
                    // this column and takes part in no measurement, so nothing else pushes down for it.
                    Spacer(Modifier.height(BACK_BUTTON_STRIP))
                    TitleBlock(inRoom = state.inRoom)
                    ConnectionLine(state.connection, viewModel::connect, viewModel::disconnect)
                    AnimatedVisibility(visible = state.inRoom) {
                        RoomCodePoster(state, copyCode, shareCode)
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 4.dp, end = 40.dp, top = 30.dp)
                            .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WorkArea(state, displayName, codeInput, viewModel, { managing = it }, openSettings)
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(BACK_BUTTON_STRIP))
                TitleBlock(inRoom = state.inRoom)
                ConnectionLine(state.connection, viewModel::connect, viewModel::disconnect)
                AnimatedVisibility(visible = state.inRoom) {
                    RoomCodePoster(state, copyCode, shareCode)
                }
                WorkArea(state, displayName, codeInput, viewModel, { managing = it }, openSettings)
            }
        }

        BackButton(backdrop) { navController.navigateUp() }
    }

    managing?.let { member ->
        MemberActionDialog(
            member = member,
            onTransferHost = {
                viewModel.transferHost(member.userId)
                managing = null
            },
            onKick = {
                viewModel.kickUser(member.userId)
                managing = null
            },
            onBlock = {
                viewModel.blockAndKick(member.userId, member.username)
                managing = null
            },
            onDismiss = { managing = null },
        )
    }
}

/**
 * Everything that changes as you use the screen: the form before a room, the people inside one.
 *
 * A `ColumnScope` extension so the two-column and one-column skeletons share it verbatim instead
 * of drifting apart — the bug where a fix lands on one layout only.
 */
@Composable
private fun ColumnScope.WorkArea(
    state: ListenTogetherRoom,
    displayName: String,
    codeInput: String,
    viewModel: ListenTogetherViewModel,
    onManage: (RoomMember) -> Unit,
    onSettings: () -> Unit,
) {
    // Held so the card keeps its text while it animates out — `state.error` is already null by then
    // and the card would collapse through a frame of empty space.
    var lastError by remember { mutableStateOf("") }
    LaunchedEffect(state.error) { state.error?.let { lastError = it } }
    AnimatedVisibility(visible = state.error != null) {
        ErrorCard(message = lastError, onDismiss = viewModel::clearError)
    }

    when {
        state.inRoom -> {
            AnimatedVisibility(visible = state.waitingFor.isNotEmpty()) {
                BufferBanner(state.waitingForNames)
            }
            AnimatedVisibility(visible = state.isHost && state.joinRequests.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    JoinRequests(state.joinRequests, viewModel::approveJoin, viewModel::rejectJoin)
                }
            }
            AnimatedVisibility(visible = state.isHost && state.suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Suggestions(state.suggestions, viewModel::approveSuggestion, viewModel::rejectSuggestion)
                }
            }
            Members(
                members = state.members,
                selfId = state.selfUserId,
                canManage = state.isHost,
                onManage = onManage,
            )
            FooterActions(onLeave = viewModel::leaveRoom, onSettings = onSettings)
        }

        state.pendingJoinCode != null -> {
            WaitingForApproval(state.pendingJoinCode.orEmpty(), viewModel::cancelJoin)
            FooterActions(onLeave = null, onSettings = onSettings)
        }

        else -> {
            NameField(displayName, viewModel::onDisplayNameChange)

            PrimaryButton(
                text = stringResource(Res.string.lt_create_room),
                enabled = displayName.isNotBlank() && state.isConnected,
                onClick = viewModel::createRoom,
            )

            DividerLabel(stringResource(Res.string.lt_or_join_with_code))

            CodeInput(codeInput, viewModel::onRoomCodeChange)

            SecondaryButton(
                text = stringResource(Res.string.lt_join_room),
                enabled = displayName.isNotBlank() && codeInput.length == ROOM_CODE_LENGTH && state.isConnected,
                onClick = viewModel::joinRoom,
            )

            Text(
                text = stringResource(Res.string.lt_background_warning),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )

            FooterActions(onLeave = null, onSettings = onSettings)
        }
    }

    CreditFooter()
    EndOfPage(withoutCredit = true)
}

/**
 * Where the protocol came from.
 *
 * Left-aligned with everything else: EndOfPage centres its own credit, which reads as misaligned
 * the moment the page stops being a single centred column.
 */
@Composable
private fun CreditFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.lt_credit_protocol),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.lt_credit_compatible),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Prefix for the share button; the code alone means nothing to the recipient. */
private const val SHARE_PREFIX = "Join my SimpMusic room with code "

// ───────────────────────────────── structure ─────────────────────────────────

/** Status-bar inset + the 8.dp above the button + its own 48.dp. */
private val BACK_BUTTON_STRIP = 56.dp

@Composable
private fun BoxScope.BackButton(
    backdrop: PlatformBackdrop,
    onBack: () -> Unit,
) {
    // Glass, like every other back button in the app. It only works as an OVERLAY: in the scrolling
    // column it used to sit in, what is behind it is the flat page background, and glass with
    // nothing to refract renders as a grey coin. Floating it over the content gives it the page to
    // refract — and keeps it reachable once the page has scrolled, which the in-flow one did not.
    LiquidGlassIconButton(
        backdrop = backdrop,
        imageVector = SimpIcons.ArrowBackIosNew,
        // NOT the default Color.White: every other caller sits on a ForceDark screen, but this page
        // follows the theme — at light theme a white glyph sits on light glass (or on the light
        // fallback pill) and disappears. onSurface flips with the scheme.
        tint = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        modifier =
            Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 12.dp, top = 8.dp)
                .size(48.dp),
        onClick = onBack,
    )
}

/**
 * The page title, left-aligned and large enough to be the biggest thing on screen after the room
 * code. It was a centred `titleMedium` in a top bar over a decorative circle icon — which is the
 * layout every generated screen has, and it left nothing on the page holding rank.
 */
@Composable
private fun TitleBlock(inRoom: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.listen_together),
            style = typo().titleLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!inRoom) {
            Text(
                text = stringResource(Res.string.lt_tagline),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Connection state as one line of text, not a bordered card with a filled pill in it.
 *
 * It is ambient information — you glance at it and move on — so giving it the same card treatment
 * as the room itself was what made every block on this screen look equally important.
 */
@Composable
private fun ConnectionLine(
    connection: RoomConnection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val connected = connection is RoomConnection.Connected
    val accent =
        when (connection) {
            is RoomConnection.Connected -> MaterialTheme.colorScheme.primary
            is RoomConnection.Connecting -> MaterialTheme.colorScheme.tertiary
            is RoomConnection.Failed -> MaterialTheme.colorScheme.error
            RoomConnection.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val label =
        when (connection) {
            is RoomConnection.Connected -> stringResource(Res.string.lt_connected)
            is RoomConnection.Connecting -> stringResource(Res.string.lt_connecting)
            is RoomConnection.Failed -> connection.reason
            RoomConnection.Disconnected -> stringResource(Res.string.lt_not_connected)
        }

    // Connecting is the one state the user is waiting on, and a static dot is indistinguishable
    // from a stuck one.
    val pulse = rememberInfiniteTransition(label = "ltConnecting")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "ltConnectingAlpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (connection is RoomConnection.Connecting) accent.copy(alpha = dotAlpha) else accent,
                ),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = typo().bodyMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "The Meowery · Poland",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text =
                if (connected) {
                    stringResource(Res.string.lt_disconnect)
                } else {
                    stringResource(Res.string.lt_connect)
                },
            style = typo().labelMedium,
            color = if (connected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable { if (connected) onDisconnect() else onConnect() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * The room code, as a poster.
 *
 * This is the one thing on the screen a person reads out loud to someone sitting next to them, so
 * it is the one thing allowed to be outsized — and it sits directly on the background rather than
 * inside a card, because a card would rank it level with the Settings row.
 */
@Composable
private fun RoomCodePoster(
    state: ListenTogetherRoom,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text =
                if (state.isHost) {
                    stringResource(Res.string.lt_room_code)
                } else {
                    stringResource(Res.string.lt_in_room)
                },
            style = typo().labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text =
                state.roomCode
                    ?.chunked(4)
                    ?.joinToString(" ")
                    .orEmpty(),
            // The app's scale stops at 25sp because nothing else has to carry across a room. Sized
            // so eight monospace characters still fit a 390dp phone once padding is taken out.
            style =
                typo().titleLarge.copy(
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Text(
                text =
                    if (state.isHost) {
                        "You are the host · ${state.members.size} listening"
                    } else {
                        "${state.members.firstOrNull { it.isHost }?.username.orEmpty()} is controlling playback"
                    },
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isHost) {
            var copied by remember { mutableStateOf(false) }
            LaunchedEffect(copied) {
                if (copied) {
                    delay(COPIED_FEEDBACK_MS)
                    copied = false
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                // A silent clipboard write is indistinguishable from a dead button, and this one
                // is the whole point of the screen — the tick is the only proof it did anything.
                Crossfade(targetState = copied, label = "ltCopied") { done ->
                    GlyphButton(if (done) SimpIcons.Check else SimpIcons.ContentCopy) {
                        onCopyCode()
                        copied = true
                    }
                }
                GlyphButton(SimpIcons.Share, onShareCode)
            }
        }
    }
}

/**
 * The buffer barrier, said out loud.
 *
 * Playback genuinely stops until the slowest device is ready; naming who is being waited for is
 * what stops the silence reading as the app having hung.
 */
@Composable
private fun BufferBanner(names: List<String>) {
    Surface(tint = MaterialTheme.colorScheme.tertiary) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = if (names.isEmpty()) "Waiting for everyone" else "Waiting for ${names.joinToString(", ")}",
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                "Playback resumes when everyone is ready",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WaitingForApproval(
    code: String,
    onCancel: () -> Unit,
) {
    Surface(tint = MaterialTheme.colorScheme.tertiary) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(Res.string.lt_waiting_approval),
                style = typo().titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                code.chunked(4).joinToString("  "),
                style = typo().titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(Res.string.lt_waiting_approval_desc),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Chip(stringResource(Res.string.lt_cancel_join), onClick = onCancel)
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(tint = MaterialTheme.colorScheme.error) {
        Row(
            Modifier.fillMaxWidth().clickable { onDismiss() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = typo().bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            Icon(SimpIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun JoinRequests(
    requests: List<RoomJoinRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.lt_join_requests), requests.size)
    requests.forEach { request ->
        Surface {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(initialOf(request.username), tintFor(request.userId), 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(request.username, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    Text(
                        stringResource(Res.string.lt_just_asked),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ActionGlyph(SimpIcons.Check, MaterialTheme.colorScheme.primary) { onApprove(request.userId) }
                ActionGlyph(SimpIcons.Close, MaterialTheme.colorScheme.error) { onReject(request.userId) }
            }
        }
    }
}

@Composable
private fun Suggestions(
    suggestions: List<RoomSuggestion>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.lt_suggestions), suggestions.size)
    suggestions.forEach { suggestion ->
        Surface {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        suggestion.track.title,
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${suggestion.fromUsername} suggested",
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                ActionGlyph(SimpIcons.Check, MaterialTheme.colorScheme.primary) { onApprove(suggestion.suggestionId) }
                ActionGlyph(SimpIcons.Close, MaterialTheme.colorScheme.error) { onReject(suggestion.suggestionId) }
            }
        }
    }
}

@Composable
private fun Members(
    members: List<RoomMember>,
    selfId: String,
    canManage: Boolean,
    onManage: (RoomMember) -> Unit,
) {
    SectionHeader(stringResource(Res.string.lt_in_room), members.size)
    // A plain divided list, not one card per person: a room of five people was five identical
    // rounded rectangles, which reads as five unrelated objects rather than one list.
    Column(Modifier.fillMaxWidth()) {
        members.forEachIndexed { index, member ->
            if (index > 0) {
                Box(
                    Modifier
                        .padding(start = 52.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)),
                )
            }
            val manageable = canManage && member.userId != selfId
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (manageable) Modifier.clickable { onManage(member) } else Modifier)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(initialOf(member.username), tintFor(member.userId), 40.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            member.username,
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (member.isHost) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    stringResource(Res.string.lt_host_badge),
                                    style = typo().labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape).background(
                                when {
                                    member.isBuffering -> MaterialTheme.colorScheme.tertiary
                                    member.isConnected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        )
                        Text(
                            text =
                                when {
                                    member.userId == selfId -> stringResource(Res.string.lt_you)
                                    member.isBuffering -> "loading…"
                                    member.isConnected -> "in sync"
                                    else -> "disconnected"
                                },
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (manageable) {
                    Icon(
                        SimpIcons.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * Leaving and settings, as quiet text actions on one line.
 *
 * They were two full-width cards, which gave "Settings" the same visual weight as the room itself.
 */
@Composable
private fun FooterActions(
    onLeave: (() -> Unit)?,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onLeave != null) {
            TextAction(
                icon = SimpIcons.Logout,
                text = stringResource(Res.string.lt_leave_room),
                color = MaterialTheme.colorScheme.error,
                onClick = onLeave,
            )
        }
        TextAction(
            icon = SimpIcons.Settings,
            text = stringResource(Res.string.settings),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onSettings,
        )
    }
}

@Composable
private fun TextAction(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    // A real Material button, not a Row with clickable on it: that hand-rolled version had no
    // ripple, no pressed state and no guaranteed minimum touch target.
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = color.copy(alpha = 0.12f),
                contentColor = color,
            ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = typo().bodyMedium)
    }
}

@Composable
private fun MemberActionDialog(
    member: RoomMember,
    onTransferHost: () -> Unit,
    onKick: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .width(320.dp)
                    .clip(CARD_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(initialOf(member.username), tintFor(member.userId), 40.dp)
                Text(
                    member.username,
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogAction(
                    stringResource(Res.string.lt_transfer_host),
                    stringResource(Res.string.lt_transfer_host_desc),
                    MaterialTheme.colorScheme.onSurface,
                    onTransferHost,
                )
                DialogAction(
                    stringResource(Res.string.lt_kick),
                    stringResource(Res.string.lt_kick_desc),
                    MaterialTheme.colorScheme.error,
                    onKick,
                )
                DialogAction(
                    stringResource(Res.string.lt_block),
                    stringResource(Res.string.lt_block_desc),
                    MaterialTheme.colorScheme.error,
                    onBlock,
                )
            }
        }
    }
}

// ───────────────────────────────── building blocks ─────────────────────────────────

/** One card. Every panel on this screen is one of these, so they cannot drift apart. */
@Composable
private fun Surface(
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(
                    tint?.copy(alpha = 0.07f) ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                ).border(
                    1.dp,
                    tint?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    CARD_SHAPE,
                ),
    ) { content() }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = typo().titleSmall, color = MaterialTheme.colorScheme.onBackground)
        Box(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        ) {
            Text("$count", style = typo().labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Avatar(
    letter: String,
    background: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = typo().titleSmall, color = MaterialTheme.colorScheme.surface)
    }
}

@Composable
private fun Chip(
    text: String,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(36.dp)
                .clip(CircleShape)
                .then(
                    if (filled) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
                    },
                ).clickable { onClick() }
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().labelMedium,
            color = if (filled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlyphButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionGlyph(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                // primary/onPrimary, never primary/surface: on the light theme `surface` is nearly
                // white and the filled button became white text on pale cyan. onPrimary is generated
                // against primary, so it is the only text colour guaranteed to survive both themes.
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    },
                ).clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().titleSmall,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.35f else 0.15f), CircleShape)
                .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
private fun DangerButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), CircleShape)
                .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = typo().titleSmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DialogAction(
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(ROW_SHAPE)
                .border(1.dp, tint.copy(alpha = 0.25f), ROW_SHAPE)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, style = typo().bodyMedium, color = tint)
        Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DividerLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
        Text(text, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.lt_display_name), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(ROW_SHAPE)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
        Text(stringResource(Res.string.lt_display_name_hint), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // TextFieldValue, not String: a plain String re-places the caret at index 0 on every
    // externally-driven recomposition, and backspace at index 0 deletes nothing — the field takes
    // characters and then refuses to give them back.
    val fieldValue = TextFieldValue(text = code, selection = TextRange(code.length))

    BasicTextField(
        value = fieldValue,
        onValueChange = { onCodeChange(it.text) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            // Zero-sized but composed: without it nothing holds the cursor and the field takes no
            // input at all.
            Box(Modifier.size(0.dp)) { innerTextField() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(ROOM_CODE_LENGTH) { index ->
                    val filled = index < code.length
                    // The caret is drawn by hand because the field that owns it is zero-sized.
                    val isCaret = index == code.length
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                .then(
                                    if (isCaret) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier,
                                ).clickable { focusRequester.requestFocus() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (filled) code[index].toString() else "",
                            style = typo().titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
    )
}