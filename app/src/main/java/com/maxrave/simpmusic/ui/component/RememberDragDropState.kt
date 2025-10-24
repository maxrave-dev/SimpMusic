package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onSwap: (Int, Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val state =
        remember(lazyListState) {
            DragDropState(
                state = lazyListState,
                onSwap = onSwap,
                scope = scope,
            )
        }
    return state
}

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    this
        .layoutInfo
        .visibleItemsInfo
        .getOrNull(
            absoluteIndex -
                this.layoutInfo.visibleItemsInfo
                    .first()
                    .index,
        )

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit,
) {
    val current: Float by animateFloatAsState(dragDropState.draggingItemOffset)
    val previous: Float by animateFloatAsState(dragDropState.previousItemOffset.value)
    val dragging = index == dragDropState.currentIndexOfDraggedItem
    val draggingModifier =
        if (dragging) {
            Modifier
                .zIndex(1f)
                .graphicsLayer {
                    translationY = current
                }
        } else if (index == dragDropState.previousIndexOfDraggedItem) {
            Modifier
                .zIndex(1f)
                .graphicsLayer {
                    translationY = previous
                }
        } else {
            Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = tween(easing = FastOutLinearInEasing),
            )
        }
    Column(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val onSwap: (Int, Int) -> Unit,
) {
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() =
            draggingItemLayoutInfo?.let { item ->
                draggingItemInitialOffset + draggedDistance - item.offset
            } ?: 0f
    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() =
            state.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == currentIndexOfDraggedItem }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    // used to obtain initial offsets on drag start
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { Pair(it.offset, it.offsetEnd) }

    private val currentElement: LazyListItemInfo?
        get() =
            currentIndexOfDraggedItem?.let {
                state.getVisibleItemInfoFor(absoluteIndex = it)
            }

    private var currentSwapFromTo by mutableStateOf<Pair<Int, Int>?>(null)

    fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
                draggingItemInitialOffset = it.offset
            }
    }

    fun onDragInterrupted(end: Boolean = false) {
        currentSwapFromTo?.let { (from, to) ->
            if (from != to && from >= 0 && to >= 0 && end) {
                Logger.w("QueueBottomSheet", "onDragInterrupted: $from, $to")
                onSwap(from, to)
            }
            currentIndexOfDraggedItem = to
        }
        currentSwapFromTo = null
        if (currentIndexOfDraggedItem != null) {
            previousIndexOfDraggedItem = currentIndexOfDraggedItem
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    tween(easing = FastOutLinearInEasing),
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemInitialOffset = 0
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance

            currentElement?.let { hovered ->
                state.layoutInfo.visibleItemsInfo
                    .filterNot { item -> item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index }
                    .apply {
                        forEach { item ->
                            Logger.w("QueueBottomSheet", "onDrag: ${item.index}")
                        }
                    }.firstOrNull { item ->
                        val delta = (startOffset - hovered.offset)
                        when {
                            delta > 0 -> (endOffset > item.offsetEnd)
                            else -> (startOffset < item.offset)
                        }
                    }?.also { item ->
                        currentIndexOfDraggedItem?.let { current ->
                            currentSwapFromTo = Pair(current, item.index)
                        }
                    }
            }
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offsetEnd + draggedDistance
            return@let when {
                draggedDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset + 50f).takeIf { diff -> diff > 0 }
                draggedDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset - 50f).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}