package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.maxrave.domain.data.model.librarysync.PendingSettings
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.day_s_ago
import simpmusic.composeapp.generated.resources.hour_s_ago
import simpmusic.composeapp.generated.resources.library_sync_apply_question
import simpmusic.composeapp.generated.resources.library_sync_apply_restart
import simpmusic.composeapp.generated.resources.library_sync_just_now
import simpmusic.composeapp.generated.resources.library_sync_minutes_ago
import simpmusic.composeapp.generated.resources.library_sync_not_now
import simpmusic.composeapp.generated.resources.library_sync_settings_from
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Desktop: the pairing QR and the paired phones. Android: scan, pair, then the pairing's status (#2663). */
@Composable
expect fun LibrarySyncDialog(onDismiss: () -> Unit)

/** Restarts the app after synced settings were written, the same way a restored backup does. */
expect fun restartAfterSyncedSettings()

/** "3 min ago" for an epoch-millisecond time. */
@OptIn(ExperimentalTime::class)
@Composable
internal fun syncedAgo(millis: Long): String {
    val minutes = (Clock.System.now().toEpochMilliseconds() - millis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> stringResource(Res.string.library_sync_just_now)
        minutes < 60 -> stringResource(Res.string.library_sync_minutes_ago, minutes.toInt())
        minutes < 24 * 60 -> stringResource(Res.string.hour_s_ago, (minutes / 60).toInt())
        else -> stringResource(Res.string.day_s_ago, (minutes / (24 * 60)).toInt())
    }
}

// ponytail: names come from the DataStore key ("lyrics_provider" → "Lyrics provider"), so they are
// English on every locale; give the 29 allowlisted keys string resources if translators ask.
internal fun settingLabel(key: String): String = key.replace('_', ' ').replaceFirstChar { it.uppercase() }

/** A problem worded for the user, with an optional fix next to it. */
@Composable
internal fun SyncProblemBox(
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
    ) {
        Text(text = text, style = typo().bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        if (action != null) {
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(text = action, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

/** The other device's settings, named one per line, with Not now / Apply & restart. */
@Composable
internal fun PendingSettingsCard(
    pending: PendingSettings,
    differ: String,
    onNotNow: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(Res.string.library_sync_settings_from, pending.fromName), style = typo().titleSmall)
        Text(text = differ, style = typo().bodySmall)
        pending.keys.forEach { Text(text = "• ${settingLabel(it)}", style = typo().bodyMedium) }
        Text(text = stringResource(Res.string.library_sync_apply_question), style = typo().bodySmall)
        Row(modifier = Modifier.align(Alignment.End)) {
            TextButton(onClick = onNotNow) { Text(text = stringResource(Res.string.library_sync_not_now), color = LocalContentColor.current) }
            TextButton(onClick = onApply) { Text(text = stringResource(Res.string.library_sync_apply_restart), color = LocalContentColor.current) }
        }
    }
}
