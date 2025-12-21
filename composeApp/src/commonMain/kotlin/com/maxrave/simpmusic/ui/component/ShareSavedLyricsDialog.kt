package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.contributor_email
import simpmusic.composeapp.generated.resources.contributor_name
import simpmusic.composeapp.generated.resources.help_build_lyrics_database
import simpmusic.composeapp.generated.resources.help_build_lyrics_database_description
import simpmusic.composeapp.generated.resources.later
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.use_anonymous

@Composable
@ExperimentalMaterial3Api
fun ShareSavedLyricsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (
        contributor: Pair<String, String>?,
    ) -> Unit, // contributor name and email, null if anonymous
) {
    var useAnonymous by remember {
        mutableStateOf(true)
    }
    var contributorName by remember {
        mutableStateOf("")
    }
    var contributorEmail by remember {
        mutableStateOf("")
    }

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
                onDismissRequest.invoke()
                onConfirm(
                    if (useAnonymous) {
                        null
                    } else {
                        Pair(contributorName, contributorEmail)
                    },
                )
            }) {
                Text(
                    stringResource(Res.string.ok),
                    style = typo().bodySmall,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest.invoke()
            }) {
                Text(
                    stringResource(Res.string.later),
                    style = typo().bodySmall,
                )
            }
        },
        title = {
            Text(
                stringResource(Res.string.help_build_lyrics_database),
                style = typo().labelSmall,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(Res.string.help_build_lyrics_database_description),
                    style = typo().bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(
                    modifier =
                        Modifier.padding(
                            vertical = 12.dp,
                        ),
                    visible = !useAnonymous,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally(),
                ) {
                    Column {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            OutlinedTextField(
                                value = contributorName,
                                textStyle = typo().bodySmall,
                                onValueChange = { contributorName = it },
                                label = {
                                    Text(
                                        stringResource(Res.string.contributor_name),
                                        style =
                                            typo().labelSmall.copy(
                                                fontSize = 8.sp,
                                            ),
                                    )
                                },
                                placeholder = { Text(stringResource(Res.string.contributor_name), style = typo().bodySmall) },
                                singleLine = true,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contributorEmail,
                                textStyle = typo().bodySmall,
                                onValueChange = { contributorEmail = it },
                                label = {
                                    Text(
                                        stringResource(Res.string.contributor_email),
                                        style =
                                            typo().labelSmall.copy(
                                                fontSize = 8.sp,
                                            ),
                                    )
                                },
                                placeholder = { Text(stringResource(Res.string.contributor_email), style = typo().bodySmall) },
                                singleLine = true,
                                isError = contributorEmail.isNotEmpty() && !contributorEmail.contains("@"),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Row(
                    modifier =
                        Modifier.clickable {
                            useAnonymous = !useAnonymous
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        Checkbox(
                            checked = useAnonymous,
                            onCheckedChange = { useAnonymous = it },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.use_anonymous),
                        style = typo().bodySmall,
                    )
                }
            }
        },
    )
}