package  com.kengine.sdl.image

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import sdl3.image.SDL_Color

fun sdlColor(r: UInt, g: UInt, b: UInt, a: UInt): SDL_Color {
    return sdlColor(r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte())
}

fun sdlColor(r: UByte, g: UByte, b: UByte, a: UByte): SDL_Color {
    return memScoped {
        alloc<SDL_Color>().apply {
            this.r = r
            this.g = g
            this.b = b
            this.a = a
        }
    }
}
