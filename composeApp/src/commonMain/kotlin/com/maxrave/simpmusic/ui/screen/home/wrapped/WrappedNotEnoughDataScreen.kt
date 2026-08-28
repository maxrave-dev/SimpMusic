package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.viewModel.WrappedUiState
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.wrapped_not_enough_body
import simpmusic.composeapp.generated.resources.wrapped_not_enough_days
import simpmusic.composeapp.generated.resources.wrapped_not_enough_progress
import simpmusic.composeapp.generated.resources.wrapped_not_enough_title
import simpmusic.composeapp.generated.resources.wrapped_open_analytics

/**
 * What the reel shows when the year is too thin to say anything true.
 *
 * Drawn inside the reel's own theme rather than the app's, because this is still Wrapped — the
 * user arrived expecting the event, and handing them a differently-coloured error page would read
 * as another feature having failed. It says the number out loud and shows the distance left, so the
 * answer is "come back later" rather than "something went wrong".
 *
 * Both ways out do the same thing: [onBack] pops back to Analytics, which is where the entry
 * banner lives and the only place this screen is reachable from.
 */
@Composable
fun WrappedNotEnoughDataScreen(
    state: WrappedUiState.NotEnoughData,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    // 12dp rather than the 20dp the body column uses: the icon button carries its
                    // own 12dp of touch target around a 24dp glyph, so the glyph itself still lands
                    // on the gutter.
                    .padding(start = 12.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Deliberately NOT the reel's liquid glass: this page is a flat ground with nothing
            // behind the button to refract, and glass over a flat colour is a border pretending to
            // be a surface. The reel's own close button floats over artwork, which is the case
            // glass exists for.
            RippleIconButton(
                imageVector = SimpIcons.ArrowBackIosNew,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(Res.string.wrapped),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.wrapped_not_enough_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = notEnoughBody(state.activeDays),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(28.dp))

            WrappedDaysProgress(
                activeDays = state.activeDays,
                requiredDays = state.requiredDays,
            )
        }

        // A real Material button, not a Row with clickable on it: that hand-rolled version had no
        // ripple, no pressed state and no container colour of its own. The label carries the app's
        // own style explicitly, because typo() defines no labelLarge and the button's default would
        // fall back to the platform font.
        FilledTonalButton(
            onClick = onBack,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 32.dp, end = 32.dp, bottom = 46.dp)
                    .height(48.dp),
        ) {
            Text(
                text = stringResource(Res.string.wrapped_open_analytics),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/**
 * The body sentence with the day count lifted out of the grey.
 *
 * The count is built as its own resource first and then found inside the sentence, rather than the
 * sentence being split around a format specifier: a translator is free to move the placeholder,
 * and some languages will decline the phrase so that it no longer matches character for character.
 * When the lookup fails the whole line simply prints in the body colour, which is a missing
 * emphasis rather than a broken sentence.
 */
@Composable
private fun notEnoughBody(activeDays: Int): AnnotatedString {
    val days = stringResource(Res.string.wrapped_not_enough_days, formatCount(activeDays))
    val sentence = stringResource(Res.string.wrapped_not_enough_body, days)
    val start = sentence.indexOf(days)
    val emphasis = MaterialTheme.colorScheme.onSurface
    return buildAnnotatedString {
        if (start < 0) {
            append(sentence)
            return@buildAnnotatedString
        }
        append(sentence.substring(0, start))
        withStyle(SpanStyle(color = emphasis)) {
            append(days)
        }
        append(sentence.substring(start + days.length))
    }
}

/** How far through the month of listening Wrapped needs before it will say anything. */
@Composable
private fun WrappedDaysProgress(
    activeDays: Int,
    requiredDays: Int,
) {
    // A zero denominator would be a bug upstream rather than a state to render, but dividing by it
    // here would take the whole screen down with a NaN width.
    val fraction =
        if (requiredDays > 0) (activeDays.toFloat() / requiredDays).coerceIn(0f, 1f) else 0f

    Column {
        // The same indicator the reel's segments are, at the one size this screen needs it: a real
        // progress bar rather than two stacked boxes, so the track, the fill and the rounded ends
        // are one component's business. The stop indicator is off — this is distance remaining, not
        // a target the bar should mark.
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
        )
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text =
                stringResource(
                    Res.string.wrapped_not_enough_progress,
                    formatCount(activeDays),
                    formatCount(requiredDays),
                ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The unlit part of the bar is the same ink as the lit part, simply held back. */
private const val TRACK_ALPHA = 0.24f
