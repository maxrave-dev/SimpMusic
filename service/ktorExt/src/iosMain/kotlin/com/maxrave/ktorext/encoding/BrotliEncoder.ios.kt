package com.maxrave.ktorext.encoding

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlin.coroutines.CoroutineContext

object BrotliEncoder : ContentEncoder {
    override val name: String
        get() = "br"

    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel {
        TODO("Not yet implemented")
    }

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext,
    ): ByteWriteChannel {
        TODO("Not yet implemented")
    }

    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel {
        TODO("Not yet implemented")
    }
}

actual fun createBrotliEncoder(): ContentEncoder {
    TODO("Not yet implemented")
}