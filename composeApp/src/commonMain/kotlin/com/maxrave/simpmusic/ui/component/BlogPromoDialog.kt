package com.maxrave.simpmusic.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.blog_promo_message
import simpmusic.composeapp.generated.resources.blog_promo_title
import simpmusic.composeapp.generated.resources.later
import simpmusic.composeapp.generated.resources.mono
import simpmusic.composeapp.generated.resources.visit_blog

@Composable
@ExperimentalMaterial3Api
fun BlogPromoDialog(
    onDismissRequest: () -> Unit,
    onVisitBlog: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        confirmButton = {
            TextButton(onClick = {
                onVisitBlog.invoke()
                uriHandler.openUri("https://maxrave.dev")
            }) {
                Text(
                    stringResource(Res.string.visit_blog),
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
        icon = {
            Icon(painterResource(Res.drawable.mono), "App Icon")
        },
        title = {
            Text(
                stringResource(Res.string.blog_promo_title),
                style = typo().labelSmall,
            )
        },
        text = {
            Text(
                stringResource(Res.string.blog_promo_message),
                textAlign = TextAlign.Center,
                style = typo().bodySmall,
            )
        },
    )
}
