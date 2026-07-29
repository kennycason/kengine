package hextris

class Board(val width: Int = 15, val height: Int = 25) {
    private val grid = Array(height) { arrayOfNulls<Int>(width) }
    private val pieceCounter = IntArray(PieceType.entries.size)

    private var pieceX = 0
    private var pieceY = 0
    private var nextSeed = 0x12345
    private var currentPiece: Piece? = null
    private var nextPiece: Piece? = null

    var score = 0
        private set
    var lines = 0
        private set
    var level = 0
        private set
    var gameOver = false
        private set

    init {
        reset()
    }

    fun moveLeft(): Boolean {
        val piece = currentPiece ?: return false
        if (!canPlacePiece(piece, pieceX - 1, pieceY)) return false
        pieceX -= 1
        return true
    }

    fun moveRight(): Boolean {
        val piece = currentPiece ?: return false
        if (!canPlacePiece(piece, pieceX + 1, pieceY)) return false
        pieceX += 1
        return true
    }

    fun moveDown(): Boolean {
        val piece = currentPiece ?: return false
        if (!canPlacePiece(piece, pieceX, pieceY + 1)) return false
        pieceY += 1
        return true
    }

    fun drop(): Int {
        val piece = currentPiece ?: return 0
        var distance = 0
        while (canPlacePiece(piece, pieceX, pieceY + 1)) {
            pieceY += 1
            distance += 1
        }
        return distance
    }

    fun currentDropDistance(): Int {
        val piece = currentPiece ?: return 0
        var distance = 0
        while (canPlacePiece(piece, pieceX, pieceY + distance + 1)) {
            distance += 1
        }
        return distance
    }

    fun rotateClockwise(): Boolean {
        val piece = currentPiece ?: return false
        piece.rotateClockwise()
        if (placeWithKick(piece)) return true
        piece.rotateCounterClockwise()
        return false
    }

    fun rotateCounterClockwise(): Boolean {
        val piece = currentPiece ?: return false
        piece.rotateCounterClockwise()
        if (placeWithKick(piece)) return true
        piece.rotateClockwise()
        return false
    }

    fun rotate180(): Boolean {
        val piece = currentPiece ?: return false
        piece.rotateClockwise()
        piece.rotateClockwise()
        if (placeWithKick(piece)) return true
        piece.rotateClockwise()
        piece.rotateClockwise()
        return false
    }

    fun lockPiece(): Int {
        val piece = currentPiece ?: return 0
        pieceCounter[piece.type.ordinal] += 1

        for (block in piece.getBlocks()) {
            val x = pieceX + block.x
            val y = pieceY + block.y
            if (x in 0 until width && y in 0 until height) {
                grid[y][x] = piece.color
            }
        }

        val completed = mutableListOf<Int>()
        for (y in 0 until height) {
            if (grid[y].all { it != null }) {
                completed += y
            }
        }

        for (line in completed) {
            for (y in line downTo 1) {
                grid[y] = grid[y - 1].copyOf()
            }
            grid[0] = arrayOfNulls(width)
        }

        val cleared = completed.size
        if (cleared > 0) {
            score += lineScore(cleared) * (level + 1)
            lines += cleared
            level = lines / 10
        }

        spawnPiece()
        return cleared
    }

    fun reset() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = null
            }
        }

        pieceCounter.fill(0)
        score = 0
        lines = 0
        level = 0
        gameOver = false
        nextSeed = 0x12345
        nextPiece = createPiece()
        spawnPiece()
    }

    fun canPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        for (block in piece.getBlocks()) {
            val blockX = x + block.x
            val blockY = y + block.y

            if (blockX < 0 || blockX >= width || blockY >= height) {
                return false
            }
            if (blockY < 0) {
                continue
            }
            if (grid[blockY][blockX] != null) {
                return false
            }
        }
        return true
    }

    fun getCurrentPiece(): Piece? = currentPiece

    fun getNextPiece(): Piece? = nextPiece

    fun getCurrentPiecePosition(): GridPoint = GridPoint(pieceX, pieceY)

    fun getColorAt(x: Int, y: Int): Int? {
        if (x !in 0 until width || y !in 0 until height) {
            return null
        }
        return grid[y][x]
    }

    fun getPieceCounts(): Map<PieceType, Int> {
        return PieceType.entries.associateWith { pieceCounter[it.ordinal] }
    }

    fun totalPieces(): Int = pieceCounter.sum()

    private fun spawnPiece() {
        currentPiece = nextPiece
        nextPiece = createPiece()
        pieceX = width / 2 - 1
        pieceY = 0

        val piece = currentPiece
        if (piece != null && !canPlacePiece(piece, pieceX, pieceY)) {
            gameOver = true
        }
    }

    private fun createPiece(): Piece {
        nextSeed = nextSeed * 1_103_515_245 + 12_345
        val type = PieceType.fromSeed(nextSeed)
        return Piece(type, Sprites.colorIndex(type))
    }

    private fun placeWithKick(piece: Piece): Boolean {
        for (offset in SLIDE_ATTEMPTS) {
            val nextX = pieceX + offset.first
            val nextY = pieceY + offset.second
            if (canPlacePiece(piece, nextX, nextY)) {
                pieceX = nextX
                pieceY = nextY
                return true
            }
        }
        return false
    }

    private fun lineScore(cleared: Int): Int {
        return when (cleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 1000
        }
    }

    companion object {
        private val SLIDE_ATTEMPTS = listOf(
            0 to 0,
            -1 to 0,
            1 to 0,
            -2 to 0,
            2 to 0,
            0 to -1,
            -1 to -1,
            1 to -1,
            -3 to 0,
            3 to 0,
            0 to -2,
            -2 to -1,
            2 to -1,
            -1 to -2,
            1 to -2
        )
    }
}
