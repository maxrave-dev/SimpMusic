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
val SimpIcons.Insights: ImageVector
  get() {
    if (_Insights != null) {
      return _Insights!!
    }
    _Insights =
      ImageVector.Builder(
          name = "Insights",
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
            moveTo(3f, 20f)
            quadTo(2.18f, 20f, 1.59f, 19.41f)
            reflectiveQuadTo(1f, 18f)
            reflectiveQuadTo(1.59f, 16.59f)
            reflectiveQuadTo(3f, 16f)
            quadToRelative(0.15f, 0f, 0.26f, 0f)
            reflectiveQuadTo(3.5f, 16.05f)
            lineTo(8.05f, 11.5f)
            quadTo(8f, 11.38f, 8f, 11.26f)
            reflectiveQuadTo(8f, 11f)
            quadTo(8f, 10.17f, 8.59f, 9.59f)
            reflectiveQuadTo(10f, 9f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            reflectiveQuadTo(12f, 11f)
            quadToRelative(0f, 0.05f, -0.05f, 0.5f)
            lineToRelative(2.55f, 2.55f)
            quadTo(14.63f, 14f, 14.74f, 14f)
            reflectiveQuadTo(15f, 14f)
            reflectiveQuadToRelative(0.26f, 0f)
            reflectiveQuadToRelative(0.24f, 0.05f)
            lineTo(19.05f, 10.5f)
            quadTo(19f, 10.38f, 19f, 10.26f)
            reflectiveQuadTo(19f, 10f)
            quadTo(19f, 9.17f, 19.59f, 8.59f)
            reflectiveQuadTo(21f, 8f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            reflectiveQuadTo(23f, 10f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(21f, 12f)
            quadToRelative(-0.15f, 0f, -0.26f, 0f)
            reflectiveQuadTo(20.5f, 11.95f)
            lineTo(16.95f, 15.5f)
            quadTo(17f, 15.63f, 17f, 15.74f)
            reflectiveQuadTo(17f, 16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(15f, 18f)
            reflectiveQuadTo(13.59f, 17.41f)
            reflectiveQuadTo(13f, 16f)
            quadToRelative(0f, -0.15f, 0f, -0.26f)
            quadToRelative(0f, -0.11f, 0.05f, -0.24f)
            lineTo(10.5f, 12.95f)
            quadTo(10.38f, 13f, 10.26f, 13f)
            reflectiveQuadTo(10f, 13f)
            quadTo(9.95f, 13f, 9.5f, 12.95f)
            lineTo(4.95f, 17.5f)
            quadTo(5f, 17.63f, 5f, 17.74f)
            reflectiveQuadTo(5f, 18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(3f, 20f)
            close()
            moveTo(4f, 9.98f)
            lineTo(3.38f, 8.63f)
            lineTo(2.03f, 8f)
            lineTo(3.38f, 7.38f)
            lineTo(4f, 6.02f)
            lineTo(4.63f, 7.38f)
            lineTo(5.98f, 8f)
            lineTo(4.63f, 8.63f)
            lineTo(4f, 9.98f)
            close()
            moveTo(15f, 9f)
            lineTo(14.05f, 6.95f)
            lineTo(12f, 6f)
            lineTo(14.05f, 5.05f)
            lineTo(15f, 3f)
            lineToRelative(0.95f, 2.05f)
            lineTo(18f, 6f)
            lineTo(15.95f, 6.95f)
            lineTo(15f, 9f)
            close()
          }
        }
        .build()
    return _Insights!!
  }

private var _Insights: ImageVector? = null
