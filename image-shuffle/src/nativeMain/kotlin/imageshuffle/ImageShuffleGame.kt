package imageshuffle

import com.kengine.Game
import com.kengine.GameContext
import com.kengine.context.useContext
import com.kengine.graphics.SpriteSheet
import com.kengine.graphics.useSpriteContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.timeSinceMs

class ImageShuffleGame : Game, Logging {
    private val spriteSheet by lazy { loadSprites() }
    private val tileSize = 200
    private val puzzleSize = 4
    private val tiles = Array(puzzleSize * puzzleSize) { it } // 0 to 8, where 0 is empty tile
    private var emptyTileIndex = 0
    private var lastMovedMs = 0L

    init {
//        shufflePuzzle()
    }

    private fun loadSprites(): SpriteSheet {
        return useSpriteContext {
            addSpriteSheet(
                "puzzle_sheet",
                SpriteSheet.fromFilePath("assets/sprites/colorful_robot.bmp", tileSize, tileSize)
            )
            getSpriteSheet("puzzle_sheet")
        }
    }

    private fun shufflePuzzle() {
        repeat(100) {
            val possibleMoves = getPossibleMoves()
            val randomMove = possibleMoves.random()
            logger.info { "$randomMove, $possibleMoves" }
            swapTiles(emptyTileIndex, randomMove)
        }
    }

    private fun getPossibleMoves(): List<Int> {
        val moves = mutableListOf<Int>()
        val emptyRow = emptyTileIndex / puzzleSize
        val emptyCol = emptyTileIndex % puzzleSize

        if (emptyRow > 0) moves.add(emptyTileIndex - puzzleSize)
        if (emptyRow < puzzleSize - 1) moves.add(emptyTileIndex + puzzleSize)
        if (emptyCol > 0) moves.add(emptyTileIndex - 1)
        if (emptyCol < puzzleSize - 1) moves.add(emptyTileIndex + 1)

        return moves
    }

    private fun swapTiles(from: Int, to: Int) {
        tiles[from] = tiles[to].also { tiles[to] = tiles[from] }
        emptyTileIndex = to
    }

    override fun update() {
        useKeyboardContext {
            if (keyboard.isEscapePressed()) {
                useContext<GameContext> {
                    isRunning = false
                }
            }

            val emptyRow = emptyTileIndex / puzzleSize
            val emptyCol = emptyTileIndex % puzzleSize

            if (timeSinceMs(lastMovedMs) > 150) {
                lastMovedMs = getClockContext().totalTimeMs
                if (keyboard.isUpPressed() && emptyRow < puzzleSize - 1)
                    swapTiles(emptyTileIndex, emptyTileIndex + puzzleSize)
                if (keyboard.isDownPressed() && emptyRow > 0)
                    swapTiles(emptyTileIndex, emptyTileIndex - puzzleSize)
                if (keyboard.isLeftPressed() && emptyCol < puzzleSize - 1)
                    swapTiles(emptyTileIndex, emptyTileIndex + 1)
                if (keyboard.isRightPressed() && emptyCol > 0)
                    swapTiles(emptyTileIndex, emptyTileIndex - 1)
                if (keyboard.isRPressed()) shufflePuzzle()
            }
        }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(32u, 32u, 32u)
            for (i in tiles.indices) {
                if (tiles[i] == 0) continue // skip empty tile

                val tileNum = tiles[i] - 1 // -1 because tile 0 is empty
                val srcX = (tileNum + puzzleSize + 1) % puzzleSize
                val srcY = tileNum / puzzleSize

                val destX = (i % puzzleSize) * tileSize
                val destY = (i / puzzleSize) * tileSize

                spriteSheet.getTile(srcX, srcY).draw(destX.toDouble(), destY.toDouble())
            }
            flipScreen()
        }
    }

    private fun isComplete(): Boolean {
        return tiles.mapIndexed { index, value ->
            if (index == tiles.lastIndex) value == 0
            else value == index + 1
        }.all { it }
    }

    override fun cleanup() {
        spriteSheet.cleanup()
    }
}