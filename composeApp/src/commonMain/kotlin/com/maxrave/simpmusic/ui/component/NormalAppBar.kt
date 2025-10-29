package com.maxrave.simpmusic.ui.component


import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_more_vert_24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalAppBar(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    leftIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (RowScope.() -> Unit)? = null,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = leftIcon ?: {},
        actions = rightIcon ?: {},
    )
}

@Preview
@Composable
fun NormalAppBarPreview() {
    NormalAppBar(
        title = {
            Text(text = "Title")
        },
        leftIcon = {
            IconButton(onClick = { }) {
                Icon(
                    painterResource(Res.drawable.baseline_arrow_back_ios_new_24),
                    contentDescription = "Back",
                )
            }
        },
        rightIcon = {
            IconButton(onClick = { }) {
                Icon(
                    painterResource(Res.drawable.baseline_more_vert_24),
                    contentDescription = "Back",
                )
            }
        },
    )
}