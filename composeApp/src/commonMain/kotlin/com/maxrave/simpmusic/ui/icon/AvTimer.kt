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
val SimpIcons.AvTimer: ImageVector
  get() {
    if (_AvTimer != null) {
      return _AvTimer!!
    }
    _AvTimer =
      ImageVector.Builder(
          name = "AvTimer",
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
            moveTo(8.51f, 20.29f)
            quadTo(6.88f, 19.58f, 5.65f, 18.35f)
            reflectiveQuadTo(3.71f, 15.49f)
            reflectiveQuadTo(3f, 12f)
            quadTo(3f, 10.05f, 3.75f, 8.35f)
            reflectiveQuadTo(5.85f, 5.43f)
            quadTo(6.18f, 5.15f, 6.58f, 5.16f)
            quadToRelative(0.4f, 0.01f, 0.67f, 0.29f)
            lineTo(12.7f, 10.9f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            quadToRelative(0f, 0.42f, -0.28f, 0.7f)
            reflectiveQuadTo(12f, 12.58f)
            reflectiveQuadTo(11.3f, 12.3f)
            lineTo(6.6f, 7.6f)
            quadTo(5.85f, 8.5f, 5.43f, 9.61f)
            quadTo(5f, 10.73f, 5f, 12f)
            quadToRelative(0f, 2.9f, 2.05f, 4.95f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadToRelative(4.95f, -2.05f)
            reflectiveQuadTo(19f, 12f)
            quadTo(19f, 9.32f, 17.29f, 7.39f)
            reflectiveQuadTo(13f, 5.1f)
            verticalLineTo(6f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadTo(11.29f, 6.71f)
            quadTo(11f, 6.43f, 11f, 6f)
            verticalLineTo(4f)
            quadTo(11f, 3.57f, 11.29f, 3.29f)
            reflectiveQuadTo(12f, 3f)
            quadToRelative(1.85f, 0f, 3.49f, 0.71f)
            reflectiveQuadToRelative(2.86f, 1.94f)
            reflectiveQuadToRelative(1.94f, 2.86f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.71f, 3.49f)
            reflectiveQuadToRelative(-1.94f, 2.86f)
            reflectiveQuadToRelative(-2.86f, 1.94f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(8.51f, 20.29f)
            close()
            moveTo(6.29f, 12.71f)
            quadTo(6f, 12.43f, 6f, 12f)
            reflectiveQuadTo(6.29f, 11.29f)
            reflectiveQuadTo(7f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(8f, 12f)
            reflectiveQuadTo(7.71f, 12.71f)
            reflectiveQuadTo(7f, 13f)
            quadTo(6.58f, 13f, 6.29f, 12.71f)
            close()
            moveToRelative(5f, 5f)
            quadTo(11f, 17.43f, 11f, 17f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 17f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadTo(11.29f, 17.71f)
            close()
            moveToRelative(5f, -5f)
            quadTo(16f, 12.43f, 16f, 12f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(17f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(17f, 13f)
            reflectiveQuadTo(16.29f, 12.71f)
            close()
          }
        }
        .build()
    return _AvTimer!!
  }

private var _AvTimer: ImageVector? = null
