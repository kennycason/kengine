package com.kengine.graphics

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.image.SDL_Texture

@OptIn(ExperimentalForeignApi::class)
data class Texture(
    val texture: CPointer<SDL_Texture>,
    val width: Int,
    val height: Int,
    val format: UInt,
    val access: Int
)
