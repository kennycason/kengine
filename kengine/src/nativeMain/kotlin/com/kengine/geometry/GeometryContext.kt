package com.kengine.geometry

import com.kengine.context.Context
import com.kengine.context.useContext
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import sdl2.SDL_GetError
import sdl2.SDL_Rect
import sdl2.SDL_RenderDrawLine
import sdl2.SDL_RenderDrawPoint
import sdl2.SDL_RenderDrawRect
import sdl2.SDL_RenderFillRect
import sdl2.SDL_SetRenderDrawColor
import sdl2gfx.circleRGBA
import sdl2gfx.filledCircleRGBA

/**
 *
 * TODO better handle Int to Short cast
 */
@OptIn(ExperimentalForeignApi::class)
class GeometryContext private constructor() : Context() {

    fun drawCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            val result = circleRGBA(renderer, centerX.toShort(), centerY.toShort(), radius.toShort(), r, g, b, a)
            if (result != 0) {
                Logger.error("Error drawing circle: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun fillCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            val result = filledCircleRGBA(renderer, centerX.toShort(), centerY.toShort(), radius.toShort(), r, g, b, a)
            if (result != 0) {
                Logger.error("Error filling circle: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawSquare(
        x: Int, y: Int,
        size: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        drawRectangle(x, y, size, size, r, g, b, a)
    }

    fun drawRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            memScoped {
                val rect = alloc<SDL_Rect>().apply {
                    this.x = x
                    this.y = y
                    this.w = width
                    this.h = height
                }
                val result = SDL_RenderDrawRect(renderer, rect.ptr)
                if (result != 0) {
                    Logger.error("Error drawing rectangle: ${SDL_GetError()?.toKString()}")
                }
            }
        }
    }

    fun fillSquare(
        x: Int, y: Int,
        size: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        fillRectangle(x, y, size, size, r, g, b, a)
    }

    fun fillRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            memScoped {
                val rect = alloc<SDL_Rect>()
                rect.x = x
                rect.y = y
                rect.w = width
                rect.h = height
                val result = SDL_RenderFillRect(renderer, rect.ptr)
                if (result != 0) {
                    Logger.error("Error filling rectangle: ${SDL_GetError()?.toKString()}")
                }
            }
        }
    }


    fun drawLine(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        r: UByte, g: UByte, b: UByte, a: UByte
    ) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            val result = SDL_RenderDrawLine(renderer, startX, startY, endX, endY)
            if (result != 0) {
                Logger.error("Error drawing line: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawPixel(x: Int, y: Int, rgba: UInt) {
        useContext<SDLContext> {
            val r = ((rgba shr 24) and 0xFFu).toUByte()
            val g = ((rgba shr 16) and 0xFFu).toUByte()
            val b = ((rgba shr 8) and 0xFFu).toUByte()
            val a = (rgba and 0xFFu).toUByte()
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            val result = SDL_RenderDrawPoint(renderer, x, y)
            if (result != 0) {
                Logger.error("Error drawing pixel: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawPixel(x: Int, y: Int, r: UByte, g: UByte, b: UByte, a: UByte) {
        useContext<SDLContext> {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            val result = SDL_RenderDrawPoint(renderer, x, y)
            if (result != 0) {
                Logger.error("Error drawing pixel: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    companion object {
        private var currentContext: GeometryContext? = null

        fun get(): GeometryContext {
            if (currentContext == null) {
                currentContext = GeometryContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create GeometryContext")
        }
    }

    override fun cleanup() {
    }

}