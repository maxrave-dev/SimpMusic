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
val SimpIcons.IosShare: ImageVector
  get() {
    if (_IosShare != null) {
      return _IosShare!!
    }
    _IosShare =
      ImageVector.Builder(
          name = "IosShare",
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
            moveTo(6f, 22f)
            quadTo(5.18f, 22f, 4.59f, 21.41f)
            reflectiveQuadTo(4f, 20f)
            verticalLineTo(10f)
            quadTo(4f, 9.17f, 4.59f, 8.59f)
            reflectiveQuadTo(6f, 8f)
            horizontalLineTo(8f)
            quadTo(8.43f, 8f, 8.71f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            quadTo(9f, 9.42f, 8.71f, 9.71f)
            reflectiveQuadTo(8f, 10f)
            horizontalLineTo(6f)
            verticalLineTo(20f)
            horizontalLineTo(18f)
            verticalLineTo(10f)
            horizontalLineTo(16f)
            quadTo(15.58f, 10f, 15.29f, 9.71f)
            reflectiveQuadTo(15f, 9f)
            quadTo(15f, 8.57f, 15.29f, 8.29f)
            reflectiveQuadTo(16f, 8f)
            horizontalLineToRelative(2f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(20f, 10f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 22f)
            horizontalLineTo(6f)
            close()
            moveToRelative(5.29f, -6.29f)
            quadTo(11f, 15.43f, 11f, 15f)
            verticalLineTo(4.82f)
            lineToRelative(-0.9f, 0.9f)
            quadTo(9.8f, 6.02f, 9.4f, 6.01f)
            reflectiveQuadTo(8.7f, 5.7f)
            quadTo(8.43f, 5.4f, 8.41f, 5f)
            reflectiveQuadTo(8.7f, 4.3f)
            lineTo(11.3f, 1.7f)
            quadTo(11.45f, 1.55f, 11.63f, 1.49f)
            reflectiveQuadTo(12f, 1.42f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 1.7f)
            lineToRelative(2.6f, 2.6f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.69f)
            reflectiveQuadTo(15.3f, 5.7f)
            quadTo(15f, 6f, 14.59f, 6f)
            reflectiveQuadTo(13.88f, 5.7f)
            lineTo(13f, 4.82f)
            verticalLineTo(15f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadTo(11.29f, 15.71f)
            close()
          }
        }
        .build()
    return _IosShare!!
  }

private var _IosShare: ImageVector? = null
