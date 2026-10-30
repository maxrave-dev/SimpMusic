package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Error
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.UserLrcMark
import com.maxrave.simpmusic.utils.UserLrcProblem
import com.maxrave.simpmusic.utils.checkUserLrc
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_lyrics
import simpmusic.composeapp.generated.resources.add_lyrics_bad_offset
import simpmusic.composeapp.generated.resources.add_lyrics_bad_time
import simpmusic.composeapp.generated.resources.add_lyrics_bad_word_time
import simpmusic.composeapp.generated.resources.add_lyrics_hint
import simpmusic.composeapp.generated.resources.add_lyrics_line
import simpmusic.composeapp.generated.resources.add_lyrics_lines
import simpmusic.composeapp.generated.resources.add_lyrics_no_time
import simpmusic.composeapp.generated.resources.add_lyrics_no_timed_lines
import simpmusic.composeapp.generated.resources.add_lyrics_placeholder
import simpmusic.composeapp.generated.resources.add_lyrics_problems
import simpmusic.composeapp.generated.resources.add_lyrics_repeated_word_line
import simpmusic.composeapp.generated.resources.add_lyrics_word_time_backwards
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.save

/**
 * Lyrics typed or pasted by hand for a song no provider has lyrics for: plain LRC, or eLRC timed
 * word by word. Times are coloured as they are typed, every problem is named with its line, and
 * Save stays off until nothing is left to fix. [onSave] gets lyrics shaped like a provider's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLyricsSheet(
    songTitle: String,
    artist: String,
    onSave: (Lyrics) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val textState = rememberTextFieldState()
    val text = textState.text.toString()
    val check = remember(text) { checkUserLrc(text) }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }

    val lineTimeColor = MaterialTheme.colorScheme.primary
    val wordTimeColor = MaterialTheme.colorScheme.tertiary
    val problemColor = MaterialTheme.colorScheme.error
    val tagColor = colors.subtitle
    // Marks are read off the field's own text rather than [check]: the field can draw a newer text
    // than the last composition saw, and a mark past its end is out of range.
    val highlight =
        remember(lineTimeColor, wordTimeColor, problemColor, tagColor) {
            OutputTransformation {
                checkUserLrc(asCharSequence().toString()).marks.forEach { mark ->
                    val style =
                        when (mark.kind) {
                            UserLrcMark.Kind.LINE_TIME -> SpanStyle(color = lineTimeColor)
                            UserLrcMark.Kind.WORD_TIME -> SpanStyle(color = wordTimeColor)
                            UserLrcMark.Kind.TAG -> SpanStyle(color = tagColor)
                            UserLrcMark.Kind.PROBLEM -> SpanStyle(color = problemColor, textDecoration = TextDecoration.Underline)
                        }
                    addStyle(style, mark.start, mark.end)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // Transparent above the card, so the sheet stops below the status bar.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(32.dp)
                                .height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.add_lyrics),
                            style = typo().titleMedium,
                            color = colors.content,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                check.lyrics?.let {
                                    onSave(it)
                                    close()
                                }
                            },
                            enabled = check.lyrics != null,
                        ) {
                            Text(text = stringResource(Res.string.save), color = LocalContentColor.current)
                        }
                        IconButton(onClick = close) {
                            Icon(imageVector = SimpIcons.Close, contentDescription = null, tint = colors.content)
                        }
                    }
                    Text(
                        text = listOf(songTitle, artist).filter { it.isNotBlank() }.joinToString(" · "),
                        style = typo().bodyMedium,
                        color = colors.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), thickness = 1.dp)
                    BasicTextField(
                        state = textState,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        textStyle = typo().bodyMedium.copy(color = colors.content),
                        cursorBrush = SolidColor(colors.content),
                        outputTransformation = highlight,
                        decorator = { innerTextField ->
                            Box {
                                if (textState.text.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.add_lyrics_placeholder),
                                        style = typo().bodyMedium,
                                        color = colors.disabled,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)) {
                        val lyrics = check.lyrics
                        when {
                            text.isBlank() -> {
                                Text(text = stringResource(Res.string.add_lyrics_hint), style = typo().bodyMedium, color = colors.subtitle)
                            }

                            check.problems.isNotEmpty() -> {
                                val count = check.problems.size
                                StatusRow(
                                    icon = SimpIcons.Error,
                                    tint = problemColor,
                                    text = pluralStringResource(Res.plurals.add_lyrics_problems, count, count),
                                    color = colors.content,
                                )
                                // Every problem is underlined in the field; the first few say what to fix.
                                check.problems.take(3).forEach { problem ->
                                    val message = problem.message()
                                    val line = if (problem.line > 0) stringResource(Res.string.add_lyrics_line, problem.line) else null
                                    Text(
                                        text =
                                            buildAnnotatedString {
                                                if (line != null) {
                                                    withStyle(SpanStyle(color = problemColor, fontWeight = FontWeight.Medium)) { append(line) }
                                                    append("  ")
                                                }
                                                append(message)
                                            },
                                        style = typo().bodyMedium,
                                        color = colors.subtitle,
                                        modifier = Modifier.padding(start = 24.dp),
                                    )
                                }
                            }

                            lyrics != null -> {
                                val count = lyrics.lines?.size ?: 0
                                val sync = stringResource(if (lyrics.syncType == "RICH_SYNCED") Res.string.rich_synced else Res.string.line_synced)
                                StatusRow(
                                    icon = SimpIcons.Check,
                                    tint = lineTimeColor,
                                    text = pluralStringResource(Res.plurals.add_lyrics_lines, count, count) + " · " + sync,
                                    color = colors.content,
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
private fun StatusRow(
    icon: ImageVector,
    tint: Color,
    text: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = typo().bodyMedium, color = color)
    }
}

@Composable
private fun UserLrcProblem.message(): String =
    when (kind) {
        UserLrcProblem.Kind.NO_TIME -> stringResource(Res.string.add_lyrics_no_time)
        UserLrcProblem.Kind.BAD_TIME -> stringResource(Res.string.add_lyrics_bad_time, value)
        UserLrcProblem.Kind.BAD_WORD_TIME -> stringResource(Res.string.add_lyrics_bad_word_time, value)
        UserLrcProblem.Kind.WORD_TIME_BACKWARDS -> stringResource(Res.string.add_lyrics_word_time_backwards, value)
        UserLrcProblem.Kind.REPEATED_WORD_LINE -> stringResource(Res.string.add_lyrics_repeated_word_line)
        UserLrcProblem.Kind.BAD_OFFSET -> stringResource(Res.string.add_lyrics_bad_offset)
        UserLrcProblem.Kind.NO_TIMED_LINES -> stringResource(Res.string.add_lyrics_no_timed_lines)
    }
