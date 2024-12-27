package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

object NintendoSwitch : ControllerMapping {
    override val name = "Nintendo Switch Pro Controller"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("Nintendo", ignoreCase = true) &&
               (controllerName.contains("Pro Controller", ignoreCase = true) ||
                controllerName.contains("Switch", ignoreCase = true))
    }

    // Face buttons
    const val B = 0
    const val A = 1
    const val Y = 2
    const val X = 3

    // Special buttons
    const val MINUS = 4  // Select
    const val PLUS = 6   // Start
    const val HOME = 5
    const val CAPTURE = 7

    // Stick buttons
    const val L3 = 7
    const val R3 = 8

    // Shoulder buttons
    const val L1 = 9
    const val R1 = 10

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
    const val ZL_TRIGGER_AXIS: Int = 4
    const val ZR_TRIGGER_AXIS: Int = 5

    override val buttonMappings = mapOf(
        B to ButtonType.REGULAR,
        A to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        X to ButtonType.REGULAR,
        MINUS to ButtonType.REGULAR,
        PLUS to ButtonType.REGULAR,
        HOME to ButtonType.REGULAR,
        CAPTURE to ButtonType.REGULAR,
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,
        L1 to ButtonType.REGULAR,
        R1 to ButtonType.REGULAR,
        ZL_TRIGGER_AXIS to ButtonType.REGULAR,
        ZR_TRIGGER_AXIS to ButtonType.REGULAR,
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    override val gamepadMappings = mapOf(
        Buttons.B to B,
        Buttons.A to A,
        Buttons.Y to Y,
        Buttons.X to X,

        Buttons.SELECT to MINUS,
        Buttons.START to PLUS,

        Buttons.L1 to L1,
        Buttons.R1 to R1,
        Buttons.L2 to ZL_TRIGGER_AXIS,
        Buttons.R2 to ZR_TRIGGER_AXIS,
        Buttons.L3 to L3,
        Buttons.R3 to R3,

        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    val axisMappings = mapOf(
        L_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        L_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        R_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        R_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        ZL_TRIGGER_AXIS to AxisType.TRIGGER,
        ZR_TRIGGER_AXIS to AxisType.TRIGGER
    )
}
