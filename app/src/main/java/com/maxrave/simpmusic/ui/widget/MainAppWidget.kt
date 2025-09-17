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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.wrapContentHeight
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
import com.maxrave.common.R
import com.maxrave.simpmusic.ui.MainActivity
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainAppWidget :
    GlanceAppWidget(),
    KoinComponent {
    val sharedViewModel by inject<SharedViewModel>()

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val scope = rememberCoroutineScope()

            val controllerState by sharedViewModel.controllerState.collectAsState()
            val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

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
                        is SuccessResult -> result.image.toBitmap()
                        else -> null
                    }
            }

            Box(
                GlanceModifier.fillMaxSize().background(Color.Black).clickable {
                    actionStartActivity(MainActivity::class.java)
                },
                contentAlignment = Alignment.TopCenter,
            ) {
                randomImage?.let {
                    Image(
                        provider = ImageProvider(it),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        modifier =
                            GlanceModifier
                                .fillMaxSize(),
                    )
                } ?: Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    Column(
                        GlanceModifier.fillMaxWidth().defaultWeight().padding(
                            start = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp,
                            end = 8.dp,
                        ),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = title,
                            style =
                                TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = typo.headlineMedium.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start,
                                ),
                            maxLines = 1,
                            modifier =
                                GlanceModifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                        )
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Text(
                            text = artist,
                            style =
                                TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = typo.bodyMedium.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start,
                                ),
                            maxLines = 1,
                            modifier =
                                GlanceModifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                        )
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