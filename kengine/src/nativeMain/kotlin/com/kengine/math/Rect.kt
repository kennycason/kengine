package com.kengine.math

data class Rect(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
) {

    /**
     * Checks if this rectangle overlaps with another rectangle.
     *
     * @param rect The other rectangle to check overlap with.
     * @return True if the rectangles overlap, false otherwise.
     */
    fun overlaps(rect: Rect): Boolean {
        return this.x < rect.x + rect.w &&
                this.x + this.w > rect.x &&
                this.y < rect.y + rect.h &&
                this.y + this.h > rect.y
    }

    /**
     * Checks if this rectangle contains a given point.
     *
     * @param point The point to check.
     * @return True if the point is inside the rectangle, false otherwise.
     */
    fun contains(point: Vec2): Boolean {
        return point.x >= this.x &&
                point.x <= this.x + this.w &&
                point.y >= this.y &&
                point.y <= this.y + this.h
    }

    /**
     * Calculates the area of the rectangle.
     *
     * @return The area (width * height).
     */
    fun area() = w * h

    /**
     * Calculates the perimeter of the rectangle.
     *
     * @return The perimeter (2 * (width + height)).
     */
    fun perimeter(): Double = 2 * (w + h)


    /**
     * Translate and return rectangle by the given offsets.
     *
     * @param dx The offset in the x-direction.
     * @param dy The offset in the y-direction.
     * @return A new Rect instance translated by (dx, dy).
     */
    fun translate(dx: Double, dy: Double): Rect {
        return this.copy(x = this.x + dx, y = this.y + dy)
    }

    /**
     * Translates the current rectangle by the given offsets.
     *
     * @param dx The offset in the x-direction.
     * @param dy The offset in the y-direction.
     */
    fun translateAssign(dx: Double, dy: Double) {
        this.x + dx
        this.y + dy
    }

    /**
     * Scale and return a new rectangle
     *
     * @param scaleX The scale factor in the x-direction.
     * @param scaleY The scale factor in the y-direction.
     * @return A new Rect instance scaled by (scaleX, scaleY).
     */
    fun scale(scaleX: Double, scaleY: Double): Rect {
        return this.copy(w = this.w * scaleX, h = this.h * scaleY)
    }

    /**
     * Scale a rectangle
     *
     * @param scaleX The scale factor in the x-direction.
     * @param scaleY The scale factor in the y-direction.
     */
    fun scaleAssign(scaleX: Double, scaleY: Double) {
        this.w * scaleX
        this.h * scaleY
    }

    /**
     * Computes the intersection of this rectangle and another rectangle.
     *
     * @param rect The other rectangle to intersect with.
     * @return A new Rect representing the intersection, or null if there is no overlap.
     */
    fun intersection(rect: Rect): Rect? {
        val interX = maxOf(this.x, rect.x)
        val interY = maxOf(this.y, rect.y)
        val interW = minOf(this.x + this.w, rect.x + rect.w) - interX
        val interH = minOf(this.y + this.h, rect.y + rect.h) - interY

        return if (interW > 0 && interH > 0) {
            Rect(interX, interY, interW, interH)
        } else {
            null
        }
    }

    /**
     * Computes the union of this rectangle and another rectangle.
     *
     * @param rect The other rectangle to union with.
     * @return A new Rect representing the smallest rectangle that contains both.
     */
    fun union(rect: Rect): Rect {
        val unionX = minOf(this.x, rect.x)
        val unionY = minOf(this.y, rect.y)
        val unionW = maxOf(this.x + this.w, rect.x + rect.w) - unionX
        val unionH = maxOf(this.y + this.h, rect.y + rect.h) - unionY
        return Rect(unionX, unionY, unionW, unionH)
    }

}