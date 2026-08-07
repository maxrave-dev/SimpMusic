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
val SimpIcons.Add: ImageVector
  get() {
    if (_Add != null) {
      return _Add!!
    }
    _Add =
      ImageVector.Builder(
          name = "Add",
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
            moveTo(11f, 13f)
            horizontalLineTo(6f)
            quadTo(5.58f, 13f, 5.29f, 12.71f)
            quadTo(5f, 12.43f, 5f, 12f)
            reflectiveQuadTo(5.29f, 11.29f)
            reflectiveQuadTo(6f, 11f)
            horizontalLineToRelative(5f)
            verticalLineTo(6f)
            quadTo(11f, 5.57f, 11.29f, 5.29f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 6f)
            verticalLineToRelative(5f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(18f, 13f)
            horizontalLineTo(13f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(11.29f, 18.71f)
            quadTo(11f, 18.43f, 11f, 18f)
            verticalLineTo(13f)
            close()
          }
        }
        .build()
    return _Add!!
  }

private var _Add: ImageVector? = null
