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
val SimpIcons.ArrowForwardIos: ImageVector
  get() {
    if (_ArrowForwardIos != null) {
      return _ArrowForwardIos!!
    }
    _ArrowForwardIos =
      ImageVector.Builder(
          name = "ArrowForwardIos",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
          autoMirror = true,
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
            moveTo(14.48f, 12f)
            lineTo(7.13f, 4.65f)
            quadTo(6.75f, 4.27f, 6.76f, 3.76f)
            reflectiveQuadTo(7.15f, 2.88f)
            reflectiveQuadTo(8.04f, 2.5f)
            reflectiveQuadTo(8.93f, 2.88f)
            lineToRelative(7.68f, 7.7f)
            quadToRelative(0.3f, 0.3f, 0.45f, 0.68f)
            reflectiveQuadTo(17.2f, 12f)
            reflectiveQuadToRelative(-0.15f, 0.75f)
            reflectiveQuadTo(16.6f, 13.43f)
            lineToRelative(-7.7f, 7.7f)
            quadTo(8.53f, 21.5f, 8.03f, 21.49f)
            reflectiveQuadTo(7.15f, 21.1f)
            reflectiveQuadTo(6.78f, 20.21f)
            reflectiveQuadTo(7.15f, 19.33f)
            lineTo(14.48f, 12f)
            close()
          }
        }
        .build()
    return _ArrowForwardIos!!
  }

private var _ArrowForwardIos: ImageVector? = null
