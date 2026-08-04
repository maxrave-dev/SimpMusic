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
val SimpIcons.RssFeed: ImageVector
  get() {
    if (_RssFeed != null) {
      return _RssFeed!!
    }
    _RssFeed =
      ImageVector.Builder(
          name = "RssFeed",
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
            moveTo(3.59f, 20.41f)
            quadTo(3f, 19.83f, 3f, 19f)
            reflectiveQuadTo(3.59f, 17.59f)
            reflectiveQuadTo(5f, 17f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(7f, 18.18f, 7f, 19f)
            reflectiveQuadTo(6.41f, 20.41f)
            reflectiveQuadTo(5f, 21f)
            reflectiveQuadTo(3.59f, 20.41f)
            close()
            moveTo(18.5f, 21f)
            quadToRelative(-0.65f, 0f, -1.09f, -0.48f)
            reflectiveQuadTo(16.9f, 19.4f)
            quadTo(16.63f, 16.98f, 15.59f, 14.86f)
            reflectiveQuadTo(12.9f, 11.1f)
            reflectiveQuadTo(9.14f, 8.41f)
            reflectiveQuadTo(4.6f, 7.1f)
            quadTo(3.95f, 7.02f, 3.48f, 6.59f)
            reflectiveQuadTo(3f, 5.5f)
            reflectiveQuadTo(3.45f, 4.44f)
            quadTo(3.9f, 4.02f, 4.53f, 4.07f)
            quadTo(7.6f, 4.35f, 10.29f, 5.64f)
            reflectiveQuadToRelative(4.74f, 3.34f)
            reflectiveQuadToRelative(3.34f, 4.74f)
            reflectiveQuadToRelative(1.56f, 5.76f)
            quadToRelative(0.05f, 0.63f, -0.36f, 1.07f)
            reflectiveQuadTo(18.5f, 21f)
            close()
            moveToRelative(-6f, 0f)
            quadToRelative(-0.63f, 0f, -1.07f, -0.44f)
            reflectiveQuadTo(10.85f, 19.5f)
            quadTo(10.63f, 18.27f, 10.06f, 17.24f)
            reflectiveQuadTo(8.65f, 15.35f)
            reflectiveQuadTo(6.76f, 13.94f)
            reflectiveQuadTo(4.5f, 13.15f)
            quadTo(3.88f, 13.02f, 3.44f, 12.58f)
            reflectiveQuadTo(3f, 11.5f)
            quadTo(3f, 10.85f, 3.45f, 10.43f)
            reflectiveQuadTo(4.53f, 10.1f)
            quadToRelative(1.83f, 0.25f, 3.41f, 1.06f)
            quadToRelative(1.59f, 0.81f, 2.84f, 2.06f)
            reflectiveQuadToRelative(2.06f, 2.84f)
            reflectiveQuadToRelative(1.06f, 3.41f)
            quadTo(14f, 20.1f, 13.58f, 20.55f)
            reflectiveQuadTo(12.5f, 21f)
            close()
          }
        }
        .build()
    return _RssFeed!!
  }

private var _RssFeed: ImageVector? = null
