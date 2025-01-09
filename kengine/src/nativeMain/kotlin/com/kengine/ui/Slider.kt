package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State
import kotlin.math.max
import kotlin.math.min

class Slider(
    id: String,
    // "desired" positioning & size
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,  // Required width
    h: Double,  // Required height

    // Extra slider fields
    val min: Double = 0.0,
    val max: Double = 100.0,
    private val state: State<Double>,
    private val onValueChanged: ((Double) -> Unit)? = null,

    padding: Double = 5.0,
    bgColor: Color? = null,
    bgSprite: Sprite? = null,

    // Track / handle customization
    private val trackWidth: Double? = null,
    private val trackColor: Color = Color.gray10,
    private val handleWidth: Double? = null,
    private val handleHeight: Double? = null,
    private val handleColor: Color = Color.white,
    private val handleSprite: Sprite? = null,

    parent: View? = null
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    bgColor = bgColor,
    bgSprite = bgSprite,
    padding = padding,
    parent = parent
) {
    /**
     * True while user is dragging this slider's handle.
     */
    var isDragging: Boolean = false
        private set

    /**
     * If user sets no custom trackWidth, we’ll default to a fraction of layoutW.
     */
    private val actualTrackWidth: Double
        get() = trackWidth ?: (layoutW * 0.2)  // e.g. 20% of total width

    /**
     * Similarly for handle dimensions, if not specified:
     */
    private val actualHandleWidth: Double
        get() = handleWidth ?: (layoutW * 0.4) // e.g. 40% of total width

    private val actualHandleHeight: Double
        get() = handleHeight ?: actualHandleWidth // or just a square handle

    /**
     * The vertical space for the slider track. We'll assume it's fully vertical.
     */
    private val effectiveTrackHeight: Double
        get() = layoutH - (padding * 2.0)

    /**
     * Returns the top Y offset of the handle in final layout coordinates.
     * By default, we interpret "slider" as vertical: top-to-bottom.
     *
     *   - state.get() in [min.. max]
     *   - handle is at top for "max," at bottom for "min."
     *   - You can invert if you prefer the opposite direction.
     */
    private fun calculateHandleY(): Double {
        val range = max - min
        val fraction = (state.get() - min) / range   // [0..1]
        val available = effectiveTrackHeight - actualHandleHeight

        // If we want the "max" at top, we do (1 - fraction).
        val topOffset = padding + (1.0 - fraction) * available
        return layoutY + topOffset
    }

    /**
     * We'll place the track in the center (horizontal) by default:
     */
    private fun trackX(): Double = layoutX + (layoutW / 2.0) - (actualTrackWidth / 2.0)
    private fun trackY(): Double = layoutY + padding

    /**
     * We center the handle horizontally:
     */
    private fun handleX(): Double = layoutX + (layoutW / 2.0) - (actualHandleWidth / 2.0)
    private fun handleY(): Double = calculateHandleY()

    /**
     * Convert an absolute mouseY coordinate into a slider value [min.. max].
     * We clamp the result so it doesn't overshoot.
     */
    private fun valueAt(absMouseY: Double): Double {
        // 1) Identify top/bottom (the track region)
        val topY    = layoutY + padding
        val bottomY = layoutY + layoutH - padding - actualHandleHeight

        // 2) Always reorder them to (yMin..yMax)
        val yMin = min(topY, bottomY)
        val yMax = max(topY, bottomY)

        // 3) Clamp the mouse to that range
        val clampedY = absMouseY.coerceIn(yMin, yMax)

        // 4) Suppose we want top=“max” and bottom=“min”.
        val range = max - min
        // fraction=1 means clampedY==yMin => “top”
        val fraction = 1.0 - (clampedY - yMin) / (yMax - yMin)
        val newVal = min + fraction * range
        return newVal.coerceIn(min, max)
    }

    override fun draw() {
        // Return early if hidden
        if (!visible) return

        // 1) Draw our background if we want (this uses layoutX/Y/W/H)
        super.draw()  // Optionally let the parent do "fillRectangle()" for bg

        // 2) We'll draw the "track"
        val xTrack = trackX()
        val yTrack = trackY()
        val hTrack = effectiveTrackHeight

        useGeometryContext {
            fillRectangle(
                xTrack,  // left
                yTrack,  // top
                actualTrackWidth,
                hTrack,
                trackColor
            )
        }

        // 3) Draw the handle
        val hx = handleX()
        val hy = handleY()

        useGeometryContext {
            if (handleSprite != null) {
                // If we have a custom sprite, draw that
                handleSprite.draw(hx, hy)
            } else {
                // Otherwise, fill a rectangle or circle
                fillRectangle(hx, hy, actualHandleWidth, actualHandleHeight, handleColor)
            }
        }
    }

    /**
     * When user clicks inside the slider area, let's see if they clicked the handle.
     * Or, if you want track-click behavior, you can jump the handle there.
     */
    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // If user clicked anywhere within the slider’s bounding box,
        // we consider that a drag begin:
        if (isWithinBounds(mouseX, mouseY)) {
            isDragging = true

            // Claim exclusive drag focus so we can keep updating
            // even if mouse moves outside slider bounds:
            getViewContext().setDragFocus(this)

            onClick?.invoke()
            logger.info("Slider `$id` => click => isDragging=true")

            // Optionally jump the handle right away:
            updateValue(mouseX, mouseY)
        }
    }

    /**
     * If the user is dragging, update the state on each hover call.
     */
    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // If we lost drag focus, forcibly end drag
        if (!getViewContext().isDragging(this)) {
            if (isDragging) {
                isDragging = false
                logger.info("Slider `$id` => forcibly ended drag (lost focus).")
            }
            return
        }

        // If *are* still the dragFocus => update value
        if (isDragging) {
            updateValue(mouseX, mouseY)
        }
    }

    /**
     * Release ends the drag.
     * (Though, you might require they release inside the handle? It's up to you.)
     */
    override fun release(mouseX: Double, mouseY: Double) {
        if (!visible) return

        if (isDragging) {
            isDragging = false
            logger.info("Slider `$id` => release => isDragging=false")
        }

        // Release the dragFocus if we had it
        getViewContext().clearDragFocus(this)

        // Then call our optional onRelease and pass it to children
        super.release(mouseX, mouseY)
    }

    /**
     * For the "click" or "hover" or "release" to consider the entire track+handle,
     * you could define isWithinBounds() to check the entire track area or just the handle area.
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val left   = layoutX
        val right  = layoutX + layoutW
        val top    = layoutY
        val bottom = layoutY + layoutH

        return (
            mouseX >= left && mouseX <= right &&
                mouseY >= top  && mouseY <= bottom
            )
    }

    /**
     * Helper to set state & call onValueChanged.
     */
    private fun updateValue(mouseX: Double, mouseY: Double) {
        val newVal = valueAt(mouseY)
        state.set(newVal)
        onValueChanged?.invoke(newVal)
    }

}
