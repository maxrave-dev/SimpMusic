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
val SimpIcons.ThumbUpOutline: ImageVector
  get() {
    if (_ThumbUpOutline != null) {
      return _ThumbUpOutline!!
    }
    _ThumbUpOutline =
      ImageVector.Builder(
          name = "ThumbUpOutline",
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
            moveTo(21f, 8f)
            quadToRelative(0.8f, 0f, 1.4f, 0.6f)
            reflectiveQuadTo(23f, 10f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.17f, -0.05f, 0.38f)
            reflectiveQuadToRelative(-0.1f, 0.38f)
            lineToRelative(-3f, 7.05f)
            quadToRelative(-0.23f, 0.5f, -0.75f, 0.85f)
            reflectiveQuadTo(18f, 21f)
            horizontalLineTo(7f)
            verticalLineTo(8f)
            lineTo(13f, 2.05f)
            quadTo(13.38f, 1.67f, 13.89f, 1.61f)
            reflectiveQuadTo(14.88f, 1.8f)
            reflectiveQuadToRelative(0.7f, 0.7f)
            reflectiveQuadToRelative(0.1f, 0.92f)
            lineTo(14.55f, 8f)
            horizontalLineTo(21f)
            close()
            moveTo(9f, 8.85f)
            verticalLineTo(19f)
            horizontalLineToRelative(9f)
            lineToRelative(3f, -7f)
            verticalLineTo(10f)
            horizontalLineTo(12f)
            lineTo(13.35f, 4.5f)
            lineTo(9f, 8.85f)
            close()
            moveTo(4f, 21f)
            quadTo(3.18f, 21f, 2.59f, 20.41f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(10f)
            quadTo(2f, 9.17f, 2.59f, 8.59f)
            reflectiveQuadTo(4f, 8f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            verticalLineToRelative(9f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            close()
            moveTo(9f, 19f)
            verticalLineTo(8.85f)
            verticalLineTo(10f)
            verticalLineToRelative(2f)
            verticalLineToRelative(7f)
            close()
          }
        }
        .buildSimpIcon()
    return _ThumbUpOutline!!
  }

private var _ThumbUpOutline: ImageVector? = null
