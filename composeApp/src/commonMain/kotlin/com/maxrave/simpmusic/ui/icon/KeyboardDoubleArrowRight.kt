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
val SimpIcons.KeyboardDoubleArrowRight: ImageVector
  get() {
    if (_KeyboardDoubleArrowRight != null) {
      return _KeyboardDoubleArrowRight!!
    }
    _KeyboardDoubleArrowRight =
      ImageVector.Builder(
          name = "KeyboardDoubleArrowRight",
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
            moveTo(9.58f, 12f)
            lineTo(5.7f, 8.1f)
            quadTo(5.43f, 7.82f, 5.41f, 7.41f)
            reflectiveQuadTo(5.7f, 6.7f)
            quadTo(5.98f, 6.43f, 6.4f, 6.43f)
            reflectiveQuadTo(7.1f, 6.7f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(11.98f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(11.7f, 12.7f)
            lineTo(7.1f, 17.3f)
            quadTo(6.83f, 17.58f, 6.41f, 17.59f)
            reflectiveQuadTo(5.7f, 17.3f)
            quadTo(5.43f, 17.02f, 5.43f, 16.6f)
            reflectiveQuadTo(5.7f, 15.9f)
            lineTo(9.58f, 12f)
            close()
            moveToRelative(6.6f, 0f)
            lineTo(12.3f, 8.1f)
            quadTo(12.03f, 7.82f, 12.01f, 7.41f)
            reflectiveQuadTo(12.3f, 6.7f)
            quadTo(12.58f, 6.43f, 13f, 6.43f)
            reflectiveQuadTo(13.7f, 6.7f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(18.58f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(18.3f, 12.7f)
            lineToRelative(-4.6f, 4.6f)
            quadToRelative(-0.28f, 0.27f, -0.69f, 0.29f)
            reflectiveQuadTo(12.3f, 17.3f)
            quadTo(12.03f, 17.02f, 12.03f, 16.6f)
            reflectiveQuadTo(12.3f, 15.9f)
            lineTo(16.18f, 12f)
            close()
          }
        }
        .build()
    return _KeyboardDoubleArrowRight!!
  }

private var _KeyboardDoubleArrowRight: ImageVector? = null
