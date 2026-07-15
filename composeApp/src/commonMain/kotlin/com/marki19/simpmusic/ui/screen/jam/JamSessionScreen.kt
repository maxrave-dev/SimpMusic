package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.marki19.domain.jam.JamParticipant
import com.marki19.domain.jam.JamQueueItem
import com.marki19.domain.jam.JamRepeatMode
import com.marki19.domain.jam.JamSessionState
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// ─────────────────────────────────────────────────────────────────────────────
//  Jam Session Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JamSessionScreen(
    viewModel: JamViewModel,
    sharedViewModel: SharedViewModel = koinInject(),
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    onBack: () -> Unit,
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val hostTransferNotice by viewModel.hostTransferNotice.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showAddSongSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    // Navigate back if session ended
    if (sessionState == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val session = sessionState!!
    val isHost = session.isHost
    val perms = session.permissions

    // HOST_TRANSFER snackbar
    LaunchedEffect(hostTransferNotice) {
        hostTransferNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice, duration = SnackbarDuration.Short)
            viewModel.dismissHostTransferNotice()
        }
    }

    // Full queue = manual + recommendations (already merged by repository)
    val fullQueue = session.playbackState.queue
    val manualQueue = fullQueue.filter { !it.isRecommendation }
    val recommendations = fullQueue.filter { it.isRecommendation }

    // Group manual queue by contributor
    val groupedManual = manualQueue.groupBy { it.addedBy }

    // Lazy list state for reordering
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        // Calculate actual reordering within the manual queue (skip section headers)
        val fromItem = from.key as? String ?: return@rememberReorderableLazyListState
        val toItem = to.key as? String ?: return@rememberReorderableLazyListState
        val toIndex = manualQueue.indexOfFirst { it.queueId == toItem }
        if (toIndex >= 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.moveQueueItem(fromItem, toIndex)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            JamTopBar(
                session = session,
                isSyncing = isSyncing,
                onBack = onBack,
                onChat = { showChatSheet = true },
                onSettings = { showSettingsSheet = true },
                onLeave = { viewModel.leaveSession(); onBack() },
            )
        },
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {

                // ── Room Code Card ────────────────────────────────────────────
                item(key = "room_code") {
                    RoomCodeCard(
                        roomId = session.roomId,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString("https://simpmusic.app/jam/${session.roomId}"))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                // ── Now Playing ───────────────────────────────────────────────
                item(key = "now_playing_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Now Playing",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        if (isHost || perms.allowAddSongs) {
                            FilledTonalButton(
                                onClick = { showAddSongSheet = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add songs")
                            }
                        }
                    }
                }

                item(key = "now_playing") {
                    val currentItem = session.playbackState.queue.find { it.videoId == session.playbackState.currentSongId }
                    nowPlayingState?.let { state ->
                        NowPlayingRow(
                            title = currentItem?.title ?: state.songEntity?.title
                                ?: state.mediaItem.metadata.title?.toString() ?: "Unknown",
                            artist = currentItem?.artist ?: state.songEntity?.artistName?.joinToString(", ")
                                ?: state.mediaItem.metadata.artist?.toString() ?: "Unknown Artist",
                            artworkUrl = currentItem?.thumbnailUrl ?: state.mediaItem.metadata.artworkUri?.toString()
                                ?: state.songEntity?.thumbnails,
                            isPlaying = session.playbackState.isPlaying,
                            shuffle = session.playbackState.shuffle,
                            repeat = session.playbackState.repeatMode,
                            canControl = isHost || perms.allowPause,
                            onToggleShuffle = { viewModel.setShuffle(!session.playbackState.shuffle) },
                            onCycleRepeat = {
                                val next = when (session.playbackState.repeatMode) {
                                    JamRepeatMode.OFF -> JamRepeatMode.QUEUE
                                    JamRepeatMode.QUEUE -> JamRepeatMode.ONE
                                    JamRepeatMode.ONE -> JamRepeatMode.OFF
                                }
                                viewModel.setRepeat(next)
                            },
                        )
                    } ?: run {
                        Text(
                            "Nothing playing",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── Queue header ──────────────────────────────────────────────
                item(key = "queue_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Next in Queue",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Manual queue, grouped by contributor ───────────────────────
                if (manualQueue.isEmpty()) {
                    item(key = "empty_queue") {
                        Text(
                            "Queue is empty — add a song to get started!",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    groupedManual.forEach { (contributor, songs) ->

                        // Section header per contributor
                        item(key = "header_$contributor") {
                            val participant = session.participants.find { it.userId == contributor }
                            ContributorHeader(
                                participant = participant,
                                fallbackName = contributor,
                                isHost = contributor == session.hostId,
                                modifier = Modifier.animateItem(),
                            )
                        }

                        // Songs for this contributor
                        items(songs, key = { it.queueId }) { item ->
                            val canDrag = isHost || (perms.allowReorder && item.addedBy == viewModel.localUserId)
                            val canRemove = isHost || (perms.allowRemoveSongs && item.addedBy == viewModel.localUserId)
                            val hasVoted = item.voterIds.contains(viewModel.localUserId)

                            ReorderableItem(reorderState, key = item.queueId, enabled = canDrag) { isDragging ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart && canRemove) {
                                            viewModel.removeFromQueue(item.queueId)
                                        }
                                        false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(),
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = canRemove,
                                    backgroundContent = {
                                        val alpha = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 0.7f else 0f
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = alpha))
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd,
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    },
                                ) {
                                    JamQueueItem(
                                        item = item,
                                        isDragging = isDragging,
                                        canDrag = canDrag,
                                        votingAllowed = isHost || perms.allowVoting,
                                        hasVoted = hasVoted,
                                        onVote = { viewModel.voteForSong(item.queueId) },
                                        dragModifier = Modifier.draggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Recommendations section ───────────────────────────────────
                if (recommendations.isNotEmpty()) {
                    item(key = "recs_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Based on Everyone's Taste",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (isHost) {
                                Row {
                                    IconButton(onClick = { viewModel.refreshRecommendations() }) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                                    }
                                    IconButton(onClick = { viewModel.toggleRecommendations(false) }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Disable")
                                    }
                                }
                            }
                        }
                    }

                    items(recommendations, key = { it.queueId }) { item ->
                        val hasVoted = item.voterIds.contains(viewModel.localUserId)
                        JamQueueItem(
                            item = item,
                            isDragging = false,
                            canDrag = false,
                            votingAllowed = isHost || perms.allowVoting,
                            hasVoted = hasVoted,
                            onVote = { viewModel.voteForSong(item.queueId) },
                            dragModifier = Modifier,
                            modifier = Modifier.animateItem(),
                        )
                    }
                } else if (isHost && !session.recommendationsEnabled) {
                    item(key = "recs_disabled") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Recommendations off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { viewModel.toggleRecommendations(true) }) {
                                Text("Enable")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }

            // ── Chat side sheet ────────────────────────────────────────────────
            if (showChatSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showChatSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    var textInput by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Room Chat", style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = { showChatSheet = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            reverseLayout = true,
                        ) {
                            items(chatMessages.reversed()) { msg ->
                                val isMe = msg.senderId == viewModel.localUserId
                                val senderParticipant = session.participants.find { it.userId == msg.senderId }
                                val senderName = senderParticipant?.name ?: msg.senderId
                                val senderImage = senderParticipant?.imageUrl ?: ""
                                
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    if (!isMe) {
                                        Box {
                                            if (senderImage.isNotBlank()) {
                                                AsyncImage(
                                                    model = senderImage,
                                                    contentDescription = "Profile",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        senderName.take(2).uppercase(),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        style = MaterialTheme.typography.labelSmall,
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                    ) {
                                        Text(
                                            if (isMe) "You" else senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp),
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message…") },
                                maxLines = 3,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    viewModel.sendChatMessage(textInput)
                                    textInput = ""
                                },
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────────
    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            JamSettingsSheetContent(
                session = session,
                isHost = isHost,
                onUpdatePermissions = viewModel::updatePermissions,
                onToggleRecommendations = viewModel::toggleRecommendations,
                onLeave = { showSettingsSheet = false; viewModel.leaveSession(); onBack() },
                onDismiss = { showSettingsSheet = false },
            )
        }
    }

    // ── Add Song sheet ─────────────────────────────────────────────────────────
    if (showAddSongSheet) {
        JamAddSongBottomSheet(
            onDismissRequest = { showAddSongSheet = false },
            jamViewModel = viewModel,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamTopBar(
    session: com.marki19.domain.jam.JamSessionState,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onLeave: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    if (session.isHost) "Your Jam" else "Jam Session",
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Syncing…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        // Participant avatars with online indicators
                        LazyRow(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            items(session.participants) { participant ->
                                ParticipantAvatar(
                                    participant = participant,
                                    fallbackName = participant.userId,
                                    isHost = participant.userId == session.hostId,
                                )
                            }
                        }
                        if (session.participants.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${session.participants.size} listening",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onChat) {
                Icon(Icons.Rounded.Chat, contentDescription = "Chat")
            }
            if (session.isHost) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Settings")
                }
            } else {
                TextButton(onClick = onLeave) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Room code card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomCodeCard(
    roomId: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Room Code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    roomId,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Share this code to invite friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy invite link")
                }
                Text(
                    "Copy Link",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Now Playing Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: JamRepeatMode,
    canControl: Boolean,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Cover",
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Playing indicator
            if (isPlaying) {
                Icon(
                    Icons.Rounded.Equalizer,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (canControl) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = shuffle,
                    onClick = onToggleShuffle,
                    label = { Text("Shuffle") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
                FilterChip(
                    selected = repeat != JamRepeatMode.OFF,
                    onClick = onCycleRepeat,
                    label = {
                        Text(when (repeat) {
                            JamRepeatMode.OFF -> "Repeat"
                            JamRepeatMode.QUEUE -> "Repeat All"
                            JamRepeatMode.ONE -> "Repeat One"
                        })
                    },
                    leadingIcon = {
                        Icon(
                            if (repeat == JamRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Contributor section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContributorHeader(
    participant: JamParticipant?,
    fallbackName: String,
    isHost: Boolean,
    modifier: Modifier = Modifier,
) {
    val displayName = participant?.name ?: fallbackName
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            if (participant != null && participant.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = participant.imageUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            // Online indicator dot
            val isOnline = participant?.online != false
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFBDBDBD))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildString {
                append("Added by ")
                append(displayName)
                if (isHost) append(" 👑")
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Queue item row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JamQueueItem(
    item: JamQueueItem,
    isDragging: Boolean,
    canDrag: Boolean,
    votingAllowed: Boolean,
    hasVoted: Boolean,
    onVote: () -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val elevation by androidx.compose.animation.core.animateFloatAsState(
        if (isDragging) 8f else 0f,
        label = "drag elevation",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = elevation.dp,
        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drag handle
            if (canDrag) {
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp).then(dragModifier),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }

            // Artwork
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = "Cover",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Vote badge
            if (votingAllowed) {
                Spacer(modifier = Modifier.width(8.dp))
                VoteBadge(
                    voteCount = item.voteCount,
                    hasVoted = hasVoted,
                    onVote = onVote,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Vote badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoteBadge(
    voteCount: Int,
    hasVoted: Boolean,
    onVote: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            if (!hasVoted) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onVote()
            }
        },
        shape = RoundedCornerShape(50),
        color = if (hasVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.ThumbUp,
                contentDescription = "Vote",
                tint = if (hasVoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            if (voteCount > 0) {
                Text(
                    text = "$voteCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasVoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Participant avatar with online dot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParticipantAvatar(
    participant: JamParticipant?,
    fallbackName: String,
    isHost: Boolean,
) {
    val displayName = participant?.name ?: fallbackName
    Box(modifier = Modifier.size(24.dp)) {
        if (participant != null && participant.imageUrl.isNotBlank()) {
            AsyncImage(
                model = participant.imageUrl,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHost) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    color = if (isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                )
            }
        }
        val isOnline = participant?.online != false
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFBDBDBD))
                .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                .align(Alignment.BottomEnd),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Settings sheet content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JamSettingsSheetContent(
    session: com.marki19.domain.jam.JamSessionState,
    isHost: Boolean,
    onUpdatePermissions: (com.marki19.domain.jam.JamPermissions) -> Unit,
    onToggleRecommendations: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val perms = session.permissions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text("Session Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        if (isHost) {
            Text(
                "Guest Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow("Add Songs", perms.allowAddSongs) {
                onUpdatePermissions(perms.copy(allowAddSongs = it))
            }
            PermissionRow("Remove Songs", perms.allowRemoveSongs) {
                onUpdatePermissions(perms.copy(allowRemoveSongs = it))
            }
            PermissionRow("Reorder Queue", perms.allowReorder) {
                onUpdatePermissions(perms.copy(allowReorder = it))
            }
            PermissionRow("Pause / Play", perms.allowPause) {
                onUpdatePermissions(perms.copy(allowPause = it))
            }
            PermissionRow("Skip Songs", perms.allowSkip) {
                onUpdatePermissions(perms.copy(allowSkip = it))
            }
            PermissionRow("Seek", perms.allowSeek) {
                onUpdatePermissions(perms.copy(allowSeek = it))
            }
            PermissionRow("Vote on Songs", perms.allowVoting) {
                onUpdatePermissions(perms.copy(allowVoting = it))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PermissionRow("Based on Everyone's Taste", session.recommendationsEnabled) {
                onToggleRecommendations(it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        TextButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(if (isHost) "End Session" else "Leave Session")
        }
    }
}

@Composable
private fun PermissionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
