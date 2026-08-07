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
val SimpIcons.FavoriteBorder: ImageVector
  get() {
    if (_FavoriteBorder != null) {
      return _FavoriteBorder!!
    }
    _FavoriteBorder =
      ImageVector.Builder(
          name = "FavoriteBorder",
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
            moveTo(11.29f, 20.2f)
            quadTo(10.93f, 20.08f, 10.65f, 19.8f)
            lineTo(8.93f, 18.23f)
            quadTo(6.28f, 15.8f, 4.14f, 13.41f)
            quadTo(2f, 11.02f, 2f, 8.15f)
            quadTo(2f, 5.8f, 3.58f, 4.22f)
            reflectiveQuadTo(7.5f, 2.65f)
            quadToRelative(1.33f, 0f, 2.5f, 0.56f)
            reflectiveQuadToRelative(2f, 1.54f)
            quadTo(12.83f, 3.77f, 14f, 3.21f)
            reflectiveQuadTo(16.5f, 2.65f)
            quadToRelative(2.35f, 0f, 3.93f, 1.57f)
            reflectiveQuadTo(22f, 8.15f)
            quadToRelative(0f, 2.88f, -2.13f, 5.28f)
            reflectiveQuadToRelative(-4.82f, 4.83f)
            lineToRelative(-1.7f, 1.55f)
            quadToRelative(-0.28f, 0.27f, -0.64f, 0.4f)
            reflectiveQuadTo(12f, 20.33f)
            reflectiveQuadTo(11.29f, 20.2f)
            close()
            moveTo(11.05f, 6.75f)
            quadTo(10.33f, 5.72f, 9.5f, 5.19f)
            quadTo(8.68f, 4.65f, 7.5f, 4.65f)
            quadTo(6f, 4.65f, 5f, 5.65f)
            reflectiveQuadTo(4f, 8.15f)
            quadToRelative(0f, 1.3f, 0.93f, 2.76f)
            reflectiveQuadToRelative(2.21f, 2.84f)
            reflectiveQuadToRelative(2.65f, 2.57f)
            reflectiveQuadTo(12f, 18.3f)
            quadToRelative(0.85f, -0.78f, 2.21f, -1.98f)
            reflectiveQuadToRelative(2.65f, -2.57f)
            reflectiveQuadToRelative(2.21f, -2.84f)
            reflectiveQuadTo(20f, 8.15f)
            quadToRelative(0f, -1.5f, -1f, -2.5f)
            reflectiveQuadToRelative(-2.5f, -1f)
            quadToRelative(-1.17f, 0f, -2f, 0.54f)
            quadTo(13.68f, 5.72f, 12.95f, 6.75f)
            quadTo(12.78f, 7f, 12.53f, 7.13f)
            reflectiveQuadTo(12f, 7.25f)
            reflectiveQuadTo(11.48f, 7.13f)
            reflectiveQuadTo(11.05f, 6.75f)
            close()
            moveTo(12f, 11.48f)
            close()
          }
        }
        .build()
    return _FavoriteBorder!!
  }

private var _FavoriteBorder: ImageVector? = null
