package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.simpmusic.extension.formatTimeAgo
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NotificationViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.ic_rss_feed_24
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.no_notification
import simpmusic.composeapp.generated.resources.notification
import simpmusic.composeapp.generated.resources.singles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = koinViewModel(),
) {
    val listNotification by viewModel.listNotification.collectAsStateWithLifecycle()
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.notification),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                RippleIconButton(resId = Res.drawable.baseline_arrow_back_ios_new_24) {
                    navController.navigateUp()
                }
            },
        )
        Crossfade(targetState = listNotification) {
            if (it == null) {
                Box(
                    Modifier.fillMaxSize(),
                ) {
                    CenterLoadingBox(modifier = Modifier.align(Alignment.Center))
                }
            } else if (it.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(it) { notification ->
                        NotificationItem(
                            notification = notification,
                            navController,
                        )
                    }
                    item {
                        EndOfPage()
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(Res.string.no_notification),
                        style = typo().titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    navController: NavController,
) {
    if (notification.type == NotificationEntity.TYPE_BLOG) {
        BlogNotificationItem(notification)
        return
    }
    Box(
        modifier =
            Modifier
                .padding(5.dp)
                .fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.clickable {
                    navController.navigate(
                        ArtistDestination(
                            channelId = notification.channelId,
                        ),
                    )
                },
            ) {
                val thumb = notification.thumbnail
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(thumb)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumb)
                            .crossfade(true)
                            .build(),
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .size(50.dp)
                            .clip(
                                CircleShape,
                            ),
                )
                Spacer(modifier = Modifier.padding(5.dp))
                Column {
                    Text(text = stringResource(Res.string.new_release), style = typo().titleSmall)
                    Spacer(modifier = Modifier.padding(3.dp))
                    Text(text = notification.name, style = typo().headlineMedium)
                }
            }
            LazyRow(
                Modifier.padding(top = 15.dp),
            ) {
                items(notification.single) { single ->
                    ItemAlbumNotification(
                        isAlbum = false,
                        browseId = single["browseId"] ?: "",
                        title = single["title"] ?: "",
                        thumbnail = single["thumbnails"],
                        navController,
                    )
                }
                items(notification.album) { album ->
                    ItemAlbumNotification(
                        isAlbum = true,
                        browseId = album["browseId"] ?: "",
                        title = album["title"] ?: "",
                        thumbnail = album["thumbnails"],
                        navController = navController,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = notification.time.formatTimeAgo(),
            style = typo().titleSmall,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 15.dp),
        )
    }
}

@Composable
fun BlogNotificationItem(notification: NotificationEntity) {
    val uriHandler = LocalUriHandler.current
    val link = notification.link
    Box(
        modifier =
            Modifier
                .padding(5.dp)
                .fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !link.isNullOrEmpty()) {
                    link?.let { uriHandler.openUri(it) }
                },
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Top)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rss_feed_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.padding(end = 56.dp)) {
                Text(text = "New blog post", style = typo().titleSmall)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = notification.name, style = typo().titleMedium)
                notification.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = typo().bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = notification.time.formatTimeAgo(),
            style = typo().titleSmall,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 15.dp),
        )
    }
}

@Composable
fun ItemAlbumNotification(
    isAlbum: Boolean,
    browseId: String,
    title: String,
    thumbnail: String?,
    navController: NavController,
) {
    Box(
        modifier =
            Modifier
                .clickable {
                    navController.navigate(
                        AlbumDestination(
                            browseId = browseId,
                        ),
                    )
                },
    ) {
        Column(
            Modifier.padding(5.dp),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumbnail)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumbnail)
                        .crossfade(true)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(150.dp)
                        .clip(
                            RoundedCornerShape(10),
                        ),
            )
            Text(
                text = title,
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(150.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 10.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Text(
                text = if (isAlbum) stringResource(Res.string.album) else stringResource(Res.string.singles),
                style = typo().bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(150.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 10.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        }
    }
}