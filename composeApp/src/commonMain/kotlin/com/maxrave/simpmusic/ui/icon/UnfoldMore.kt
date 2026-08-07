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
val SimpIcons.UnfoldMore: ImageVector
  get() {
    if (_UnfoldMore != null) {
      return _UnfoldMore!!
    }
    _UnfoldMore =
      ImageVector.Builder(
          name = "UnfoldMore",
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
            moveTo(12f, 18.1f)
            lineToRelative(2.33f, -2.33f)
            quadToRelative(0.3f, -0.3f, 0.72f, -0.3f)
            reflectiveQuadToRelative(0.72f, 0.3f)
            reflectiveQuadToRelative(0.3f, 0.72f)
            reflectiveQuadToRelative(-0.3f, 0.73f)
            lineTo(12.7f, 20.3f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(12f, 20.58f)
            reflectiveQuadTo(11.63f, 20.51f)
            reflectiveQuadTo(11.3f, 20.3f)
            lineTo(8.23f, 17.23f)
            quadTo(7.93f, 16.93f, 7.93f, 16.5f)
            reflectiveQuadToRelative(0.3f, -0.72f)
            quadToRelative(0.3f, -0.3f, 0.73f, -0.3f)
            reflectiveQuadToRelative(0.72f, 0.3f)
            lineTo(12f, 18.1f)
            close()
            moveTo(12f, 6f)
            lineTo(9.68f, 8.32f)
            quadToRelative(-0.3f, 0.3f, -0.72f, 0.3f)
            reflectiveQuadTo(8.23f, 8.32f)
            quadTo(7.93f, 8.02f, 7.93f, 7.6f)
            quadToRelative(0f, -0.42f, 0.3f, -0.72f)
            lineTo(11.3f, 3.8f)
            quadTo(11.45f, 3.65f, 11.63f, 3.59f)
            reflectiveQuadTo(12f, 3.52f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 3.8f)
            lineToRelative(3.07f, 3.08f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.72f)
            reflectiveQuadToRelative(-0.3f, 0.73f)
            reflectiveQuadToRelative(-0.72f, 0.3f)
            reflectiveQuadTo(14.33f, 8.32f)
            lineTo(12f, 6f)
            close()
          }
        }
        .build()
    return _UnfoldMore!!
  }

private var _UnfoldMore: ImageVector? = null
