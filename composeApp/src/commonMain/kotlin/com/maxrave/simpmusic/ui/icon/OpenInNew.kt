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
val SimpIcons.OpenInNew: ImageVector
  get() {
    if (_OpenInNew != null) {
      return _OpenInNew!!
    }
    _OpenInNew =
      ImageVector.Builder(
          name = "OpenInNew",
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
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(11f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(20f, 12f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 13f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(19f, 6.4f)
            lineTo(10.4f, 15f)
            quadToRelative(-0.28f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(9f, 15f)
            reflectiveQuadTo(8.73f, 14.3f)
            quadTo(8.73f, 13.88f, 9f, 13.6f)
            lineTo(17.6f, 5f)
            horizontalLineTo(15f)
            quadTo(14.58f, 5f, 14.29f, 4.71f)
            reflectiveQuadTo(14f, 4f)
            quadTo(14f, 3.57f, 14.29f, 3.29f)
            reflectiveQuadTo(15f, 3f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 4f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 10f)
            reflectiveQuadTo(19.29f, 9.71f)
            reflectiveQuadTo(19f, 9f)
            verticalLineTo(6.4f)
            close()
          }
        }
        .build()
    return _OpenInNew!!
  }

private var _OpenInNew: ImageVector? = null
