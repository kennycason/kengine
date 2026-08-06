package com.kengine.math

class List2D<out T> private constructor(
    val width: Int,
    val height: Int,
    private val values: List<T>
) {
    init {
        requireValidDimensions(width, height)
        require(values.size == width * height) {
            "List2D expected ${width * height} values, got ${values.size}."
        }
    }

    operator fun get(x: Int, y: Int): T = values[indexFor(x, y)]

    operator fun get(point: Point2): T = get(point.x, point.y)

    operator fun get(x: Int): List2DColumn<T> {
        requireXInBounds(x, width)
        return List2DColumn(this, x)
    }

    fun contains(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun contains(point: Point2): Boolean = contains(point.x, point.y)

    fun toRows(): List<List<T>> {
        return List(height) { y ->
            List(width) { x -> get(x, y) }
        }
    }

    private fun indexFor(x: Int, y: Int): Int {
        requireInBounds(x, y, width, height)
        return y * width + x
    }

    companion object {
        fun <T> filled(width: Int, height: Int, value: T): List2D<T> {
            requireValidDimensions(width, height)
            return List2D(width, height, List(width * height) { value })
        }

        fun <T> generate(width: Int, height: Int, valueAt: (x: Int, y: Int) -> T): List2D<T> {
            requireValidDimensions(width, height)
            return List2D(
                width,
                height,
                List(width * height) { index ->
                    val x = index % width
                    val y = index / width
                    valueAt(x, y)
                }
            )
        }

        fun <T> fromRows(rows: List<List<T>>): List2D<T> {
            val height = rows.size
            val width = rows.firstOrNull()?.size ?: 0
            require(rows.all { it.size == width }) {
                "All List2D rows must have the same width."
            }
            return generate(width, height) { x, y -> rows[y][x] }
        }
    }
}

class List2DColumn<out T> internal constructor(
    private val grid: List2D<T>,
    private val x: Int
) {
    operator fun get(y: Int): T = grid[x, y]
}
