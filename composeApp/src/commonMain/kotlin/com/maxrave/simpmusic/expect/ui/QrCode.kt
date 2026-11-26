package com.maxrave.simpmusic.expect.ui

/** A QR code's modules, including its quiet zone. `true` is a dark module. */
class QrMatrix(
    val size: Int,
    private val modules: BooleanArray,
) {
    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = modules[y * size + x]
}

/** ZXing is a plain JVM artifact, so each platform declares it and calls it itself. */
expect fun encodeQrMatrix(text: String): QrMatrix
