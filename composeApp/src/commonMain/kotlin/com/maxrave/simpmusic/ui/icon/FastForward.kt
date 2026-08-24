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
val SimpIcons.FastForward: ImageVector
  get() {
    if (_FastForward != null) {
      return _FastForward!!
    }
    _FastForward =
      ImageVector.Builder(
          name = "FastForward",
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
            moveTo(2.5f, 16.13f)
            verticalLineTo(7.88f)
            quadTo(2.5f, 7.43f, 2.8f, 7.15f)
            quadTo(3.1f, 6.88f, 3.5f, 6.88f)
            quadToRelative(0.13f, 0f, 0.28f, 0.02f)
            quadTo(3.93f, 6.93f, 4.05f, 7.02f)
            lineToRelative(6.2f, 4.15f)
            quadToRelative(0.23f, 0.15f, 0.34f, 0.36f)
            reflectiveQuadTo(10.7f, 12f)
            reflectiveQuadToRelative(-0.11f, 0.46f)
            quadToRelative(-0.11f, 0.21f, -0.34f, 0.36f)
            lineToRelative(-6.2f, 4.15f)
            quadTo(3.93f, 17.08f, 3.78f, 17.1f)
            reflectiveQuadTo(3.5f, 17.13f)
            quadToRelative(-0.4f, 0f, -0.7f, -0.27f)
            reflectiveQuadTo(2.5f, 16.13f)
            close()
            moveToRelative(10f, 0f)
            verticalLineTo(7.88f)
            quadToRelative(0f, -0.45f, 0.3f, -0.73f)
            quadTo(13.1f, 6.88f, 13.5f, 6.88f)
            quadToRelative(0.13f, 0f, 0.28f, 0.02f)
            quadToRelative(0.15f, 0.03f, 0.28f, 0.13f)
            lineToRelative(6.2f, 4.15f)
            quadToRelative(0.23f, 0.15f, 0.34f, 0.36f)
            reflectiveQuadTo(20.7f, 12f)
            reflectiveQuadToRelative(-0.11f, 0.46f)
            quadToRelative(-0.11f, 0.21f, -0.34f, 0.36f)
            lineToRelative(-6.2f, 4.15f)
            quadToRelative(-0.13f, 0.1f, -0.28f, 0.13f)
            reflectiveQuadTo(13.5f, 17.13f)
            quadToRelative(-0.4f, 0f, -0.7f, -0.27f)
            reflectiveQuadTo(12.5f, 16.13f)
            close()
          }
        }
        .build()
    return _FastForward!!
  }

private var _FastForward: ImageVector? = null
