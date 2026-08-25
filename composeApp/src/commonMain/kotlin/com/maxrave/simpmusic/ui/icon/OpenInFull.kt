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
val SimpIcons.OpenInFull: ImageVector
  get() {
    if (_OpenInFull != null) {
      return _OpenInFull!!
    }
    _OpenInFull =
      ImageVector.Builder(
          name = "OpenInFull",
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
            verticalLineTo(14f)
            quadTo(3f, 13.58f, 3.29f, 13.29f)
            reflectiveQuadTo(4f, 13f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 14f)
            verticalLineToRelative(3.6f)
            lineTo(17.6f, 5f)
            horizontalLineTo(14f)
            quadTo(13.58f, 5f, 13.29f, 4.71f)
            reflectiveQuadTo(13f, 4f)
            quadTo(13f, 3.57f, 13.29f, 3.29f)
            reflectiveQuadTo(14f, 3f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 4f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 11f)
            reflectiveQuadTo(19.29f, 10.71f)
            quadTo(19f, 10.43f, 19f, 10f)
            verticalLineTo(6.4f)
            lineTo(6.4f, 19f)
            horizontalLineTo(10f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 20f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 21f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _OpenInFull!!
  }

private var _OpenInFull: ImageVector? = null
