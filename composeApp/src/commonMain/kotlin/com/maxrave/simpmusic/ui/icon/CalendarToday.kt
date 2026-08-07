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
val SimpIcons.CalendarToday: ImageVector
  get() {
    if (_CalendarToday != null) {
      return _CalendarToday!!
    }
    _CalendarToday =
      ImageVector.Builder(
          name = "CalendarToday",
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
            moveTo(5f, 22f)
            quadTo(4.18f, 22f, 3.59f, 21.41f)
            reflectiveQuadTo(3f, 20f)
            verticalLineTo(6f)
            quadTo(3f, 5.18f, 3.59f, 4.59f)
            reflectiveQuadTo(5f, 4f)
            horizontalLineTo(6f)
            verticalLineTo(3f)
            quadTo(6f, 2.57f, 6.29f, 2.29f)
            reflectiveQuadTo(7f, 2f)
            reflectiveQuadTo(7.71f, 2.29f)
            reflectiveQuadTo(8f, 3f)
            verticalLineTo(4f)
            horizontalLineToRelative(8f)
            verticalLineTo(3f)
            quadTo(16f, 2.57f, 16.29f, 2.29f)
            reflectiveQuadTo(17f, 2f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 3f)
            verticalLineTo(4f)
            horizontalLineToRelative(1f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(21f, 5.18f, 21f, 6f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 22f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 20f)
            horizontalLineTo(19f)
            verticalLineTo(10f)
            horizontalLineTo(5f)
            verticalLineTo(20f)
            close()
          }
        }
        .build()
    return _CalendarToday!!
  }

private var _CalendarToday: ImageVector? = null
