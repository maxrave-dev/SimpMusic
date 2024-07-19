package com.maxrave.simpmusic.ui.component

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.TimeLine
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil


@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
) {
    val TAG = "LyricsView"

    val textMeasurer = rememberTextMeasurer()
    val localDensity = LocalDensity.current

    var columnHeightDp by remember {
        mutableStateOf(0.dp)
    }
    var columnWidthDp by remember {
        mutableStateOf(0.dp)
    }
    var currentLineHeight by remember {
        mutableIntStateOf(0)
    }
    val listState = rememberLazyListState()
    val current by timeLine.collectAsState()
    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    LaunchedEffect(key1 = current) {
        val lines = lyricsData.lyrics.lines
        if (current.current > 0L) {
            lines?.indices?.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                // estimate the end time of the current sentence based on the start time of the next sentence
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                        // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                        startTimeMs + 60000
                    }
                if (current.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            if (!lines.isNullOrEmpty() && (
                    current.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                            ).toLong()
                        )
                    )
            ) {
                currentLineIndex = -1
            }
        }
        else {
            currentLineIndex = -1
        }
    }
    LaunchedEffect(key1 = currentLineIndex, key2 = currentLineHeight) {
        if (currentLineIndex > -1 && currentLineHeight > 0) {
            val boxEnd = listState.layoutInfo.viewportEndOffset
            val boxStart = listState.layoutInfo.viewportStartOffset
            val viewPort = boxEnd - boxStart
            val offset = viewPort/2 - currentLineHeight/2
            Log.w(TAG, "Offset: $offset")
            listState.animateScrollAndCentralizeItem(
                index = currentLineIndex,
                this
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.onGloballyPositioned { coordinates ->
            columnHeightDp = with(localDensity) { coordinates.size.height.toDp() }
            columnWidthDp = with(localDensity) { coordinates.size.width.toDp() }
        }.fillMaxSize()
    ) {
        items(lyricsData.lyrics.lines?.size ?: 0) { index ->
            val line = lyricsData.lyrics.lines?.getOrNull(index)
            val translatedWords = lyricsData.translatedLyrics?.lines?.getOrNull(index)?.words
            line?.words?.let {
                LyricsLineItem(
                    originalWords = it, translatedWords = translatedWords, isBold = index <= currentLineIndex,
                    modifier = Modifier
                        .clickable {
                            onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                        }
                        .onGloballyPositioned { c ->
                            currentLineHeight = c.size.height
                        }
                )
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    modifier: Modifier = Modifier
) {
    Crossfade(targetState = isBold) {
        if (it) {
            Column(
                modifier = modifier
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = originalWords, style = typo.headlineMedium, color = Color.White)
                if (translatedWords != null) {
                    Text(text = translatedWords, style = typo.bodyMedium, color = Color.Yellow)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
    if (!isBold) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = originalWords, style = typo.bodyLarge, color = Color.LightGray)
            if (translatedWords != null) {
                Text(text = translatedWords, style = typo.bodyMedium, color = Color.Yellow)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

fun textHeightCalculate(
    text: String,
    width: Dp,
    style: TextStyle,
    localDensity: Density,
    textMeasurer: TextMeasurer
): Dp {
    val heightDp = with(localDensity) { textMeasurer.measure(text, style).size.height.toDp() }
    val widthDp = with(localDensity) { textMeasurer.measure(text, style).size.width.toDp() }
    val lineNumber = ceil(widthDp.value.toDouble() / width.value).toInt()
    Log.w("LyricsView", "lineNumber: $lineNumber")
    return heightDp * lineNumber
}