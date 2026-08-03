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
val SimpIcons.PersonAdd: ImageVector
  get() {
    if (_PersonAdd != null) {
      return _PersonAdd!!
    }
    _PersonAdd =
      ImageVector.Builder(
          name = "PersonAdd",
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
            moveTo(18f, 11f)
            horizontalLineTo(16f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(15f, 10.43f, 15f, 10f)
            quadTo(15f, 9.57f, 15.29f, 9.29f)
            reflectiveQuadTo(16f, 9f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            quadTo(18f, 6.57f, 18.29f, 6.29f)
            reflectiveQuadTo(19f, 6f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(20f, 7f)
            verticalLineTo(9f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(23f, 10f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(22f, 11f)
            horizontalLineTo(20f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(19f, 14f)
            reflectiveQuadTo(18.29f, 13.71f)
            quadTo(18f, 13.43f, 18f, 13f)
            verticalLineTo(11f)
            close()
            moveTo(6.18f, 10.83f)
            quadTo(5f, 9.65f, 5f, 8f)
            reflectiveQuadTo(6.18f, 5.18f)
            reflectiveQuadTo(9f, 4f)
            reflectiveQuadToRelative(2.83f, 1.18f)
            reflectiveQuadTo(13f, 8f)
            reflectiveQuadToRelative(-1.17f, 2.82f)
            reflectiveQuadTo(9f, 12f)
            reflectiveQuadTo(6.18f, 10.83f)
            close()
            moveTo(1f, 18f)
            verticalLineTo(17.2f)
            quadTo(1f, 16.35f, 1.44f, 15.64f)
            quadTo(1.88f, 14.93f, 2.6f, 14.55f)
            quadTo(4.15f, 13.77f, 5.75f, 13.39f)
            reflectiveQuadTo(9f, 13f)
            reflectiveQuadToRelative(3.25f, 0.39f)
            reflectiveQuadToRelative(3.15f, 1.16f)
            quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
            reflectiveQuadTo(17f, 17.2f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(15f, 20f)
            horizontalLineTo(3f)
            quadTo(2.18f, 20f, 1.59f, 19.41f)
            reflectiveQuadTo(1f, 18f)
            close()
          }
        }
        .build()
    return _PersonAdd!!
  }

private var _PersonAdd: ImageVector? = null
