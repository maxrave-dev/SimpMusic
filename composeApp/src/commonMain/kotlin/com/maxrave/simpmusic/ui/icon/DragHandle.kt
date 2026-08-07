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
val SimpIcons.DragHandle: ImageVector
  get() {
    if (_DragHandle != null) {
      return _DragHandle!!
    }
    _DragHandle =
      ImageVector.Builder(
          name = "DragHandle",
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
            moveTo(5f, 15f)
            quadTo(4.58f, 15f, 4.29f, 14.71f)
            reflectiveQuadTo(4f, 14f)
            reflectiveQuadTo(4.29f, 13.29f)
            reflectiveQuadTo(5f, 13f)
            horizontalLineTo(19f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 14f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(19f, 15f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 11f)
            quadTo(4.58f, 11f, 4.29f, 10.71f)
            quadTo(4f, 10.43f, 4f, 10f)
            quadTo(4f, 9.57f, 4.29f, 9.29f)
            reflectiveQuadTo(5f, 9f)
            horizontalLineTo(19f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 10f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(19f, 11f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build()
    return _DragHandle!!
  }

private var _DragHandle: ImageVector? = null
