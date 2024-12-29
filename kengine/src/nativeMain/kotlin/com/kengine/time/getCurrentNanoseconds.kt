package com.kengine.time

import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_GetPerformanceCounter
import sdl3.SDL_GetPerformanceFrequency

@OptIn(ExperimentalForeignApi::class)
fun getCurrentNanoseconds(): Long {
    val counter = SDL_GetPerformanceCounter()
    val frequency = SDL_GetPerformanceFrequency()
    return ((counter * 1_000_000_000u) / frequency).toLong()
}
