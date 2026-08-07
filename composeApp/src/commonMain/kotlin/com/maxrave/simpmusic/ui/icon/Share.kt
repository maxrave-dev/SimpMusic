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
val SimpIcons.Share: ImageVector
  get() {
    if (_Share != null) {
      return _Share!!
    }
    _Share =
      ImageVector.Builder(
          name = "Share",
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
            moveTo(17f, 22f)
            quadToRelative(-1.25f, 0f, -2.13f, -0.88f)
            reflectiveQuadTo(14f, 19f)
            quadToRelative(0f, -0.15f, 0.08f, -0.7f)
            lineTo(7.05f, 14.2f)
            quadToRelative(-0.4f, 0.38f, -0.93f, 0.59f)
            reflectiveQuadTo(5f, 15f)
            quadTo(3.75f, 15f, 2.88f, 14.13f)
            reflectiveQuadTo(2f, 12f)
            reflectiveQuadTo(2.88f, 9.88f)
            reflectiveQuadTo(5f, 9f)
            quadTo(5.6f, 9f, 6.13f, 9.21f)
            reflectiveQuadTo(7.05f, 9.8f)
            lineTo(14.08f, 5.7f)
            quadTo(14.03f, 5.52f, 14.01f, 5.36f)
            reflectiveQuadTo(14f, 5f)
            quadTo(14f, 3.75f, 14.88f, 2.88f)
            reflectiveQuadTo(17f, 2f)
            reflectiveQuadToRelative(2.13f, 0.88f)
            reflectiveQuadTo(20f, 5f)
            reflectiveQuadTo(19.13f, 7.13f)
            reflectiveQuadTo(17f, 8f)
            quadTo(16.4f, 8f, 15.88f, 7.79f)
            reflectiveQuadTo(14.95f, 7.2f)
            lineTo(7.93f, 11.3f)
            quadToRelative(0.05f, 0.18f, 0.06f, 0.34f)
            reflectiveQuadTo(8f, 12f)
            reflectiveQuadTo(7.99f, 12.36f)
            reflectiveQuadTo(7.93f, 12.7f)
            lineToRelative(7.03f, 4.1f)
            quadToRelative(0.4f, -0.38f, 0.92f, -0.59f)
            reflectiveQuadTo(17f, 16f)
            quadToRelative(1.25f, 0f, 2.13f, 0.88f)
            reflectiveQuadTo(20f, 19f)
            reflectiveQuadToRelative(-0.88f, 2.13f)
            reflectiveQuadTo(17f, 22f)
            close()
          }
        }
        .build()
    return _Share!!
  }

private var _Share: ImageVector? = null
