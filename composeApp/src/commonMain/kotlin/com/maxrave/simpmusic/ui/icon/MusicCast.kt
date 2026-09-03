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
val SimpIcons.MusicCast: ImageVector
  get() {
    if (_MusicCast != null) {
      return _MusicCast!!
    }
    _MusicCast =
      ImageVector.Builder(
          name = "MusicCast",
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
            moveTo(11.18f, 18.83f)
            quadTo(10f, 17.65f, 10f, 16f)
            reflectiveQuadToRelative(1.18f, -2.83f)
            reflectiveQuadTo(14f, 12f)
            quadToRelative(0.58f, 0f, 1.06f, 0.14f)
            reflectiveQuadTo(16f, 12.55f)
            verticalLineTo(5f)
            quadTo(16f, 4.57f, 16.29f, 4.29f)
            reflectiveQuadTo(17f, 4f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 5f)
            verticalLineTo(6f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(21f, 7f)
            horizontalLineTo(18f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 1.65f, -1.18f, 2.82f)
            reflectiveQuadTo(14f, 20f)
            reflectiveQuadTo(11.18f, 18.83f)
            close()
            moveTo(5f, 11.66f)
            quadTo(4.25f, 13.2f, 4.05f, 14.95f)
            quadTo(4f, 15.38f, 3.71f, 15.69f)
            reflectiveQuadTo(3f, 16f)
            reflectiveQuadTo(2.29f, 15.68f)
            reflectiveQuadTo(2.05f, 14.93f)
            quadToRelative(0.2f, -2.17f, 1.11f, -4.06f)
            quadTo(4.08f, 8.98f, 5.53f, 7.52f)
            reflectiveQuadTo(8.86f, 5.16f)
            reflectiveQuadTo(12.93f, 4.05f)
            quadTo(13.35f, 4f, 13.68f, 4.29f)
            reflectiveQuadTo(14f, 5f)
            reflectiveQuadTo(13.69f, 5.71f)
            reflectiveQuadTo(12.95f, 6.05f)
            quadTo(11.2f, 6.25f, 9.68f, 6.99f)
            quadTo(8.15f, 7.72f, 6.95f, 8.92f)
            reflectiveQuadTo(5f, 11.66f)
            close()
            moveToRelative(3.68f, 1.57f)
            quadToRelative(-0.45f, 0.84f, -0.6f, 1.79f)
            quadTo(8f, 15.45f, 7.71f, 15.73f)
            reflectiveQuadTo(7f, 16f)
            quadTo(6.58f, 16f, 6.3f, 15.69f)
            reflectiveQuadTo(6.08f, 14.95f)
            quadTo(6.25f, 13.6f, 6.84f, 12.44f)
            quadTo(7.43f, 11.27f, 8.35f, 10.35f)
            quadTo(9.28f, 9.42f, 10.44f, 8.84f)
            reflectiveQuadTo(12.95f, 8.07f)
            quadTo(13.38f, 8.02f, 13.69f, 8.3f)
            reflectiveQuadTo(14f, 9f)
            quadToRelative(0f, 0.42f, -0.27f, 0.71f)
            reflectiveQuadToRelative(-0.7f, 0.36f)
            quadToRelative(-0.95f, 0.15f, -1.78f, 0.59f)
            reflectiveQuadTo(9.78f, 11.75f)
            reflectiveQuadToRelative(-1.1f, 1.49f)
            close()
          }
        }
        .build()
    return _MusicCast!!
  }

private var _MusicCast: ImageVector? = null
