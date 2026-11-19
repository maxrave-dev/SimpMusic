package com.maxrave.simpmusic.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlaylistReorderPermutationTest {
    @Test
    fun chainedMovesMatchDirectListMutableMovesIsomorphically() {
        val backing = listOf("A", "B", "C", "D", "E")
        val direct = backing.toMutableList()
        var perm = emptyList<Int>()

        val moves = listOf(4 to 1, 2 to 0, 0 to 3, 3 to 4, 1 to 2)
        for ((from, to) in moves) {
            val item = direct.removeAt(from)
            direct.add(to, item)
            perm = applyReorderMove(perm, backing.size, from, to)
            assertEquals(direct, reorderSnapshot(backing, perm))
        }
    }

    @Test
    fun pagingItemCountGrowthPreservesPriorMovesAndMapsNewPagesToIdentity() {
        val page1 = listOf("S0", "S1", "S2", "S3")
        var perm = applyReorderMove(emptyList(), page1.size, fromIndex = 3, toIndex = 1)
        assertEquals(listOf("S0", "S3", "S1", "S2"), reorderSnapshot(page1, perm))

        val twoPages = listOf("S0", "S1", "S2", "S3", "S4", "S5")
        assertEquals(listOf("S0", "S3", "S1", "S2", "S4", "S5"), reorderSnapshot(twoPages, perm))

        perm = applyReorderMove(perm, twoPages.size, fromIndex = 5, toIndex = 2)
        assertEquals(listOf("S0", "S3", "S5", "S1", "S2", "S4"), reorderSnapshot(twoPages, perm))
    }

    @Test
    fun rollbackAndOutOfBoundsMovesNeverProduceDuplicateBackingIndices() {
        val backing = listOf("A", "B", "C", "D")
        val moved = applyReorderMove(emptyList(), backing.size, fromIndex = 0, toIndex = 2)
        assertEquals(listOf("B", "C", "A", "D"), reorderSnapshot(backing, moved))

        assertEquals(moved, applyReorderMove(moved, backing.size, fromIndex = -1, toIndex = 2))
        assertEquals(moved, applyReorderMove(moved, backing.size, fromIndex = 1, toIndex = 4))
        assertEquals(moved, applyReorderMove(moved, backing.size, fromIndex = 2, toIndex = 2))

        // Even if a stale permutation exceeds a shrunken itemCount, normalizePermutationForCount
        // and resolveBackingIndex guarantee a strict bijection with zero duplicate keys.
        val stalePerm = listOf(5, 3, 0, 2, 1, 4)
        val resolvedForThree = (0 until 3).map { resolveBackingIndex(stalePerm, it, itemCount = 3) }
        assertEquals(3, resolvedForThree.distinct().size)
        assertEquals(listOf(0, 2, 1), resolvedForThree)
    }

    @Test
    fun removeSingleAndDuplicateBackingIndicesKeepPermutationAligned() {
        val backing = listOf("A", "B", "C", "B")
        // Move second "B" (index 3) -> index 1: display [A, B#3, B#1, C] (perm = [0, 3, 1, 2])
        val perm = applyReorderMove(emptyList(), backing.size, fromIndex = 3, toIndex = 1)

        // Removing both "B" entries (backing indices 1 and 3) leaves [A, C] cleanly
        val updatedPerm = removeBackingIndicesFromPermutation(perm, backing.size, setOf(1, 3))
        val filteredBacking = listOf("A", "C")
        assertEquals(listOf("A", "C"), reorderSnapshot(filteredBacking, updatedPerm))
        assertEquals(listOf(0, 1), updatedPerm)
    }

    @Test
    fun stablePlaylistRowKeyDependsOnPairPrimaryKeyNotMutablePosition() {
        val keyBeforeMove = stablePlaylistRowKey(pairId = 42, videoId = "vid1", fallbackIndex = 0)
        val keyAfterMove = stablePlaylistRowKey(pairId = 42, videoId = "vid1", fallbackIndex = 7)
        assertEquals(keyBeforeMove, keyAfterMove)

        val differentRowSameSong = stablePlaylistRowKey(pairId = 43, videoId = "vid1", fallbackIndex = 7)
        assertNotEquals(keyBeforeMove, differentRowSameSong)
    }
}
