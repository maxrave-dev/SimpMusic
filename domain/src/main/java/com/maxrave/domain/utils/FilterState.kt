package com.maxrave.domain.utils

import com.maxrave.common.R

sealed interface FilterState {
    val displayNameRes: Int

    object OlderFirst : FilterState {
        override val displayNameRes = R.string.older_first
    }

    object NewerFirst : FilterState {
        override val displayNameRes = R.string.newer_first
    }

    object Title : FilterState {
        override val displayNameRes = R.string.title
    }
}