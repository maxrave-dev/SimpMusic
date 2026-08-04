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
val SimpIcons.Album: ImageVector
  get() {
    if (_Album != null) {
      return _Album!!
    }
    _Album =
      ImageVector.Builder(
          name = "Album",
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
            moveTo(12f, 16.5f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(16.5f, 12f)
            reflectiveQuadTo(15.19f, 8.81f)
            reflectiveQuadTo(12f, 7.5f)
            reflectiveQuadTo(8.81f, 8.81f)
            reflectiveQuadTo(7.5f, 12f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 16.5f)
            close()
            moveTo(11.29f, 12.71f)
            quadTo(11f, 12.43f, 11f, 12f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(12f, 13f)
            reflectiveQuadTo(11.29f, 12.71f)
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
          }
        }
        .build()
    return _Album!!
  }

private var _Album: ImageVector? = null
