package com.kengine.render

import com.kengine.geometry.useGeometryContext
import com.kengine.sdl.useSDLContext

class RenderContextSdlRenderer(
    private val spriteRegistry: PortableSpriteRegistry = PortableSpriteRegistry()
) {
    fun render(render: RenderContext) {
        var commandIndex = 0
        while (commandIndex < render.commandCount) {
            execute(render, commandIndex)
            commandIndex += 1
        }
    }

    private fun execute(render: RenderContext, commandIndex: Int) {
        when (render.commandField(commandIndex, RenderCommandBuffer.FIELD_TYPE)) {
            RenderCommandType.CLEAR -> clear(render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR))
            RenderCommandType.FILL_RECT -> fillRect(
                x = render.commandField(commandIndex, RenderCommandBuffer.FIELD_X),
                y = render.commandField(commandIndex, RenderCommandBuffer.FIELD_Y),
                width = render.commandField(commandIndex, RenderCommandBuffer.FIELD_WIDTH),
                height = render.commandField(commandIndex, RenderCommandBuffer.FIELD_HEIGHT),
                color = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR)
            )
            RenderCommandType.DRAW_LINE -> drawLine(
                startX = render.commandField(commandIndex, RenderCommandBuffer.FIELD_X),
                startY = render.commandField(commandIndex, RenderCommandBuffer.FIELD_Y),
                endX = render.commandField(commandIndex, RenderCommandBuffer.FIELD_WIDTH),
                endY = render.commandField(commandIndex, RenderCommandBuffer.FIELD_HEIGHT),
                color = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR)
            )
            RenderCommandType.DRAW_SPRITE -> drawSprite(
                x = render.commandField(commandIndex, RenderCommandBuffer.FIELD_X),
                y = render.commandField(commandIndex, RenderCommandBuffer.FIELD_Y),
                width = render.commandField(commandIndex, RenderCommandBuffer.FIELD_WIDTH),
                height = render.commandField(commandIndex, RenderCommandBuffer.FIELD_HEIGHT),
                tint = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR),
                spriteId = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR2),
                frame = render.commandField(commandIndex, RenderCommandBuffer.FIELD_PARAM)
            )
            RenderCommandType.VERTICAL_GRADIENT -> verticalGradient(
                render = render,
                topColor = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR),
                bottomColor = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR2),
                pulseSeed = render.commandField(commandIndex, RenderCommandBuffer.FIELD_PARAM)
            )
        }
    }

    private fun clear(color: Int) {
        useSDLContext {
            fillScreen(red(color), green(color), blue(color), alpha(color))
        }
    }

    private fun fillRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        if (width <= 0 || height <= 0) return
        useGeometryContext {
            fillRectangle(
                x = x.toDouble(),
                y = y.toDouble(),
                width = width.toDouble(),
                height = height.toDouble(),
                r = red(color),
                g = green(color),
                b = blue(color),
                a = alpha(color)
            )
        }
    }

    private fun drawSprite(x: Int, y: Int, width: Int, height: Int, tint: Int, spriteId: Int, frame: Int) {
        if (width <= 0 || height <= 0) return

        val sprite = spriteRegistry.getSprite(spriteId, frame)
        if (sprite != null) {
            sprite.draw(
                x = x.toDouble(),
                y = y.toDouble(),
                width = width.toDouble(),
                height = height.toDouble()
            )
            return
        }

        fillRect(x, y, width, height, tint)
        drawLine(x, y, x + width, y + height, RenderContext.WHITE)
        drawLine(x + width, y, x, y + height, RenderContext.WHITE)
    }

    private fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, color: Int) {
        useGeometryContext {
            drawLine(
                startX = startX.toDouble(),
                startY = startY.toDouble(),
                endX = endX.toDouble(),
                endY = endY.toDouble(),
                r = red(color),
                g = green(color),
                b = blue(color),
                a = alpha(color)
            )
        }
    }

    private fun verticalGradient(render: RenderContext, topColor: Int, bottomColor: Int, pulseSeed: Int) {
        var y = 0
        while (y < render.height) {
            val amount = (y * 120) / render.height
            val pulse = ((pulseSeed + y) shr 4) and 15
            val rowColor = colorAdd(colorMix(topColor, bottomColor, amount, 255), pulse)
            fillRect(0, y, render.width, 1, rowColor)
            y += 1
        }
    }

    private fun colorMix(from: Int, to: Int, amount: Int, maximum: Int): Int {
        val safeMaximum = maximum.coerceAtLeast(1)
        val safeAmount = amount.coerceIn(0, safeMaximum)
        val inverse = safeMaximum - safeAmount

        val r = (redInt(from) * inverse + redInt(to) * safeAmount) / safeMaximum
        val g = (greenInt(from) * inverse + greenInt(to) * safeAmount) / safeMaximum
        val b = (blueInt(from) * inverse + blueInt(to) * safeAmount) / safeMaximum
        return rgba(r, g, b, 255)
    }

    private fun colorAdd(color: Int, amount: Int): Int {
        return rgba(
            (redInt(color) + amount).coerceIn(0, 255),
            (greenInt(color) + amount).coerceIn(0, 255),
            (blueInt(color) + amount).coerceIn(0, 255),
            alphaInt(color)
        )
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int): Int {
        return (r.coerceIn(0, 255)) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (a.coerceIn(0, 255) shl 24)
    }

    private fun red(color: Int): UByte = redInt(color).toUByte()
    private fun green(color: Int): UByte = greenInt(color).toUByte()
    private fun blue(color: Int): UByte = blueInt(color).toUByte()
    private fun alpha(color: Int): UByte = alphaInt(color).toUByte()

    private fun redInt(color: Int): Int = color and 0xff
    private fun greenInt(color: Int): Int = (color ushr 8) and 0xff
    private fun blueInt(color: Int): Int = (color ushr 16) and 0xff
    private fun alphaInt(color: Int): Int = (color ushr 24) and 0xff
}
