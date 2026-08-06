package com.kengine.math

data class Rect(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
) {
    fun overlaps(rect: Rect): Boolean {
        return x < rect.x + rect.w &&
            x + w > rect.x &&
            y < rect.y + rect.h &&
            y + h > rect.y
    }

    fun overlaps(rect: IntRect): Boolean {
        return x < rect.x + rect.w &&
            x + w > rect.x &&
            y < rect.y + rect.h &&
            y + h > rect.y
    }

    fun contains(point: Vec2): Boolean {
        return point.x >= x &&
            point.x <= x + w &&
            point.y >= y &&
            point.y <= y + h
    }

    fun contains(point: IntVec2): Boolean {
        return point.x >= x &&
            point.x <= x + w &&
            point.y >= y &&
            point.y <= y + h
    }

    fun area(): Double = w * h

    fun perimeter(): Double = 2 * (w + h)

    fun translate(dx: Double, dy: Double): Rect = copy(x = x + dx, y = y + dy)

    fun translateAssign(dx: Double, dy: Double) {
        x += dx
        y += dy
    }

    fun scale(scaleX: Double, scaleY: Double): Rect = copy(w = w * scaleX, h = h * scaleY)

    fun scaleAssign(scaleX: Double, scaleY: Double) {
        w *= scaleX
        h *= scaleY
    }

    fun intersection(rect: Rect): Rect? {
        val interX = maxOf(x, rect.x)
        val interY = maxOf(y, rect.y)
        val interW = minOf(x + w, rect.x + rect.w) - interX
        val interH = minOf(y + h, rect.y + rect.h) - interY

        return if (interW > 0 && interH > 0) {
            Rect(interX, interY, interW, interH)
        } else {
            null
        }
    }

    fun union(rect: Rect): Rect {
        val unionX = minOf(x, rect.x)
        val unionY = minOf(y, rect.y)
        val unionW = maxOf(x + w, rect.x + rect.w) - unionX
        val unionH = maxOf(y + h, rect.y + rect.h) - unionY
        return Rect(unionX, unionY, unionW, unionH)
    }
}
