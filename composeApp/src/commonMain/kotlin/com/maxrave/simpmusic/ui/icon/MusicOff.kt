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
val SimpIcons.MusicOff: ImageVector
  get() {
    if (_MusicOff != null) {
      return _MusicOff!!
    }
    _MusicOff =
      ImageVector.Builder(
          name = "MusicOff",
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
            moveTo(19.1f, 21.9f)
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
            close()
            moveTo(14f, 7f)
            verticalLineTo(8.75f)
            quadToRelative(0f, 0.5f, -0.31f, 0.74f)
            quadTo(13.38f, 9.73f, 13f, 9.73f)
            reflectiveQuadTo(12.31f, 9.48f)
            reflectiveQuadTo(12f, 8.73f)
            verticalLineTo(4f)
            quadTo(12f, 3.57f, 12.29f, 3.29f)
            reflectiveQuadTo(13f, 3f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(18f, 4f)
            verticalLineTo(6f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 7f)
            horizontalLineTo(14f)
            close()
            moveTo(10f, 21f)
            quadTo(8.35f, 21f, 7.18f, 19.83f)
            reflectiveQuadTo(6f, 17f)
            reflectiveQuadTo(7.18f, 14.18f)
            reflectiveQuadTo(10f, 13f)
            quadToRelative(0.58f, 0f, 1.06f, 0.14f)
            reflectiveQuadTo(12f, 13.55f)
            verticalLineTo(12f)
            lineToRelative(2f, 2f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 1.65f, -1.17f, 2.82f)
            reflectiveQuadTo(10f, 21f)
            close()
          }
        }
        .buildSimpIcon()
    return _MusicOff!!
  }

private var _MusicOff: ImageVector? = null
