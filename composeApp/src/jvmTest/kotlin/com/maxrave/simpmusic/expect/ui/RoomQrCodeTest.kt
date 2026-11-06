package com.maxrave.simpmusic.expect.ui

import com.maxrave.simpmusic.utils.listenTogetherLink
import com.maxrave.simpmusic.utils.roomCodeFromText
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomQrCodeTest {
    private fun QrMatrix.toImage(scale: Int = 6): BufferedImage {
        val image = BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.setRGB(x, y, if (this[x / scale, y / scale]) 0x000000 else 0xFFFFFF)
            }
        }
        return image
    }

    @Test
    fun aGeneratedRoomQrCodeScansBackToItsRoom() {
        val decoded = decodeQrCode(encodeQrMatrix(listenTogetherLink("ABCD1234")).toImage())
        assertEquals(listenTogetherLink("ABCD1234"), decoded)
        assertEquals("ABCD1234", roomCodeFromText(decoded.orEmpty()))
    }

    @Test
    fun anImageWithNoCodeReadsAsNothing() {
        assertNull(decodeQrCode(BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)))
    }
}
