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
val SimpIcons.Speed: ImageVector
  get() {
    if (_Speed != null) {
      return _Speed!!
    }
    _Speed =
      ImageVector.Builder(
          name = "Speed",
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
            moveTo(10.45f, 15.5f)
            quadToRelative(0.63f, 0.63f, 1.57f, 0.59f)
            reflectiveQuadTo(13.4f, 15.4f)
            lineTo(17.63f, 9.07f)
            quadTo(17.85f, 8.73f, 17.56f, 8.44f)
            quadTo(17.28f, 8.15f, 16.93f, 8.38f)
            lineTo(10.6f, 12.6f)
            quadTo(9.95f, 13.05f, 9.89f, 13.96f)
            quadToRelative(-0.06f, 0.91f, 0.56f, 1.54f)
            close()
            moveTo(5.1f, 20f)
            quadTo(4.55f, 20f, 4.09f, 19.76f)
            reflectiveQuadTo(3.35f, 19.05f)
            quadTo(2.7f, 17.88f, 2.35f, 16.61f)
            reflectiveQuadTo(2f, 14f)
            quadTo(2f, 11.93f, 2.79f, 10.1f)
            quadTo(3.58f, 8.27f, 4.93f, 6.93f)
            quadTo(6.28f, 5.57f, 8.1f, 4.79f)
            quadTo(9.93f, 4f, 12f, 4f)
            quadToRelative(2.05f, 0f, 3.85f, 0.77f)
            reflectiveQuadTo(19f, 6.89f)
            quadToRelative(1.35f, 1.34f, 2.15f, 3.13f)
            reflectiveQuadToRelative(0.82f, 3.84f)
            quadTo(22f, 15.23f, 21.66f, 16.54f)
            reflectiveQuadToRelative(-1.04f, 2.51f)
            quadToRelative(-0.27f, 0.47f, -0.74f, 0.71f)
            reflectiveQuadTo(18.88f, 20f)
            horizontalLineTo(5.1f)
            close()
          }
        }
        .build()
    return _Speed!!
  }

private var _Speed: ImageVector? = null
