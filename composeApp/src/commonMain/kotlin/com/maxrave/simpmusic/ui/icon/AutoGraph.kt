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
val SimpIcons.AutoGraph: ImageVector
  get() {
    if (_AutoGraph != null) {
      return _AutoGraph!!
    }
    _AutoGraph =
      ImageVector.Builder(
          name = "AutoGraph",
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
            moveTo(7.4f, 6.6f)
            lineTo(6f, 5.95f)
            quadTo(5.7f, 5.82f, 5.7f, 5.5f)
            reflectiveQuadTo(6f, 5.05f)
            lineTo(7.4f, 4.4f)
            lineTo(8.05f, 3f)
            quadTo(8.18f, 2.7f, 8.5f, 2.7f)
            quadTo(8.83f, 2.7f, 8.95f, 3f)
            lineTo(9.6f, 4.4f)
            lineTo(11f, 5.05f)
            quadToRelative(0.3f, 0.13f, 0.3f, 0.45f)
            reflectiveQuadTo(11f, 5.95f)
            lineTo(9.6f, 6.6f)
            lineTo(8.95f, 8f)
            quadTo(8.83f, 8.3f, 8.5f, 8.3f)
            quadTo(8.18f, 8.3f, 8.05f, 8f)
            lineTo(7.4f, 6.6f)
            close()
            moveToRelative(6.65f, 3.35f)
            lineTo(12.98f, 9.45f)
            quadTo(12.7f, 9.32f, 12.7f, 9f)
            quadToRelative(0f, -0.33f, 0.28f, -0.45f)
            lineToRelative(1.07f, -0.5f)
            lineToRelative(0.5f, -1.08f)
            quadTo(14.68f, 6.7f, 15f, 6.7f)
            reflectiveQuadToRelative(0.45f, 0.27f)
            lineToRelative(0.5f, 1.08f)
            lineToRelative(1.08f, 0.5f)
            quadTo(17.3f, 8.67f, 17.3f, 9f)
            quadToRelative(0f, 0.32f, -0.27f, 0.45f)
            lineToRelative(-1.08f, 0.5f)
            lineToRelative(-0.5f, 1.07f)
            quadTo(15.33f, 11.3f, 15f, 11.3f)
            reflectiveQuadTo(14.55f, 11.02f)
            lineTo(14.05f, 9.95f)
            close()
            moveToRelative(-11f, 2f)
            lineTo(1.98f, 11.45f)
            quadTo(1.7f, 11.33f, 1.7f, 11f)
            reflectiveQuadTo(1.98f, 10.55f)
            lineToRelative(1.08f, -0.5f)
            lineTo(3.55f, 8.98f)
            quadTo(3.68f, 8.7f, 4f, 8.7f)
            quadToRelative(0.33f, 0f, 0.45f, 0.28f)
            lineToRelative(0.5f, 1.07f)
            lineToRelative(1.08f, 0.5f)
            quadTo(6.3f, 10.68f, 6.3f, 11f)
            reflectiveQuadTo(6.03f, 11.45f)
            lineToRelative(-1.08f, 0.5f)
            lineToRelative(-0.5f, 1.07f)
            quadTo(4.33f, 13.3f, 4f, 13.3f)
            reflectiveQuadTo(3.55f, 13.02f)
            lineTo(3.05f, 11.95f)
            close()
            moveToRelative(0.7f, 6.3f)
            lineTo(9.08f, 12.93f)
            quadTo(9.65f, 12.35f, 10.5f, 12.35f)
            reflectiveQuadToRelative(1.43f, 0.57f)
            lineTo(14.5f, 15.5f)
            lineTo(20.9f, 8.3f)
            quadTo(21.18f, 7.97f, 21.61f, 7.97f)
            reflectiveQuadToRelative(0.74f, 0.3f)
            quadToRelative(0.27f, 0.28f, 0.29f, 0.66f)
            quadToRelative(0.01f, 0.39f, -0.26f, 0.69f)
            lineToRelative(-6.48f, 7.3f)
            quadToRelative(-0.57f, 0.65f, -1.42f, 0.68f)
            reflectiveQuadTo(13f, 17f)
            lineTo(10.5f, 14.5f)
            lineTo(5.25f, 19.75f)
            quadTo(4.93f, 20.08f, 4.5f, 20.08f)
            quadToRelative(-0.42f, 0f, -0.75f, -0.32f)
            reflectiveQuadTo(3.43f, 19f)
            reflectiveQuadTo(3.75f, 18.25f)
            close()
          }
        }
        .build()
    return _AutoGraph!!
  }

private var _AutoGraph: ImageVector? = null
