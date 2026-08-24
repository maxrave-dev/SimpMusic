package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.lt_as_host
import simpmusic.composeapp.generated.resources.lt_auto_approve_joins
import simpmusic.composeapp.generated.resources.lt_auto_approve_joins_desc
import simpmusic.composeapp.generated.resources.lt_blocked
import simpmusic.composeapp.generated.resources.lt_blocked_empty
import simpmusic.composeapp.generated.resources.lt_custom_server
import simpmusic.composeapp.generated.resources.lt_custom_server_desc
import simpmusic.composeapp.generated.resources.lt_default_server_location
import simpmusic.composeapp.generated.resources.lt_default_server_name
import simpmusic.composeapp.generated.resources.lt_save_server
import simpmusic.composeapp.generated.resources.lt_server
import simpmusic.composeapp.generated.resources.lt_unblock

/** Matches ListenTogetherScreen — a phone-width column, centred in a wide window. */
private const val CONTENT_MAX_WIDTH_DP = 560

/**
 * Listen Together settings, following the "Cài đặt" artboard.
 *
 * The blocklist is **client-side and by name**. The protocol has `kick_user` but no ban, and the
 * server hands out a fresh `user_<nanotime>_<rand>` id on every connection, so there is no stable
 * identity to block on — a name is the only thing that persists across reconnects, and a determined
 * person can change it. It is a convenience, not a security control.
 */
@Composable
fun ListenTogetherSettingsScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherSettingsViewModel = koinViewModel(),
) {
    val usingCustom by viewModel.usingCustomServer.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val autoJoins by viewModel.autoApproveJoins.collectAsStateWithLifecycle()
    val blocked by viewModel.blockedNames.collectAsStateWithLifecycle()

    var draftUrl by remember(serverUrl) { mutableStateOf(serverUrl) }

    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    // See ListenTogetherScreen: measure the space actually given, not the window.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val contentWidth = minOf(maxWidth.value, CONTENT_MAX_WIDTH_DP.toFloat()).dp

        // The same ambient ground as the main Listen Together page — theme-primary glow painted
        // inside the backdrop source, so the glass back button has something to refract. See the
        // long note there for why the source is a matchParentSize sibling and the button must
        // stay outside it.
        val bg = MaterialTheme.colorScheme.background
        val glow =
            if (bg.luminance() > 0.5f) {
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
            } else {
                MaterialTheme.colorScheme.primary.rgbFactor(0.3f)
            }
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

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Full-width header — see ListenTogetherScreen; only the content below is capped.
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(48.dp)
                        .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The back button FLOATS over this strip (end of BoxWithConstraints) so it stays
                // put while the page scrolls — in this row it scrolled away with the page. This
                // spacer and its twin below only keep the title centred over the hole it leaves.
                Spacer(Modifier.width(48.dp))
                Text(
                    text = stringResource(Res.string.listen_together),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }

            // Only the content is capped; the header above stays full width.
            Column(
                modifier = Modifier.width(contentWidth).padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle(stringResource(Res.string.lt_server))

                    ServerOption(
                        title = stringResource(Res.string.lt_default_server_name),
                        subtitle = stringResource(Res.string.lt_default_server_location),
                        selected = !usingCustom,
                        onClick = { viewModel.useDefaultServer() },
                    )
                    ServerOption(
                        title = stringResource(Res.string.lt_custom_server),
                        subtitle = stringResource(Res.string.lt_custom_server_desc),
                        selected = usingCustom,
                        onClick = { if (!usingCustom) viewModel.setServerUrl(draftUrl.ifBlank { "wss://" }) },
                    )

                    BasicTextField(
                        value = draftUrl,
                        onValueChange = { draftUrl = it },
                        singleLine = true,
                        textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (draftUrl.isEmpty()) {
                                    Text("wss://…", style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        },
                    )
                    // Committing on focus loss would silently store a half-typed address, so the value
                    // is only written when the user asks for it.
                    if (draftUrl != serverUrl) {
                        SmallAction(text = stringResource(Res.string.lt_save_server)) { viewModel.setServerUrl(draftUrl) }
                    }
                }

                HLine()

                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SectionTitle(stringResource(Res.string.lt_as_host))
                    ToggleRow(
                        title = stringResource(Res.string.lt_auto_approve_joins),
                        subtitle = stringResource(Res.string.lt_auto_approve_joins_desc),
                        checked = autoJoins,
                        onCheckedChange = { viewModel.setAutoApproveJoins(it) },
                    )
                }

                HLine()

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionTitle(stringResource(Res.string.lt_blocked))
                        Text("${blocked.size}", style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (blocked.isEmpty()) {
                        Text(
                            stringResource(Res.string.lt_blocked_empty),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        blocked.forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(
                                                40.dp,
                                            ).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        name.trim().firstOrNull()?.uppercase() ?: "?",
                                        style = typo().titleSmall,
                                        color = MaterialTheme.colorScheme.surface,
                                    )
                                }
                                Text(
                                    name,
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                SmallAction(text = stringResource(Res.string.lt_unblock)) { viewModel.unblock(name) }
                            }
                        }
                    }
                }

                EndOfPage()
            }
        }

        // Floats over the strip the header row reserves — a sibling of both the scroll column and
        // the backdrop source, exactly the arrangement the main Listen Together page uses.
        LiquidGlassIconButton(
            backdrop = backdrop,
            imageVector = SimpIcons.ArrowBackIosNew,
            tint = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 12.dp, top = 8.dp)
                    .size(48.dp),
        ) {
            navController.navigateUp()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = typo().titleSmall, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun ServerOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)) else Modifier)
                .border(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.40f,
                        )
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                    },
                    RoundedCornerShape(16.dp),
                ).clickable { onClick() }
                .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) {
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f), CircleShape)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(SimpIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = typo().bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LtSwitch(checked = checked)
    }
}

/** The artboard's own switch: a 44×26 track with a 20dp thumb sliding between two insets. */
@Composable
private fun LtSwitch(checked: Boolean) {
    val thumbOffset by animateDpAsState(if (checked) 21.dp else 3.dp, label = "ltSwitchThumb")
    Box(
        modifier =
            Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f)),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 3.dp)
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    // onPrimary over the filled track, surface over the grey one — a fixed white
                    // thumb disappears into the light theme's track.
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
        )
    }
}

@Composable
private fun SmallAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(15.dp))
                .clickable { onClick() }
                .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)))
}