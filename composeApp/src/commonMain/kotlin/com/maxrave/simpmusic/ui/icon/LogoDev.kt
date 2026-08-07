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
val SimpIcons.LogoDev: ImageVector
  get() {
    if (_LogoDev != null) {
      return _LogoDev!!
    }
    _LogoDev =
      ImageVector.Builder(
          name = "LogoDev",
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
            moveTo(8.4f, 15f)
            quadToRelative(0.47f, 0f, 0.81f, -0.34f)
            reflectiveQuadTo(9.55f, 13.85f)
            verticalLineToRelative(-3.7f)
            quadTo(9.55f, 9.67f, 9.21f, 9.34f)
            reflectiveQuadTo(8.4f, 9f)
            horizontalLineTo(6.75f)
            quadTo(6.43f, 9f, 6.21f, 9.21f)
            reflectiveQuadTo(6f, 9.75f)
            verticalLineToRelative(4.5f)
            quadToRelative(0f, 0.32f, 0.21f, 0.54f)
            reflectiveQuadTo(6.75f, 15f)
            horizontalLineTo(8.4f)
            close()
            moveTo(7.15f, 13.85f)
            verticalLineToRelative(-3.7f)
            horizontalLineTo(8.4f)
            verticalLineToRelative(3.7f)
            horizontalLineTo(7.15f)
            close()
            moveTo(10.88f, 15f)
            horizontalLineToRelative(1.9f)
            quadTo(13f, 15f, 13.16f, 14.83f)
            reflectiveQuadTo(13.33f, 14.4f)
            quadToRelative(0f, -0.22f, -0.16f, -0.39f)
            reflectiveQuadTo(12.78f, 13.85f)
            horizontalLineToRelative(-1.5f)
            verticalLineToRelative(-1.3f)
            horizontalLineTo(12f)
            quadToRelative(0.25f, 0f, 0.41f, -0.16f)
            reflectiveQuadToRelative(0.16f, -0.41f)
            reflectiveQuadTo(12.41f, 11.56f)
            reflectiveQuadTo(12f, 11.4f)
            horizontalLineTo(11.28f)
            verticalLineTo(10.15f)
            horizontalLineToRelative(1.47f)
            quadToRelative(0.25f, 0f, 0.41f, -0.16f)
            reflectiveQuadTo(13.33f, 9.57f)
            reflectiveQuadTo(13.16f, 9.16f)
            reflectiveQuadTo(12.75f, 9f)
            horizontalLineTo(10.88f)
            quadTo(10.55f, 9f, 10.34f, 9.21f)
            reflectiveQuadTo(10.13f, 9.75f)
            verticalLineToRelative(4.5f)
            quadToRelative(0f, 0.32f, 0.21f, 0.54f)
            reflectiveQuadTo(10.88f, 15f)
            close()
            moveToRelative(5.71f, -0.24f)
            quadToRelative(0.24f, -0.21f, 0.31f, -0.54f)
            lineToRelative(1.2f, -4.5f)
            quadTo(18.18f, 9.45f, 18f, 9.23f)
            reflectiveQuadTo(17.55f, 9f)
            quadToRelative(-0.2f, 0f, -0.36f, 0.11f)
            quadTo(17.03f, 9.23f, 16.98f, 9.42f)
            lineTo(16.03f, 13.1f)
            lineTo(15.05f, 9.42f)
            quadTo(15f, 9.23f, 14.85f, 9.11f)
            reflectiveQuadTo(14.5f, 9f)
            quadTo(14.23f, 9f, 14.05f, 9.23f)
            reflectiveQuadToRelative(-0.1f, 0.5f)
            lineToRelative(1.2f, 4.5f)
            quadToRelative(0.08f, 0.32f, 0.31f, 0.54f)
            reflectiveQuadToRelative(0.56f, 0.21f)
            reflectiveQuadToRelative(0.56f, -0.21f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build()
    return _LogoDev!!
  }

private var _LogoDev: ImageVector? = null
