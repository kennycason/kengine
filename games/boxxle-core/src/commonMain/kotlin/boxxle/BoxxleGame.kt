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
    private var moveAnimation: MoveAnimation? = null
    private var directionIntent: Direction? = null
    private var queuedDirection: Direction? = null
    private var leftLevelHoldFrames = 0
    private var rightLevelHoldFrames = 0

    private val tileSheet = RenderAssetId.sprite(BoxxleAssets.TILES_ID)
    private val finishSound = AudioAssetId.sound(BoxxleAssets.FINISH_ID)
    private val mainMusic = AudioAssetId.music(BoxxleAssets.MAIN_ID)
    private val floorColor = rgba(255, 255, 255)
    private val floorGridColor = rgba(222, 226, 214)

    override fun update(input: InputState) {
        val inputDirection = updateDirectionIntent(input)

        if (justPressed(input, InputButton.B) || justPressed(input, InputButton.START)) {
            loadLevel(levelNumber)
            previousMask = input.mask
            return
        }

        if (moveAnimation != null) {
            resetLevelShortcutHolds()
            if (inputDirection != null) {
                queuedDirection = inputDirection
            }
            advanceMoveAnimation()
            if (moveAnimation == null) {
                if (startLevelCompleteIfNeeded()) {
                    previousMask = input.mask
                    return
                }
                val nextDirection = queuedDirection
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
        audio.loopMusic(mainMusic, volume = 140)
        if (pendingFinishSound) {
            audio.playSound(finishSound, volume = 190)
            pendingFinishSound = false
        }
    }

    override fun draw(render: RenderContext) {
        render.clear(rgba(236, 238, 218))

        val layout = layout(render)
        val uiScale = uiScale(render)
        val titleColor = rgba(42, 52, 45)
        val levelText = "LVL ${levelNumber + 1}/${LEVEL_DATA.size}"
        render.drawText("BOXXLE", HUD_MARGIN, 8, titleColor, uiScale)
        render.drawText(levelText, render.width - textWidth(levelText, uiScale) - HUD_MARGIN, 10, titleColor, uiScale)

        drawBoard(render, layout)
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

        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
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
            }
        }

        level.goals.forEach { goal ->
            render.drawSprite(
                tileSheet,
                layout.left + goal.x * layout.tile,
                layout.top + goal.y * layout.tile,
                layout.tile,
                layout.tile,
                frame = FRAME_GOAL
            )
        }

        val animation = moveAnimation
        level.boxes.forEachIndexed { index, box ->
            val frame = if (level.isGoal(box)) FRAME_BOX_PLACED else FRAME_BOX
            if (animation != null && animation.hasBox && animation.boxIndex == index) {
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
        val placed = level.boxes.count { level.isGoal(it) }
        render.drawText("BOXES $placed/${level.goals.size}", HUD_MARGIN, 8 + 12 * uiScale, rgba(67, 84, 70), uiScale)
        render.drawText("B RESET  L/R LEVEL", HUD_MARGIN, render.height - 8 * uiScale - 4, rgba(67, 84, 70), uiScale)
    }

    private fun drawCompleteOverlay(render: RenderContext, uiScale: Int) {
        val width = textWidth("CLEAR!", uiScale) + 36
        val height = 14 * uiScale + 14
        val x = (render.width - width) / 2
        val y = (render.height - height) / 2
        render.fillRect(x, y, width, height, rgba(42, 52, 45))
        render.drawText("CLEAR!", x + 18, y + 7, rgba(236, 238, 218), uiScale)
    }

    private fun layout(render: RenderContext): BoardLayout {
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
        return BoardLayout(
            left = (render.width - boardWidth) / 2,
            top = topHudHeight + ((availableHeight - boardHeight) / 2).coerceAtLeast(0),
            tile = tile
        )
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
        return level.goals.all { goal -> level.boxes.any { it == goal } }
    }

    private fun startLevelCompleteIfNeeded(): Boolean {
        if (!isLevelComplete()) {
            return false
        }
        if (completeFrames == 0) {
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
        moveAnimation = null
    }

    private fun nextLevelNumber(): Int = (levelNumber + 1) % LEVEL_DATA.size

    private fun previousLevelNumber(): Int = positiveModulo(levelNumber - 1, LEVEL_DATA.size)

    private fun updateDirectionIntent(input: InputState): Direction? {
        val newlyPressed = DIRECTION_PRIORITY.firstOrNull { direction ->
            input.isPressed(direction.button) &&
                (previousMask and InputState.bitFor(direction.button)) == 0
        }
        if (newlyPressed != null) {
            directionIntent = newlyPressed
            return newlyPressed
        }

        val currentIntent = directionIntent
        if (currentIntent != null && input.isPressed(currentIntent.button)) {
            return currentIntent
        }

        return DIRECTION_PRIORITY.firstOrNull { input.isPressed(it.button) }
            .also { directionIntent = it }
    }

    private fun drawBoardGrid(render: RenderContext, layout: BoardLayout) {
        val boardWidth = level.width * layout.tile
        val boardHeight = level.height * layout.tile

        for (x in 0..level.width) {
            val px = layout.left + x * layout.tile
            render.drawLine(px, layout.top, px, layout.top + boardHeight, floorGridColor)
        }
        for (y in 0..level.height) {
            val py = layout.top + y * layout.tile
            render.drawLine(layout.left, py, layout.left + boardWidth, py, floorGridColor)
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

private data class BoardLayout(val left: Int, val top: Int, val tile: Int)

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
            levelSwitchHoldFrames = 1
        )
        val Nintendo64 = BoxxleTiming(
            moveAnimationFrames = 2,
            levelCompleteDelayFrames = 72,
            levelSwitchHoldFrames = 5
        )
    }
}

private val DIRECTION_PRIORITY = listOf(Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN)

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
        if (y !in data.tiles.indices) return Tiles.BRICK
        val row = data.tiles[y]
        if (x !in row.indices) return Tiles.BRICK
        return row[x]
    }

    fun isWalkable(position: TilePosition): Boolean {
        return position.x in 0 until width &&
            position.y in 0 until height &&
            tileAt(position.x, position.y) != Tiles.BRICK
    }

    fun boxIndexAt(position: TilePosition): Int {
        return boxes.indexOfFirst { it == position }
    }

    fun canMoveBoxTo(position: TilePosition): Boolean {
        return isWalkable(position) && boxIndexAt(position) < 0
    }

    fun isGoal(position: TilePosition): Boolean {
        return goals.any { it == position }
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
