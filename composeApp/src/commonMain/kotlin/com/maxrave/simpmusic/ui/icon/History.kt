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
val SimpIcons.History: ImageVector
  get() {
    if (_History != null) {
      return _History!!
    }
    _History =
      ImageVector.Builder(
          name = "History",
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
            moveTo(12f, 21f)
            quadTo(8.85f, 21f, 6.43f, 19.09f)
            quadTo(4f, 17.18f, 3.28f, 14.2f)
            quadTo(3.18f, 13.83f, 3.43f, 13.51f)
            reflectiveQuadTo(4.1f, 13.15f)
            quadTo(4.5f, 13.1f, 4.83f, 13.3f)
            reflectiveQuadToRelative(0.45f, 0.6f)
            quadToRelative(0.6f, 2.25f, 2.47f, 3.68f)
            reflectiveQuadTo(12f, 19f)
            quadToRelative(2.93f, 0f, 4.96f, -2.04f)
            quadTo(19f, 14.93f, 19f, 12f)
            quadTo(19f, 9.07f, 16.96f, 7.04f)
            reflectiveQuadTo(12f, 5f)
            quadTo(10.28f, 5f, 8.78f, 5.8f)
            reflectiveQuadTo(6.25f, 8f)
            horizontalLineTo(8f)
            quadTo(8.43f, 8f, 8.71f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            quadTo(9f, 9.42f, 8.71f, 9.71f)
            reflectiveQuadTo(8f, 10f)
            horizontalLineTo(4f)
            quadTo(3.58f, 10f, 3.29f, 9.71f)
            reflectiveQuadTo(3f, 9f)
            verticalLineTo(5f)
            quadTo(3f, 4.57f, 3.29f, 4.29f)
            reflectiveQuadTo(4f, 4f)
            reflectiveQuadTo(4.71f, 4.29f)
            reflectiveQuadTo(5f, 5f)
            verticalLineTo(6.35f)
            quadTo(6.28f, 4.75f, 8.11f, 3.88f)
            reflectiveQuadTo(12f, 3f)
            quadToRelative(1.88f, 0f, 3.51f, 0.71f)
            reflectiveQuadToRelative(2.85f, 1.93f)
            reflectiveQuadToRelative(1.93f, 2.85f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.71f, 3.51f)
            reflectiveQuadToRelative(-1.93f, 2.85f)
            reflectiveQuadToRelative(-2.85f, 1.93f)
            reflectiveQuadTo(12f, 21f)
            close()
            moveToRelative(1f, -9.4f)
            lineToRelative(2.5f, 2.5f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            quadToRelative(0f, 0.43f, -0.28f, 0.7f)
            reflectiveQuadToRelative(-0.7f, 0.28f)
            reflectiveQuadTo(14.1f, 15.5f)
            lineTo(11.3f, 12.7f)
            quadTo(11.15f, 12.55f, 11.08f, 12.36f)
            reflectiveQuadTo(11f, 11.98f)
            verticalLineTo(8f)
            quadTo(11f, 7.57f, 11.29f, 7.29f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 8f)
            verticalLineToRelative(3.6f)
            close()
          }
        }
        .build()
    return _History!!
  }

private var _History: ImageVector? = null
