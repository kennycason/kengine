package hextris

import com.kengine.math.IntVec2

/**
 * Represents the Hextris game board.
 */
class Board(val width: Int = 15, val height: Int = 25) {
    // The board grid: null means empty, Int means filled with a color
    private val grid = Array(height) { Array<Int?>(width) { null } }

    // Current piece position
    private var pieceX = 0
    private var pieceY = 0

    // Current piece
    private var currentPiece: Piece? = null

    // Next piece
    private var nextPiece: Piece? = null

    // Game statistics
    var score = 0
        private set
    var lines = 0
        private set
    var level = 0
        private set

    // Game state
    var gameOver = false
        private set

    init {
        // Initialize the next piece
        spawnNextPiece()
    }

    /**
     * Spawns a new piece at the top of the board.
     */
    fun spawnPiece() {
        if (nextPiece == null) {
            spawnNextPiece()
        }

        currentPiece = nextPiece
        nextPiece = createRandomPiece()

        // Center the piece horizontally
        pieceX = width / 2 - 1
        pieceY = 0

        // Check if the new piece can be placed
        if (!canPlacePiece(currentPiece!!, pieceX, pieceY)) {
            gameOver = true
        }
    }

    /**
     * Creates the next piece.
     */
    private fun spawnNextPiece() {
        nextPiece = createRandomPiece()
    }

    /**
     * Creates a random piece.
     * Each piece type has a fixed color for consistency.
     */
    private fun createRandomPiece(): Piece {
        val type = PieceType.random()
        // Assign color based on piece type (consistent color per piece type)
        val color = type.ordinal % Sprites.BLOCK_COLORS.size
        return Piece(type, color)
    }

    /**
     * Moves the current piece left if possible.
     * @return true if the piece was moved, false otherwise
     */
    fun moveLeft(): Boolean {
        val piece = currentPiece ?: return false
        if (canPlacePiece(piece, pieceX - 1, pieceY)) {
            pieceX--
            return true
        }
        return false
    }

    /**
     * Moves the current piece right if possible.
     * @return true if the piece was moved, false otherwise
     */
    fun moveRight(): Boolean {
        val piece = currentPiece ?: return false
        if (canPlacePiece(piece, pieceX + 1, pieceY)) {
            pieceX++
            return true
        }
        return false
    }

    /**
     * Moves the current piece down if possible.
     * @return true if the piece was moved, false otherwise
     */
    fun moveDown(): Boolean {
        val piece = currentPiece ?: return false
        if (canPlacePiece(piece, pieceX, pieceY + 1)) {
            pieceY++
            return true
        }
        return false
    }

    /**
     * Drops the current piece to the bottom.
     * @return the number of cells the piece dropped
     */
    fun drop(): Int {
        val piece = currentPiece ?: return 0
        var dropDistance = 0

        while (canPlacePiece(piece, pieceX, pieceY + 1)) {
            pieceY++
            dropDistance++
        }

        return dropDistance
    }

    /**
     * Rotates the current piece clockwise if possible.
     * @return true if the piece was rotated, false otherwise
     */
    fun rotateClockwise(): Boolean {
        val piece = currentPiece ?: return false

        // Try to rotate
        piece.rotateClockwise()

        // Check if the rotated piece can be placed
        if (canPlacePiece(piece, pieceX, pieceY)) {
            return true
        }

        // If not, try wall kicks
        for (offset in listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1),  // Left, Right, Up
            Pair(-2, 0), Pair(2, 0),               // Far Left, Far Right
            Pair(-1, -1), Pair(1, -1)              // Left-Up, Right-Up
        )) {
            if (canPlacePiece(piece, pieceX + offset.first, pieceY + offset.second)) {
                pieceX += offset.first
                pieceY += offset.second
                return true
            }
        }

        // If all wall kicks fail, revert the rotation
        piece.rotateCounterClockwise()
        return false
    }

    /**
     * Rotates the current piece counter-clockwise if possible.
     * @return true if the piece was rotated, false otherwise
     */
    fun rotateCounterClockwise(): Boolean {
        val piece = currentPiece ?: return false

        // Try to rotate
        piece.rotateCounterClockwise()

        // Check if the rotated piece can be placed
        if (canPlacePiece(piece, pieceX, pieceY)) {
            return true
        }

        // If not, try wall kicks
        for (offset in listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1),  // Left, Right, Up
            Pair(-2, 0), Pair(2, 0),               // Far Left, Far Right
            Pair(-1, -1), Pair(1, -1)              // Left-Up, Right-Up
        )) {
            if (canPlacePiece(piece, pieceX + offset.first, pieceY + offset.second)) {
                pieceX += offset.first
                pieceY += offset.second
                return true
            }
        }

        // If all wall kicks fail, revert the rotation
        piece.rotateClockwise()
        return false
    }

    /**
     * Locks the current piece in place and checks for completed lines.
     * @return the number of lines cleared
     */
    fun lockPiece(): Int {
        val piece = currentPiece ?: return 0

        // Place the piece on the grid
        for (block in piece.getBlocks()) {
            val x = pieceX + block.x
            val y = pieceY + block.y

            if (y >= 0 && y < height && x >= 0 && x < width) {
                grid[y][x] = piece.color
            }
        }

        // Check for completed lines
        val completedLines = mutableListOf<Int>()
        for (y in 0 until height) {
            if (grid[y].all { it != null }) {
                completedLines.add(y)
            }
        }

        // Remove completed lines
        for (y in completedLines) {
            for (yy in y downTo 1) {
                grid[yy] = grid[yy - 1].copyOf()
            }
            grid[0] = Array(width) { null }
        }

        // Update score and level
        val linesCleared = completedLines.size
        if (linesCleared > 0) {
            // Score calculation: more points for more lines at once
            val linePoints = when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 1000
            }

            score += linePoints * (level + 1)
            lines += linesCleared
            level = lines / 10
        }

        // Spawn a new piece
        spawnPiece()

        return linesCleared
    }

    /**
     * Checks if the given piece can be placed at the given position.
     */
    fun canPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        for (block in piece.getBlocks()) {
            val blockX = x + block.x
            val blockY = y + block.y

            // Check if the block is out of bounds
            if (blockX < 0 || blockX >= width || blockY >= height) {
                return false
            }

            // Allow blocks above the top of the board
            if (blockY < 0) {
                continue
            }

            // Check if the block collides with another block
            if (grid[blockY][blockX] != null) {
                return false
            }
        }

        return true
    }

    /**
     * Gets the current piece.
     */
    fun getCurrentPiece(): Piece? {
        return currentPiece
    }

    /**
     * Gets the next piece.
     */
    fun getNextPiece(): Piece? {
        return nextPiece
    }

    /**
     * Gets the current piece position.
     */
    fun getCurrentPiecePosition(): IntVec2 {
        return IntVec2(pieceX, pieceY)
    }

    /**
     * Gets the color at the given position, or null if the position is empty.
     */
    fun getColorAt(x: Int, y: Int): Int? {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null
        }
        return grid[y][x]
    }

    /**
     * Gets the histogram of blocks in each column.
     * @return an array of integers representing the height of each column
     */
    fun getHistogram(): IntArray {
        val histogram = IntArray(width) { 0 }

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (grid[y][x] != null) {
                    histogram[x]++
                }
            }
        }

        return histogram
    }

    /**
     * Gets the colors of blocks in each column for the histogram.
     * @return a map of column indices to colors
     */
    fun getHistogramColors(): Map<Int, Int> {
        val colorMap = mutableMapOf<Int, Int>()

        // For each column, find the topmost block and use its color
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = grid[y][x]
                if (color != null) {
                    colorMap[x] = color
                    break  // Use the first (topmost) color found in this column
                }
            }
        }

        return colorMap
    }

    /**
     * Resets the game.
     */
    fun reset() {
        // Clear the grid
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = null
            }
        }

        // Reset game state
        score = 0
        lines = 0
        level = 0
        gameOver = false

        // Spawn a new piece
        spawnNextPiece()
        spawnPiece()
    }
}
