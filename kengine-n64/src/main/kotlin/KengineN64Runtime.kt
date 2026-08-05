import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.PortableGame
import com.kengine.render.RenderContext
import kengine.n64runtime.n64GameName
import kengine.n64runtime.createN64PortableGame
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.set

private const val INPUT_LEFT = 1
private const val INPUT_RIGHT = 1 shl 1
private const val INPUT_UP = 1 shl 2
private const val INPUT_DOWN = 1 shl 3
private const val INPUT_A = 1 shl 4
private const val INPUT_B = 1 shl 5
private const val INPUT_START = 1 shl 6
private const val INPUT_X = 1 shl 7
private const val INPUT_Y = 1 shl 8
private const val INPUT_L = 1 shl 9
private const val INPUT_R = 1 shl 10
private const val INPUT_SELECT = 1 shl 11
private const val COMMAND_CAPACITY = 256
private const val AUDIO_COMMAND_CAPACITY = 16

private class KengineN64Runtime {
    private val input = InputState()
    private val audio = AudioContext(AUDIO_COMMAND_CAPACITY)
    private val render = RenderContext(COMMAND_CAPACITY)
    private var game: PortableGame? = null
    private var started = false
    private var checksum = 0x4E_36_34

    fun start(): String {
        if (!started) {
            val createdGame = createN64PortableGame()
            createdGame.attachStorage(N64PortableStorage(createdGame.storageNamespace))
            game = createdGame
            started = true
            checksum = mix(checksum, n64GameName().length)
        }
        return "started ${snapshotPayload()}"
    }

    fun update(hostFrame: Int, inputMask: Int): Int {
        val activeGame = game ?: return -1
        inputFromMask(inputMask)
        activeGame.update(input)
        checksum = mix(checksum xor input.mask, hostFrame)
        return checksum
    }

    fun audio(hostFrame: Int): Int {
        val activeGame = game ?: return -1
        audio.beginFrame()
        activeGame.audio(audio)
        checksum = mix(checksum + audio.commandCount, audio.droppedCommandCount xor hostFrame)
        return checksum
    }

    fun draw(hostFrame: Int, screenWidth: Int, screenHeight: Int): Int {
        val activeGame = game ?: return -1
        render.beginFrame(screenWidth, screenHeight)
        activeGame.draw(render)
        checksum = mix(checksum + render.commandCount, render.droppedCommandCount xor hostFrame)
        return checksum
    }

    fun snapshot(): String {
        val state = if (started) "running" else "stopped"
        return "$state ${snapshotPayload()}"
    }

    fun cleanup(): String {
        val message = "cleanup ${snapshotPayload()}"
        game?.cleanup()
        game = null
        started = false
        return message
    }

    @OptIn(ExperimentalForeignApi::class)
    fun copyCommandsTo(destination: CPointer<IntVar>?, maxCommands: Int): Int {
        if (destination == null || maxCommands <= 0) {
            return 0
        }

        val commandLimit = minOf(render.commandCount, maxCommands)
        var outputIndex = 0
        var commandIndex = 0
        while (commandIndex < commandLimit) {
            var fieldIndex = 0
            while (fieldIndex < RenderContext.FIELD_COUNT) {
                destination[outputIndex] = render.commandField(commandIndex, fieldIndex)
                outputIndex += 1
                fieldIndex += 1
            }
            commandIndex += 1
        }
        return commandLimit
    }

    @OptIn(ExperimentalForeignApi::class)
    fun copyAudioCommandsTo(destination: CPointer<IntVar>?, maxCommands: Int): Int {
        if (destination == null || maxCommands <= 0) {
            return 0
        }

        val commandLimit = minOf(audio.commandCount, maxCommands)
        var outputIndex = 0
        var commandIndex = 0
        while (commandIndex < commandLimit) {
            var fieldIndex = 0
            while (fieldIndex < AudioContext.FIELD_COUNT) {
                destination[outputIndex] = audio.commandField(commandIndex, fieldIndex)
                outputIndex += 1
                fieldIndex += 1
            }
            commandIndex += 1
        }
        return commandLimit
    }

    fun commandText(commandIndex: Int): String {
        return render.commandText(commandIndex)
    }

    private fun inputFromMask(inputMask: Int) {
        input.reset()
        input.set(InputButton.LEFT, (inputMask and INPUT_LEFT) != 0)
        input.set(InputButton.RIGHT, (inputMask and INPUT_RIGHT) != 0)
        input.set(InputButton.UP, (inputMask and INPUT_UP) != 0)
        input.set(InputButton.DOWN, (inputMask and INPUT_DOWN) != 0)
        input.set(InputButton.A, (inputMask and INPUT_A) != 0)
        input.set(InputButton.B, (inputMask and INPUT_B) != 0)
        input.set(InputButton.START, (inputMask and INPUT_START) != 0)
        input.set(InputButton.X, (inputMask and INPUT_X) != 0)
        input.set(InputButton.Y, (inputMask and INPUT_Y) != 0)
        input.set(InputButton.L, (inputMask and INPUT_L) != 0)
        input.set(InputButton.R, (inputMask and INPUT_R) != 0)
        input.set(InputButton.SELECT, (inputMask and INPUT_SELECT) != 0)
    }

    private fun snapshotPayload(): String {
        return "${game?.let { n64GameName() } ?: "without game"} commands=${render.commandCount}/${render.droppedCommandCount} audio=${audio.commandCount}/${audio.droppedCommandCount} checksum=$checksum"
    }

    private fun mix(value: Int, salt: Int): Int {
        return (value * 1_103_515_245 + 12_345) xor salt
    }
}

private val kengineN64Runtime = KengineN64Runtime()

fun kengineN64RuntimeStart(): String {
    return kengineN64Runtime.start()
}

fun kengineN64RuntimeUpdate(hostFrame: Int, inputMask: Int): Int {
    return kengineN64Runtime.update(hostFrame, inputMask)
}

fun kengineN64RuntimeAudio(hostFrame: Int): Int {
    return kengineN64Runtime.audio(hostFrame)
}

fun kengineN64RuntimeDraw(hostFrame: Int, screenWidth: Int, screenHeight: Int): Int {
    return kengineN64Runtime.draw(hostFrame, screenWidth, screenHeight)
}

fun kengineN64RuntimeSnapshot(): String {
    return kengineN64Runtime.snapshot()
}

fun kengineN64RuntimeCleanup(): String {
    return kengineN64Runtime.cleanup()
}

@OptIn(ExperimentalForeignApi::class)
fun kengineN64RuntimeCopyCommands(destination: CPointer<IntVar>?, maxCommands: Int): Int {
    return kengineN64Runtime.copyCommandsTo(destination, maxCommands)
}

@OptIn(ExperimentalForeignApi::class)
fun kengineN64RuntimeCopyAudioCommands(destination: CPointer<IntVar>?, maxCommands: Int): Int {
    return kengineN64Runtime.copyAudioCommandsTo(destination, maxCommands)
}

fun kengineN64RuntimeCommandText(commandIndex: Int): String {
    return kengineN64Runtime.commandText(commandIndex)
}
