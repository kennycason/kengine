package com.kengine.time

import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_GetTicks

@OptIn(ExperimentalForeignApi::class)
fun getCurrentTimestampMilliseconds(): Long {
    return SDL_GetTicks().toLong()
}