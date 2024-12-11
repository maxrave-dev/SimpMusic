package com.maxrave.kotlinytmusicscraper.models.response.spotify

import kotlinx.serialization.Serializable

@Serializable
data class PersonalTokenResponse(
    val clientId: String,
    val accessToken: String,
    val accessTokenExpirationTimestampMs: Long,
    val isAnonymous: Boolean,
)
// {
//    "clientId": "f6a40776580943a7bc5173125a1e8832",
//    "accessToken": "BQAtU9wgHQv36td_-vYix6b-8i_I0utzxmbAjw9cI1meDV2hFvbmUcCACcMlL5eFe55CaN-4Lp8n9u7w8nu2Y3QTfoYdDYEK-thIkiWYaJxyBNHTYBBq7IckaV8prsK3iJyZM9O7bUerwnJCfJVgJfUCrWbt92G1-OY1eGZlg7enUUGJateUWtkTQDijLewqnYPlufMOBP10eEbr9RgQPjVIoSFu5brCU_-wAcMihqWcHL1xPzf4roBof2PZO9W2OAU0DK2v7ke73tBbr7mKU0ajB1lDnrW39OCln2lRXn0ZYscJBe5AdKXv2sU",
//    "accessTokenExpirationTimestampMs": 1707400170241,
//    "isAnonymous": false
// }