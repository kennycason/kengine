package hextris

import com.kengine.Game
import com.kengine.font.Font
import com.kengine.font.useFontContext
import com.kengine.graphics.Color
import com.kengine.graphics.SpriteSheet
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.geometry.useGeometryContext
import com.kengine.math.Vec2
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.timeSinceMs

/**
 * Main Hextris game class.
 */
class HextrisGame : Game, Logging {
    enum class State {
        INIT, PLAY, GAME_OVER
    }

    // Game state
    private var state = State.INIT
    private var board = Board()
    private var dropTime = 0L
    private var dropSpeed = 300L // milliseconds (reduced from 500ms for faster gameplay)
    private var fastDropSpeed = 50L // milliseconds (reduced from 100ms for faster dropping)
    private var isDownKeyPressed = false // Track if down key is pressed

    // Separate timers for each movement direction for more responsive controls
    private var moveLeftTime = 0L
    private var moveRightTime = 0L
    private var moveDownTime = 0L
    private var moveSpeed = 0L // milliseconds (set to 0 to eliminate throttling)

    private var rotateTime = 0L
    private var rotateSpeed = 50L // milliseconds (reduced from 150ms for more responsive rotation)
    private var timeSinceOptionChangeMs = 0L
    private val inputDelayMs = 20L // reduced from 50ms for more responsive input

    // UI
    private lateinit var blockSprites: SpriteSheet
    private lateinit var menuFont: Font
    private lateinit var scoreFont: Font

    // Layout
    private val boardPosition = Vec2(340.0, 0.0)
    private val nextPiecePosition = Vec2(0.0,  0.0)
    private val histogramPosition = Vec2(530.0, 0.0)

    init {
        // Set log level to DEBUG to see debug messages
        Logger.setLevel(Logger.Level.DEBUG)
        logger.info("HextrisGame initialized. Log level set to DEBUG.")

        // Initialize fonts
        useFontContext {
            addFont(Fonts.ARCADE_CLASSIC, Fonts.ARCADE_CLASSIC_TTF, fontSize = 32f)
            addFont(Fonts.ARCADE_CLASSIC, Fonts.ARCADE_CLASSIC_TTF, fontSize = 16f)
            menuFont = getFont(Fonts.ARCADE_CLASSIC, 32f)
            scoreFont = getFont(Fonts.ARCADE_CLASSIC, 32f)
        }

        // Initialize sprites
        useSDLContext {
            blockSprites = SpriteSheet.fromFilePath(Sprites.BLOCK_SPRITES, Sprites.BLOCK_SIZE, Sprites.BLOCK_SIZE)
        }

        // Start the game
        reset()
    }

    override fun update() {
        when (state) {
            State.INIT -> init()
            State.PLAY -> play()
            State.GAME_OVER -> handleGameOver()
        }
    }

    override fun draw() {
        useSDLContext {
            // Clear the screen
            fillScreen(Color.black)

            // Draw the board
            drawBoard()

            // Draw the next piece
            drawNextPiece()

            // Draw the histogram
            drawHistogram()

            // Draw the score, level, and lines
            drawStats()

            // Draw game over message if needed
            if (state == State.GAME_OVER) {
                menuFont.drawText("GAME OVER", 300, 300, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
                menuFont.drawText("PRESS SPACE TO RESTART", 200, 350, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
            }

            // Flip the screen
            flipScreen()
        }
    }

    private fun init() {
        state = State.PLAY
    }

    private fun play() {
        // Handle keyboard input
        handleKeyboardInput()

        // Handle controller input
        handleControllerInput()

        // Handle automatic dropping - use fastDropSpeed if down key is pressed
        val currentDropSpeed = if (isDownKeyPressed) fastDropSpeed else dropSpeed
        if (timeSinceMs(dropTime) > currentDropSpeed) {
            dropTime = getCurrentMilliseconds()
            logger.debug("Auto dropping piece. Current drop speed: $currentDropSpeed ms")

            if (!board.moveDown()) {
                // If the piece can't move down, lock it in place
                logger.debug("Piece can't move down, locking in place")
                board.lockPiece()

                // Check if the game is over
                if (board.gameOver) {
                    logger.info("Game over")
                    state = State.GAME_OVER
                }
            }
        }
    }

    private fun handleGameOver() {
        // Handle keyboard input for restarting
        useKeyboardContext {
            if (keyboard.isSpacePressed() && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                reset()
            }
        }

        // Handle controller input for restarting
        useControllerContext {
            if (controller.isButtonPressed(Buttons.A) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                reset()
            }
        }
    }

    private fun handleKeyboardInput() {
        useKeyboardContext {
            // Move left (Left Arrow or A) - no throttling
            if (keyboard.isLeftPressed() || keyboard.isAPressed()) {
                val timeSinceLastMove = timeSinceMs(moveLeftTime)
                moveLeftTime = getCurrentMilliseconds()
                logger.debug("Moving left. Time since last move: $timeSinceLastMove ms")
                board.moveLeft()
            }

            // Move right (Right Arrow or D) - no throttling
            if (keyboard.isRightPressed() || keyboard.isDPressed()) {
                val timeSinceLastMove = timeSinceMs(moveRightTime)
                moveRightTime = getCurrentMilliseconds()
                logger.debug("Moving right. Time since last move: $timeSinceLastMove ms")
                board.moveRight()
            }

            // Move down (soft drop) - track when down key is pressed/released (Down Arrow or S)
            val wasDownPressed = isDownKeyPressed
            isDownKeyPressed = keyboard.isDownPressed() || keyboard.isSPressed()

            // Only move down manually if the key is pressed - no throttling
            if (isDownKeyPressed) {
                val timeSinceLastMove = timeSinceMs(moveDownTime)
                moveDownTime = getCurrentMilliseconds()
                logger.debug("Moving down (soft drop). Time since last move: $timeSinceLastMove ms")
                board.moveDown()
            }

            // Move up (W key) - not used for gameplay but included for WASD completeness
            if (keyboard.isWPressed()) {
                // No action for up movement in Tetris-like games
            }

            // Hard drop (Space)
            if (keyboard.isSpacePressed() && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                val timeSinceLastAction = timeSinceMs(timeSinceOptionChangeMs)
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                logger.debug("Hard drop. Time since last action: $timeSinceLastAction ms (inputDelayMs: $inputDelayMs ms)")
                board.drop()
                board.lockPiece()

                // Check if the game is over
                if (board.gameOver) {
                    logger.info("Game over after hard drop")
                    state = State.GAME_OVER
                }
            }

            // Rotate clockwise (Up Arrow or L)
            if ((keyboard.isUpPressed() || keyboard.isLPressed()) && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Rotating clockwise. Time since last rotate: $timeSinceLastRotate ms (rotateSpeed: $rotateSpeed ms)")
                board.rotateClockwise()
            }

            // Rotate counter-clockwise (Z or J)
            if ((keyboard.isZPressed() || keyboard.isJPressed()) && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Rotating counter-clockwise. Time since last rotate: $timeSinceLastRotate ms (rotateSpeed: $rotateSpeed ms)")
                board.rotateCounterClockwise()
            }

            // 180-degree rotation (K)
            if (keyboard.isKPressed() && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Rotating 180 degrees. Time since last rotate: $timeSinceLastRotate ms (rotateSpeed: $rotateSpeed ms)")
                // Rotate twice for 180 degrees
                board.rotateClockwise()
                board.rotateClockwise()
            }

            // Reset game (R)
            if (keyboard.isRPressed() && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                reset()
            }
        }
    }

    private fun handleControllerInput() {
        useControllerContext {
            // Move left - Button 7 - no throttling
            if (controller.isButtonPressed(7)) {
                val timeSinceLastMove = timeSinceMs(moveLeftTime)
                moveLeftTime = getCurrentMilliseconds()
                logger.debug("Controller: Moving left. Time since last move: $timeSinceLastMove ms")
                board.moveLeft()
            }

            // Move right - Button 8 - no throttling
            if (controller.isButtonPressed(8)) {
                val timeSinceLastMove = timeSinceMs(moveRightTime)
                moveRightTime = getCurrentMilliseconds()
                logger.debug("Controller: Moving right. Time since last move: $timeSinceLastMove ms")
                board.moveRight()
            }

            // Move down (soft drop) - Button 6 (conflicts with START)
            val wasDownPressed = isDownKeyPressed
            val isControllerDownPressed = controller.isButtonPressed(6)

            // Update isDownKeyPressed if controller down is pressed (keyboard OR controller)
            if (isControllerDownPressed) {
                isDownKeyPressed = true
            }

            // Only move down manually if the button is pressed - no throttling
            if (isControllerDownPressed) {
                val timeSinceLastMove = timeSinceMs(moveDownTime)
                moveDownTime = getCurrentMilliseconds()
                logger.debug("Controller: Moving down (soft drop). Time since last move: $timeSinceLastMove ms")
                board.moveDown()
            }

            // Hard drop - Button 0 (A button)
            if (controller.isButtonPressed(0) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                val timeSinceLastAction = timeSinceMs(timeSinceOptionChangeMs)
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                logger.debug("Controller: Hard drop. Time since last action: $timeSinceLastAction ms")
                board.drop()
                board.lockPiece()

                // Check if the game is over
                if (board.gameOver) {
                    logger.info("Controller: Game over after hard drop")
                    state = State.GAME_OVER
                }
            }

            // Rotate clockwise - Button 9 (UP or L button)
            if (controller.isButtonPressed(9) && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Controller: Rotating clockwise. Time since last rotate: $timeSinceLastRotate ms")
                board.rotateClockwise()
            }

            // Rotate counter-clockwise - Button 1 (B button)
            if (controller.isButtonPressed(1) && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Controller: Rotating counter-clockwise. Time since last rotate: $timeSinceLastRotate ms")
                board.rotateCounterClockwise()
            }

            // 180-degree rotation - Button 3 (X button)
            if (controller.isButtonPressed(3) && timeSinceMs(rotateTime) > rotateSpeed) {
                val timeSinceLastRotate = timeSinceMs(rotateTime)
                rotateTime = getCurrentMilliseconds()
                logger.debug("Controller: Rotating 180 degrees. Time since last rotate: $timeSinceLastRotate ms")
                // Rotate twice for 180 degrees
                board.rotateClockwise()
                board.rotateClockwise()
            }

            // Reset game - Button 2 (Y button) or Button 10 (R button)
            if ((controller.isButtonPressed(2) || controller.isButtonPressed(10)) &&
                timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                logger.debug("Controller: Resetting game")
                reset()
            }

            // Alternative reset: SELECT (4) + START (6) combination
            if (controller.isButtonPressed(4) && controller.isButtonPressed(6) &&
                timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                logger.debug("Controller: Resetting game (SELECT+START)")
                reset()
            }
        }
    }

    private fun drawBoard() {
        val boardX = boardPosition.x
        val boardY = boardPosition.y
        useSDLContext {
            // Draw the board background
            useGeometryContext {
                drawRectangle(
                    boardX - (board.width * Sprites.BLOCK_SIZE) / 2.0 - 1,
                    boardY - 1,
                    (board.width * Sprites.BLOCK_SIZE).toDouble() + 2,
                    (board.height * Sprites.BLOCK_SIZE).toDouble() + 2,
                    Color.white
                )
            }

            // Draw the grid
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val color = board.getColorAt(x, y)
                    if (color != null) {
                        drawBlock(
                            boardX.toInt() - (board.width * Sprites.BLOCK_SIZE) / 2 + x * Sprites.BLOCK_SIZE,
                            boardY.toInt() + y * Sprites.BLOCK_SIZE,
                            color
                        )
                    }
                }
            }

            // Draw the current piece
            val currentPiece = board.getCurrentPiece()
            if (currentPiece != null) {
                val position = board.getCurrentPiecePosition()
                for (block in currentPiece.getBlocks()) {
                    val x = position.x + block.x
                    val y = position.y + block.y

                    // Only draw blocks that are on the board
                    if (y >= 0 && y < board.height && x >= 0 && x < board.width) {
                        drawBlock(
                            boardX.toInt() - (board.width * Sprites.BLOCK_SIZE) / 2 + x * Sprites.BLOCK_SIZE,
                            boardY.toInt() + y * Sprites.BLOCK_SIZE,
                            currentPiece.color
                        )
                    }
                }
            }
        }
    }

    private fun drawNextPiece() {
        useSDLContext {
            // Draw the next piece background
            useGeometryContext {
                drawRectangle(
                    nextPiecePosition.x + 10,
                    nextPiecePosition.y + 30,
                    (6 * Sprites.BLOCK_SIZE).toDouble(),
                    (7 * Sprites.BLOCK_SIZE).toDouble(),
                    Color.white
                )
            }

            // Draw the next piece
            val nextPiece = board.getNextPiece()
            if (nextPiece != null) {
                // Center the piece in the next piece area
                val blocks = nextPiece.getBlocks()
                val minX = blocks.minOf { it.x }
                val maxX = blocks.maxOf { it.x }
                val minY = blocks.minOf { it.y }
                val maxY = blocks.maxOf { it.y }
                val width = maxX - minX + 1
                val height = maxY - minY + 1

                val offsetX = (nextPiecePosition.x + (6 * Sprites.BLOCK_SIZE - width * Sprites.BLOCK_SIZE) / 2).toInt()
                val offsetY = (nextPiecePosition.y + (6 * Sprites.BLOCK_SIZE - height * Sprites.BLOCK_SIZE) / 2).toInt()

                for (block in blocks) {
                    drawBlock(
                        offsetX + (block.x - minX) * Sprites.BLOCK_SIZE + 10,
                        offsetY + (block.y - minY) * Sprites.BLOCK_SIZE + 40,
                        nextPiece.color
                    )
                }
            }

            // Draw the "NEXT" label
            menuFont.drawText("NEXT", nextPiecePosition.x + 10, nextPiecePosition.y, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
        }
    }

    private fun drawHistogram() {
        useSDLContext {
            // Draw the histogram background
            useGeometryContext {
                drawRectangle(
                    histogramPosition.x,
                    histogramPosition.y,
                    (8 * Sprites.BLOCK_SIZE).toDouble(),
                    (board.height * Sprites.BLOCK_SIZE).toDouble(),
                    Color.white
                )
            }

            // Draw the histogram
            val theHistogram = board.getHistogram()
            val colorMap = board.getHistogramColors() // Get colors for each column

            for (x in 0 until board.width) {
                val height = theHistogram[x]
                for (y in 0 until height) {
                    // Get the color for this column (or use a default if not available)
                    val color = colorMap[x] ?: 0

                    // Draw a colored block for each cell in the histogram
                    drawBlock(
                        histogramPosition.x.toInt() + x * Sprites.BLOCK_SIZE,
                        histogramPosition.y.toInt() + (board.height - y - 1) * Sprites.BLOCK_SIZE,
                        color
                    )
                }
            }

            // Draw the "HISTOGRAM" label
            menuFont.drawText("HISTOGRAM", histogramPosition.x.toInt(), histogramPosition.y.toInt() - 40, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
        }
    }

    private fun drawStats() {
        useSDLContext {
            // Draw the score
            scoreFont.drawText("SCORE ${board.score}", nextPiecePosition.x + 10, nextPiecePosition.y + 200, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Draw the level
            scoreFont.drawText("LEVEL ${board.level}", nextPiecePosition.x + 10, nextPiecePosition.y + 230, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Draw the lines
            scoreFont.drawText("LINES ${board.lines}", nextPiecePosition.x + 10, nextPiecePosition.y + 260, r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
        }
    }

    private fun drawBlock(x: Int, y: Int, color: Int) {
        useSDLContext {
            val sprite = blockSprites.getTile(Sprites.BLOCK_COLORS[color].first, Sprites.BLOCK_COLORS[color].second)
            sprite.draw(x.toDouble(), y.toDouble())
        }
    }

    private fun reset() {
        board.reset()
        state = State.INIT
        dropTime = getCurrentMilliseconds()
        moveLeftTime = getCurrentMilliseconds()
        moveRightTime = getCurrentMilliseconds()
        moveDownTime = getCurrentMilliseconds()
        rotateTime = getCurrentMilliseconds()
    }

    override fun cleanup() {
        // Nothing to clean up
    }
}
