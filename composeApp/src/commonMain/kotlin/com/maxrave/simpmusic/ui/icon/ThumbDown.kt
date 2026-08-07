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
val SimpIcons.ThumbDown: ImageVector
  get() {
    if (_ThumbDown != null) {
      return _ThumbDown!!
    }
    _ThumbDown =
      ImageVector.Builder(
          name = "ThumbDown",
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
            moveTo(3f, 16f)
            quadTo(2.2f, 16f, 1.6f, 15.4f)
            reflectiveQuadTo(1f, 14f)
            verticalLineTo(12f)
            quadTo(1f, 11.83f, 1.04f, 11.63f)
            reflectiveQuadTo(1.15f, 11.25f)
            lineToRelative(3f, -7.05f)
            quadTo(4.38f, 3.7f, 4.9f, 3.35f)
            reflectiveQuadTo(6f, 3f)
            horizontalLineToRelative(8f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(16f, 5f)
            verticalLineTo(15.18f)
            quadToRelative(0f, 0.4f, -0.16f, 0.76f)
            reflectiveQuadTo(15.4f, 16.58f)
            lineToRelative(-5.42f, 5.4f)
            quadTo(9.6f, 22.33f, 9.09f, 22.4f)
            quadTo(8.58f, 22.48f, 8.1f, 22.23f)
            quadTo(7.63f, 21.98f, 7.41f, 21.53f)
            quadTo(7.2f, 21.08f, 7.33f, 20.6f)
            lineTo(8.45f, 16f)
            horizontalLineTo(3f)
            close()
            moveTo(20f, 3f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 5f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 16f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(18f, 14f)
            verticalLineTo(5f)
            quadTo(18f, 4.17f, 18.59f, 3.59f)
            reflectiveQuadTo(20f, 3f)
            close()
          }
        }
        .build()
    return _ThumbDown!!
  }

private var _ThumbDown: ImageVector? = null
