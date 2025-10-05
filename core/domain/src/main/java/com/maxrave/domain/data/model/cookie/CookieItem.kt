package com.maxrave.domain.data.model.cookie

data class CookieItem(
    val url: String,
    val content: List<Content>
) {
    data class Content(
        val domain: String,
        val name: String,
        val value: String,
        val isSecure: Boolean,
        val expiresUtc: Long,
        val hostKey: String,
        val path: String,
    )
    companion object {
        const val NAME = "name"
        const val VALUE = "value"
        const val SECURE = "is_secure"
        const val EXPIRY = "expires_utc"
        const val HOST = "host_key"
        const val PATH = "path"
    }
}