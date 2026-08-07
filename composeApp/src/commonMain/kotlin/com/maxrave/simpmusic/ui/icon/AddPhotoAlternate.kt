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
val SimpIcons.AddPhotoAlternate: ImageVector
  get() {
    if (_AddPhotoAlternate != null) {
      return _AddPhotoAlternate!!
    }
    _AddPhotoAlternate =
      ImageVector.Builder(
          name = "AddPhotoAlternate",
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
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineToRelative(7.45f)
            quadToRelative(0.5f, 0f, 0.76f, 0.44f)
            reflectiveQuadToRelative(0.06f, 0.94f)
            quadToRelative(-0.13f, 0.4f, -0.2f, 0.8f)
            quadTo(13f, 5.57f, 13f, 6f)
            quadToRelative(0f, 2.07f, 1.46f, 3.54f)
            reflectiveQuadTo(18f, 11f)
            quadToRelative(0.43f, 0f, 0.82f, -0.08f)
            quadToRelative(0.4f, -0.07f, 0.8f, -0.2f)
            quadToRelative(0.5f, -0.18f, 0.94f, 0.07f)
            reflectiveQuadTo(21f, 11.55f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(6f, 17f)
            horizontalLineTo(18f)
            lineTo(14.25f, 12f)
            lineToRelative(-3f, 4f)
            lineTo(9f, 13f)
            lineTo(6f, 17f)
            close()
            moveTo(17.29f, 8.71f)
            quadTo(17f, 8.42f, 17f, 8f)
            verticalLineTo(7f)
            horizontalLineTo(16f)
            quadTo(15.58f, 7f, 15.29f, 6.71f)
            quadTo(15f, 6.43f, 15f, 6f)
            reflectiveQuadTo(15.29f, 5.29f)
            reflectiveQuadTo(16f, 5f)
            horizontalLineToRelative(1f)
            verticalLineTo(4f)
            quadTo(17f, 3.57f, 17.29f, 3.29f)
            reflectiveQuadTo(18f, 3f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(19f, 4f)
            verticalLineTo(5f)
            horizontalLineToRelative(1f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 6f)
            reflectiveQuadTo(20.71f, 6.71f)
            reflectiveQuadTo(20f, 7f)
            horizontalLineTo(19f)
            verticalLineTo(8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(18f, 9f)
            reflectiveQuadTo(17.29f, 8.71f)
            close()
          }
        }
        .build()
    return _AddPhotoAlternate!!
  }

private var _AddPhotoAlternate: ImageVector? = null
