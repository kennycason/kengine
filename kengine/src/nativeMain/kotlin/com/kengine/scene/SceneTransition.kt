package com.kengine.scene

import com.kengine.sdl.getSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl3.image.SDL_FRect
import sdl3.image.SDL_RenderFillRect
import sdl3.image.SDL_SetRenderDrawColor

abstract class SceneTransition(val durationMs: Long) {
    abstract fun render(progress: Double, from: Scene?, to: Scene)
    fun isComplete(progress: Double): Boolean = progress >= 1.0
}

@OptIn(ExperimentalForeignApi::class)
class FadeTransition(durationMs: Long = 300) : SceneTransition(durationMs) {
    override fun render(progress: Double, from: Scene?, to: Scene) {
        val sdl = getSDLContext()
        if (progress < 0.5) {
            from?.draw()
            val alpha = ((progress * 2.0) * 255).toInt().coerceIn(0, 255)
            SDL_SetRenderDrawColor(sdl.renderer, 0u, 0u, 0u, alpha.toUByte())
            memScoped {
                val rect = alloc<SDL_FRect>()
                rect.x = 0f; rect.y = 0f
                rect.w = sdl.screenWidth.toFloat(); rect.h = sdl.screenHeight.toFloat()
                SDL_RenderFillRect(sdl.renderer, rect.ptr)
            }
        } else {
            to.draw()
            val alpha = ((1.0 - (progress - 0.5) * 2.0) * 255).toInt().coerceIn(0, 255)
            SDL_SetRenderDrawColor(sdl.renderer, 0u, 0u, 0u, alpha.toUByte())
            memScoped {
                val rect = alloc<SDL_FRect>()
                rect.x = 0f; rect.y = 0f
                rect.w = sdl.screenWidth.toFloat(); rect.h = sdl.screenHeight.toFloat()
                SDL_RenderFillRect(sdl.renderer, rect.ptr)
            }
        }
    }
}

class CutTransition : SceneTransition(0) {
    override fun render(progress: Double, from: Scene?, to: Scene) {
        to.draw()
    }
}
