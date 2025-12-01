package com.maxrave.simpmusic.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.components.CircleIconButton
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
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.transparent
import com.maxrave.simpmusic.ui.theme.white
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class MainAppWidget :
    GlanceAppWidget(),
    KoinComponent {
    val sharedViewModel by inject<SharedViewModel>()
    val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        serviceScope.launch {
            val controllerJob =
                launch {
                    sharedViewModel.controllerState.collectLatest {
                        updateWidget(context)
                    }
                }
            val nowPlayingJob =
                launch {
                    sharedViewModel.nowPlayingScreenData.collectLatest {
                        updateWidget(context)
                    }
                }
            controllerJob.join()
            nowPlayingJob.join()
        }

        provideContent {
            GlanceTheme {
                val scope = rememberCoroutineScope()

                val controllerState by sharedViewModel.controllerState.collectAsState()
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

                val paletteState = rememberPaletteState()

                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                var bgColor by remember {
                    mutableStateOf(md_theme_dark_background)
                }

                val thumbUrl by remember {
                    derivedStateOf {
                        screenDataState.thumbnailURL
                    }
                }

                val title by remember {
                    derivedStateOf {
                        screenDataState.nowPlayingTitle
                    }
                }

                val artist by remember {
                    derivedStateOf {
                        screenDataState.artistName
                    }
                }

                var randomImage by remember(thumbUrl) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(bgColor) {
                    updateWidget(context)
                }

                LaunchedEffect(bitmap) {
                    val bm = bitmap?.asImageBitmap()
                    if (bm != null) {
                        paletteState.generate(bm)
                    }
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { paletteState.palette }
                        .distinctUntilChanged()
                        .collectLatest {
                            bgColor = it.getColorFromPalette()
                        }
                }

                LaunchedEffect(thumbUrl) {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(thumbUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbUrl + "BIGGER")
                            .crossfade(true)
                            .placeholder(R.drawable.holder)
                            .error(R.drawable.holder)
                            .allowHardware(false)
                            .build()
                    val result = ImageLoader(context).execute(request)
                    randomImage =
                        when (result) {
                            is SuccessResult ->
                                result.image.toBitmap().also {
                                    bitmap = it
                                }

                            else -> null
                        }
                }

                Box(
                    GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(bgColor))
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            GlanceModifier
                                .fillMaxWidth(),
                    ) {
                        Spacer(
                            modifier =
                                GlanceModifier
                                    .width(16.dp)
                                    .height(1.dp),
                        )
                        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(96.dp)) {
                            randomImage?.let {
                                Image(
                                    provider = ImageProvider(it),
                                    contentDescription = "",
                                    contentScale = ContentScale.FillBounds,
                                    modifier =
                                        GlanceModifier
                                            .fillMaxSize()
                                            .cornerRadius(12.dp),
                                )
                            } ?: CircularProgressIndicator(
                                GlanceModifier.size(24.dp),
                                color = ColorProvider(white),
                            )
                        }
                        Column(
                            GlanceModifier.fillMaxWidth().defaultWeight().padding(
                                start = 16.dp,
                                end = 8.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                style =
                                    TextStyle(
                                        color = ColorProvider(white),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                    ),
                                maxLines = 1,
                                modifier =
                                    GlanceModifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                            )
                            Text(
                                text = artist,
                                style =
                                    TextStyle(
                                        color = ColorProvider(Color.LightGray),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                    ),
                                maxLines = 1,
                                modifier =
                                    GlanceModifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                            )
                            Spacer(GlanceModifier.size(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier =
                                    GlanceModifier
                                        .wrapContentSize()
                                        .fillMaxWidth(),
                            ) {
                                CircleIconButton(
                                    modifier =
                                        GlanceModifier
                                            .background(transparent)
                                            .size(32.dp),
                                    imageProvider = ImageProvider(R.drawable.baseline_shuffle_24),
                                    contentDescription = "Shuffle",
                                    contentColor = if (controllerState.isShuffle) ColorProvider(seed) else ColorProvider(white),
                                    backgroundColor = ColorProvider(transparent),
                                    onClick = {
                                        sharedViewModel.onUIEvent(UIEvent.Shuffle)
                                    },
                                )
                                Spacer(GlanceModifier.size(8.dp))
                                CircleIconButton(
                                    modifier =
                                        GlanceModifier
                                            .size(32.dp),
                                    imageProvider = ImageProvider(R.drawable.baseline_skip_previous_24),
                                    contentDescription = "Previous",
                                    contentColor = ColorProvider(if (controllerState.isPreviousAvailable) white else Color.Gray),
                                    backgroundColor = ColorProvider(transparent),
                                    enabled = controllerState.isPreviousAvailable,
                                    onClick = {
                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                    },
                                )
                                Spacer(GlanceModifier.size(8.dp))
                                CircleIconButton(
                                    modifier =
                                        GlanceModifier
                                            .size(48.dp),
                                    imageProvider =
                                        if (controllerState.isPlaying) {
                                            ImageProvider(R.drawable.baseline_pause_circle_24)
                                        } else {
                                            ImageProvider(R.drawable.baseline_play_circle_24)
                                        },
                                    contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                    contentColor = ColorProvider(white),
                                    backgroundColor = ColorProvider(transparent),
                                    onClick = {
                                        sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                    },
                                )
                                Spacer(GlanceModifier.size(8.dp))
                                CircleIconButton(
                                    modifier =
                                        GlanceModifier
                                            .size(32.dp),
                                    imageProvider = ImageProvider(R.drawable.baseline_skip_next_24),
                                    contentDescription = "Next",
                                    contentColor = ColorProvider(if (controllerState.isNextAvailable) white else Color.Gray),
                                    backgroundColor = ColorProvider(transparent),
                                    enabled = controllerState.isNextAvailable,
                                    onClick = {
                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                    },
                                )
                                Spacer(GlanceModifier.size(8.dp))
                                CircleIconButton(
                                    modifier =
                                        GlanceModifier
                                            .size(32.dp),
                                    imageProvider =
                                        when (controllerState.repeatState) {
                                            RepeatState.None -> ImageProvider(R.drawable.baseline_repeat_24)
                                            RepeatState.All -> ImageProvider(R.drawable.baseline_repeat_24_enable)
                                            RepeatState.One -> ImageProvider(R.drawable.baseline_repeat_one_24)
                                        },
                                    contentDescription = "REPEAT",
                                    contentColor =
                                        if (controllerState.repeatState is RepeatState.None) {
                                            ColorProvider(white)
                                        } else {
                                            ColorProvider(seed)
                                        },
                                    backgroundColor = ColorProvider(transparent),
                                    onClick = {
                                        sharedViewModel.onUIEvent(UIEvent.Repeat)
                                    },
                                )
                            }
                        }
                        Box(
                            modifier =
                                GlanceModifier
                                    .padding(vertical = 16.dp)
                                    .padding(end = 16.dp),
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.mono),
                                contentDescription = "Logo",
                                modifier =
                                    GlanceModifier
                                        .size(40.dp),
                                contentScale = ContentScale.FillBounds,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        Logger.w("Widget", "State Changed")
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(this@MainAppWidget.javaClass)
        glanceIds.forEach { glanceId ->
            this@MainAppWidget.update(context, glanceId)
        }
    }
}