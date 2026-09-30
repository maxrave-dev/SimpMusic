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
val SimpIcons.Speaker: ImageVector
  get() {
    if (_Speaker != null) {
      return _Speaker!!
    }
    _Speaker =
      ImageVector.Builder(
          name = "Speaker",
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
            moveTo(17f, 22f)
            horizontalLineTo(7f)
            quadTo(6.18f, 22f, 5.59f, 21.41f)
            reflectiveQuadTo(5f, 20f)
            verticalLineTo(4f)
            quadTo(5f, 3.17f, 5.59f, 2.59f)
            reflectiveQuadTo(7f, 2f)
            horizontalLineTo(17f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(19f, 4f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 22f)
            close()
            moveTo(13.41f, 8.41f)
            quadTo(14f, 7.82f, 14f, 7f)
            reflectiveQuadTo(13.41f, 5.59f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadTo(10.59f, 5.59f)
            quadTo(10f, 6.18f, 10f, 7f)
            reflectiveQuadToRelative(0.59f, 1.41f)
            reflectiveQuadTo(12f, 9f)
            reflectiveQuadTo(13.41f, 8.41f)
            close()
            moveToRelative(1.41f, 9.41f)
            quadTo(16f, 16.65f, 16f, 15f)
            reflectiveQuadTo(14.83f, 12.18f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadTo(9.18f, 12.18f)
            reflectiveQuadTo(8f, 15f)
            reflectiveQuadToRelative(1.18f, 2.82f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadToRelative(2.83f, -1.18f)
            close()
            moveTo(10.59f, 16.41f)
            quadTo(10f, 15.83f, 10f, 15f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 13f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 14.18f, 14f, 15f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 17f)
            reflectiveQuadTo(10.59f, 16.41f)
            close()
          }
        }
        .build()
    return _Speaker!!
  }

private var _Speaker: ImageVector? = null
