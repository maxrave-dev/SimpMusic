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
val SimpIcons.TipsAndUpdates: ImageVector
  get() {
    if (_TipsAndUpdates != null) {
      return _TipsAndUpdates!!
    }
    _TipsAndUpdates =
      ImageVector.Builder(
          name = "TipsAndUpdates",
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
            moveTo(21.38f, 8.63f)
            lineTo(20.5f, 8.23f)
            quadTo(20.35f, 8.15f, 20.35f, 8f)
            reflectiveQuadTo(20.5f, 7.77f)
            lineToRelative(0.88f, -0.4f)
            lineTo(21.78f, 6.5f)
            quadTo(21.85f, 6.35f, 22f, 6.35f)
            reflectiveQuadTo(22.23f, 6.5f)
            lineToRelative(0.4f, 0.88f)
            lineToRelative(0.88f, 0.4f)
            quadTo(23.65f, 7.85f, 23.65f, 8f)
            reflectiveQuadTo(23.5f, 8.23f)
            lineToRelative(-0.88f, 0.4f)
            lineTo(22.23f, 9.5f)
            quadTo(22.15f, 9.65f, 22f, 9.65f)
            reflectiveQuadTo(21.78f, 9.5f)
            lineTo(21.38f, 8.63f)
            close()
            moveTo(18.05f, 3.95f)
            lineTo(16.5f, 3.22f)
            quadTo(16.35f, 3.15f, 16.35f, 3f)
            reflectiveQuadTo(16.5f, 2.77f)
            lineTo(18.05f, 2.05f)
            lineTo(18.78f, 0.5f)
            quadTo(18.85f, 0.35f, 19f, 0.35f)
            reflectiveQuadTo(19.23f, 0.5f)
            lineToRelative(0.73f, 1.55f)
            lineTo(21.5f, 2.77f)
            quadTo(21.65f, 2.85f, 21.65f, 3f)
            reflectiveQuadTo(21.5f, 3.22f)
            lineTo(19.95f, 3.95f)
            lineTo(19.23f, 5.5f)
            quadTo(19.15f, 5.65f, 19f, 5.65f)
            reflectiveQuadTo(18.78f, 5.5f)
            lineTo(18.05f, 3.95f)
            close()
            moveTo(7.59f, 21.41f)
            quadTo(7f, 20.83f, 7f, 20f)
            horizontalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(9f, 22f)
            quadTo(8.18f, 22f, 7.59f, 21.41f)
            close()
            moveTo(6f, 19f)
            quadTo(5.58f, 19f, 5.29f, 18.71f)
            quadTo(5f, 18.43f, 5f, 18f)
            reflectiveQuadTo(5.29f, 17.29f)
            reflectiveQuadTo(6f, 17f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(13f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(12f, 19f)
            horizontalLineTo(6f)
            close()
            moveTo(5.25f, 16f)
            quadTo(3.53f, 14.98f, 2.51f, 13.25f)
            quadTo(1.5f, 11.52f, 1.5f, 9.5f)
            quadTo(1.5f, 6.38f, 3.69f, 4.19f)
            reflectiveQuadTo(9f, 2f)
            reflectiveQuadToRelative(5.31f, 2.19f)
            reflectiveQuadTo(16.5f, 9.5f)
            quadToRelative(0f, 2.02f, -1.01f, 3.75f)
            reflectiveQuadTo(12.75f, 16f)
            horizontalLineTo(5.25f)
            close()
          }
        }
        .build()
    return _TipsAndUpdates!!
  }

private var _TipsAndUpdates: ImageVector? = null
