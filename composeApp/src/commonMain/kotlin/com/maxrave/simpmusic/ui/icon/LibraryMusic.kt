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
val SimpIcons.LibraryMusic: ImageVector
  get() {
    if (_LibraryMusic != null) {
      return _LibraryMusic!!
    }
    _LibraryMusic =
      ImageVector.Builder(
          name = "LibraryMusic",
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
            moveTo(12.5f, 15f)
            quadToRelative(1.05f, 0f, 1.78f, -0.73f)
            reflectiveQuadTo(15f, 12.5f)
            verticalLineTo(7f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(18f, 6.43f, 18f, 6f)
            reflectiveQuadTo(17.71f, 5.29f)
            reflectiveQuadTo(17f, 5f)
            horizontalLineTo(15f)
            quadTo(14.58f, 5f, 14.29f, 5.29f)
            reflectiveQuadTo(14f, 6f)
            verticalLineToRelative(4.5f)
            quadTo(13.68f, 10.25f, 13.3f, 10.13f)
            reflectiveQuadTo(12.5f, 10f)
            quadToRelative(-1.05f, 0f, -1.77f, 0.72f)
            reflectiveQuadTo(10f, 12.5f)
            reflectiveQuadToRelative(0.73f, 1.77f)
            reflectiveQuadTo(12.5f, 15f)
            close()
            moveTo(8f, 18f)
            quadTo(7.18f, 18f, 6.59f, 17.41f)
            reflectiveQuadTo(6f, 16f)
            verticalLineTo(4f)
            quadTo(6f, 3.17f, 6.59f, 2.59f)
            reflectiveQuadTo(8f, 2f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 4f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 18f)
            horizontalLineTo(8f)
            close()
            moveTo(4f, 22f)
            quadTo(3.18f, 22f, 2.59f, 21.41f)
            reflectiveQuadTo(2f, 20f)
            verticalLineTo(7f)
            quadTo(2f, 6.57f, 2.29f, 6.29f)
            reflectiveQuadTo(3f, 6f)
            reflectiveQuadTo(3.71f, 6.29f)
            reflectiveQuadTo(4f, 7f)
            verticalLineTo(20f)
            horizontalLineTo(17f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(18f, 21f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(17f, 22f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _LibraryMusic!!
  }

private var _LibraryMusic: ImageVector? = null
