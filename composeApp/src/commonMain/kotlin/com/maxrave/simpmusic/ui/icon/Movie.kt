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
val SimpIcons.Movie: ImageVector
  get() {
    if (_Movie != null) {
      return _Movie!!
    }
    _Movie =
      ImageVector.Builder(
          name = "Movie",
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
            moveTo(4f, 4f)
            lineTo(5.63f, 7.25f)
            quadTo(5.8f, 7.6f, 6.13f, 7.8f)
            reflectiveQuadTo(6.83f, 8f)
            quadTo(7.58f, 8f, 7.98f, 7.36f)
            quadTo(8.38f, 6.72f, 8.03f, 6.05f)
            lineTo(7f, 4f)
            horizontalLineTo(9f)
            lineToRelative(1.63f, 3.25f)
            quadTo(10.8f, 7.6f, 11.13f, 7.8f)
            reflectiveQuadTo(11.83f, 8f)
            quadToRelative(0.75f, 0f, 1.15f, -0.64f)
            quadToRelative(0.4f, -0.64f, 0.05f, -1.31f)
            lineTo(12f, 4f)
            horizontalLineToRelative(2f)
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
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            close()
            moveToRelative(0f, 6f)
            verticalLineToRelative(8f)
            horizontalLineTo(20f)
            verticalLineTo(10f)
            horizontalLineTo(4f)
            close()
            moveToRelative(0f, 0f)
            verticalLineToRelative(8f)
            verticalLineTo(10f)
            close()
          }
        }
        .build()
    return _Movie!!
  }

private var _Movie: ImageVector? = null
