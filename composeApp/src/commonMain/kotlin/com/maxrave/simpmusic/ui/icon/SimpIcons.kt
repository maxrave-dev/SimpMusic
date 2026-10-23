package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.path
import com.maxrave.logger.Logger
import kotlin.math.abs
import kotlin.math.max

/**
 * Material Symbols Rounded, generated from fonts.gstatic.com at
 * opsz 24 / wght 400 / GRAD 0 / ROND 50. One receiver object so call sites read
 * `SimpIcons.PlayArrow`, mirroring how androidx exposes `Icons.Rounded.PlayArrow`.
 */
object SimpIcons {
    /**
     * Strokes the 20×20 live area every icon is fitted into, so a row of icons can be compared by
     * eye. Set it before any icon is built (first thing at startup): icons are cached once built.
     */
    var debugRings: Boolean = false
}

private const val TAG = "SimpIcons"

/**
 * Material's live area as a share of the icon's box: glyphs belong inside the centred 20×20 square
 * of the 24 grid, with 2dp of padding around it. Most Symbols keep to it, and Google balances those
 * by keyline shapes of about equal area; 24 spill into the padding (`Lyrics` and `Groups` reach the
 * box edge), and those read bigger than the rest at the same size.
 */
internal const val ICON_LIVE_AREA = 20f / 24f

/** Overshoot this small is rounding in the generated coordinates, not a glyph past the live area. */
private const val FIT_TOLERANCE = 0.01f

/** A fit tighter than this means the measurement went wrong, not that the glyph is that big. */
private const val MIN_FIT_SCALE = 0.6f

/** Points sampled along each curve. The curves in these glyphs are short, so 16 is exact enough. */
private const val CURVE_SAMPLES = 16

/**
 * Glyphs left exactly as Google drew them although they pass the live area. `Groups` spills only
 * sideways: fitting its 24 width also cut its height, which was never too tall, from 12 to 10 of
 * 24, and it read shorter than every icon beside it.
 */
internal val KEEP_AS_DRAWN = setOf("Groups")

/**
 * Ends every icon's builder. A glyph that spills past [ICON_LIVE_AREA] is shrunk about the centre
 * of its box until it fits; a glyph inside it, or named in [KEEP_AS_DRAWN], is left exactly as
 * Google drew it, and nothing is ever enlarged. Anything that cannot be measured or rebuilt keeps
 * the glyph as generated, so a bad measurement can only leave an icon unfitted, never break it.
 */
internal fun ImageVector.Builder.buildSimpIcon(): ImageVector {
    val glyph = build()
    val scale = glyph.fitScale()
    if (scale == 1f && !SimpIcons.debugRings) return glyph
    return try {
        glyph.rebuilt(scale)
    } catch (e: Exception) {
        Logger.e(TAG, "${glyph.name}: kept unfitted, rebuilding it failed", e)
        glyph
    }
}

/** 1 when the glyph already fits, is kept as drawn or cannot be measured; otherwise the shrink that fits it. */
private fun ImageVector.fitScale(): Float {
    if (name in KEEP_AS_DRAWN) return 1f
    val limit = minOf(viewportWidth, viewportHeight) * ICON_LIVE_AREA / 2f
    val measured = extent()
    return when {
        measured == null -> {
            Logger.w(TAG, "$name: kept unfitted, its paths cannot be measured")
            1f
        }
        measured.isNaN() || measured.isInfinite() -> {
            Logger.w(TAG, "$name: kept unfitted, measured extent $measured")
            1f
        }
        measured <= limit + FIT_TOLERANCE -> 1f
        limit / measured < MIN_FIT_SCALE -> {
            Logger.w(TAG, "$name: kept unfitted, extent $measured of $viewportWidth looks wrong")
            1f
        }
        else -> limit / measured
    }
}

/**
 * How far the glyph reaches from the centre of its box along either axis, so the smallest centred
 * square holding it is twice this wide; null when it cannot be measured.
 */
private fun ImageVector.extent(): Float? {
    val cx = viewportWidth / 2f
    val cy = viewportHeight / 2f
    var far = 0f
    for (node in root) {
        // Generated icons are flat lists of paths; a group may carry a transform this does not apply.
        val path = node as? VectorPath ?: return null
        far = max(far, path.pathData.extent(cx, cy) ?: return null)
    }
    return far
}

/**
 * How far an outline reaches from ([cx], [cy]) along either axis, following SVG path semantics.
 * Arcs return null: the generator never emits them, and their bulge is not measured here.
 */
internal fun List<PathNode>.extent(
    cx: Float,
    cy: Float,
): Float? {
    var far = 0f
    var x = 0f
    var y = 0f
    var startX = 0f
    var startY = 0f
    // The last control point, mirrored by the reflective commands that follow a curve of their kind.
    var ctrlX = 0f
    var ctrlY = 0f
    var previous: PathNode? = null

    fun visit(
        px: Float,
        py: Float,
    ) {
        far = max(far, max(abs(px - cx), abs(py - cy)))
    }

    fun quad(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        for (i in 1..CURVE_SAMPLES) {
            val t = i / CURVE_SAMPLES.toFloat()
            val mt = 1f - t
            visit(mt * mt * x + 2f * mt * t * x1 + t * t * x2, mt * mt * y + 2f * mt * t * y1 + t * t * y2)
        }
        ctrlX = x1
        ctrlY = y1
        x = x2
        y = y2
    }

    fun cubic(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        for (i in 1..CURVE_SAMPLES) {
            val t = i / CURVE_SAMPLES.toFloat()
            val mt = 1f - t
            visit(
                mt * mt * mt * x + 3f * mt * mt * t * x1 + 3f * mt * t * t * x2 + t * t * t * x3,
                mt * mt * mt * y + 3f * mt * mt * t * y1 + 3f * mt * t * t * y2 + t * t * t * y3,
            )
        }
        ctrlX = x2
        ctrlY = y2
        x = x3
        y = y3
    }

    fun lineTo(
        px: Float,
        py: Float,
    ) {
        x = px
        y = py
        visit(x, y)
    }

    for (node in this) {
        val afterQuad = previous?.isQuad == true
        val afterCurve = previous?.isCurve == true
        when (node) {
            is PathNode.MoveTo -> {
                lineTo(node.x, node.y)
                startX = x
                startY = y
            }
            is PathNode.RelativeMoveTo -> {
                lineTo(x + node.dx, y + node.dy)
                startX = x
                startY = y
            }
            is PathNode.LineTo -> lineTo(node.x, node.y)
            is PathNode.RelativeLineTo -> lineTo(x + node.dx, y + node.dy)
            is PathNode.HorizontalTo -> lineTo(node.x, y)
            is PathNode.RelativeHorizontalTo -> lineTo(x + node.dx, y)
            is PathNode.VerticalTo -> lineTo(x, node.y)
            is PathNode.RelativeVerticalTo -> lineTo(x, y + node.dy)
            is PathNode.QuadTo -> quad(node.x1, node.y1, node.x2, node.y2)
            is PathNode.RelativeQuadTo -> quad(x + node.dx1, y + node.dy1, x + node.dx2, y + node.dy2)
            is PathNode.ReflectiveQuadTo ->
                if (afterQuad) quad(2f * x - ctrlX, 2f * y - ctrlY, node.x, node.y) else quad(x, y, node.x, node.y)
            is PathNode.RelativeReflectiveQuadTo ->
                if (afterQuad) {
                    quad(2f * x - ctrlX, 2f * y - ctrlY, x + node.dx, y + node.dy)
                } else {
                    quad(x, y, x + node.dx, y + node.dy)
                }
            is PathNode.CurveTo -> cubic(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
            is PathNode.RelativeCurveTo ->
                cubic(x + node.dx1, y + node.dy1, x + node.dx2, y + node.dy2, x + node.dx3, y + node.dy3)
            is PathNode.ReflectiveCurveTo ->
                if (afterCurve) {
                    cubic(2f * x - ctrlX, 2f * y - ctrlY, node.x1, node.y1, node.x2, node.y2)
                } else {
                    cubic(x, y, node.x1, node.y1, node.x2, node.y2)
                }
            is PathNode.RelativeReflectiveCurveTo ->
                if (afterCurve) {
                    cubic(2f * x - ctrlX, 2f * y - ctrlY, x + node.dx1, y + node.dy1, x + node.dx2, y + node.dy2)
                } else {
                    cubic(x, y, x + node.dx1, y + node.dy1, x + node.dx2, y + node.dy2)
                }
            is PathNode.ArcTo, is PathNode.RelativeArcTo -> return null
            PathNode.Close -> {
                x = startX
                y = startY
            }
        }
        previous = node
    }
    return far
}

/** The same icon with its glyph scaled by [scale] about the centre of its box, and the debug square. */
private fun ImageVector.rebuilt(scale: Float): ImageVector {
    val builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            tintColor = tintColor,
            tintBlendMode = tintBlendMode,
            autoMirror = autoMirror,
        )
    builder.addGroup(scaleX = scale, scaleY = scale, pivotX = viewportWidth / 2f, pivotY = viewportHeight / 2f)
    for (node in root) {
        val path = node as? VectorPath ?: error("$name: a group cannot be carried over")
        builder.addPath(
            pathData = path.pathData,
            pathFillType = path.pathFillType,
            name = path.name,
            fill = path.fill,
            fillAlpha = path.fillAlpha,
            stroke = path.stroke,
            strokeAlpha = path.strokeAlpha,
            strokeLineWidth = path.strokeLineWidth,
            strokeLineCap = path.strokeLineCap,
            strokeLineJoin = path.strokeLineJoin,
            strokeLineMiter = path.strokeLineMiter,
            trimPathStart = path.trimPathStart,
            trimPathEnd = path.trimPathEnd,
            trimPathOffset = path.trimPathOffset,
        )
    }
    builder.clearGroup()
    if (SimpIcons.debugRings) {
        // The live area's outline, its inner edge on the area's edge so a glyph that fits touches it
        // without being covered. An icon's tint recolours it like the glyph; the alpha keeps it lighter.
        val half = minOf(viewportWidth, viewportHeight) * ICON_LIVE_AREA / 2f + 0.25f
        val cx = viewportWidth / 2f
        val cy = viewportHeight / 2f
        builder.path(fill = null, stroke = SolidColor(Color.Black), strokeAlpha = 0.6f, strokeLineWidth = 0.5f) {
            moveTo(cx - half, cy - half)
            horizontalLineTo(cx + half)
            verticalLineTo(cy + half)
            horizontalLineTo(cx - half)
            close()
        }
    }
    return builder.build()
}
