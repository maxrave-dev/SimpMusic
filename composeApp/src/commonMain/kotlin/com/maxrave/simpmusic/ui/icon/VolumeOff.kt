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
val SimpIcons.VolumeOff: ImageVector
  get() {
    if (_VolumeOff != null) {
      return _VolumeOff!!
    }
    _VolumeOff =
      ImageVector.Builder(
          name = "VolumeOff",
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
            moveTo(16.78f, 19.58f)
            quadTo(16.5f, 19.75f, 16.23f, 19.9f)
            reflectiveQuadToRelative(-0.58f, 0.28f)
            quadToRelative(-0.38f, 0.18f, -0.76f, 0f)
            reflectiveQuadTo(14.35f, 19.6f)
            quadTo(14.2f, 19.23f, 14.39f, 18.86f)
            reflectiveQuadToRelative(0.56f, -0.54f)
            quadToRelative(0.1f, -0.05f, 0.19f, -0.1f)
            reflectiveQuadToRelative(0.19f, -0.1f)
            lineTo(12f, 14.8f)
            verticalLineToRelative(2.78f)
            quadToRelative(0f, 0.68f, -0.61f, 0.94f)
            reflectiveQuadTo(10.3f, 18.3f)
            lineTo(7f, 15f)
            horizontalLineTo(4f)
            quadTo(3.58f, 15f, 3.29f, 14.71f)
            reflectiveQuadTo(3f, 14f)
            verticalLineTo(10f)
            quadTo(3f, 9.57f, 3.29f, 9.29f)
            reflectiveQuadTo(4f, 9f)
            horizontalLineTo(6.2f)
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
            lineTo(16.78f, 19.58f)
            close()
            moveTo(19f, 11.98f)
            quadTo(19f, 9.9f, 17.9f, 8.19f)
            quadTo(16.8f, 6.47f, 14.95f, 5.63f)
            quadTo(14.58f, 5.45f, 14.4f, 5.09f)
            reflectiveQuadTo(14.35f, 4.35f)
            quadTo(14.5f, 3.95f, 14.89f, 3.77f)
            reflectiveQuadToRelative(0.79f, 0f)
            quadToRelative(2.43f, 1.07f, 3.88f, 3.28f)
            reflectiveQuadTo(21f, 11.98f)
            quadToRelative(0f, 0.82f, -0.15f, 1.64f)
            reflectiveQuadToRelative(-0.43f, 1.56f)
            quadToRelative(-0.2f, 0.55f, -0.61f, 0.69f)
            reflectiveQuadToRelative(-0.76f, 0.01f)
            reflectiveQuadTo(18.49f, 15.43f)
            reflectiveQuadTo(18.48f, 14.68f)
            quadToRelative(0.27f, -0.65f, 0.4f, -1.31f)
            reflectiveQuadTo(19f, 11.98f)
            close()
            moveTo(14.78f, 8.42f)
            quadTo(15.6f, 8.95f, 16.05f, 10f)
            reflectiveQuadToRelative(0.45f, 2f)
            quadToRelative(0f, 0.13f, 0f, 0.25f)
            reflectiveQuadTo(16.48f, 12.5f)
            quadToRelative(-0.05f, 0.32f, -0.35f, 0.42f)
            reflectiveQuadTo(15.58f, 12.77f)
            lineTo(14.3f, 11.5f)
            quadTo(14.15f, 11.35f, 14.08f, 11.16f)
            reflectiveQuadTo(14f, 10.77f)
            verticalLineTo(8.85f)
            quadToRelative(0f, -0.3f, 0.26f, -0.44f)
            quadToRelative(0.26f, -0.14f, 0.51f, 0.01f)
            close()
            moveTo(9.75f, 6.95f)
            quadTo(9.6f, 6.8f, 9.6f, 6.6f)
            reflectiveQuadTo(9.75f, 6.25f)
            lineTo(10.3f, 5.7f)
            quadTo(10.78f, 5.22f, 11.39f, 5.49f)
            reflectiveQuadTo(12f, 6.43f)
            verticalLineTo(8f)
            quadToRelative(0f, 0.35f, -0.3f, 0.47f)
            reflectiveQuadTo(11.15f, 8.35f)
            lineTo(9.75f, 6.95f)
            close()
          }
        }
        .build()
    return _VolumeOff!!
  }

private var _VolumeOff: ImageVector? = null
