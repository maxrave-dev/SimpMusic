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
val SimpIcons.PushPin: ImageVector
  get() {
    if (_PushPin != null) {
      return _PushPin!!
    }
    _PushPin =
      ImageVector.Builder(
          name = "PushPin",
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
            moveTo(16f, 5f)
            verticalLineToRelative(7f)
            lineToRelative(1.7f, 1.7f)
            quadToRelative(0.15f, 0.15f, 0.23f, 0.34f)
            reflectiveQuadTo(18f, 14.43f)
            verticalLineTo(15f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 16f)
            horizontalLineTo(13f)
            verticalLineToRelative(5.85f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 22.85f)
            reflectiveQuadTo(11.29f, 22.56f)
            reflectiveQuadTo(11f, 21.85f)
            verticalLineTo(16f)
            horizontalLineTo(7f)
            quadTo(6.58f, 16f, 6.29f, 15.71f)
            reflectiveQuadTo(6f, 15f)
            verticalLineTo(14.43f)
            quadToRelative(0f, -0.2f, 0.08f, -0.39f)
            reflectiveQuadTo(6.3f, 13.7f)
            lineTo(8f, 12f)
            verticalLineTo(5f)
            quadTo(7.58f, 5f, 7.29f, 4.71f)
            reflectiveQuadTo(7f, 4f)
            quadTo(7f, 3.57f, 7.29f, 3.29f)
            reflectiveQuadTo(8f, 3f)
            horizontalLineToRelative(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 5f)
            close()
          }
        }
        .build()
    return _PushPin!!
  }

private var _PushPin: ImageVector? = null
