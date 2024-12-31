package com.maxrave.kotlinytmusicscraper.encoder

import io.ktor.client.plugins.compression.ContentEncodingConfig
import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.brotli.dec.BrotliInputStream
import kotlin.coroutines.CoroutineContext

object BrotliEncoder : ContentEncoder {
    override val name: String = "br"
    override fun decode(source: ByteReadChannel, coroutineContext: CoroutineContext): ByteReadChannel {
        return BrotliInputStream(source.toInputStream()).toByteReadChannel(coroutineContext)
    }

    override fun encode(source: ByteReadChannel, coroutineContext: CoroutineContext): ByteReadChannel {
        throw UnsupportedOperationException("Encode not implemented by the library yet.")
    }

    override fun encode(source: ByteWriteChannel, coroutineContext: CoroutineContext): ByteWriteChannel {
        throw UnsupportedOperationException("Encode not implemented by the library yet.")
    }
}

fun ContentEncodingConfig.brotli(quality: Float? = null) {
    customEncoder(BrotliEncoder, quality)
}