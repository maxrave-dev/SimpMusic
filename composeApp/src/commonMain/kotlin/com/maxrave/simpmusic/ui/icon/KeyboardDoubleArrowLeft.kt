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
val SimpIcons.KeyboardDoubleArrowLeft: ImageVector
  get() {
    if (_KeyboardDoubleArrowLeft != null) {
      return _KeyboardDoubleArrowLeft!!
    }
    _KeyboardDoubleArrowLeft =
      ImageVector.Builder(
          name = "KeyboardDoubleArrowLeft",
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
            moveTo(7.83f, 12f)
            lineToRelative(3.88f, 3.9f)
            quadToRelative(0.28f, 0.28f, 0.29f, 0.69f)
            reflectiveQuadTo(11.7f, 17.3f)
            quadTo(11.43f, 17.58f, 11f, 17.58f)
            reflectiveQuadTo(10.3f, 17.3f)
            lineTo(5.7f, 12.7f)
            quadTo(5.55f, 12.55f, 5.49f, 12.38f)
            reflectiveQuadTo(5.43f, 12f)
            reflectiveQuadTo(5.49f, 11.63f)
            reflectiveQuadTo(5.7f, 11.3f)
            lineTo(10.3f, 6.7f)
            quadTo(10.58f, 6.43f, 10.99f, 6.41f)
            reflectiveQuadTo(11.7f, 6.7f)
            quadToRelative(0.28f, 0.27f, 0.28f, 0.7f)
            reflectiveQuadTo(11.7f, 8.1f)
            lineTo(7.83f, 12f)
            close()
            moveToRelative(6.6f, 0f)
            lineToRelative(3.88f, 3.9f)
            quadToRelative(0.27f, 0.28f, 0.29f, 0.69f)
            reflectiveQuadTo(18.3f, 17.3f)
            quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
            reflectiveQuadTo(16.9f, 17.3f)
            lineTo(12.3f, 12.7f)
            quadTo(12.15f, 12.55f, 12.09f, 12.38f)
            reflectiveQuadTo(12.03f, 12f)
            reflectiveQuadToRelative(0.06f, -0.38f)
            reflectiveQuadTo(12.3f, 11.3f)
            lineTo(16.9f, 6.7f)
            quadTo(17.18f, 6.43f, 17.59f, 6.41f)
            reflectiveQuadTo(18.3f, 6.7f)
            quadToRelative(0.27f, 0.27f, 0.27f, 0.7f)
            reflectiveQuadTo(18.3f, 8.1f)
            lineTo(14.43f, 12f)
            close()
          }
        }
        .build()
    return _KeyboardDoubleArrowLeft!!
  }

private var _KeyboardDoubleArrowLeft: ImageVector? = null
