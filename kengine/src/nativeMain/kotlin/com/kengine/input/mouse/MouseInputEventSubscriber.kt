package com.kengine.input.mouse

//import com.kengine.sdl.SDLContext
//import com.kengine.sdl.SDLEventContext
import com.kengine.math.Vec2
import com.kengine.time.getCurrentMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi

//import sdl2.SDL_BUTTON_LEFT
//import sdl2.SDL_BUTTON_MIDDLE
//import sdl2.SDL_BUTTON_RIGHT
//import sdl2.SDL_Event
//import sdl2.SDL_MOUSEBUTTONDOWN
//import sdl2.SDL_MOUSEBUTTONUP
//import sdl2.SDL_MOUSEMOTION

@OptIn(ExperimentalForeignApi::class)
class MouseInputEventSubscriber {
    private val buttonStates = mutableMapOf<Int, ButtonState>()
    private var mouseCursor: Vec2 = Vec2()

    data class ButtonState(
        var isPressed: Boolean = false,
        var lastPressedTime: Long = 0
    )

    // must be called
    fun init() {
//        useContext <SDLContext> {
//            sdlEvents.subscribe(SDLEventContext.EventType.MOUSE, ::handleMouseEvent)
//        }
    }

//    fun handleMouseEvent(event: SDL_Event) {
//        when (event.type) {
//            SDL_MOUSEBUTTONDOWN -> {
//                val button = event.button.button.toInt()
//                if (!buttonStates.containsKey(button)) {
//                    buttonStates[button] = ButtonState()
//                }
//                buttonStates[button]?.apply {
//                    isPressed = true
//                    lastPressedTime = getCurrentMilliseconds()
//                }
//            }
//            SDL_MOUSEBUTTONUP -> {
//                val button = event.button.button.toInt()
//                buttonStates[button]?.isPressed = false
//            }
//            SDL_MOUSEMOTION -> {
//                mouseCursor.x = event.motion.x.toDouble()
//                mouseCursor.y = event.motion.y.toDouble()
//            }
//        }
//    }

    /**
     * Get mouse cursor (x,y) position
     */
    fun getCursor(): Vec2 = mouseCursor

    /**
     * Check if a specific mouse button is currently pressed.
     */
    fun isButtonPressed(buttonCode: Int): Boolean {
        return buttonStates[buttonCode]?.isPressed ?: false
    }

    /**
     * Check how much time has passed since a button was last pressed.
     */
    fun timeSinceButtonPressed(buttonCode: Int): Long {
        val currentTime = getCurrentMilliseconds()
        return buttonStates[buttonCode]?.let {
            if (it.isPressed) 0L else currentTime - it.lastPressedTime
        } ?: Long.MAX_VALUE
    }
//
//    fun isLeftPressed() = isButtonPressed(SDL_BUTTON_LEFT)
//    fun isRightPressed() = isButtonPressed(SDL_BUTTON_RIGHT)
//    fun isMiddlePressed() = isButtonPressed(SDL_BUTTON_MIDDLE)
//
//    fun timeSinceLeftPressed() = timeSinceButtonPressed(SDL_BUTTON_LEFT)
//    fun timeSinceRightPressed() = timeSinceButtonPressed(SDL_BUTTON_RIGHT)
//    fun timeSinceMiddlePressed() = timeSinceButtonPressed(SDL_BUTTON_MIDDLE)
}
