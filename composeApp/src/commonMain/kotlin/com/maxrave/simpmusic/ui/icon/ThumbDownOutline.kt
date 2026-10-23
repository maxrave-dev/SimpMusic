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
val SimpIcons.ThumbDownOutline: ImageVector
  get() {
    if (_ThumbDownOutline != null) {
      return _ThumbDownOutline!!
    }
    _ThumbDownOutline =
      ImageVector.Builder(
          name = "ThumbDownOutline",
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
            moveTo(3f, 16f)
            quadTo(2.2f, 16f, 1.6f, 15.4f)
            reflectiveQuadTo(1f, 14f)
            verticalLineTo(12f)
            quadTo(1f, 11.83f, 1.05f, 11.63f)
            reflectiveQuadToRelative(0.1f, -0.38f)
            lineToRelative(3f, -7.05f)
            quadTo(4.38f, 3.7f, 4.9f, 3.35f)
            reflectiveQuadTo(6f, 3f)
            horizontalLineTo(17f)
            verticalLineTo(16f)
            lineToRelative(-6f, 5.95f)
            quadToRelative(-0.38f, 0.38f, -0.89f, 0.44f)
            reflectiveQuadTo(9.13f, 22.2f)
            reflectiveQuadTo(8.43f, 21.5f)
            reflectiveQuadTo(8.33f, 20.58f)
            lineTo(9.45f, 16f)
            horizontalLineTo(3f)
            close()
            moveTo(15f, 15.15f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            lineTo(3f, 12f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(9f)
            lineToRelative(-1.35f, 5.5f)
            lineTo(15f, 15.15f)
            close()
            moveTo(20f, 3f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 5f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 16f)
            horizontalLineTo(17f)
            verticalLineTo(14f)
            horizontalLineToRelative(3f)
            verticalLineTo(5f)
            horizontalLineTo(17f)
            verticalLineTo(3f)
            horizontalLineToRelative(3f)
            close()
            moveTo(15f, 5f)
            verticalLineTo(15.15f)
            verticalLineTo(14f)
            verticalLineTo(12f)
            verticalLineTo(5f)
            close()
          }
        }
        .buildSimpIcon()
    return _ThumbDownOutline!!
  }

private var _ThumbDownOutline: ImageVector? = null
