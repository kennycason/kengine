package com.kengine.input

import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDLK_0
import sdl2.SDLK_1
import sdl2.SDLK_2
import sdl2.SDLK_3
import sdl2.SDLK_4
import sdl2.SDLK_5
import sdl2.SDLK_6
import sdl2.SDLK_7
import sdl2.SDLK_8
import sdl2.SDLK_9
import sdl2.SDLK_BACKSPACE
import sdl2.SDLK_DELETE
import sdl2.SDLK_DOWN
import sdl2.SDLK_END
import sdl2.SDLK_ESCAPE
import sdl2.SDLK_F1
import sdl2.SDLK_F10
import sdl2.SDLK_F11
import sdl2.SDLK_F12
import sdl2.SDLK_F2
import sdl2.SDLK_F3
import sdl2.SDLK_F4
import sdl2.SDLK_F5
import sdl2.SDLK_F6
import sdl2.SDLK_F7
import sdl2.SDLK_F8
import sdl2.SDLK_F9
import sdl2.SDLK_HOME
import sdl2.SDLK_INSERT
import sdl2.SDLK_LALT
import sdl2.SDLK_LCTRL
import sdl2.SDLK_LEFT
import sdl2.SDLK_LGUI
import sdl2.SDLK_LSHIFT
import sdl2.SDLK_PAGEDOWN
import sdl2.SDLK_PAGEUP
import sdl2.SDLK_RALT
import sdl2.SDLK_RCTRL
import sdl2.SDLK_RETURN
import sdl2.SDLK_RGUI
import sdl2.SDLK_RIGHT
import sdl2.SDLK_RSHIFT
import sdl2.SDLK_SPACE
import sdl2.SDLK_TAB
import sdl2.SDLK_UP
import sdl2.SDLK_a
import sdl2.SDLK_b
import sdl2.SDLK_c
import sdl2.SDLK_d
import sdl2.SDLK_e
import sdl2.SDLK_f
import sdl2.SDLK_g
import sdl2.SDLK_h
import sdl2.SDLK_i
import sdl2.SDLK_j
import sdl2.SDLK_k
import sdl2.SDLK_l
import sdl2.SDLK_m
import sdl2.SDLK_n
import sdl2.SDLK_o
import sdl2.SDLK_p
import sdl2.SDLK_q
import sdl2.SDLK_r
import sdl2.SDLK_s
import sdl2.SDLK_t
import sdl2.SDLK_u
import sdl2.SDLK_v
import sdl2.SDLK_w
import sdl2.SDLK_x
import sdl2.SDLK_y
import sdl2.SDLK_z

@OptIn(ExperimentalForeignApi::class)
object Keys {
    // alphabet
    const val A: UInt = SDLK_a
    const val B: UInt = SDLK_b
    const val C: UInt = SDLK_c
    const val D: UInt = SDLK_d
    const val E: UInt = SDLK_e
    const val F: UInt = SDLK_f
    const val G: UInt = SDLK_g
    const val H: UInt = SDLK_h
    const val I: UInt = SDLK_i
    const val J: UInt = SDLK_j
    const val K: UInt = SDLK_k
    const val L: UInt = SDLK_l
    const val M: UInt = SDLK_m
    const val N: UInt = SDLK_n
    const val O: UInt = SDLK_o
    const val P: UInt = SDLK_p
    const val Q: UInt = SDLK_q
    const val R: UInt = SDLK_r
    const val S: UInt = SDLK_s
    const val T: UInt = SDLK_t
    const val U: UInt = SDLK_u
    const val V: UInt = SDLK_v
    const val W: UInt = SDLK_w
    const val X: UInt = SDLK_x
    const val Y: UInt = SDLK_y
    const val Z: UInt = SDLK_z

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