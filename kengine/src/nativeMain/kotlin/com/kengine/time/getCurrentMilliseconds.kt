package com.kengine.time

import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_GetTicks

/**
 * While this is safe to use, default to use ClockContext
 *
 * useGameContext {
 *     println("${clock.totalTimeMs} ${clock.deltaTimeMs}")
 * }
 */
@OptIn(ExperimentalForeignApi::class)
fun getCurrentMilliseconds(): Long {
    return SDL_GetTicks().toLong()
}
