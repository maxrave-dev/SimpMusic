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
val SimpIcons.MovieOff: ImageVector
  get() {
    if (_MovieOff != null) {
      return _MovieOff!!
    }
    _MovieOff =
      ImageVector.Builder(
          name = "MovieOff",
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
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            lineToRelative(6f, 6f)
            horizontalLineTo(4f)
            verticalLineToRelative(8f)
            horizontalLineTo(18f)
            lineToRelative(2f, 2f)
            horizontalLineTo(4f)
            close()
            moveTo(21.78f, 18.9f)
            lineTo(20f, 17.13f)
            verticalLineTo(10f)
            horizontalLineTo(12.85f)
            lineToRelative(-6f, -6f)
            horizontalLineTo(8.58f)
            lineToRelative(1.65f, 3.27f)
            quadToRelative(0.17f, 0.35f, 0.5f, 0.54f)
            reflectiveQuadTo(11.43f, 8f)
            quadToRelative(0.75f, 0f, 1.14f, -0.63f)
            reflectiveQuadTo(12.6f, 6.07f)
            lineTo(11.58f, 4f)
            horizontalLineTo(14f)
            lineToRelative(1.63f, 3.25f)
            quadTo(15.8f, 7.6f, 16.13f, 7.8f)
            reflectiveQuadTo(16.83f, 8f)
            quadToRelative(0.75f, 0f, 1.15f, -0.64f)
            quadToRelative(0.4f, -0.64f, 0.05f, -1.31f)
            lineTo(17f, 4f)
            horizontalLineToRelative(3f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.25f, -0.05f, 0.48f)
            reflectiveQuadTo(21.78f, 18.9f)
            close()
            moveToRelative(-2.7f, 3f)
            lineTo(2.08f, 4.93f)
            quadTo(1.78f, 4.63f, 1.78f, 4.21f)
            reflectiveQuadTo(2.08f, 3.5f)
            reflectiveQuadTo(2.79f, 3.2f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineToRelative(17f, 17f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.7f)
            reflectiveQuadToRelative(-0.3f, 0.7f)
            reflectiveQuadToRelative(-0.71f, 0.3f)
            reflectiveQuadTo(19.08f, 21.9f)
            close()
            moveTo(11.15f, 14f)
            close()
            moveToRelative(5.28f, -0.45f)
            close()
          }
        }
        .build()
    return _MovieOff!!
  }

private var _MovieOff: ImageVector? = null
