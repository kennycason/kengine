package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ControllerMapping

object XboxSeriesX : ControllerMapping {
    override val name = "Xbox Series X Controller"

    override fun isMatches(controllerName: String): Boolean {
        return controllerName.contains("Xbox", ignoreCase = true) &&
               (controllerName.contains("Series", ignoreCase = true) ||
                // Some systems just report it as "Xbox Controller"
                (!controllerName.contains("One", ignoreCase = true) &&
                 !controllerName.contains("360", ignoreCase = true)))
    }

    // same mappings as Xbox One
    const val A = XboxOne.A
    const val B = XboxOne.B
    const val X = XboxOne.X
    const val Y = XboxOne.Y
    const val VIEW = XboxOne.VIEW
    const val MENU = XboxOne.MENU
    const val XBOX = XboxOne.XBOX
    const val SHARE = XboxOne.SHARE
    const val L3 = XboxOne.L3
    const val R3 = XboxOne.R3
    const val LB = XboxOne.LB
    const val RB = XboxOne.RB
    const val LT = XboxOne.LT
    const val RT = XboxOne.RT
    const val DPAD_UP = XboxOne.DPAD_UP
    const val DPAD_DOWN = XboxOne.DPAD_DOWN
    const val DPAD_LEFT = XboxOne.DPAD_LEFT
    const val DPAD_RIGHT = XboxOne.DPAD_RIGHT

    const val L_STICK_HORIZONTAL_AXIS = XboxOne.L_STICK_HORIZONTAL_AXIS
    const val L_STICK_VERTICAL_AXIS = XboxOne.L_STICK_VERTICAL_AXIS
    const val R_STICK_HORIZONTAL_AXIS = XboxOne.R_STICK_HORIZONTAL_AXIS
    const val R_STICK_VERTICAL_AXIS = XboxOne.R_STICK_VERTICAL_AXIS
    const val LT_AXIS = XboxOne.LT_AXIS
    const val RT_AXIS = XboxOne.RT_AXIS

    override val buttonMappings = XboxOne.buttonMappings
    override val gamepadMappings = XboxOne.gamepadMappings

    override val axisMappings = mapOf(
        XboxOne.L_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        XboxOne.L_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        XboxOne.R_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        XboxOne.R_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        XboxOne.LT to AxisType.TRIGGER,
        XboxOne.RT to AxisType.TRIGGER
    )
}
