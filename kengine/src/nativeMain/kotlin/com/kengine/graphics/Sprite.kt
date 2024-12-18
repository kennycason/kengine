package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.IntRect
import com.kengine.math.Vec2
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.convert
//import kotlinx.cinterop.memScoped
//import kotlinx.cinterop.toKString
//import sdl3.SDL_GetError
//import sdl3.SDL_GetNumberProperty
//import sdl3.image.IMG_Load
//import sdl3.image.SDL_CreateTexture
//import sdl3.image.SDL_CreateTextureFromSurface
//import sdl3.image.SDL_DestroySurface
//import sdl3.image.SDL_DestroyTexture
//import sdl3.image.SDL_GetTextureProperties
//import sdl3.image.SDL_RenderTexture
//import sdl3.image.SDL_SetRenderTarget
//import sdl3.image.SDL_Surface
//import sdl3.image.SDL_Texture
//import sdl3.image.SDL_TextureAccess

@OptIn(ExperimentalForeignApi::class)
class Sprite private constructor(
    val texture: Texture,
    val clip: IntRect? = null,
    val scale: Vec2 = Vec2(1.0, 1.0),
) : Logging {
    val width: Int = clip?.w ?: texture.width
    val height: Int = clip?.h ?: texture.height

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) = draw(p.x, p.y, flip)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE, angle: Double = 0.0) {
        useSDLContext {
//            memScoped {
//                val clipRect = if (clip == null) null
//                else alloc<SDL_Rect>().apply {
//                    this.x = clip.x
//                    this.y = clip.y
//                    this.w = clip.w
//                    this.h = clip.h
//                }
//
//                val destRect = alloc<SDL_Rect>().apply {
//                    this.x = (x * scale.x).toInt()
//                    this.y = (y * scale.y).toInt()
//                    this.w = ((clipRect?.w ?: texture.width) * scale.x).toInt()
//                    this.h = ((clipRect?.h ?: texture.height) * scale.y).toInt()
//                }
//
//                if (angle == 0.0 && flip == FlipMode.NONE) {
//                    SDL_RenderCopy(renderer, texture.texture, clipRect?.ptr, destRect.ptr)
//                } else {
//                    val center = alloc<SDL_Point>().apply {
//                        this.x = destRect.w / 2
//                        this.y = destRect.h / 2
//                    }
//                    val result = SDL_RenderCopyEx(
//                        renderer = renderer,
//                        texture = texture.texture,
//                        srcrect = clipRect?.ptr,
//                        dstrect = destRect.ptr,
//                        angle = angle,
//                        center = center.ptr,
//                        flip = flip.flag
//                    )
//                    if (result != 0) {
//                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
//                    }
//                    return
//                }
//            }
        }
    }

    fun cleanup() {
         // TextureManager handles texture cleanup
    }

    companion object {
        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getTextureContext().getTexture(filePath)
            return Sprite(texture, clip)
        }

        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }
    }

}
