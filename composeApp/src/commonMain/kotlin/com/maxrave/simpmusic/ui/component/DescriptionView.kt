package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.less
import simpmusic.composeapp.generated.resources.more
import kotlin.math.roundToInt

@Composable
fun DescriptionView(
    modifier: Modifier = Modifier,
    text: String,
    limitLine: Int = 3,
    onTimeClicked: (time: String) -> Unit,
    onURLClicked: (url: String) -> Unit,
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    var shouldHideExpandButton by rememberSaveable {
        mutableStateOf(false)
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(layoutResult) {
        val lineCount = layoutResult?.lineCount ?: 0
        if (lineCount < limitLine) {
            shouldHideExpandButton = true
        }
    }

    val timeRegex = Regex("""(\d+):(\d+)(?::(\d+))?""")
    // Parentheses count only as a pair: descriptions wrap links in them — "From Wikipedia
    // (https://…/Ariana_Grande)" — and `\S+` swallowed the closing one into the URL, while
    // "…/Queen_(band)" must keep its own.
    val urlRegex = Regex("""https?://(?:[^\s()]|\([^\s()]*\))+""")
    val annotatedString = AnnotatedString.Builder()
    var currentIndex = 0
    val style =
        SpanStyle(
            color = Color(0xFF00B0FF),
            fontWeight = FontWeight.Normal,
        )
    val combinedRegex = Regex("${timeRegex.pattern}|${urlRegex.pattern}")
    val matchedWords = combinedRegex.findAll(text)
    matchedWords.forEachIndexed { index, matchResult ->
        // Add text before the match
        if (matchResult.range.first > currentIndex) {
            annotatedString.append(text.substring(currentIndex, matchResult.range.first))
        }

        // Add the matched text with the given style
        annotatedString.withStyle(style) {
            if (timeRegex.matches(matchResult.value)) {
                pushStringAnnotation("CLICKABLE_USER_TIME", matchResult.value)
                append(matchResult.value)
                pop()
            } else if (urlRegex.matches(matchResult.value)) {
                pushStringAnnotation("CLICKABLE_USER_URL", matchResult.value)
                append(matchResult.value)
                pop()
            }
        }
        if (index == matchedWords.count() - 1) {
            annotatedString.append(text.substring(matchResult.range.last + 1, text.length))
        }

        // Update the current index to the end of the match
        currentIndex = matchResult.range.last + 1
    }
    if (matchedWords.count() == 0) {
        annotatedString.append(text)
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = typo().bodyMedium

    Column(modifier) {
        // The full text is always laid out; what animates is the HEIGHT of the window onto it, in
        // pixels, between "limitLine lines" and "all of it", with the rest clipped. Animating
        // maxLines instead steps a whole line per frame (visibly jerky), and switching it at once
        // under a size animation cut the text before the box caught up. Both heights are measured
        // at the real width from BoxWithConstraints, so they are right from the first frame.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val maxWidthPx = constraints.maxWidth
            val (fullHeightPx, collapsedHeightPx) =
                remember(text, maxWidthPx, textStyle, limitLine) {
                    val layout =
                        textMeasurer.measure(
                            annotatedString.toAnnotatedString(),
                            textStyle,
                            constraints = Constraints(maxWidth = maxWidthPx),
                        )
                    val collapsed =
                        if (layout.lineCount > limitLine) {
                            layout.getLineBottom(limitLine - 1).roundToInt()
                        } else {
                            layout.size.height
                        }
                    layout.size.height to collapsed
                }
            val heightPx by animateIntAsState(
                targetValue = if (expanded) fullHeightPx else collapsedHeightPx,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
            Text(
                text = annotatedString.toAnnotatedString(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(with(LocalDensity.current) { heightPx.toDp() })
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                layoutResult?.let { layoutResult ->
                                    val position = layoutResult.getOffsetForPosition(offset)
                                    Logger.w("DescriptionView", "Position: $position")
                                    annotatedString
                                        .toAnnotatedString()
                                        .getStringAnnotations(
                                            start = position,
                                            end = position,
                                        ).firstOrNull { annotation ->
                                            Logger.w("DescriptionView", "Annotation: ${annotation.tag}")
                                            annotation.tag.startsWith("CLICKABLE_USER_")
                                        }?.let { annotation ->
                                            when (annotation.tag) {
                                                "CLICKABLE_USER_TIME" -> {
                                                    Logger.w("DescriptionView", "Time clicked: ${annotation.item}")
                                                    onTimeClicked(annotation.item)
                                                }

                                                "CLICKABLE_USER_URL" -> {
                                                    Logger.w("DescriptionView", "URL clicked: ${annotation.item}")
                                                    onURLClicked(annotation.item)
                                                }
                                            }
                                        }
                                }
                            }
                        },
                onTextLayout = { layoutResult = it },
                style = textStyle,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.animation.AnimatedVisibility(!shouldHideExpandButton) {
            Text(
                text = if (expanded) stringResource(Res.string.less) else stringResource(Res.string.more),
                color = Color.LightGray,
                modifier =
                    Modifier.clickable {
                        expanded = !expanded
                    },
                style = typo().labelSmall,
            )
        }
    }
}