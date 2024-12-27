package com.kengine.input.controller.controls

interface ControllerMapping {
    val name: String
    val buttonMappings: Map<Int, ButtonType>
}

enum class ButtonType {
    REGULAR,  // non-hat/normal button press
    HAT_UP,
    HAT_DOWN,
    HAT_LEFT,
    HAT_RIGHT
}

enum class AxisType {
    STICK_X,   // Horizontal stick movement
    STICK_Y,   // Vertical stick movement
    TRIGGER    // L2/R2 triggers
}
