package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.typo

@Composable
@ExperimentalMaterial3Api
fun ShareSavedLyricsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm.invoke()
            }) {
                Text(
                    stringResource(R.string.ok),
                    style = typo.bodySmall,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest.invoke()
            }) {
                Text(
                    stringResource(id = R.string.later),
                    style = typo.bodySmall,
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.help_build_lyrics_database),
                style = typo.labelSmall,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.help_build_lyrics_database_description),
                    style = typo.bodySmall,
                )
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.lyrics_database_description))
                        append(" ")
                        withLink(
                            LinkAnnotation.Url(
                                "https://github.com/maxrave-dev/lyrics",
                                TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary)),
                            ),
                        ) {
                            append("https://github.com/maxrave-dev/lyrics")
                        }
                    },
                    style = typo.bodySmall,
                )
            }
        },
    )
}