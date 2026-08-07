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
val SimpIcons.ThumbUp: ImageVector
  get() {
    if (_ThumbUp != null) {
      return _ThumbUp!!
    }
    _ThumbUp =
      ImageVector.Builder(
          name = "ThumbUp",
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
            moveTo(21f, 8f)
            quadToRelative(0.8f, 0f, 1.4f, 0.6f)
            reflectiveQuadTo(23f, 10f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.17f, -0.04f, 0.38f)
            reflectiveQuadToRelative(-0.11f, 0.38f)
            lineToRelative(-3f, 7.05f)
            quadToRelative(-0.23f, 0.5f, -0.75f, 0.85f)
            reflectiveQuadTo(18f, 21f)
            horizontalLineTo(10f)
            quadTo(9.18f, 21f, 8.59f, 20.41f)
            reflectiveQuadTo(8f, 19f)
            verticalLineTo(8.82f)
            quadTo(8f, 8.42f, 8.16f, 8.06f)
            quadTo(8.33f, 7.7f, 8.6f, 7.43f)
            lineToRelative(5.43f, -5.4f)
            quadTo(14.4f, 1.67f, 14.91f, 1.6f)
            reflectiveQuadTo(15.9f, 1.77f)
            reflectiveQuadToRelative(0.69f, 0.7f)
            reflectiveQuadTo(16.68f, 3.4f)
            lineTo(15.55f, 8f)
            horizontalLineTo(21f)
            close()
            moveTo(4f, 21f)
            quadTo(3.18f, 21f, 2.59f, 20.41f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(10f)
            quadTo(2f, 9.17f, 2.59f, 8.59f)
            reflectiveQuadTo(4f, 8f)
            quadTo(4.83f, 8f, 5.41f, 8.59f)
            reflectiveQuadTo(6f, 10f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(4f, 21f)
            close()
          }
        }
        .build()
    return _ThumbUp!!
  }

private var _ThumbUp: ImageVector? = null
