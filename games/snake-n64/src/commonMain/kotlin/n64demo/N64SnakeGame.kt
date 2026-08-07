package n64demo

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class N64SnakeGame : PortableGame {
    override val storageNamespace: String = "snake64"

    private var storage: PortableStorage? = null

    private var frame = 0
    private var highScore = 0
    private var score = 0
    private var previousInputMask = 0
    private val snake3D = N64ShapeSnake3D()

    private val collectSound = AudioAssetId.sound("collect")

    private var pendingSound = NO_SOUND

    override fun attachStorage(storage: PortableStorage) {
        this.storage = storage
        loadHighScore()
    }

    override fun update(input: InputState) {
        val startPressed = input.isPressed(InputButton.START)
        val startJustPressed = startPressed && (previousInputMask and InputState.bitFor(InputButton.START)) == 0

        if (startJustPressed) {
            snake3D.reset()
            score = 0
        }

        snake3D.update(input, frame)
        score = snake3D.score
        if (snake3D.consumedThisFrame) {
            pendingSound = collectSound
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
//        render.drawText("Stick/D-pad turn  A/B height", 10, 216, rgba(186, 194, 218), 1)
//        render.drawText("C-UP/DN zoom  Z/L/R camera", 10, 228, rgba(186, 194, 218), 1)
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
