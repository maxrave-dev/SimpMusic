package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.ui.encodeQrMatrix

/** Dark modules on white whatever the theme: scanners expect that polarity. */
@Composable
fun QrCodeImage(
    text: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(text) { encodeQrMatrix(text) }
    Canvas(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        val cell = size.minDimension / matrix.size
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                // Half a pixel of overlap hides the seams between fractional-width modules.
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
