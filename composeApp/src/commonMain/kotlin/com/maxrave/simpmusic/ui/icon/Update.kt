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
val SimpIcons.Update: ImageVector
  get() {
    if (_Update != null) {
      return _Update!!
    }
    _Update =
      ImageVector.Builder(
          name = "Update",
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
            quadTo(10.13f, 21f, 8.49f, 20.29f)
            reflectiveQuadTo(5.64f, 18.36f)
            reflectiveQuadTo(3.71f, 15.51f)
            reflectiveQuadTo(3f, 12f)
            reflectiveQuadTo(3.71f, 8.49f)
            reflectiveQuadTo(5.64f, 5.64f)
            quadTo(6.85f, 4.42f, 8.49f, 3.71f)
            reflectiveQuadTo(12f, 3f)
            quadToRelative(2.05f, 0f, 3.89f, 0.88f)
            reflectiveQuadTo(19f, 6.35f)
            verticalLineTo(5f)
            quadTo(19f, 4.57f, 19.29f, 4.29f)
            reflectiveQuadTo(20f, 4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 10f)
            horizontalLineTo(16f)
            quadTo(15.58f, 10f, 15.29f, 9.71f)
            reflectiveQuadTo(15f, 9f)
            quadTo(15f, 8.57f, 15.29f, 8.29f)
            reflectiveQuadTo(16f, 8f)
            horizontalLineToRelative(1.75f)
            quadTo(16.73f, 6.6f, 15.23f, 5.8f)
            reflectiveQuadTo(12f, 5f)
            quadTo(9.08f, 5f, 7.04f, 7.04f)
            reflectiveQuadTo(5f, 12f)
            reflectiveQuadToRelative(2.04f, 4.96f)
            reflectiveQuadTo(12f, 19f)
            quadToRelative(2.38f, 0f, 4.25f, -1.43f)
            reflectiveQuadTo(18.73f, 13.9f)
            quadToRelative(0.13f, -0.4f, 0.45f, -0.6f)
            reflectiveQuadTo(19.9f, 13.15f)
            quadToRelative(0.43f, 0.05f, 0.68f, 0.36f)
            reflectiveQuadToRelative(0.15f, 0.69f)
            quadTo(20f, 17.18f, 17.58f, 19.09f)
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
    return _Update!!
  }

private var _Update: ImageVector? = null
