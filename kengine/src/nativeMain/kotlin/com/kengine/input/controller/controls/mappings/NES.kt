package com.kengine.input.controller.controls.mappings

import com.kengine.input.controller.controls.AxisType
import com.kengine.input.controller.controls.ButtonType
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.controls.ControllerMapping

/**
* An attempt to match most generic NES style USB controllers
* Classic 8-bit layout with just A, B, Start, Select, and D-pad
*/
object NES : ControllerMapping {
   override val name = "NES"

   override fun isMatches(controllerName: String): Boolean {
       return controllerName.contains("NES", ignoreCase = false) ||
              controllerName.contains("Nintendo Entertainment System", ignoreCase = true)
   }

   // NES-style button layout - much simpler than SNES
   const val B = 0      // Left button
   const val A = 1      // Right button
   const val SELECT = 2
   const val START = 3

   // D-Pad using standard logical IDs
   const val DPAD_UP = 100
   const val DPAD_DOWN = 101
   const val DPAD_LEFT = 102
   const val DPAD_RIGHT = 103

   override val buttonMappings = mapOf(
       B to ButtonType.REGULAR,
       A to ButtonType.REGULAR,
       SELECT to ButtonType.REGULAR,
       START to ButtonType.REGULAR,
       DPAD_UP to ButtonType.HAT_UP,
       DPAD_DOWN to ButtonType.HAT_DOWN,
       DPAD_LEFT to ButtonType.HAT_LEFT,
       DPAD_RIGHT to ButtonType.HAT_RIGHT
   )

   override val gamepadMappings = mapOf(
       Buttons.B to B,        // Maps to original NES B
       Buttons.A to A,        // Maps to original NES A
       Buttons.SELECT to SELECT,
       Buttons.START to START,
       Buttons.DPAD_UP to DPAD_UP,
       Buttons.DPAD_DOWN to DPAD_DOWN,
       Buttons.DPAD_LEFT to DPAD_LEFT,
       Buttons.DPAD_RIGHT to DPAD_RIGHT
   )

    override val axisMappings: Map<Int, AxisType> = linkedMapOf()
}
