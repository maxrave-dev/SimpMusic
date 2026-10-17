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
val SimpIcons.CommentOutline: ImageVector
  get() {
    if (_CommentOutline != null) {
      return _CommentOutline!!
    }
    _CommentOutline =
      ImageVector.Builder(
          name = "CommentOutline",
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
            moveTo(7f, 14f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 13.43f, 18f, 13f)
            reflectiveQuadTo(17.71f, 12.29f)
            reflectiveQuadTo(17f, 12f)
            horizontalLineTo(7f)
            quadTo(6.58f, 12f, 6.29f, 12.29f)
            reflectiveQuadTo(6f, 13f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 14f)
            close()
            moveTo(7f, 11f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 10.43f, 18f, 10f)
            quadTo(18f, 9.57f, 17.71f, 9.29f)
            reflectiveQuadTo(17f, 9f)
            horizontalLineTo(7f)
            quadTo(6.58f, 9f, 6.29f, 9.29f)
            reflectiveQuadTo(6f, 10f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(7f, 11f)
            close()
            moveTo(7f, 8f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 7.43f, 18f, 7f)
            reflectiveQuadTo(17.71f, 6.29f)
            reflectiveQuadTo(17f, 6f)
            horizontalLineTo(7f)
            quadTo(6.58f, 6f, 6.29f, 6.29f)
            reflectiveQuadTo(6f, 7f)
            reflectiveQuadTo(6.29f, 7.71f)
            reflectiveQuadTo(7f, 8f)
            close()
            moveTo(4f, 18f)
            quadTo(3.18f, 18f, 2.59f, 17.41f)
            reflectiveQuadTo(2f, 16f)
            verticalLineTo(4f)
            quadTo(2f, 3.17f, 2.59f, 2.59f)
            reflectiveQuadTo(4f, 2f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 4f)
            verticalLineTo(19.58f)
            quadToRelative(0f, 0.68f, -0.61f, 0.94f)
            reflectiveQuadTo(20.3f, 20.3f)
            lineTo(18f, 18f)
            horizontalLineTo(4f)
            close()
            moveTo(18.85f, 16f)
            lineTo(20f, 17.13f)
            verticalLineTo(4f)
            horizontalLineTo(4f)
            verticalLineTo(16f)
            horizontalLineTo(18.85f)
            close()
            moveTo(4f, 16f)
            verticalLineTo(4f)
            verticalLineTo(16f)
            close()
          }
        }
        .buildSimpIcon()
    return _CommentOutline!!
  }

private var _CommentOutline: ImageVector? = null
