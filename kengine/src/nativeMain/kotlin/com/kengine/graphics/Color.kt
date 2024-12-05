package com.kengine.graphics

import platform.darwin.UInt32

data class Color(
    val r: UByte,
    val g: UByte,
    val b: UByte,
    val a: UByte
) {
    fun toUInt32(): UInt32 =
        (r.toUInt() shl 24 or (g.toUInt() shl 16) or (b.toUInt() shl 8) or a.toUInt())

    companion object {
        fun fromUInt32(color: UInt32): Color = Color(
            r = (color shr 24 and 0xFFu).toUByte(),
            g = (color shr 16 and 0xFFu).toUByte(),
            b = (color shr 8 and 0xFFu).toUByte(),
            a = (color and 0xFFu).toUByte()
        )
    }
}