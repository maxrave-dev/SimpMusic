package com.mocharealm.accompanist.lyrics.ui.utils.modifier

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

class SpringPlacementModifierNode(
    var lookaheadScope: LookaheadScope,
    var itemKey: Any,
    var isManualScrolling: Boolean,
    var stiffness: Float = 140f,
    var dampingRatio: Float = 0.85f
) : ApproachLayoutModifierNode, Modifier.Node() {
    private var offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)

    private var isFirstFrame = true

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        val target = with(lookaheadScope) {
            lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
        }
        val shouldSpring = !isManualScrolling && !isFirstFrame
        offsetAnimation.updateTarget(target, coroutineScope, if (shouldSpring) spring(dampingRatio = dampingRatio, stiffness = stiffness) else snap())
        return !offsetAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val coordinates = coordinates
            if (coordinates != null) {
                val target = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
                }

                val shouldSpring = !isManualScrolling && !isFirstFrame
                val animatedOffset = offsetAnimation.updateTarget(
                    target,
                    coroutineScope,
                    if (shouldSpring) spring(dampingRatio = dampingRatio, stiffness = stiffness) else snap()
                )

                isFirstFrame = false

                val placementOffset = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                }

                val delta = animatedOffset - placementOffset
                placeable.place(delta.x, delta.y)
            } else {
                placeable.place(0, 0)
            }
        }
    }

    fun updateState(
        newScope: LookaheadScope,
        newKey: Any,
        newIsManualScrolling: Boolean,
        newStiffness: Float,
        newDampingRatio: Float = 0.85f
    ) {
        lookaheadScope = newScope
        isManualScrolling = newIsManualScrolling
        stiffness = newStiffness
        dampingRatio = newDampingRatio
        if (itemKey != newKey) {
            itemKey = newKey
            offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
            isFirstFrame = true
        }
    }
}

data class SpringPlacementNodeElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val stiffness: Float = 140f,
    val dampingRatio: Float = 0.85f
) : ModifierNodeElement<SpringPlacementModifierNode>() {
    override fun update(node: SpringPlacementModifierNode) {
        node.updateState(lookaheadScope, itemKey, isManualScrolling, stiffness, dampingRatio)
    }
    override fun create(): SpringPlacementModifierNode =
        SpringPlacementModifierNode(lookaheadScope, itemKey, isManualScrolling, stiffness, dampingRatio)
}

fun Modifier.springPlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float = 140f,
    dampingRatio: Float = 0.85f
) = this.then(SpringPlacementNodeElement(lookaheadScope, itemKey, isManualScrolling, stiffness, dampingRatio))