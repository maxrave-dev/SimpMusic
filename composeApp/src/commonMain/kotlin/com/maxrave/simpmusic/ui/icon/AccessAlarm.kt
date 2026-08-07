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
val SimpIcons.AccessAlarm: ImageVector
  get() {
    if (_AccessAlarm != null) {
      return _AccessAlarm!!
    }
    _AccessAlarm =
      ImageVector.Builder(
          name = "AccessAlarm",
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
            moveTo(13f, 12.6f)
            verticalLineTo(9f)
            quadTo(13f, 8.57f, 12.71f, 8.29f)
            reflectiveQuadTo(12f, 8f)
            reflectiveQuadTo(11.29f, 8.29f)
            reflectiveQuadTo(11f, 9f)
            verticalLineToRelative(3.97f)
            quadToRelative(0f, 0.2f, 0.08f, 0.39f)
            reflectiveQuadTo(11.3f, 13.7f)
            lineToRelative(2.8f, 2.8f)
            quadToRelative(0.27f, 0.27f, 0.7f, 0.27f)
            reflectiveQuadTo(15.5f, 16.5f)
            quadToRelative(0.28f, -0.27f, 0.28f, -0.7f)
            quadToRelative(0f, -0.42f, -0.28f, -0.7f)
            lineTo(13f, 12.6f)
            close()
            moveTo(8.49f, 21.29f)
            quadTo(6.85f, 20.58f, 5.64f, 19.36f)
            reflectiveQuadTo(3.71f, 16.51f)
            reflectiveQuadTo(3f, 13f)
            reflectiveQuadTo(3.71f, 9.49f)
            reflectiveQuadTo(5.64f, 6.64f)
            reflectiveQuadTo(8.49f, 4.71f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(3.51f, 0.71f)
            quadToRelative(1.64f, 0.71f, 2.85f, 1.93f)
            reflectiveQuadToRelative(1.93f, 2.85f)
            reflectiveQuadTo(21f, 13f)
            reflectiveQuadToRelative(-0.71f, 3.51f)
            reflectiveQuadToRelative(-1.93f, 2.85f)
            reflectiveQuadToRelative(-2.85f, 1.93f)
            reflectiveQuadTo(12f, 22f)
            reflectiveQuadTo(8.49f, 21.29f)
            close()
            moveTo(2.05f, 7.3f)
            quadTo(1.78f, 7.02f, 1.78f, 6.6f)
            quadToRelative(0f, -0.42f, 0.28f, -0.7f)
            lineTo(4.9f, 3.05f)
            quadTo(5.18f, 2.77f, 5.6f, 2.77f)
            reflectiveQuadTo(6.3f, 3.05f)
            reflectiveQuadToRelative(0.27f, 0.7f)
            quadToRelative(0f, 0.42f, -0.27f, 0.7f)
            lineTo(3.45f, 7.3f)
            quadTo(3.18f, 7.57f, 2.75f, 7.57f)
            quadTo(2.33f, 7.57f, 2.05f, 7.3f)
            close()
            moveToRelative(19.9f, 0f)
            quadToRelative(-0.28f, 0.27f, -0.7f, 0.27f)
            reflectiveQuadTo(20.55f, 7.3f)
            lineTo(17.7f, 4.45f)
            quadTo(17.43f, 4.17f, 17.43f, 3.75f)
            quadToRelative(0f, -0.43f, 0.27f, -0.7f)
            quadTo(17.98f, 2.77f, 18.4f, 2.77f)
            reflectiveQuadToRelative(0.7f, 0.27f)
            lineTo(21.95f, 5.9f)
            quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
            reflectiveQuadTo(21.95f, 7.3f)
            close()
          }
        }
        .build()
    return _AccessAlarm!!
  }

private var _AccessAlarm: ImageVector? = null
