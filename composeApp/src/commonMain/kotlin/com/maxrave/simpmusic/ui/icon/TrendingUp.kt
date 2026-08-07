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
val SimpIcons.TrendingUp: ImageVector
  get() {
    if (_TrendingUp != null) {
      return _TrendingUp!!
    }
    _TrendingUp =
      ImageVector.Builder(
          name = "TrendingUp",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
          autoMirror = true,
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
            moveTo(2.7f, 17.63f)
            quadTo(2.4f, 17.33f, 2.41f, 16.91f)
            quadTo(2.43f, 16.5f, 2.7f, 16.23f)
            lineTo(7.98f, 10.88f)
            quadTo(8.55f, 10.3f, 9.4f, 10.3f)
            reflectiveQuadToRelative(1.43f, 0.58f)
            lineToRelative(2.57f, 2.6f)
            lineTo(18.6f, 8.32f)
            horizontalLineTo(17f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            reflectiveQuadTo(16f, 7.32f)
            reflectiveQuadTo(16.29f, 6.61f)
            quadTo(16.58f, 6.32f, 17f, 6.32f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 7.32f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(21f, 12.33f)
            reflectiveQuadTo(20.29f, 12.04f)
            reflectiveQuadTo(20f, 11.33f)
            verticalLineTo(9.73f)
            lineTo(14.83f, 14.9f)
            quadToRelative(-0.58f, 0.57f, -1.43f, 0.57f)
            reflectiveQuadTo(11.98f, 14.9f)
            lineTo(9.4f, 12.33f)
            lineToRelative(-5.3f, 5.3f)
            quadTo(3.83f, 17.9f, 3.4f, 17.9f)
            reflectiveQuadTo(2.7f, 17.63f)
            close()
          }
        }
        .build()
    return _TrendingUp!!
  }

private var _TrendingUp: ImageVector? = null
