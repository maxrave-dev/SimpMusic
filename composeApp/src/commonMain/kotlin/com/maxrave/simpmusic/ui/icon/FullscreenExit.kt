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
val SimpIcons.FullscreenExit: ImageVector
  get() {
    if (_FullscreenExit != null) {
      return _FullscreenExit!!
    }
    _FullscreenExit =
      ImageVector.Builder(
          name = "FullscreenExit",
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
            moveTo(6f, 18f)
            horizontalLineTo(4f)
            quadTo(3.58f, 18f, 3.29f, 17.71f)
            quadTo(3f, 17.43f, 3f, 17f)
            reflectiveQuadTo(3.29f, 16.29f)
            reflectiveQuadTo(4f, 16f)
            horizontalLineTo(7f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(8f, 17f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(7f, 21f)
            quadTo(6.58f, 21f, 6.29f, 20.71f)
            quadTo(6f, 20.43f, 6f, 20f)
            verticalLineTo(18f)
            close()
            moveToRelative(12f, 0f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 21f)
            reflectiveQuadTo(16.29f, 20.71f)
            quadTo(16f, 20.43f, 16f, 20f)
            verticalLineTo(17f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(17f, 16f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 17f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 18f)
            horizontalLineTo(18f)
            close()
            moveTo(6f, 6f)
            verticalLineTo(4f)
            quadTo(6f, 3.57f, 6.29f, 3.29f)
            reflectiveQuadTo(7f, 3f)
            reflectiveQuadTo(7.71f, 3.29f)
            reflectiveQuadTo(8f, 4f)
            verticalLineTo(7f)
            quadTo(8f, 7.43f, 7.71f, 7.71f)
            reflectiveQuadTo(7f, 8f)
            horizontalLineTo(4f)
            quadTo(3.58f, 8f, 3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            reflectiveQuadTo(3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(6f)
            close()
            moveTo(18f, 6f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 7f)
            reflectiveQuadTo(20.71f, 7.71f)
            reflectiveQuadTo(20f, 8f)
            horizontalLineTo(17f)
            quadTo(16.58f, 8f, 16.29f, 7.71f)
            quadTo(16f, 7.43f, 16f, 7f)
            verticalLineTo(4f)
            quadTo(16f, 3.57f, 16.29f, 3.29f)
            reflectiveQuadTo(17f, 3f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 4f)
            verticalLineTo(6f)
            close()
          }
        }
        .build()
    return _FullscreenExit!!
  }

private var _FullscreenExit: ImageVector? = null
