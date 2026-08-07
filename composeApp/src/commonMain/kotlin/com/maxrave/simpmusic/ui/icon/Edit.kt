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
val SimpIcons.Edit: ImageVector
  get() {
    if (_Edit != null) {
      return _Edit!!
    }
    _Edit =
      ImageVector.Builder(
          name = "Edit",
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
            moveTo(4f, 21f)
            quadTo(3.58f, 21f, 3.29f, 20.71f)
            quadTo(3f, 20.43f, 3f, 20f)
            verticalLineTo(17.58f)
            quadToRelative(0f, -0.4f, 0.15f, -0.76f)
            reflectiveQuadTo(3.58f, 16.18f)
            lineTo(16.2f, 3.57f)
            quadTo(16.5f, 3.3f, 16.86f, 3.15f)
            reflectiveQuadTo(17.63f, 3f)
            quadToRelative(0.4f, 0f, 0.78f, 0.15f)
            reflectiveQuadTo(19.05f, 3.6f)
            lineTo(20.43f, 5f)
            quadToRelative(0.3f, 0.27f, 0.44f, 0.65f)
            reflectiveQuadTo(21f, 6.4f)
            quadToRelative(0f, 0.4f, -0.14f, 0.76f)
            reflectiveQuadTo(20.43f, 7.82f)
            lineTo(7.83f, 20.43f)
            quadTo(7.55f, 20.7f, 7.19f, 20.85f)
            quadTo(6.83f, 21f, 6.43f, 21f)
            horizontalLineTo(4f)
            close()
            moveTo(17.6f, 7.8f)
            lineTo(19f, 6.4f)
            lineTo(17.6f, 5f)
            lineTo(16.2f, 6.4f)
            lineToRelative(1.4f, 1.4f)
            close()
          }
        }
        .build()
    return _Edit!!
  }

private var _Edit: ImageVector? = null
