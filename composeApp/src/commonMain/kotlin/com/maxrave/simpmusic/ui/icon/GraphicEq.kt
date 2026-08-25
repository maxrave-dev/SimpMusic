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
val SimpIcons.GraphicEq: ImageVector
  get() {
    if (_GraphicEq != null) {
      return _GraphicEq!!
    }
    _GraphicEq =
      ImageVector.Builder(
          name = "GraphicEq",
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
            moveTo(7f, 17f)
            verticalLineTo(7f)
            quadTo(7f, 6.57f, 7.29f, 6.29f)
            reflectiveQuadTo(8f, 6f)
            reflectiveQuadTo(8.71f, 6.29f)
            reflectiveQuadTo(9f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(8f, 18f)
            quadTo(7.58f, 18f, 7.29f, 17.71f)
            quadTo(7f, 17.43f, 7f, 17f)
            close()
            moveToRelative(4f, 4f)
            verticalLineTo(3f)
            quadTo(11f, 2.57f, 11.29f, 2.29f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 3f)
            verticalLineTo(21f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 22f)
            reflectiveQuadTo(11.29f, 21.71f)
            quadTo(11f, 21.43f, 11f, 21f)
            close()
            moveTo(3f, 13f)
            verticalLineTo(11f)
            quadTo(3f, 10.58f, 3.29f, 10.29f)
            reflectiveQuadTo(4f, 10f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 11f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(4f, 14f)
            reflectiveQuadTo(3.29f, 13.71f)
            quadTo(3f, 13.43f, 3f, 13f)
            close()
            moveToRelative(12f, 4f)
            verticalLineTo(7f)
            quadTo(15f, 6.57f, 15.29f, 6.29f)
            reflectiveQuadTo(16f, 6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 18f)
            reflectiveQuadTo(15.29f, 17.71f)
            quadTo(15f, 17.43f, 15f, 17f)
            close()
            moveToRelative(4f, -4f)
            verticalLineTo(11f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(20f, 10f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 11f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 14f)
            reflectiveQuadTo(19.29f, 13.71f)
            quadTo(19f, 13.43f, 19f, 13f)
            close()
          }
        }
        .build()
    return _GraphicEq!!
  }

private var _GraphicEq: ImageVector? = null
