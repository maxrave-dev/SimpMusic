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
val SimpIcons.MoreVert: ImageVector
  get() {
    if (_MoreVert != null) {
      return _MoreVert!!
    }
    _MoreVert =
      ImageVector.Builder(
          name = "MoreVert",
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
            moveTo(12f, 20f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 18f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 17.18f, 14f, 18f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(0f, -6f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            close()
            moveTo(12f, 8f)
            quadTo(11.18f, 8f, 10.59f, 7.41f)
            reflectiveQuadTo(10f, 6f)
            reflectiveQuadTo(10.59f, 4.59f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 5.18f, 14f, 6f)
            reflectiveQuadTo(13.41f, 7.41f)
            reflectiveQuadTo(12f, 8f)
            close()
          }
        }
        .build()
    return _MoreVert!!
  }

private var _MoreVert: ImageVector? = null
