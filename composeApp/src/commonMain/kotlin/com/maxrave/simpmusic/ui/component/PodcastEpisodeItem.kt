package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_more_vert_24
import simpmusic.composeapp.generated.resources.holder

@Composable
fun PodcastEpisodeFullWidthItem(
    modifier: Modifier = Modifier,
    episode: PodcastBrowse.EpisodeItem,
    onClick: (String) -> Unit,
    onMoreClickListener: (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .clickable { onClick(episode.videoId) }
                .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.size(50.dp)) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(episode.thumbnail.lastOrNull()?.url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(episode.thumbnail.lastOrNull()?.url)
                            .crossfade(true)
                            .build(),
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = episode.title,
                    style = typo().labelMedium,
                    maxLines = 1,
                    color = Color.White,
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
                    text = "${episode.createdDay ?: ""}${if (!episode.durationString.isNullOrEmpty()) " • ${episode.durationString}" else ""}",
                    style = typo().bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                val description = episode.description
                if (description != null) {
                    Text(
                        text = description,
                        style = typo().bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
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

            if (onMoreClickListener != null) {
                RippleIconButton(resId = Res.drawable.baseline_more_vert_24, fillMaxSize = false) {
                    onMoreClickListener.invoke()
                }
            }
        }
    }
}