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
val SimpIcons.Refresh: ImageVector
  get() {
    if (_Refresh != null) {
      return _Refresh!!
    }
    _Refresh =
      ImageVector.Builder(
          name = "Refresh",
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
            quadTo(8.65f, 20f, 6.33f, 17.68f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(1.73f, 0f, 3.3f, 0.71f)
            quadTo(16.88f, 5.43f, 18f, 6.75f)
            verticalLineTo(5f)
            quadTo(18f, 4.57f, 18.29f, 4.29f)
            reflectiveQuadTo(19f, 4f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(19f, 11f)
            horizontalLineTo(14f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(13f, 10.43f, 13f, 10f)
            quadTo(13f, 9.57f, 13.29f, 9.29f)
            reflectiveQuadTo(14f, 9f)
            horizontalLineToRelative(3.2f)
            quadTo(16.4f, 7.6f, 15.01f, 6.8f)
            reflectiveQuadTo(12f, 6f)
            quadTo(9.5f, 6f, 7.75f, 7.75f)
            reflectiveQuadTo(6f, 12f)
            reflectiveQuadToRelative(1.75f, 4.25f)
            reflectiveQuadTo(12f, 18f)
            quadToRelative(1.7f, 0f, 3.11f, -0.86f)
            quadToRelative(1.41f, -0.86f, 2.19f, -2.31f)
            quadToRelative(0.2f, -0.35f, 0.56f, -0.49f)
            reflectiveQuadTo(18.6f, 14.33f)
            quadToRelative(0.4f, 0.13f, 0.57f, 0.53f)
            reflectiveQuadTo(19.15f, 15.6f)
            quadToRelative(-1.03f, 2f, -2.93f, 3.2f)
            reflectiveQuadTo(12f, 20f)
            close()
          }
        }
        .buildSimpIcon()
    return _Refresh!!
  }

private var _Refresh: ImageVector? = null
