package com.maxrave.simpmusic.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/** How many covers fit across a 4-cell widget before they turn into stamps. */
private const val RECENT_TILE_COUNT = 5

/**
 * Every band wraps its own content instead of filling what is left. A `fillMaxSize` on the strip
 * made it swallow the whole remaining height, which is what left that block of dead colour under
 * the covers when the launcher handed the widget more room than the content needed.
 */

/** Covers are square, so this is both width and height — a weight would stretch them sideways. */
private val PLAYLIST_TILE_SIZE = 64.dp

/** Decode sizes in pixels — see the note on [loadBitmap] for why these are capped. */
private const val PLAYER_DECODE_PX = 512
private const val TILE_DECODE_PX = 256

/**
 * A 4x2 widget: a now-playing row above a strip of recently added covers.
 *
 * The strip shows real artwork rather than an icon per shortcut. A cover is recognised at a
 * glance where a generic icon plus a label has to be read — and those labels were what made the
 * first version cramped, since every tile had to be wide enough to fit its text.
 *
 * Tiles open `simpmusic://…` rather than passing extras: a widget tap often arrives with the app
 * not running, and a deep link is handled the same on a cold start as on a warm one.
 */
class PlaylistsWidget :
    GlanceAppWidget(),
    KoinComponent {
    // Exact, not the default Single: with Single the composition is laid out against the
    // XML min sizes and the launcher then squeezes that rendering into whatever cells it
    // actually granted — non-uniformly on grids whose cells have a different aspect, which
    // is how every square in these widgets came out a rectangle on some launchers. Exact
    // re-composes per granted size, so the layout works with the truth.
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()
    private val commonRepository by inject<CommonRepository>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        serviceScope.launch {
            val controllerJob =
                launch {
                    sharedViewModel.controllerState.collectLatest { updateWidget(context) }
                }
            val nowPlayingJob =
                launch {
                    sharedViewModel.nowPlayingScreenData.collectLatest { updateWidget(context) }
                }
            controllerJob.join()
            nowPlayingJob.join()
        }

        provideContent {
            GlanceTheme {
                val controllerState by sharedViewModel.controllerState.collectAsState()
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

                val paletteState = rememberPaletteState()
                var artwork by remember { mutableStateOf<Bitmap?>(null) }
                var bgColor by remember { mutableStateOf(Color.Black) }
                var tiles by remember { mutableStateOf<List<PlaylistTile>>(emptyList()) }

                val thumbUrl by remember { derivedStateOf { screenDataState.thumbnailURL } }

                LaunchedEffect(bgColor) { updateWidget(context) }

                LaunchedEffect(artwork) {
                    artwork?.asImageBitmap()?.let { paletteState.generate(it) }
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { paletteState.palette }
                        .distinctUntilChanged()
                        .collectLatest { bgColor = it.getColorFromPalette() }
                }

                LaunchedEffect(thumbUrl) {
                    artwork = context.loadBitmap(thumbUrl, thumbUrl + "BIGGER", PLAYER_DECODE_PX)
                }

                LaunchedEffect(Unit) {
                    tiles =
                        commonRepository
                            .getAllRecentData()
                            .firstOrNull()
                            .orEmpty()
                            .take(RECENT_TILE_COUNT)
                            .mapNotNull { it.toTile(context) }
                }

                // The outer column carries the strip's colour so that any height the launcher
                // hands over beyond what the two bands need reads as more strip, rather than as
                // a hole showing the home screen through it.
                Column(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(bgColor.darkened()))
                            // The whole widget opens the app. Only the artwork and the five
                            // covers used to be tappable, so a press on the title, on the label
                            // or on any gap between them fell straight through to the launcher.
                            .clickable(
                                actionStartActivity<MainActivity>(),
                                rippleOverride = R.drawable.no_ripple,
                            ),
                ) {
                    // Two bands rather than one surface: the strip sits on a darker shade of the
                    // same palette colour, which is what separates it from the player above
                    // without needing a divider line.
                    // The player takes the leftover height; the strip below hugs its covers.
                    // A widget always fills the cell the launcher gives it, so the spare space
                    // has to land somewhere — here it goes into the artwork and the controls.
                    Row(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                                .background(ColorProvider(bgColor))
                                .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                GlanceModifier
                                    .size(88.dp)
                                    .clickable(
                                        actionStartActivity<MainActivity>(),
                                        rippleOverride = R.drawable.no_ripple,
                                    ),
                        ) {
                            artwork?.let {
                                Image(
                                    provider = ImageProvider(it),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp),
                                )
                            }
                        }
                        Column(modifier = GlanceModifier.defaultWeight().padding(start = 14.dp)) {
                            Text(
                                text = screenDataState.nowPlayingTitle,
                                maxLines = 1,
                                style =
                                    TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                            )
                            Text(
                                text = screenDataState.artistName,
                                maxLines = 1,
                                style =
                                    TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 12.sp,
                                    ),
                            )
                            Spacer(GlanceModifier.height(8.dp))
                            // Bare glyphs, no discs: at this size a filled circle per button
                            // dominates the row, and Spotify's own widget leaves them plain too.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(R.drawable.rounded_skip_previous_24),
                                    contentDescription = "Previous",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier =
                                        GlanceModifier
                                            .size(26.dp)
                                            .clickable(rippleOverride = R.drawable.no_ripple) {
                                                sharedViewModel.onUIEvent(UIEvent.Previous)
                                            },
                                )
                                Spacer(GlanceModifier.width(16.dp))
                                Image(
                                    provider =
                                        if (controllerState.isPlaying) {
                                            ImageProvider(R.drawable.rounded_pause_24)
                                        } else {
                                            ImageProvider(R.drawable.rounded_play_arrow_24)
                                        },
                                    contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier =
                                        GlanceModifier
                                            .size(32.dp)
                                            .clickable(rippleOverride = R.drawable.no_ripple) {
                                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                            },
                                )
                                Spacer(GlanceModifier.width(16.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.rounded_skip_next_24),
                                    contentDescription = "Next",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier =
                                        GlanceModifier
                                            .size(26.dp)
                                            .clickable(rippleOverride = R.drawable.no_ripple) {
                                                sharedViewModel.onUIEvent(UIEvent.Next)
                                            },
                                )
                            }
                        }
                    }

                    // Takes whatever height is left rather than hugging its content: a widget
                    // always fills the cell the launcher gives it, so the spare height has to go
                    // somewhere. Absorbed here and centred, it reads as a roomier strip instead
                    // of a dead band under the covers.
                    // Both bands carry defaultWeight(), so the cell splits evenly between them
                    // instead of the player swallowing every pixel the launcher hands over.
                    // Glance has no weighted ratio — defaultWeight() is always 1 — so an even
                    // split is the whole vocabulary here.
                    Column(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                                .background(ColorProvider(bgColor.darkened()))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = context.getString(R.string.widget_recently_added),
                            style =
                                TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            modifier = GlanceModifier.padding(bottom = 6.dp),
                        )
                        // The gaps stretch, not the tiles. Glance has no aspectRatio, so a tile
                        // given defaultWeight() takes its width from the row and keeps its fixed
                        // height — which is how these came out rectangular before. Weighted
                        // spacers spread the row instead, leaving every cover square at any width.
                        // Shrink-only, same arithmetic as the insights rows: five 64.dp
                        // squares need 320.dp, more than the 250.dp a 4-cell widget may get.
                        val tileSide = minOf(PLAYLIST_TILE_SIZE, (LocalSize.current.width - 36.dp) / 5)
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            tiles.forEachIndexed { index, tile ->
                                if (index > 0) Spacer(GlanceModifier.defaultWeight())
                                PlaylistCover(context, tile, tileSide) { song ->
                                    sharedViewModel.loadMediaItemFromTrack(
                                        song.toTrack(),
                                        Config.SONG_CLICK,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@PlaylistsWidget.javaClass).forEach { glanceId ->
            this@PlaylistsWidget.update(context, glanceId)
        }
    }
}

private data class PlaylistTile(
    val title: String,
    val cover: Bitmap?,
    val uri: String,
    /**
     * Set only for a song, and when set the tile plays it instead of opening the app.
     *
     * A widget runs inside the app's own process, so it can reach SharedViewModel directly — the
     * same way the transport buttons above already do. Routing a song through a deep link meant
     * launching MainActivity, parsing a URI and navigating, only to end up calling this. The
     * other three types genuinely need a screen, so they keep the deep link.
     */
    val song: SongEntity? = null,
)

/**
 * Maps one entry of the recently-added list onto a tile.
 *
 * Every branch reads a different field for the same thing — songs keep artwork in `thumbnails`
 * as a plain URL, albums and playlists carry their own, and the deep link differs per type, so
 * a tap lands on the album page rather than dumping the user in the library.
 */
private suspend fun RecentlyType.toTile(context: Context): PlaylistTile? =
    when (objectType()) {
        RecentlyType.Type.SONG ->
            (this as? SongEntity)?.let {
                PlaylistTile(
                    title = it.title,
                    cover = context.loadBitmap(it.thumbnails, "RS${it.videoId}", TILE_DECODE_PX),
                    uri = "simpmusic://watch?v=${it.videoId}",
                    song = it,
                )
            }

        RecentlyType.Type.ALBUM ->
            (this as? AlbumEntity)?.let {
                PlaylistTile(
                    title = it.title,
                    cover = context.loadBitmap(it.thumbnails, "RA${it.browseId}", TILE_DECODE_PX),
                    uri = "simpmusic://album?id=${it.browseId}",
                )
            }

        RecentlyType.Type.PLAYLIST ->
            (this as? PlaylistEntity)?.let {
                PlaylistTile(
                    title = it.title,
                    cover = context.loadBitmap(it.thumbnails, "RP${it.id}", TILE_DECODE_PX),
                    uri = "simpmusic://playlist?list=${it.id}",
                )
            }

        RecentlyType.Type.ARTIST ->
            (this as? ArtistEntity)?.let {
                PlaylistTile(
                    title = it.name,
                    cover = context.loadBitmap(it.thumbnails, "RAr${it.channelId}", TILE_DECODE_PX),
                    uri = "simpmusic://channel/${it.channelId}",
                )
            }
    }

@SuppressLint("RestrictedApi")
@Composable
private fun PlaylistCover(
    context: Context,
    tile: PlaylistTile,
    side: androidx.compose.ui.unit.Dp,
    onPlay: (SongEntity) -> Unit,
) {
    val song = tile.song
    Box(
        modifier =
            GlanceModifier
                .size(side)
                .background(ImageProvider(R.drawable.widget_shortcut_tile))
                .let { base ->
                    if (song != null) {
                        base.clickable(rippleOverride = R.drawable.no_ripple) { onPlay(song) }
                    } else {
                        base.clickable(
                            actionStartActivity(context.openAppIntent(tile.uri)),
                            rippleOverride = R.drawable.no_ripple,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        tile.cover?.let {
            Image(
                provider = ImageProvider(it),
                contentDescription = tile.title,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
            )
        } ?: Image(
            provider = ImageProvider(R.drawable.rounded_playlist_play_24),
            contentDescription = tile.title,
            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

/**
 * Intent for opening the app from the widget, optionally on a deep link.
 *
 * FLAG_ACTIVITY_NEW_TASK is not optional here: the widget fires this from a non-Activity
 * context, and without the flag Android drops the start silently — the tap simply does nothing.
 */
private fun Context.openAppIntent(uri: String): Intent =
    Intent(Intent.ACTION_VIEW, uri.toUri())
        .setClass(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

/**
 * A step darker, for the band the covers sit on.
 *
 * Scales the channels rather than blending towards black so the hue survives — the strip has to
 * read as the same colour family as the player above it, only deeper.
 */
private fun Color.darkened(factor: Float = 0.72f): Color =
    Color(red = red * factor, green = green * factor, blue = blue * factor, alpha = alpha)

/** Decodes a cover for the widget; null when there is no URL or the fetch fails. */
internal suspend fun Context.loadBitmap(
    url: String?,
    cacheKey: String,
    sizePx: Int? = null,
): Bitmap? {
    if (url.isNullOrBlank()) return null
    val request =
        ImageRequest
            .Builder(this)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCacheKey(cacheKey)
            .allowHardware(false)
            // Decoding to the size it is drawn at is not an optimisation, it is what keeps the
            // widget on screen at all. Every bitmap is copied into the RemoteViews and the system
            // caps that at ~20 MB per update; a YouTube cover arrives around 1145 px, which is
            // 5.2 MB each, so a handful of them blows the cap and the launcher shows its
            // "Can't show content" placeholder instead — with the real cause only in logcat.
            .apply { if (sizePx != null) size(sizePx, sizePx) }
            .build()
    return (ImageLoader(this).execute(request) as? SuccessResult)?.image?.toBitmap()
}
