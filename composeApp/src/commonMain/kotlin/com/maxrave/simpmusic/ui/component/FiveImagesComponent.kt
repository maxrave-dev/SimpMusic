package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun FiveImagesComponent(
    modifier: Modifier,
    images: List<ImageData>,
) {
    if (images.isEmpty()) {
        return
    }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clickable {
                    images.first().onClick()
                },
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(images.first().imageUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(images.first().imageUrl)
                        .crossfade(550)
                        .build(),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(
                                            alpha = 0.4f,
                                        ),
                                        Color.Black,
                                    ),
                            ),
                    ),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = images.first().title,
                    style = typo().labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Text(
                    text = images.first().subtitle,
                    style = typo().bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                images.first().thirdTitle?.let {
                    Text(
                        text = it,
                        style = typo().bodySmall,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                }
            }
        }
        if (images.size < 3) {
            return@Column
        }
        Row(Modifier.fillMaxWidth()) {
            images.subList(1, 3).forEach { image ->
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .weight(1f)
                        .clickable {
                            image.onClick()
                        },
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(image.imageUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(image.imageUrl)
                                .crossfade(550)
                                .build(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(
                                                    alpha = 0.4f,
                                                ),
                                                Color.Black,
                                            ),
                                    ),
                            ),
                    )
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = image.title,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                        Text(
                            text = image.subtitle,
                            style = typo().bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                        image.thirdTitle?.let {
                            Text(
                                text = it,
                                style = typo().bodySmall,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }
                }
            }
        }
        if (images.size < 5) {
            return@Column
        }
        Row {
            images.subList(3, 5).forEach { image ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .weight(1f)
                        .clickable {
                            image.onClick()
                        },
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(image.imageUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(image.imageUrl)
                                .crossfade(550)
                                .build(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(
                                                    alpha = 0.4f,
                                                ),
                                                Color.Black,
                                            ),
                                    ),
                            ),
                    )
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = image.title,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                        Text(
                            text = image.subtitle,
                            style = typo().bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                        image.thirdTitle?.let {
                            Text(
                                text = it,
                                style = typo().bodySmall,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ImageData(
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val thirdTitle: String? = null,
    val onClick: () -> Unit,
)