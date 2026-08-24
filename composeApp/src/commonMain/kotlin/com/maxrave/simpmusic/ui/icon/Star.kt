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
val SimpIcons.Star: ImageVector
  get() {
    if (_Star != null) {
      return _Star!!
    }
    _Star =
      ImageVector.Builder(
          name = "Star",
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
            moveTo(12f, 17.27f)
            lineToRelative(-4.15f, 2.5f)
            quadTo(7.58f, 19.95f, 7.28f, 19.93f)
            reflectiveQuadTo(6.75f, 19.73f)
            reflectiveQuadTo(6.4f, 19.29f)
            quadTo(6.28f, 19.02f, 6.35f, 18.7f)
            lineToRelative(1.1f, -4.72f)
            lineTo(3.78f, 10.8f)
            quadTo(3.53f, 10.58f, 3.46f, 10.29f)
            reflectiveQuadTo(3.5f, 9.73f)
            reflectiveQuadTo(3.8f, 9.27f)
            reflectiveQuadTo(4.35f, 9.05f)
            lineTo(9.2f, 8.63f)
            lineTo(11.08f, 4.17f)
            quadTo(11.2f, 3.88f, 11.46f, 3.72f)
            reflectiveQuadTo(12f, 3.57f)
            quadToRelative(0.28f, 0f, 0.54f, 0.15f)
            quadToRelative(0.26f, 0.15f, 0.39f, 0.45f)
            lineTo(14.8f, 8.63f)
            lineToRelative(4.85f, 0.42f)
            quadTo(20f, 9.1f, 20.2f, 9.27f)
            reflectiveQuadToRelative(0.3f, 0.45f)
            reflectiveQuadToRelative(0.04f, 0.56f)
            reflectiveQuadTo(20.23f, 10.8f)
            lineToRelative(-3.68f, 3.18f)
            lineToRelative(1.1f, 4.72f)
            quadToRelative(0.07f, 0.32f, -0.05f, 0.59f)
            reflectiveQuadToRelative(-0.35f, 0.44f)
            quadToRelative(-0.22f, 0.17f, -0.52f, 0.2f)
            reflectiveQuadTo(16.15f, 19.77f)
            lineTo(12f, 17.27f)
            close()
          }
        }
        .build()
    return _Star!!
  }

private var _Star: ImageVector? = null
