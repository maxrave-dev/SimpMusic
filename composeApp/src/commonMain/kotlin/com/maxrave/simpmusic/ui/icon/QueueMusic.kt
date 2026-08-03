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
val SimpIcons.QueueMusic: ImageVector
  get() {
    if (_QueueMusic != null) {
      return _QueueMusic!!
    }
    _QueueMusic =
      ImageVector.Builder(
          name = "QueueMusic",
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
            moveTo(16f, 20f)
            quadToRelative(-1.25f, 0f, -2.13f, -0.88f)
            reflectiveQuadTo(13f, 17f)
            reflectiveQuadToRelative(0.88f, -2.13f)
            reflectiveQuadTo(16f, 14f)
            quadToRelative(0.28f, 0f, 0.53f, 0.04f)
            reflectiveQuadTo(17f, 14.2f)
            verticalLineTo(7f)
            quadTo(17f, 6.57f, 17.29f, 6.29f)
            reflectiveQuadTo(18f, 6f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 7f)
            reflectiveQuadTo(21.71f, 7.71f)
            reflectiveQuadTo(21f, 8f)
            horizontalLineTo(19f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 1.25f, -0.88f, 2.13f)
            reflectiveQuadTo(16f, 20f)
            close()
            moveTo(4f, 16f)
            quadTo(3.58f, 16f, 3.29f, 15.71f)
            reflectiveQuadTo(3f, 15f)
            reflectiveQuadTo(3.29f, 14.29f)
            reflectiveQuadTo(4f, 14f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 15f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 16f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 12f)
            quadTo(3.58f, 12f, 3.29f, 11.71f)
            quadTo(3f, 11.43f, 3f, 11f)
            reflectiveQuadTo(3.29f, 10.29f)
            reflectiveQuadTo(4f, 10f)
            horizontalLineTo(14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 11f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(14f, 12f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 8f)
            quadTo(3.58f, 8f, 3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            reflectiveQuadTo(3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 7f)
            reflectiveQuadTo(14.71f, 7.71f)
            reflectiveQuadTo(14f, 8f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _QueueMusic!!
  }

private var _QueueMusic: ImageVector? = null
