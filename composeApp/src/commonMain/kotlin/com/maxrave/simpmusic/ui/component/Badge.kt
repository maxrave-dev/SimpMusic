package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.ai

@Composable
fun ExplicitBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Explicit,
            "Explicit",
            tint = Color.LightGray,
        )
    }
}

@Composable
fun AIBadge() {
    Box(
        modifier = Modifier.height(24.dp)
            .padding(3.dp)
            .wrapContentWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(Res.string.ai),
            color = { Color.Black },
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp),
            style = typo().labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewBadge() {
    Column {
        ExplicitBadge()
        AIBadge()
    }
}