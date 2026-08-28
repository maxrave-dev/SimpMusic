package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.math.abs

/**
 * Apple Music renders EVERY lyric line at the same size — what separates the line being sung from
 * the rest is focus, not scale: the active line is opaque and sharp, its neighbours dim and
 * progressively blurred with distance, as if the page had a shallow depth of field.
 *
 * That is the whole trick, and it is why the style is gated on blur support: without the blur the
 * lines merely fade, which the app's Classic renderer already does better by changing size too.
 */
// The DISTANCE rule is AMLL's (resolveBlurLevel in base/index.ts): the sung line is exempt
// outright, and lines already sung carry a +1 so the page behind the singer recedes faster than
// the page ahead — you are meant to read forward, not back.
//
// The MAGNITUDE is not AMLL's. Theirs is `min(5, (1 + distance) * 0.8)` in CSS px, sized for the
// font its host happens to set; at 32sp that lands around 1.6dp one line out, which is invisible.
// These numbers come from measuring the reference screenshot instead — its text measures ~68px on
// a 920px-wide capture, i.e. 32pt, the same size used here, so the blur radii transfer directly:
//
//     one line out   ~6px  -> 2.8pt    three out  ~20px -> 9.3pt
//     two out       ~12px  -> 5.6pt    four out   ~28px -> 13pt
//
// Expressed against the font size rather than in fixed dp, so changing the type size keeps the
// depth of field proportional.
private const val BLUR_PER_LINE_EM = 0.095f
private const val BLUR_MAX_EM = 0.45f
// AMLL's resolveOpacity returns a flat 1 for unsung lines, but the reference screenshots plainly
// fade with distance — the line under the sung one sits at roughly half, the next at a third, and
// beyond that they all but vanish. AMLL is reproducing Apple, not defining it, and on this point
// the screenshots win.
//
// Blur and this work together rather than either doing the job alone: blur alone leaves far lines
// as bright smears, fade alone leaves them sharp and readable when they should not be.
private const val ACTIVE_LINE_ALPHA = 1f
// Measured off the same screenshot: one line out still reads clearly, two is noticeably dimmer,
// and past three they level off — blur is doing most of the work by then, so driving opacity any
// harder just turns the far lines black.
private const val ALPHA_FALLOFF_PER_LINE = 0.25f
private const val MIN_LINE_ALPHA = 0.25f

// Before the FIRST line is due — an intro, a long instrumental opening — there is no sung line for
// anything to be near, so distance is meaningless and every line is equally "not yet". They are
// dimmed uniformly and NOT blurred: blur means "far from where we are in the song", and during an
// intro nowhere is where we are. It also has to stay readable, since this is exactly when someone
// glances down to see what is coming. One flat value, deliberately not the distance falloff, or
// the top of the sheet would read as the active line by being the brightest thing on it.
private const val PRE_ROLL_LINE_ALPHA = 0.6f

// The unsung REMAINDER of the line currently being sung — "ce" in "dan|ce". It has to be clearly
// grey against the white of what has already been sung, or the wipe carries no contrast and the
// line never reads as lighting up. Classic's DimRichPendingColor is LightGray at 60%, far too
// close to white for that.
internal val AppleMusicPendingWordColor = Color(0xFF8E8E8E)

/**
 * Apple sets lyrics far larger than body copy — a line fills most of the width and wraps after a
 * few words, which is what makes the page readable at arm's length while the phone sits on a desk.
 * The app's own headlineLarge is 23sp, sized for list headers rather than for this, so the size is
 * overridden here while the weight and font family still come from the theme.
 */
internal val AppleMusicLyricFontSize = 28.sp
// Taken from AMLL's stylesheet (packages/core/src/styles/lyric-player.module.css) rather than
// measured off a screenshot. Everything there is expressed in `em`, so it scales with whatever
// font size the host picks — these are those ratios resolved against AppleMusicLyricFontSize.
//
//   .lyricLineWrapper { padding: 0.4em <x> }  -> 0.4 * 32 = 12.8dp above AND below each line
//   .lyricMainLine    — no line-height at all -> the browser default, ~1.2em
//
// Note which one is bigger: the leading INSIDE a wrapped line is tighter than the padding between
// two lines, and that is what makes a wrapped lyric read as one thought instead of two.
internal val AppleMusicLyricLineHeight = 34.sp
internal val AppleMusicLyricGap = 20.dp

// Sizing from .lyricSubLine { font-size: max(0.5em, 10px); line-height: 1.5em } plus the flex
// `gap: 0.3em` above it.
//
// Its `opacity: 0.3` is deliberately NOT copied. That rule dresses AMLL's romanisation/ruby row —
// a pronunciation aid you glance at — whereas this row is the TRANSLATION, which the user turned
// on specifically to read. At 0.3 it was unreadable even on the line being sung. It follows the
// line's own colour instead: white-ish while sung, grey with the rest.
internal val AppleMusicSubLineFontSize = 14.sp
internal val AppleMusicSubLineHeight = 21.sp
internal val AppleMusicMainToSubGap = 12.dp

// .lyricLineWrapper { border-radius: 0.25em } plus its :hover / :active background. There is no
// hover on a phone, so the press state takes the more visible of the two (#fff1). Note it is a
// tinted panel behind the WHOLE wrapper — original line and translation together — not a ripple:
// AMLL never uses one, and a ripple centred on a tap point reads as a button press.
// --lyric-line-padding-x: 20px on a narrow viewport. It lives on .lyricLineWrapper in AMLL, i.e.
// on the LINE, not on whatever screen happens to host the list — which is why LyricsView applies
// it itself rather than trusting each caller. FullscreenLyricsSheet passes a bare fillMaxSize(),
// so a caller-supplied gutter would have left the fullscreen sheet's text flush against both
// edges while the Now Playing tab looked right.
internal val AppleMusicLyricPaddingX = 20.dp

internal val AppleMusicLyricCornerRadius = 8.dp
internal val AppleMusicLyricPressedBackground = Color.White.copy(alpha = 0.067f)

// FlowRow lays wrapped words out itself and ignores lineHeight entirely, so the rich-sync path
// needs the same leading expressed as explicit spacing between its rows — and it has to stay
// TIGHTER than AppleMusicLyricGap, for the same reason: rows here are one lyric wrapping, while
// the gap separates two different lyrics. 38sp of leading over the ~37sp a 32sp line occupies
// leaves almost nothing, which matches AMLL letting the default leading stand.
internal val AppleMusicWrappedLineSpacing = 2.dp

// White, like Apple — NOT the Classic renderer's yellow. Slightly under full opacity so the
// translation still reads as secondary to the line it translates.
internal val AppleMusicTranslatedColor = Color.White.copy(alpha = 0.78f)

// A shade under the translation's: on a line carrying original + reading + translation, the
// reading is the one you stop needing once you know the song, so it recedes first.
internal val AppleMusicRomanizedColor = Color.White.copy(alpha = 0.62f)

// The line NOT being sung is grey, full stop — white at reduced opacity is still white on a dark
// page, which is why every line looked equally lit no matter how the alpha was tuned. This is the
// single biggest signal separating the sung line from the rest; blur and opacity only refine it.
internal val AppleMusicInactiveLineColor = Color(0xFF9B9B9B)

// Passing this to a rich-synced line is what ENABLES AMLL's emphasis maths in AnimatedWord; the
// alpha and radius here are placeholders, because that code computes both from the word's own
// duration and its position within the wipe. Only the colour is read as-is.
//
// Deliberately NOT applied to line-synced lyrics: those carry no per-word timing, so there is no
// sustain to reflect, and a constant halo there is pure decoration — which is what made the first
// attempt look wrong.
internal val AppleMusicActiveLineGlow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 0f)

// No glow constant for line-synced lyrics on purpose. Without per-word timing there is no single
// word to flare, and lighting the whole line instead just makes it look smudged — the sung line is
// marked by being WHITE against grey, and that is all.

/**
 * Depth-of-field treatment for one lyric line. [distanceFromCurrent] is signed line distance from
 * the line being sung; its magnitude drives both blur and dimming.
 *
 * Both values are animated rather than applied outright, so the focus glides down the page with
 * the song instead of snapping — the same reason [LyricsView] animates its scroll rather than
 * jumping.
 */
@Composable
fun Modifier.appleMusicLyricFocus(
    distanceFromCurrent: Int,
    blurEnabled: Boolean,
    hasActiveLine: Boolean = true,
): Modifier {
    // Signed: negative means this line has already been sung. AMLL adds one to that side so the
    // page behind the singer recedes faster than the page ahead of it.
    val distance =
        when {
            distanceFromCurrent < 0 -> abs(distanceFromCurrent) + 1
            else -> distanceFromCurrent
        }
    val fontSizeDp = with(LocalDensity.current) { AppleMusicLyricFontSize.toDp() }
    val targetBlur: Dp =
        if (!blurEnabled || !hasActiveLine || distanceFromCurrent == 0) {
            0.dp
        } else {
            fontSizeDp * (distance * BLUR_PER_LINE_EM).coerceAtMost(BLUR_MAX_EM)
        }
    val targetAlpha =
        when {
            // Checked FIRST: the caller passes distance 0 for every line while no line is active,
            // and 0 otherwise means "this is the sung line". Read in the other order, the whole
            // sheet would take the sung line's full opacity during an intro.
            !hasActiveLine -> PRE_ROLL_LINE_ALPHA
            distanceFromCurrent == 0 -> ACTIVE_LINE_ALPHA
            else -> (1f - distance * ALPHA_FALLOFF_PER_LINE).coerceAtLeast(MIN_LINE_ALPHA)
        }

    val blurRadius by animateDpAsState(targetValue = targetBlur, animationSpec = tween(400), label = "appleMusicLyricBlur")
    val lineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(400), label = "appleMusicLyricAlpha")

    // alpha BEFORE blur: blurring an already-faded line keeps the two effects independent, whereas
    // fading a blurred layer washes the blur out into a flat smear.
    return this
        .alpha(lineAlpha)
        .then(
            if (blurRadius > 0.dp) {
                // Unbounded, NOT the default, on Android. blur(radius) alone uses
                // BlurredEdgeTreatment.Rectangle, which clips the blur to the line's own bounds —
                // so the softened glyphs get sliced off square at the edges and the line reads as a
                // smudged block rather than an out-of-focus word. Unbounded lets the blur bleed
                // past the bounds, which is what makes it look like depth of field.
                //
                // Desktop cannot have that. Each line is a LazyColumn item and skiko clips an item
                // to its own bounds, so the part Unbounded deliberately paints outside gets sliced
                // off at the item boundary — a hard horizontal tear across the line below the one
                // being sung. Rectangle keeps every pixel inside the item, which is what that clip
                // wants. The trade is softer-looking edges, only on lines already out of focus.
                val edge =
                    if (getPlatform() == Platform.Desktop) {
                        BlurredEdgeTreatment.Rectangle
                    } else {
                        BlurredEdgeTreatment.Unbounded
                    }
                Modifier.blur(blurRadius, edge)
            } else {
                Modifier
            },
        )
}

/**
 * One line-synced lyric line, Apple Music style: same size for every line (the Classic renderer
 * swaps headlineLarge/headlineMedium instead), white, hard left, with the translation underneath.
 * Focus is applied by the caller through [appleMusicLyricFocus] so the blur wraps the whole line
 * including its translation.
 */
@Composable
fun AppleMusicLyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isCurrent: Boolean,
    // Sits between the original and the translation. Shares the sub-line type scale with the
    // translation, since both are secondary to the lyric itself and a second size at this scale
    // would just look like a mistake.
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
        Text(
            text = originalWords,
            // fillMaxWidth + Start, both explicit: a wrapped line must break against the SAME left
            // edge as every other line, and a short line must not drift toward the middle.
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = if (isCurrent) Color.White else AppleMusicInactiveLineColor,
            style =
                typo().headlineLarge.copy(
                    fontSize = AppleMusicLyricFontSize,
                    lineHeight = AppleMusicLyricLineHeight,
                ),
        )
        if (romanizedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            Text(
                text = romanizedWords,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style =
                    typo().bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                // Follows the line's own state exactly as the translation does: readable white-ish
                // on the sung line, the same grey as the lyric everywhere else.
                color = if (isCurrent) AppleMusicRomanizedColor else AppleMusicInactiveLineColor,
            )
        }
        if (translatedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            Text(
                text = translatedWords,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style =
                    typo().bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                // The colour still follows the line's own state; AMLL carries the whole sub-line at
                // a flat 0.3 opacity on top of that, which is applied as a modifier so it composes
                // with appleMusicLyricFocus's own dimming instead of fighting it.
                color = if (isCurrent) AppleMusicTranslatedColor else AppleMusicInactiveLineColor,
            )
        }
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
    }
}
