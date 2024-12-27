package com.kengine.input.controller.controls

object Playstation4 : ControllerMapping {
    override val name = "PS4 Controller"

    // Face buttons
    const val X = 0
    const val O = 1
    const val SQUARE = 2
    const val TRIANGLE = 3

    // Special buttons
    const val SHARE = 4
    const val OPTIONS = 6

    // Stick buttons
    const val L3 = 7
    const val R3 = 8

    // Shoulder buttons
    const val L1 = 9
    const val R1 = 10

    // D-Pad (using same logical IDs as before)
    const val DPAD_UP = 100
    const val DPAD_DOWN = 101
    const val DPAD_LEFT = 102
    const val DPAD_RIGHT = 103

    // Axes
    const val L_STICK_HORIZONTAL_AXIS = 0
    const val L_STICK_VERTICAL_AXIS = 1
    const val R_STICK_HORIZONTAL_AXIS = 2
    const val R_STICK_VERTICAL_AXIS = 3
    const val L2_AXIS = 4
    const val R2_AXIS = 5

    override val buttonMappings = mapOf(
        // Face buttons
        X to ButtonType.REGULAR,
        O to ButtonType.REGULAR,
        SQUARE to ButtonType.REGULAR,
        TRIANGLE to ButtonType.REGULAR,

        // Special buttons
        SHARE to ButtonType.REGULAR,
        OPTIONS to ButtonType.REGULAR,

        // Stick buttons
        L3 to ButtonType.REGULAR,
        R3 to ButtonType.REGULAR,

        // Shoulder buttons
        L1 to ButtonType.REGULAR,
        R1 to ButtonType.REGULAR,

        // D-Pad
        DPAD_UP to ButtonType.HAT_UP,
        DPAD_DOWN to ButtonType.HAT_DOWN,
        DPAD_LEFT to ButtonType.HAT_LEFT,
        DPAD_RIGHT to ButtonType.HAT_RIGHT
    )

    // Optional: Add axis mappings if you want to normalize/configure axis handling
    val axisMappings = mapOf(
        L_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        L_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        R_STICK_HORIZONTAL_AXIS to AxisType.STICK_X,
        R_STICK_VERTICAL_AXIS to AxisType.STICK_Y,
        L2_AXIS to AxisType.TRIGGER,
        R2_AXIS to AxisType.TRIGGER
    )
}
