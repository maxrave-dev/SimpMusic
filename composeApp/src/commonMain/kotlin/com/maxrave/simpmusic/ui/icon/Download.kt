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
val SimpIcons.Download: ImageVector
  get() {
    if (_Download != null) {
      return _Download!!
    }
    _Download =
      ImageVector.Builder(
          name = "Download",
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
            moveTo(11.63f, 15.51f)
            quadTo(11.45f, 15.45f, 11.3f, 15.3f)
            lineTo(7.7f, 11.7f)
            quadTo(7.4f, 11.4f, 7.41f, 11f)
            reflectiveQuadTo(7.7f, 10.3f)
            quadTo(8f, 10f, 8.41f, 9.99f)
            quadTo(8.83f, 9.98f, 9.13f, 10.27f)
            lineTo(11f, 12.15f)
            verticalLineTo(5f)
            quadTo(11f, 4.57f, 11.29f, 4.29f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 5f)
            verticalLineToRelative(7.15f)
            lineToRelative(1.88f, -1.88f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.29f)
            reflectiveQuadTo(16.3f, 10.3f)
            quadToRelative(0.27f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(16.3f, 11.7f)
            lineToRelative(-3.6f, 3.6f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(12f, 15.58f)
            reflectiveQuadTo(11.63f, 15.51f)
            close()
            moveTo(6f, 20f)
            quadTo(5.18f, 20f, 4.59f, 19.41f)
            reflectiveQuadTo(4f, 18f)
            verticalLineTo(16f)
            quadTo(4f, 15.58f, 4.29f, 15.29f)
            reflectiveQuadTo(5f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(6f, 16f)
            verticalLineToRelative(2f)
            horizontalLineTo(18f)
            verticalLineTo(16f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(19f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(20f, 16f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 20f)
            horizontalLineTo(6f)
            close()
          }
        }
        .build()
    return _Download!!
  }

private var _Download: ImageVector? = null
