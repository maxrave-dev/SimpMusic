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
val SimpIcons.SkipNext: ImageVector
  get() {
    if (_SkipNext != null) {
      return _SkipNext!!
    }
    _SkipNext =
      ImageVector.Builder(
          name = "SkipNext",
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
            moveTo(16.5f, 17f)
            verticalLineTo(7f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(17.5f, 6f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18.5f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17.5f, 18f)
            reflectiveQuadTo(16.79f, 17.71f)
            quadTo(16.5f, 17.43f, 16.5f, 17f)
            close()
            moveTo(5.5f, 16.13f)
            verticalLineTo(7.88f)
            quadTo(5.5f, 7.43f, 5.8f, 7.15f)
            quadTo(6.1f, 6.88f, 6.5f, 6.88f)
            quadToRelative(0.13f, 0f, 0.28f, 0.02f)
            quadTo(6.93f, 6.93f, 7.05f, 7.02f)
            lineToRelative(6.2f, 4.15f)
            quadToRelative(0.23f, 0.15f, 0.34f, 0.36f)
            reflectiveQuadTo(13.7f, 12f)
            reflectiveQuadToRelative(-0.11f, 0.46f)
            quadToRelative(-0.11f, 0.21f, -0.34f, 0.36f)
            lineToRelative(-6.2f, 4.15f)
            quadTo(6.93f, 17.08f, 6.78f, 17.1f)
            reflectiveQuadTo(6.5f, 17.13f)
            quadToRelative(-0.4f, 0f, -0.7f, -0.27f)
            reflectiveQuadTo(5.5f, 16.13f)
            close()
          }
        }
        .build()
    return _SkipNext!!
  }

private var _SkipNext: ImageVector? = null
