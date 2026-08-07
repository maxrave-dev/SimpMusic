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
val SimpIcons.PlaylistAdd: ImageVector
  get() {
    if (_PlaylistAdd != null) {
      return _PlaylistAdd!!
    }
    _PlaylistAdd =
      ImageVector.Builder(
          name = "PlaylistAdd",
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
            moveTo(4f, 16f)
            quadTo(3.58f, 16f, 3.29f, 15.71f)
            reflectiveQuadTo(3f, 15f)
            reflectiveQuadTo(3.29f, 14.29f)
            reflectiveQuadTo(4f, 14f)
            horizontalLineTo(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(10f, 15f)
            reflectiveQuadTo(9.71f, 15.71f)
            reflectiveQuadTo(9f, 16f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 12f)
            quadTo(3.58f, 12f, 3.29f, 11.71f)
            quadTo(3f, 11.43f, 3f, 11f)
            reflectiveQuadTo(3.29f, 10.29f)
            reflectiveQuadTo(4f, 10f)
            horizontalLineToRelative(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(14f, 11f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(13f, 12f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 8f)
            quadTo(3.58f, 8f, 3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            reflectiveQuadTo(3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineToRelative(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(14f, 7f)
            reflectiveQuadTo(13.71f, 7.71f)
            reflectiveQuadTo(13f, 8f)
            horizontalLineTo(4f)
            close()
            moveTo(16.29f, 19.71f)
            quadTo(16f, 19.43f, 16f, 19f)
            verticalLineTo(16f)
            horizontalLineTo(13f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(13f, 14f)
            horizontalLineToRelative(3f)
            verticalLineTo(11f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(17f, 10f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 11f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 15f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(21f, 16f)
            horizontalLineTo(18f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 20f)
            reflectiveQuadTo(16.29f, 19.71f)
            close()
          }
        }
        .build()
    return _PlaylistAdd!!
  }

private var _PlaylistAdd: ImageVector? = null
