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
val SimpIcons.Headphones: ImageVector
  get() {
    if (_Headphones != null) {
      return _Headphones!!
    }
    _Headphones =
      ImageVector.Builder(
          name = "Headphones",
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
            moveTo(7f, 21f)
            horizontalLineTo(5f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(12f)
            quadTo(3f, 10.13f, 3.71f, 8.49f)
            reflectiveQuadTo(5.64f, 5.64f)
            quadTo(6.85f, 4.42f, 8.49f, 3.71f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadToRelative(3.51f, 0.71f)
            reflectiveQuadToRelative(2.85f, 1.93f)
            reflectiveQuadToRelative(1.93f, 2.85f)
            reflectiveQuadTo(21f, 12f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(17f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(15f, 19f)
            verticalLineTo(15f)
            quadToRelative(0f, -0.83f, 0.59f, -1.41f)
            reflectiveQuadTo(17f, 13f)
            horizontalLineToRelative(2f)
            verticalLineTo(12f)
            quadTo(19f, 9.07f, 16.96f, 7.04f)
            reflectiveQuadTo(12f, 5f)
            quadTo(9.08f, 5f, 7.04f, 7.04f)
            reflectiveQuadTo(5f, 12f)
            verticalLineToRelative(1f)
            horizontalLineTo(7f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(9f, 14.18f, 9f, 15f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            quadTo(7.83f, 21f, 7f, 21f)
            close()
          }
        }
        .build()
    return _Headphones!!
  }

private var _Headphones: ImageVector? = null
