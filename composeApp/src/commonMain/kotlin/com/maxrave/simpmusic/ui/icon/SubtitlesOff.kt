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
val SimpIcons.SubtitlesOff: ImageVector
  get() {
    if (_SubtitlesOff != null) {
      return _SubtitlesOff!!
    }
    _SubtitlesOff =
      ImageVector.Builder(
          name = "SubtitlesOff",
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
            moveTo(7.71f, 11.71f)
            quadTo(8f, 11.43f, 8f, 11f)
            reflectiveQuadTo(7.71f, 10.29f)
            reflectiveQuadTo(7f, 10f)
            quadTo(6.58f, 10f, 6.29f, 10.29f)
            reflectiveQuadTo(6f, 11f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 12f)
            reflectiveQuadTo(7.71f, 11.71f)
            close()
            moveTo(22f, 6f)
            verticalLineTo(16.73f)
            quadToRelative(0f, 0.67f, -0.61f, 0.94f)
            reflectiveQuadTo(20.3f, 17.45f)
            lineTo(14.85f, 12f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 11.43f, 18f, 11f)
            reflectiveQuadTo(17.71f, 10.29f)
            reflectiveQuadTo(17f, 10f)
            horizontalLineTo(12.85f)
            lineTo(8.55f, 5.7f)
            quadTo(8.08f, 5.22f, 8.34f, 4.61f)
            reflectiveQuadTo(9.28f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            close()
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            lineToRelative(8f, 8f)
            horizontalLineTo(9.2f)
            lineTo(1.35f, 4.15f)
            quadTo(1.08f, 3.88f, 1.08f, 3.45f)
            reflectiveQuadTo(1.35f, 2.75f)
            quadTo(1.63f, 2.47f, 2.05f, 2.47f)
            reflectiveQuadToRelative(0.7f, 0.28f)
            lineToRelative(18.5f, 18.5f)
            quadToRelative(0.28f, 0.27f, 0.29f, 0.69f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadToRelative(-0.7f, -0.28f)
            lineTo(17.15f, 20f)
            horizontalLineTo(4f)
            close()
            moveToRelative(7.15f, -6f)
            horizontalLineTo(7f)
            quadTo(6.58f, 14f, 6.29f, 14.29f)
            reflectiveQuadTo(6f, 15f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 16f)
            horizontalLineToRelative(6.15f)
            lineToRelative(-2f, -2f)
            close()
          }
        }
        .build()
    return _SubtitlesOff!!
  }

private var _SubtitlesOff: ImageVector? = null
