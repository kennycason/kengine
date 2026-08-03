package hextris

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext
import com.kengine.storage.NoOpPortableStorage
import com.kengine.storage.PortableStorage

class HextrisGame : PortableGame {
    override val assets = HextrisAssets
    override val storageNamespace = "hextris"

    private val board = Board()
    private var storage: PortableStorage = NoOpPortableStorage
    private var highScore = 0

    private var frame = 0
    private var previousMask = 0
    private var fallCounter = 0
    private var leftHold = 0
    private var rightHold = 0
    private var clockwiseHold = 0
    private var counterClockwiseHold = 0
    private var state = PlayState.RUNNING
    private var combo = 0
    private var comboFlashFrames = 0
    private val pendingSoundIds = IntArray(8)
    private var pendingSoundCount = 0

    override fun update(input: InputState) {
        frame += 1
        if (comboFlashFrames > 0) {
            comboFlashFrames -= 1
        }

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

    override fun attachStorage(storage: PortableStorage) {
        this.storage = storage
        highScore = storage.loadString(HIGH_SCORE_KEY)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    override fun audio(audio: AudioContext) {
        audio.loopMusic(AudioAssetId.music(Sounds.MUSIC))
        for (index in 0 until pendingSoundCount) {
            audio.playSound(pendingSoundIds[index])
        }
        pendingSoundCount = 0
    }

    override fun draw(render: RenderContext) {
        val colors = palette(frame, board.level, comboFlashFrames)
        render.verticalGradient(colors.backgroundTop, colors.backgroundBottom, frame)

        val cell = cellSize(render)
        val boardWidth = board.width * cell
        val boardHeight = board.height * cell
        val boardLeft = (render.width - boardWidth) / 2
        val boardTop = (render.height - boardHeight) / 2
        val rightPanelX = boardLeft + boardWidth + 32

        render.fillRect(boardLeft - 6, boardTop - 6, boardWidth + 12, boardHeight + 12, colors.panelBorder)
        render.fillRect(boardLeft, boardTop, boardWidth, boardHeight, colors.panel)
        drawLandingGuide(render, boardLeft, boardTop, cell, colors)
        drawGrid(render, boardLeft, boardTop, boardWidth, boardHeight, cell, colors.grid)
        drawBoardBlocks(render, boardLeft, boardTop, cell)
        drawCurrentPiece(render, boardLeft, boardTop, cell)

        if (comboFlashFrames > 0) {
            drawComboPulse(render, boardLeft, boardTop, boardWidth, boardHeight, colors)
        }

        drawSidePanels(render, boardLeft, rightPanelX, boardTop, boardHeight, cell, colors)

        if (state == PlayState.PAUSED) {
            drawOverlay(render, "PAUSED", colors.warning)
        } else if (state == PlayState.GAME_OVER) {
            drawOverlay(render, "GAME OVER", colors.warning)
        }
    }

    override fun cleanup() {
    }

    fun snapshot(): String {
        return "state=$state score=${board.score} highScore=$highScore lines=${board.lines} frame=$frame"
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
        boardLeft: Int,
        rightPanelX: Int,
        boardTop: Int,
        boardHeight: Int,
        cell: Int,
        colors: Palette
    ) {
        val nextSize = cell * 6
        val previewGap = (cell / 2).coerceAtLeast(12)
        val nextX = boardLeft - nextSize - previewGap
        val nextNextX = nextX - nextSize - previewGap
        val leftPanelX = nextNextX.coerceAtLeast(10)
        val statValueX = nextX.coerceAtLeast(leftPanelX + 170)

        drawPreviewBox(render, board.getNextNextPiece(), leftPanelX, boardTop, nextSize, cell, colors, label = null)
        drawPreviewBox(render, board.getNextPiece(), nextX, boardTop, nextSize, cell, colors, label = "NEXT")

        val statTop = boardTop + nextSize + 22
        drawStatLine(render, "SCORE", board.score.toString(), leftPanelX, statValueX, statTop, colors)
        drawStatLine(render, "BEST", highScore.toString(), leftPanelX, statValueX, statTop + 40, colors)
        drawStatLine(render, "LEVEL", board.level.toString(), leftPanelX, statValueX, statTop + 80, colors)
        drawStatLine(render, "LINES", board.lines.toString(), leftPanelX, statValueX, statTop + 120, colors)
        drawStatLine(render, "PIECES", board.totalPieces().toString(), leftPanelX, statValueX, statTop + 160, colors)

        drawControls(render, leftPanelX, statTop + 222, colors)
        drawPieceStats(
            render = render,
            left = rightPanelX,
            top = boardTop,
            width = render.width - rightPanelX - 10,
            height = boardHeight,
            cell = cell,
            colors = colors
        )
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
        render.drawText(label, left, y, colors.mutedText, 4)
        render.drawText(value, valueX, y, colors.text, 4)
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

    private fun drawPreviewBox(
        render: RenderContext,
        piece: Piece?,
        left: Int,
        top: Int,
        size: Int,
        cell: Int,
        colors: Palette,
        label: String?
    ) {
        render.fillRect(left - 4, top - 4, size + 8, size + 8, colors.panelBorder)
        render.fillRect(left, top, size, size, colors.panel)
        if (label != null) {
            render.drawText(label, left + 8, top + 8, colors.text, 2)
        }
        if (piece == null) return

        val blocks = piece.getBlocks()
        val minX = blocks.minOf { it.x }
        val maxX = blocks.maxOf { it.x }
        val minY = blocks.minOf { it.y }
        val maxY = blocks.maxOf { it.y }
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val labelOffset = if (label == null) 0 else cell / 2
        val offsetX = left + ((6 - width) * cell) / 2
        val offsetY = top + ((6 - height) * cell) / 2 + labelOffset

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

    private fun drawPieceStats(
        render: RenderContext,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        cell: Int,
        colors: Palette
    ) {
        val counts = board.getPieceCounts()
        val pieceTypes = PieceType.entries
        val piecesPerColumn = (pieceTypes.size + 1) / 2
        val columnWidth = (width / 2).coerceAtLeast(150)
        val smallCell = minOf(cell * 5 / 8, (columnWidth - 72) / 6).coerceIn(13, 18)
        val previewWidth = smallCell * 6
        val rowHeight = (height / piecesPerColumn).coerceAtLeast(smallCell * 3)

        for (index in pieceTypes.indices) {
            val pieceType = pieceTypes[index]
            val column = index / piecesPerColumn
            val row = index % piecesPerColumn
            val x = left + column * columnWidth
            val y = top + row * rowHeight
            val pieceY = y + ((rowHeight - smallCell * 3) / 2).coerceAtLeast(0)
            val countY = y + ((rowHeight - 21) / 2).coerceAtLeast(0)

            drawSmallPiece(render, x, pieceY, smallCell, pieceType)
            render.drawText("x${counts[pieceType] ?: 0}", x + previewWidth + 8, countY, colors.text, 3)
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
            combo += 1
            comboFlashFrames = (12 + combo * 8 + cleared * 4).coerceAtMost(40)
            queueSound(Sounds.LINE_CLEAR)
        } else {
            combo = 0
            queueSound(Sounds.LOCK)
        }
        recordHighScore(board.score)
    }

    internal fun recordHighScore(score: Int) {
        if (score <= highScore) {
            return
        }
        highScore = score
        storage.saveString(HIGH_SCORE_KEY, highScore.toString())
    }

    private fun reset() {
        board.reset()
        state = PlayState.RUNNING
        fallCounter = 0
        leftHold = 0
        rightHold = 0
        clockwiseHold = 0
        counterClockwiseHold = 0
        combo = 0
        comboFlashFrames = 0
    }

    private fun queueSound(name: String) {
        if (pendingSoundCount >= pendingSoundIds.size) {
            return
        }
        pendingSoundIds[pendingSoundCount] = AudioAssetId.sound(name)
        pendingSoundCount += 1
    }

    private fun cellSize(render: RenderContext): Int {
        val byHeight = (render.height - 20) / board.height
        val byWidth = (render.width * 44 / 100) / board.width
        return minOf(byHeight, byWidth).coerceIn(10, 40)
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

    private fun palette(seed: Int, level: Int, comboFlashFrames: Int): Palette {
        val pulse = (seed / 12) % 32
        val levelTheme = level % 6
        val baseTop = when (levelTheme) {
            0 -> rgba(4 + pulse / 10, 4 + pulse / 12, 14 + pulse / 4)
            1 -> rgba(1 + pulse / 14, 15 + pulse / 5, 18 + pulse / 7)
            2 -> rgba(15 + pulse / 5, 5 + pulse / 12, 22 + pulse / 6)
            3 -> rgba(17 + pulse / 6, 10 + pulse / 8, 1 + pulse / 14)
            4 -> rgba(3 + pulse / 14, 15 + pulse / 7, 7 + pulse / 10)
            else -> rgba(14 + pulse / 7, 2 + pulse / 14, 7 + pulse / 9)
        }
        val baseBottom = when (levelTheme) {
            0 -> rgba(1, 1, 8 + pulse / 3)
            1 -> rgba(0, 8 + pulse / 6, 11 + pulse / 5)
            2 -> rgba(8 + pulse / 8, 1, 14 + pulse / 4)
            3 -> rgba(12 + pulse / 5, 5 + pulse / 10, 0)
            4 -> rgba(0, 10 + pulse / 5, 4 + pulse / 12)
            else -> rgba(10 + pulse / 4, 0, 4 + pulse / 8)
        }
        val flashActive = comboFlashFrames > 0 && (seed / 3) % 2 == 0
        val backgroundTop = if (flashActive) blend(baseTop, rgba(255, 246, 165), 42) else baseTop
        val backgroundBottom = if (flashActive) blend(baseBottom, rgba(235, 73, 186), 32) else baseBottom

        return Palette(
            backgroundTop = backgroundTop,
            backgroundBottom = backgroundBottom,
            panel = rgba(0, 0, 0, 230),
            panelBorder = if (flashActive) rgba(255, 236, 127, 190) else rgba(60, 60, 100, 150),
            grid = rgba(30, 30, 30, 105),
            text = rgba(255, 255, 255),
            mutedText = rgba(204, 204, 255),
            warning = rgba(255, 255, 0),
            landingTarget = rgba(54, 63, 82, 125),
            comboAccent = if (flashActive) rgba(255, 245, 160, 215) else rgba(130, 160, 220, 80)
        )
    }

    private fun drawComboPulse(render: RenderContext, left: Int, top: Int, width: Int, height: Int, colors: Palette) {
        val inset = (comboFlashFrames % 8) + 2
        render.drawLine(left - inset, top - inset, left + width + inset, top - inset, colors.comboAccent)
        render.drawLine(left - inset, top + height + inset, left + width + inset, top + height + inset, colors.comboAccent)
        render.drawLine(left - inset, top - inset, left - inset, top + height + inset, colors.comboAccent)
        render.drawLine(left + width + inset, top - inset, left + width + inset, top + height + inset, colors.comboAccent)
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return r.coerceIn(0, 255) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (a.coerceIn(0, 255) shl 24)
    }

    private fun blend(from: Int, to: Int, percent: Int): Int {
        val amount = percent.coerceIn(0, 100)
        fun channel(shift: Int): Int {
            val start = (from shr shift) and 0xff
            val end = (to shr shift) and 0xff
            return start + (end - start) * amount / 100
        }
        return rgba(channel(0), channel(8), channel(16), channel(24))
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
        val landingTarget: Int,
        val comboAccent: Int
    )

    private enum class PlayState {
        RUNNING,
        PAUSED,
        GAME_OVER
    }

    companion object {
        const val HIGH_SCORE_KEY = "high-score"
    }
}
