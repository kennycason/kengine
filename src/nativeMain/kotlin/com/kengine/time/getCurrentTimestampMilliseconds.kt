package com.kengine.time

import sdl2.SDL_GetTicks

fun getCurrentTimestampMilliseconds(): Long {
    return SDL_GetTicks().toLong()
}