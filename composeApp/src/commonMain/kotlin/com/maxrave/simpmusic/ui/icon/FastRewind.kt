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
val SimpIcons.FastRewind: ImageVector
  get() {
    if (_FastRewind != null) {
      return _FastRewind!!
    }
    _FastRewind =
      ImageVector.Builder(
          name = "FastRewind",
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
            moveTo(19.95f, 16.98f)
            lineToRelative(-6.2f, -4.15f)
            quadTo(13.53f, 12.68f, 13.41f, 12.46f)
            reflectiveQuadTo(13.3f, 12f)
            reflectiveQuadToRelative(0.11f, -0.46f)
            reflectiveQuadToRelative(0.34f, -0.36f)
            lineToRelative(6.2f, -4.15f)
            quadTo(20.08f, 6.93f, 20.23f, 6.9f)
            quadTo(20.38f, 6.88f, 20.5f, 6.88f)
            quadToRelative(0.4f, 0f, 0.7f, 0.27f)
            quadToRelative(0.3f, 0.28f, 0.3f, 0.73f)
            verticalLineToRelative(8.25f)
            quadToRelative(0f, 0.45f, -0.3f, 0.73f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            quadToRelative(-0.13f, 0f, -0.27f, -0.02f)
            reflectiveQuadTo(19.95f, 16.98f)
            close()
            moveToRelative(-10f, 0f)
            lineTo(3.75f, 12.83f)
            quadTo(3.53f, 12.68f, 3.41f, 12.46f)
            reflectiveQuadTo(3.3f, 12f)
            reflectiveQuadTo(3.41f, 11.54f)
            reflectiveQuadTo(3.75f, 11.18f)
            lineTo(9.95f, 7.02f)
            quadTo(10.08f, 6.93f, 10.23f, 6.9f)
            quadTo(10.38f, 6.88f, 10.5f, 6.88f)
            quadToRelative(0.4f, 0f, 0.7f, 0.27f)
            quadToRelative(0.3f, 0.28f, 0.3f, 0.73f)
            verticalLineToRelative(8.25f)
            quadToRelative(0f, 0.45f, -0.3f, 0.73f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            quadToRelative(-0.13f, 0f, -0.27f, -0.02f)
            reflectiveQuadTo(9.95f, 16.98f)
            close()
          }
        }
        .build()
    return _FastRewind!!
  }

private var _FastRewind: ImageVector? = null
