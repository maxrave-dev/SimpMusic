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
val SimpIcons.Explicit: ImageVector
  get() {
    if (_Explicit != null) {
      return _Explicit!!
    }
    _Explicit =
      ImageVector.Builder(
          name = "Explicit",
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
            moveTo(11f, 15f)
            verticalLineTo(13f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(15f, 12.43f, 15f, 12f)
            reflectiveQuadTo(14.71f, 11.29f)
            reflectiveQuadTo(14f, 11f)
            horizontalLineTo(11f)
            verticalLineTo(9f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            reflectiveQuadTo(15f, 8f)
            quadTo(15f, 7.57f, 14.71f, 7.29f)
            reflectiveQuadTo(14f, 7f)
            horizontalLineTo(10f)
            quadTo(9.58f, 7f, 9.29f, 7.29f)
            reflectiveQuadTo(9f, 8f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            quadTo(9.58f, 17f, 10f, 17f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(15f, 16.43f, 15f, 16f)
            reflectiveQuadTo(14.71f, 15.29f)
            reflectiveQuadTo(14f, 15f)
            horizontalLineTo(11f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build()
    return _Explicit!!
  }

private var _Explicit: ImageVector? = null
