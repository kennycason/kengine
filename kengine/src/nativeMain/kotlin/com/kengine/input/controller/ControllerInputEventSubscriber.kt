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
import sdl3.SDL_EVENT_JOYSTICK_BALL_MOTION
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
                    val joystick = SDL_OpenJoystick(instanceId)  // New way to open joystick
                    if (joystick != null) {
                        println("Controller ${SDL_GetJoystickName(joystick)?.toKString()} connected.")
                        buttonStates[i.toUInt()] = ControllerState(
                            axes = FloatArray(SDL_GetNumJoystickAxes(joystick)),
                            buttons = BooleanArray(SDL_GetNumJoystickButtons(joystick))
                        )
                    } else {
                        logger.warn { "Failed to open joystick $i: ${SDL_GetError()!!.toKString()}" }
                    }
                }
            }
        }
    }

    private fun handleControllerEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_JOYSTICK_BUTTON_DOWN -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button
                buttonStates[joystickID]?.buttons?.set(button.toInt(), true)
            }
            SDL_EVENT_JOYSTICK_BUTTON_UP -> {
                val joystickID = event.jbutton.which
                val button = event.jbutton.button
                buttonStates[joystickID]?.buttons?.set(button.toInt(), false)
            }
            SDL_EVENT_JOYSTICK_AXIS_MOTION -> {
                val joystickID = event.jaxis.which
                val axis = event.jaxis.axis
                val value = event.jaxis.value / 32767.0f // Normalize to -1.0 to 1.0
                buttonStates[joystickID]?.axes?.set(axis.toInt(), value)
            }
            SDL_EVENT_JOYSTICK_BALL_MOTION,
            SDL_EVENT_JOYSTICK_HAT_MOTION,
            SDL_EVENT_JOYSTICK_ADDED,
            SDL_EVENT_JOYSTICK_REMOVED -> {
                logger.info { "Unhandled controller events ${event.type}" }
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
        for (controllerButtonStates in buttonStates.entries) {
            if (controllerButtonStates.value.buttons.getOrElse(buttonIndex) { false }) return true
        }
        return false
    }

}
