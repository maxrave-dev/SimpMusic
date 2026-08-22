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
val SimpIcons.Groups: ImageVector
  get() {
    if (_Groups != null) {
      return _Groups!!
    }
    _Groups =
      ImageVector.Builder(
          name = "Groups",
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
            moveTo(1f, 18f)
            quadTo(0.58f, 18f, 0.29f, 17.71f)
            quadTo(0f, 17.43f, 0f, 17f)
            verticalLineTo(16.43f)
            quadTo(0f, 15.35f, 1.1f, 14.68f)
            reflectiveQuadTo(4f, 14f)
            quadToRelative(0.33f, 0f, 0.63f, 0.01f)
            reflectiveQuadTo(5.2f, 14.08f)
            quadTo(4.85f, 14.6f, 4.68f, 15.18f)
            reflectiveQuadTo(4.5f, 16.38f)
            verticalLineTo(18f)
            horizontalLineTo(1f)
            close()
            moveToRelative(6f, 0f)
            quadTo(6.58f, 18f, 6.29f, 17.71f)
            quadTo(6f, 17.43f, 6f, 17f)
            verticalLineTo(16.38f)
            quadToRelative(0f, -0.8f, 0.44f, -1.46f)
            reflectiveQuadTo(7.68f, 13.75f)
            reflectiveQuadTo(9.59f, 13f)
            reflectiveQuadTo(12f, 12.75f)
            quadToRelative(1.33f, 0f, 2.44f, 0.25f)
            reflectiveQuadToRelative(1.91f, 0.75f)
            reflectiveQuadToRelative(1.22f, 1.16f)
            reflectiveQuadTo(18f, 16.38f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 18f)
            horizontalLineTo(7f)
            close()
            moveToRelative(12.5f, 0f)
            verticalLineTo(16.38f)
            quadToRelative(0f, -0.65f, -0.16f, -1.22f)
            reflectiveQuadTo(18.85f, 14.08f)
            quadToRelative(0.27f, -0.05f, 0.56f, -0.06f)
            reflectiveQuadTo(20f, 14f)
            quadToRelative(1.8f, 0f, 2.9f, 0.66f)
            reflectiveQuadTo(24f, 16.43f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(23f, 18f)
            horizontalLineTo(19.5f)
            close()
            moveTo(4f, 13f)
            quadTo(3.18f, 13f, 2.59f, 12.41f)
            reflectiveQuadTo(2f, 11f)
            quadTo(2f, 10.15f, 2.59f, 9.57f)
            reflectiveQuadTo(4f, 9f)
            quadTo(4.85f, 9f, 5.43f, 9.57f)
            reflectiveQuadTo(6f, 11f)
            quadToRelative(0f, 0.82f, -0.57f, 1.41f)
            reflectiveQuadTo(4f, 13f)
            close()
            moveToRelative(16f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(18f, 11f)
            quadToRelative(0f, -0.85f, 0.59f, -1.43f)
            reflectiveQuadTo(20f, 9f)
            quadToRelative(0.85f, 0f, 1.43f, 0.57f)
            reflectiveQuadTo(22f, 11f)
            quadToRelative(0f, 0.82f, -0.57f, 1.41f)
            reflectiveQuadTo(20f, 13f)
            close()
            moveTo(12f, 12f)
            quadTo(10.75f, 12f, 9.88f, 11.13f)
            reflectiveQuadTo(9f, 9f)
            quadTo(9f, 7.72f, 9.88f, 6.86f)
            reflectiveQuadTo(12f, 6f)
            quadToRelative(1.28f, 0f, 2.14f, 0.86f)
            quadTo(15f, 7.72f, 15f, 9f)
            quadToRelative(0f, 1.25f, -0.86f, 2.13f)
            reflectiveQuadTo(12f, 12f)
            close()
          }
        }
        .build()
    return _Groups!!
  }

private var _Groups: ImageVector? = null
