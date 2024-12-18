package com.kengine.graphics

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.image.SDL_Texture

@OptIn(ExperimentalForeignApi::class)
data class Texture(
    val texture: CValuesRef<SDL_Texture>,
    val width: Int,
    val height: Int,
    val format: UInt,
    val access: Int
)
