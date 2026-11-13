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
val SimpIcons.Translate: ImageVector
    get() {
        if (_Translate != null) {
            return _Translate!!
        }
        _Translate =
            ImageVector.Builder(
                name = "Translate",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
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
                    moveTo(12.87f, 15.07f)
                    lineToRelative(-2.54f, -2.51f)
                    lineToRelative(0.03f, -0.03f)
                    curveToRelative(1.74f, -1.94f, 2.98f, -4.17f, 3.71f, -6.53f)
                    horizontalLineTo(17f)
                    verticalLineTo(4f)
                    horizontalLineToRelative(-7f)
                    verticalLineTo(2f)
                    horizontalLineTo(8f)
                    verticalLineToRelative(2f)
                    horizontalLineTo(1f)
                    verticalLineToRelative(1.99f)
                    horizontalLineToRelative(11.17f)
                    curveTo(11.5f, 7.92f, 10.44f, 9.75f, 9f, 11.35f)
                    curveTo(8.07f, 10.32f, 7.3f, 9.19f, 6.69f, 8f)
                    horizontalLineToRelative(-2f)
                    curveToRelative(0.73f, 1.63f, 1.73f, 3.17f, 2.98f, 4.56f)
                    lineToRelative(-5.09f, 5.02f)
                    lineTo(4f, 19f)
                    lineToRelative(5f, -5f)
                    lineToRelative(3.11f, 3.11f)
                    lineToRelative(0.76f, -2.04f)
                    close()
                    moveTo(18.5f, 10f)
                    horizontalLineToRelative(-2f)
                    lineTo(12f, 22f)
                    horizontalLineToRelative(2f)
                    lineToRelative(1.12f, -3f)
                    horizontalLineToRelative(4.75f)
                    lineTo(21f, 22f)
                    horizontalLineToRelative(2f)
                    lineToRelative(-4.5f, -12f)
                    close()
                    moveTo(15.88f, 17f)
                    lineToRelative(1.62f, -4.33f)
                    lineTo(19.12f, 17f)
                    horizontalLineToRelative(-3.24f)
                    close()
                }
            }.build()
        return _Translate!!
    }

private var _Translate: ImageVector? = null
