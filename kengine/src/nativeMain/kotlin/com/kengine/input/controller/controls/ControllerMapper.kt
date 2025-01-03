package com.kengine.input.controller.controls

import com.kengine.input.controller.controls.mappings.GenericGamepad
import com.kengine.input.controller.controls.mappings.Logitech
import com.kengine.input.controller.controls.mappings.NES
import com.kengine.input.controller.controls.mappings.NintendoSwitch
import com.kengine.input.controller.controls.mappings.Playstation4
import com.kengine.input.controller.controls.mappings.Playstation5
import com.kengine.input.controller.controls.mappings.SNES
import com.kengine.input.controller.controls.mappings.Steam
import com.kengine.input.controller.controls.mappings.XboxOne
import com.kengine.input.controller.controls.mappings.XboxSeriesX

object ControllerMapper {
    private val knownControllers = listOf(
        Playstation4,
        Playstation5,
        NintendoSwitch,
        XboxOne,
        XboxSeriesX,
        Logitech,
        Steam,
        SNES,   // SNES / NES order matters
        NES,
        GenericGamepad  // fallback
    )

    /**
     * Gets the appropriate controller mapping for a given controller name
     * @param controllerName The name returned by SDL for the controller
     * @return The matching ControllerMapping or null if no match found
     */
    fun getMapping(controllerName: String): ControllerMapping? {
        return knownControllers.firstOrNull {
            it.isMatches(controllerName)
        }
    }
}
