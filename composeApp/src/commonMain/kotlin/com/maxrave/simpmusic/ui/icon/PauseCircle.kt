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
val SimpIcons.PauseCircle: ImageVector
  get() {
    if (_PauseCircle != null) {
      return _PauseCircle!!
    }
    _PauseCircle =
      ImageVector.Builder(
          name = "PauseCircle",
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
            moveTo(10.71f, 15.71f)
            quadTo(11f, 15.43f, 11f, 15f)
            verticalLineTo(9f)
            quadTo(11f, 8.57f, 10.71f, 8.29f)
            reflectiveQuadTo(10f, 8f)
            quadTo(9.58f, 8f, 9.29f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            quadTo(9.58f, 16f, 10f, 16f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(4f, 0f)
            quadTo(15f, 15.43f, 15f, 15f)
            verticalLineTo(9f)
            quadTo(15f, 8.57f, 14.71f, 8.29f)
            reflectiveQuadTo(14f, 8f)
            reflectiveQuadTo(13.29f, 8.29f)
            reflectiveQuadTo(13f, 9f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(14f, 16f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _PauseCircle!!
  }

private var _PauseCircle: ImageVector? = null
