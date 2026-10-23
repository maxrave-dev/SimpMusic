package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.comments.CommentLikeState
import com.maxrave.domain.data.model.comments.YouTubeComment
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Send
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ThumbDown
import com.maxrave.simpmusic.ui.icon.ThumbDownOutline
import com.maxrave.simpmusic.ui.icon.ThumbUp
import com.maxrave.simpmusic.ui.icon.ThumbUpOutline
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.CommentReplies
import com.maxrave.simpmusic.viewModel.CommentsStatus
import com.maxrave.simpmusic.viewModel.CommentsUiState
import com.maxrave.simpmusic.viewModel.CommentsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_favorite_24
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.comments
import simpmusic.composeapp.generated.resources.comments_load_error
import simpmusic.composeapp.generated.resources.comments_log_in_hint
import simpmusic.composeapp.generated.resources.comments_log_in_message
import simpmusic.composeapp.generated.resources.comments_turned_off
import simpmusic.composeapp.generated.resources.go_to_log_in_page
import simpmusic.composeapp.generated.resources.log_in
import simpmusic.composeapp.generated.resources.reply
import simpmusic.composeapp.generated.resources.replying_to
import simpmusic.composeapp.generated.resources.retry
import kotlin.math.roundToInt

// The colour DescriptionView gives links and timestamps; "N replies" and "Show more replies" are links too.
private val LinkColor = Color(0xFF00B0FF)

/**
 * YouTube Music's comments for the track playing. Opens half way and drags up to full height; the
 * comment box stays pinned to the bottom of whatever part of the sheet is showing.
 *
 * Reading needs no account. Writing (post, reply, like, dislike) needs YouTube: signed out, every
 * one of them asks to log in instead. Comment text is handled the way a description is: a
 * timestamp seeks the track, a link opens normally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    videoId: String,
    isLoggedIn: Boolean,
    onTimestamp: (String) -> Unit,
    onLogIn: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: CommentsViewModel = koinViewModel(),
) {
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val localDensity = LocalDensity.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogIn by remember { mutableStateOf(false) }
    var inputHeight by remember { mutableIntStateOf(0) }

    // The sheet follows the track: skipping while it is open loads the new one's comments. open()
    // checks the sign-in state itself, so a page loaded signed out reloads after logging in from here.
    LaunchedEffect(videoId) { viewModel.open(videoId) }
    // Nothing to write into until the comments are in: loading, failed, or turned off.
    val loaded = uiState.status == CommentsStatus.LOADED

    // Signed out, every write asks to log in first.
    fun signedIn(write: () -> Unit) {
        if (isLoggedIn) write() else showLogIn = true
    }

    val rowActions =
        CommentRowActions(
            onTimestamp = onTimestamp,
            onUrl = { uriHandler.openUri(it) },
            onLike = { id -> signedIn { viewModel.toggleLike(id) } },
            onDislike = { id -> signedIn { viewModel.toggleDislike(id) } },
            onReply = { id -> signedIn { viewModel.startReply(id) } },
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // Transparent above the card, so a fully expanded sheet stops below the status bar.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(32.dp)
                                    .height(4.dp),
                            colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                            shape = RoundedCornerShape(50),
                        ) {}
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.comments),
                                style = typo().titleMedium,
                                color = colors.content,
                            )
                            val count = uiState.header?.let { it.countShort ?: it.countText }
                            if (count != null && uiState.status == CommentsStatus.LOADED) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = count, style = typo().bodyMedium, color = colors.subtitle)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            }) {
                                Icon(imageVector = SimpIcons.Close, contentDescription = null, tint = colors.content)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), thickness = 1.dp)
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            CommentsBody(
                                uiState = uiState,
                                viewModel = viewModel,
                                colors = colors,
                                actions = rowActions,
                                bottomPadding = with(localDensity) { (if (loaded) inputHeight else 0).toDp() },
                            )
                        }
                    }
                    if (loaded) {
                        // Keyed on the video: text typed for one track must not be sent to the next one.
                        key(uiState.videoId) {
                            CommentInput(
                                sheetState = sheetState,
                                isLoggedIn = isLoggedIn,
                                userAvatar = uiState.header?.userAvatar,
                                placeholder = uiState.replyingTo?.replyPlaceholder ?: uiState.header?.placeholder.orEmpty(),
                                replyingTo = uiState.replyingTo?.author,
                                posting = uiState.posting,
                                colors = colors,
                                onCancelReply = viewModel::cancelReply,
                                onFocus = { scope.launch { sheetState.expand() } },
                                onPost = { text, clear -> viewModel.post(text, clear) },
                                onLogIn = { showLogIn = true },
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .onSizeChanged { inputHeight = it.height },
                            )
                        }
                    }
                }
            }
        }
        if (showLogIn) {
            AlertDialog(
                title = { Text(stringResource(Res.string.log_in)) },
                text = { Text(stringResource(Res.string.comments_log_in_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogIn = false
                        onLogIn()
                    }) {
                        Text(stringResource(Res.string.go_to_log_in_page), color = LocalContentColor.current)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogIn = false }) {
                        Text(stringResource(Res.string.cancel), color = LocalContentColor.current)
                    }
                },
                onDismissRequest = { showLogIn = false },
            )
        }
    }
}

private class CommentRowActions(
    val onTimestamp: (String) -> Unit,
    val onUrl: (String) -> Unit,
    val onLike: (String) -> Unit,
    val onDislike: (String) -> Unit,
    val onReply: (String) -> Unit,
)

@Composable
private fun CommentsBody(
    uiState: CommentsUiState,
    viewModel: CommentsViewModel,
    colors: SurfaceDarkColors,
    actions: CommentRowActions,
    bottomPadding: Dp,
) {
    when {
        uiState.status == CommentsStatus.LOADING -> {
            Box(Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.content, strokeWidth = 3.dp)
            }
        }
        uiState.status == CommentsStatus.TURNED_OFF -> {
            Box(Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
                Text(stringResource(Res.string.comments_turned_off), style = typo().bodyMedium, color = colors.subtitle)
            }
        }
        uiState.status == CommentsStatus.ERROR -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.comments_load_error), style = typo().bodyMedium, color = colors.subtitle)
                TextButton(onClick = viewModel::retry) {
                    Text(stringResource(Res.string.retry), color = colors.content)
                }
            }
        }
        else -> {
            val listState = rememberLazyListState()
            // Load more as the list nears its end, as YouTube Music does.
            val nearEnd by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
                }
            }
            LaunchedEffect(nearEnd, uiState.nextToken, uiState.moreFailed) {
                if (nearEnd && !uiState.moreFailed) viewModel.loadMore()
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = bottomPadding),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        avatarSize = 36.dp,
                        gap = 12.dp,
                        colors = colors,
                        actions = actions,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        comment.repliesLabel?.let { label ->
                            Text(
                                text = label,
                                style = typo().labelSmall,
                                color = LinkColor,
                                modifier = Modifier.padding(top = 8.dp).clickable { viewModel.toggleReplies(comment) },
                            )
                        }
                        uiState.replies[comment.id]?.takeIf { it.open }?.let { replies ->
                            Replies(comment.id, replies, viewModel, colors, actions)
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        when {
                            uiState.moreFailed ->
                                TextButton(onClick = viewModel::loadMore) {
                                    Text(stringResource(Res.string.retry), color = colors.content)
                                }
                            uiState.nextToken != null ->
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.subtitle, strokeWidth = 3.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Replies(
    commentId: String,
    replies: CommentReplies,
    viewModel: CommentsViewModel,
    colors: SurfaceDarkColors,
    actions: CommentRowActions,
) {
    replies.items.forEach { reply ->
        CommentRow(
            comment = reply,
            avatarSize = 24.dp,
            gap = 10.dp,
            colors = colors,
            actions = actions,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    when {
        replies.loading ->
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 10.dp).size(18.dp).align(Alignment.CenterHorizontally),
                color = colors.subtitle,
                strokeWidth = 3.dp,
            )
        replies.nextToken != null && replies.nextLabel != null ->
            Text(
                text = replies.nextLabel,
                style = typo().labelSmall,
                color = LinkColor,
                modifier = Modifier.padding(start = 34.dp, top = 8.dp).clickable { viewModel.loadMoreReplies(commentId) },
            )
    }
}

@Composable
private fun CommentRow(
    comment: YouTubeComment,
    avatarSize: Dp,
    gap: Dp,
    colors: SurfaceDarkColors,
    actions: CommentRowActions,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(colors.handle),
        )
        Spacer(modifier = Modifier.width(gap))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comment.author.isNotEmpty()) {
                    Text(
                        text = comment.author,
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (comment.isVerified) {
                    Icon(
                        imageVector = SimpIcons.CheckCircle,
                        contentDescription = null,
                        tint = colors.subtitle,
                        modifier = Modifier.padding(start = 3.dp).size(12.dp),
                    )
                }
                if (comment.publishedTime.isNotEmpty()) {
                    Text(
                        text = if (comment.author.isNotEmpty()) " · ${comment.publishedTime}" else comment.publishedTime,
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            DescriptionView(
                text = comment.text,
                limitLine = 4,
                color = colors.content,
                showMoreOnlyWhenOverflowing = true,
                onTimeClicked = actions.onTimestamp,
                onURLClicked = actions.onUrl,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                val liked = comment.likeState == CommentLikeState.LIKED
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape).clickable { actions.onLike(comment.id) }.padding(4.dp),
                ) {
                    Icon(
                        imageVector = if (liked) SimpIcons.ThumbUp else SimpIcons.ThumbUpOutline,
                        contentDescription = null,
                        tint = if (liked) colors.content else colors.subtitle,
                        modifier = Modifier.size(16.dp),
                    )
                    val count = if (liked) comment.likeCountLiked else comment.likeCount
                    if (count.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = count, style = typo().bodySmall, color = if (liked) colors.content else colors.subtitle)
                    }
                }
                val disliked = comment.likeState == CommentLikeState.DISLIKED
                Icon(
                    imageVector = if (disliked) SimpIcons.ThumbDown else SimpIcons.ThumbDownOutline,
                    contentDescription = null,
                    tint = if (disliked) colors.content else colors.subtitle,
                    modifier = Modifier.clip(CircleShape).clickable { actions.onDislike(comment.id) }.padding(4.dp).size(16.dp),
                )
                Text(
                    text = stringResource(Res.string.reply),
                    style = typo().labelSmall,
                    color = colors.subtitle,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { actions.onReply(comment.id) }.padding(4.dp),
                )
                if (comment.isHearted) {
                    FavoriteHeart(size = 14.dp)
                }
            }
            footer()
        }
    }
}

/** The creator's heart: the same red heart the app's like uses. */
@Composable
private fun FavoriteHeart(size: Dp) {
    Image(
        painter = painterResource(Res.drawable.baseline_favorite_24),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentInput(
    sheetState: SheetState,
    isLoggedIn: Boolean,
    userAvatar: String?,
    placeholder: String,
    replyingTo: String?,
    posting: Boolean,
    colors: SurfaceDarkColors,
    onCancelReply: () -> Unit,
    onFocus: () -> Unit,
    onPost: (text: String, clear: () -> Unit) -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                // Half open, the bottom of the sheet sits below the screen; lift the box onto the part that shows.
                .offset { IntOffset(0, -(runCatching { sheetState.requireOffset() }.getOrDefault(0f)).roundToInt().coerceAtLeast(0)) }
                .fillMaxWidth()
                .background(colors.container)
                .navigationBarsPadding()
                .imePadding(),
    ) {
        HorizontalDivider(thickness = 1.dp)
        if (!isLoggedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.comments_log_in_hint),
                    style = typo().bodyMedium,
                    color = colors.subtitle,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLogIn) {
                    Text(stringResource(Res.string.log_in), color = colors.content)
                }
            }
        } else {
            SignedInInput(
                userAvatar = userAvatar,
                placeholder = placeholder,
                replyingTo = replyingTo,
                posting = posting,
                colors = colors,
                onCancelReply = onCancelReply,
                onFocus = onFocus,
                onPost = onPost,
            )
        }
    }
}

@Composable
private fun SignedInInput(
    userAvatar: String?,
    placeholder: String,
    replyingTo: String?,
    posting: Boolean,
    colors: SurfaceDarkColors,
    onCancelReply: () -> Unit,
    onFocus: () -> Unit,
    onPost: (text: String, clear: () -> Unit) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        if (replyingTo != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.replying_to, replyingTo),
                    style = typo().bodySmall,
                    color = colors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = SimpIcons.Close,
                    contentDescription = null,
                    tint = colors.subtitle,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onCancelReply).padding(4.dp).size(16.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AsyncImage(
                model = userAvatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.handle),
            )
            Spacer(modifier = Modifier.width(12.dp))
            val send = { if (!posting) onPost(text) { text = "" } }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = typo().bodyMedium.copy(color = colors.content),
                cursorBrush = SolidColor(colors.content),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier =
                    Modifier
                        .weight(1f)
                        .onFocusChanged { if (it.isFocused) onFocus() },
                decorationBox = { field ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.content.copy(alpha = 0.08f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text(text = placeholder, style = typo().bodyMedium, color = colors.subtitle, maxLines = 1)
                        }
                        field()
                    }
                },
            )
            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                if (posting) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(20.dp), color = colors.content, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = send) {
                        Icon(imageVector = SimpIcons.Send, contentDescription = null, tint = colors.content)
                    }
                }
            }
        }
    }
}
