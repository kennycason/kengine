//package com.kengine.ui
//
//import com.kengine.geometry.useGeometryContext
//import com.kengine.graphics.Color
//import com.kengine.graphics.Sprite
//import com.kengine.hooks.state.State
//
//class Knob(
//    id: String,
//    x: Double,
//    y: Double,
//    w: Double,  // Required width
//    h: Double,  // Required height
//    padding: Double = 5.0,
//    val min: Double = 0.0,
//    val max: Double = 100.0,
//    private val state: State<Double>,
//    private val onValueChanged: ((Double) -> Unit)? = null,
//    stepSize: Double? = null,
//    bgColor: Color? = null,
//    bgSprite: Sprite? = null,
//    private val knobColor: Color = Color.gray10,
//    private val indicatorColor: Color = Color.white,
//    parent: View? = null
//) : View(
//    id = id,
//    x = x,
//    y = y,
//    w = w,
//    h = h,
//    padding = padding,
//    bgColor = bgColor,
//    bgImage = bgSprite,
//    parent = parent
//) {
//    // Drag tracking
//    private var dragStartY: Double = 0.0
//    private var dragStartValue: Double = 0.0
//
//    // Constants for knob behavior
//    private val rotationRange = 270.0  // Degrees of rotation
//    private val startAngle = 135.0     // Start at bottom left
//    private val stepSize: Double = adaptStepSize(stepSize, min, max)
//    private val dragSensitivity: Double = calculateDragSensitivity(this.stepSize, min, max)
//
//    // Calculates dynamic step size based on range
//    private fun adaptStepSize(stepSize: Double?, min: Double, max: Double): Double {
//        if (stepSize != null) return stepSize
//
//        val range = max - min
//        return when {
//            range > 1000 -> range / 100
//            range > 100 -> range / 50
//            range > 10 -> range / 25
//            else -> range / 100
//        }
//    }
//
//    // Sensitivity scales with range
//    private fun calculateDragSensitivity(step: Double, min: Double, max: Double): Double {
//        val range = max - min
//        return when {
//            range <= 1.0 -> step
//            range <= 10.0 -> step * 2.0
//            range <= 100.0 -> step * 5.0
//            else -> step / 10.0
//        }
//    }
//
//    private fun valueToAngle(value: Double): Double {
//        val normalizedValue = (value - min) / (max - min)
//        return startAngle + (normalizedValue * rotationRange)
//    }
//
//    private fun calculateValueFromDrag(currentY: Double): Double {
//        val deltaY = dragStartY - currentY
//        val steps = (deltaY / dragSensitivity).toInt()
//        val deltaValue = steps * stepSize
//        return (dragStartValue + deltaValue).coerceIn(min, max)
//    }
//
//    /**
//     * Draws the knob and its indicator.
//     */
//    override fun draw(parentX: Double, parentY: Double) {
//        if (!visible) return
//
//        val absX = parentX + x
//        val absY = parentY + y
//
//        // Draw background if specified
//        super.draw(parentX, parentY)
//
//        val centerX = absX + w / 2.0
//        val centerY = absY + h / 2.0
//        val radius = (kotlin.math.min(w, h) / 2.0) - padding
//
//        useGeometryContext {
//            // Draw knob circle
//            fillCircle(centerX, centerY, radius.toInt(), knobColor)
//
//            // Draw indicator line
//            val angle = valueToAngle(state.get()) * (kotlin.math.PI / 180.0)
//            val indicatorLength = radius * 0.8
//            val endX = centerX + indicatorLength * kotlin.math.cos(angle)
//            val endY = centerY + indicatorLength * kotlin.math.sin(angle)
//
//            drawLine(centerX, centerY, endX, endY, indicatorColor)
//        }
//    }
//
//    /**
//     * Handles click events to start dragging.
//     */
//    override fun click(x: Double, y: Double) {
//        if (!visible || !isWithinBounds(x, y)) return
//
//        // Start dragging
//        activeDragView = this
//        dragStartY = y
//        dragStartValue = state.get().coerceIn(min, max)
//    }
//
//    /**
//     * Handles hover events to update value while dragging.
//     */
//    override fun hover(x: Double, y: Double) {
//        if (activeDragView != this) return // Only update if dragging
//
//        // Update value based on drag position
//        val newValue = calculateValueFromDrag(y)
//        if (kotlin.math.abs(newValue - state.get()) > stepSize / 2) {
//            state.set(newValue)
//            onValueChanged?.invoke(newValue)
//        }
//    }
//
//    /**
//     * Handles release events to stop dragging.
//     */
//    override fun release(x: Double, y: Double) {
//        if (activeDragView == this) {
//            activeDragView = null // Clear drag state
//        }
//        super.release(x, y)
//    }
//
//    /**
//     * Determines if a point is within the knob's bounds.
//     */
//    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
//        val (absX, absY) = getAbsolutePosition()
//        val radius = kotlin.math.min(w, h) / 2.0
//        val centerX = absX + radius
//        val centerY = absY + radius
//        val dx = mouseX - centerX
//        val dy = mouseY - centerY
//        return (dx * dx + dy * dy) <= (radius * radius)
//    }
//}
