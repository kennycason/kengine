package com.kengine.input.controller.controls.mappings

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

    // SNES-style button layout
    const val B = 0      // Bottom button
    const val A = 1      // Right button
    const val Y = 2      // Left button
    const val X = 3      // Top button
    const val L = 4      // Left shoulder
    const val R = 5      // Right shoulder
    const val SELECT = 6
    const val START = 7

    // D-Pad using standard logical IDs
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    override val buttonMappings = mapOf(
        B to ButtonType.REGULAR,
        A to ButtonType.REGULAR,
        Y to ButtonType.REGULAR,
        X to ButtonType.REGULAR,
        L to ButtonType.REGULAR,
        R to ButtonType.REGULAR,
        SELECT to ButtonType.REGULAR,
        START to ButtonType.REGULAR,
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
}
