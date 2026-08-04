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
val SimpIcons.Sync: ImageVector
  get() {
    if (_Sync != null) {
      return _Sync!!
    }
    _Sync =
      ImageVector.Builder(
          name = "Sync",
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
            moveTo(6f, 12.05f)
            quadToRelative(0f, 1.13f, 0.43f, 2.19f)
            reflectiveQuadTo(7.75f, 16.2f)
            lineTo(8f, 16.45f)
            verticalLineTo(15f)
            quadTo(8f, 14.58f, 8.29f, 14.29f)
            quadTo(8.58f, 14f, 9f, 14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(10f, 15f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(9f, 20f)
            horizontalLineTo(5f)
            quadTo(4.58f, 20f, 4.29f, 19.71f)
            quadTo(4f, 19.43f, 4f, 19f)
            reflectiveQuadTo(4.29f, 18.29f)
            reflectiveQuadTo(5f, 18f)
            horizontalLineTo(6.75f)
            lineTo(6.35f, 17.65f)
            quadTo(5.05f, 16.5f, 4.53f, 15.03f)
            reflectiveQuadTo(4f, 12.05f)
            quadTo(4f, 9.7f, 5.2f, 7.79f)
            quadTo(6.4f, 5.88f, 8.43f, 4.85f)
            quadTo(8.78f, 4.65f, 9.16f, 4.82f)
            reflectiveQuadTo(9.68f, 5.4f)
            quadTo(9.8f, 5.77f, 9.66f, 6.15f)
            reflectiveQuadTo(9.18f, 6.72f)
            quadTo(7.73f, 7.52f, 6.86f, 8.94f)
            reflectiveQuadTo(6f, 12.05f)
            close()
            moveToRelative(12f, -0.1f)
            quadTo(18f, 10.83f, 17.58f, 9.76f)
            reflectiveQuadTo(16.25f, 7.8f)
            lineTo(16f, 7.55f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(15f, 10f)
            reflectiveQuadTo(14.29f, 9.71f)
            reflectiveQuadTo(14f, 9f)
            verticalLineTo(5f)
            quadTo(14f, 4.57f, 14.29f, 4.29f)
            reflectiveQuadTo(15f, 4f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            reflectiveQuadTo(19.71f, 5.71f)
            reflectiveQuadTo(19f, 6f)
            horizontalLineTo(17.25f)
            lineToRelative(0.4f, 0.35f)
            quadToRelative(1.22f, 1.23f, 1.79f, 2.66f)
            reflectiveQuadTo(20f, 11.95f)
            quadToRelative(0f, 2.35f, -1.2f, 4.26f)
            reflectiveQuadToRelative(-3.22f, 2.94f)
            quadToRelative(-0.35f, 0.2f, -0.74f, 0.03f)
            reflectiveQuadTo(14.33f, 18.6f)
            quadTo(14.2f, 18.23f, 14.34f, 17.85f)
            reflectiveQuadToRelative(0.49f, -0.58f)
            quadToRelative(1.45f, -0.8f, 2.31f, -2.21f)
            reflectiveQuadTo(18f, 11.95f)
            close()
          }
        }
        .build()
    return _Sync!!
  }

private var _Sync: ImageVector? = null
