package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.librarysync.LibrarySyncPairingState
import com.maxrave.domain.data.model.librarysync.LibrarySyncProblem
import com.maxrave.domain.data.model.librarysync.SyncPeer
import com.maxrave.domain.repository.LibrarySyncHostRepository
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibrarySyncHostViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.library_sync_desktop_footer
import simpmusic.composeapp.generated.resources.library_sync_desktop_instructions
import simpmusic.composeapp.generated.resources.library_sync_failed
import simpmusic.composeapp.generated.resources.library_sync_first_sync_desktop
import simpmusic.composeapp.generated.resources.library_sync_ignore
import simpmusic.composeapp.generated.resources.library_sync_logins_message
import simpmusic.composeapp.generated.resources.library_sync_logins_title
import simpmusic.composeapp.generated.resources.library_sync_open_network_settings
import simpmusic.composeapp.generated.resources.library_sync_pair_another
import simpmusic.composeapp.generated.resources.library_sync_pair_phone
import simpmusic.composeapp.generated.resources.library_sync_paired_with
import simpmusic.composeapp.generated.resources.library_sync_peer_not_synced
import simpmusic.composeapp.generated.resources.library_sync_peer_synced
import simpmusic.composeapp.generated.resources.library_sync_port_in_use
import simpmusic.composeapp.generated.resources.library_sync_remove
import simpmusic.composeapp.generated.resources.library_sync_remove_message
import simpmusic.composeapp.generated.resources.library_sync_remove_title
import simpmusic.composeapp.generated.resources.library_sync_settings_differ_desktop
import simpmusic.composeapp.generated.resources.library_sync_title
import simpmusic.composeapp.generated.resources.library_sync_use
import simpmusic.composeapp.generated.resources.library_sync_windows_public
import simpmusic.composeapp.generated.resources.login_sync_new_code
import simpmusic.composeapp.generated.resources.login_sync_no_network
import simpmusic.composeapp.generated.resources.login_sync_pick_address
import simpmusic.composeapp.generated.resources.ok
import kotlin.system.exitProcess

@Composable
actual fun LibrarySyncDialog(onDismiss: () -> Unit) {
    val viewModel: LibrarySyncHostViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pairing by viewModel.pairing.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSettings.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<SyncPeer?>(null) }

    LaunchedEffect(viewModel) { viewModel.open() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }

    val showingQr = pairing !is LibrarySyncPairingState.Idle && pairing !is LibrarySyncPairingState.Paired
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.library_sync_title), style = typo().titleSmall) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val p = pairing) {
                    LibrarySyncPairingState.Starting -> {
                        CircularProgressIndicator()
                    }

                    is LibrarySyncPairingState.Waiting -> {
                        QrCode(p.invite, Modifier.size(240.dp))
                        Text(text = stringResource(Res.string.library_sync_desktop_instructions), style = typo().bodyMedium, textAlign = TextAlign.Center)
                        Text(text = stringResource(Res.string.login_sync_pick_address), style = typo().bodySmall, textAlign = TextAlign.Center)
                        Column(Modifier.fillMaxWidth()) {
                            p.addresses.forEach { address ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = address == p.selected,
                                                role = Role.RadioButton,
                                                onClick = { viewModel.select(address) },
                                            ).padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = address == p.selected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = address.ip, style = typo().bodyMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = address.interfaceName, style = typo().bodySmall, maxLines = 1)
                                }
                            }
                        }
                        WindowsPublicBox(state.problem)
                    }

                    LibrarySyncPairingState.NoNetwork -> {
                        Text(text = stringResource(Res.string.login_sync_no_network))
                    }

                    is LibrarySyncPairingState.Failed -> {
                        SyncProblemBox(
                            if (p.problem == LibrarySyncProblem.PORT_IN_USE) {
                                stringResource(Res.string.library_sync_port_in_use)
                            } else {
                                stringResource(Res.string.library_sync_failed, p.detail.orEmpty())
                            },
                        )
                    }

                    is LibrarySyncPairingState.Paired -> {
                        Text(text = stringResource(Res.string.library_sync_paired_with, p.peer.name), style = typo().bodyMedium)
                        Text(text = stringResource(Res.string.library_sync_first_sync_desktop), style = typo().bodySmall)
                    }

                    LibrarySyncPairingState.Idle -> {
                        state.peers.forEach { peer ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(text = peer.name, style = typo().bodyMedium)
                                    Text(
                                        text =
                                            peer.lastSyncAt?.let { stringResource(Res.string.library_sync_peer_synced, peer.os, syncedAgo(it)) }
                                                ?: stringResource(Res.string.library_sync_peer_not_synced, peer.os),
                                        style = typo().bodySmall,
                                    )
                                }
                                TextButton(onClick = { removing = peer }) {
                                    Text(text = stringResource(Res.string.library_sync_remove), color = LocalContentColor.current)
                                }
                            }
                        }
                        pending?.let {
                            PendingSettingsCard(
                                pending = it,
                                differ = stringResource(Res.string.library_sync_settings_differ_desktop),
                                onNotNow = viewModel::dismissSettings,
                                onApply = { viewModel.applySettings(::restartAfterSyncedSettings) },
                            )
                        }
                        WindowsPublicBox(state.problem)
                        if (state.problem == LibrarySyncProblem.PORT_IN_USE) SyncProblemBox(stringResource(Res.string.library_sync_port_in_use))
                        Text(text = stringResource(Res.string.library_sync_desktop_footer), style = typo().bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            when {
                pairing is LibrarySyncPairingState.Paired -> {
                    TextButton(onClick = viewModel::stopPairing) { Text(text = stringResource(Res.string.ok), color = LocalContentColor.current) }
                }

                // Cancel from the QR goes back to the list when a phone is paired, else closes.
                showingQr -> {
                    TextButton(onClick = { if (state.isPaired) viewModel.stopPairing() else onDismiss() }) {
                        Text(text = stringResource(Res.string.cancel), color = LocalContentColor.current)
                    }
                }

                else -> {
                    TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.ok), color = LocalContentColor.current) }
                }
            }
        },
        dismissButton = {
            when {
                pairing is LibrarySyncPairingState.NoNetwork || pairing is LibrarySyncPairingState.Failed -> {
                    TextButton(onClick = viewModel::startPairing) {
                        Text(text = stringResource(Res.string.login_sync_new_code), color = LocalContentColor.current)
                    }
                }

                pairing is LibrarySyncPairingState.Idle -> {
                    TextButton(onClick = viewModel::startPairing) {
                        Text(
                            text = stringResource(if (state.isPaired) Res.string.library_sync_pair_another else Res.string.library_sync_pair_phone),
                            color = LocalContentColor.current,
                        )
                    }
                }

                else -> {}
            }
        },
    )

    removing?.let { peer ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(text = stringResource(Res.string.library_sync_remove_title, peer.name), style = typo().titleSmall) },
            text = { Text(text = stringResource(Res.string.library_sync_remove_message, peer.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePhone(peer.id)
                    removing = null
                }) { Text(text = stringResource(Res.string.library_sync_remove), color = LocalContentColor.current) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(text = stringResource(Res.string.cancel), color = LocalContentColor.current) }
            },
        )
    }
}

@Composable
private fun WindowsPublicBox(problem: LibrarySyncProblem?) {
    if (problem != LibrarySyncProblem.WINDOWS_PUBLIC_NETWORK) return
    SyncProblemBox(
        text = stringResource(Res.string.library_sync_windows_public),
        action = stringResource(Res.string.library_sync_open_network_settings),
        onAction = { openUrl("ms-settings:network-status") },
    )
}

/**
 * Sign-ins a paired phone sent: nothing is used until the user here says so (D4). Lives over the
 * whole window, since the phone sends them whenever its user asks.
 */
@Composable
fun LibrarySyncLoginPrompt() {
    val repository: LibrarySyncHostRepository = koinInject()
    val pending by repository.pendingLogins.collectAsStateWithLifecycle()
    val logins = pending ?: return
    // Outside App(), so it brings a theme of its own: the app's default dark one.
    AppTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(Res.string.library_sync_logins_title, logins.fromName), style = typo().titleSmall) },
            text = { Text(text = stringResource(Res.string.library_sync_logins_message, logins.services.joinToString { it.label })) },
            confirmButton = {
                TextButton(onClick = { repository.answerLogins(true) }) {
                    Text(text = stringResource(Res.string.library_sync_use), color = LocalContentColor.current)
                }
            },
            dismissButton = {
                TextButton(onClick = { repository.answerLogins(false) }) {
                    Text(text = stringResource(Res.string.library_sync_ignore), color = LocalContentColor.current)
                }
            },
        )
    }
}

// The same exit a restored backup takes; Desktop has no relaunch of its own.
actual fun restartAfterSyncedSettings() {
    exitProcess(0)
}
