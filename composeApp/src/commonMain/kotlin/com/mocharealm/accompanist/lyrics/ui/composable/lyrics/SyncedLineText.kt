package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

@Composable
fun SyncedLineText(
    line: SyncedLine,
    isLineRtl: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = if (isLineRtl) Alignment.End else Alignment.Start
    ) {
        Text(
            text = line.content,
            style = textStyle,
            color = textColor,
            textAlign = if (isLineRtl) TextAlign.End else TextAlign.Start
        )
        if (showTranslation) {
            line.translation?.let {
                if (it.trim().isNotEmpty() && !it.trim().equals(line.content.trim(), ignoreCase = true)) {
                    Text(
                        text = it,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = if (isLineRtl) TextAlign.End else TextAlign.Start
                    )
                }
            }
        }
    }
}

