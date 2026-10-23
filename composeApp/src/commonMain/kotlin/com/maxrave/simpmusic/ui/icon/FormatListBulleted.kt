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
val SimpIcons.FormatListBulleted: ImageVector
  get() {
    if (_FormatListBulleted != null) {
      return _FormatListBulleted!!
    }
    _FormatListBulleted =
      ImageVector.Builder(
          name = "FormatListBulleted",
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
            moveTo(10f, 19f)
            quadTo(9.58f, 19f, 9.29f, 18.71f)
            quadTo(9f, 18.43f, 9f, 18f)
            reflectiveQuadTo(9.29f, 17.29f)
            quadTo(9.58f, 17f, 10f, 17f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 19f)
            horizontalLineTo(10f)
            close()
            moveToRelative(0f, -6f)
            quadTo(9.58f, 13f, 9.29f, 12.71f)
            quadTo(9f, 12.43f, 9f, 12f)
            reflectiveQuadTo(9.29f, 11.29f)
            quadTo(9.58f, 11f, 10f, 11f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 13f)
            horizontalLineTo(10f)
            close()
            moveTo(10f, 7f)
            quadTo(9.58f, 7f, 9.29f, 6.71f)
            quadTo(9f, 6.43f, 9f, 6f)
            reflectiveQuadTo(9.29f, 5.29f)
            quadTo(9.58f, 5f, 10f, 5f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 6f)
            reflectiveQuadTo(20.71f, 6.71f)
            reflectiveQuadTo(20f, 7f)
            horizontalLineTo(10f)
            close()
            moveTo(5f, 20f)
            quadTo(4.18f, 20f, 3.59f, 19.41f)
            reflectiveQuadTo(3f, 18f)
            reflectiveQuadTo(3.59f, 16.59f)
            reflectiveQuadTo(5f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(7f, 17.18f, 7f, 18f)
            reflectiveQuadTo(6.41f, 19.41f)
            reflectiveQuadTo(5f, 20f)
            close()
            moveTo(5f, 14f)
            quadTo(4.18f, 14f, 3.59f, 13.41f)
            reflectiveQuadTo(3f, 12f)
            reflectiveQuadTo(3.59f, 10.59f)
            reflectiveQuadTo(5f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(7f, 11.18f, 7f, 12f)
            reflectiveQuadTo(6.41f, 13.41f)
            reflectiveQuadTo(5f, 14f)
            close()
            moveTo(3.59f, 7.41f)
            quadTo(3f, 6.82f, 3f, 6f)
            reflectiveQuadTo(3.59f, 4.59f)
            reflectiveQuadTo(5f, 4f)
            reflectiveQuadTo(6.41f, 4.59f)
            quadTo(7f, 5.18f, 7f, 6f)
            reflectiveQuadTo(6.41f, 7.41f)
            reflectiveQuadTo(5f, 8f)
            reflectiveQuadTo(3.59f, 7.41f)
            close()
          }
        }
        .buildSimpIcon()
    return _FormatListBulleted!!
  }

private var _FormatListBulleted: ImageVector? = null
