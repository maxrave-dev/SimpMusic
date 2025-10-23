package com.maxrave.simpmusic.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.*

@Composable
@ExperimentalMaterial3Api
fun ReviewDialog(
    onDismissRequest: () -> Unit,
    onDoneReview: () -> Unit,
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
                onDoneReview.invoke()
                uriHandler.openUri("https://github.com/maxrave-dev/SimpMusic")
            }) {
                Text(
                    stringResource(Res.string.give_a_star),
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
                stringResource(Res.string.enjoying_simpmusic),
                style = typo().labelSmall,
            )
        },
        text = {
            Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.if_you_enjoy_using_simpmusic_star_simpmusic_on_github_or_leave_a_review_on))
                    withLink(
                        LinkAnnotation.Url(
                            "https://www.producthunt.com/products/simpmusic",
                            TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = seed)),
                        ) {
                            onDoneReview.invoke()
                            onDismissRequest.invoke()
                            uriHandler.openUri("https://www.producthunt.com/products/simpmusic")
                        },
                    ) {
                        append(" ProductHunt")
                    }
                    append("\n")
                    append(stringResource(Res.string.if_you_love_my_work_consider))
                    withLink(
                        LinkAnnotation.Url(
                            "https://buymeacoffee.com/maxrave",
                            TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = seed)),
                        ) {
                            onDoneReview.invoke()
                            onDismissRequest.invoke()
                            uriHandler.openUri("https://buymeacoffee.com/maxrave")
                        },
                    ) {
                        append(stringResource(Res.string.buying_me_a_coffee))
                    }
                },
                textAlign = TextAlign.Center,
                style = typo().bodySmall,
            )
        },
    )
}