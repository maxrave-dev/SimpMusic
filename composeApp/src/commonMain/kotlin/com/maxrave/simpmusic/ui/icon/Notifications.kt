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
val SimpIcons.Notifications: ImageVector
  get() {
    if (_Notifications != null) {
      return _Notifications!!
    }
    _Notifications =
      ImageVector.Builder(
          name = "Notifications",
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
            quadTo(4.58f, 19f, 4.29f, 18.71f)
            quadTo(4f, 18.43f, 4f, 18f)
            reflectiveQuadTo(4.29f, 17.29f)
            reflectiveQuadTo(5f, 17f)
            horizontalLineTo(6f)
            verticalLineTo(10f)
            quadTo(6f, 7.93f, 7.25f, 6.31f)
            reflectiveQuadTo(10.5f, 4.2f)
            verticalLineTo(3.5f)
            quadToRelative(0f, -0.63f, 0.44f, -1.06f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(13.5f, 3.5f)
            verticalLineTo(4.2f)
            quadToRelative(2f, 0.5f, 3.25f, 2.11f)
            reflectiveQuadTo(18f, 10f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(1f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(19f, 19f)
            horizontalLineTo(5f)
            close()
            moveToRelative(7f, -7.5f)
            close()
            moveTo(12f, 22f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 20f)
            horizontalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(8f, 17f)
            horizontalLineToRelative(8f)
            verticalLineTo(10f)
            quadTo(16f, 8.35f, 14.83f, 7.18f)
            reflectiveQuadTo(12f, 6f)
            reflectiveQuadTo(9.18f, 7.18f)
            reflectiveQuadTo(8f, 10f)
            verticalLineToRelative(7f)
            close()
          }
        }
        .build()
    return _Notifications!!
  }

private var _Notifications: ImageVector? = null
