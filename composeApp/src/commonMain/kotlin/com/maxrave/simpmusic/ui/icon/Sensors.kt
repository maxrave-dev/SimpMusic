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
val SimpIcons.Sensors: ImageVector
  get() {
    if (_Sensors != null) {
      return _Sensors!!
    }
    _Sensors =
      ImageVector.Builder(
          name = "Sensors",
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
            moveTo(4f, 12f)
            quadToRelative(0f, 1.4f, 0.42f, 2.64f)
            reflectiveQuadToRelative(1.23f, 2.24f)
            quadToRelative(0.28f, 0.35f, 0.28f, 0.76f)
            reflectiveQuadToRelative(-0.3f, 0.71f)
            quadToRelative(-0.3f, 0.3f, -0.72f, 0.29f)
            reflectiveQuadTo(4.2f, 18.27f)
            quadTo(3.15f, 16.98f, 2.58f, 15.39f)
            reflectiveQuadTo(2f, 12f)
            reflectiveQuadTo(2.58f, 8.6f)
            reflectiveQuadTo(4.2f, 5.72f)
            quadTo(4.48f, 5.38f, 4.9f, 5.36f)
            reflectiveQuadTo(5.63f, 5.65f)
            reflectiveQuadToRelative(0.3f, 0.71f)
            quadToRelative(0f, 0.41f, -0.28f, 0.76f)
            quadTo(4.85f, 8.15f, 4.43f, 9.39f)
            reflectiveQuadTo(4f, 12f)
            close()
            moveToRelative(4f, 0f)
            quadToRelative(0f, 0.57f, 0.15f, 1.09f)
            reflectiveQuadToRelative(0.43f, 0.96f)
            quadTo(8.8f, 14.4f, 8.79f, 14.83f)
            quadTo(8.78f, 15.25f, 8.48f, 15.55f)
            quadToRelative(-0.3f, 0.3f, -0.72f, 0.29f)
            quadTo(7.33f, 15.83f, 7.1f, 15.48f)
            quadTo(6.58f, 14.75f, 6.29f, 13.85f)
            reflectiveQuadTo(6f, 12f)
            quadTo(6f, 11.02f, 6.29f, 10.14f)
            reflectiveQuadTo(7.1f, 8.52f)
            quadTo(7.35f, 8.17f, 7.76f, 8.17f)
            reflectiveQuadToRelative(0.71f, 0.3f)
            quadToRelative(0.3f, 0.3f, 0.31f, 0.71f)
            reflectiveQuadTo(8.58f, 9.95f)
            quadTo(8.3f, 10.4f, 8.15f, 10.91f)
            reflectiveQuadTo(8f, 12f)
            close()
            moveToRelative(2.59f, 1.41f)
            quadTo(10f, 12.83f, 10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            reflectiveQuadTo(10.59f, 13.41f)
            close()
            moveTo(16f, 12f)
            quadToRelative(0f, -0.58f, -0.15f, -1.09f)
            reflectiveQuadTo(15.43f, 9.95f)
            quadTo(15.2f, 9.6f, 15.23f, 9.17f)
            reflectiveQuadTo(15.55f, 8.45f)
            reflectiveQuadToRelative(0.7f, -0.29f)
            quadToRelative(0.4f, 0.01f, 0.65f, 0.36f)
            quadToRelative(0.52f, 0.73f, 0.81f, 1.61f)
            reflectiveQuadTo(18f, 12f)
            quadToRelative(0f, 0.95f, -0.29f, 1.85f)
            reflectiveQuadTo(16.9f, 15.48f)
            quadToRelative(-0.23f, 0.35f, -0.65f, 0.35f)
            reflectiveQuadToRelative(-0.72f, -0.3f)
            reflectiveQuadTo(15.21f, 14.81f)
            reflectiveQuadToRelative(0.21f, -0.76f)
            quadTo(15.7f, 13.6f, 15.85f, 13.09f)
            quadTo(16f, 12.58f, 16f, 12f)
            close()
            moveToRelative(4f, 0f)
            quadTo(20f, 10.6f, 19.58f, 9.36f)
            quadTo(19.15f, 8.13f, 18.35f, 7.13f)
            quadTo(18.08f, 6.77f, 18.06f, 6.36f)
            reflectiveQuadTo(18.35f, 5.65f)
            reflectiveQuadTo(19.09f, 5.36f)
            reflectiveQuadTo(19.8f, 5.72f)
            quadTo(20.85f, 7f, 21.43f, 8.6f)
            reflectiveQuadTo(22f, 12f)
            reflectiveQuadToRelative(-0.57f, 3.39f)
            reflectiveQuadTo(19.8f, 18.27f)
            quadToRelative(-0.27f, 0.35f, -0.7f, 0.36f)
            reflectiveQuadTo(18.38f, 18.35f)
            reflectiveQuadToRelative(-0.3f, -0.71f)
            quadToRelative(0f, -0.41f, 0.28f, -0.76f)
            quadToRelative(0.8f, -1.03f, 1.23f, -2.26f)
            quadTo(20f, 13.38f, 20f, 12f)
            close()
          }
        }
        .build()
    return _Sensors!!
  }

private var _Sensors: ImageVector? = null
