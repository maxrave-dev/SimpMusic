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
val SimpIcons.Subtitles: ImageVector
  get() {
    if (_Subtitles != null) {
      return _Subtitles!!
    }
    _Subtitles =
      ImageVector.Builder(
          name = "Subtitles",
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
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(7f, 16f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            reflectiveQuadTo(14f, 15f)
            reflectiveQuadTo(13.71f, 14.29f)
            reflectiveQuadTo(13f, 14f)
            horizontalLineTo(7f)
            quadTo(6.58f, 14f, 6.29f, 14.29f)
            reflectiveQuadTo(6f, 15f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 16f)
            close()
            moveToRelative(4f, -4f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 11.43f, 18f, 11f)
            reflectiveQuadTo(17.71f, 10.29f)
            reflectiveQuadTo(17f, 10f)
            horizontalLineTo(11f)
            quadToRelative(-0.42f, 0f, -0.71f, 0.29f)
            reflectiveQuadTo(10f, 11f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(11f, 12f)
            close()
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
            moveToRelative(10f, 4f)
            quadTo(18f, 15.43f, 18f, 15f)
            reflectiveQuadTo(17.71f, 14.29f)
            reflectiveQuadTo(17f, 14f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(16f, 15f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(17f, 16f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
          }
        }
        .build()
    return _Subtitles!!
  }

private var _Subtitles: ImageVector? = null
