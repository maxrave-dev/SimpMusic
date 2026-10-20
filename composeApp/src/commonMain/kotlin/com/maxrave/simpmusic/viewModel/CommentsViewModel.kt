package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.comments.CommentLikeState
import com.maxrave.domain.data.model.comments.YouTubeComment
import com.maxrave.domain.data.model.comments.YouTubeCommentsHeader
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.comment_post_error
import simpmusic.composeapp.generated.resources.comments_load_error
import simpmusic.composeapp.generated.resources.comments_need_channel
import simpmusic.composeapp.generated.resources.error

enum class CommentsStatus { LOADING, LOADED, TURNED_OFF, ERROR }

data class CommentReplies(
    val open: Boolean = false,
    val items: List<YouTubeComment> = emptyList(),
    val nextToken: String? = null,
    /** "Show more replies", in the request's language. */
    val nextLabel: String? = null,
    val loading: Boolean = false,
)

data class CommentsUiState(
    val videoId: String? = null,
    /** Whether the page was loaded signed in: a signed-out page carries no comment box and no action tokens. */
    val signedIn: Boolean? = null,
    val status: CommentsStatus = CommentsStatus.LOADING,
    val header: YouTubeCommentsHeader? = null,
    val comments: List<YouTubeComment> = emptyList(),
    val nextToken: String? = null,
    val loadingMore: Boolean = false,
    /** Loading the next page failed: it waits for a tap instead of retrying on every scroll. */
    val moreFailed: Boolean = false,
    /** Bumped by every new track, so a page that arrives late cannot land in the wrong list. */
    val generation: Int = 0,
    /** By comment id. */
    val replies: Map<String, CommentReplies> = emptyMap(),
    val replyingTo: YouTubeComment? = null,
    /** The top-level comment whose thread the reply joins; a reply to a reply joins its parent's thread. */
    val replyThreadId: String? = null,
    val posting: Boolean = false,
)

/**
 * The comments sheet of Now Playing: YouTube Music's Comments tab for the track playing, its pages
 * and replies, and the signed-in writes (post, reply, like, dislike).
 */
class CommentsViewModel(
    private val songRepository: SongRepository,
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> get() = _uiState

    private var pageJob: Job? = null
    private var moreJob: Job? = null
    private val reactJobs = mutableMapOf<String, Job>()
    private var localIds = 0

    /**
     * Shows [videoId]'s comments. The ones already loaded for it stay as they are, unless they were
     * loaded with the other sign-in state: logging in from the sheet must bring the comment box and
     * the like and reply actions back. The sign-in state is read here rather than passed in: a sheet
     * recreated by a rotation sees "signed out" for its first frame, which would throw the page away.
     */
    suspend fun open(videoId: String) {
        val signedIn = dataStoreManager.cookie.first().isNotEmpty()
        val state = _uiState.value
        if (state.videoId == videoId && state.signedIn == signedIn && state.status != CommentsStatus.ERROR) return
        load(videoId, signedIn)
    }

    fun retry() {
        val state = _uiState.value
        state.videoId?.let { load(it, state.signedIn == true) }
    }

    private fun load(
        videoId: String,
        signedIn: Boolean,
    ) {
        pageJob?.cancel()
        moreJob?.cancel()
        val generation = _uiState.value.generation + 1
        _uiState.value = CommentsUiState(videoId = videoId, signedIn = signedIn, status = CommentsStatus.LOADING, generation = generation)
        pageJob =
            viewModelScope.launch {
                songRepository.getComments(videoId).collect { res ->
                    val page = res.data
                    _uiState.update { cur ->
                        when {
                            cur.generation != generation -> cur
                            res is Resource.Error -> cur.copy(status = CommentsStatus.ERROR)
                            page == null -> cur.copy(status = CommentsStatus.TURNED_OFF)
                            else ->
                                cur.copy(
                                    status = CommentsStatus.LOADED,
                                    header = page.header,
                                    // YouTube sometimes repeats a comment, and a repeated id crashes the keyed list.
                                    comments = page.comments.distinctBy { it.id },
                                    nextToken = page.nextToken,
                                )
                        }
                    }
                }
            }
    }

    /** The next page, asked for as the list nears its end. */
    fun loadMore() {
        val state = _uiState.value
        val token = state.nextToken ?: return
        if (state.status != CommentsStatus.LOADED || state.loadingMore) return
        val generation = state.generation
        _uiState.update { it.copy(loadingMore = true, moreFailed = false) }
        moreJob =
            viewModelScope.launch {
                songRepository.getCommentsPage(token).collect { res ->
                    val page = res.data
                    _uiState.update { cur ->
                        when {
                            cur.generation != generation -> cur
                            res is Resource.Success && page != null -> {
                                // Pages can repeat comments (YouTube's looping comments).
                                val comments = (cur.comments + page.comments).distinctBy { it.id }
                                cur.copy(
                                    loadingMore = false,
                                    comments = comments,
                                    // A page with nothing new means the comments have looped: its token only leads
                                    // round again, and asking for it at the end of the list would never stop.
                                    nextToken = if (comments.size == cur.comments.size) null else page.nextToken,
                                )
                            }
                            else -> cur.copy(loadingMore = false, moreFailed = true)
                        }
                    }
                }
            }
    }

    /** Opens or closes a comment's replies, loading them the first time. */
    fun toggleReplies(comment: YouTubeComment) {
        val replies = _uiState.value.replies[comment.id]
        if (replies != null && (replies.items.isNotEmpty() || replies.loading)) {
            setReplies(comment.id) { it.copy(open = !it.open) }
            return
        }
        val token = comment.repliesToken ?: return
        loadReplies(comment.id, token, append = false)
    }

    fun loadMoreReplies(commentId: String) {
        val replies = _uiState.value.replies[commentId] ?: return
        val token = replies.nextToken ?: return
        if (replies.loading) return
        loadReplies(commentId, token, append = true)
    }

    private fun loadReplies(
        commentId: String,
        token: String,
        append: Boolean,
    ) {
        setReplies(commentId) { it.copy(open = true, loading = true) }
        viewModelScope.launch {
            songRepository.getCommentsPage(token).collect { res ->
                val page = res.data
                if (res is Resource.Error || page == null) {
                    setReplies(commentId) { it.copy(loading = false, open = it.items.isNotEmpty()) }
                    makeToast(getString(Res.string.comments_load_error))
                    return@collect
                }
                setReplies(commentId) {
                    it.copy(
                        loading = false,
                        // distinctBy keeps a reply posted from the sheet from showing twice once its page loads.
                        items = (if (append) it.items + page.comments else page.comments).distinctBy { reply -> reply.id },
                        nextToken = page.nextToken,
                        nextLabel = page.nextLabel,
                    )
                }
            }
        }
    }

    fun toggleLike(commentId: String) {
        val comment = findComment(commentId) ?: return
        react(comment, if (comment.likeState == CommentLikeState.LIKED) CommentLikeState.NONE else CommentLikeState.LIKED)
    }

    fun toggleDislike(commentId: String) {
        val comment = findComment(commentId) ?: return
        react(comment, if (comment.likeState == CommentLikeState.DISLIKED) CommentLikeState.NONE else CommentLikeState.DISLIKED)
    }

    /** Shows [target] at once and sends it; a failure puts the comment back as it was. */
    private fun react(
        comment: YouTubeComment,
        target: CommentLikeState,
    ) {
        if (reactJobs[comment.id]?.isActive == true) return
        val action =
            when (target) {
                CommentLikeState.LIKED -> comment.likeAction
                CommentLikeState.DISLIKED -> comment.dislikeAction
                CommentLikeState.NONE ->
                    if (comment.likeState == CommentLikeState.LIKED) comment.unlikeAction else comment.undislikeAction
            }
        if (action == null) {
            // Signed in without a channel: YouTube offers no command to send.
            makeToast(getString(Res.string.comments_need_channel))
            return
        }
        val before = comment.likeState
        updateComment(comment.id) { it.copy(likeState = target) }
        reactJobs[comment.id] =
            viewModelScope.launch {
                songRepository.performCommentAction(action).collect { res ->
                    if (res is Resource.Error) {
                        updateComment(comment.id) { it.copy(likeState = before) }
                        makeToast(getString(Res.string.error))
                    }
                }
            }
    }

    fun startReply(commentId: String) {
        val comment = findComment(commentId) ?: return
        if (comment.replyParams == null) {
            makeToast(getString(Res.string.comments_need_channel))
            return
        }
        val state = _uiState.value
        // A reply to a reply lands in the same thread, and threads are only drawn under their top-level comment.
        val threadId =
            if (state.comments.any { it.id == commentId }) {
                commentId
            } else {
                state.replies.entries.firstOrNull { (_, replies) -> replies.items.any { it.id == commentId } }?.key ?: commentId
            }
        _uiState.update { it.copy(replyingTo = comment, replyThreadId = threadId) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null, replyThreadId = null) }
    }

    /**
     * Posts [text] as a comment, or as a reply to [CommentsUiState.replyingTo]. [onPosted] runs once
     * YouTube has it, so the box keeps the text if posting fails.
     */
    fun post(
        text: String,
        onPosted: () -> Unit,
    ) {
        val state = _uiState.value
        val body = text.trim()
        if (body.isEmpty() || state.posting) return
        val target = state.replyingTo
        val threadId = state.replyThreadId ?: target?.id
        val params = if (target != null) target.replyParams else state.header?.createParams
        if (params == null) {
            makeToast(getString(Res.string.comments_need_channel))
            return
        }
        val videoId = state.videoId
        _uiState.update { it.copy(posting = true) }
        viewModelScope.launch {
            val request = if (target != null) songRepository.postCommentReply(params, body) else songRepository.postComment(params, body)
            request.collect { res ->
                // The track changed while this was sending: it went to the video it was written for,
                // but the list on screen now belongs to another one, so leave that list alone.
                if (_uiState.value.videoId != videoId) return@collect
                if (res is Resource.Error) {
                    _uiState.update { it.copy(posting = false) }
                    makeToast(getString(Res.string.comment_post_error))
                    return@collect
                }
                // YouTube's answer normally carries the new comment; if it does not, show the text as sent.
                val posted = res.data ?: localComment(body, state.header?.userAvatar)
                _uiState.update { cur ->
                    when {
                        cur.videoId != videoId -> cur
                        target == null || threadId == null ->
                            cur.copy(posting = false, comments = (listOf(posted) + cur.comments).distinctBy { it.id })
                        else -> {
                            val existing = cur.replies[threadId]
                            // A thread never opened keeps its own token, so the replies already in it can still be loaded.
                            val thread =
                                if (existing != null && (existing.items.isNotEmpty() || existing.nextToken != null)) {
                                    existing
                                } else {
                                    val root = cur.comments.firstOrNull { it.id == threadId }
                                    CommentReplies(nextToken = root?.repliesToken, nextLabel = root?.repliesLabel)
                                }
                            cur.copy(
                                posting = false,
                                replyingTo = null,
                                replyThreadId = null,
                                replies =
                                    cur.replies +
                                        (threadId to thread.copy(open = true, items = (listOf(posted) + thread.items).distinctBy { it.id })),
                            )
                        }
                    }
                }
                onPosted()
            }
        }
    }

    private fun localComment(
        text: String,
        avatar: String?,
    ) = YouTubeComment(
        id = "local-${localIds++}",
        author = "",
        authorAvatar = avatar,
        authorChannelId = null,
        isVerified = false,
        isCreator = false,
        text = text,
        publishedTime = "",
        likeCount = "",
        likeCountLiked = "",
        likeState = CommentLikeState.NONE,
        isHearted = false,
        isPinned = false,
        repliesLabel = null,
        repliesToken = null,
        likeAction = null,
        unlikeAction = null,
        dislikeAction = null,
        undislikeAction = null,
        replyParams = null,
        replyPlaceholder = null,
    )

    private fun findComment(id: String): YouTubeComment? {
        val state = _uiState.value
        return state.comments.firstOrNull { it.id == id }
            ?: state.replies.values.firstNotNullOfOrNull { replies -> replies.items.firstOrNull { it.id == id } }
    }

    private fun updateComment(
        id: String,
        change: (YouTubeComment) -> YouTubeComment,
    ) {
        _uiState.update { state ->
            state.copy(
                comments = state.comments.map { if (it.id == id) change(it) else it },
                replies = state.replies.mapValues { (_, replies) -> replies.copy(items = replies.items.map { if (it.id == id) change(it) else it }) },
            )
        }
    }

    private fun setReplies(
        commentId: String,
        change: (CommentReplies) -> CommentReplies,
    ) {
        _uiState.update { state -> state.copy(replies = state.replies + (commentId to change(state.replies[commentId] ?: CommentReplies()))) }
    }
}
