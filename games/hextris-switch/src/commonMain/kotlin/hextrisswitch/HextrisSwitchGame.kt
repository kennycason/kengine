package hextrisswitch

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext

class HextrisSwitchGame : PortableGame {
    private val board = Board()

    private var frame = 0
    private var previousMask = 0
    private var fallCounter = 0
    private var leftHold = 0
    private var rightHold = 0
    private var clockwiseHold = 0
    private var counterClockwiseHold = 0
    private var state = PlayState.RUNNING
    private val pendingSoundIds = IntArray(8)
    private var pendingSoundCount = 0

    override fun update(input: InputState) {
        frame += 1

        if (justPressed(input, InputButton.SELECT)) {
            reset()
            queueSound(Sounds.RESET)
            previousMask = input.mask
            return
        }

        if (justPressed(input, InputButton.START)) {
            val previousState = state
            state = when (state) {
                PlayState.RUNNING -> PlayState.PAUSED
                PlayState.PAUSED -> PlayState.RUNNING
                PlayState.GAME_OVER -> PlayState.RUNNING.also { reset() }
            }
            queueSound(if (previousState == PlayState.GAME_OVER) Sounds.RESET else Sounds.PAUSE)
            previousMask = input.mask
            return
        }

        if (state != PlayState.RUNNING) {
            previousMask = input.mask
            return
        }

        leftHold = nextHold(input.isPressed(InputButton.LEFT), leftHold)
        rightHold = nextHold(input.isPressed(InputButton.RIGHT), rightHold)

        if (shouldRepeat(leftHold)) {
            board.moveLeft()
        }
        if (shouldRepeat(rightHold)) {
            board.moveRight()
        }

        val clockwisePressed = input.isPressed(InputButton.A) || input.isPressed(InputButton.R)
        val counterClockwisePressed = input.isPressed(InputButton.B) ||
            input.isPressed(InputButton.L) ||
            input.isPressed(InputButton.Y)
        clockwiseHold = nextHold(clockwisePressed, clockwiseHold)
        counterClockwiseHold = nextHold(counterClockwisePressed, counterClockwiseHold)

        when {
            justPressed(input, InputButton.X) ||
                (justPressed(input, InputButton.L) && input.isPressed(InputButton.R)) ||
                (justPressed(input, InputButton.R) && input.isPressed(InputButton.L)) -> {
                if (board.rotate180()) queueSound(Sounds.ROTATE)
            }
            shouldRepeat(counterClockwiseHold, firstDelay = 14, repeatRate = 8) -> {
                if (board.rotateCounterClockwise()) queueSound(Sounds.ROTATE)
            }
            shouldRepeat(clockwiseHold, firstDelay = 14, repeatRate = 8) -> {
                if (board.rotateClockwise()) queueSound(Sounds.ROTATE)
            }
        }

        if (justPressed(input, InputButton.UP)) {
            if (board.drop() > 0) {
                queueSound(Sounds.HARD_DROP)
            }
            lockCurrentPiece()
            previousMask = input.mask
            return
        }

        val fallPeriod = if (input.isPressed(InputButton.DOWN)) {
            2
        } else {
            normalFallPeriod()
        }
        fallCounter += 1
        if (fallCounter >= fallPeriod) {
            fallCounter = 0
            if (!board.moveDown()) {
                lockCurrentPiece()
            }
        }

        previousMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        audio.loopMusic(AudioAssetId.music(Sounds.MUSIC))
        for (index in 0 until pendingSoundCount) {
            audio.playSound(pendingSoundIds[index])
        }
        pendingSoundCount = 0
    }

    override fun draw(render: RenderContext) {
        val colors = palette(frame)
        render.verticalGradient(colors.backgroundTop, colors.backgroundBottom, frame)

        val cell = cellSize(render)
        val boardWidth = board.width * cell
        val boardHeight = board.height * cell
        val boardLeft = (render.width - boardWidth) / 2
        val boardTop = (render.height - boardHeight) / 2
        val rightPanelX = boardLeft + boardWidth + 32
        val leftPanelX = 36

        render.fillRect(boardLeft - 6, boardTop - 6, boardWidth + 12, boardHeight + 12, colors.panelBorder)
        render.fillRect(boardLeft, boardTop, boardWidth, boardHeight, colors.panel)
        drawLandingGuide(render, boardLeft, boardTop, cell, colors)
        drawGrid(render, boardLeft, boardTop, boardWidth, boardHeight, cell, colors.grid)
        drawBoardBlocks(render, boardLeft, boardTop, cell)
        drawCurrentPiece(render, boardLeft, boardTop, cell)

        drawSidePanels(render, leftPanelX, rightPanelX, boardTop, cell, colors)

        if (state == PlayState.PAUSED) {
            drawOverlay(render, "PAUSED", colors.warning)
        } else if (state == PlayState.GAME_OVER) {
            drawOverlay(render, "GAME OVER", colors.warning)
        }
    }

    override fun cleanup() {
    }

    fun snapshot(): String {
        return "state=$state score=${board.score} lines=${board.lines} frame=$frame"
    }

    private fun drawLandingGuide(render: RenderContext, left: Int, top: Int, cell: Int, colors: Palette) {
        if (state != PlayState.RUNNING) {
            return
        }

        val piece = board.getCurrentPiece() ?: return
        val position = board.getCurrentPiecePosition()
        val dropDistance = board.currentDropDistance()
        if (dropDistance <= 0) {
            return
        }

        val currentColumnBottoms = IntArray(board.width) { -1 }
        val landingColumnBottoms = IntArray(board.width) { -1 }
        val blocks = piece.getBlocks()
        for (block in blocks) {
            val x = position.x + block.x
            val y = position.y + block.y
            val landingY = y + dropDistance
            if (x !in 0 until board.width) {
                continue
            }
            if (y in 0 until board.height && y > currentColumnBottoms[x]) {
                currentColumnBottoms[x] = y
            }
            if (landingY in 0 until board.height && landingY > landingColumnBottoms[x]) {
                landingColumnBottoms[x] = landingY
            }
        }

        for (x in 0 until board.width) {
            val landingBottom = landingColumnBottoms[x]
            if (landingBottom < 0) {
                continue
            }
            val startY = (currentColumnBottoms[x] + 1).coerceAtLeast(0)
            if (landingBottom >= startY) {
                render.fillRect(
                    left + x * cell + 2,
                    top + startY * cell + 2,
                    cell - 4,
                    (landingBottom - startY + 1) * cell - 4,
                    colors.landingGuide
                )
            }
        }

        for (block in blocks) {
            val x = position.x + block.x
            val y = position.y + block.y + dropDistance
            if (x in 0 until board.width && y in 0 until board.height) {
                render.fillRect(
                    left + x * cell + 5,
                    top + y * cell + 5,
                    cell - 10,
                    cell - 10,
                    colors.landingTarget
                )
            }
        }
    }

    private fun drawGrid(
        render: RenderContext,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        cell: Int,
        color: Int
    ) {
        for (x in 0..board.width) {
            val px = left + x * cell
            render.drawLine(px, top, px, top + height, color)
        }
        for (y in 0..board.height) {
            val py = top + y * cell
            render.drawLine(left, py, left + width, py, color)
        }
    }

    private fun drawBoardBlocks(render: RenderContext, left: Int, top: Int, cell: Int) {
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val color = board.getColorAt(x, y)
                if (color != null) {
                    drawBlock(render, left + x * cell, top + y * cell, cell, color)
                }
            }
        }
    }

    private fun drawCurrentPiece(render: RenderContext, left: Int, top: Int, cell: Int) {
        val piece = board.getCurrentPiece() ?: return
        val position = board.getCurrentPiecePosition()
        for (block in piece.getBlocks()) {
            val x = position.x + block.x
            val y = position.y + block.y
            if (x in 0 until board.width && y in 0 until board.height) {
                drawBlock(render, left + x * cell, top + y * cell, cell, piece.color)
            }
        }
    }

    private fun drawSidePanels(
        render: RenderContext,
        leftPanelX: Int,
        rightPanelX: Int,
        boardTop: Int,
        cell: Int,
        colors: Palette
    ) {
        val nextSize = cell * 6
        render.drawText("HEXTRIS", leftPanelX, boardTop, colors.text, 4)
        render.drawText("SCORE", leftPanelX, boardTop + 58, colors.mutedText, 2)
        render.drawText("${board.score}", leftPanelX, boardTop + 82, colors.text, 3)
        render.drawText("LEVEL ${board.level}", leftPanelX, boardTop + 128, colors.text, 2)
        render.drawText("LINES ${board.lines}", leftPanelX, boardTop + 152, colors.text, 2)
        render.drawText("PIECES ${board.totalPieces()}", leftPanelX, boardTop + 176, colors.text, 2)

        render.drawText("NEXT", rightPanelX, boardTop, colors.text, 3)
        render.fillRect(rightPanelX, boardTop + 34, nextSize, nextSize, colors.panel)
        drawNextPiece(render, rightPanelX, boardTop + 34, cell)

        render.drawText("A/R CW", rightPanelX, boardTop + nextSize + 58, colors.mutedText, 2)
        render.drawText("B/L/Y CCW", rightPanelX, boardTop + nextSize + 82, colors.mutedText, 2)
        render.drawText("X 180", rightPanelX, boardTop + nextSize + 106, colors.mutedText, 2)
        render.drawText("UP DROP", rightPanelX, boardTop + nextSize + 130, colors.mutedText, 2)
        render.drawText("START PAUSE", rightPanelX, boardTop + nextSize + 154, colors.mutedText, 2)
        render.drawText("SELECT RESET", rightPanelX, boardTop + nextSize + 178, colors.mutedText, 2)
    }

    private fun drawNextPiece(render: RenderContext, left: Int, top: Int, cell: Int) {
        val piece = board.getNextPiece() ?: return
        val blocks = piece.getBlocks()
        val minX = blocks.minOf { it.x }
        val maxX = blocks.maxOf { it.x }
        val minY = blocks.minOf { it.y }
        val maxY = blocks.maxOf { it.y }
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val offsetX = left + ((6 - width) * cell) / 2
        val offsetY = top + ((6 - height) * cell) / 2

        for (block in blocks) {
            drawBlock(
                render,
                offsetX + (block.x - minX) * cell,
                offsetY + (block.y - minY) * cell,
                cell,
                piece.color
            )
        }
    }

    private fun drawBlock(render: RenderContext, x: Int, y: Int, size: Int, color: Int) {
        render.drawSprite(
            spriteId = RenderAssetId.sprite(Sprites.BLOCK_SPRITE_ID),
            x = x + 1,
            y = y + 1,
            width = size - 2,
            height = size - 2,
            frame = color
        )
    }

    private fun drawOverlay(render: RenderContext, label: String, color: Int) {
        val width = 310
        val height = 92
        val left = (render.width - width) / 2
        val top = (render.height - height) / 2
        render.fillRect(left, top, width, height, rgba(8, 10, 18))
        render.fillRect(left, top, width, 4, color)
        render.fillRect(left, top + height - 4, width, 4, color)
        render.drawText(label, left + 36, top + 28, color, 4)
    }

    private fun lockCurrentPiece() {
        val levelBefore = board.level
        val cleared = board.lockPiece()
        if (board.level != levelBefore) {
            fallCounter = 0
        }
        if (board.gameOver) {
            state = PlayState.GAME_OVER
            queueSound(Sounds.GAME_OVER)
        } else if (cleared > 0) {
            queueSound(Sounds.LINE_CLEAR)
        } else {
            queueSound(Sounds.LOCK)
        }
    }

    private fun reset() {
        board.reset()
        state = PlayState.RUNNING
        fallCounter = 0
        leftHold = 0
        rightHold = 0
        clockwiseHold = 0
        counterClockwiseHold = 0
    }

    private fun queueSound(name: String) {
        if (pendingSoundCount >= pendingSoundIds.size) {
            return
        }
        pendingSoundIds[pendingSoundCount] = AudioAssetId.sound(name)
        pendingSoundCount += 1
    }

    private fun cellSize(render: RenderContext): Int {
        val byHeight = (render.height - 96) / board.height
        val byWidth = (render.width * 38 / 100) / board.width
        return minOf(byHeight, byWidth).coerceIn(10, 32)
    }

    private fun normalFallPeriod(): Int {
        return (42 - board.level * 3).coerceAtLeast(8)
    }

    private fun nextHold(pressed: Boolean, current: Int): Int {
        return if (pressed) current + 1 else 0
    }

    private fun shouldRepeat(counter: Int, firstDelay: Int = 12, repeatRate: Int = 5): Boolean {
        return counter == 1 || (counter > firstDelay && (counter - firstDelay) % repeatRate == 0)
    }

    private fun justPressed(input: InputState, button: InputButton): Boolean {
        val bit = InputState.bitFor(button)
        return input.isPressed(button) && (previousMask and bit) == 0
    }

    private fun palette(seed: Int): Palette {
        val pulse = (seed / 12) % 32
        return Palette(
            backgroundTop = rgba(8 + pulse / 4, 12, 24 + pulse / 2),
            backgroundBottom = rgba(20, 28 + pulse / 2, 44),
            panel = rgba(7, 9, 16),
            panelBorder = rgba(78, 90, 130),
            grid = rgba(32, 38, 58),
            text = rgba(246, 248, 238),
            mutedText = rgba(172, 202, 220),
            warning = rgba(248, 210, 88),
            landingGuide = rgba(114, 202, 220, 70),
            landingTarget = rgba(176, 248, 255, 145)
        )
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return r.coerceIn(0, 255) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (a.coerceIn(0, 255) shl 24)
    }

    private data class Palette(
        val backgroundTop: Int,
        val backgroundBottom: Int,
        val panel: Int,
        val panelBorder: Int,
        val grid: Int,
        val text: Int,
        val mutedText: Int,
        val warning: Int,
        val landingGuide: Int,
        val landingTarget: Int
    )

    private enum class PlayState {
        RUNNING,
        PAUSED,
        GAME_OVER
    }
}
