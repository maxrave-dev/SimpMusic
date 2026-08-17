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
val SimpIcons.PictureInPictureAlt: ImageVector
  get() {
    if (_PictureInPictureAlt != null) {
      return _PictureInPictureAlt!!
    }
    _PictureInPictureAlt =
      ImageVector.Builder(
          name = "PictureInPictureAlt",
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
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 18f)
            horizontalLineTo(20f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            close()
            moveToRelative(0f, 0f)
            verticalLineTo(6f)
            verticalLineTo(18f)
            close()
            moveToRelative(8f, -1f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(19f, 16.43f, 19f, 16f)
            verticalLineTo(12f)
            quadToRelative(0f, -0.43f, -0.29f, -0.71f)
            reflectiveQuadTo(18f, 11f)
            horizontalLineTo(12f)
            quadToRelative(-0.42f, 0f, -0.71f, 0.29f)
            reflectiveQuadTo(11f, 12f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 17f)
            close()
            moveToRelative(1f, -2f)
            verticalLineTo(13f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineTo(13f)
            close()
          }
        }
        .build()
    return _PictureInPictureAlt!!
  }

private var _PictureInPictureAlt: ImageVector? = null
