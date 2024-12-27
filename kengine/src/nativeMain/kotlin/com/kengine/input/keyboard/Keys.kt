package com.kengine.input.keyboard

import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDLK_0
import sdl3.SDLK_1
import sdl3.SDLK_2
import sdl3.SDLK_3
import sdl3.SDLK_4
import sdl3.SDLK_5
import sdl3.SDLK_6
import sdl3.SDLK_7
import sdl3.SDLK_8
import sdl3.SDLK_9
import sdl3.SDLK_A
import sdl3.SDLK_B
import sdl3.SDLK_BACKSPACE
import sdl3.SDLK_C
import sdl3.SDLK_D
import sdl3.SDLK_DELETE
import sdl3.SDLK_DOWN
import sdl3.SDLK_E
import sdl3.SDLK_END
import sdl3.SDLK_ESCAPE
import sdl3.SDLK_F
import sdl3.SDLK_F1
import sdl3.SDLK_F10
import sdl3.SDLK_F11
import sdl3.SDLK_F12
import sdl3.SDLK_F2
import sdl3.SDLK_F3
import sdl3.SDLK_F4
import sdl3.SDLK_F5
import sdl3.SDLK_F6
import sdl3.SDLK_F7
import sdl3.SDLK_F8
import sdl3.SDLK_F9
import sdl3.SDLK_G
import sdl3.SDLK_H
import sdl3.SDLK_HOME
import sdl3.SDLK_I
import sdl3.SDLK_INSERT
import sdl3.SDLK_J
import sdl3.SDLK_K
import sdl3.SDLK_L
import sdl3.SDLK_LALT
import sdl3.SDLK_LCTRL
import sdl3.SDLK_LEFT
import sdl3.SDLK_LGUI
import sdl3.SDLK_LSHIFT
import sdl3.SDLK_M
import sdl3.SDLK_N
import sdl3.SDLK_O
import sdl3.SDLK_P
import sdl3.SDLK_PAGEDOWN
import sdl3.SDLK_PAGEUP
import sdl3.SDLK_Q
import sdl3.SDLK_R
import sdl3.SDLK_RALT
import sdl3.SDLK_RCTRL
import sdl3.SDLK_RETURN
import sdl3.SDLK_RGUI
import sdl3.SDLK_RIGHT
import sdl3.SDLK_RSHIFT
import sdl3.SDLK_S
import sdl3.SDLK_SPACE
import sdl3.SDLK_T
import sdl3.SDLK_TAB
import sdl3.SDLK_U
import sdl3.SDLK_UP
import sdl3.SDLK_V
import sdl3.SDLK_W
import sdl3.SDLK_X
import sdl3.SDLK_Y
import sdl3.SDLK_Z

@OptIn(ExperimentalForeignApi::class)
object Keys {
    // alphabet
    const val A: UInt = SDLK_A
    const val B: UInt = SDLK_B
    const val C: UInt = SDLK_C
    const val D: UInt = SDLK_D
    const val E: UInt = SDLK_E
    const val F: UInt = SDLK_F
    const val G: UInt = SDLK_G
    const val H: UInt = SDLK_H
    const val I: UInt = SDLK_I
    const val J: UInt = SDLK_J
    const val K: UInt = SDLK_K
    const val L: UInt = SDLK_L
    const val M: UInt = SDLK_M
    const val N: UInt = SDLK_N
    const val O: UInt = SDLK_O
    const val P: UInt = SDLK_P
    const val Q: UInt = SDLK_Q
    const val R: UInt = SDLK_R
    const val S: UInt = SDLK_S
    const val T: UInt = SDLK_T
    const val U: UInt = SDLK_U
    const val V: UInt = SDLK_V
    const val W: UInt = SDLK_W
    const val X: UInt = SDLK_X
    const val Y: UInt = SDLK_Y
    const val Z: UInt = SDLK_Z

    // numbers
    const val ZERO: UInt = SDLK_0
    const val ONE: UInt = SDLK_1
    const val TWO: UInt = SDLK_2
    const val THREE: UInt = SDLK_3
    const val FOUR: UInt = SDLK_4
    const val FIVE: UInt = SDLK_5
    const val SIX: UInt = SDLK_6
    const val SEVEN: UInt = SDLK_7
    const val EIGHT: UInt = SDLK_8
    const val NINE: UInt = SDLK_9

    // arrow keys
    const val UP: UInt = SDLK_UP
    const val DOWN: UInt = SDLK_DOWN
    const val LEFT: UInt = SDLK_LEFT
    const val RIGHT: UInt = SDLK_RIGHT

    // modifier keys
    const val LSHIFT: UInt = SDLK_LSHIFT
    const val RSHIFT: UInt = SDLK_RSHIFT
    const val LCTRL: UInt = SDLK_LCTRL
    const val RCTRL: UInt = SDLK_RCTRL
    const val LALT: UInt = SDLK_LALT
    const val RALT: UInt = SDLK_RALT
    const val LGUI: UInt = SDLK_LGUI
    const val RGUI: UInt = SDLK_RGUI

    // other important keys
    const val SPACE: UInt = SDLK_SPACE
    const val RETURN: UInt = SDLK_RETURN
    const val ESCAPE: UInt = SDLK_ESCAPE
    const val TAB: UInt = SDLK_TAB
    const val BACKSPACE: UInt = SDLK_BACKSPACE
    const val INSERT: UInt = SDLK_INSERT
    const val DELETE: UInt = SDLK_DELETE
    const val HOME: UInt = SDLK_HOME
    const val END: UInt = SDLK_END
    const val PAGEUP: UInt = SDLK_PAGEUP
    const val PAGEDOWN: UInt = SDLK_PAGEDOWN

    // function keys
    const val F1: UInt = SDLK_F1
    const val F2: UInt = SDLK_F2
    const val F3: UInt = SDLK_F3
    const val F4: UInt = SDLK_F4
    const val F5: UInt = SDLK_F5
    const val F6: UInt = SDLK_F6
    const val F7: UInt = SDLK_F7
    const val F8: UInt = SDLK_F8
    const val F9: UInt = SDLK_F9
    const val F10: UInt = SDLK_F10
    const val F11: UInt = SDLK_F11
    const val F12: UInt = SDLK_F12
}
