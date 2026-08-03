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
val SimpIcons.Sort: ImageVector
  get() {
    if (_Sort != null) {
      return _Sort!!
    }
    _Sort =
      ImageVector.Builder(
          name = "Sort",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
          autoMirror = true,
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
            moveTo(4f, 18f)
            quadTo(3.58f, 18f, 3.29f, 17.71f)
            quadTo(3f, 17.43f, 3f, 17f)
            reflectiveQuadTo(3.29f, 16.29f)
            reflectiveQuadTo(4f, 16f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 17f)
            reflectiveQuadTo(8.71f, 17.71f)
            reflectiveQuadTo(8f, 18f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 13f)
            quadTo(3.58f, 13f, 3.29f, 12.71f)
            quadTo(3f, 12.43f, 3f, 12f)
            reflectiveQuadTo(3.29f, 11.29f)
            reflectiveQuadTo(4f, 11f)
            horizontalLineTo(14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(14f, 13f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 8f)
            quadTo(3.58f, 8f, 3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            reflectiveQuadTo(3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 7f)
            reflectiveQuadTo(20.71f, 7.71f)
            reflectiveQuadTo(20f, 8f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _Sort!!
  }

private var _Sort: ImageVector? = null
