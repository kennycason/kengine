package com.kengine.sdl.image

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import sdl3.image.SDL_Color
import sdl3.image.SDL_FColor

fun sdlFColor(r: UInt, g: UInt, b: UInt, a: UInt): SDL_FColor =
    sdlFColor(
        r.toFloat() / 255f,
        g.toFloat() / 255f,
        b.toFloat() / 255f,
        a.toFloat() / 255f
    )

fun sdlFColor(r: Float, g: Float, b: Float, a: Float): SDL_FColor = memScoped {
    alloc<SDL_FColor>().apply {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }
}

// Extension: Convert SDL_Color -> SDL_FColor
fun SDL_Color.toFColor(): SDL_FColor =
    sdlFColor(r.toFloat() / 255f, g.toFloat() / 255f, b.toFloat() / 255f, a.toFloat() / 255f)
