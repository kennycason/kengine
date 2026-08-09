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
            RenderCommandType.DRAW_TRIANGLE -> fillTriangle(
                x1 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_X),
                y1 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_Y),
                x2 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_WIDTH),
                y2 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_HEIGHT),
                x3 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR2),
                y3 = render.commandField(commandIndex, RenderCommandBuffer.FIELD_PARAM),
                color = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR),
                screenWidth = render.width,
                screenHeight = render.height
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
            RenderCommandType.DRAW_TEXT -> drawText(
                text = render.commandText(commandIndex),
                x = render.commandField(commandIndex, RenderCommandBuffer.FIELD_X),
                y = render.commandField(commandIndex, RenderCommandBuffer.FIELD_Y),
                color = render.commandField(commandIndex, RenderCommandBuffer.FIELD_COLOR),
                scale = render.commandField(commandIndex, RenderCommandBuffer.FIELD_WIDTH)
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

    private fun drawText(text: String, x: Int, y: Int, color: Int, scale: Int) {
        val safeScale = scale.coerceAtLeast(1)
        var cursorX = x
        var cursorY = y

        for (char in text) {
            when (char) {
                '\n' -> {
                    cursorX = x
                    cursorY += TEXT_LINE_HEIGHT * safeScale
                }
                ' ' -> cursorX += TEXT_SPACE_WIDTH * safeScale
                else -> {
                    drawGlyph(asciiUpper(char), cursorX, cursorY, color, safeScale)
                    cursorX += TEXT_GLYPH_ADVANCE * safeScale
                }
            }
        }
    }

    private fun drawGlyph(char: Char, x: Int, y: Int, color: Int, scale: Int) {
        val bits = glyphBits(char)
        var row = 0
        while (row < TEXT_GLYPH_HEIGHT) {
            val rowBits = ((bits shr ((TEXT_GLYPH_HEIGHT - 1 - row) * TEXT_GLYPH_WIDTH)) and 0x1f).toInt()
            var column = 0
            while (column < TEXT_GLYPH_WIDTH) {
                if ((rowBits and (1 shl (TEXT_GLYPH_WIDTH - 1 - column))) != 0) {
                    fillRect(x + column * scale, y + row * scale, scale, scale, color)
                }
                column += 1
            }
            row += 1
        }
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

    private fun fillTriangle(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        x3: Int,
        y3: Int,
        color: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        var ax = x1
        var ay = y1
        var bx = x2
        var by = y2
        var cx = x3
        var cy = y3

        if (ay > by) {
            val swapX = ax
            val swapY = ay
            ax = bx
            ay = by
            bx = swapX
            by = swapY
        }
        if (by > cy) {
            val swapX = bx
            val swapY = by
            bx = cx
            by = cy
            cx = swapX
            cy = swapY
        }
        if (ay > by) {
            val swapX = ax
            val swapY = ay
            ax = bx
            ay = by
            bx = swapX
            by = swapY
        }

        if (cy < 0 || ay >= screenHeight || screenWidth <= 0 || screenHeight <= 0) return

        if (ay == cy) {
            drawHorizontalSpan(ay, minOf(ax, bx, cx), maxOf(ax, bx, cx), color, screenWidth, screenHeight)
            return
        }

        var row = ay.coerceAtLeast(0)
        val lastRow = cy.coerceAtMost(screenHeight - 1)
        while (row <= lastRow) {
            val longX = interpolateX(ax, ay, cx, cy, row)
            val shortX = if (row < by) {
                interpolateX(ax, ay, bx, by, row)
            } else {
                interpolateX(bx, by, cx, cy, row)
            }
            drawHorizontalSpan(row, longX, shortX, color, screenWidth, screenHeight)
            row += 1
        }
    }

    private fun interpolateX(x1: Int, y1: Int, x2: Int, y2: Int, y: Int): Int {
        if (y1 == y2) return x1
        return x1 + (((x2.toLong() - x1.toLong()) * (y.toLong() - y1.toLong())) / (y2.toLong() - y1.toLong())).toInt()
    }

    private fun drawHorizontalSpan(y: Int, x1: Int, x2: Int, color: Int, screenWidth: Int, screenHeight: Int) {
        if (y < 0 || y >= screenHeight) return
        var left = minOf(x1, x2)
        var right = maxOf(x1, x2)
        if (right < 0 || left >= screenWidth) return
        left = left.coerceAtLeast(0)
        right = right.coerceAtMost(screenWidth - 1)
        drawLine(left, y, right, y, color)
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

    private fun asciiUpper(char: Char): Char {
        return if (char in 'a'..'z') {
            (char.code - 32).toChar()
        } else {
            char
        }
    }

    private fun glyphBits(char: Char): Long {
        return when (char) {
            '0' -> glyph(0x0e, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0e)
            '1' -> glyph(0x04, 0x0c, 0x04, 0x04, 0x04, 0x04, 0x0e)
            '2' -> glyph(0x0e, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1f)
            '3' -> glyph(0x1e, 0x01, 0x01, 0x0e, 0x01, 0x01, 0x1e)
            '4' -> glyph(0x02, 0x06, 0x0a, 0x12, 0x1f, 0x02, 0x02)
            '5' -> glyph(0x1f, 0x10, 0x10, 0x1e, 0x01, 0x01, 0x1e)
            '6' -> glyph(0x0e, 0x10, 0x10, 0x1e, 0x11, 0x11, 0x0e)
            '7' -> glyph(0x1f, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08)
            '8' -> glyph(0x0e, 0x11, 0x11, 0x0e, 0x11, 0x11, 0x0e)
            '9' -> glyph(0x0e, 0x11, 0x11, 0x0f, 0x01, 0x01, 0x0e)
            'A' -> glyph(0x0e, 0x11, 0x11, 0x1f, 0x11, 0x11, 0x11)
            'B' -> glyph(0x1e, 0x11, 0x11, 0x1e, 0x11, 0x11, 0x1e)
            'C' -> glyph(0x0e, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0e)
            'D' -> glyph(0x1e, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1e)
            'E' -> glyph(0x1f, 0x10, 0x10, 0x1e, 0x10, 0x10, 0x1f)
            'F' -> glyph(0x1f, 0x10, 0x10, 0x1e, 0x10, 0x10, 0x10)
            'G' -> glyph(0x0e, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0f)
            'H' -> glyph(0x11, 0x11, 0x11, 0x1f, 0x11, 0x11, 0x11)
            'I' -> glyph(0x0e, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0e)
            'J' -> glyph(0x07, 0x02, 0x02, 0x02, 0x12, 0x12, 0x0c)
            'K' -> glyph(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11)
            'L' -> glyph(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1f)
            'M' -> glyph(0x11, 0x1b, 0x15, 0x15, 0x11, 0x11, 0x11)
            'N' -> glyph(0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11)
            'O' -> glyph(0x0e, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0e)
            'P' -> glyph(0x1e, 0x11, 0x11, 0x1e, 0x10, 0x10, 0x10)
            'Q' -> glyph(0x0e, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0d)
            'R' -> glyph(0x1e, 0x11, 0x11, 0x1e, 0x14, 0x12, 0x11)
            'S' -> glyph(0x0f, 0x10, 0x10, 0x0e, 0x01, 0x01, 0x1e)
            'T' -> glyph(0x1f, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04)
            'U' -> glyph(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0e)
            'V' -> glyph(0x11, 0x11, 0x11, 0x11, 0x11, 0x0a, 0x04)
            'W' -> glyph(0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x0a)
            'X' -> glyph(0x11, 0x11, 0x0a, 0x04, 0x0a, 0x11, 0x11)
            'Y' -> glyph(0x11, 0x11, 0x0a, 0x04, 0x04, 0x04, 0x04)
            'Z' -> glyph(0x1f, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1f)
            ':' -> glyph(0x00, 0x04, 0x04, 0x00, 0x04, 0x04, 0x00)
            '-' -> glyph(0x00, 0x00, 0x00, 0x1f, 0x00, 0x00, 0x00)
            '/' -> glyph(0x01, 0x01, 0x02, 0x04, 0x08, 0x10, 0x10)
            '.' -> glyph(0x00, 0x00, 0x00, 0x00, 0x00, 0x0c, 0x0c)
            ',' -> glyph(0x00, 0x00, 0x00, 0x00, 0x0c, 0x04, 0x08)
            '+' -> glyph(0x00, 0x04, 0x04, 0x1f, 0x04, 0x04, 0x00)
            '!' -> glyph(0x04, 0x04, 0x04, 0x04, 0x04, 0x00, 0x04)
            '_' -> glyph(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1f)
            '=' -> glyph(0x00, 0x00, 0x1f, 0x00, 0x1f, 0x00, 0x00)
            else -> glyph(0x0e, 0x11, 0x01, 0x02, 0x04, 0x00, 0x04)
        }
    }

    private fun glyph(row0: Int, row1: Int, row2: Int, row3: Int, row4: Int, row5: Int, row6: Int): Long {
        return (row0.toLong() shl 30) or
            (row1.toLong() shl 25) or
            (row2.toLong() shl 20) or
            (row3.toLong() shl 15) or
            (row4.toLong() shl 10) or
            (row5.toLong() shl 5) or
            row6.toLong()
    }

    companion object {
        private const val TEXT_GLYPH_WIDTH = 5
        private const val TEXT_GLYPH_HEIGHT = 7
        private const val TEXT_GLYPH_ADVANCE = 6
        private const val TEXT_SPACE_WIDTH = 4
        private const val TEXT_LINE_HEIGHT = 8
    }
}
