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
val SimpIcons.CloudOff: ImageVector
  get() {
    if (_CloudOff != null) {
      return _CloudOff!!
    }
    _CloudOff =
      ImageVector.Builder(
          name = "CloudOff",
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
            moveTo(6.5f, 20f)
            quadTo(4.2f, 20f, 2.6f, 18.4f)
            reflectiveQuadTo(1f, 14.5f)
            quadTo(1f, 12.58f, 2.19f, 11.08f)
            reflectiveQuadTo(5.25f, 9.15f)
            quadTo(5.33f, 8.95f, 5.4f, 8.76f)
            reflectiveQuadTo(5.55f, 8.35f)
            lineTo(2.1f, 4.9f)
            quadTo(1.83f, 4.63f, 1.83f, 4.2f)
            reflectiveQuadTo(2.1f, 3.5f)
            quadTo(2.38f, 3.22f, 2.8f, 3.22f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineToRelative(17f, 17f)
            quadToRelative(0.28f, 0.27f, 0.29f, 0.69f)
            reflectiveQuadTo(20.5f, 21.9f)
            quadToRelative(-0.27f, 0.28f, -0.69f, 0.29f)
            reflectiveQuadTo(19.1f, 21.93f)
            lineTo(17.15f, 20f)
            horizontalLineTo(6.5f)
            close()
            moveTo(21.6f, 18.75f)
            lineTo(8.05f, 5.22f)
            quadTo(8.93f, 4.63f, 9.91f, 4.31f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(2.93f, 0f, 4.96f, 2.04f)
            reflectiveQuadTo(19f, 11f)
            quadToRelative(1.73f, 0.2f, 2.86f, 1.49f)
            reflectiveQuadTo(23f, 15.5f)
            quadToRelative(0f, 0.98f, -0.38f, 1.81f)
            reflectiveQuadTo(21.6f, 18.75f)
            close()
          }
        }
        .build()
    return _CloudOff!!
  }

private var _CloudOff: ImageVector? = null
