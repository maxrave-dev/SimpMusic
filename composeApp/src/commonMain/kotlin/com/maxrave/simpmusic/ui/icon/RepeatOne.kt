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
val SimpIcons.RepeatOne: ImageVector
  get() {
    if (_RepeatOne != null) {
      return _RepeatOne!!
    }
    _RepeatOne =
      ImageVector.Builder(
          name = "RepeatOne",
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
            moveTo(11.5f, 10.5f)
            horizontalLineTo(10.75f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(10f, 9.75f)
            quadTo(10f, 9.42f, 10.21f, 9.21f)
            reflectiveQuadTo(10.75f, 9f)
            horizontalLineTo(12f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(13f, 10f)
            verticalLineToRelative(4.25f)
            quadToRelative(0f, 0.32f, -0.21f, 0.54f)
            reflectiveQuadTo(12.25f, 15f)
            reflectiveQuadTo(11.71f, 14.79f)
            reflectiveQuadTo(11.5f, 14.25f)
            verticalLineTo(10.5f)
            close()
            moveTo(6.85f, 19f)
            lineTo(7.7f, 19.85f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(7.7f, 21.25f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.31f)
            reflectiveQuadTo(6.28f, 21.28f)
            lineTo(3.7f, 18.7f)
            quadTo(3.55f, 18.55f, 3.49f, 18.38f)
            reflectiveQuadTo(3.43f, 18f)
            reflectiveQuadTo(3.49f, 17.63f)
            reflectiveQuadTo(3.7f, 17.3f)
            lineTo(6.28f, 14.73f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.29f)
            reflectiveQuadTo(7.7f, 14.75f)
            quadToRelative(0.28f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(7.7f, 16.15f)
            lineTo(6.85f, 17f)
            horizontalLineTo(17f)
            verticalLineTo(14f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(18f, 13f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(19f, 14f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 19f)
            horizontalLineTo(6.85f)
            close()
            moveTo(17.15f, 7f)
            horizontalLineTo(7f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(6f, 11f)
            quadTo(5.58f, 11f, 5.29f, 10.71f)
            quadTo(5f, 10.43f, 5f, 10f)
            verticalLineTo(7f)
            quadTo(5f, 6.18f, 5.59f, 5.59f)
            reflectiveQuadTo(7f, 5f)
            horizontalLineTo(17.15f)
            lineTo(16.3f, 4.15f)
            quadTo(16f, 3.85f, 16.01f, 3.45f)
            quadToRelative(0.01f, -0.4f, 0.29f, -0.7f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.31f)
            quadToRelative(0.41f, -0.01f, 0.71f, 0.29f)
            lineTo(20.3f, 5.3f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.32f)
            reflectiveQuadTo(20.58f, 6f)
            reflectiveQuadTo(20.51f, 6.38f)
            reflectiveQuadTo(20.3f, 6.7f)
            lineTo(17.73f, 9.27f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(16.3f, 9.25f)
            quadTo(16.03f, 8.95f, 16.01f, 8.55f)
            reflectiveQuadTo(16.3f, 7.85f)
            lineTo(17.15f, 7f)
            close()
          }
        }
        .build()
    return _RepeatOne!!
  }

private var _RepeatOne: ImageVector? = null
