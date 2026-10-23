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
val SimpIcons.LeftPanelOpen: ImageVector
  get() {
    if (_LeftPanelOpen != null) {
      return _LeftPanelOpen!!
    }
    _LeftPanelOpen =
      ImageVector.Builder(
          name = "LeftPanelOpen",
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
            moveTo(12.5f, 9.2f)
            verticalLineToRelative(5.6f)
            quadToRelative(0f, 0.35f, 0.3f, 0.48f)
            reflectiveQuadToRelative(0.55f, -0.13f)
            lineTo(15.8f, 12.7f)
            quadTo(16.1f, 12.4f, 16.1f, 12f)
            reflectiveQuadTo(15.8f, 11.3f)
            lineTo(13.35f, 8.85f)
            quadTo(13.1f, 8.6f, 12.8f, 8.73f)
            reflectiveQuadTo(12.5f, 9.2f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveToRelative(5f, -2f)
            horizontalLineToRelative(9f)
            verticalLineTo(5f)
            horizontalLineTo(10f)
            verticalLineTo(19f)
            close()
          }
        }
        .buildSimpIcon()
    return _LeftPanelOpen!!
  }

private var _LeftPanelOpen: ImageVector? = null
