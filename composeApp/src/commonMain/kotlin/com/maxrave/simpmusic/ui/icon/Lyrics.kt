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
val SimpIcons.Lyrics: ImageVector
  get() {
    if (_Lyrics != null) {
      return _Lyrics!!
    }
    _Lyrics =
      ImageVector.Builder(
          name = "Lyrics",
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
            moveTo(7f, 14f)
            horizontalLineTo(9f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(10f, 13.43f, 10f, 13f)
            reflectiveQuadTo(9.71f, 12.29f)
            reflectiveQuadTo(9f, 12f)
            horizontalLineTo(7f)
            quadTo(6.58f, 12f, 6.29f, 12.29f)
            reflectiveQuadTo(6f, 13f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 14f)
            close()
            moveTo(19f, 12f)
            quadToRelative(-1.25f, 0f, -2.13f, -0.88f)
            reflectiveQuadTo(16f, 9f)
            reflectiveQuadTo(16.88f, 6.88f)
            reflectiveQuadTo(19f, 6f)
            quadToRelative(0.28f, 0f, 0.53f, 0.05f)
            reflectiveQuadTo(20f, 6.18f)
            verticalLineTo(2f)
            quadTo(20f, 1.57f, 20.29f, 1.29f)
            reflectiveQuadTo(21f, 1f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(24f, 2f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(23f, 3f)
            horizontalLineTo(22f)
            verticalLineTo(9f)
            quadToRelative(0f, 1.25f, -0.88f, 2.13f)
            reflectiveQuadTo(19f, 12f)
            close()
            moveTo(7f, 11f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(13f, 10.43f, 13f, 10f)
            quadTo(13f, 9.57f, 12.71f, 9.29f)
            reflectiveQuadTo(12f, 9f)
            horizontalLineTo(7f)
            quadTo(6.58f, 9f, 6.29f, 9.29f)
            reflectiveQuadTo(6f, 10f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 11f)
            close()
            moveTo(7f, 8f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(13f, 7.43f, 13f, 7f)
            reflectiveQuadTo(12.71f, 6.29f)
            reflectiveQuadTo(12f, 6f)
            horizontalLineTo(7f)
            quadTo(6.58f, 6f, 6.29f, 6.29f)
            reflectiveQuadTo(6f, 7f)
            reflectiveQuadTo(6.29f, 7.71f)
            reflectiveQuadTo(7f, 8f)
            close()
            moveTo(6f, 18f)
            lineTo(3.7f, 20.3f)
            quadTo(3.55f, 20.45f, 3.38f, 20.53f)
            reflectiveQuadTo(3f, 20.6f)
            quadToRelative(-0.4f, 0f, -0.7f, -0.29f)
            reflectiveQuadTo(2f, 19.58f)
            verticalLineTo(4f)
            quadTo(2f, 3.17f, 2.59f, 2.59f)
            reflectiveQuadTo(4f, 2f)
            horizontalLineTo(15f)
            quadToRelative(0.78f, 0f, 1.36f, 0.47f)
            reflectiveQuadTo(16.95f, 3.7f)
            quadToRelative(0f, 0.35f, -0.16f, 0.63f)
            reflectiveQuadTo(16.35f, 4.77f)
            quadToRelative(-1.1f, 0.68f, -1.73f, 1.8f)
            reflectiveQuadTo(14f, 9f)
            quadToRelative(0f, 1.35f, 0.66f, 2.5f)
            reflectiveQuadToRelative(1.84f, 1.82f)
            quadToRelative(0.55f, 0.32f, 0.88f, 0.86f)
            reflectiveQuadToRelative(0.32f, 1.19f)
            quadToRelative(0f, 1.13f, -0.79f, 1.88f)
            reflectiveQuadTo(15f, 18f)
            horizontalLineTo(6f)
            close()
          }
        }
        .build()
    return _Lyrics!!
  }

private var _Lyrics: ImageVector? = null
