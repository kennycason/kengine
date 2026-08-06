package com.kengine.math

data class IntRect(
    var x: Int = 0,
    var y: Int = 0,
    var w: Int = 0,
    var h: Int = 0,
) {
    fun overlaps(rect: IntRect): Boolean {
        return x < rect.x + rect.w &&
            x + w > rect.x &&
            y < rect.y + rect.h &&
            y + h > rect.y
    }

    fun overlaps(rect: Rect): Boolean {
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

    fun area(): Int = w * h

    fun perimeter(): Int = 2 * (w + h)

    fun translate(dx: Int, dy: Int): IntRect = copy(x = x + dx, y = y + dy)

    fun translateAssign(dx: Int, dy: Int) {
        x += dx
        y += dy
    }

    fun scale(scaleX: Int, scaleY: Int): IntRect = copy(w = w * scaleX, h = h * scaleY)

    fun scaleAssign(scaleX: Int, scaleY: Int) {
        w *= scaleX
        h *= scaleY
    }

    fun intersection(rect: IntRect): IntRect? {
        val interX = maxOf(x, rect.x)
        val interY = maxOf(y, rect.y)
        val interW = minOf(x + w, rect.x + rect.w) - interX
        val interH = minOf(y + h, rect.y + rect.h) - interY

        return if (interW > 0 && interH > 0) {
            IntRect(interX, interY, interW, interH)
        } else {
            null
        }
    }

    fun union(rect: IntRect): IntRect {
        val unionX = minOf(x, rect.x)
        val unionY = minOf(y, rect.y)
        val unionW = maxOf(x + w, rect.x + rect.w) - unionX
        val unionH = maxOf(y + h, rect.y + rect.h) - unionY
        return IntRect(unionX, unionY, unionW, unionH)
    }
}
