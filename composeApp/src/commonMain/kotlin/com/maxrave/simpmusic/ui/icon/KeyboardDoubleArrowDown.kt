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
val SimpIcons.KeyboardDoubleArrowDown: ImageVector
  get() {
    if (_KeyboardDoubleArrowDown != null) {
      return _KeyboardDoubleArrowDown!!
    }
    _KeyboardDoubleArrowDown =
      ImageVector.Builder(
          name = "KeyboardDoubleArrowDown",
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
            moveTo(12f, 16.18f)
            lineTo(15.9f, 12.3f)
            quadToRelative(0.28f, -0.28f, 0.69f, -0.29f)
            reflectiveQuadTo(17.3f, 12.3f)
            quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
            reflectiveQuadTo(17.3f, 13.7f)
            lineToRelative(-4.6f, 4.6f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(12f, 18.58f)
            reflectiveQuadTo(11.63f, 18.51f)
            reflectiveQuadTo(11.3f, 18.3f)
            lineTo(6.7f, 13.7f)
            quadTo(6.43f, 13.43f, 6.41f, 13.01f)
            reflectiveQuadTo(6.7f, 12.3f)
            quadTo(6.98f, 12.02f, 7.4f, 12.02f)
            reflectiveQuadTo(8.1f, 12.3f)
            lineTo(12f, 16.18f)
            close()
            moveToRelative(0f, -6f)
            lineTo(15.9f, 6.3f)
            quadTo(16.18f, 6.02f, 16.59f, 6.01f)
            reflectiveQuadTo(17.3f, 6.3f)
            quadTo(17.58f, 6.57f, 17.58f, 7f)
            reflectiveQuadTo(17.3f, 7.7f)
            lineToRelative(-4.6f, 4.6f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(12f, 12.58f)
            reflectiveQuadTo(11.63f, 12.51f)
            reflectiveQuadTo(11.3f, 12.3f)
            lineTo(6.7f, 7.7f)
            quadTo(6.43f, 7.43f, 6.41f, 7.01f)
            reflectiveQuadTo(6.7f, 6.3f)
            quadTo(6.98f, 6.02f, 7.4f, 6.02f)
            reflectiveQuadTo(8.1f, 6.3f)
            lineTo(12f, 10.17f)
            close()
          }
        }
        .build()
    return _KeyboardDoubleArrowDown!!
  }

private var _KeyboardDoubleArrowDown: ImageVector? = null
