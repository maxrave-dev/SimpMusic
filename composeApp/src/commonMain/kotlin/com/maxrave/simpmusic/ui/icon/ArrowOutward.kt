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
val SimpIcons.ArrowOutward: ImageVector
  get() {
    if (_ArrowOutward != null) {
      return _ArrowOutward!!
    }
    _ArrowOutward =
      ImageVector.Builder(
          name = "ArrowOutward",
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
            moveTo(16f, 8.4f)
            lineTo(7.1f, 17.3f)
            quadTo(6.83f, 17.58f, 6.4f, 17.58f)
            reflectiveQuadTo(5.7f, 17.3f)
            quadTo(5.43f, 17.02f, 5.43f, 16.6f)
            reflectiveQuadTo(5.7f, 15.9f)
            lineTo(14.6f, 7f)
            horizontalLineTo(7f)
            quadTo(6.58f, 7f, 6.29f, 6.71f)
            quadTo(6f, 6.43f, 6f, 6f)
            reflectiveQuadTo(6.29f, 5.29f)
            reflectiveQuadTo(7f, 5f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(18f, 6f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 17f)
            reflectiveQuadTo(16.29f, 16.71f)
            quadTo(16f, 16.43f, 16f, 16f)
            verticalLineTo(8.4f)
            close()
          }
        }
        .build()
    return _ArrowOutward!!
  }

private var _ArrowOutward: ImageVector? = null
