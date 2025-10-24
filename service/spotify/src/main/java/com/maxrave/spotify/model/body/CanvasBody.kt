package com.maxrave.spotify.model.body

import kotlinx.serialization.Serializable

@Serializable
data class CanvasBody(
    val tracks: List<Track>,
) {
    @Serializable
    data class Track(
        val track_uri: String,
    )
}

// message CanvasRequest {
//  message Track {
//    string track_uri = 1;         // spotify:track:5osCClSjGplWagDsJmyivf
//  }
//  repeated Track tracks = 1;
// }