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
val SimpIcons.Tune: ImageVector
  get() {
    if (_Tune != null) {
      return _Tune!!
    }
    _Tune =
      ImageVector.Builder(
          name = "Tune",
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
            moveTo(11.29f, 20.71f)
            quadTo(11f, 20.43f, 11f, 20f)
            verticalLineTo(16f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 16f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(7f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 19f)
            horizontalLineTo(13f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(11.29f, 20.71f)
            close()
            moveTo(4f, 19f)
            quadTo(3.58f, 19f, 3.29f, 18.71f)
            quadTo(3f, 18.43f, 3f, 18f)
            reflectiveQuadTo(3.29f, 17.29f)
            reflectiveQuadTo(4f, 17f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 18f)
            reflectiveQuadTo(8.71f, 18.71f)
            reflectiveQuadTo(8f, 19f)
            horizontalLineTo(4f)
            close()
            moveTo(7.29f, 14.71f)
            quadTo(7f, 14.43f, 7f, 14f)
            verticalLineTo(13f)
            horizontalLineTo(4f)
            quadTo(3.58f, 13f, 3.29f, 12.71f)
            quadTo(3f, 12.43f, 3f, 12f)
            reflectiveQuadTo(3.29f, 11.29f)
            reflectiveQuadTo(4f, 11f)
            horizontalLineTo(7f)
            verticalLineTo(10f)
            quadTo(7f, 9.57f, 7.29f, 9.29f)
            reflectiveQuadTo(8f, 9f)
            reflectiveQuadTo(8.71f, 9.29f)
            reflectiveQuadTo(9f, 10f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(8f, 15f)
            quadTo(7.58f, 15f, 7.29f, 14.71f)
            close()
            moveTo(12f, 13f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(11f, 12.43f, 11f, 12f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 11f)
            horizontalLineToRelative(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 13f)
            horizontalLineTo(12f)
            close()
            moveTo(15.29f, 8.71f)
            quadTo(15f, 8.42f, 15f, 8f)
            verticalLineTo(4f)
            quadTo(15f, 3.57f, 15.29f, 3.29f)
            reflectiveQuadTo(16f, 3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 4f)
            verticalLineTo(5f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 6f)
            reflectiveQuadTo(20.71f, 6.71f)
            reflectiveQuadTo(20f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 9f)
            reflectiveQuadTo(15.29f, 8.71f)
            close()
            moveTo(4f, 7f)
            quadTo(3.58f, 7f, 3.29f, 6.71f)
            quadTo(3f, 6.43f, 3f, 6f)
            reflectiveQuadTo(3.29f, 5.29f)
            reflectiveQuadTo(4f, 5f)
            horizontalLineToRelative(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(13f, 6f)
            reflectiveQuadTo(12.71f, 6.71f)
            reflectiveQuadTo(12f, 7f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _Tune!!
  }

private var _Tune: ImageVector? = null
