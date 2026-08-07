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
val SimpIcons.Fullscreen: ImageVector
  get() {
    if (_Fullscreen != null) {
      return _Fullscreen!!
    }
    _Fullscreen =
      ImageVector.Builder(
          name = "Fullscreen",
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
            moveTo(5f, 19f)
            horizontalLineTo(7f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(8f, 20f)
            reflectiveQuadTo(7.71f, 20.71f)
            reflectiveQuadTo(7f, 21f)
            horizontalLineTo(4f)
            quadTo(3.58f, 21f, 3.29f, 20.71f)
            quadTo(3f, 20.43f, 3f, 20f)
            verticalLineTo(17f)
            quadTo(3f, 16.58f, 3.29f, 16.29f)
            reflectiveQuadTo(4f, 16f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 17f)
            verticalLineToRelative(2f)
            close()
            moveToRelative(14f, 0f)
            verticalLineTo(17f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(20f, 16f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 17f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 21f)
            horizontalLineTo(17f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            quadTo(16f, 20.43f, 16f, 20f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(17f, 19f)
            horizontalLineToRelative(2f)
            close()
            moveTo(5f, 5f)
            verticalLineTo(7f)
            quadTo(5f, 7.43f, 4.71f, 7.71f)
            reflectiveQuadTo(4f, 8f)
            reflectiveQuadTo(3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            verticalLineTo(4f)
            quadTo(3f, 3.57f, 3.29f, 3.29f)
            reflectiveQuadTo(4f, 3f)
            horizontalLineTo(7f)
            quadTo(7.43f, 3f, 7.71f, 3.29f)
            reflectiveQuadTo(8f, 4f)
            quadTo(8f, 4.42f, 7.71f, 4.71f)
            reflectiveQuadTo(7f, 5f)
            horizontalLineTo(5f)
            close()
            moveTo(19f, 5f)
            horizontalLineTo(17f)
            quadTo(16.58f, 5f, 16.29f, 4.71f)
            reflectiveQuadTo(16f, 4f)
            quadTo(16f, 3.57f, 16.29f, 3.29f)
            reflectiveQuadTo(17f, 3f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 4f)
            verticalLineTo(7f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 8f)
            reflectiveQuadTo(19.29f, 7.71f)
            quadTo(19f, 7.43f, 19f, 7f)
            verticalLineTo(5f)
            close()
          }
        }
        .build()
    return _Fullscreen!!
  }

private var _Fullscreen: ImageVector? = null
