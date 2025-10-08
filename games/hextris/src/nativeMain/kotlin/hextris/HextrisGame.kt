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
    private var moveSpeed = 60L // milliseconds - matches web version's repeat rate
    private var initialMoveDelay = 110L // Short initial delay before repeat starts - matches web version

    // Movement state tracking like the web version
    private var lastMoveDirection: String? = null
    private var moveStartTime = 0L

    private var rotateTime = 0L
    private var rotateSpeed = 150L // milliseconds - matches web version
    private var timeSinceOptionChangeMs = 0L
    private val inputDelayMs = 20L // reduced from 50ms for more responsive input

    // Input responsiveness tracking
    private var inputLogCounter = 0
    private val inputLogInterval = 300 // Log every 300 frames (about 5 seconds at 60 FPS)
    private var totalInputLatency = 0L
    private var inputLatencyCount = 0
    private var maxInputLatency = 0L

    // UI
    private lateinit var blockSprites: SpriteSheet
    private lateinit var menuFont: Font
    private lateinit var scoreFont: Font

    // Layout - adjusted to avoid overlap between board and left column
    private val boardPosition = Vec2(360.0, 5.0) // Moved right to avoid overlap with left column
    private val nextPiecePosition = Vec2(16.0, 7.0) // Top left position
    private val histogramPosition = Vec2(560.0, 0.0) // Far right position

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
        // Increment input log counter
        inputLogCounter++

        // Log input responsiveness metrics periodically
        if (inputLogCounter >= inputLogInterval) {
            inputLogCounter = 0

            if (inputLatencyCount > 0) {
                val avgLatency = totalInputLatency / inputLatencyCount
                logger.info {
                    "Input Responsiveness Metrics - " +
                    "Average Latency: ${avgLatency}ms, " +
                    "Max Latency: ${maxInputLatency}ms, " +
                    "Samples: $inputLatencyCount"
                }

                // Reset metrics for next period
                totalInputLatency = 0L
                inputLatencyCount = 0
                maxInputLatency = 0L
            } else {
                logger.info { "Input Responsiveness Metrics - No input events recorded in this period" }
            }
        }

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

                // Store the current level before locking the piece
                val currentLevel = board.level

                // Lock the piece and check for completed lines
                board.lockPiece()

                // If the level has changed, update the drop speed
                if (board.level != currentLevel) {
                    logger.info("Level up! ${currentLevel} -> ${board.level}")
                    updateDropSpeed()
                }

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
            // Implement the web version's movement control logic with initial delay and repeat rate
            val currentTime = getCurrentMilliseconds()

            // Move left (Left Arrow or A) - with throttling
            val isLeftPressed = keyboard.isLeftPressed() || keyboard.isAPressed()
            if (isLeftPressed) {
                val shouldMove = if (lastMoveDirection == "left") {
                    // If continuing to press left, check if we've passed the initial delay
                    val timeSinceStart = timeSinceMs(moveStartTime)
                    if (timeSinceStart < initialMoveDelay) {
                        // Still in initial delay, don't move again
                        false
                    } else {
                        // Past initial delay, check repeat rate
                        val timeSinceLastMove = timeSinceMs(moveLeftTime)
                        timeSinceLastMove >= moveSpeed
                    }
                } else {
                    // First press or direction change, always move
                    lastMoveDirection = "left"
                    moveStartTime = currentTime
                    true
                }

                if (shouldMove) {
                    // Record start time for latency measurement
                    val actionStartTime = getCurrentMilliseconds()

                    moveLeftTime = currentTime
                    logger.debug("Moving left. Initial delay: $initialMoveDelay ms, Repeat rate: $moveSpeed ms")
                    board.moveLeft()

                    // Measure and record input latency
                    val latency = getCurrentMilliseconds() - actionStartTime
                    totalInputLatency += latency
                    inputLatencyCount++
                    if (latency > maxInputLatency) {
                        maxInputLatency = latency
                    }

                    if (latency > 5) { // Only log unusually high latencies
                        logger.debug("Input latency for left movement: ${latency}ms")
                    }
                }
            } else if (lastMoveDirection == "left") {
                // Released left key
                lastMoveDirection = null
            }

            // Move right (Right Arrow or D) - with throttling
            val isRightPressed = keyboard.isRightPressed() || keyboard.isDPressed()
            if (isRightPressed) {
                val shouldMove = if (lastMoveDirection == "right") {
                    // If continuing to press right, check if we've passed the initial delay
                    val timeSinceStart = timeSinceMs(moveStartTime)
                    if (timeSinceStart < initialMoveDelay) {
                        // Still in initial delay, don't move again
                        false
                    } else {
                        // Past initial delay, check repeat rate
                        val timeSinceLastMove = timeSinceMs(moveRightTime)
                        timeSinceLastMove >= moveSpeed
                    }
                } else {
                    // First press or direction change, always move
                    lastMoveDirection = "right"
                    moveStartTime = currentTime
                    true
                }

                if (shouldMove) {
                    // Record start time for latency measurement
                    val actionStartTime = getCurrentMilliseconds()

                    moveRightTime = currentTime
                    logger.debug("Moving right. Initial delay: $initialMoveDelay ms, Repeat rate: $moveSpeed ms")
                    board.moveRight()

                    // Measure and record input latency
                    val latency = getCurrentMilliseconds() - actionStartTime
                    totalInputLatency += latency
                    inputLatencyCount++
                    if (latency > maxInputLatency) {
                        maxInputLatency = latency
                    }

                    if (latency > 5) { // Only log unusually high latencies
                        logger.debug("Input latency for right movement: ${latency}ms")
                    }
                }
            } else if (lastMoveDirection == "right") {
                // Released right key
                lastMoveDirection = null
            }

            // Move down (soft drop) - track when down key is pressed/released (Down Arrow or S)
            val wasDownPressed = isDownKeyPressed
            val isDownPressed = keyboard.isDownPressed() || keyboard.isSPressed()
            isDownKeyPressed = isDownPressed

            // For down movement, we only set isDownKeyPressed = true
            // The actual dropping happens in the play() method with a faster drop speed
            if (isDownPressed) {
                // Set lastMoveDirection to "down" to track that down is being pressed
                if (lastMoveDirection != "down") {
                    lastMoveDirection = "down"
                    logger.debug("Down key pressed. Using fast drop speed.")
                }
            } else if (lastMoveDirection == "down") {
                // Released down key
                lastMoveDirection = null
                logger.debug("Down key released. Restoring normal drop speed.")
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

                // Store the current level before locking the piece
                val currentLevel = board.level

                // Lock the piece and check for completed lines
                board.lockPiece()

                // If the level has changed, update the drop speed
                if (board.level != currentLevel) {
                    logger.info("Level up! ${currentLevel} -> ${board.level}")
                    updateDropSpeed()
                }

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
            // Implement the web version's movement control logic with initial delay and repeat rate
            val currentTime = getCurrentMilliseconds()

            // Move left - Button 7 - with throttling
            val isLeftPressed = controller.isButtonPressed(7)
            if (isLeftPressed) {
                val shouldMove = if (lastMoveDirection == "controller_left") {
                    // If continuing to press left, check if we've passed the initial delay
                    val timeSinceStart = timeSinceMs(moveStartTime)
                    if (timeSinceStart < initialMoveDelay) {
                        // Still in initial delay, don't move again
                        false
                    } else {
                        // Past initial delay, check repeat rate
                        val timeSinceLastMove = timeSinceMs(moveLeftTime)
                        timeSinceLastMove >= moveSpeed
                    }
                } else {
                    // First press or direction change, always move
                    lastMoveDirection = "controller_left"
                    moveStartTime = currentTime
                    true
                }

                if (shouldMove) {
                    moveLeftTime = currentTime
                    logger.debug("Controller: Moving left. Initial delay: $initialMoveDelay ms, Repeat rate: $moveSpeed ms")
                    board.moveLeft()
                }
            } else if (lastMoveDirection == "controller_left") {
                // Released left button
                lastMoveDirection = null
            }

            // Move right - Button 8 - with throttling
            val isRightPressed = controller.isButtonPressed(8)
            if (isRightPressed) {
                val shouldMove = if (lastMoveDirection == "controller_right") {
                    // If continuing to press right, check if we've passed the initial delay
                    val timeSinceStart = timeSinceMs(moveStartTime)
                    if (timeSinceStart < initialMoveDelay) {
                        // Still in initial delay, don't move again
                        false
                    } else {
                        // Past initial delay, check repeat rate
                        val timeSinceLastMove = timeSinceMs(moveRightTime)
                        timeSinceLastMove >= moveSpeed
                    }
                } else {
                    // First press or direction change, always move
                    lastMoveDirection = "controller_right"
                    moveStartTime = currentTime
                    true
                }

                if (shouldMove) {
                    moveRightTime = currentTime
                    logger.debug("Controller: Moving right. Initial delay: $initialMoveDelay ms, Repeat rate: $moveSpeed ms")
                    board.moveRight()
                }
            } else if (lastMoveDirection == "controller_right") {
                // Released right button
                lastMoveDirection = null
            }

            // Move down (soft drop) - Button 6 (conflicts with START)
            val isControllerDownPressed = controller.isButtonPressed(6)

            // Update isDownKeyPressed if controller down is pressed (keyboard OR controller)
            if (isControllerDownPressed) {
                isDownKeyPressed = true
            }

            // For down movement, we only set isDownKeyPressed = true
            // The actual dropping happens in the play() method with a faster drop speed
            if (isControllerDownPressed) {
                // Set lastMoveDirection to "controller_down" to track that down is being pressed
                if (lastMoveDirection != "controller_down") {
                    lastMoveDirection = "controller_down"
                    logger.debug("Controller: Down button pressed. Using fast drop speed.")
                }
            } else if (lastMoveDirection == "controller_down") {
                // Released down button
                lastMoveDirection = null
                logger.debug("Controller: Down button released. Restoring normal drop speed.")
            }

            // Hard drop - Button 0 (A button)
            if (controller.isButtonPressed(0) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                val timeSinceLastAction = timeSinceMs(timeSinceOptionChangeMs)
                timeSinceOptionChangeMs = getCurrentMilliseconds()
                logger.debug("Controller: Hard drop. Time since last action: $timeSinceLastAction ms")
                board.drop()

                // Store the current level before locking the piece
                val currentLevel = board.level

                // Lock the piece and check for completed lines
                board.lockPiece()

                // If the level has changed, update the drop speed
                if (board.level != currentLevel) {
                    logger.info("Controller: Level up! ${currentLevel} -> ${board.level}")
                    updateDropSpeed()
                }

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
        val boardWidthPx = (board.width * Sprites.BLOCK_SIZE).toDouble()
        val boardHeightPx = (board.height * Sprites.BLOCK_SIZE).toDouble()

        useSDLContext {
            // Draw the board background with a semi-transparent effect like the web version
            useGeometryContext {
                // Draw outer border with glow effect
                fillRectangle(
                    boardX - boardWidthPx / 2.0 - 4,
                    boardY - 4,
                    boardWidthPx + 8,
                    boardHeightPx + 8,
                    Color(60u, 60u, 100u, 150u) // Bluish glow
                )

                // Draw inner background
                fillRectangle(
                    boardX - boardWidthPx / 2.0 - 1,
                    boardY - 1,
                    boardWidthPx + 2,
                    boardHeightPx + 2,
                    Color(0u, 0u, 0u, 230u) // Nearly opaque black
                )
            }

            // Draw a subtle grid for empty cells
            useGeometryContext {
                for (y in 0 until board.height) {
                    for (x in 0 until board.width) {
                        if (board.getColorAt(x, y) == null) {
                            // Draw grid cell
                            drawRectangle(
                                boardX - boardWidthPx / 2.0 + x * Sprites.BLOCK_SIZE + 1,
                                boardY + y * Sprites.BLOCK_SIZE + 1,
                                (Sprites.BLOCK_SIZE - 2).toDouble(),
                                (Sprites.BLOCK_SIZE - 2).toDouble(),
                                Color(30u, 30u, 30u, 100u) // Very dark gray, semi-transparent
                            )
                        }
                    }
                }
            }

            // Draw filled cells
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

            // Board title removed as per requirements
        }
    }

    private fun drawNextPiece() {
        useSDLContext {
            // Draw the next piece background with semi-transparent effect like the web version
            useGeometryContext {
                fillRectangle(
                    nextPiecePosition.x,
                    nextPiecePosition.y + 35,
                    (6 * Sprites.BLOCK_SIZE).toDouble(),
                    (6 * Sprites.BLOCK_SIZE).toDouble(),
                    Color(0u, 0u, 0u, 200u) // Semi-transparent black
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

                // Calculate position to center the piece in the display area and ensure grid alignment
                // First, calculate the grid cell position that would center the piece
                val gridCellX = (6 - width) / 2
                val gridCellY = (6 - height) / 2

                // Convert grid cell position to pixel coordinates, ensuring alignment with the grid
                val offsetX = (nextPiecePosition.x + gridCellX * Sprites.BLOCK_SIZE).toInt()
                val offsetY = (nextPiecePosition.y + 35 + gridCellY * Sprites.BLOCK_SIZE).toInt()

                // Draw a subtle grid background for the piece
                useGeometryContext {
                    for (x in 0 until 6) {
                        for (y in 0 until 6) {
                            drawRectangle(
                                nextPiecePosition.x + x * Sprites.BLOCK_SIZE + 1,
                                nextPiecePosition.y + 35 + y * Sprites.BLOCK_SIZE + 1,
                                (Sprites.BLOCK_SIZE - 2).toDouble(),
                                (Sprites.BLOCK_SIZE - 2).toDouble(),
                                Color(30u, 30u, 30u, 100u) // Very dark gray, semi-transparent
                            )
                        }
                    }
                }

                // Draw each block of the next piece
                for (block in blocks) {
                    drawBlock(
                        offsetX + (block.x - minX) * Sprites.BLOCK_SIZE,
                        offsetY + (block.y - minY) * Sprites.BLOCK_SIZE,
                        nextPiece.color
                    )
                }
            }

            // Draw the "NEXT PIECE" label with a more prominent style
            menuFont.drawText("NEXT", nextPiecePosition.x, nextPiecePosition.y,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
        }
    }

    private fun drawHistogram() {
        useSDLContext {
            // Draw the histogram background with a semi-transparent effect like the web version
            useGeometryContext {
                fillRectangle(
                    histogramPosition.x,
                    histogramPosition.y,
                    (14 * Sprites.BLOCK_SIZE).toDouble(),
                    (16 * Sprites.BLOCK_SIZE).toDouble(),
                    Color(0u, 0u, 0u, 200u) // Semi-transparent black
                )
            }

            // Get the piece counts, ensuring all piece types are included
            val pieceCounts = mutableMapOf<PieceType, Int>()

            // Initialize all piece types with 0 count to ensure all are displayed
            PieceType.values().forEach { pieceCounts[it] = 0 }

            // Update with actual counts
            pieceCounts.putAll(board.getPieceCounts())

            // Define a smaller block size for the histogram pieces
            val smallBlockSize = Sprites.BLOCK_SIZE / 2

            // Calculate the layout - improved spacing and alignment
            val pieceTypes = PieceType.values()
            val piecesPerColumn = (pieceTypes.size + 1) / 2 // Ceiling division
            val columnWidth = 7 * smallBlockSize
            val rowHeight = 40 // Increase row height for better spacing
            val padding = 5
            val horizontalGap = 3 * smallBlockSize // Gap between columns

            // Draw the pieces and their counts in two columns
            for (i in pieceTypes.indices) {
                val pieceType = pieceTypes[i]
                val count = pieceCounts[pieceType] ?: 0

                // Calculate position (left or right column)
                val column = i / piecesPerColumn
                val row = i % piecesPerColumn

                val x = histogramPosition.x.toInt() + column * (columnWidth + horizontalGap)
                val y = histogramPosition.y.toInt() + row * rowHeight + padding * 2

                // Draw the piece
                drawPieceSmall(x + padding, y + padding, pieceType)

                // Draw the count with "x<count>" format to the RIGHT of the piece
                scoreFont.drawText("x$count", x + padding * 13, y + padding,
                    r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
            }

            // Draw the "HISTOGRAM" label with a more prominent style
            menuFont.drawText("STATS", histogramPosition.x.toInt(), histogramPosition.y.toInt() - 40,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Total pieces count removed from histogram as per requirements
        }
    }

    /**
     * Draws a piece at a specific position with a smaller size.
     * Uses colored rectangles that match the sprite colors.
     */
    private fun drawPieceSmall(x: Int, y: Int, pieceType: PieceType) {
        // Create a temporary piece to get the blocks
        val tempPiece = Piece(pieceType, 0) // Color doesn't matter for getting blocks
        val blocks = tempPiece.getBlocks()

        // Calculate the bounds of the piece
        val minX = blocks.minOf { it.x }
        val maxX = blocks.maxOf { it.x }
        val minY = blocks.minOf { it.y }
        val maxY = blocks.maxOf { it.y }

        // Calculate the size of the piece
        val width = maxX - minX + 1
        val height = maxY - minY + 1

        // Define a smaller block size
        val smallBlockSize = Sprites.BLOCK_SIZE / 2

        // Calculate the offset to center the piece
        val offsetX = x + ((3 - width) * smallBlockSize) / 2
        val offsetY = y + ((3 - height) * smallBlockSize) / 2

        // Get the sprite position for this piece type
        val spritePos = Sprites.PIECE_SPRITES[pieceType] ?: Sprites.PIECE_SPRITES[PieceType.O]!!

        // Map sprite positions to colors
        val blockColor = when (spritePos) {
            0 to 0 -> Color.red       // Square (O)
            1 to 0 -> Color.orange    // L
            2 to 0 -> Color.yellow    // J
            3 to 0 -> Color.green     // I
            4 to 0 -> Color.blue      // S
            5 to 0 -> Color.purple    // Z
            0 to 1 -> Color.cyan      // T
            else -> Color(
                (spritePos.first * 40 + 50).toUByte(),
                (spritePos.second * 40 + 50).toUByte(),
                ((spritePos.first + spritePos.second) * 30 + 50).toUByte(),
                255u
            )
        }

        // Draw each block of the piece using colored rectangles
        useGeometryContext {
            for (block in blocks) {
                val blockX = offsetX + (block.x - minX) * smallBlockSize
                val blockY = offsetY + (block.y - minY) * smallBlockSize

                // Draw a rectangle with the appropriate color
                fillRectangle(
                    blockX.toDouble(),
                    blockY.toDouble(),
                    smallBlockSize.toDouble(),
                    smallBlockSize.toDouble(),
                    blockColor
                )
            }
        }
    }

    private fun drawStats() {
        useSDLContext {
            // Draw the score with larger font and highlight
            scoreFont.drawText("SCORE", nextPiecePosition.x, nextPiecePosition.y + 180,
                r = 0xCCu, g = 0xCCu, b = 0xFFu, a = 0xFFu)
            scoreFont.drawText("${board.score}", nextPiecePosition.x, nextPiecePosition.y + 205,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Draw the level
            scoreFont.drawText("LEVEL", nextPiecePosition.x, nextPiecePosition.y + 230,
                r = 0xCCu, g = 0xFFu, b = 0xCCu, a = 0xFFu)
            scoreFont.drawText("${board.level}", nextPiecePosition.x, nextPiecePosition.y + 255,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Draw the lines
            scoreFont.drawText("LINES", nextPiecePosition.x, nextPiecePosition.y + 280,
                r = 0xFFu, g = 0xCCu, b = 0xCCu, a = 0xFFu)
            scoreFont.drawText("${board.lines}", nextPiecePosition.x, nextPiecePosition.y + 305,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)

            // Draw the pieces count
            val totalPieces = board.getPieceCounts().values.sum()
            scoreFont.drawText("PIECES", nextPiecePosition.x, nextPiecePosition.y + 330,
                r = 0xFFu, g = 0xCCu, b = 0xFFu, a = 0xFFu)
            scoreFont.drawText("$totalPieces", nextPiecePosition.x, nextPiecePosition.y + 355,
                r = 0xFFu, g = 0xFFu, b = 0xFFu, a = 0xFFu)
        }
    }

    private fun drawBlock(x: Int, y: Int, color: Int) {
        useSDLContext {
            // Calculate the sprite position from the color index
            // color is now calculated as x + y * 6 in createRandomPiece
            val spriteX = color % 6
            val spriteY = color / 6

            // Get the sprite at the calculated position
            val sprite = blockSprites.getTile(spriteX, spriteY)
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
        lastMoveDirection = null
        moveStartTime = getCurrentMilliseconds()
        timeSinceOptionChangeMs = getCurrentMilliseconds()

        // Reset input tracking variables
        inputLogCounter = 0
        totalInputLatency = 0L
        inputLatencyCount = 0
        maxInputLatency = 0L

        // Log reset event
        logger.info("Game reset. Input tracking variables initialized.")

        // Adjust drop speed based on level
        updateDropSpeed()
    }

    /**
     * Updates the drop speed based on the current level.
     * As the level increases, the pieces fall faster.
     */
    private fun updateDropSpeed() {
        // Base drop speed is 300ms, decrease by 10ms per level, with a minimum of 50ms
        dropSpeed = (300 - (board.level * 10)).coerceAtLeast(50).toLong()
        logger.debug("Updated drop speed to $dropSpeed ms for level ${board.level}")
    }

    override fun cleanup() {
        // Nothing to clean up
    }
}
