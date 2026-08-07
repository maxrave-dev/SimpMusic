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
val SimpIcons.ThumbsUpDown: ImageVector
  get() {
    if (_ThumbsUpDown != null) {
      return _ThumbsUpDown!!
    }
    _ThumbsUpDown =
      ImageVector.Builder(
          name = "ThumbsUpDown",
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
            moveTo(2f, 14f)
            quadTo(1.18f, 14f, 0.59f, 13.41f)
            reflectiveQuadTo(0f, 12f)
            verticalLineTo(6f)
            quadTo(0f, 5.7f, 0.13f, 5.43f)
            reflectiveQuadTo(0.45f, 4.95f)
            lineTo(3.6f, 1.8f)
            quadTo(3.83f, 1.57f, 4.1f, 1.46f)
            reflectiveQuadTo(4.65f, 1.35f)
            quadToRelative(0.65f, 0f, 1.13f, 0.5f)
            reflectiveQuadTo(6.13f, 3.13f)
            lineTo(5.8f, 5f)
            horizontalLineTo(11f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 6f)
            verticalLineTo(7.25f)
            quadTo(12f, 7.4f, 11.98f, 7.54f)
            quadTo(11.95f, 7.68f, 11.9f, 7.8f)
            lineTo(9.65f, 13.1f)
            quadTo(9.48f, 13.52f, 9.09f, 13.76f)
            reflectiveQuadTo(8.25f, 14f)
            horizontalLineTo(2f)
            close()
            moveToRelative(11f, 5f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(12f, 18.43f, 12f, 18f)
            verticalLineTo(16.75f)
            quadToRelative(0f, -0.15f, 0.03f, -0.29f)
            quadTo(12.05f, 16.33f, 12.1f, 16.2f)
            lineToRelative(2.25f, -5.3f)
            quadToRelative(0.2f, -0.42f, 0.57f, -0.66f)
            reflectiveQuadTo(15.75f, 10f)
            horizontalLineTo(22f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(24f, 11.18f, 24f, 12f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.3f, -0.11f, 0.56f)
            reflectiveQuadToRelative(-0.34f, 0.49f)
            lineTo(20.4f, 22.2f)
            quadToRelative(-0.23f, 0.23f, -0.5f, 0.34f)
            reflectiveQuadToRelative(-0.55f, 0.11f)
            quadToRelative(-0.65f, 0f, -1.13f, -0.5f)
            reflectiveQuadTo(17.88f, 20.88f)
            lineTo(18.2f, 19f)
            horizontalLineTo(13f)
            close()
          }
        }
        .build()
    return _ThumbsUpDown!!
  }

private var _ThumbsUpDown: ImageVector? = null
