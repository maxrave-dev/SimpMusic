package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeMinutes
import com.maxrave.simpmusic.viewModel.WrappedArchetype
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.mono
import simpmusic.composeapp.generated.resources.wrapped_archetype_deep_diver
import simpmusic.composeapp.generated.resources.wrapped_archetype_devotee
import simpmusic.composeapp.generated.resources.wrapped_archetype_explorer
import simpmusic.composeapp.generated.resources.wrapped_archetype_omnivore
import simpmusic.composeapp.generated.resources.wrapped_archetype_regular
import simpmusic.composeapp.generated.resources.wrapped_save
import simpmusic.composeapp.generated.resources.wrapped_share
import simpmusic.composeapp.generated.resources.wrapped_share_artists
import simpmusic.composeapp.generated.resources.wrapped_share_card_title
import simpmusic.composeapp.generated.resources.wrapped_share_days
import simpmusic.composeapp.generated.resources.wrapped_share_minutes
import simpmusic.composeapp.generated.resources.wrapped_share_title
import simpmusic.composeapp.generated.resources.wrapped_share_top_album
import simpmusic.composeapp.generated.resources.wrapped_share_top_artists
import simpmusic.composeapp.generated.resources.wrapped_share_top_tracks
import simpmusic.composeapp.generated.resources.wrapped_type_the
import kotlin.math.ceil

/**
 * Card 10 — the poster, and the only thing in the reel a stranger will ever see.
 *
 * Everything on the panel repeats a figure an earlier card already showed, which is the point: the
 * poster has to stand alone, with no reel around it and no app under it, so it re-states rather
 * than refers. It also has to be legible with no context at all, which is why the wordmark is on
 * it — a picture of someone's year with no idea what made it is a screenshot, not a share.
 *
 * The panel is a miniature, so it carries a ladder of its own rather than one hero: the year off
 * `titleLarge` at its own size, the three counters on `titleMedium`, every ranked row on
 * `titleSmall`, every caps label on `bodySmall`. Four tiers, all from the app's scale, and none of
 * them naming a colour — under `ForceDarkContent` the scale already prints titles bright and
 * labels muted, which is exactly the poster's own hierarchy.
 *
 * @param posterModifier applied to the panel and to nothing else. The shell captures the panel,
 *   not the card: the eyebrow is a caption *about* the image and the two buttons are how you ask
 *   for it, so neither belongs inside it. Defaulted, so a caller that captures some other way is
 *   not obliged to pass anything.
 */
@Composable
fun WrappedShareCard(
    wrapped: WrappedYear,
    onSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_share_title),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WrappedTokens.ScreenPadding),
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Sized from whichever axis runs out first, so the panel keeps its 9:16 on a short
            // window instead of being cropped by the button band underneath it.
            val posterHeight = min(maxHeight, maxWidth / POSTER_ASPECT)
            SharePoster(
                wrapped = wrapped,
                modifier =
                    posterModifier
                        .height(posterHeight)
                        .aspectRatio(POSTER_ASPECT),
            )
        }

        ShareActions(onSave = onSave, onShare = onShare)
    }
}

@Composable
private fun SharePoster(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val ground = MaterialTheme.colorScheme.background

    Box(modifier.clipToBounds().background(ground)) {
        PosterMosaic(
            urls = wrapped.posterArtwork(),
            modifier = Modifier.matchParentSize(),
        )
        // Heavier than the reel's other scrims and deliberately so: this panel carries eighteen
        // lines of small type, and it has to hold them over any cover the year happens to produce.
        // It is the card's own ground at three alphas rather than a colour of its own, so the
        // artwork the scheme was seeded from and the veil over it can never disagree.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.00f to ground.copy(alpha = 0.58f),
                    0.38f to ground.copy(alpha = 0.88f),
                    1.00f to ground.copy(alpha = 0.96f),
                ),
            ),
        )
        Column(
            modifier = Modifier.matchParentSize().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PosterHeader(wrapped.year)
            PosterRankings(wrapped)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    // outlineVariant is the scheme's own role for a divider — a rule between two
                    // blocks of content, not a wash of the text colour at a guessed alpha.
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            PosterFigures(wrapped)
            wrapped.topAlbums.firstOrNull()?.let { album ->
                PosterField(
                    label = stringResource(Res.string.wrapped_share_top_album),
                    value = album.album.title,
                )
            }
            Spacer(Modifier.weight(1f))
            PosterSignature(wrapped)
        }
    }
}

@Composable
private fun PosterHeader(year: Int) {
    Column {
        PosterLabel(
            text = stringResource(Res.string.wrapped_share_card_title).uppercase(),
            tracking = HEADER_TRACKING,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$year",
            // The poster's own hero, and the one size named on it. `titleLarge` arrives Bold in the
            // scale's title colour, so the size is the only thing that has to change.
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = POSTER_YEAR_SIZE,
                    lineHeight = 1.em,
                    letterSpacing = (-0.03).em,
                ),
        )
    }
}

/** Tracks and artists side by side, each numbered, each clipped rather than wrapped. */
@Composable
private fun PosterRankings(wrapped: WrappedYear) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RankedColumn(
            title = stringResource(Res.string.wrapped_share_top_tracks),
            entries = wrapped.topTracks.map { it.song.title },
            modifier = Modifier.weight(1f),
        )
        RankedColumn(
            title = stringResource(Res.string.wrapped_share_top_artists),
            entries = wrapped.topArtists.map { it.artist.name },
            modifier = Modifier.width(ARTIST_COLUMN_WIDTH),
        )
    }
}

@Composable
private fun RankedColumn(
    title: String,
    entries: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PosterLabel(text = title, tracking = LABEL_TRACKING)
        Spacer(Modifier.height(6.dp))
        // Whatever the year produced, up to five. A padded-out list with blank ranks would claim
        // favourites that do not exist.
        entries.take(POSTER_RANK_LIMIT).forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth().height(RANK_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(RANK_NUMBER_WIDTH),
                )
                Text(
                    text = entry,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PosterFigures(wrapped: WrappedYear) {
    val stats = wrapped.stats
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        PosterFigure(
            value = formatCount(wholeMinutes(stats.listenedSeconds)),
            label = stringResource(Res.string.wrapped_share_minutes),
        )
        PosterFigure(
            value = formatCount(stats.distinctArtists),
            label = stringResource(Res.string.wrapped_share_artists),
        )
        PosterFigure(
            value = formatCount(stats.activeDays),
            label = stringResource(Res.string.wrapped_share_days),
        )
    }
}

/**
 * One counter and its unit.
 *
 * `titleMedium` rather than an enlarged `titleLarge`: three of these share one row, and a
 * five-figure minute count set at hero size would push the last one off the panel. The tier below
 * still reads as the second-loudest thing on the poster, because everything under it is 13sp
 * and 11sp.
 */
@Composable
private fun PosterFigure(
    value: String,
    label: String,
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        PosterLabel(text = label, tracking = FIGURE_LABEL_TRACKING)
    }
}

@Composable
private fun PosterField(
    label: String,
    value: String,
) {
    Column {
        PosterLabel(text = label, tracking = LABEL_TRACKING)
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The poster's tracked-out caps line.
 *
 * `bodySmall` and nothing else: under `ForceDarkContent` that style is already the muted grey the
 * artboard prints these in, so the label needs no colour of its own, and tracking is the only
 * thing that varies between the three places it appears.
 */
@Composable
private fun PosterLabel(
    text: String,
    tracking: TextUnit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = tracking),
        maxLines = 1,
    )
}

/**
 * The archetype and the mark, on the line the poster ends on.
 *
 * The disc-and-glyph is the same signature the lyrics share card uses, so two images leaving this
 * app by different routes are signed the same way — down to the inversion: the disc is filled with
 * the ink the panel writes in (`onSurface`) and the logo prints back through it in the panel's own
 * ground (`surface`), which is what keeps the mark readable whatever the artwork seeded.
 *
 * `Res.drawable.mono` is the app's logo, not an icon: `SimpIcons` is the Material Symbols set and
 * has no wordmark in it, and CLAUDE.md keeps the two logos as drawables on purpose. This is the
 * only `painterResource` left in the file.
 */
@Composable
private fun PosterSignature(wrapped: WrappedYear) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = archetypeTitle(wrapped),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(10.dp),
                )
            }
            Text(
                // A brand name, not copy — it is spelled this way in every language, exactly as
                // the lyrics share card spells it, and set in the same muted weight it uses there.
                text = "SimpMusic",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * The two things the card exists to offer.
 *
 * Real Material 3 buttons, not a `Row` with `clickable` on it: the hand-rolled version had no
 * ripple, no pressed or focus state, no disabled state and no guaranteed minimum touch target —
 * the same trade `ListenTogetherScreen` already made. Neither is given a `colors` argument, so
 * Save arrives as the scheme's filled action (`primary` on `onPrimary`) and Share as its tonal one
 * (`secondaryContainer`), which is exactly the loud/quiet pair the artboard draws and is seeded
 * from the user's own artwork for free.
 *
 * The band is exactly the height of the shell's footer and the buttons sit at the TOP of it, which
 * puts them where the artboard draws them and leaves the remaining 30dp as clearance. That
 * clearance is load-bearing, not white space: it is what keeps the buttons off the system
 * navigation inset, so the band must not be shrunk to the buttons.
 *
 * This is the only card that gets that band. The shell subtracts [WrappedTokens.FooterHeight] per
 * page inside its card slot rather than from the pager's own box — the pager therefore stays one
 * constant size and nothing re-lays-out mid-swipe, while this page alone keeps the strip its
 * footer would have covered and draws its own actions into it.
 */
@Composable
private fun ShareActions(
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(WrappedTokens.FooterHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Button(
            onClick = onSave,
            shape = CircleShape,
            contentPadding = ACTION_CONTENT_PADDING,
        ) {
            ActionContent(SimpIcons.Download, stringResource(Res.string.wrapped_save))
        }
        FilledTonalButton(
            onClick = onShare,
            shape = CircleShape,
            contentPadding = ACTION_CONTENT_PADDING,
        ) {
            ActionContent(SimpIcons.Share, stringResource(Res.string.wrapped_share))
        }
    }
}

/**
 * Icon then label, at the spacing Material itself specifies for a button with a leading icon.
 *
 * The label has to be handed `LocalContentColor` explicitly. `typo()` sets a colour on every style
 * it defines, and a style's own colour beats the one the button provides — so without this, Save's
 * label would print the scale's white on a `primary` container that the artwork can just as easily
 * have made pale.
 */
@Composable
private fun ActionContent(
    icon: ImageVector,
    label: String,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = LocalContentColor.current,
        maxLines = 1,
    )
}

/**
 * Three columns of square covers across the top of the panel, each cover used exactly once.
 *
 * It stops when the covers run out rather than cycling back to the first. Cycling is right for
 * card 01's wall, where repetition under a heavy scrim reads as wallpaper, but a poster is a
 * sparser surface and it degenerates badly here: whenever the number of covers is a multiple of
 * the column count the rows all land on the same three, striping the whole panel vertically. A
 * thin year producing exactly three covers — one album heard on repeat — is the worst case and
 * not a rare one.
 *
 * Stopping early is also closer to the artboard, which tiles nine covers across the top of a
 * panel two-thirds taller than they reach and leaves the rest to the scrim.
 */
@Composable
private fun PosterMosaic(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (urls.isEmpty()) return
    BoxWithConstraints(modifier) {
        val tile = maxWidth / POSTER_MOSAIC_COLUMNS
        val fittingRows = ceil(maxHeight / tile).toInt()
        val coverRows = ceil(urls.size.toFloat() / POSTER_MOSAIC_COLUMNS).toInt()
        Column {
            repeat(minOf(fittingRows, coverRows).coerceAtLeast(1)) { row ->
                Row {
                    repeat(POSTER_MOSAIC_COLUMNS) { column ->
                        val cover = urls.getOrNull(row * POSTER_MOSAIC_COLUMNS + column)
                        // A short last row leaves ground rather than reaching back for a cover
                        // already on the panel; under this scrim the gap does not read as one.
                        if (cover == null) {
                            Spacer(Modifier.size(tile))
                        } else {
                            WrappedArtwork(url = cover, modifier = Modifier.size(tile))
                        }
                    }
                }
            }
        }
    }
}

/** Everything the year has a cover for, most-played first, each cover used once. */
private fun WrappedYear.posterArtwork(): List<String> {
    val covers =
        topTracks.map { it.song.thumbnails } +
            topAlbums.map { it.album.thumbnails } +
            topArtists.map { it.artist.thumbnails }
    return covers.mapNotNull { url -> url?.takeIf { it.isNotBlank() } }.distinctByArtwork()
}

/** "THE DEEP DIVER" — the same two pieces card 08 stacks, set on one line here. */
@Composable
private fun archetypeTitle(wrapped: WrappedYear): String {
    val article = stringResource(Res.string.wrapped_type_the)
    return "$article ${stringResource(wrapped.archetype.posterNameRes())}"
}

/**
 * Repeated rather than shared with card 08: a top-level `private` is scoped to its own file, and
 * making it `internal` would put a name every card in this package can see into the namespace two
 * agents were writing into at once. Five one-line branches is the cheaper of the two risks.
 */
private fun WrappedArchetype.posterNameRes(): StringResource =
    when (this) {
        WrappedArchetype.THE_REGULAR -> Res.string.wrapped_archetype_regular
        WrappedArchetype.THE_EXPLORER -> Res.string.wrapped_archetype_explorer
        WrappedArchetype.THE_OMNIVORE -> Res.string.wrapped_archetype_omnivore
        WrappedArchetype.THE_DEVOTEE -> Res.string.wrapped_archetype_devotee
        WrappedArchetype.THE_DEEP_DIVER -> Res.string.wrapped_archetype_deep_diver
    }

private const val POSTER_ASPECT = 268f / 477f

private const val POSTER_MOSAIC_COLUMNS = 3

private const val POSTER_RANK_LIMIT = 5

private val ARTIST_COLUMN_WIDTH = 88.dp

/** Tall enough for `titleSmall`'s own line box; the artboard's 18px was drawn around 10.5px type. */
private val RANK_ROW_HEIGHT = 20.dp

/** Wide enough for a two-digit rank, so the titles start on one line however long the list is. */
private val RANK_NUMBER_WIDTH = 14.dp

/**
 * The one figure the poster is built around.
 *
 * Named in sp because it is a font size, and fed to `titleLarge.copy(fontSize = …)` rather than
 * built into a `TextStyle` — the family, the weight and the colour still arrive from the theme.
 */
private val POSTER_YEAR_SIZE = 34.sp

private val HEADER_TRACKING = 0.22.em

private val LABEL_TRACKING = 0.16.em

private val FIGURE_LABEL_TRACKING = 0.10.em

/** ~44dp tall with Material's own icon metrics inside it — the artboard's pill, to the dp. */
private val ACTION_CONTENT_PADDING = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/** Gap between the shell's header and this card's eyebrow. */
private val CARD_TOP_GAP = 14.dp
