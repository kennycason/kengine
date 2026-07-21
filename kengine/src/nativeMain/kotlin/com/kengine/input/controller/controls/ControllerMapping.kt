package com.kengine.input.controller.controls

interface ControllerMapping {
    val name: String
    fun isMatches(controllerName: String): Boolean
    val buttonMappings: Map<Int, ButtonType>
    val gamepadMappings: Map<Buttons, Int>  // maps generic buttons to controller-specific codes
    val alternateGamepadMappings: Map<Buttons, List<Int>>
        get() = emptyMap()
    val buttonAxisMappings: Map<Buttons, List<Int>>
        get() = emptyMap()
    val axisMappings: Map<Int, AxisType>
}
