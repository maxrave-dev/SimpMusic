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
val SimpIcons.Delete: ImageVector
  get() {
    if (_Delete != null) {
      return _Delete!!
    }
    _Delete =
      ImageVector.Builder(
          name = "Delete",
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
            moveTo(7f, 21f)
            quadTo(6.18f, 21f, 5.59f, 20.41f)
            reflectiveQuadTo(5f, 19f)
            verticalLineTo(6f)
            quadTo(4.58f, 6f, 4.29f, 5.71f)
            quadTo(4f, 5.43f, 4f, 5f)
            reflectiveQuadTo(4.29f, 4.29f)
            reflectiveQuadTo(5f, 4f)
            horizontalLineTo(9f)
            quadTo(9f, 3.57f, 9.29f, 3.29f)
            quadTo(9.58f, 3f, 10f, 3f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 4f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            reflectiveQuadTo(19.71f, 5.71f)
            reflectiveQuadTo(19f, 6f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 21f)
            horizontalLineTo(7f)
            close()
            moveToRelative(3.71f, -4.29f)
            quadTo(11f, 16.43f, 11f, 16f)
            verticalLineTo(9f)
            quadTo(11f, 8.57f, 10.71f, 8.29f)
            reflectiveQuadTo(10f, 8f)
            quadTo(9.58f, 8f, 9.29f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            quadTo(9.58f, 17f, 10f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(4f, 0f)
            quadTo(15f, 16.43f, 15f, 16f)
            verticalLineTo(9f)
            quadTo(15f, 8.57f, 14.71f, 8.29f)
            reflectiveQuadTo(14f, 8f)
            reflectiveQuadTo(13.29f, 8.29f)
            reflectiveQuadTo(13f, 9f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            reflectiveQuadTo(14f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
          }
        }
        .build()
    return _Delete!!
  }

private var _Delete: ImageVector? = null
