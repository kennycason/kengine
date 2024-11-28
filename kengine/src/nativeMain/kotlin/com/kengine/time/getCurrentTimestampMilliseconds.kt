package com.kengine.time

import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_GetTicks

/**
 * While this is safe to use, default to use ClockContext
 *
 * useContext<GameContext> {
 *     println("${clock.totalTimeMs} ${clock.deltaTimeMs}")
 * }
 */
@OptIn(ExperimentalForeignApi::class)
fun getCurrentTimestampMilliseconds(): Long {
    return SDL_GetTicks().toLong()
}