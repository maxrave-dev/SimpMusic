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
val SimpIcons.Replay5: ImageVector
  get() {
    if (_Replay5 != null) {
      return _Replay5!!
    }
    _Replay5 =
      ImageVector.Builder(
          name = "Replay5",
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
            moveTo(13f, 16f)
            horizontalLineTo(10.25f)
            quadTo(9.93f, 16f, 9.71f, 15.79f)
            reflectiveQuadTo(9.5f, 15.25f)
            reflectiveQuadTo(9.71f, 14.71f)
            reflectiveQuadTo(10.25f, 14.5f)
            horizontalLineTo(12.5f)
            verticalLineToRelative(-1f)
            horizontalLineTo(10.25f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(9.5f, 12.75f)
            verticalLineToRelative(-2f)
            quadToRelative(0f, -0.33f, 0.21f, -0.54f)
            reflectiveQuadTo(10.25f, 10f)
            horizontalLineToRelative(3f)
            quadToRelative(0.33f, 0f, 0.54f, 0.21f)
            quadTo(14f, 10.43f, 14f, 10.75f)
            reflectiveQuadToRelative(-0.21f, 0.54f)
            reflectiveQuadTo(13.25f, 11.5f)
            horizontalLineTo(11f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(2.25f)
            quadToRelative(0.33f, 0f, 0.54f, 0.21f)
            quadTo(14f, 12.93f, 14f, 13.25f)
            verticalLineTo(15f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(13f, 16f)
            close()
            moveTo(8.49f, 21.29f)
            quadTo(6.85f, 20.58f, 5.64f, 19.36f)
            reflectiveQuadTo(3.71f, 16.51f)
            reflectiveQuadTo(3f, 13f)
            quadTo(3f, 12.58f, 3.29f, 12.29f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 13f)
            quadToRelative(0f, 2.92f, 2.04f, 4.96f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadToRelative(4.96f, -2.04f)
            quadTo(19f, 15.93f, 19f, 13f)
            quadTo(19f, 10.07f, 16.96f, 8.04f)
            reflectiveQuadTo(12f, 6f)
            horizontalLineTo(11.85f)
            lineTo(12.7f, 6.85f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(12.7f, 8.25f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.31f)
            quadTo(11.58f, 8.57f, 11.28f, 8.27f)
            lineTo(8.7f, 5.7f)
            quadTo(8.4f, 5.4f, 8.4f, 5f)
            reflectiveQuadTo(8.7f, 4.3f)
            lineTo(11.28f, 1.72f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.29f)
            reflectiveQuadTo(12.7f, 1.75f)
            quadToRelative(0.28f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(12.7f, 3.15f)
            lineTo(11.85f, 4f)
            horizontalLineTo(12f)
            quadToRelative(1.88f, 0f, 3.51f, 0.71f)
            quadToRelative(1.64f, 0.71f, 2.85f, 1.93f)
            reflectiveQuadToRelative(1.93f, 2.85f)
            reflectiveQuadTo(21f, 13f)
            reflectiveQuadToRelative(-0.71f, 3.51f)
            reflectiveQuadToRelative(-1.93f, 2.85f)
            reflectiveQuadToRelative(-2.85f, 1.93f)
            reflectiveQuadTo(12f, 22f)
            reflectiveQuadTo(8.49f, 21.29f)
            close()
          }
        }
        .build()
    return _Replay5!!
  }

private var _Replay5: ImageVector? = null
