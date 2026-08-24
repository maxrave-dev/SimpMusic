@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.ui.component.DraggableItem
import com.maxrave.simpmusic.ui.component.QueueItemBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.rememberDragDropState
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.continue_playing
import simpmusic.composeapp.generated.resources.continue_playing_endless_subtitle
import simpmusic.composeapp.generated.resources.endless_queue

/**
 * The QUEUE body: compact header, [Info][PlaylistAdd][Shuffle][Repeat] pills, a "Continue
 * Playing" section (with the endless-queue switch) and the upcoming tracks — the OLD queue
 * sheet's own [SongFullWidthItems] rows with its long-press-drag reorder and its per-item ⋯
 * sheet ([QueueItemBottomSheet]: move up/down/delete), matching
 * [com.maxrave.simpmusic.ui.component.QueueBottomSheet] exactly.
 * Ends in the same fixed bottom cluster as MAIN/LYRICS.
 */
@Composable
internal fun AppleMusicQueueView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    modifier: Modifier = Modifier,
    dataStoreManager: DataStoreManager = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
) {
    val localDensity = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Upcoming tracks live in the SAME index space as musicServiceHandler.swap(from, to) and
    // removeMediaItem(index): artworkQueue == queueData.data.listTracks, and both operations
    // read/write that list (and the player timeline) at the SAME position — confirmed by reading
    // MediaServiceHandlerImpl.removeMediaItem/swap and ExoPlayerAdapter.moveMediaItem/
    // removeMediaItem/getUnshuffledIndex, which all treat their index argument as "current
    // shuffle/display order", i.e. exactly artworkQueue's own order. `offset` converts a position
    // within this UPCOMING-ONLY sublist back to that absolute space.
    val offset = state.currentOrderIndex + 1
    val upcoming =
        remember(state.artworkQueue, state.currentOrderIndex) {
            if (state.currentOrderIndex < 0) emptyList() else state.artworkQueue.drop(offset)
        }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }))
        AppleMusicCompactHeader(state = state, actions = actions, typography = typography)
        AppleMusicQueuePillsRow(
            state = state,
            actions = actions,
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        AppleMusicContinuePlayingHeader(
            dataStoreManager = dataStoreManager,
            typography = typography,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val lazyListState = rememberLazyListState()
        // rememberDragDropState stores this lambda in remember(lazyListState), so it is frozen at
        // FIRST composition — capturing `offset` directly would keep using the offset from the
        // track that was playing back then and swap the wrong two rows after any track change.
        val currentOffset by rememberUpdatedState(offset)
        val dragDropState =
            rememberDragDropState(lazyListState) { from, to ->
                actions.onMoveQueueItem(from + currentOffset, to + currentOffset)
            }

        // Endless/radio queues page in as you scroll — same trigger QueueBottomSheet uses.
        val loadMoreState by remember {
            derivedStateOf { musicServiceHandler.queueData.value?.queueState ?: QueueData.StateSource.STATE_CREATED }
        }
        val shouldLoadMore by remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3 && layoutInfo.totalItemsCount > 0
            }
        }
        LaunchedEffect(Unit) {
            snapshotFlow { shouldLoadMore }
                .collect { if (it && loadMoreState == QueueData.StateSource.STATE_INITIALIZED) musicServiceHandler.loadMore() }
        }
        var overscrollJob by remember { mutableStateOf<Job?>(null) }

        // Same per-item sheet the old queue sheet opens from a row's ⋯ (move up/down/delete).
        var queueItemSheetIndex by remember { mutableStateOf(-1) }
        if (queueItemSheetIndex >= 0) {
            QueueItemBottomSheet(
                onDismiss = { queueItemSheetIndex = -1 },
                index = queueItemSheetIndex,
            )
        }

        Box(modifier = Modifier.weight(1f).appleMusicVerticalFadeEdges(topFade = 24.dp, bottomFade = 48.dp)) {
            LazyColumn(
                state = lazyListState,
                // Trailing space equal to the bottom fade, so the fade lands on blank space
                // instead of dissolving the last real row.
                contentPadding = PaddingValues(bottom = 48.dp),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, dragOffset ->
                                    change.consume()
                                    dragDropState.onDrag(offset = dragOffset)
                                    if (overscrollJob?.isActive == true) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    dragDropState
                                        .checkForOverScroll()
                                        .takeIf { it != 0f }
                                        ?.let { delta ->
                                            overscrollJob =
                                                coroutineScope.launch {
                                                    dragDropState.state.animateScrollBy(
                                                        delta * 1.3f,
                                                        tween(easing = FastOutLinearInEasing),
                                                    )
                                                }
                                        } ?: run { overscrollJob?.cancel() }
                                },
                                onDragStart = { dragStartOffset -> dragDropState.onDragStart(dragStartOffset) },
                                onDragEnd = {
                                    dragDropState.onDragInterrupted(true)
                                    overscrollJob?.cancel()
                                },
                                onDragCancel = {
                                    dragDropState.onDragInterrupted()
                                    overscrollJob?.cancel()
                                },
                            )
                        },
            ) {
                itemsIndexed(
                    upcoming,
                    // Absolute index in the key: `upcoming` is a sublist, so a bare local index
                    // shifts on every track change and invalidates every row.
                    key = { i, t -> (i + offset).toString() + t.videoId },
                ) { index, track ->
                    DraggableItem(
                        dragDropState = dragDropState,
                        index = index,
                        modifier = Modifier,
                    ) { _ ->
                        // Owner's call: the OLD queue sheet's row component, verbatim — no bespoke
                        // row. Long-press-drag reorders (list-level gesture above); ⋯ opens the
                        // same per-item sheet the queue sheet uses.
                        SongFullWidthItems(
                            track = track,
                            isPlaying = false,
                            modifier = Modifier.fillMaxWidth(),
                            onClickListener = { videoId ->
                                if (videoId == track.videoId) actions.onSeekToQueueIndex(offset + index)
                            },
                            onMoreClickListener = { queueItemSheetIndex = offset + index },
                        )
                    }
                }
            }
        }

        AppleMusicBottomCluster(
            state = state,
            actions = actions,
            typography = typography,
            viewState = viewState,
            onSelectView = onSelectView,
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            deviceVolumeController = deviceVolumeController,
        )
    }
}

/** [SimpIcons.Info] [SimpIcons.PlaylistAdd] [SimpIcons.Shuffle] [SimpIcons.Repeat] — exactly, per the corrected spec. */
@Composable
private fun AppleMusicQueuePillsRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    activePillContainer: Color,
    activePillContent: Color,
    modifier: Modifier = Modifier,
) {
    val repeatState = state.controllerState.repeatState
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppleMusicQueuePill(
            icon = SimpIcons.Info,
            active = false,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onShowInfo() },
            modifier = Modifier.weight(1f),
        )
        AppleMusicQueuePill(
            icon = SimpIcons.PlaylistAdd,
            active = false,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onShowAddToPlaylist() },
            modifier = Modifier.weight(1f),
        )
        AppleMusicQueuePill(
            icon = SimpIcons.Shuffle,
            active = state.controllerState.isShuffle,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onUIEvent(UIEvent.Shuffle) },
            modifier = Modifier.weight(1f),
        )
        AppleMusicQueuePill(
            icon = if (repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
            active = repeatState !is RepeatState.None,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onUIEvent(UIEvent.Repeat) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppleMusicQueuePill(
    icon: ImageVector,
    active: Boolean,
    activeContainer: Color,
    activeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                // Gentler than the default 1.35×: a pill is 83dp wide with only a 10dp gap, so
                // the full inflate would visibly overlap its neighbour on every tap.
                .appleMusicPressInflate(pressedScale = 1.08f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (active) activeContainer else AppleMusicPillInactive)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = if (active) activeContent else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AppleMusicContinuePlayingHeader(
    dataStoreManager: DataStoreManager,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    // remember the mapped flow: a bare .map in composition builds a new Flow every recomposition
    // (FlowOperatorInvokedInComposition, promoted to an error here).
    val endlessQueueFlow =
        remember(dataStoreManager) {
            dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }
        }
    val endlessQueueEnabled by endlessQueueFlow.collectAsStateWithLifecycle(initialValue = false)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.continue_playing),
                style = typography.queueSectionHeader,
                modifier = Modifier.weight(1f),
            )
            // The switch needs its own label, exactly like the queue sheet's — unlabelled it
            // reads as a mystery toggle.
            Text(
                text = stringResource(Res.string.endless_queue),
                style = typography.queueSectionSubtitle,
                modifier = Modifier.padding(end = 8.dp),
            )
            Switch(
                checked = endlessQueueEnabled,
                onCheckedChange = { checked ->
                    coroutineScope.launch { dataStoreManager.setEndlessQueue(checked) }
                },
                modifier = Modifier.appleMusicPressInflate(pressedScale = 1.08f),
            )
        }
        AnimatedVisibility(visible = endlessQueueEnabled) {
            Text(
                text = stringResource(Res.string.continue_playing_endless_subtitle),
                style = typography.queueSectionSubtitle,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
