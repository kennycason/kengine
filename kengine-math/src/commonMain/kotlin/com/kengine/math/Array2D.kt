package com.kengine.math

class Array2D<T> private constructor(
    val width: Int,
    val height: Int,
    private val values: MutableList<T>
) {
    init {
        requireValidDimensions(width, height)
        require(values.size == width * height) {
            "Array2D expected ${width * height} values, got ${values.size}."
        }
    }

    operator fun get(x: Int, y: Int): T = values[indexFor(x, y)]

    operator fun get(point: Point2): T = get(point.x, point.y)

    operator fun get(x: Int): Array2DColumn<T> {
        requireXInBounds(x, width)
        return Array2DColumn(this, x)
    }

    operator fun set(x: Int, y: Int, value: T) {
        values[indexFor(x, y)] = value
    }

    operator fun set(point: Point2, value: T) {
        set(point.x, point.y, value)
    }

    fun contains(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun contains(point: Point2): Boolean = contains(point.x, point.y)

    fun toList2D(): List2D<T> = List2D.generate(width, height) { x, y -> get(x, y) }

    fun toRows(): List<List<T>> = toList2D().toRows()

    private fun indexFor(x: Int, y: Int): Int {
        requireInBounds(x, y, width, height)
        return y * width + x
    }

    companion object {
        fun <T> filled(width: Int, height: Int, value: T): Array2D<T> {
            requireValidDimensions(width, height)
            return Array2D(width, height, MutableList(width * height) { value })
        }

        fun <T> generate(width: Int, height: Int, valueAt: (x: Int, y: Int) -> T): Array2D<T> {
            requireValidDimensions(width, height)
            return Array2D(
                width,
                height,
                MutableList(width * height) { index ->
                    val x = index % width
                    val y = index / width
                    valueAt(x, y)
                }
            )
        }

        fun <T> fromRows(rows: List<List<T>>): Array2D<T> {
            val height = rows.size
            val width = rows.firstOrNull()?.size ?: 0
            require(rows.all { it.size == width }) {
                "All Array2D rows must have the same width."
            }
            return generate(width, height) { x, y -> rows[y][x] }
        }
    }
}

class Array2DColumn<T> internal constructor(
    private val grid: Array2D<T>,
    private val x: Int
) {
    operator fun get(y: Int): T = grid[x, y]

    operator fun set(y: Int, value: T) {
        grid[x, y] = value
    }
}
