package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

/**
 * An attempt to match most generic SNES style USB controllers
 */
object SNES : ControllerMapping {
    override val name = "SNES"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("SNES", ignoreCase = false) ||
            controllerName.contains("Super Nintendo Entertainment System", ignoreCase = true)
    }

    const val B = 0
    const val A = 1
    const val Y = 2
    const val X = 3
    const val SELECT = 4
    const val START = 6
    const val L = 9
    const val R = 10

    // D-Pad using standard logical IDs
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    override val buttonMappings = mapOf(
        B to ButtonType.REGULAR,       // 0
        A to ButtonType.REGULAR,       // 1
        Y to ButtonType.REGULAR,       // 2
        X to ButtonType.REGULAR,       // 3
        SELECT to ButtonType.REGULAR,  // 4
        START to ButtonType.REGULAR,   // 6
        L to ButtonType.REGULAR,       // 9
        R to ButtonType.REGULAR,       // 10
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
        Buttons.L1 to L,
        Buttons.R1 to R,
        Buttons.SELECT to SELECT,
        Buttons.START to START,
        Buttons.DPAD_UP to DPAD_UP,
        Buttons.DPAD_DOWN to DPAD_DOWN,
        Buttons.DPAD_LEFT to DPAD_LEFT,
        Buttons.DPAD_RIGHT to DPAD_RIGHT
    )

    override val axisMappings: Map<Int, AxisType> = linkedMapOf()
}
