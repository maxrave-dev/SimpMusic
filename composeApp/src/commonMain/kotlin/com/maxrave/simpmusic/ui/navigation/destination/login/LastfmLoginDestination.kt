package com.maxrave.simpmusic.ui.navigation.destination.login

import kotlinx.serialization.Serializable

/**
 * Carries nothing on purpose.
 *
 * The Last.fm callback is handled by `SharedViewModel` when the browser returns, not routed here —
 * passing the token through navigation would open a second copy of this screen on top of the one
 * the user opened their browser from.
 */
@Serializable
data object LastfmLoginDestination
