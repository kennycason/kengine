package com.kengine.geometry

import com.kengine.graphics.Color
import com.kengine.graphics.alphaFromRGBA
import com.kengine.graphics.blueFromRGBA
import com.kengine.graphics.greenFromRGBA
import com.kengine.graphics.redFromRGBA
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import sdl3.SDL_FRect
import sdl3.SDL_GetError
import sdl3.SDL_RenderFillRect
import sdl3.SDL_RenderRect
import sdl3.SDL_SetRenderDrawColor
import sdl3.image.SDL_RenderLine
import sdl3.image.SDL_RenderPoint

@OptIn(ExperimentalForeignApi::class)
class GeometryContext private constructor() : Context(), Logging {

    fun drawCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        rgba: UInt
    ) = drawCircle(
        centerX, centerY, radius,
        r = redFromRGBA(rgba),
        g = greenFromRGBA(rgba),
        b = blueFromRGBA(rgba),
        a = alphaFromRGBA(rgba)
    )

    fun drawCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        color: Color
    ) = drawCircle(
        centerX, centerY, radius,
        r = color.r,
        g = color.g,
        b = color.b,
        a = color.a
    )

    fun drawCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            // SDL3 doesn't have a built-in circle-drawing function. Use a custom implementation.
            drawCirclePoints(centerX, centerY, radius, r, g, b, a)
        }
    }

    private fun drawCirclePoints(
        centerX: Int, centerY: Int, radius: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        // mid-point circle algorithm
        var x = radius
        var y = 0
        var p = 1 - radius

        while (x >= y) {
            drawSymmetricCirclePoints(centerX, centerY, x, y, r, g, b, a)
            y++
            if (p <= 0) {
                p += 2 * y + 1
            } else {
                x--
                p += 2 * y - 2 * x + 1
            }
        }
    }

    private fun drawSymmetricCirclePoints(
        centerX: Int, centerY: Int, x: Int, y: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        val points = listOf(
            centerX + x to centerY + y, centerX - x to centerY + y,
            centerX + x to centerY - y, centerX - x to centerY - y,
            centerX + y to centerY + x, centerX - y to centerY + x,
            centerX + y to centerY - x, centerX - y to centerY - x
        )
        for ((px, py) in points) {
            drawPixel(px, py, r, g, b, a)
        }
    }

    fun fillCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        color: Color
    ) = fillCircle(
        centerX, centerY, radius,
        r = color.r,
        g = color.g,
        b = color.b,
        a = color.a
    )

    fun fillCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        rgba: UInt
    ) = fillCircle(
        centerX, centerY, radius,
        r = redFromRGBA(rgba),
        g = greenFromRGBA(rgba),
        b = blueFromRGBA(rgba),
        a = alphaFromRGBA(rgba)
    )

    fun fillCircle(
        centerX: Int, centerY: Int,
        radius: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            for (y in -radius..radius) {
                val width = kotlin.math.sqrt((radius * radius - y * y).toDouble()).toInt()
                drawLine(centerX - width, centerY + y, centerX + width, centerY + y, r, g, b, a)
            }
        }
    }

    fun drawRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        color: Color
    ) = drawRectangle(
        x, y, width, height,
        r = color.r,
        g = color.g,
        b = color.b,
        a = color.a
    )

    fun drawRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        rgba: UInt
    ) = drawRectangle(
        x, y, width, height,
        r = redFromRGBA(rgba),
        g = greenFromRGBA(rgba),
        b = blueFromRGBA(rgba),
        a = alphaFromRGBA(rgba)
    )

    fun drawRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            memScoped {
                val rect = alloc<SDL_FRect>().apply {
                    this.x = x.toFloat()
                    this.y = y.toFloat()
                    this.w = width.toFloat()
                    this.h = height.toFloat()
                }
                if (!SDL_RenderRect(renderer, rect.ptr)) {
                    logger.error("Error drawing rectangle: ${SDL_GetError()?.toKString()}")
                }
            }
        }
    }

    fun fillRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        color: Color
    ) = fillRectangle(
        x, y, width, height,
        r = color.r,
        g = color.g,
        b = color.b,
        a = color.a
    )

    fun fillRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        rgba: UInt
    ) = fillRectangle(
        x, y, width, height,
        r = redFromRGBA(rgba),
        g = greenFromRGBA(rgba),
        b = blueFromRGBA(rgba),
        a = alphaFromRGBA(rgba)
    )

    fun fillRectangle(
        x: Int, y: Int,
        width: Int, height: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            memScoped {
                val rect = alloc<SDL_FRect>().apply {
                    this.x = x.toFloat()
                    this.y = y.toFloat()
                    this.w = width.toFloat()
                    this.h = height.toFloat()
                }
                if (!SDL_RenderFillRect(renderer, rect.ptr)) {
                    logger.error("Error filling rectangle: ${SDL_GetError()?.toKString()}")
                }
            }
        }
    }

    fun drawLine(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        color: Color
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, color.r, color.g, color.b, color.a)
            if (!SDL_RenderLine(renderer, startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())) {
                logger.error("Error drawing line: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawLine(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            if (!SDL_RenderLine(renderer, startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())) {
                logger.error("Error drawing line: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawPixel(
        x: Int, y: Int,
        color: Color
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, color.r, color.g, color.b, color.a)
            if (!SDL_RenderPoint(renderer, x.toFloat(), y.toFloat())) {
                logger.error("Error drawing pixel: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    fun drawPixel(
        x: Int, y: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu
    ) {
        useSDLContext {
            SDL_SetRenderDrawColor(renderer, r, g, b, a)
            if (!SDL_RenderPoint(renderer, x.toFloat(), y.toFloat())) {
                logger.error("Error drawing pixel: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    companion object {
        private var currentContext: GeometryContext? = null

        fun get(): GeometryContext {
            return currentContext ?: GeometryContext().also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up GeometryContext"}
        currentContext = null
    }
}
