package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

object XboxOne : ControllerMapping {
    override val name = "Xbox One Controller"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("Xbox", ignoreCase = true) &&
               (controllerName.contains("One", ignoreCase = true) ||
                controllerName.contains("Xbox Wireless", ignoreCase = true))
    }

    // Face buttons
    const val A = 0
    const val B = 1
    const val X = 2
    const val Y = 3

    // Special buttons
    const val VIEW = 4      // Select/Back
    const val MENU = 6      // Start
    const val XBOX = 5      // Guide button
    const val SHARE = 7     // Share button (on newer models)

    // Stick buttons
    const val L3 = 8
    const val R3 = 9

    // Shoulder buttons
    const val LB = 10
    const val RB = 11
    const val LT = 12
    const val RT = 13

    // D-Pad
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Axes
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3
    const val LT_AXIS = 4
    const val RT_AXIS = 5

    override val buttonMappings = mapOf(
        A to ButtonType.REGULAR,
        B to ButtonType.REGULAR,
        X to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        VIEW to ButtonType.REGULAR,
        MENU to ButtonType.REGULAR,
        XBOX to ButtonType.REGULAR,
        SHARE to ButtonType.REGULAR,
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,
        LB to ButtonType.REGULAR,
        RB to ButtonType.REGULAR,
        LT to ButtonType.REGULAR,
        RT to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        // Map to our standard Nintendo-based layout
        Buttons.B to A,    // A on Xbox is B position
        Buttons.A to B,    // B on Xbox is A position
        Buttons.Y to X,    // X on Xbox is Y position
        Buttons.X to Y,    // Y on Xbox is X position

        Buttons.SELECT to VIEW,
        Buttons.START to MENU,

        Buttons.L1 to LB,
        Buttons.R1 to RB,
        Buttons.L2 to LT,
        Buttons.R2 to RT,
        Buttons.L3 to L3,
        Buttons.R3 to R3,

        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    override val axisMappings = mapOf(
        L_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        L_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        R_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        R_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        LT to AxisType.TRIGGER,
        RT to AxisType.TRIGGER
    )
}
