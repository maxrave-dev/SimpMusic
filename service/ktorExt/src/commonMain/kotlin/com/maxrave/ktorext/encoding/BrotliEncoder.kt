package com.maxrave.ktorext.encoding

import io.ktor.client.plugins.compression.ContentEncodingConfig
import io.ktor.util.ContentEncoder

expect fun createBrotliEncoder(): ContentEncoder

fun ContentEncodingConfig.brotli(quality: Float? = null) {
    customEncoder(createBrotliEncoder(), quality)
}