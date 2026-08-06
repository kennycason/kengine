package n64demo

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class N64DemoGame : PortableGame {
    override val storageNamespace: String = "n64demo"

    private var storage: PortableStorage? = null

    private var playerX = 152
    private var playerY = 120
    private var frame = 0
    private var colorIndex = 0
    private var highScore = 0
    private var score = 0
    private var showSprite = true
    private var previousInputMask = 0

    private val pokeball = RenderAssetId.sprite("pokeball")
    private val beepSound = AudioAssetId.sound("beep")
    private val finishSound = AudioAssetId.sound("finish")
    private val chordSound = AudioAssetId.sound("chord")

    private val trailColors = intArrayOf(
        0xFF000040.toInt(),
        0x00FF0040.toInt(),
        0x0000FF40.toInt(),
        0xFFFF0040.toInt()
    )

    private val trail = ArrayDeque<Pair<Int, Int>>()
    private var pendingSound: Int? = null

    override fun attachStorage(storage: PortableStorage) {
        this.storage = storage
        loadHighScore()
    }

    override fun update(input: InputState) {
        val speed = 2
        val dx = input.axis(InputButton.LEFT, InputButton.RIGHT)
        val dy = input.axis(InputButton.UP, InputButton.DOWN)
        val aPressed = input.isPressed(InputButton.A)
        val bPressed = input.isPressed(InputButton.B)
        val xPressed = input.isPressed(InputButton.X)
        val yPressed = input.isPressed(InputButton.Y)
        val aJustPressed = aPressed && (previousInputMask and InputState.bitFor(InputButton.A)) == 0
        val bJustPressed = bPressed && (previousInputMask and InputState.bitFor(InputButton.B)) == 0
        val xJustPressed = xPressed && (previousInputMask and InputState.bitFor(InputButton.X)) == 0
        val yJustPressed = yPressed && (previousInputMask and InputState.bitFor(InputButton.Y)) == 0

        playerX += dx * speed
        playerY += dy * speed

        if (playerX < 0) playerX = 0
        if (playerY < 0) playerY = 0
        if (playerX + 16 > 320) playerX = 320 - 16
        if (playerY + 16 > 240) playerY = 240 - 16

        if (aJustPressed) {
            colorIndex = (colorIndex + 1) % trailColors.size
            pendingSound = beepSound
        }

        if (bJustPressed) {
            showSprite = !showSprite
        }

        if (xJustPressed) {
            pendingSound = chordSound
        }

        if (yJustPressed) {
            pendingSound = finishSound
        }

        if (dx != 0 || dy != 0) {
            trail.addLast(Pair(playerX, playerY))
            if (trail.size > 20) trail.removeFirst()

            score++
            if (score > highScore) {
                highScore = score
                saveHighScore()
            }
        }

        frame++
        previousInputMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        pendingSound?.let { sound ->
            audio.playSound(sound)
            pendingSound = null
        }
    }

    override fun draw(render: RenderContext) {
        render.clear(0x101030FF.toInt())

        val borderColor = 0x444488FF.toInt()
        render.fillRect(0, 0, 320, 2, borderColor)
        render.fillRect(0, 238, 320, 2, borderColor)
        render.fillRect(0, 0, 2, 240, borderColor)
        render.fillRect(318, 0, 2, 240, borderColor)

        for (i in trail.indices) {
            val (tx, ty) = trail[i]
            val alpha = (i * 12).coerceAtMost(255)
            val color = trailColors[colorIndex] or (alpha shl 24)
            render.fillRect(tx + 4, ty + 4, 8, 8, color)
        }

        if (showSprite) {
            render.drawSprite(pokeball, playerX, playerY, 16, 16)
        } else {
            render.fillRect(playerX, playerY, 16, 16, 0x00FF00FF.toInt())
        }

        render.drawText("Kengine N64", 100, 8, 0xFFFFFFFF.toInt(), 2)
        render.drawText("Score: $score  Hi: $highScore", 20, 30, 0xCCCCCCFF.toInt(), 1)
        render.drawText("A: beep  B: sprite  X: chord  Y: finish", 10, 214, 0xAAAAAAFF.toInt(), 1)
        render.drawText("D-Pad: move", 10, 226, 0xAAAAAAFF.toInt(), 1)
    }

    override fun cleanup() {
        saveHighScore()
    }

    private fun loadHighScore() {
        val data = storage?.load("high-score") ?: return
        if (data.size >= 4) {
            highScore = (data[0].toInt() and 0xFF) or
                ((data[1].toInt() and 0xFF) shl 8) or
                ((data[2].toInt() and 0xFF) shl 16) or
                ((data[3].toInt() and 0xFF) shl 24)
        }
    }

    private fun saveHighScore() {
        val data = ByteArray(4)
        data[0] = (highScore and 0xFF).toByte()
        data[1] = ((highScore shr 8) and 0xFF).toByte()
        data[2] = ((highScore shr 16) and 0xFF).toByte()
        data[3] = ((highScore shr 24) and 0xFF).toByte()
        storage?.save("high-score", data)
    }
}
