package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@ExperimentalSerializationApi
data class TokenResponse(
    @ProtoNumber(1)
    @SerialName("response_type")
    val responseType: String?,
    @ProtoNumber(2)
    @SerialName("granted_token")
    val grantedToken: GrantedToken?,
) {
    @Serializable
    data class GrantedToken(
        @ProtoNumber(1)
        val token: String?,
        @ProtoNumber(2)
        @SerialName("expires_after_seconds")
        val expiresAfterSeconds: Int?,
        @ProtoNumber(3)
        @SerialName("refresh_after_seconds")
        val refreshAfterSeconds: Int?,
        @ProtoNumber(4)
        val domains: List<Domain>?,
    ) {
        @Serializable
        data class Domain(
            @ProtoNumber(1)
            val domain: String?,
        )
    }
}
/*
{
    "response_type": "RESPONSE_GRANTED_TOKEN_RESPONSE",
    "granted_token": {
        "token": "AAD1YNe8Mp081SMIQt1mlxBOLekd/hhng6ihNEsZ2Qi+cdSzR7LZ49mT7ODe1G+gbfx4TOcohfHXmCPJPmshEBV/dol8AkiLXnEv0bcrN+kp22Ul6HXAE6G0TgqqHX+FFBJXF//7YRAxwT+Q1zWwDtPBJ/ZFTRcQgl7cVn+xrI4f48rAArEVnH66R3jVip1/a2RqlDMJxw0XBnDKnY8X5qbjFFUocbl7AHo0hV2CZANCobv5g3mWQfYSDqc6brVsogTRTZpoX5PCPl7C1HGrcxLD/IEoDlls7pTCn9FuK/47hZggrYmRiS3+ppAC7ahkHZ4ahm5N3xbieSkU",
        "expires_after_seconds": 1216800,
        "refresh_after_seconds": 1209600,
        "domains": [
            {
                "domain": "spotify.com"
            },
            {
                "domain": "spotify.net"
            }
        ]
    }
}
 */