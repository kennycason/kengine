package com.kengine.input.controller

import com.kengine.log.Logging
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLEventContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sdl3.SDL_EVENT_JOYSTICK_ADDED
import sdl3.SDL_EVENT_JOYSTICK_AXIS_MOTION
import sdl3.SDL_EVENT_JOYSTICK_BUTTON_DOWN
import sdl3.SDL_EVENT_JOYSTICK_BUTTON_UP
import sdl3.SDL_EVENT_JOYSTICK_HAT_MOTION
import sdl3.SDL_EVENT_JOYSTICK_REMOVED
import sdl3.SDL_Event
import sdl3.SDL_GetError
import sdl3.SDL_GetJoystickName
import sdl3.SDL_GetJoysticks
import sdl3.SDL_GetNumJoystickAxes
import sdl3.SDL_GetNumJoystickButtons
import sdl3.SDL_OpenJoystick

@OptIn(ExperimentalForeignApi::class)
class ControllerInputEventSubscriber : Logging {
    private val buttonStates = mutableMapOf<UInt, ControllerState>()

    data class ControllerState(
        var axes: FloatArray,
        var buttons: BooleanArray
    )

    fun init() {
        useSDLEventContext {
            logger.info { "Subscribed to controller events" }
            subscribe(SDLEventContext.EventType.CONTROLLER, ::handleControllerEvent)
        }

        memScoped {
            val countPtr = alloc<IntVar>()
            val joysticks = SDL_GetJoysticks(countPtr.ptr)
            val count = countPtr.value
            logger.info { "Controllers found: $count" }

            joysticks?.let { joyArray ->
                for (i in 0 until count) {
                    val instanceId = joyArray[i]
                    val joystick = SDL_OpenJoystick(instanceId)
                    if (joystick != null) {
                        val numAxes = SDL_GetNumJoystickAxes(joystick)
                        val numButtons = SDL_GetNumJoystickButtons(joystick)
                        logger.info {
                            "Controller ${SDL_GetJoystickName(joystick)?.toKString()} connected. " +
                                "ID: $instanceId, Axes: $numAxes, Buttons: $numButtons"
                        }
                        buttonStates[instanceId] = ControllerState(
                            axes = FloatArray(numAxes),
                            buttons = BooleanArray(numButtons)
                        )
                    } else {
                        logger.warn { "Failed to open joystick $i: ${SDL_GetError()?.toKString()}" }
                    }
                }
            }
        }
    }

    private fun handleControllerEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_JOYSTICK_BUTTON_DOWN -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button.toInt()
                if (logger.isDebugEnabled()) {
                    logger.debug { "Button Down - JoystickID: $joystickID, Button: $button" }
                }
                buttonStates[joystickID]?.buttons?.getOrNull(button)?.let {
                    buttonStates[joystickID]?.buttons?.set(button, true)
                }
            }
            SDL_EVENT_JOYSTICK_BUTTON_UP -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button.toInt()
                logger.debug { "Button Up - JoystickID: $joystickID, Button: $button" }

                buttonStates[joystickID]?.buttons?.getOrNull(button)?.let {
                    buttonStates[joystickID]?.buttons?.set(button, false) // Reset to false
                    logger.debug { "Button $button set to false" }
                }
            }
            SDL_EVENT_JOYSTICK_AXIS_MOTION -> {
                val joystickID = event.jaxis.which
                val axis = event.jaxis.axis.toInt()
                val value = event.jaxis.value.toFloat() / 32767.0f
                if (logger.isDebugEnabled()) {
                    logger.debug { "Axis Motion - JoystickID: $joystickID, Axis: $axis, Value: $value" }
                }
                buttonStates[joystickID]?.axes?.getOrNull(axis)?.let {
                    buttonStates[joystickID]?.axes?.set(axis, value)
                }
            }
            SDL_EVENT_JOYSTICK_AXIS_MOTION -> {
                val joystickID = event.jaxis.which
                val axis = event.jaxis.axis.toInt()
                val value = event.jaxis.value.toFloat() / 32767.0f
                buttonStates[joystickID]?.axes?.getOrNull(axis)?.let {
                    buttonStates[joystickID]?.axes?.set(axis, value)
                }
            }
            SDL_EVENT_JOYSTICK_HAT_MOTION -> {
                val joystickID = event.jhat.which
                val hatValue = event.jhat.value.toInt()

                // Map hat values to D-Pad buttons
                // TODO figure out how to handle without hard-coded values.
                // currently thorws exception
                buttonStates[joystickID]?.buttons?.let { buttons ->
                    println("uh oh")
                    buttons[11] = hatValue and 0x01 != 0  // UP
                    buttons[12] = hatValue and 0x02 != 0 // RIGHT
                    buttons[13] = hatValue and 0x04 != 0  // DOWN
                    buttons[14] = hatValue and 0x08 != 0  // LEFT

                    logger.debug {
                        "DPad State: UP=${buttons[11]}, " +
                            "RIGHT=${buttons[12]}, " +
                            "DOWN=${buttons[13]}, " +
                            "LEFT=${buttons[14]}"
                    }
                }
            }

            SDL_EVENT_JOYSTICK_ADDED -> {
                logger.info { "Controller added" }
                // Re-enumerate controllers
                initControllers()
            }
            SDL_EVENT_JOYSTICK_REMOVED -> {
                logger.info { "Controller removed" }
                val joystickID = event.jdevice.which
                buttonStates.remove(joystickID)
            }
        }
    }

    fun getAxisValue(controllerId: UInt, axisIndex: Int): Float {
        return buttonStates[controllerId]?.axes?.get(axisIndex) ?: 0.0f
    }

    fun getAxisValue(axisIndex: Int): Float {
        for (controllerButtonStates in buttonStates.entries) {
            val axisState = controllerButtonStates.value.axes.getOrElse(axisIndex) { 0.0f }
            if (axisState != 0.0f) return axisState
        }
        return 0.0f
    }

    fun isButtonPressed(controllerId: UInt, buttonIndex: Int): Boolean {
        return buttonStates[controllerId]?.buttons?.get(buttonIndex) ?: false
    }

    fun isButtonPressed(buttonIndex: Int): Boolean {
        val result = buttonStates.any { (id, state) ->
            state.buttons.getOrNull(buttonIndex)?.also {
                logger.debug { "Controller $id Button $buttonIndex state: $it" }
            } ?: false
        }
        logger.debug { "isButtonPressed($buttonIndex) = $result" }
        return result
    }

    fun cleanup() {
        logger.info { "Cleaning up ControllerInputEventSubscriber" }
        buttonStates.clear()
    }

    private fun initControllers() {
        memScoped {
            val countPtr = alloc<IntVar>()
            val joysticks = SDL_GetJoysticks(countPtr.ptr)
            val count = countPtr.value
            logger.info { "Controllers found: $count" }

            joysticks?.let { joyArray ->
                for (i in 0 until count) {
                    val instanceId = joyArray[i]
                    SDL_OpenJoystick(instanceId)?.let { joystick ->
                        val name = SDL_GetJoystickName(joystick)?.toKString() ?: "Unknown Controller"
                        logger.info { "Controller '$name' connected" }
                        buttonStates[instanceId] = ControllerState(
                            axes = FloatArray(SDL_GetNumJoystickAxes(joystick)),
                            buttons = BooleanArray(SDL_GetNumJoystickButtons(joystick))
                        )
                    } ?: run {
                        logger.warn { "Failed to open joystick $i: ${SDL_GetError()?.toKString()}" }
                    }
                }
            }
        }
    }

}
