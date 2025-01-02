package com.kengine.graphics

import kotlin.math.absoluteValue

data class Color(
    val r: UByte,
    val g: UByte,
    val b: UByte,
    val a: UByte
) {
    fun toUInt(): UInt =
        (r.toUInt() shl 24 or (g.toUInt() shl 16) or (b.toUInt() shl 8) or a.toUInt())

    fun invert(): Color {
        return Color(
            (255u - r).toUByte(),
            (255u - g).toUByte(),
            (255u - b).toUByte(),
            a
        )
    }

    inline fun adjustBrightness(factor: Float): Color {
        return Color(
            ((r.toFloat() * factor).coerceIn(0f, 255f)).toInt().toUByte(),
            ((g.toFloat() * factor).coerceIn(0f, 255f)).toInt().toUByte(),
            ((b.toFloat() * factor).coerceIn(0f, 255f)).toInt().toUByte(),
            a
        )
    }

    inline fun blend(other: Color, t: Float): Color {
        val clampedT = t.coerceIn(0f, 1f)
        // notes on casting: Converting UByte to Float is a direct operation without intermediate steps like UByte → Int → Float.
        return Color(
            ((r.toFloat() * (1 - clampedT)) + (other.r.toFloat() * clampedT)).toInt().toUByte(),
            ((g.toFloat() * (1 - clampedT)) + (other.g.toFloat() * clampedT)).toInt().toUByte(),
            ((b.toFloat() * (1 - clampedT)) + (other.b.toFloat() * clampedT)).toInt().toUByte(),
            ((a.toFloat() * (1 - clampedT)) + (other.a.toFloat() * clampedT)).toInt().toUByte()
        )
    }

    fun toGrayscale(): Color {
        val gray = ((r.toInt() * 0.299) + (g.toInt() * 0.587) + (b.toInt() * 0.114)).toInt().toUByte()
        return Color(gray, gray, gray, a)
    }

    companion object {
        fun fromUInt32(color: UInt): Color {
            return Color(
                r = (color shr 24 and 0xFFu).toUByte(),
                g = (color shr 16 and 0xFFu).toUByte(),
                b = (color shr 8 and 0xFFu).toUByte(),
                a = (color and 0xFFu).toUByte()
            )
        }

        fun fromRGBA(r: Float, g: Float, b: Float, a: Float = 1.0f): Color {
            return Color(
                (r.coerceIn(0.0f, 1.0f) * 255).toInt().toUByte(),
                (g.coerceIn(0.0f, 1.0f) * 255).toInt().toUByte(),
                (b.coerceIn(0.0f, 1.0f) * 255).toInt().toUByte(),
                (a.coerceIn(0.0f, 1.0f) * 255).toInt().toUByte()
            )
        }

        /**
         * RGB: ff0099
         * RGBA: ff0099ff
         */
        fun fromHex(hex: String): Color {
            require(hex.length == 6 || hex.length == 8) { "Hex string must be 6 (RGB) or 8 (RGBA) characters." }
            val r = hex.substring(0, 2).toInt(16).toUByte()
            val g = hex.substring(2, 4).toInt(16).toUByte()
            val b = hex.substring(4, 6).toInt(16).toUByte()
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16).toUByte() else 0xFFu.toUByte()
            return Color(r, g, b, a)
        }

        fun fromHSV(h: Float, saturation: Float = 1.0f, value: Float = 1.0f, alpha: Float = 1.0f): Color {
            val c = value * saturation
            val x = c * (1 - ((h / 60f) % 2 - 1).absoluteValue)
            val m = value - c

            val (r, g, b) = when {
                h < 60 -> Triple(c, x, 0f)
                h < 120 -> Triple(x, c, 0f)
                h < 180 -> Triple(0f, c, x)
                h < 240 -> Triple(0f, x, c)
                h < 300 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            return Color(
                ((r + m) * 255).toInt().toUByte(),
                ((g + m) * 255).toInt().toUByte(),
                ((b + m) * 255).toInt().toUByte(),
                (alpha * 255).toInt().toUByte()
            )
        }

        // Color helpers, thanks ChatGPT :)

        // Grayscale Shades (0x00 - 0xFF)
        val black = Color(0x00u, 0x00u, 0x00u, 0xFFu) // #000000
        val white = Color(0xFFu, 0xFFu, 0xFFu, 0xFFu) // #FFFFFF

        val gray10 = Color(0x1Au, 0x1Au, 0x1Au, 0xFFu) // #1A1A1A
        val gray20 = Color(0x33u, 0x33u, 0x33u, 0xFFu) // #333333
        val gray30 = Color(0x4Du, 0x4Du, 0x4Du, 0xFFu) // #4D4D4D
        val gray40 = Color(0x66u, 0x66u, 0x66u, 0xFFu) // #666666
        val gray50 = Color(0x80u, 0x80u, 0x80u, 0xFFu) // #808080
        val gray60 = Color(0x99u, 0x99u, 0x99u, 0xFFu) // #999999
        val gray70 = Color(0xB3u, 0xB3u, 0xB3u, 0xFFu) // #B3B3B3
        val gray80 = Color(0xCCu, 0xCCu, 0xCCu, 0xFFu) // #CCCCCC
        val gray90 = Color(0xE6u, 0xE6u, 0xE6u, 0xFFu) // #E6E6E6

        // Basic Colors
        val red = Color(0xFFu, 0x00u, 0x00u, 0xFFu)    // #FF0000
        val green = Color(0x00u, 0xFFu, 0x00u, 0xFFu)  // #00FF00
        val blue = Color(0x00u, 0x00u, 0xFFu, 0xFFu)   // #0000FF
        val yellow = Color(0xFFu, 0xFFu, 0x00u, 0xFFu) // #FFFF00
        val cyan = Color(0x00u, 0xFFu, 0xFFu, 0xFFu)   // #00FFFF
        val magenta = Color(0xFFu, 0x00u, 0xFFu, 0xFFu) // #FF00FF
        val orange = Color(0xFFu, 0xA5u, 0x00u, 0xFFu) // #FFA500
        val purple = Color(0x80u, 0x00u, 0x80u, 0xFFu) // #800080
        val pink = Color(0xFFu, 0xC0u, 0xCBu, 0xFFu)   // #FFC0CB
        val brown = Color(0xA5u, 0x2Au, 0x2Au, 0xFFu)  // #A52A2A
        val gold = Color(0xFFu, 0xD7u, 0x00u, 0xFFu)   // #FFD700
        val silver = Color(0xC0u, 0xC0u, 0xC0u, 0xFFu) // #C0C0C0

        // Cool Tones
        val teal = Color(0x00u, 0x80u, 0x80u, 0xFFu)   // #008080
        val navy = Color(0x00u, 0x00u, 0x80u, 0xFFu)   // #000080
        val skyBlue = Color(0x87u, 0xCEu, 0xEBu, 0xFFu) // #87CEEB
        val aqua = Color(0x00u, 0xFFu, 0xFFu, 0xFFu)  // #00FFFF
        val indigo = Color(0x4Bu, 0x00u, 0x82u, 0xFFu) // #4B0082

        // Warm Tones
        val coral = Color(0xFFu, 0x7Fu, 0x50u, 0xFFu)  // #FF7F50
        val salmon = Color(0xFAu, 0x80u, 0x72u, 0xFFu) // #FA8072
        val crimson = Color(0xDCu, 0x14u, 0x3Cu, 0xFFu) // #DC143C
        val maroon = Color(0x80u, 0x00u, 0x00u, 0xFFu) // #800000

        // Nature-Inspired
        val forestGreen = Color(0x22u, 0x8Bu, 0x22u, 0xFFu) // #228B22
        val limeGreen = Color(0x32u, 0xCDu, 0x32u, 0xFFu)   // #32CD32
        val olive = Color(0x80u, 0x80u, 0x00u, 0xFFu)       // #808000
        val wheat = Color(0xF5u, 0xDEu, 0xB3u, 0xFFu)      // #F5DEB3
        val tan = Color(0xD2u, 0xB4u, 0x8Cu, 0xFFu)         // #D2B48C

        // Dark Tones
        val darkRed = Color(0x8Bu, 0x00u, 0x00u, 0xFFu)     // #8B0000
        val darkGreen = Color(0x00u, 0x64u, 0x00u, 0xFFu)   // #006400
        val darkBlue = Color(0x00u, 0x00u, 0x8Bu, 0xFFu)    // #00008B
        val darkSlateGray = Color(0x2Fu, 0x4Fu, 0x4Fu, 0xFFu) // #2F4F4F
        val midnightBlue = Color(0x19u, 0x19u, 0x70u, 0xFFu) // #191970

        // Neon Colors 🟩🟦🟪🟥
        val neonPink = Color(0xFFu, 0x14u, 0x93u, 0xFFu)     // #FF1493
        val neonGreen = Color(0x39u, 0xFFu, 0x14u, 0xFFu)    // #39FF14
        val neonBlue = Color(0x1Fu, 0x51u, 0xFFu, 0xFFu)     // #1F51FF
        val electricLime = Color(0xCCu, 0xFFu, 0x00u, 0xFFu) // #CCFF00
        val hotPink = Color(0xFFu, 0x69u, 0xB4u, 0xFFu)     // #FF69B4

        val neonYellow = Color(0xFFu, 0xFFu, 0x00u, 0xFFu)      // #FFFF00
        val neonOrange = Color(0xFFu, 0xA5u, 0x00u, 0xFFu)      // #FFA500
        val neonCyan = Color(0x00u, 0xFFu, 0xFFu, 0xFFu)        // #00FFFF
        val neonPurple = Color(0x8Au, 0x2Bu, 0xE2u, 0xFFu)      // #8A2BE2
        val neonLime = Color(0xADu, 0xFFu, 0x2Fu, 0xFFu)        // #ADFF2F
        val neonBlueGreen = Color(0x0Fu, 0xFFu, 0xF0u, 0xFFu)   // #0FFFF0
        val neonMagenta = Color(0xFFu, 0x00u, 0xFFu, 0xFFu)     // #FF00FF
        val neonTurquoise = Color(0x40u, 0xE0u, 0xD0u, 0xFFu)   // #40E0D0
        val neonPeach = Color(0xFFu, 0x85u, 0x72u, 0xFFu)       // #FF8572

        // 🌈 Rainbow Palette (8 Shades)
        val rainbow1Red = Color(0xFFu, 0x00u, 0x00u, 0xFFu)      // Red
        val rainbow2Orange = Color(0xFFu, 0x7Fu, 0x00u, 0xFFu)   // Orange
        val rainbow3Yellow = Color(0xFFu, 0xFFu, 0x00u, 0xFFu)   // Yellow
        val rainbow4Green = Color(0x00u, 0xFFu, 0x00u, 0xFFu)    // Green
        val rainbow5Blue = Color(0x00u, 0x00u, 0xFFu, 0xFFu)     // Blue
        val rainbow6Indigo = Color(0x4Bu, 0x00u, 0x82u, 0xFFu)   // Indigo
        val rainbow7Violet = Color(0x8Au, 0x2Bu, 0xE2u, 0xFFu)   // Violet
        val rainbow8Pink = Color(0xFFu, 0x14u, 0x93u, 0xFFu)     // Neon Pink 💖

        fun rainbow(steps: Int): List<Color> {
            require(steps > 0) { "Steps must be greater than 0" }
            val maxHue = 300f // Maximum hue to prevent wrap-around (stops before red again)
            return List(steps) { i ->
                val hue = (i.toFloat() / (steps - 1)) * maxHue // Avoid looping to red
                fromHSV(hue, saturation = 1.0f, value = 1.0f)
            }
        }

        // 🎨 Retro Palettes 🎮
        object Palettes {
            // Gameboy DMG (Original Green Palette)
            object Gameboy {
                val darkGreen = Color(0x0Fu, 0x38u, 0x0Fu, 0xFFu) // #0F380F
                val mediumGreen = Color(0x30u, 0x62u, 0x30u, 0xFFu) // #306230
                val lightGreen = Color(0x8Bu, 0xACu, 0x0Fu, 0xFFu) // #8BAC0F
                val paleGreen = Color(0x9Bu, 0xBCu, 0x0Fu, 0xFFu) // #9BBC0F
            }

            // NES Palette
            object NES {
                val black = Color(0x00u, 0x00u, 0x00u, 0xFFu)   // #000000
                val gray = Color(0xB0u, 0xB0u, 0xB0u, 0xFFu)   // #B0B0B0
                val red = Color(0xFFu, 0x00u, 0x00u, 0xFFu)    // #FF0000
                val blue = Color(0x00u, 0x00u, 0xFFu, 0xFFu)   // #0000FF
                val yellow = Color(0xFFu, 0xFFu, 0x00u, 0xFFu) // #FFFF00
                val cyan = Color(0x00u, 0xFFu, 0xFFu, 0xFFu)   // #00FFFF
                val green = Color(0x00u, 0xFFu, 0x00u, 0xFFu)  // #00FF00
            }

            // Metroid Palette (NES)
            object Metroid {
                val samusRed = Color(0xA0u, 0x10u, 0x20u, 0xFFu)  // #A01020
                val suitYellow = Color(0xFFu, 0xD7u, 0x00u, 0xFFu) // #FFD700
                val energyBlue = Color(0x00u, 0x88u, 0xFFu, 0xFFu) // #0088FF
                val missileRed = Color(0xFFu, 0x44u, 0x44u, 0xFFu) // #FF4444
            }

            object SuperMarioBros {
                // Ground, bricks, and pipes
                val groundBrown = Color(0xB0u, 0x60u, 0x00u, 0xFFu)  // #B06000
                val pipeGreen = Color(0x00u, 0x88u, 0x00u, 0xFFu)   // #008800
                val brickRed = Color(0xC0u, 0x20u, 0x20u, 0xFFu)   // #C02020

                // Background sky
                val skyBlue = Color(0x88u, 0xD8u, 0xF8u, 0xFFu)     // #88D8F8
                val cloudWhite = Color(0xFCu, 0xFCu, 0xFCu, 0xFFu) // #FCFCFC

                // Mario
                val marioRed = Color(0xFFu, 0x00u, 0x00u, 0xFFu)    // #FF0000
                val marioBlue = Color(0x00u, 0x00u, 0xFFu, 0xFFu)   // #0000FF
                val marioBrown = Color(0x80u, 0x40u, 0x00u, 0xFFu)  // #804000

                // Coins and power-ups
                val coinGold = Color(0xFFu, 0xD7u, 0x00u, 0xFFu)    // #FFD700
                val mushroomRed = Color(0xFFu, 0x40u, 0x40u, 0xFFu) // #FF4040
                val fireFlowerOrange = Color(0xFFu, 0x80u, 0x00u, 0xFFu) // #FF8000
            }
        }
    }
}
