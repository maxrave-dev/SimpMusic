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
val SimpIcons.Cookie: ImageVector
  get() {
    if (_Cookie != null) {
      return _Cookie!!
    }
    _Cookie =
      ImageVector.Builder(
          name = "Cookie",
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
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.98f, 2.84f, 8.06f)
            reflectiveQuadTo(5.16f, 4.7f)
            reflectiveQuadTo(8.7f, 2.5f)
            reflectiveQuadTo(13.2f, 2.05f)
            quadToRelative(0.38f, 0.05f, 0.57f, 0.31f)
            quadTo(13.98f, 2.63f, 14f, 3.07f)
            quadToRelative(0.05f, 1.6f, 1.19f, 2.74f)
            reflectiveQuadTo(17.9f, 7f)
            quadToRelative(0.52f, 0.02f, 0.8f, 0.3f)
            quadTo(18.98f, 7.57f, 19f, 8.15f)
            quadToRelative(0.05f, 1.05f, 0.64f, 1.73f)
            reflectiveQuadToRelative(1.64f, 1.02f)
            quadToRelative(0.35f, 0.13f, 0.54f, 0.36f)
            reflectiveQuadTo(22f, 11.85f)
            quadToRelative(0.05f, 2.07f, -0.72f, 3.93f)
            reflectiveQuadToRelative(-2.13f, 3.24f)
            reflectiveQuadToRelative(-3.2f, 2.19f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(10.5f, 10f)
            quadToRelative(0.63f, 0f, 1.06f, -0.44f)
            reflectiveQuadTo(12f, 8.5f)
            reflectiveQuadTo(11.56f, 7.44f)
            reflectiveQuadTo(10.5f, 7f)
            reflectiveQuadTo(9.44f, 7.44f)
            reflectiveQuadTo(9f, 8.5f)
            reflectiveQuadTo(9.44f, 9.56f)
            reflectiveQuadTo(10.5f, 10f)
            close()
            moveToRelative(-2f, 5f)
            quadToRelative(0.63f, 0f, 1.06f, -0.44f)
            reflectiveQuadTo(10f, 13.5f)
            reflectiveQuadTo(9.56f, 12.44f)
            reflectiveQuadTo(8.5f, 12f)
            reflectiveQuadTo(7.44f, 12.44f)
            reflectiveQuadTo(7f, 13.5f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(8.5f, 15f)
            close()
            moveTo(15f, 16f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            reflectiveQuadTo(16f, 15f)
            reflectiveQuadTo(15.71f, 14.29f)
            reflectiveQuadTo(15f, 14f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(14f, 15f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(15f, 16f)
            close()
          }
        }
        .build()
    return _Cookie!!
  }

private var _Cookie: ImageVector? = null
