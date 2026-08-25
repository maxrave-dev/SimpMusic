package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.DownloadForOfflineOutlined
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AutoEqStatus
import com.maxrave.simpmusic.viewModel.AutoEqViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_autoeq_choose
import simpmusic.composeapp.generated.resources.equalizer_autoeq_downloading
import simpmusic.composeapp.generated.resources.equalizer_autoeq_empty
import simpmusic.composeapp.generated.resources.equalizer_autoeq_failed
import simpmusic.composeapp.generated.resources.equalizer_autoeq_search
import simpmusic.composeapp.generated.resources.equalizer_autoeq_unavailable

private val MENU_LIST_MAX_HEIGHT = 280.dp

/**
 * Pick the headphone whose AutoEq correction should become the curve.
 *
 * Built as a dropdown rather than a dialog so it behaves exactly like the presets control beside
 * it: the two rows answer two different questions — what the music should sound like, and what
 * this pair of headphones gets wrong — and nothing about them should feel like different kinds of
 * control. There are several thousand headphones, so this one carries a filter.
 */
@Composable
fun AutoEqPicker(
    label: String?,
    modifier: Modifier = Modifier,
    viewModel: AutoEqViewModel = koinViewModel(),
) {
    var open by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val cachedPaths by viewModel.cachedPaths.collectAsStateWithLifecycle()

    // The index is only wanted once the menu is actually opened, which is why nothing here runs on
    // first composition: the equalizer card is in the settings list whether or not anyone scrolls
    // to it, and it must not spend a request on being scrolled past.
    LaunchedEffect(open) {
        if (open) viewModel.onOpen()
    }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { open = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label ?: stringResource(Res.string.equalizer_autoeq_choose),
                style = typo().bodyMedium,
                color =
                    if (label != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Icon(
                imageVector = SimpIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(modifier = Modifier.width(320.dp).padding(horizontal = 12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        failed = false
                        viewModel.onQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = typo().bodyMedium,
                    label = { Text(text = stringResource(Res.string.equalizer_autoeq_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (status == AutoEqStatus.DOWNLOADING || status == AutoEqStatus.APPLYING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                if (failed) {
                    Message(
                        text = stringResource(Res.string.equalizer_autoeq_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                when {
                    status == AutoEqStatus.DOWNLOADING ->
                        Message(stringResource(Res.string.equalizer_autoeq_downloading))

                    status == AutoEqStatus.UNAVAILABLE ->
                        Message(stringResource(Res.string.equalizer_autoeq_unavailable))

                    results.isEmpty() -> Message(stringResource(Res.string.equalizer_autoeq_empty))

                    else ->
                        // A plain scrolling column, deliberately not a lazy list. A menu measures
                        // its content with `width(IntrinsicSize.Max)`, and anything built on
                        // SubcomposeLayout — every lazy list — throws rather than answer an
                        // intrinsic query. The result count is capped by the repository, so there
                        // is a known ceiling on how many rows this composes at once.
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = MENU_LIST_MAX_HEIGHT)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            results.forEach { entry ->
                                val downloaded = entry.path in cachedPaths
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                // Keyed to the row, because the list is rebuilt on
                                                // every keystroke and an unkeyed remember here
                                                // would hand a row the previous one's slot.
                                                interactionSource = remember(entry.path) { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                failed = false
                                                viewModel.apply(entry) { ok ->
                                                    if (ok) open = false else failed = true
                                                }
                                            }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = entry.name, style = typo().bodyMedium)
                                        Text(
                                            // The same headphone is measured by several sources,
                                            // and sometimes on more than one rig, so the row says
                                            // which.
                                            text = listOfNotNull(entry.source, entry.rig).joinToString(" · "),
                                            style = typo().bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    // Filled means the curve is already on disk and this row works
                                    // with no connection; outlined means picking it has to fetch.
                                    Icon(
                                        imageVector =
                                            if (downloaded) {
                                                SimpIcons.DownloadForOffline
                                            } else {
                                                SimpIcons.DownloadForOfflineOutlined
                                            },
                                        contentDescription = null,
                                        tint =
                                            if (downloaded) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            },
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun Message(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = typo().bodySmall,
        color = color,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}
