package boxxle

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext

class BoxxleGame(
    private val timing: BoxxleTiming = BoxxleTiming.Desktop
) : PortableGame {
    override val assets = BoxxleAssets
    override val storageNamespace = "boxxle"

    private var levelNumber = 0
    private var level = BoxxleLevel.from(0)
    private var player = TilePosition(level.data.start[0], level.data.start[1])
    private var face = Direction.DOWN
    private var previousMask = 0
    private var completeFrames = 0
    private var pendingFinishSound = false
    private var pendingStopMainMusic = false
    private var mainMusicEnabled = true
    private var moveAnimation: MoveAnimation? = null
    private var directionIntent: Direction? = null
    private var queuedDirection: Direction? = null
    private var leftLevelHoldFrames = 0
    private var rightLevelHoldFrames = 0
    private val boardLayout = BoardLayout()

    private val tileSheet = RenderAssetId.sprite(BoxxleAssets.TILES_ID)
    private val finishSound = AudioAssetId.sound(BoxxleAssets.FINISH_ID)
    private val mainMusic = AudioAssetId.music(BoxxleAssets.MAIN_ID)
    private val floorColor = rgba(255, 255, 255)
    private val floorGridColor = rgba(222, 226, 214)

    override fun update(input: InputState) {
        val inputDirection = updateDirectionIntent(input)
        val newlyPressedDirection = newlyPressedDirection(input)

        if (justPressed(input, InputButton.B) || justPressed(input, InputButton.START)) {
            loadLevel(levelNumber)
            previousMask = input.mask
            return
        }

        if (moveAnimation != null) {
            resetLevelShortcutHolds()
            if (newlyPressedDirection != null) {
                queuedDirection = newlyPressedDirection
            }
            advanceMoveAnimation()
            if (moveAnimation == null) {
                if (startLevelCompleteIfNeeded()) {
                    previousMask = input.mask
                    return
                }
                val nextDirection = queuedDirection ?: inputDirection
                queuedDirection = null
                if (nextDirection != null) {
                    tryMove(nextDirection)
                }
            }
            previousMask = input.mask
            return
        }

        if (completeFrames > 0) {
            completeFrames -= 1
            if (completeFrames == 0) {
                loadLevel(nextLevelNumber())
            }
            previousMask = input.mask
            return
        }

        if (handleLevelShortcut(input, inputDirection)) {
            previousMask = input.mask
            return
        }

        if (inputDirection != null) {
            tryMove(inputDirection)
        }

        if (moveAnimation == null) {
            startLevelCompleteIfNeeded()
        }

        previousMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        if (pendingStopMainMusic) {
            audio.stopMusic(mainMusic)
            pendingStopMainMusic = false
        }
        if (pendingFinishSound) {
            audio.playSound(finishSound, volume = 190)
            pendingFinishSound = false
        }
        if (mainMusicEnabled) {
            audio.loopMusic(mainMusic, volume = 230)
        }
    }

    override fun draw(render: RenderContext) {
        render.clear(rgba(236, 238, 218))

        updateLayout(render, boardLayout)
        val uiScale = uiScale(render)
        val titleColor = rgba(42, 52, 45)
        render.drawText("BOXXLE", HUD_MARGIN, 8, titleColor, uiScale)
        drawLevelText(render, uiScale, titleColor)

        drawBoard(render, boardLayout)
        drawHud(render, uiScale)

        if (completeFrames > 0) {
            drawCompleteOverlay(render, uiScale)
        }
    }

    override fun cleanup() {
    }

    fun snapshot(): String {
        return "level=$levelNumber player=${player.x},${player.y} boxes=${level.boxes.joinToString { "${it.x},${it.y}" }}"
    }

    private fun drawBoard(render: RenderContext, layout: BoardLayout) {
        val boardWidth = level.width * layout.tile
        val boardHeight = level.height * layout.tile
        render.fillRect(layout.left - 4, layout.top - 4, boardWidth + 8, boardHeight + 8, rgba(138, 154, 122))
        render.fillRect(layout.left, layout.top, boardWidth, boardHeight, floorColor)
        drawBoardGrid(render, layout)

        var y = 0
        while (y < level.height) {
            var x = 0
            while (x < level.width) {
                if (level.tileAt(x, y) == Tiles.BRICK) {
                    render.drawSprite(
                        tileSheet,
                        layout.left + x * layout.tile,
                        layout.top + y * layout.tile,
                        layout.tile,
                        layout.tile,
                        frame = FRAME_BRICK
                    )
                }
                x += 1
            }
            y += 1
        }

        var goalIndex = 0
        while (goalIndex < level.goals.size) {
            val goal = level.goals[goalIndex]
            render.drawSprite(
                tileSheet,
                layout.left + goal.x * layout.tile,
                layout.top + goal.y * layout.tile,
                layout.tile,
                layout.tile,
                frame = FRAME_GOAL
            )
            goalIndex += 1
        }

        val animation = moveAnimation
        var boxIndex = 0
        while (boxIndex < level.boxes.size) {
            val box = level.boxes[boxIndex]
            val frame = if (level.isGoal(box)) FRAME_BOX_PLACED else FRAME_BOX
            if (animation != null && animation.hasBox && animation.boxIndex == boxIndex) {
                drawAnimatedTile(
                    render = render,
                    layout = layout,
                    fromX = animation.boxFromX,
                    fromY = animation.boxFromY,
                    toX = animation.boxToX,
                    toY = animation.boxToY,
                    animation = animation,
                    frame = frame
                )
            } else {
                drawTile(render, layout, box, frame)
            }
            boxIndex += 1
        }

        if (animation != null) {
            drawAnimatedTile(
                render = render,
                layout = layout,
                fromX = animation.playerFromX,
                fromY = animation.playerFromY,
                toX = animation.playerToX,
                toY = animation.playerToY,
                animation = animation,
                frame = face.frame
            )
        } else {
            drawTile(render, layout, player, face.frame)
        }
    }

    private fun drawHud(render: RenderContext, uiScale: Int) {
        drawBoxesText(render, placedBoxCount(), uiScale)
        render.drawText("B RESET  L/R LEVEL", HUD_MARGIN, render.height - 8 * uiScale - 4, rgba(67, 84, 70), uiScale)
    }

    private fun drawCompleteOverlay(render: RenderContext, uiScale: Int) {
        val width = textWidth("CLEAR!", uiScale) + 36
        val height = 14 * uiScale + 14
        val x = (render.width - width) / 2
        val y = (render.height - height) / 2 - 4
        render.fillRect(x, y, width, height, rgba(42, 52, 45))
        render.drawText("CLEAR!", x + 18, y + 7, rgba(236, 238, 218), uiScale)
    }

    private fun updateLayout(render: RenderContext, layout: BoardLayout) {
        val uiScale = uiScale(render)
        val topHudHeight = if (uiScale == 1) 38 else 58
        val bottomHudHeight = if (uiScale == 1) 30 else 34
        val availableWidth = (render.width - 16).coerceAtLeast(64)
        val availableHeight = (render.height - topHudHeight - bottomHudHeight).coerceAtLeast(64)
        val preferredTile = (BASE_TILE_DIM * level.data.scale).toInt().coerceAtLeast(8)
        val fitTile = minOf(availableWidth / level.width, availableHeight / level.height).coerceAtLeast(6)
        val tile = minOf(preferredTile, fitTile).coerceAtLeast(6)
        val boardWidth = level.width * tile
        val boardHeight = level.height * tile
        layout.left = (render.width - boardWidth) / 2
        layout.top = topHudHeight + ((availableHeight - boardHeight) / 2).coerceAtLeast(0)
        layout.tile = tile
    }

    private fun tryMove(direction: Direction) {
        face = direction
        val playerFrom = player
        val target = player + direction.delta
        if (!level.isWalkable(target)) {
            return
        }

        val boxIndex = level.boxIndexAt(target)
        var hasBox = false
        var boxFromX = 0
        var boxFromY = 0
        var boxToX = 0
        var boxToY = 0
        if (boxIndex >= 0) {
            val boxTarget = target + direction.delta
            if (!level.canMoveBoxTo(boxTarget)) {
                return
            }
            hasBox = true
            boxFromX = target.x
            boxFromY = target.y
            boxToX = boxTarget.x
            boxToY = boxTarget.y
            level.boxes[boxIndex] = boxTarget
        }

        player = target
        val animationFrames = timing.moveAnimationFrames.coerceAtLeast(1)
        moveAnimation = MoveAnimation(
            playerFromX = playerFrom.x,
            playerFromY = playerFrom.y,
            playerToX = target.x,
            playerToY = target.y,
            boxIndex = boxIndex,
            hasBox = hasBox,
            boxFromX = boxFromX,
            boxFromY = boxFromY,
            boxToX = boxToX,
            boxToY = boxToY,
            totalFrames = animationFrames,
            framesRemaining = (animationFrames - 1).coerceAtLeast(0)
        )
    }

    private fun advanceMoveAnimation() {
        val animation = moveAnimation ?: return
        animation.framesRemaining -= 1
        if (animation.framesRemaining <= 0) {
            moveAnimation = null
        }
    }

    private fun isLevelComplete(): Boolean {
        var goalIndex = 0
        while (goalIndex < level.goals.size) {
            val goal = level.goals[goalIndex]
            var boxIndex = 0
            var found = false
            while (boxIndex < level.boxes.size) {
                if (level.boxes[boxIndex] == goal) {
                    found = true
                    break
                }
                boxIndex += 1
            }
            if (!found) {
                return false
            }
            goalIndex += 1
        }
        return true
    }

    private fun startLevelCompleteIfNeeded(): Boolean {
        if (!isLevelComplete()) {
            return false
        }
        if (completeFrames == 0) {
            mainMusicEnabled = false
            pendingStopMainMusic = true
            pendingFinishSound = true
            completeFrames = timing.levelCompleteDelayFrames.coerceAtLeast(1)
        }
        return true
    }

    private fun loadLevel(newLevelNumber: Int) {
        levelNumber = positiveModulo(newLevelNumber, LEVEL_DATA.size)
        level = BoxxleLevel.from(levelNumber)
        player = TilePosition(level.data.start[0], level.data.start[1])
        face = Direction.DOWN
        directionIntent = null
        queuedDirection = null
        resetLevelShortcutHolds()
        completeFrames = 0
        pendingFinishSound = false
        pendingStopMainMusic = true
        mainMusicEnabled = true
        moveAnimation = null
    }

    private fun nextLevelNumber(): Int = (levelNumber + 1) % LEVEL_DATA.size

    private fun previousLevelNumber(): Int = positiveModulo(levelNumber - 1, LEVEL_DATA.size)

    private fun updateDirectionIntent(input: InputState): Direction? {
        val newlyPressed = newlyPressedDirection(input)
        if (newlyPressed != null) {
            directionIntent = newlyPressed
            return newlyPressed
        }

        val currentIntent = directionIntent
        if (currentIntent != null && input.isPressed(currentIntent.button)) {
            return currentIntent
        }

        val pressed = pressedDirection(input)
        directionIntent = pressed
        return pressed
    }

    private fun newlyPressedDirection(input: InputState): Direction? {
        if (isNewlyPressed(input, Direction.LEFT)) return Direction.LEFT
        if (isNewlyPressed(input, Direction.RIGHT)) return Direction.RIGHT
        if (isNewlyPressed(input, Direction.UP)) return Direction.UP
        if (isNewlyPressed(input, Direction.DOWN)) return Direction.DOWN
        return null
    }

    private fun pressedDirection(input: InputState): Direction? {
        if (input.isPressed(Direction.LEFT.button)) return Direction.LEFT
        if (input.isPressed(Direction.RIGHT.button)) return Direction.RIGHT
        if (input.isPressed(Direction.UP.button)) return Direction.UP
        if (input.isPressed(Direction.DOWN.button)) return Direction.DOWN
        return null
    }

    private fun isNewlyPressed(input: InputState, direction: Direction): Boolean {
        return input.isPressed(direction.button) &&
            (previousMask and InputState.bitFor(direction.button)) == 0
    }

    private fun drawBoardGrid(render: RenderContext, layout: BoardLayout) {
        val boardWidth = level.width * layout.tile
        val boardHeight = level.height * layout.tile

        var x = 0
        while (x <= level.width) {
            val px = layout.left + x * layout.tile
            render.drawLine(px, layout.top, px, layout.top + boardHeight, floorGridColor)
            x += 1
        }
        var y = 0
        while (y <= level.height) {
            val py = layout.top + y * layout.tile
            render.drawLine(layout.left, py, layout.left + boardWidth, py, floorGridColor)
            y += 1
        }
    }

    private fun handleLevelShortcut(input: InputState, heldDirection: Direction?): Boolean {
        if (heldDirection != null) {
            resetLevelShortcutHolds()
            return false
        }

        leftLevelHoldFrames = nextHoldFrames(input.isPressed(InputButton.L), leftLevelHoldFrames)
        rightLevelHoldFrames = nextHoldFrames(input.isPressed(InputButton.R), rightLevelHoldFrames)
        val threshold = timing.levelSwitchHoldFrames.coerceAtLeast(1)

        return when {
            rightLevelHoldFrames == threshold && leftLevelHoldFrames == 0 -> {
                loadLevel(nextLevelNumber())
                true
            }
            leftLevelHoldFrames == threshold && rightLevelHoldFrames == 0 -> {
                loadLevel(previousLevelNumber())
                true
            }
            else -> false
        }
    }

    private fun nextHoldFrames(isPressed: Boolean, currentFrames: Int): Int {
        return if (isPressed) currentFrames + 1 else 0
    }

    private fun resetLevelShortcutHolds() {
        leftLevelHoldFrames = 0
        rightLevelHoldFrames = 0
    }

    private fun drawLevelText(render: RenderContext, uiScale: Int, color: Int) {
        val x = render.width - levelTextWidth(uiScale) - HUD_MARGIN
        var nextX = drawTextAndAdvance(render, "LVL ", x, 10, color, uiScale)
        nextX = drawNumberAndAdvance(render, levelNumber + 1, nextX, 10, color, uiScale)
        nextX = drawTextAndAdvance(render, "/", nextX, 10, color, uiScale)
        drawNumberAndAdvance(render, LEVEL_DATA.size, nextX, 10, color, uiScale)
    }

    private fun drawBoxesText(render: RenderContext, placed: Int, uiScale: Int) {
        val color = rgba(67, 84, 70)
        val y = 8 + 12 * uiScale
        var nextX = drawTextAndAdvance(render, "BOXES ", HUD_MARGIN, y, color, uiScale)
        nextX = drawNumberAndAdvance(render, placed, nextX, y, color, uiScale)
        nextX = drawTextAndAdvance(render, "/", nextX, y, color, uiScale)
        drawNumberAndAdvance(render, level.goals.size, nextX, y, color, uiScale)
    }

    private fun drawTextAndAdvance(render: RenderContext, text: String, x: Int, y: Int, color: Int, scale: Int): Int {
        render.drawText(text, x, y, color, scale)
        return x + textWidth(text, scale)
    }

    private fun drawNumberAndAdvance(render: RenderContext, value: Int, x: Int, y: Int, color: Int, scale: Int): Int {
        var nextX = x
        val clamped = value.coerceIn(0, 999)
        if (clamped >= 100) {
            nextX = drawDigitAndAdvance(render, clamped / 100, nextX, y, color, scale)
            nextX = drawDigitAndAdvance(render, (clamped / 10) % 10, nextX, y, color, scale)
            return drawDigitAndAdvance(render, clamped % 10, nextX, y, color, scale)
        }
        if (clamped >= 10) {
            nextX = drawDigitAndAdvance(render, clamped / 10, nextX, y, color, scale)
            return drawDigitAndAdvance(render, clamped % 10, nextX, y, color, scale)
        }
        return drawDigitAndAdvance(render, clamped, nextX, y, color, scale)
    }

    private fun drawDigitAndAdvance(render: RenderContext, digit: Int, x: Int, y: Int, color: Int, scale: Int): Int {
        val text = digitText(digit)
        render.drawText(text, x, y, color, scale)
        return x + textWidth(text, scale)
    }

    private fun levelTextWidth(scale: Int): Int {
        return textWidth("LVL ", scale) +
            numberWidth(levelNumber + 1, scale) +
            textWidth("/", scale) +
            numberWidth(LEVEL_DATA.size, scale)
    }

    private fun numberWidth(value: Int, scale: Int): Int {
        val clamped = value.coerceIn(0, 999)
        val digits = if (clamped >= 100) 3 else if (clamped >= 10) 2 else 1
        return digits * 6 * scale
    }

    private fun digitText(digit: Int): String {
        return when (digit) {
            0 -> "0"
            1 -> "1"
            2 -> "2"
            3 -> "3"
            4 -> "4"
            5 -> "5"
            6 -> "6"
            7 -> "7"
            8 -> "8"
            else -> "9"
        }
    }

    private fun placedBoxCount(): Int {
        var placed = 0
        var index = 0
        while (index < level.boxes.size) {
            if (level.isGoal(level.boxes[index])) {
                placed += 1
            }
            index += 1
        }
        return placed
    }

    private fun drawTile(render: RenderContext, layout: BoardLayout, position: TilePosition, frame: Int) {
        render.drawSprite(
            tileSheet,
            layout.left + position.x * layout.tile,
            layout.top + position.y * layout.tile,
            layout.tile,
            layout.tile,
            frame = frame
        )
    }

    private fun drawAnimatedTile(
        render: RenderContext,
        layout: BoardLayout,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        animation: MoveAnimation,
        frame: Int
    ) {
        render.drawSprite(
            tileSheet,
            animatedPixel(layout.left, fromX, toX, layout.tile, animation),
            animatedPixel(layout.top, fromY, toY, layout.tile, animation),
            layout.tile,
            layout.tile,
            frame = frame
        )
    }

    private fun animatedPixel(origin: Int, fromTile: Int, toTile: Int, tileSize: Int, animation: MoveAnimation): Int {
        val totalFrames = animation.totalFrames.coerceAtLeast(1)
        val elapsedFrames = (totalFrames - animation.framesRemaining).coerceIn(0, totalFrames)
        val deltaPixels = (toTile - fromTile) * tileSize
        return origin + fromTile * tileSize + easedPixelOffset(deltaPixels, elapsedFrames, totalFrames)
    }

    private fun easedPixelOffset(deltaPixels: Int, elapsedFrames: Int, totalFrames: Int): Int {
        if (elapsedFrames <= 0) return 0
        if (elapsedFrames >= totalFrames) return deltaPixels

        val numerator = elapsedFrames * elapsedFrames * (3 * totalFrames - 2 * elapsedFrames)
        val denominator = totalFrames * totalFrames * totalFrames
        val scaled = deltaPixels * numerator
        return if (scaled >= 0) {
            (scaled + denominator / 2) / denominator
        } else {
            (scaled - denominator / 2) / denominator
        }
    }

    private fun uiScale(render: RenderContext): Int {
        return if (render.width <= 360 || render.height <= 260) 1 else 2
    }

    private fun textWidth(text: String, scale: Int): Int {
        return text.length * 6 * scale
    }

    private fun justPressed(input: InputState, button: InputButton): Boolean {
        return input.isPressed(button) && (previousMask and InputState.bitFor(button)) == 0
    }

    private fun positiveModulo(value: Int, divisor: Int): Int {
        val remainder = value % divisor
        return if (remainder < 0) remainder + divisor else remainder
    }

    private fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return red.coerceIn(0, 255) or
            (green.coerceIn(0, 255) shl 8) or
            (blue.coerceIn(0, 255) shl 16) or
            (alpha.coerceIn(0, 255) shl 24)
    }

    private companion object {
        const val BASE_TILE_DIM = 32
        const val HUD_MARGIN = 10
        const val FRAME_BRICK = 0
        const val FRAME_BOX = 1
        const val FRAME_BOX_PLACED = 2
        const val FRAME_GOAL = 3
    }
}

private data class TilePosition(val x: Int, val y: Int) {
    operator fun plus(delta: TileDelta): TilePosition = TilePosition(x + delta.x, y + delta.y)
}

private data class TileDelta(val x: Int, val y: Int)

private class BoardLayout(
    var left: Int = 0,
    var top: Int = 0,
    var tile: Int = 0
)

private data class MoveAnimation(
    val playerFromX: Int,
    val playerFromY: Int,
    val playerToX: Int,
    val playerToY: Int,
    val boxIndex: Int,
    val hasBox: Boolean,
    val boxFromX: Int,
    val boxFromY: Int,
    val boxToX: Int,
    val boxToY: Int,
    val totalFrames: Int,
    var framesRemaining: Int
)

data class BoxxleTiming(
    val moveAnimationFrames: Int,
    val levelCompleteDelayFrames: Int,
    val levelSwitchHoldFrames: Int
) {
    companion object {
        val Desktop = BoxxleTiming(
            moveAnimationFrames = 8,
            levelCompleteDelayFrames = 360,
            levelSwitchHoldFrames = 18
        )
        val Nintendo64 = BoxxleTiming(
            moveAnimationFrames = 2,
            levelCompleteDelayFrames = 72,
            levelSwitchHoldFrames = 5
        )
    }
}

private enum class Direction(val delta: TileDelta, val frame: Int, val button: InputButton) {
    UP(TileDelta(0, -1), 4, InputButton.UP),
    DOWN(TileDelta(0, 1), 5, InputButton.DOWN),
    LEFT(TileDelta(-1, 0), 6, InputButton.LEFT),
    RIGHT(TileDelta(1, 0), 7, InputButton.RIGHT)
}

private object Tiles {
    const val EMPTY = 0
    const val BRICK = 1
}

private class BoxxleLevel private constructor(
    val data: LevelData,
    val boxes: MutableList<TilePosition>,
    val goals: List<TilePosition>
) {
    val width: Int = data.tiles.maxOf { it.size }
    val height: Int = data.tiles.size

    fun tileAt(x: Int, y: Int): Int {
        if (y < 0 || y >= data.tiles.size) return Tiles.BRICK
        val row = data.tiles[y]
        if (x < 0 || x >= row.size) return Tiles.BRICK
        return row[x]
    }

    fun isWalkable(position: TilePosition): Boolean {
        return position.x >= 0 &&
            position.x < width &&
            position.y >= 0 &&
            position.y < height &&
            tileAt(position.x, position.y) != Tiles.BRICK
    }

    fun boxIndexAt(position: TilePosition): Int {
        var index = 0
        while (index < boxes.size) {
            if (boxes[index] == position) {
                return index
            }
            index += 1
        }
        return -1
    }

    fun canMoveBoxTo(position: TilePosition): Boolean {
        return isWalkable(position) && boxIndexAt(position) < 0
    }

    fun isGoal(position: TilePosition): Boolean {
        var index = 0
        while (index < goals.size) {
            if (goals[index] == position) {
                return true
            }
            index += 1
        }
        return false
    }

    companion object {
        fun from(levelNumber: Int): BoxxleLevel {
            val data = LEVEL_DATA[levelNumber]
            return BoxxleLevel(
                data = data,
                boxes = data.boxes.map { TilePosition(it[0], it[1]) }.toMutableList(),
                goals = data.goals.map { TilePosition(it[0], it[1]) }
            )
        }
    }
}
