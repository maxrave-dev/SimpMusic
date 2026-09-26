package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.modified_build_message
import simpmusic.composeapp.generated.resources.modified_build_title

/** Replaces the whole app on a repackaged copy, inside App's own theme. Nothing here leads back into it. */
@Composable
fun UnofficialBuildScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.modified_build_title),
            style = typo().titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.modified_build_message),
            style = typo().bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Button(
            onClick = { openUrl("https://simpmusic.org/download") },
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            // typo() bakes a body colour into its styles; the label must take the button's own.
            Text(
                text = stringResource(Res.string.download),
                style = typo().labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
