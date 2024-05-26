package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.wear.compose.material3.ripple
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import java.time.format.DateTimeFormatter

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun LocalPlaylistHeader(
    id: Long,
    sharedViewModel: SharedViewModel,
    viewModel: LocalPlaylistViewModel,
) {
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by remember { mutableStateOf(false) }
//    val hazeState = remember { HazeState() }
    val localPlaylist by viewModel.localPlaylist.collectAsState(initial = null)
    val listTrack by viewModel.listTrack.collectAsState(initial = null)
    LaunchedEffect(key1 = id) {
        viewModel.getLocalPlaylist(id)
    }
    LaunchedEffect(key1 = localPlaylist) {
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    Box {
//        AnimatedVisibility(
//            visible = shouldHideTopBar,
//            enter = fadeIn() + slideInVertically(),
//            exit = fadeOut() + slideOutVertically(),
//            modifier = Modifier.wrapContentHeight().fillMaxWidth().align(Alignment.TopCenter),
//        ) {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = localPlaylist?.title ?: stringResource(id = R.string.playlist),
//                    )
//                },
//                navigationIcon = {
//                    RippleIconButton(
//                        R.drawable.baseline_arrow_back_ios_new_24,
//                        Modifier
//                            .size(32.dp),
//                    ) {
//                    }
//                },
//            )
//        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            state = lazyState,
        ) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(Color.Transparent),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                    ) {
                        CoilImage(
                            imageModel = {
                                localPlaylist?.thumbnail
                            },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    CrossfadePlugin(
                                        duration = 550,
                                    )
                                },
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(8.dp),
                                    ),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        brush =
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0x75000000),
                                                    Color.Black,
                                                ),
                                            ),
                                    ),
                        )
                    }
                    Column(
                        Modifier
                            .background(Color.Transparent),
//                            .hazeChild(hazeState, style = HazeMaterials.thin()),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp),
                        ) {
                            RippleIconButton(
                                resId = R.drawable.baseline_arrow_back_ios_new_24,
                            ) {
                                TODO("Navigate back")
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                        ) {
                            CoilImage(
                                imageModel = {
                                    localPlaylist?.thumbnail
                                },
                                imageOptions =
                                    ImageOptions(
                                        contentScale = ContentScale.FillHeight,
                                        alignment = Alignment.Center,
                                    ),
                                previewPlaceholder = painterResource(id = R.drawable.holder),
                                component =
                                    rememberImageComponent {
                                        CrossfadePlugin(
                                            duration = 550,
                                        )
                                    },
                                modifier =
                                    Modifier
                                        .height(250.dp)
                                        .wrapContentWidth()
                                        .align(Alignment.CenterHorizontally)
                                        .clip(
                                            RoundedCornerShape(8.dp),
                                        ),
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                            ) {
                                Column(Modifier.padding(horizontal = 32.dp)) {
                                    Spacer(modifier = Modifier.size(25.dp))
                                    Text(
                                        text = localPlaylist?.title ?: "",
                                        style = typo.titleLarge,
                                        color = Color.White,
                                    )
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.your_playlist),
                                            style = typo.bodyLarge,
                                            color = Color.White,
                                            modifier =
                                                Modifier.clickable(
                                                    interactionSource =
                                                        remember {
                                                            MutableInteractionSource()
                                                        },
                                                    indication = ripple(),
                                                    onClick = { },
                                                ),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    id = R.string.created_at,
                                                    localPlaylist?.inLibrary?.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "kk:mm - dd MMM uuuu",
                                                        ),
                                                    ) ?: "",
                                                ),
                                            style = typo.bodyLarge,
                                            color = Color(0xC4FFFFFF),
                                        )
                                    }
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RippleIconButton(
                                            resId = R.drawable.baseline_play_circle_24,
                                            fillMaxSize = true,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                        }
                                        Spacer(modifier = Modifier.size(5.dp))
                                        HeartCheckBox(
                                            checked = false,
                                            onStateChange = { },
                                            size = 36,
                                        )
                                        Spacer(modifier = Modifier.size(5.dp))
                                        RippleIconButton(
                                            fillMaxSize = true,
                                            resId = R.drawable.download_button,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                        }
                                        Spacer(Modifier.weight(1f))
                                        RippleIconButton(
                                            modifier =
                                                Modifier.size(36.dp),
                                            resId = R.drawable.baseline_shuffle_24,
                                            fillMaxSize = true,
                                        ) {
                                        }
                                        Spacer(Modifier.size(5.dp))
                                        RippleIconButton(
                                            modifier =
                                                Modifier.size(36.dp),
                                            resId = R.drawable.baseline_more_vert_24,
                                            fillMaxSize = true,
                                        ) {
                                        }
                                    }
                                    // Hide in local playlist
                                    //                                ExpandableText(
                                    //                                    modifier = Modifier.padding(vertical = 8.dp),
                                    //                                    text = stringResource(id = R.string.demo_description),
                                    //                                    fontSize = typo.bodyLarge.fontSize,
                                    //                                    showMoreStyle = SpanStyle(Color.Gray),
                                    //                                    showLessStyle = SpanStyle(Color.Gray),
                                    //                                    style = TextStyle(
                                    //                                        color = Color(0xC4FFFFFF)
                                    //                                    )
                                    //                                )
                                    Text(
                                        text =
                                            stringResource(
                                                id = R.string.album_length,
                                                (localPlaylist?.tracks?.size ?: 0).toString(),
                                                "",
                                            ),
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                    //
                                }
                            }
                        }
                    }
                }
            }
            items(listTrack ?: listOf()) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaylistItems(
                        isPlaying = false,
                        songEntity = item,
                    )
                }
            }
            item {
                EndOfPage()
            }
        }
    }
}