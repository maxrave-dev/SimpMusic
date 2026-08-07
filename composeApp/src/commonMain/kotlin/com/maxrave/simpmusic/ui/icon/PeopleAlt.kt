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
val SimpIcons.PeopleAlt: ImageVector
  get() {
    if (_PeopleAlt != null) {
      return _PeopleAlt!!
    }
    _PeopleAlt =
      ImageVector.Builder(
          name = "PeopleAlt",
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
            moveTo(1f, 17.2f)
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
            verticalLineTo(17.2f)
            close()
            moveTo(18.45f, 20f)
            quadToRelative(0.28f, -0.45f, 0.41f, -0.96f)
            quadTo(19f, 18.52f, 19f, 18f)
            verticalLineTo(17f)
            quadToRelative(0f, -1.1f, -0.61f, -2.11f)
            quadTo(17.78f, 13.88f, 16.65f, 13.15f)
            quadToRelative(1.27f, 0.15f, 2.4f, 0.51f)
            quadToRelative(1.13f, 0.36f, 2.1f, 0.89f)
            quadToRelative(0.9f, 0.5f, 1.38f, 1.11f)
            reflectiveQuadTo(23f, 17f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(21f, 20f)
            horizontalLineTo(18.45f)
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
            moveToRelative(11.65f, 0f)
            quadTo(16.65f, 12f, 15f, 12f)
            quadToRelative(-0.27f, 0f, -0.7f, -0.06f)
            reflectiveQuadTo(13.6f, 11.8f)
            quadTo(14.28f, 11f, 14.64f, 10.02f)
            reflectiveQuadTo(15f, 8f)
            reflectiveQuadTo(14.64f, 5.97f)
            reflectiveQuadTo(13.6f, 4.2f)
            quadTo(13.95f, 4.07f, 14.3f, 4.04f)
            reflectiveQuadTo(15f, 4f)
            quadToRelative(1.65f, 0f, 2.82f, 1.18f)
            reflectiveQuadTo(19f, 8f)
            reflectiveQuadToRelative(-1.18f, 2.82f)
            close()
          }
        }
        .build()
    return _PeopleAlt!!
  }

private var _PeopleAlt: ImageVector? = null
