package com.kengine.graphics


fun redFromRGBA(rgba: UInt): UByte {
    return ((rgba shr 24) and 0xFFu).toUByte()
}

fun greenFromRGBA(rgba: UInt): UByte {
    return ((rgba shr 16) and 0xFFu).toUByte()
}

fun blueFromRGBA(rgba: UInt): UByte {
    return ((rgba shr 8) and 0xFFu).toUByte()
}

fun alphaFromRGBA(rgba: UInt): UByte {
    return (rgba and 0xFFu).toUByte()
}

fun redFromRGB(rgb: UInt): UByte {
    return ((rgb shr 16) and 0xFFu).toUByte()
}

fun greenFromRGB(rgb: UInt): UByte {
    return ((rgb shr 8) and 0xFFu).toUByte()
}

fun blueFromRGB(rgb: UInt): UByte {
    return (rgb and 0xFFu).toUByte()
}
