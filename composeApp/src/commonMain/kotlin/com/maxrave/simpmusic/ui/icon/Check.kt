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
val SimpIcons.Check: ImageVector
  get() {
    if (_Check != null) {
      return _Check!!
    }
    _Check =
      ImageVector.Builder(
          name = "Check",
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
            moveTo(9.55f, 15.15f)
            lineTo(18.03f, 6.68f)
            quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
            reflectiveQuadToRelative(0.7f, 0.3f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
            reflectiveQuadTo(19.43f, 8.1f)
            lineToRelative(-9.18f, 9.2f)
            quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
            reflectiveQuadTo(8.85f, 17.3f)
            lineTo(4.55f, 13f)
            quadTo(4.25f, 12.7f, 4.26f, 12.29f)
            reflectiveQuadTo(4.58f, 11.58f)
            reflectiveQuadToRelative(0.71f, -0.3f)
            reflectiveQuadTo(6f, 11.58f)
            lineToRelative(3.55f, 3.58f)
            close()
          }
        }
        .build()
    return _Check!!
  }

private var _Check: ImageVector? = null
