package n64demo

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class N64SnakeGame : PortableGame {
    override val storageNamespace: String = "snake64"

    private var storage: PortableStorage? = null

    private var playerX = 152
    private var playerY = 120
    private var frame = 0
    private var colorIndex = 0
    private var highScore = 0
    private var score = 0
    private var previousInputMask = 0
    private val snake3D = N64ShapeSnake3D()

//    private val pokeball = RenderAssetId.sprite("pokeball")
    private val finishSound = AudioAssetId.sound("finish")
    private val chordSound = AudioAssetId.sound("chord")

    private val trailColors = intArrayOf(
        rgba(255, 64, 96, 70),
        rgba(72, 240, 130, 70),
        rgba(74, 164, 255, 70),
        rgba(255, 224, 70, 70)
    )

    private var pendingSound = NO_SOUND

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
        val startPressed = input.isPressed(InputButton.START)
        val aJustPressed = aPressed && (previousInputMask and InputState.bitFor(InputButton.A)) == 0
        val bJustPressed = bPressed && (previousInputMask and InputState.bitFor(InputButton.B)) == 0
        val xJustPressed = xPressed && (previousInputMask and InputState.bitFor(InputButton.X)) == 0
        val yJustPressed = yPressed && (previousInputMask and InputState.bitFor(InputButton.Y)) == 0
        val startJustPressed = startPressed && (previousInputMask and InputState.bitFor(InputButton.START)) == 0

        playerX += dx * speed
        playerY += dy * speed

        if (playerX < 0) playerX = 0
        if (playerY < 0) playerY = 0
        if (playerX + 16 > 320) playerX = 320 - 16
        if (playerY + 16 > 240) playerY = 240 - 16

        if (aJustPressed) {
            colorIndex = (colorIndex + 1) % trailColors.size
        }

        if (xJustPressed) {
            pendingSound = chordSound
        }

        if (yJustPressed) {
            pendingSound = finishSound
        }

        if (startJustPressed) {
            snake3D.reset()
            score = 0
            pendingSound = finishSound
        }

        snake3D.update(input, frame)
        score = snake3D.score
        if (snake3D.consumedThisFrame) {
            pendingSound = chordSound
            if (score > highScore) {
                highScore = score
                saveHighScore()
            }
        }

        frame++
        previousInputMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        if (pendingSound != NO_SOUND) {
            audio.playSound(pendingSound)
            pendingSound = NO_SOUND
        }
    }

    override fun draw(render: RenderContext) {
        render.verticalGradient(rgba(5, 8, 24), rgba(20, 18, 56), frame)

        val borderColor = rgba(68, 68, 136)
        render.fillRect(0, 0, 320, 2, borderColor)
        render.fillRect(0, 238, 320, 2, borderColor)
        render.fillRect(0, 0, 2, 240, borderColor)
        render.fillRect(318, 0, 2, 240, borderColor)

        snake3D.draw(render, frame)

        render.drawText("SNAKE 64", 12, 8, rgba(255, 255, 255), 2)
        render.drawText("Score: $score", 140, 8, rgba(218, 228, 255), 1)
        render.drawText("Hi: $highScore", 230, 8, rgba(218, 228, 255), 1)
      //  render.drawText("LEFT/RIGHT steer  UP/A boost  DOWN brake", 10, 214, rgba(186, 194, 218), 1)
      //  render.drawText("L/R camera  START reset  C sfx", 10, 226, rgba(186, 194, 218), 1)
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

    private fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return clampColor(red) or
            (clampColor(green) shl 8) or
            (clampColor(blue) shl 16) or
            (clampColor(alpha) shl 24)
    }

    private fun clampColor(value: Int): Int {
        return when {
            value < 0 -> 0
            value > 255 -> 255
            else -> value
        }
    }

    private companion object {
        const val NO_SOUND = 0
    }
}
