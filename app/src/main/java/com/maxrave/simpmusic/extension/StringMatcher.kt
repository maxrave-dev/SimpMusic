package com.maxrave.simpmusic.extension

import com.maxrave.logger.Logger

fun levenshtein(
    lhs: CharSequence,
    rhs: CharSequence,
): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i

        for (j in 1..lhsLength) {
            val editCost = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + editCost
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength]
}

fun bestMatchingIndex(
    s: String,
    list: List<String>,
): Int? {
    val listCost = ArrayList<Int>()
    for (i in list.indices) {
        listCost.add(levenshtein(s, list[i]))
    }
    Logger.d("Lyrics", "Best cost " + listCost.minOrNull().toString())
    val min = listCost.minOrNull()
    return if (min != null && min < 20) listCost.indexOf(listCost.minOrNull()) else null
}

fun get3MatchingIndex(
    s: String,
    list: List<String>,
): ArrayList<Int> {
    val listIndex = ArrayList<Int>()
    val listCost = ArrayList<Int>()
    for (i in list.indices) {
        listCost.add(levenshtein(s, list[i]))
    }
    listIndex.add(listCost.indexOf(listCost.minOrNull()))
    var count = 1
    while (count <= 3) {
        val cloneList = ArrayList(list)
        cloneList.remove(list[listIndex.last()])
        listCost.clear()
        for (i in list.indices) {
            if (listIndex.contains(i)) continue
            listCost.add(levenshtein(s, list[i]))
        }
        listIndex.add(listCost.indexOf(listCost.minOrNull()))
        count++
    }
    return listIndex
}