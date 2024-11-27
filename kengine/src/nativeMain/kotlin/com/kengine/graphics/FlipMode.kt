package com.kengine.graphics

import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_FLIP_HORIZONTAL
import sdl2.SDL_FLIP_NONE
import sdl2.SDL_FLIP_VERTICAL

@OptIn(ExperimentalForeignApi::class)
enum class FlipMode(val flag: UInt) {
    NONE(SDL_FLIP_NONE),                                // 00
    HORIZONTAL(SDL_FLIP_HORIZONTAL),                    // 01
    VERTICAL(SDL_FLIP_VERTICAL),                        // 10
    BOTH(SDL_FLIP_HORIZONTAL or SDL_FLIP_VERTICAL) // 11
}