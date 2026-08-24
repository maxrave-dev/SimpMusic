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
val SimpIcons.VolumeDown: ImageVector
  get() {
    if (_VolumeDown != null) {
      return _VolumeDown!!
    }
    _VolumeDown =
      ImageVector.Builder(
          name = "VolumeDown",
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
            moveTo(9f, 15f)
            horizontalLineTo(6f)
            quadTo(5.58f, 15f, 5.29f, 14.71f)
            reflectiveQuadTo(5f, 14f)
            verticalLineTo(10f)
            quadTo(5f, 9.57f, 5.29f, 9.29f)
            reflectiveQuadTo(6f, 9f)
            horizontalLineTo(9f)
            lineTo(12.3f, 5.7f)
            quadTo(12.78f, 5.22f, 13.39f, 5.49f)
            reflectiveQuadTo(14f, 6.43f)
            verticalLineTo(17.58f)
            quadToRelative(0f, 0.68f, -0.61f, 0.94f)
            reflectiveQuadTo(12.3f, 18.3f)
            lineTo(9f, 15f)
            close()
            moveToRelative(9.5f, -3f)
            quadToRelative(0f, 1.05f, -0.47f, 1.99f)
            reflectiveQuadToRelative(-1.25f, 1.54f)
            quadToRelative(-0.25f, 0.15f, -0.51f, 0.01f)
            reflectiveQuadTo(16f, 15.1f)
            verticalLineTo(8.85f)
            quadToRelative(0f, -0.3f, 0.26f, -0.44f)
            quadToRelative(0.26f, -0.14f, 0.51f, 0.01f)
            quadTo(17.55f, 9.05f, 18.03f, 10f)
            reflectiveQuadToRelative(0.47f, 2f)
            close()
          }
        }
        .build()
    return _VolumeDown!!
  }

private var _VolumeDown: ImageVector? = null
