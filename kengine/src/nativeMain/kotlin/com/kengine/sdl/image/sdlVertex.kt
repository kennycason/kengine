package com.kengine.sdl.image

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import sdl3.image.SDL_FColor
import sdl3.image.SDL_Vertex

fun sdlVertex(
    x: Float, y: Float,
    r: UInt, g: UInt, b: UInt, a: UInt,
    texX: Float = 0.0f, texY: Float = 0.0f
): SDL_Vertex {
    return sdlVertex(
        x, y,
        sdlColor(r, g, b, a).toFColor(),
        texX, texY
    )
}

fun sdlVertex(
    x: Float, y: Float,
    color: SDL_FColor,
    texX: Float = 0.0f, texY: Float = 0.0f
): SDL_Vertex {
    return memScoped {
        alloc<SDL_Vertex>().apply {
            this.position.x = x
            this.position.y = y
            this.color.r = color.r
            this.color.g = color.g
            this.color.b = color.b
            this.color.a = color.a
            this.tex_coord.x = texX
            this.tex_coord.y = texY
        }
    }
}

fun copySdlVertex(from: SDL_Vertex, to: SDL_Vertex) {
    to.position.x = from.position.x
    to.position.y = from.position.y
    to.color.r = from.color.r
    to.color.g = from.color.g
    to.color.b = from.color.b
    to.color.a = from.color.a
    to.tex_coord.x = from.tex_coord.x
    to.tex_coord.y = from.tex_coord.y
}
