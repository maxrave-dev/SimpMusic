package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.ArrowBackIosNew: ImageVector
  get() {
    if (_ArrowBackIosNew != null) {
      return _ArrowBackIosNew!!
    }
    _ArrowBackIosNew =
      ImageVector.Builder(
          name = "ArrowBackIosNew",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.55f, 12f)
            lineToRelative(7.35f, 7.35f)
            quadToRelative(0.38f, 0.38f, 0.36f, 0.88f)
            reflectiveQuadTo(16.88f, 21.1f)
            reflectiveQuadTo(16f, 21.48f)
            reflectiveQuadTo(15.13f, 21.1f)
            lineTo(7.43f, 13.43f)
            quadTo(7.13f, 13.13f, 6.98f, 12.75f)
            reflectiveQuadTo(6.83f, 12f)
            reflectiveQuadTo(6.98f, 11.25f)
            reflectiveQuadTo(7.43f, 10.58f)
            lineToRelative(7.7f, -7.7f)
            quadTo(15.5f, 2.5f, 16.01f, 2.51f)
            quadTo(16.53f, 2.52f, 16.9f, 2.9f)
            reflectiveQuadToRelative(0.38f, 0.88f)
            reflectiveQuadTo(16.9f, 4.65f)
            lineTo(9.55f, 12f)
            close()
          }
        }
        .build()
    return _ArrowBackIosNew!!
  }

private var _ArrowBackIosNew: ImageVector? = null
