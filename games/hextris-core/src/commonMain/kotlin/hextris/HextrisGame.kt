package hextris

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext

class HextrisGame : PortableGame {
    override val assets = HextrisAssets

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

        val blocks = piece.getBlocks()

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
        render.drawText("NEXT", leftPanelX, boardTop, colors.text, 3)
        render.fillRect(leftPanelX, boardTop + 34, nextSize, nextSize, colors.panel)
        drawNextPiece(render, leftPanelX, boardTop + 34, cell)

        val statTop = boardTop + nextSize + 58
        val statValueX = leftPanelX + 142
        drawStatLine(render, "SCORE", board.score.toString(), leftPanelX, statValueX, statTop, colors)
        drawStatLine(render, "LEVEL", board.level.toString(), leftPanelX, statValueX, statTop + 48, colors)
        drawStatLine(render, "LINES", board.lines.toString(), leftPanelX, statValueX, statTop + 96, colors)
        drawStatLine(render, "PIECES", board.totalPieces().toString(), leftPanelX, statValueX, statTop + 144, colors)

        drawControls(render, leftPanelX, statTop + 218, colors)
        drawPieceStats(render, rightPanelX, boardTop, cell, colors)
    }

    private fun drawStatLine(
        render: RenderContext,
        label: String,
        value: String,
        left: Int,
        valueX: Int,
        y: Int,
        colors: Palette
    ) {
        render.drawText(label, left, y + 4, colors.mutedText, 2)
        render.drawText(value, valueX, y, colors.text, 3)
    }

    private fun drawControls(render: RenderContext, left: Int, top: Int, colors: Palette) {
        render.drawText("CONTROLS", left, top, colors.text, 2)
        render.drawText("A/R CW", left, top + 34, colors.mutedText, 2)
        render.drawText("B/L/Y CCW", left, top + 58, colors.mutedText, 2)
        render.drawText("X 180", left, top + 82, colors.mutedText, 2)
        render.drawText("UP DROP", left, top + 106, colors.mutedText, 2)
        render.drawText("START PAUSE", left, top + 130, colors.mutedText, 2)
        render.drawText("SELECT RESET", left, top + 154, colors.mutedText, 2)
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

    private fun drawPieceStats(render: RenderContext, left: Int, top: Int, cell: Int, colors: Palette) {
        val counts = board.getPieceCounts()
        val pieceTypes = PieceType.entries
        val piecesPerColumn = (pieceTypes.size + 1) / 2
        val smallCell = (cell / 2).coerceIn(10, 12)
        val previewWidth = smallCell * 6
        val columnWidth = previewWidth + 112
        val rowHeight = 40

        render.drawText("STATS", left, top, colors.text, 3)

        for (index in pieceTypes.indices) {
            val pieceType = pieceTypes[index]
            val column = index / piecesPerColumn
            val row = index % piecesPerColumn
            val x = left + column * columnWidth
            val y = top + 42 + row * rowHeight

            drawSmallPiece(render, x, y, smallCell, pieceType)
            render.drawText("x${counts[pieceType] ?: 0}", x + previewWidth + 12, y + 8, colors.text, 2)
        }
    }

    private fun drawSmallPiece(render: RenderContext, left: Int, top: Int, cell: Int, type: PieceType) {
        val piece = Piece(type, Sprites.colorIndex(type))
        val blocks = piece.getBlocks()
        val minX = blocks.minOf { it.x }
        val maxX = blocks.maxOf { it.x }
        val minY = blocks.minOf { it.y }
        val maxY = blocks.maxOf { it.y }
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val offsetX = left + ((6 - width) * cell) / 2
        val offsetY = top + ((3 - height) * cell) / 2

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
            backgroundTop = rgba(pulse / 8, pulse / 8, pulse / 6),
            backgroundBottom = rgba(5, 5, 12 + pulse / 3),
            panel = rgba(0, 0, 0, 230),
            panelBorder = rgba(60, 60, 100, 150),
            grid = rgba(30, 30, 30, 105),
            text = rgba(255, 255, 255),
            mutedText = rgba(204, 204, 255),
            warning = rgba(255, 255, 0),
            landingTarget = rgba(54, 63, 82, 125)
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
        val landingTarget: Int
    )

    private enum class PlayState {
        RUNNING,
        PAUSED,
        GAME_OVER
    }
}
