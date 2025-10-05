package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
data class CanvasResponse(
    @ProtoNumber(1)
    val canvases: List<Canvas>,
) {
    @Serializable
    data class Canvas(
        @ProtoNumber(1)
        val id: String,
        @ProtoNumber(2)
        val canvas_url: String,
        @ProtoNumber(5)
        val track_uri: String,
        @ProtoNumber(6)
        val artist: Artist,
        @ProtoNumber(9)
        val other_id: String? = null,
        @ProtoNumber(11)
        val canvas_uri: String,
        @ProtoNumber(13)
        val thumbsOfCanva: List<ThumbOfCanva>? = null,
    ) {
        @Serializable
        data class Artist(
            @ProtoNumber(1)
            val artist_uri: String,
            @ProtoNumber(2)
            val artist_name: String,
            @ProtoNumber(3)
            val artist_img_url: String,
        )

        @Serializable
        data class ThumbOfCanva(
            @ProtoNumber(1)
            val height: Int? = null,
            @ProtoNumber(2)
            val width: Int? = null,
            @ProtoNumber(3)
            val url: String? = null,
        )
    }
}

// message CanvasResponse {
//  message Canvas {
//    string id = 1;                // ef3bc2ac86ba4a39b2cddff19dca884a
//    string canvas_url = 2;        // https://canvaz.scdn.co/upload/artist/6i1GVNJCyyssRwXmnaeEFH/video/ef3bc2ac86ba4a39b2cddff19dca884a.cnvs.mp4
//    string track_uri = 5;         // spotify:track:5osCClSjGplWagDsJmyivf
//    message Artist {
//      string artist_uri = 1;      // spotify:artist:3E61SnNA9oqKP7hI0K3vZv
//      string artist_name = 2;     // CALVO
//      string artist_img_url = 3;  // https://i.scdn.co/image/2d7b0ebe1e06c74f5c6b9a2384d746673051241d
//    }
//    Artist artist = 6;
//    string other_id = 9;          // 957a9be5e5c1b9ef1ac1c96b7cebf396
//    string canvas_uri = 11;       // spotify:canvas:1OuybAWK7XOQMG725ZtFwG
//  }
//  repeated Canvas canvases = 1;
// }