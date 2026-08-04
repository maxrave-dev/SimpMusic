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
val SimpIcons.KeyboardDoubleArrowUp: ImageVector
  get() {
    if (_KeyboardDoubleArrowUp != null) {
      return _KeyboardDoubleArrowUp!!
    }
    _KeyboardDoubleArrowUp =
      ImageVector.Builder(
          name = "KeyboardDoubleArrowUp",
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
            moveTo(12f, 13.83f)
            lineTo(8.1f, 17.7f)
            quadTo(7.83f, 17.98f, 7.41f, 17.99f)
            reflectiveQuadTo(6.7f, 17.7f)
            quadTo(6.43f, 17.43f, 6.43f, 17f)
            reflectiveQuadTo(6.7f, 16.3f)
            lineToRelative(4.6f, -4.6f)
            quadToRelative(0.15f, -0.15f, 0.32f, -0.21f)
            reflectiveQuadTo(12f, 11.43f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 11.7f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.27f, 0.27f, 0.29f, 0.69f)
            reflectiveQuadTo(17.3f, 17.7f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(15.9f, 17.7f)
            lineTo(12f, 13.83f)
            close()
            moveToRelative(0f, -6f)
            lineTo(8.1f, 11.7f)
            quadTo(7.83f, 11.98f, 7.41f, 11.99f)
            reflectiveQuadTo(6.7f, 11.7f)
            quadTo(6.43f, 11.43f, 6.43f, 11f)
            reflectiveQuadTo(6.7f, 10.3f)
            lineTo(11.3f, 5.7f)
            quadTo(11.45f, 5.55f, 11.63f, 5.49f)
            reflectiveQuadTo(12f, 5.43f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 5.7f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.27f, 0.28f, 0.29f, 0.69f)
            reflectiveQuadTo(17.3f, 11.7f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(15.9f, 11.7f)
            lineTo(12f, 7.82f)
            close()
          }
        }
        .build()
    return _KeyboardDoubleArrowUp!!
  }

private var _KeyboardDoubleArrowUp: ImageVector? = null
