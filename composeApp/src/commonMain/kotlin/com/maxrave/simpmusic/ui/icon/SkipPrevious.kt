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
val SimpIcons.SkipPrevious: ImageVector
  get() {
    if (_SkipPrevious != null) {
      return _SkipPrevious!!
    }
    _SkipPrevious =
      ImageVector.Builder(
          name = "SkipPrevious",
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
            moveTo(5.5f, 17f)
            verticalLineTo(7f)
            quadTo(5.5f, 6.57f, 5.79f, 6.29f)
            reflectiveQuadTo(6.5f, 6f)
            reflectiveQuadTo(7.21f, 6.29f)
            reflectiveQuadTo(7.5f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(6.5f, 18f)
            quadTo(6.08f, 18f, 5.79f, 17.71f)
            quadTo(5.5f, 17.43f, 5.5f, 17f)
            close()
            moveTo(16.95f, 16.98f)
            lineToRelative(-6.2f, -4.15f)
            quadTo(10.53f, 12.68f, 10.41f, 12.46f)
            reflectiveQuadTo(10.3f, 12f)
            reflectiveQuadToRelative(0.11f, -0.46f)
            reflectiveQuadToRelative(0.34f, -0.36f)
            lineToRelative(6.2f, -4.15f)
            quadTo(17.08f, 6.93f, 17.23f, 6.9f)
            quadTo(17.38f, 6.88f, 17.5f, 6.88f)
            quadToRelative(0.4f, 0f, 0.7f, 0.27f)
            quadToRelative(0.3f, 0.28f, 0.3f, 0.73f)
            verticalLineToRelative(8.25f)
            quadToRelative(0f, 0.45f, -0.3f, 0.73f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            quadToRelative(-0.13f, 0f, -0.27f, -0.02f)
            reflectiveQuadTo(16.95f, 16.98f)
            close()
          }
        }
        .build()
    return _SkipPrevious!!
  }

private var _SkipPrevious: ImageVector? = null
