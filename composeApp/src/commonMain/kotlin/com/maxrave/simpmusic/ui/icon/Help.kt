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
val SimpIcons.Help: ImageVector
  get() {
    if (_Help != null) {
      return _Help!!
    }
    _Help =
      ImageVector.Builder(
          name = "Help",
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
            moveTo(12.84f, 17.64f)
            quadTo(13.2f, 17.27f, 13.2f, 16.75f)
            reflectiveQuadTo(12.84f, 15.86f)
            reflectiveQuadTo(11.95f, 15.5f)
            reflectiveQuadToRelative(-0.89f, 0.36f)
            quadTo(10.7f, 16.23f, 10.7f, 16.75f)
            reflectiveQuadToRelative(0.36f, 0.89f)
            reflectiveQuadTo(11.95f, 18f)
            reflectiveQuadToRelative(0.89f, -0.36f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(12.1f, 7.7f)
            quadToRelative(0.63f, 0f, 1.09f, 0.4f)
            reflectiveQuadToRelative(0.46f, 1f)
            quadToRelative(0f, 0.55f, -0.34f, 0.97f)
            reflectiveQuadToRelative(-0.76f, 0.8f)
            quadToRelative(-0.57f, 0.5f, -1.01f, 1.1f)
            reflectiveQuadTo(11.1f, 13.33f)
            quadToRelative(0f, 0.35f, 0.26f, 0.59f)
            reflectiveQuadToRelative(0.61f, 0.24f)
            quadToRelative(0.38f, 0f, 0.64f, -0.25f)
            reflectiveQuadToRelative(0.34f, -0.63f)
            quadToRelative(0.1f, -0.52f, 0.45f, -0.94f)
            quadToRelative(0.35f, -0.41f, 0.75f, -0.79f)
            quadTo(14.73f, 11f, 15.14f, 10.35f)
            reflectiveQuadTo(15.55f, 8.9f)
            quadToRelative(0f, -1.27f, -1.04f, -2.09f)
            reflectiveQuadTo(12.1f, 6f)
            quadTo(11.15f, 6f, 10.29f, 6.4f)
            reflectiveQuadTo(8.98f, 7.63f)
            quadTo(8.8f, 7.93f, 8.86f, 8.26f)
            reflectiveQuadTo(9.2f, 8.77f)
            quadTo(9.55f, 8.98f, 9.93f, 8.9f)
            reflectiveQuadTo(10.55f, 8.48f)
            quadTo(10.83f, 8.1f, 11.24f, 7.9f)
            reflectiveQuadTo(12.1f, 7.7f)
            close()
          }
        }
        .build()
    return _Help!!
  }

private var _Help: ImageVector? = null
