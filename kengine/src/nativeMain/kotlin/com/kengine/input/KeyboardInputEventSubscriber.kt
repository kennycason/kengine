package com.kengine.input

import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLContext
import com.kengine.time.getCurrentTimestampMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Event
import sdl2.SDL_KEYDOWN
import sdl2.SDL_KEYUP

@OptIn(ExperimentalForeignApi::class)
class KeyboardInputEventSubscriber {
    private val keyStates = mutableMapOf<Int, KeyState>()

    data class KeyState(
        var isPressed: Boolean = false,
        var lastPressed: Long = 0L
    )

    // must be called
    fun init() {
        useSDLContext {
            sdlEvents.subscribe(SDLEventContext.EventType.KEYBOARD, ::handleKeyboardEvent)
        }
    }

    fun handleKeyboardEvent(event: SDL_Event) {
        when (event.type) {
            SDL_KEYDOWN -> {
                val key = event.key.keysym.sym
                if (!keyStates.containsKey(key)) {
                    keyStates[key] = KeyState()
                }
                keyStates[key]?.apply {
                    isPressed = true
                    lastPressed = getCurrentTimestampMilliseconds()
                }
            }

            SDL_KEYUP -> {
                val key = event.key.keysym.sym
                keyStates[key]?.isPressed = false
            }
        }
    }

    /**
     * check if a specific key is currently pressed
     */
    fun isPressed(keyCode: UInt): Boolean {
        return keyStates[keyCode.toInt()]?.isPressed ?: false
    }

    /**
     * check how much time has passed since a key was last pressed
     * TODO remove
     */
    fun timeSincePressed(keyCode: UInt): Long {
        val currentTime = getCurrentTimestampMilliseconds()
        return keyStates[keyCode.toInt()]?.let {
            if (it.isPressed) 0L else currentTime - it.lastPressed
        } ?: Long.MAX_VALUE
    }

    // convenience helpers TODO determine if this is overkill :D
    // alphabet
    fun isAPressed() = isPressed(Keys.A)
    fun timeSinceAPressed() = timeSincePressed(Keys.A)
    fun isBPressed() = isPressed(Keys.B)
    fun timeSinceBPressed() = timeSincePressed(Keys.B)
    fun isCPressed() = isPressed(Keys.C)
    fun timeSinceCPressed() = timeSincePressed(Keys.C)
    fun isDPressed() = isPressed(Keys.D)
    fun timeSinceDPressed() = timeSincePressed(Keys.D)
    fun isEPressed() = isPressed(Keys.E)
    fun timeSinceEPressed() = timeSincePressed(Keys.E)
    fun isFPressed() = isPressed(Keys.F)
    fun timeSinceFPressed() = timeSincePressed(Keys.F)
    fun isGPressed() = isPressed(Keys.G)
    fun timeSinceGPressed() = timeSincePressed(Keys.G)
    fun isHPressed() = isPressed(Keys.H)
    fun timeSinceHPressed() = timeSincePressed(Keys.H)
    fun isIPressed() = isPressed(Keys.I)
    fun timeSinceIPressed() = timeSincePressed(Keys.I)
    fun isJPressed() = isPressed(Keys.J)
    fun timeSinceJPressed() = timeSincePressed(Keys.J)
    fun isKPressed() = isPressed(Keys.K)
    fun timeSinceKPressed() = timeSincePressed(Keys.K)
    fun isLPressed() = isPressed(Keys.L)
    fun timeSinceLPressed() = timeSincePressed(Keys.L)
    fun isMPressed() = isPressed(Keys.M)
    fun timeSinceMPressed() = timeSincePressed(Keys.M)
    fun isNPressed() = isPressed(Keys.N)
    fun timeSinceNPressed() = timeSincePressed(Keys.N)
    fun isOPressed() = isPressed(Keys.O)
    fun timeSinceOPressed() = timeSincePressed(Keys.O)
    fun isPPressed() = isPressed(Keys.P)
    fun timeSincePPressed() = timeSincePressed(Keys.P)
    fun isQPressed() = isPressed(Keys.Q)
    fun timeSinceQPressed() = timeSincePressed(Keys.Q)
    fun isRPressed() = isPressed(Keys.R)
    fun timeSinceRPressed() = timeSincePressed(Keys.R)
    fun isSPressed() = isPressed(Keys.S)
    fun timeSinceSPressed() = timeSincePressed(Keys.S)
    fun isTPressed() = isPressed(Keys.T)
    fun timeSinceTPressed() = timeSincePressed(Keys.T)
    fun isUPressed() = isPressed(Keys.U)
    fun timeSinceUPressed() = timeSincePressed(Keys.U)
    fun isVPressed() = isPressed(Keys.V)
    fun timeSinceVPressed() = timeSincePressed(Keys.V)
    fun isWPressed() = isPressed(Keys.W)
    fun timeSinceWPressed() = timeSincePressed(Keys.W)
    fun isXPressed() = isPressed(Keys.X)
    fun timeSinceXPressed() = timeSincePressed(Keys.X)
    fun isYPressed() = isPressed(Keys.Y)
    fun timeSinceYPressed() = timeSincePressed(Keys.Y)
    fun isZPressed() = isPressed(Keys.Z)
    fun timeSinceZPressed() = timeSincePressed(Keys.Z)

    // numbers
    fun isZeroPressed() = isPressed(Keys.ZERO)
    fun timeSinceZeroPressed() = timeSincePressed(Keys.ZERO)
    fun isOnePressed() = isPressed(Keys.ONE)
    fun timeSinceOnePressed() = timeSincePressed(Keys.ONE)
    fun isTwoPressed() = isPressed(Keys.TWO)
    fun timeSinceTwoPressed() = timeSincePressed(Keys.TWO)
    fun isThreePressed() = isPressed(Keys.THREE)
    fun timeSinceThreePressed() = timeSincePressed(Keys.THREE)
    fun isFourPressed() = isPressed(Keys.FOUR)
    fun timeSinceFourPressed() = timeSincePressed(Keys.FOUR)

    fun isFivePressed() = isPressed(Keys.FIVE)
    fun timeSinceFivePressed() = timeSincePressed(Keys.FIVE)
    fun isSixPressed() = isPressed(Keys.SIX)
    fun timeSinceSixPressed() = timeSincePressed(Keys.SIX)
    fun isSevenPressed() = isPressed(Keys.SEVEN)
    fun timeSinceSevenPressed() = timeSincePressed(Keys.SEVEN)
    fun isEightPressed() = isPressed(Keys.EIGHT)
    fun timeSinceEightPressed() = timeSincePressed(Keys.EIGHT)
    fun isNinePressed() = isPressed(Keys.NINE)
    fun timeSinceNinePressed() = timeSincePressed(Keys.NINE)

    // arrow keys
    fun isUpPressed() = isPressed(Keys.UP)
    fun timeSinceUpPressed() = timeSincePressed(Keys.UP)
    fun isDownPressed() = isPressed(Keys.DOWN)
    fun timeSinceDownPressed() = timeSincePressed(Keys.DOWN)
    fun isLeftPressed() = isPressed(Keys.LEFT)
    fun timeSinceLeftPressed() = timeSincePressed(Keys.LEFT)
    fun isRightPressed() = isPressed(Keys.RIGHT)
    fun timeSinceRightPressed() = timeSincePressed(Keys.RIGHT)

    // modifier keys
    fun isLShiftPressed() = isPressed(Keys.LSHIFT)
    fun timeSinceLShiftPressed() = timeSincePressed(Keys.LSHIFT)
    fun isRShiftPressed() = isPressed(Keys.RSHIFT)
    fun timeSinceRShiftPressed() = timeSincePressed(Keys.RSHIFT)
    fun isLCtrlPressed() = isPressed(Keys.LCTRL)
    fun timeSinceLCtrlPressed() = timeSincePressed(Keys.LCTRL)
    fun isRCtrlPressed() = isPressed(Keys.RCTRL)
    fun timeSinceRCtrlPressed() = timeSincePressed(Keys.RCTRL)
    fun isLAltPressed() = isPressed(Keys.LALT)
    fun timeSinceLAltPressed() = timeSincePressed(Keys.LALT)
    fun isRAltPressed() = isPressed(Keys.RALT)
    fun timeSinceRAltPressed() = timeSincePressed(Keys.RALT)
    fun isLGUIPressed() = isPressed(Keys.LGUI)
    fun timeSinceLGUIPressed() = timeSincePressed(Keys.LGUI)
    fun isRGUIPressed() = isPressed(Keys.RGUI)
    fun timeSinceRGUIPressed() = timeSincePressed(Keys.RGUI)

    // other important keys
    fun isSpacePressed() = isPressed(Keys.SPACE)
    fun timeSinceSpacePressed() = timeSincePressed(Keys.SPACE)
    fun isReturnPressed() = isPressed(Keys.RETURN)
    fun timeSinceReturnPressed() = timeSincePressed(Keys.RETURN)
    fun isEscapePressed() = isPressed(Keys.ESCAPE)
    fun timeSinceEscapePressed() = timeSincePressed(Keys.ESCAPE)
    fun isTabPressed() = isPressed(Keys.TAB)
    fun timeSinceTabPressed() = timeSincePressed(Keys.TAB)
    fun isBackspacePressed() = isPressed(Keys.BACKSPACE)
    fun timeSinceBackspacePressed() = timeSincePressed(Keys.BACKSPACE)
    fun isInsertPressed() = isPressed(Keys.INSERT)
    fun timeSinceInsertPressed() = timeSincePressed(Keys.INSERT)
    fun isDeletePressed() = isPressed(Keys.DELETE)
    fun timeSinceDeletePressed() = timeSincePressed(Keys.DELETE)
    fun isHomePressed() = isPressed(Keys.HOME)
    fun timeSinceHomePressed() = timeSincePressed(Keys.HOME)
    fun isEndPressed() = isPressed(Keys.END)
    fun timeSinceEndPressed() = timeSincePressed(Keys.END)
    fun isPageUpPressed() = isPressed(Keys.PAGEUP)
    fun timeSincePageUpPressed() = timeSincePressed(Keys.PAGEUP)
    fun isPageDownPressed() = isPressed(Keys.PAGEDOWN)
    fun timeSincePageDownPressed() = timeSincePressed(Keys.PAGEDOWN)

    // function keys
    fun isF1Pressed() = isPressed(Keys.F1)
    fun timeSinceF1Pressed() = timeSincePressed(Keys.F1)
    fun isF2Pressed() = isPressed(Keys.F2)
    fun timeSinceF2Pressed() = timeSincePressed(Keys.F2)
    fun isF3Pressed() = isPressed(Keys.F3)
    fun timeSinceF3Pressed() = timeSincePressed(Keys.F3)
    fun isF4Pressed() = isPressed(Keys.F4)
    fun timeSinceF4Pressed() = timeSincePressed(Keys.F4)
    fun isF5Pressed() = isPressed(Keys.F5)
    fun timeSinceF5Pressed() = timeSincePressed(Keys.F5)
    fun isF6Pressed() = isPressed(Keys.F6)
    fun timeSinceF6Pressed() = timeSincePressed(Keys.F6)
    fun isF7Pressed() = isPressed(Keys.F7)
    fun timeSinceF7Pressed() = timeSincePressed(Keys.F7)
    fun isF8Pressed() = isPressed(Keys.F8)
    fun timeSinceF8Pressed() = timeSincePressed(Keys.F8)
    fun isF9Pressed() = isPressed(Keys.F9)
    fun timeSinceF9Pressed() = timeSincePressed(Keys.F9)
    fun isF10Pressed() = isPressed(Keys.F10)
    fun timeSinceF10Pressed() = timeSincePressed(Keys.F10)
    fun isF11Pressed() = isPressed(Keys.F11)
    fun timeSinceF11Pressed() = timeSincePressed(Keys.F11)
    fun isF12Pressed() = isPressed(Keys.F12)
    fun timeSinceF12Pressed() = timeSincePressed(Keys.F12)
}