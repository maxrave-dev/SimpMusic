package com.maxrave.simpmusic.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/** How many days of history the two top charts cover. */
private const val INSIGHTS_WINDOW_DAYS = 30

/** Five across, matching the Recently added strip on the Playlists widget. */
private const val INSIGHTS_TILE_COUNT = 5

/**
 * How deep to read each chart before filtering.
 *
 * The charts rank by play events, but the artwork comes from the artist / album / song
 * tables, and a row only lands there once its page has been opened. Cutting to four first
 * and dropping the misses afterwards is what left the artist row one short — take the
 * surplus, drop what has no cached row, then cut.
 */
private const val INSIGHTS_QUERY_DEPTH = 12

private val SONG_TILE_SIZE = 58.dp

/**
 * Decode size for every tile on this widget, in pixels.
 *
 * Eight covers at their native ~1145 px is 42 MB of bitmap, and the system rejects any
 * RemoteViews update over ~20 MB — the widget then renders Glance's error layout. 256 px
 * still oversamples a 54 dp tile on a 3x screen and brings all eight down to ~2 MB.
 */
private const val TILE_DECODE_PX = 256

/**
 * Secondary text, as white at 60%.
 *
 * A fixed grey only works against a fixed background; now that the surface is the
 * artwork's palette it has to be derived from white so it stays legible whatever the
 * cover happens to be. The palette colour is already chosen dark enough for white.
 */
private val mutedOnPalette = Color.White.copy(alpha = 0.6f)
private val ARTIST_TILE_SIZE = 52.dp

private data class SongTile(
    val artwork: Bitmap?,
    val title: String,
)

private data class ArtistTile(
    val artwork: Bitmap?,
    val name: String,
)

/**
 * A 4x4 widget showing total listening time, play count, and the 30-day top songs and artists.
 *
 * It reads the repositories directly rather than AnalyticsViewModel: that one is registered in
 * Koin as `viewModel { }` and has no owner outside a composition, whereas the repositories are
 * singletons. Queries run once per widget update inside provideGlance — never on the drawing
 * path — which is why this widget's update period is deliberately long.
 *
 * The two charts replaced a plain text list of recently played tracks. That list duplicated the
 * Playlists widget's "Recently added" strip almost row for row, and it left the lower half of a
 * 4x4 cell empty; the top queries were already in AnalyticsRepository and were going unused.
 *
 * Mood & Genres and Recommendations from the reference layout are still left out: SimpMusic has
 * no genre data yet, and shipping a permanently empty panel is worse than not showing it.
 */
class ListeningInsightsWidget :
    GlanceAppWidget(),
    KoinComponent {
    // Exact, not the default Single: with Single the composition is laid out against the
    // XML min sizes and the launcher then squeezes that rendering into whatever cells it
    // actually granted — non-uniformly on grids whose cells have a different aspect, which
    // is how every square in these widgets came out a rectangle on some launchers. Exact
    // re-composes per granted size, so the layout works with the truth.
    override val sizeMode: SizeMode = SizeMode.Exact

    private val analyticsRepository by inject<AnalyticsRepository>()
    private val songRepository by inject<SongRepository>()
    private val artistRepository by inject<ArtistRepository>()
    private val sharedViewModel by inject<SharedViewModel>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        serviceScope.launch {
            sharedViewModel.nowPlayingScreenData.collectLatest { refresh(context) }
        }

        provideContent {
            GlanceTheme {
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

                // The background is the palette of whatever is playing, the same source the
                // Turntable and Playlists widgets read. It used to be a hardcoded plum that
                // matched nothing: with all three widgets on one screen, two of them shifted
                // with the music while this one sat still and read as another app's.
                val paletteState = rememberPaletteState()
                var paletteArtwork by remember { mutableStateOf<Bitmap?>(null) }
                var bgColor by remember { mutableStateOf(Color.Black) }
                val thumbUrl by remember { derivedStateOf { screenDataState.thumbnailURL } }

                LaunchedEffect(bgColor) { refresh(context) }

                LaunchedEffect(paletteArtwork) {
                    paletteArtwork?.asImageBitmap()?.let { paletteState.generate(it) }
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { paletteState.palette }
                        .distinctUntilChanged()
                        .collectLatest { bgColor = it.getColorFromPalette() }
                }

                LaunchedEffect(thumbUrl) {
                    // The shared "BIGGER" cache key, so all three widgets decode the identical
                    // bitmap — a different key can hand back another scaling, which is enough to
                    // move the palette onto a different colour.
                    paletteArtwork = context.loadBitmap(thumbUrl, thumbUrl + "BIGGER", TILE_DECODE_PX)
                }

                var listeningTime by remember { mutableStateOf<Long?>(null) }
                var plays by remember { mutableStateOf<Long?>(null) }
                var topSongs by remember { mutableStateOf<List<SongTile>>(emptyList()) }
                var topArtists by remember { mutableStateOf<List<ArtistTile>>(emptyList()) }

                LaunchedEffect(Unit) {
                    listeningTime = analyticsRepository.getTotalListeningTimeInSeconds().firstOrNull()
                    plays = analyticsRepository.getTotalPlaybackEventCount().firstOrNull()

                    val ranked =
                        analyticsRepository
                            .queryTopPlayedSongsLastXDays(INSIGHTS_WINDOW_DAYS)
                            .firstOrNull()
                            .orEmpty()
                            .take(INSIGHTS_QUERY_DEPTH)
                    if (ranked.isNotEmpty()) {
                        val byId =
                            songRepository
                                .getSongsByListVideoId(ranked.map { it.videoId })
                                .firstOrNull()
                                .orEmpty()
                                .associateBy { it.videoId }
                        // Re-sorted by the ranking, not by what the database hands back, so the
                        // number on each cover matches the cover it sits on.
                        topSongs =
                            ranked
                                .mapNotNull { byId[it.videoId] }
                                .take(INSIGHTS_TILE_COUNT)
                                .map {
                                    SongTile(
                                        artwork = context.loadBitmap(it.thumbnails, it.videoId + "INSIGHT", TILE_DECODE_PX),
                                        title = it.title,
                                    )
                                }
                    }

                    topArtists =
                        analyticsRepository
                            .queryTopArtistsLastXDays(INSIGHTS_WINDOW_DAYS)
                            .firstOrNull()
                            .orEmpty()
                            .take(INSIGHTS_QUERY_DEPTH)
                            .mapNotNull { artistRepository.getArtistById(it.channelId).firstOrNull() }
                            .take(INSIGHTS_TILE_COUNT)
                            .map {
                                ArtistTile(
                                    artwork = context.loadBitmap(it.thumbnails, it.channelId + "INSIGHT", TILE_DECODE_PX),
                                    name = it.name,
                                )
                            }
                }

                Column(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(bgColor))
                            .padding(12.dp)
                            .clickable(
                                actionStartActivity<MainActivity>(),
                                rippleOverride = R.drawable.no_ripple,
                            ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.rounded_insights_24),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                            modifier = GlanceModifier.size(18.dp),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.widget_insights_label),
                            style =
                                TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                    }

                    Spacer(GlanceModifier.height(20.dp))

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatTile(
                            value = listeningTime?.let { formatListeningTime(it) } ?: "—",
                            label = context.getString(R.string.widget_insights_time),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        StatTile(
                            value = plays?.toString() ?: "—",
                            label = context.getString(R.string.widget_insights_plays),
                        )
                    }

                    if (topSongs.isNotEmpty()) {
                        SectionLabel(context.getString(R.string.widget_insights_top_songs))
                        // Shrink-only: five fixed squares plus the container's 24.dp of padding
                        // can outgrow a narrow cell (5 × 58 = 290.dp against a 250.dp widget),
                        // which used to push the fifth tile past the edge. Growing stays the
                        // spacers' job, so wide cells keep square tiles with wider gaps.
                        val songSide = minOf(SONG_TILE_SIZE, (LocalSize.current.width - 36.dp) / 5)
                        // Weighted spacers between fixed-size tiles, never weighted tiles: in
                        // Glance defaultWeight() takes its width from the row while the height
                        // stays as declared, which turns a square cover into a rectangle.
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            topSongs.forEachIndexed { index, tile ->
                                if (index > 0) Spacer(GlanceModifier.defaultWeight())
                                Box(contentAlignment = Alignment.BottomStart) {
                                    Cover(tile.artwork, tile.title, songSide, 8.dp)
                                    Text(
                                        text = "${index + 1}",
                                        style =
                                            TextStyle(
                                                color = ColorProvider(Color.White),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        modifier =
                                            GlanceModifier
                                                .padding(3.dp)
                                                .background(ImageProvider(R.drawable.widget_rank_badge))
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (topArtists.isNotEmpty()) {
                        SectionLabel(context.getString(R.string.widget_insights_top_artists))
                        val artistSide = minOf(ARTIST_TILE_SIZE, (LocalSize.current.width - 36.dp) / 5)
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            topArtists.forEachIndexed { index, tile ->
                                if (index > 0) Spacer(GlanceModifier.defaultWeight())
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Half the side rounds the square into a circle. Glance only
                                    // applies cornerRadius from API 31; below that it stays square,
                                    // which is the same trade-off the other widgets already make.
                                    Cover(
                                        artwork = tile.artwork,
                                        description = tile.name,
                                        side = artistSide,
                                        radius = artistSide / 2,
                                        fallbackInitial = tile.name.firstOrNull()?.uppercase(),
                                    )
                                    Spacer(GlanceModifier.height(3.dp))
                                    Text(
                                        text = tile.name,
                                        maxLines = 1,
                                        style =
                                            TextStyle(
                                                color = ColorProvider(mutedOnPalette),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                            ),
                                        modifier = GlanceModifier.width(artistSide + 14.dp),
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    suspend fun refresh(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@ListeningInsightsWidget.javaClass).forEach { glanceId ->
            this@ListeningInsightsWidget.update(context, glanceId)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun SectionLabel(text: String) {
    Spacer(GlanceModifier.height(34.dp))
    Text(
        text = text,
        style =
            TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        modifier = GlanceModifier.padding(bottom = 7.dp),
    )
}

/**
 * One square tile. Falls back to a tinted plate — carrying the initial when one is given — since
 * an artist the user has never opened has no cached image to show.
 */
@SuppressLint("RestrictedApi")
@Composable
private fun Cover(
    artwork: Bitmap?,
    description: String,
    side: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
    fallbackInitial: String? = null,
) {
    if (artwork != null) {
        Image(
            provider = ImageProvider(artwork),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(side).cornerRadius(radius),
        )
    } else {
        Box(
            modifier =
                GlanceModifier
                    .size(side)
                    .background(ImageProvider(R.drawable.widget_shortcut_tile))
                    .cornerRadius(radius),
            contentAlignment = Alignment.Center,
        ) {
            if (fallbackInitial != null) {
                Text(
                    text = fallbackInitial,
                    style =
                        TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun RowScope.StatTile(
    value: String,
    label: String,
) {
    Column(
        modifier =
            GlanceModifier
                .defaultWeight()
                .background(ImageProvider(R.drawable.widget_shortcut_tile))
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style =
                TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(mutedOnPalette), fontSize = 10.sp),
        )
    }
}

/** Seconds into "14h 22m" / "22m" — hours are dropped entirely below one hour. */
private fun formatListeningTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
