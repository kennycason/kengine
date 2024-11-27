package com.kengine.input

import com.kengine.context.useContext
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
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

    init {
        useContext(SDLContext.get()) {
            events.subscribe(SDLEventContext.EventType.KEYBOARD, ::handleKeyboardEvent)
        }
    }

    private fun handleKeyboardEvent(event: SDL_Event) {
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
    fun isKeyPressed(keyCode: UInt): Boolean {
        return keyStates[keyCode.toInt()]?.isPressed ?: false
    }

    /**
     * check how much time has passed since a key was last pressed
     */
    fun timeSinceKeyPressed(keyCode: UInt): Long {
        val currentTime = getCurrentTimestampMilliseconds()
        return keyStates[keyCode.toInt()]?.let {
            if (it.isPressed) 0L else currentTime - it.lastPressed
        } ?: Long.MAX_VALUE
    }

    // convenience helpers TODO determine if this is overkill :D
    // alphabet
    fun isAPressed() = isKeyPressed(Keys.A)
    fun timeSinceAPressed() = timeSinceKeyPressed(Keys.A)
    fun isBPressed() = isKeyPressed(Keys.B)
    fun timeSinceBPressed() = timeSinceKeyPressed(Keys.B)
    fun isCPressed() = isKeyPressed(Keys.C)
    fun timeSinceCPressed() = timeSinceKeyPressed(Keys.C)
    fun isDPressed() = isKeyPressed(Keys.D)
    fun timeSinceDPressed() = timeSinceKeyPressed(Keys.D)
    fun isEPressed() = isKeyPressed(Keys.E)
    fun timeSinceEPressed() = timeSinceKeyPressed(Keys.E)
    fun isFPressed() = isKeyPressed(Keys.F)
    fun timeSinceFPressed() = timeSinceKeyPressed(Keys.F)
    fun isGPressed() = isKeyPressed(Keys.G)
    fun timeSinceGPressed() = timeSinceKeyPressed(Keys.G)
    fun isHPressed() = isKeyPressed(Keys.H)
    fun timeSinceHPressed() = timeSinceKeyPressed(Keys.H)
    fun isIPressed() = isKeyPressed(Keys.I)
    fun timeSinceIPressed() = timeSinceKeyPressed(Keys.I)
    fun isJPressed() = isKeyPressed(Keys.J)
    fun timeSinceJPressed() = timeSinceKeyPressed(Keys.J)
    fun isKPressed() = isKeyPressed(Keys.K)
    fun timeSinceKPressed() = timeSinceKeyPressed(Keys.K)
    fun isLPressed() = isKeyPressed(Keys.L)
    fun timeSinceLPressed() = timeSinceKeyPressed(Keys.L)
    fun isMPressed() = isKeyPressed(Keys.M)
    fun timeSinceMPressed() = timeSinceKeyPressed(Keys.M)
    fun isNPressed() = isKeyPressed(Keys.N)
    fun timeSinceNPressed() = timeSinceKeyPressed(Keys.N)
    fun isOPressed() = isKeyPressed(Keys.O)
    fun timeSinceOPressed() = timeSinceKeyPressed(Keys.O)
    fun isPPressed() = isKeyPressed(Keys.P)
    fun timeSincePPressed() = timeSinceKeyPressed(Keys.P)
    fun isQPressed() = isKeyPressed(Keys.Q)
    fun timeSinceQPressed() = timeSinceKeyPressed(Keys.Q)
    fun isRPressed() = isKeyPressed(Keys.R)
    fun timeSinceRPressed() = timeSinceKeyPressed(Keys.R)
    fun isSPressed() = isKeyPressed(Keys.S)
    fun timeSinceSPressed() = timeSinceKeyPressed(Keys.S)
    fun isTPressed() = isKeyPressed(Keys.T)
    fun timeSinceTPressed() = timeSinceKeyPressed(Keys.T)
    fun isUPressed() = isKeyPressed(Keys.U)
    fun timeSinceUPressed() = timeSinceKeyPressed(Keys.U)
    fun isVPressed() = isKeyPressed(Keys.V)
    fun timeSinceVPressed() = timeSinceKeyPressed(Keys.V)
    fun isWPressed() = isKeyPressed(Keys.W)
    fun timeSinceWPressed() = timeSinceKeyPressed(Keys.W)
    fun isXPressed() = isKeyPressed(Keys.X)
    fun timeSinceXPressed() = timeSinceKeyPressed(Keys.X)
    fun isYPressed() = isKeyPressed(Keys.Y)
    fun timeSinceYPressed() = timeSinceKeyPressed(Keys.Y)
    fun isZPressed() = isKeyPressed(Keys.Z)
    fun timeSinceZPressed() = timeSinceKeyPressed(Keys.Z)

    // numbers
    fun isZeroPressed() = isKeyPressed(Keys.ZERO)
    fun timeSinceZeroPressed() = timeSinceKeyPressed(Keys.ZERO)
    fun isOnePressed() = isKeyPressed(Keys.ONE)
    fun timeSinceOnePressed() = timeSinceKeyPressed(Keys.ONE)
    fun isTwoPressed() = isKeyPressed(Keys.TWO)
    fun timeSinceTwoPressed() = timeSinceKeyPressed(Keys.TWO)
    fun isThreePressed() = isKeyPressed(Keys.THREE)
    fun timeSinceThreePressed() = timeSinceKeyPressed(Keys.THREE)
    fun isFourPressed() = isKeyPressed(Keys.FOUR)
    fun timeSinceFourPressed() = timeSinceKeyPressed(Keys.FOUR)

    fun isFivePressed() = isKeyPressed(Keys.FIVE)
    fun timeSinceFivePressed() = timeSinceKeyPressed(Keys.FIVE)
    fun isSixPressed() = isKeyPressed(Keys.SIX)
    fun timeSinceSixPressed() = timeSinceKeyPressed(Keys.SIX)
    fun isSevenPressed() = isKeyPressed(Keys.SEVEN)
    fun timeSinceSevenPressed() = timeSinceKeyPressed(Keys.SEVEN)
    fun isEightPressed() = isKeyPressed(Keys.EIGHT)
    fun timeSinceEightPressed() = timeSinceKeyPressed(Keys.EIGHT)
    fun isNinePressed() = isKeyPressed(Keys.NINE)
    fun timeSinceNinePressed() = timeSinceKeyPressed(Keys.NINE)

    // arrow keys
    fun isUpPressed() = isKeyPressed(Keys.UP)
    fun timeSinceUpPressed() = timeSinceKeyPressed(Keys.UP)
    fun isDownPressed() = isKeyPressed(Keys.DOWN)
    fun timeSinceDownPressed() = timeSinceKeyPressed(Keys.DOWN)
    fun isLeftPressed() = isKeyPressed(Keys.LEFT)
    fun timeSinceLeftPressed() = timeSinceKeyPressed(Keys.LEFT)
    fun isRightPressed() = isKeyPressed(Keys.RIGHT)
    fun timeSinceRightPressed() = timeSinceKeyPressed(Keys.RIGHT)

    // modifier keys
    fun isLShiftPressed() = isKeyPressed(Keys.LSHIFT)
    fun timeSinceLShiftPressed() = timeSinceKeyPressed(Keys.LSHIFT)
    fun isRShiftPressed() = isKeyPressed(Keys.RSHIFT)
    fun timeSinceRShiftPressed() = timeSinceKeyPressed(Keys.RSHIFT)
    fun isLCtrlPressed() = isKeyPressed(Keys.LCTRL)
    fun timeSinceLCtrlPressed() = timeSinceKeyPressed(Keys.LCTRL)
    fun isRCtrlPressed() = isKeyPressed(Keys.RCTRL)
    fun timeSinceRCtrlPressed() = timeSinceKeyPressed(Keys.RCTRL)
    fun isLAltPressed() = isKeyPressed(Keys.LALT)
    fun timeSinceLAltPressed() = timeSinceKeyPressed(Keys.LALT)
    fun isRAltPressed() = isKeyPressed(Keys.RALT)
    fun timeSinceRAltPressed() = timeSinceKeyPressed(Keys.RALT)
    fun isLGUIPressed() = isKeyPressed(Keys.LGUI)
    fun timeSinceLGUIPressed() = timeSinceKeyPressed(Keys.LGUI)
    fun isRGUIPressed() = isKeyPressed(Keys.RGUI)
    fun timeSinceRGUIPressed() = timeSinceKeyPressed(Keys.RGUI)

    // other important keys
    fun isSpacePressed() = isKeyPressed(Keys.SPACE)
    fun timeSinceSpacePressed() = timeSinceKeyPressed(Keys.SPACE)
    fun isReturnPressed() = isKeyPressed(Keys.RETURN)
    fun timeSinceReturnPressed() = timeSinceKeyPressed(Keys.RETURN)
    fun isEscapePressed() = isKeyPressed(Keys.ESCAPE)
    fun timeSinceEscapePressed() = timeSinceKeyPressed(Keys.ESCAPE)
    fun isTabPressed() = isKeyPressed(Keys.TAB)
    fun timeSinceTabPressed() = timeSinceKeyPressed(Keys.TAB)
    fun isBackspacePressed() = isKeyPressed(Keys.BACKSPACE)
    fun timeSinceBackspacePressed() = timeSinceKeyPressed(Keys.BACKSPACE)
    fun isInsertPressed() = isKeyPressed(Keys.INSERT)
    fun timeSinceInsertPressed() = timeSinceKeyPressed(Keys.INSERT)
    fun isDeletePressed() = isKeyPressed(Keys.DELETE)
    fun timeSinceDeletePressed() = timeSinceKeyPressed(Keys.DELETE)
    fun isHomePressed() = isKeyPressed(Keys.HOME)
    fun timeSinceHomePressed() = timeSinceKeyPressed(Keys.HOME)
    fun isEndPressed() = isKeyPressed(Keys.END)
    fun timeSinceEndPressed() = timeSinceKeyPressed(Keys.END)
    fun isPageUpPressed() = isKeyPressed(Keys.PAGEUP)
    fun timeSincePageUpPressed() = timeSinceKeyPressed(Keys.PAGEUP)
    fun isPageDownPressed() = isKeyPressed(Keys.PAGEDOWN)
    fun timeSincePageDownPressed() = timeSinceKeyPressed(Keys.PAGEDOWN)

    // function keys
    fun isF1Pressed() = isKeyPressed(Keys.F1)
    fun timeSinceF1Pressed() = timeSinceKeyPressed(Keys.F1)
    fun isF2Pressed() = isKeyPressed(Keys.F2)
    fun timeSinceF2Pressed() = timeSinceKeyPressed(Keys.F2)
    fun isF3Pressed() = isKeyPressed(Keys.F3)
    fun timeSinceF3Pressed() = timeSinceKeyPressed(Keys.F3)
    fun isF4Pressed() = isKeyPressed(Keys.F4)
    fun timeSinceF4Pressed() = timeSinceKeyPressed(Keys.F4)
    fun isF5Pressed() = isKeyPressed(Keys.F5)
    fun timeSinceF5Pressed() = timeSinceKeyPressed(Keys.F5)
    fun isF6Pressed() = isKeyPressed(Keys.F6)
    fun timeSinceF6Pressed() = timeSinceKeyPressed(Keys.F6)
    fun isF7Pressed() = isKeyPressed(Keys.F7)
    fun timeSinceF7Pressed() = timeSinceKeyPressed(Keys.F7)
    fun isF8Pressed() = isKeyPressed(Keys.F8)
    fun timeSinceF8Pressed() = timeSinceKeyPressed(Keys.F8)
    fun isF9Pressed() = isKeyPressed(Keys.F9)
    fun timeSinceF9Pressed() = timeSinceKeyPressed(Keys.F9)
    fun isF10Pressed() = isKeyPressed(Keys.F10)
    fun timeSinceF10Pressed() = timeSinceKeyPressed(Keys.F10)
    fun isF11Pressed() = isKeyPressed(Keys.F11)
    fun timeSinceF11Pressed() = timeSinceKeyPressed(Keys.F11)
    fun isF12Pressed() = isKeyPressed(Keys.F12)
    fun timeSinceF12Pressed() = timeSinceKeyPressed(Keys.F12)
}