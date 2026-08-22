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
val SimpIcons.Logout: ImageVector
  get() {
    if (_Logout != null) {
      return _Logout!!
    }
    _Logout =
      ImageVector.Builder(
          name = "Logout",
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
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(11f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(11f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(17.18f, 13f)
            horizontalLineTo(10f)
            quadTo(9.58f, 13f, 9.29f, 12.71f)
            quadTo(9f, 12.43f, 9f, 12f)
            reflectiveQuadTo(9.29f, 11.29f)
            quadTo(9.58f, 11f, 10f, 11f)
            horizontalLineToRelative(7.18f)
            lineTo(15.3f, 9.13f)
            quadTo(15.03f, 8.85f, 15.03f, 8.45f)
            reflectiveQuadTo(15.3f, 7.75f)
            reflectiveQuadTo(16f, 7.44f)
            quadToRelative(0.43f, -0.01f, 0.73f, 0.29f)
            lineTo(20.3f, 11.3f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.7f)
            reflectiveQuadToRelative(-0.3f, 0.7f)
            lineToRelative(-3.57f, 3.57f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(15.3f, 16.25f)
            quadToRelative(-0.28f, -0.3f, -0.26f, -0.71f)
            reflectiveQuadToRelative(0.29f, -0.69f)
            lineTo(17.18f, 13f)
            close()
          }
        }
        .build()
    return _Logout!!
  }

private var _Logout: ImageVector? = null
