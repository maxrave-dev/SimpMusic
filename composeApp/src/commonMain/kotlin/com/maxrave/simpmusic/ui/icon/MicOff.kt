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
val SimpIcons.MicOff: ImageVector
  get() {
    if (_MicOff != null) {
      return _MicOff!!
    }
    _MicOff =
      ImageVector.Builder(
          name = "MicOff",
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
            moveTo(16.8f, 14.58f)
            quadToRelative(-0.35f, -0.2f, -0.45f, -0.61f)
            reflectiveQuadToRelative(0.1f, -0.76f)
            quadToRelative(0.18f, -0.28f, 0.29f, -0.59f)
            reflectiveQuadTo(16.9f, 11.98f)
            quadToRelative(0.1f, -0.43f, 0.39f, -0.7f)
            reflectiveQuadTo(18f, 11f)
            reflectiveQuadToRelative(0.7f, 0.3f)
            quadToRelative(0.28f, 0.3f, 0.23f, 0.72f)
            quadToRelative(-0.07f, 0.58f, -0.26f, 1.13f)
            reflectiveQuadTo(18.18f, 14.2f)
            quadToRelative(-0.2f, 0.35f, -0.61f, 0.46f)
            reflectiveQuadTo(16.8f, 14.58f)
            close()
            moveTo(13.3f, 10.45f)
            lineTo(9.58f, 6.72f)
            quadTo(9.3f, 6.45f, 9.15f, 6.09f)
            reflectiveQuadTo(9f, 5.32f)
            verticalLineTo(5f)
            quadTo(9f, 3.75f, 9.88f, 2.88f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(2.13f, 0.88f)
            reflectiveQuadTo(15f, 5f)
            verticalLineTo(9.73f)
            quadToRelative(0f, 0.67f, -0.61f, 0.94f)
            reflectiveQuadTo(13.3f, 10.45f)
            close()
            moveTo(11f, 20f)
            verticalLineTo(17.9f)
            quadTo(8.7f, 17.6f, 7.06f, 15.98f)
            quadTo(5.43f, 14.35f, 5.08f, 12.02f)
            quadTo(5.03f, 11.6f, 5.3f, 11.3f)
            reflectiveQuadTo(6f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(7.1f, 12f)
            quadToRelative(0.35f, 1.75f, 1.74f, 2.88f)
            reflectiveQuadTo(12f, 16f)
            quadToRelative(0.85f, 0f, 1.61f, -0.26f)
            quadTo(14.38f, 15.48f, 15f, 15f)
            lineToRelative(1.43f, 1.43f)
            quadTo(15.7f, 17f, 14.84f, 17.39f)
            reflectiveQuadTo(13f, 17.9f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(11.29f, 20.71f)
            quadTo(11f, 20.43f, 11f, 20f)
            close()
            moveToRelative(8.1f, 1.9f)
            lineTo(2.1f, 4.9f)
            quadTo(1.83f, 4.63f, 1.83f, 4.2f)
            reflectiveQuadTo(2.1f, 3.5f)
            quadTo(2.38f, 3.22f, 2.8f, 3.22f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineToRelative(17f, 17f)
            quadToRelative(0.28f, 0.27f, 0.28f, 0.7f)
            reflectiveQuadTo(20.5f, 21.9f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(19.1f, 21.9f)
            close()
          }
        }
        .buildSimpIcon()
    return _MicOff!!
  }

private var _MicOff: ImageVector? = null
