package com.maxrave.simpmusic.pagination

/**
 * Pure index-permutation helpers for in-place drag-reordering of paged local playlists (#2540).
 *
 * Calling `LazyPagingItems.refresh()` after each drag-drop triggers a full paging reload whose
 * intermediate empty/anchor state snaps `LazyListState` back to the top of the playlist. Keeping
 * a lightweight `displayIndex -> backingIndex` bijection over the loaded `LazyPagingItems`
 * updates row order immediately in memory while Room/YouTube persistence runs in the background,
 * preserving scroll position across arbitrary chains of moves, deletions, and paged appends.
 */
fun normalizePermutationForCount(
    permutation: List<Int>,
    itemCount: Int,
): List<Int> {
    if (permutation.isEmpty() || itemCount <= 0) return emptyList()
    val seen = BooleanArray(itemCount)
    val result = ArrayList<Int>(itemCount)
    for (idx in permutation) {
        if (idx in 0 until itemCount && !seen[idx]) {
            seen[idx] = true
            result.add(idx)
        }
    }
    for (idx in 0 until itemCount) {
        if (!seen[idx]) {
            result.add(idx)
        }
    }
    return result
}

fun applyReorderMove(
    currentPermutation: List<Int>,
    itemCount: Int,
    fromIndex: Int,
    toIndex: Int,
): List<Int> {
    if (itemCount <= 1 || fromIndex == toIndex || fromIndex !in 0 until itemCount || toIndex !in 0 until itemCount) {
        return currentPermutation
    }
    val mutable =
        if (currentPermutation.isEmpty()) {
            MutableList(itemCount) { it }
        } else {
            normalizePermutationForCount(currentPermutation, itemCount).toMutableList()
        }
    val movedBackingIndex = mutable.removeAt(fromIndex)
    mutable.add(toIndex, movedBackingIndex)
    return mutable
}

fun resolveBackingIndex(
    permutation: List<Int>,
    displayIndex: Int,
    itemCount: Int,
): Int {
    if (permutation.isEmpty() || displayIndex !in 0 until itemCount) return displayIndex
    if (permutation.size == itemCount) {
        val direct = permutation[displayIndex]
        if (direct in 0 until itemCount) return direct
    }
    return normalizePermutationForCount(permutation, itemCount)[displayIndex]
}

fun removeBackingIndicesFromPermutation(
    currentPermutation: List<Int>,
    itemCount: Int,
    removedBackingIndices: Set<Int>,
): List<Int> {
    if (currentPermutation.isEmpty() || itemCount <= 0 || removedBackingIndices.isEmpty()) {
        return currentPermutation
    }
    val normalized = normalizePermutationForCount(currentPermutation, itemCount)
    var shift = 0
    val newIndexMap =
        IntArray(itemCount) { idx ->
            if (idx in removedBackingIndices) {
                shift++
                -1
            } else {
                idx - shift
            }
        }
    if (shift == 0) return normalized
    val remainingCount = (itemCount - shift).coerceAtLeast(0)
    if (remainingCount == 0) return emptyList()
    val result = ArrayList<Int>(remainingCount)
    for (backing in normalized) {
        val mapped = newIndexMap[backing]
        if (mapped >= 0) {
            result.add(mapped)
        }
    }
    return result
}

fun removeBackingIndexFromPermutation(
    currentPermutation: List<Int>,
    itemCount: Int,
    removedBackingIndex: Int,
): List<Int> =
    if (removedBackingIndex < 0) {
        currentPermutation
    } else {
        removeBackingIndicesFromPermutation(currentPermutation, itemCount, setOf(removedBackingIndex))
    }

fun <T> reorderSnapshot(
    items: List<T>,
    permutation: List<Int>,
): List<T> {
    if (permutation.isEmpty() || items.isEmpty()) return items
    val normalized = normalizePermutationForCount(permutation, items.size)
    return List(items.size) { displayIdx ->
        items[normalized[displayIdx]]
    }
}

fun stablePlaylistRowKey(
    pairId: Int?,
    videoId: String?,
    fallbackIndex: Int,
): Any =
    when {
        pairId != null && pairId > 0 -> "pair_$pairId"
        !videoId.isNullOrEmpty() -> "video_${videoId}_$fallbackIndex"
        else -> "placeholder_$fallbackIndex"
    }
