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
val SimpIcons.Downloading: ImageVector
  get() {
    if (_Downloading != null) {
      return _Downloading!!
    }
    _Downloading =
      ImageVector.Builder(
          name = "Downloading",
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
            moveTo(9.8f, 21.75f)
            quadTo(6.38f, 20.98f, 4.19f, 18.25f)
            quadTo(2f, 15.53f, 2f, 12f)
            reflectiveQuadTo(4.19f, 5.75f)
            quadTo(6.38f, 3.02f, 9.8f, 2.25f)
            quadToRelative(0.47f, -0.13f, 0.84f, 0.17f)
            reflectiveQuadTo(11f, 3.22f)
            quadToRelative(0f, 0.35f, -0.2f, 0.63f)
            quadTo(10.6f, 4.13f, 10.25f, 4.2f)
            quadTo(7.5f, 4.82f, 5.75f, 7f)
            quadTo(4f, 9.17f, 4f, 12f)
            reflectiveQuadToRelative(1.75f, 5f)
            reflectiveQuadToRelative(4.5f, 2.8f)
            quadToRelative(0.35f, 0.07f, 0.55f, 0.35f)
            quadTo(11f, 20.43f, 11f, 20.78f)
            quadToRelative(0f, 0.5f, -0.36f, 0.8f)
            reflectiveQuadTo(9.8f, 21.75f)
            close()
            moveToRelative(4.4f, 0f)
            quadToRelative(-0.47f, 0.13f, -0.84f, -0.18f)
            reflectiveQuadTo(13f, 20.78f)
            quadToRelative(0f, -0.35f, 0.2f, -0.63f)
            quadToRelative(0.2f, -0.27f, 0.55f, -0.35f)
            quadToRelative(0.68f, -0.15f, 1.31f, -0.41f)
            reflectiveQuadToRelative(1.21f, -0.64f)
            quadToRelative(0.27f, -0.2f, 0.61f, -0.15f)
            quadToRelative(0.34f, 0.05f, 0.59f, 0.3f)
            quadToRelative(0.35f, 0.35f, 0.3f, 0.81f)
            quadToRelative(-0.05f, 0.46f, -0.45f, 0.74f)
            quadTo(16.6f, 20.9f, 15.81f, 21.23f)
            quadToRelative(-0.79f, 0.32f, -1.61f, 0.52f)
            close()
            moveTo(18.9f, 17.5f)
            quadTo(18.65f, 17.25f, 18.6f, 16.91f)
            reflectiveQuadTo(18.75f, 16.3f)
            quadToRelative(0.38f, -0.58f, 0.64f, -1.23f)
            reflectiveQuadTo(19.8f, 13.75f)
            quadToRelative(0.07f, -0.35f, 0.35f, -0.54f)
            reflectiveQuadToRelative(0.63f, -0.19f)
            quadToRelative(0.5f, 0f, 0.8f, 0.36f)
            reflectiveQuadToRelative(0.17f, 0.84f)
            quadToRelative(-0.2f, 0.82f, -0.52f, 1.61f)
            reflectiveQuadToRelative(-0.77f, 1.51f)
            quadToRelative(-0.28f, 0.4f, -0.74f, 0.45f)
            reflectiveQuadTo(18.9f, 17.5f)
            close()
            moveTo(20.78f, 11f)
            quadToRelative(-0.35f, 0f, -0.63f, -0.2f)
            reflectiveQuadTo(19.8f, 10.25f)
            quadTo(19.65f, 9.57f, 19.39f, 8.94f)
            reflectiveQuadTo(18.75f, 7.72f)
            quadTo(18.55f, 7.43f, 18.6f, 7.09f)
            reflectiveQuadTo(18.9f, 6.5f)
            quadTo(19.25f, 6.15f, 19.71f, 6.2f)
            reflectiveQuadToRelative(0.74f, 0.48f)
            quadTo(20.9f, 7.4f, 21.23f, 8.19f)
            reflectiveQuadTo(21.75f, 9.8f)
            quadToRelative(0.13f, 0.47f, -0.17f, 0.84f)
            reflectiveQuadTo(20.78f, 11f)
            close()
            moveTo(16.28f, 5.25f)
            quadTo(15.7f, 4.88f, 15.06f, 4.61f)
            reflectiveQuadTo(13.75f, 4.2f)
            quadTo(13.4f, 4.13f, 13.2f, 3.85f)
            reflectiveQuadTo(13f, 3.22f)
            quadToRelative(0f, -0.5f, 0.36f, -0.8f)
            quadTo(13.73f, 2.13f, 14.2f, 2.25f)
            quadToRelative(0.82f, 0.2f, 1.61f, 0.52f)
            reflectiveQuadToRelative(1.51f, 0.77f)
            quadToRelative(0.43f, 0.28f, 0.48f, 0.74f)
            reflectiveQuadTo(17.5f, 5.1f)
            quadTo(17.25f, 5.35f, 16.91f, 5.4f)
            reflectiveQuadTo(16.28f, 5.25f)
            close()
            moveToRelative(-3.3f, 7.88f)
            lineToRelative(1.85f, -1.85f)
            quadToRelative(0.3f, -0.3f, 0.72f, -0.3f)
            reflectiveQuadToRelative(0.73f, 0.3f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.73f)
            reflectiveQuadToRelative(-0.31f, 0.72f)
            lineTo(12.68f, 16.3f)
            quadToRelative(-0.28f, 0.27f, -0.7f, 0.27f)
            reflectiveQuadTo(11.28f, 16.3f)
            lineTo(7.65f, 12.65f)
            quadTo(7.35f, 12.35f, 7.36f, 11.94f)
            quadTo(7.38f, 11.52f, 7.68f, 11.23f)
            reflectiveQuadToRelative(0.71f, -0.3f)
            reflectiveQuadToRelative(0.71f, 0.3f)
            lineToRelative(1.88f, 1.9f)
            verticalLineTo(8f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(11.98f, 7f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(12.98f, 8f)
            verticalLineToRelative(5.13f)
            close()
          }
        }
        .build()
    return _Downloading!!
  }

private var _Downloading: ImageVector? = null
