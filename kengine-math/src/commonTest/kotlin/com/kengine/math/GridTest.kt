package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GridTest {
    @Test
    fun list2DReadsRowsWithXThenYAccess() {
        val grid = List2D.fromRows(
            listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6)
            )
        )

        assertEquals(3, grid.width)
        assertEquals(2, grid.height)
        assertEquals(1, grid[0][0])
        assertEquals(5, grid[1][1])
        assertEquals(6, grid[Point2(2, 1)])
        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5, 6)), grid.toRows())
    }

    @Test
    fun array2DMutatesWithXThenYAccess() {
        val grid = Array2D.filled(width = 3, height = 2, value = 0)

        grid[1][0] = 7
        grid[Point2(2, 1)] = 9

        assertEquals(7, grid[1, 0])
        assertEquals(9, grid[2][1])
        assertEquals(listOf(listOf(0, 7, 0), listOf(0, 0, 9)), grid.toRows())
    }

    @Test
    fun gridsReportContainment() {
        val grid = Array2D.generate(width = 2, height = 2) { x, y -> x + y }

        assertTrue(grid.contains(1, 1))
        assertFalse(grid.contains(2, 1))
        assertFalse(grid.contains(Point2(1, -1)))
    }

    @Test
    fun gridsRejectJaggedRows() {
        assertFailsWith<IllegalArgumentException> {
            List2D.fromRows(listOf(listOf(1), listOf(2, 3)))
        }
        assertFailsWith<IllegalArgumentException> {
            Array2D.fromRows(listOf(listOf(1), listOf(2, 3)))
        }
    }
}
