package com.maxrave.simpmusic.expect.ui

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

actual fun encodeQrMatrix(text: String): QrMatrix {
    val matrix =
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    return QrMatrix(matrix.width, BooleanArray(matrix.width * matrix.width) { matrix[it % matrix.width, it / matrix.width] })
}
