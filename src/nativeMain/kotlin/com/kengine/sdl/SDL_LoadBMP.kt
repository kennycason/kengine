package com.kengine.sdl

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import sdl2.SDL_LoadBMP_RW
import sdl2.SDL_RWFromFile
import sdl2.SDL_Surface

// TODO where did this wrapper go? I recall this being in SDL before...
internal fun SDL_LoadBMP(filePath: String): CPointer<SDL_Surface>? {
    memScoped {
        val file = SDL_RWFromFile(filePath, "rb") ?: return null
        val surface = SDL_LoadBMP_RW(file, 1) // 1 means close the RW file after reading
        return surface
    }
}