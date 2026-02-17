package com.maxrave.spotify.model.body

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyClientBody(
    val client_data: ClientData = ClientData(),
) {
    @Serializable
    data class ClientData(
        val client_version: String = "1.2.62.476.g2ad6e7f3",
        val client_id: String = "d8a5ed958d274c2e8ee717e6a4b0971d",
        val js_sdk_data: JsSdkData = JsSdkData(),
    ) {
        @Serializable
        data class JsSdkData(
            val device_brand: String = "Apple",
            val device_model: String = "unknown",
            val os: String = "macos",
            val os_version: String = "10.15.7",
            val device_id: String = "4fd0c748-b282-4927-9658-6d51a24e58b7",
            val device_type: String = "computer",
        )
    }
}

/*
{"client_data":{"client_version":"1.2.62.476.g2ad6e7f3","client_id":"d8a5ed958d274c2e8ee717e6a4b0971d","js_sdk_data":{"device_brand":"Apple","device_model":"unknown","os":"macos","os_version":"10.15.7","device_id":"4fd0c748-b282-4927-9658-6d51a24e58b7","device_type":"computer"}}}
 */