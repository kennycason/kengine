package n64demo

import com.kengine.PortableGame
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class N64DemoGame : PortableGame {
    override val storageNamespace: String = "n64demo"

    private var storage: PortableStorage? = null

    private var squareX = 120
    private var squareY = 80
    private var squareSize = 24
    private var frame = 0
    private var colorIndex = 0
    private var highScore = 0
    private var score = 0

    private val colors = intArrayOf(
        0x00FF00FF.toInt(),
        0xFF0000FF.toInt(),
        0x0000FFFF.toInt(),
        0xFFFF00FF.toInt(),
        0xFF00FFFF.toInt(),
        0xFFFFFFFF.toInt()
    )

    override fun attachStorage(storage: PortableStorage) {
        this.storage = storage
        loadHighScore()
    }

    override fun update(input: InputState) {
        val speed = 2
        val dx = input.axis(InputButton.LEFT, InputButton.RIGHT)
        val dy = input.axis(InputButton.UP, InputButton.DOWN)

        squareX += dx * speed
        squareY += dy * speed

        if (squareX < 0) squareX = 0
        if (squareY < 0) squareY = 0
        if (squareX + squareSize > 320) squareX = 320 - squareSize
        if (squareY + squareSize > 240) squareY = 240 - squareSize

        if (input.isPressed(InputButton.A)) {
            colorIndex = (colorIndex + 1) % colors.size
        }

        if (input.isPressed(InputButton.B)) {
            squareSize = if (squareSize < 48) squareSize + 2 else 8
        }

        if (dx != 0 || dy != 0) {
            score++
            if (score > highScore) {
                highScore = score
                saveHighScore()
            }
        }

        frame++
    }

    override fun audio(audio: AudioContext) {
    }

    override fun draw(render: RenderContext) {
        render.clear(0x101030FF.toInt())

        render.fillRect(squareX, squareY, squareSize, squareSize, colors[colorIndex])

        val borderColor = 0x444488FF.toInt()
        render.fillRect(0, 0, 320, 2, borderColor)
        render.fillRect(0, 238, 320, 2, borderColor)
        render.fillRect(0, 0, 2, 240, borderColor)
        render.fillRect(318, 0, 2, 240, borderColor)

        render.drawText("Kengine N64", 100, 8, 0xFFFFFFFF.toInt(), 2)
        render.drawText("D-Pad: move  A: color  B: size", 20, 226, 0xAAAAAAFF.toInt(), 1)
        render.drawText("Score: $score  Hi: $highScore", 20, 30, 0xCCCCCCFF.toInt(), 1)
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
