package com.kengine.graphics

//import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
data class Texture(
//    val texture: CValuesRef<cnames.structs.SDL_Texture>,
    val width: Int,
    val height: Int,
    val format: UInt,
    val access: Int
)
