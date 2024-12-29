package com.kengine.graphics

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretCPointer
import sdl3.image.SDL_Texture
import kotlin.native.internal.NativePtr.Companion.NULL

@OptIn(ExperimentalForeignApi::class)
data class Texture(
    val texture: CPointer<SDL_Texture> = interpretCPointer(NULL)!!,
    val width: Int,
    val height: Int,
    val format: UInt,
    val access: Int
)
