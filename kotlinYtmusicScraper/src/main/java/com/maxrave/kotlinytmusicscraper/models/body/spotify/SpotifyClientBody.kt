package com.maxrave.kotlinytmusicscraper.models.body.spotify

import com.maxrave.kotlinytmusicscraper.extension.randomString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@ExperimentalSerializationApi
class SpotifyClientBody(
    @ProtoNumber(1)
    @SerialName("client_data") val client_data: ClientData = ClientData()
) {
    @Serializable
    data class ClientData(
        @ProtoNumber(1)
        @SerialName("client_version") val client_version: String = "1.2.51.175.gfc0f5b10",
        @ProtoNumber(2)
        @SerialName("client_id") val client_id: String = "d8a5ed958d274c2e8ee717e6a4b0971d",
        @ProtoNumber(3)
        @SerialName("js_sdk_data") val js_sdk_data: JsSdkData = JsSdkData()
    ) {
        @Serializable
        data class JsSdkData(
            @ProtoNumber(1)
            @SerialName("device_brand") val device_brand: String = "unknown",
            @ProtoNumber(2)
            @SerialName("device_model") val device_model: String = "unknown",
            @ProtoNumber(3)
            val os: String = "linux",
            @ProtoNumber(4)
            @SerialName("os_version") val os_version: String = "unknown",
            @ProtoNumber(5)
            @SerialName("device_id") val device_id: String = randomString(32),
            @ProtoNumber(6)
            @SerialName("device_type") val device_type: String = "computer"
        )
    }
}

/*
{"client_data":{"client_version":"1.2.51.175.gfc0f5b10","client_id":"d8a5ed958d274c2e8ee717e6a4b0971d","js_sdk_data":{"device_brand":"unknown","device_model":"unknown","os":"linux","os_version":"unknown","device_id":"213e49fcbb6778c014533a5fa8fc0113","device_type":"computer"}}}
 */