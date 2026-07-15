package com.marki19.simpmusic.ui.navigation.destination.jam

import kotlinx.serialization.Serializable

@Serializable
data object JamMenuDestination

@Serializable
data object JamHostDestination

@Serializable
data object JamGuestDestination

@Serializable
data class JamSessionDestination(val roomCode: String)
