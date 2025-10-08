package com.maxrave.ktorext.encoding

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlin.coroutines.CoroutineContext

actual object BrotliEncoder : ContentEncoder {
    actual override val name: String
        get() = TODO("Not yet implemented")

    actual override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel {
        TODO("Not yet implemented")
    }

    actual override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel {
        TODO("Not yet implemented")
    }

    actual override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext,
    ): ByteWriteChannel {
        TODO("Not yet implemented")
    }
}

actual fun createBrotliEncoder(): ContentEncoder {
    TODO("Not yet implemented")
}