package com.kengine.math

internal fun requireValidDimensions(width: Int, height: Int) {
    require(width >= 0) { "Width must be non-negative, got $width." }
    require(height >= 0) { "Height must be non-negative, got $height." }
}

internal fun requireXInBounds(x: Int, width: Int) {
    require(x in 0 until width) {
        "X index out of bounds: x=$x width=$width."
    }
}

internal fun requireInBounds(x: Int, y: Int, width: Int, height: Int) {
    require(x in 0 until width && y in 0 until height) {
        "Index out of bounds: x=$x y=$y width=$width height=$height."
    }
}
