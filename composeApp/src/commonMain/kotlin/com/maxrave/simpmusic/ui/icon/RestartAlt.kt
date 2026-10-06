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
val SimpIcons.RestartAlt: ImageVector
  get() {
    if (_RestartAlt != null) {
      return _RestartAlt!!
    }
    _RestartAlt =
      ImageVector.Builder(
          name = "RestartAlt",
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
            moveTo(9.83f, 20.7f)
            quadTo(7.25f, 19.98f, 5.63f, 17.86f)
            reflectiveQuadTo(4f, 13f)
            quadTo(4f, 11.58f, 4.48f, 10.29f)
            reflectiveQuadTo(5.83f, 7.93f)
            quadTo(6.1f, 7.63f, 6.5f, 7.61f)
            reflectiveQuadTo(7.23f, 7.93f)
            quadTo(7.5f, 8.2f, 7.51f, 8.6f)
            reflectiveQuadTo(7.25f, 9.35f)
            quadToRelative(-0.6f, 0.78f, -0.92f, 1.7f)
            quadTo(6f, 11.98f, 6f, 13f)
            quadToRelative(0f, 2.03f, 1.19f, 3.61f)
            reflectiveQuadToRelative(3.06f, 2.16f)
            quadToRelative(0.33f, 0.1f, 0.54f, 0.38f)
            quadTo(11f, 19.43f, 11f, 19.75f)
            quadToRelative(0f, 0.5f, -0.35f, 0.79f)
            reflectiveQuadTo(9.83f, 20.7f)
            close()
            moveToRelative(4.35f, 0f)
            quadTo(13.7f, 20.83f, 13.35f, 20.53f)
            quadTo(13f, 20.23f, 13f, 19.73f)
            quadToRelative(0f, -0.3f, 0.21f, -0.58f)
            quadToRelative(0.21f, -0.27f, 0.54f, -0.38f)
            quadToRelative(1.88f, -0.6f, 3.06f, -2.17f)
            reflectiveQuadTo(18f, 13f)
            quadTo(18f, 10.5f, 16.25f, 8.75f)
            reflectiveQuadTo(12f, 7f)
            horizontalLineTo(11.93f)
            lineToRelative(0.4f, 0.4f)
            quadTo(12.6f, 7.68f, 12.6f, 8.1f)
            reflectiveQuadTo(12.33f, 8.8f)
            reflectiveQuadToRelative(-0.7f, 0.28f)
            reflectiveQuadTo(10.93f, 8.8f)
            lineTo(8.83f, 6.7f)
            quadTo(8.68f, 6.55f, 8.61f, 6.38f)
            reflectiveQuadTo(8.55f, 6f)
            reflectiveQuadTo(8.61f, 5.63f)
            reflectiveQuadTo(8.83f, 5.3f)
            lineToRelative(2.1f, -2.1f)
            quadTo(11.2f, 2.92f, 11.63f, 2.92f)
            reflectiveQuadToRelative(0.7f, 0.28f)
            quadTo(12.6f, 3.47f, 12.6f, 3.9f)
            reflectiveQuadTo(12.33f, 4.6f)
            lineTo(11.93f, 5f)
            horizontalLineTo(12f)
            quadToRelative(3.35f, 0f, 5.68f, 2.32f)
            reflectiveQuadTo(20f, 13f)
            quadToRelative(0f, 2.72f, -1.63f, 4.85f)
            reflectiveQuadToRelative(-4.2f, 2.85f)
            close()
          }
        }
        .build()
    return _RestartAlt!!
  }

private var _RestartAlt: ImageVector? = null
