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
val SimpIcons.Settings: ImageVector
  get() {
    if (_Settings != null) {
      return _Settings!!
    }
    _Settings =
      ImageVector.Builder(
          name = "Settings",
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
            moveTo(10.83f, 22f)
            quadTo(10.15f, 22f, 9.66f, 21.55f)
            reflectiveQuadTo(9.08f, 20.45f)
            lineTo(8.85f, 18.8f)
            quadTo(8.53f, 18.68f, 8.24f, 18.5f)
            reflectiveQuadTo(7.68f, 18.13f)
            lineTo(6.13f, 18.77f)
            quadTo(5.5f, 19.05f, 4.88f, 18.83f)
            reflectiveQuadTo(3.9f, 18.02f)
            lineTo(2.73f, 15.98f)
            quadTo(2.38f, 15.4f, 2.53f, 14.75f)
            reflectiveQuadTo(3.2f, 13.68f)
            lineToRelative(1.33f, -1f)
            quadTo(4.5f, 12.5f, 4.5f, 12.34f)
            quadToRelative(0f, -0.16f, 0f, -0.34f)
            reflectiveQuadToRelative(0f, -0.34f)
            reflectiveQuadTo(4.53f, 11.33f)
            lineToRelative(-1.33f, -1f)
            quadTo(2.68f, 9.9f, 2.53f, 9.25f)
            reflectiveQuadTo(2.73f, 8.02f)
            lineTo(3.9f, 5.97f)
            quadTo(4.25f, 5.4f, 4.88f, 5.18f)
            reflectiveQuadTo(6.13f, 5.22f)
            lineTo(7.68f, 5.88f)
            quadTo(7.95f, 5.68f, 8.25f, 5.5f)
            reflectiveQuadTo(8.85f, 5.2f)
            lineTo(9.08f, 3.55f)
            quadTo(9.18f, 2.9f, 9.66f, 2.45f)
            reflectiveQuadTo(10.83f, 2f)
            horizontalLineToRelative(2.35f)
            quadToRelative(0.68f, 0f, 1.16f, 0.45f)
            reflectiveQuadToRelative(0.59f, 1.1f)
            lineTo(15.15f, 5.2f)
            quadToRelative(0.33f, 0.13f, 0.61f, 0.3f)
            reflectiveQuadToRelative(0.56f, 0.38f)
            lineTo(17.88f, 5.22f)
            quadTo(18.5f, 4.95f, 19.13f, 5.18f)
            reflectiveQuadToRelative(0.98f, 0.8f)
            lineToRelative(1.18f, 2.05f)
            quadToRelative(0.35f, 0.58f, 0.2f, 1.23f)
            reflectiveQuadTo(20.8f, 10.33f)
            lineToRelative(-1.32f, 1f)
            quadToRelative(0.02f, 0.18f, 0.02f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(-0.05f, 0.34f)
            lineToRelative(1.32f, 1f)
            quadToRelative(0.52f, 0.43f, 0.68f, 1.08f)
            reflectiveQuadToRelative(-0.2f, 1.22f)
            lineToRelative(-1.2f, 2.05f)
            quadToRelative(-0.35f, 0.58f, -0.98f, 0.8f)
            reflectiveQuadTo(17.83f, 18.77f)
            lineToRelative(-1.5f, -0.65f)
            quadToRelative(-0.27f, 0.2f, -0.57f, 0.38f)
            reflectiveQuadToRelative(-0.6f, 0.3f)
            lineToRelative(-0.22f, 1.65f)
            quadToRelative(-0.1f, 0.65f, -0.59f, 1.1f)
            reflectiveQuadTo(13.18f, 22f)
            horizontalLineTo(10.83f)
            close()
            moveToRelative(1.22f, -6.5f)
            quadToRelative(1.45f, 0f, 2.47f, -1.03f)
            reflectiveQuadTo(15.55f, 12f)
            reflectiveQuadTo(14.53f, 9.52f)
            reflectiveQuadTo(12.05f, 8.5f)
            quadToRelative(-1.47f, 0f, -2.49f, 1.02f)
            reflectiveQuadTo(8.55f, 12f)
            reflectiveQuadToRelative(1.01f, 2.47f)
            reflectiveQuadToRelative(2.49f, 1.03f)
            close()
          }
        }
        .build()
    return _Settings!!
  }

private var _Settings: ImageVector? = null
