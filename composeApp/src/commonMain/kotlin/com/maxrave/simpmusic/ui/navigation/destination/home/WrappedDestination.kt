package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/**
 * The Wrapped reel.
 *
 * Carries no year argument on purpose: there is exactly one Wrapped — this year's — and the view
 * model resolves which that is. A year in the route would imply an archive the app does not keep,
 * and would let a deep link ask for a year with no [com.maxrave.domain.data.entities.analytics.PlaybackEventEntity]
 * rows behind it.
 */
@Serializable
object WrappedDestination
