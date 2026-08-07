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
val SimpIcons.UnfoldLess: ImageVector
  get() {
    if (_UnfoldLess != null) {
      return _UnfoldLess!!
    }
    _UnfoldLess =
      ImageVector.Builder(
          name = "UnfoldLess",
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
            moveTo(12f, 16.9f)
            lineTo(9.6f, 19.3f)
            quadTo(9.33f, 19.58f, 8.9f, 19.58f)
            reflectiveQuadTo(8.2f, 19.3f)
            quadTo(7.93f, 19.02f, 7.93f, 18.6f)
            reflectiveQuadTo(8.2f, 17.9f)
            lineToRelative(3.1f, -3.1f)
            quadToRelative(0.15f, -0.15f, 0.32f, -0.21f)
            reflectiveQuadTo(12f, 14.53f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 14.8f)
            lineToRelative(3.1f, 3.1f)
            quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
            quadToRelative(0f, 0.42f, -0.27f, 0.7f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            reflectiveQuadTo(14.4f, 19.3f)
            lineTo(12f, 16.9f)
            close()
            moveTo(12f, 7.1f)
            lineTo(14.4f, 4.7f)
            quadTo(14.68f, 4.42f, 15.1f, 4.42f)
            reflectiveQuadTo(15.8f, 4.7f)
            quadToRelative(0.27f, 0.27f, 0.27f, 0.7f)
            reflectiveQuadTo(15.8f, 6.1f)
            lineTo(12.7f, 9.2f)
            quadTo(12.55f, 9.35f, 12.38f, 9.41f)
            reflectiveQuadTo(12f, 9.48f)
            reflectiveQuadTo(11.63f, 9.41f)
            reflectiveQuadTo(11.3f, 9.2f)
            lineTo(8.2f, 6.1f)
            quadTo(7.93f, 5.82f, 7.93f, 5.4f)
            reflectiveQuadTo(8.2f, 4.7f)
            quadTo(8.48f, 4.42f, 8.9f, 4.42f)
            reflectiveQuadTo(9.6f, 4.7f)
            lineTo(12f, 7.1f)
            close()
          }
        }
        .build()
    return _UnfoldLess!!
  }

private var _UnfoldLess: ImageVector? = null
