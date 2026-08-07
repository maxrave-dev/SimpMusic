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
val SimpIcons.Home: ImageVector
  get() {
    if (_Home != null) {
      return _Home!!
    }
    _Home =
      ImageVector.Builder(
          name = "Home",
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
            moveTo(4f, 19f)
            verticalLineTo(10f)
            quadTo(4f, 9.52f, 4.21f, 9.1f)
            quadTo(4.43f, 8.67f, 4.8f, 8.4f)
            lineToRelative(6f, -4.5f)
            quadTo(11.33f, 3.5f, 12f, 3.5f)
            reflectiveQuadToRelative(1.2f, 0.4f)
            lineToRelative(6f, 4.5f)
            quadToRelative(0.38f, 0.28f, 0.59f, 0.7f)
            quadTo(20f, 9.52f, 20f, 10f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 21f)
            horizontalLineTo(15f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(14f, 20.43f, 14f, 20f)
            verticalLineTo(15f)
            quadToRelative(0f, -0.43f, -0.29f, -0.71f)
            reflectiveQuadTo(13f, 14f)
            horizontalLineTo(11f)
            quadToRelative(-0.42f, 0f, -0.71f, 0.29f)
            reflectiveQuadTo(10f, 15f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(9f, 21f)
            horizontalLineTo(6f)
            quadTo(5.18f, 21f, 4.59f, 20.41f)
            reflectiveQuadTo(4f, 19f)
            close()
          }
        }
        .build()
    return _Home!!
  }

private var _Home: ImageVector? = null
