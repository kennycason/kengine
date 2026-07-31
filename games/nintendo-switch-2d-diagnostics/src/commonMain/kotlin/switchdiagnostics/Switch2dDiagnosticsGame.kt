package switchdiagnostics

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioCommandBuffer
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext

const val DIAGNOSTICS_SPRITES = Switch2dDiagnosticsAssets.SPRITES_ID

class Switch2dDiagnosticsGame : PortableGame {
    override val assets = Switch2dDiagnosticsAssets

    private var frame = 0
    private var page = 0
    private var previousMask = 0
    private var stressEnabled = false
    private var stressCommands = 360
    private var musicEnabled = true
    private var musicVolume = 190
    private var stopMusicRequested = false
    private var lifecycleResets = 0
    private var audioBursts = 0
    private var checksum = 0x2D_44_49_41
    private var lastRenderCommands = 0
    private var lastRenderDropped = 0
    private var lastAudioCommands = 0
    private var lastAudioDropped = 0
    private val pendingSoundIds = IntArray(32)
    private var pendingSoundCount = 0

    override fun update(input: InputState) {
        frame += 1

        if (justPressed(input, InputButton.L)) {
            page = (page + PAGE_COUNT - 1) % PAGE_COUNT
        }
        if (justPressed(input, InputButton.R)) {
            page = (page + 1) % PAGE_COUNT
        }
        if (justPressed(input, InputButton.UP)) {
            stressCommands = (stressCommands + 120).coerceAtMost(1_320)
        }
        if (justPressed(input, InputButton.DOWN)) {
            stressCommands = (stressCommands - 120).coerceAtLeast(120)
        }
        if (justPressed(input, InputButton.START)) {
            stressEnabled = !stressEnabled
        }
        if (justPressed(input, InputButton.SELECT)) {
            resetDiagnostics()
        }

        if (justPressed(input, InputButton.A)) {
            queueSound(Sound.BLIP)
        }
        if (justPressed(input, InputButton.B)) {
            queueSound(Sound.CHORD)
        }
        if (justPressed(input, InputButton.Y)) {
            queueSound(Sound.NOISE)
        }
        if (justPressed(input, InputButton.X)) {
            musicEnabled = !musicEnabled
            if (!musicEnabled) {
                stopMusicRequested = true
            }
        }

        if (page == PAGE_AUDIO && stressEnabled && frame % 8 == 0) {
            queueSound(
                when ((frame / 8) % 3) {
                    0 -> Sound.BLIP
                    1 -> Sound.CHORD
                    else -> Sound.NOISE
                }
            )
            audioBursts += 1
        }

        musicVolume = if (page == PAGE_AUDIO) {
            80 + triangle(frame * 2, 140)
        } else {
            190
        }

        checksum = mix(checksum xor input.mask, frame + page * 31 + stressCommands)
        previousMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        if (stopMusicRequested) {
            audio.stopMusic(AudioAssetId.music(Switch2dDiagnosticsAssets.PULSE_ID))
            stopMusicRequested = false
        } else if (musicEnabled) {
            audio.loopMusic(AudioAssetId.music(Switch2dDiagnosticsAssets.PULSE_ID), musicVolume)
        }

        for (index in 0 until pendingSoundCount) {
            audio.playSound(pendingSoundIds[index], AudioCommandBuffer.MAX_VOLUME)
        }
        pendingSoundCount = 0
        lastAudioCommands = audio.commandCount
        lastAudioDropped = audio.droppedCommandCount
    }

    override fun draw(render: RenderContext) {
        render.verticalGradient(rgba(12, 18, 30), rgba(20, 92, 88), frame)
        drawHeader(render)

        when (page) {
            PAGE_VISUAL -> drawVisualMatrix(render)
            PAGE_TEXT -> drawTextAndLineMatrix(render)
            PAGE_AUDIO -> drawAudioMatrix(render)
            PAGE_PERF -> drawPerformanceMatrix(render)
        }

        if (stressEnabled) {
            drawStressCommands(render)
        }

        lastRenderCommands = render.commandCount
        lastRenderDropped = render.droppedCommandCount
    }

    override fun cleanup() {
        lifecycleResets += 1
        pendingSoundCount = 0
        previousMask = 0
        stopMusicRequested = true
    }

    fun snapshot(): String {
        return "page=$page frame=$frame render=$lastRenderCommands dropped=$lastRenderDropped audio=$lastAudioCommands stress=$stressCommands enabled=$stressEnabled resets=$lifecycleResets"
    }

    private fun drawHeader(render: RenderContext) {
        render.fillRect(0, 0, render.width, 86, rgba(7, 10, 18, 220))
        render.drawText("SWITCH 2D DIAGNOSTICS", 28, 18, WHITE, 3)
        render.drawText("PAGE ${page + 1}/$PAGE_COUNT ${pageName()}  L/R PAGE  START STRESS", 28, 54, CYAN, 2)
        render.drawText("CMD $lastRenderCommands DROP $lastRenderDropped  AUD $lastAudioCommands  STRESS $stressCommands", 700, 22, YELLOW, 2)
        render.drawText("A BLIP  B CHORD  Y NOISE  X MUSIC  UP/DOWN BUDGET", 700, 52, GREEN, 2)
    }

    private fun drawVisualMatrix(render: RenderContext) {
        val spriteId = RenderAssetId.sprite(DIAGNOSTICS_SPRITES)
        val top = 124
        val left = 48
        render.drawText("SPRITE ALPHA TINT SCALE CLIP OFFSCREEN FRAMES", left, top - 30, WHITE, 2)

        for (index in 0 until 12) {
            val x = left + (index % 6) * 150
            val y = top + (index / 6) * 150
            val size = 42 + (index % 4) * 18
            val tint = when (index % 4) {
                0 -> rgba(255, 255, 255, 255)
                1 -> rgba(255, 120, 120, 190)
                2 -> rgba(120, 255, 180, 150)
                else -> rgba(120, 180, 255, 110)
            }
            render.fillRect(x - 8, y - 8, 116, 116, rgba(8, 14, 24, 170))
            render.drawSprite(spriteId, x, y, size, size, tint, frame = index)
            render.drawText("F$index", x, y + 94, WHITE, 2)
        }

        render.drawText("EDGE CLIPS", 48, 474, YELLOW, 2)
        render.drawSprite(spriteId, -30, 500, 112, 112, rgba(255, 255, 255, 190), frame = (frame / 8) % 16)
        render.drawSprite(spriteId, render.width - 74, 500, 112, 112, rgba(255, 255, 255, 190), frame = ((frame / 8) + 1) % 16)
        render.drawSprite(spriteId, 230, render.height - 56, 120, 120, rgba(255, 255, 255, 190), frame = ((frame / 8) + 2) % 16)
        render.drawLine(24, 466, render.width - 24, 466, CYAN)
        render.drawLine(24, render.height - 24, render.width - 24, 500, MAGENTA)
    }

    private fun drawTextAndLineMatrix(render: RenderContext) {
        render.drawText("TEXT GLYPH COVERAGE", 48, 124, WHITE, 3)
        render.drawText("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 48, 174, CYAN, 2)
        render.drawText("0123456789 :-/.,+!_='", 48, 214, YELLOW, 2)
        render.drawText("SCALE 1", 48, 268, GREEN, 1)
        render.drawText("SCALE 2", 48, 300, GREEN, 2)
        render.drawText("SCALE 4", 48, 350, GREEN, 4)

        val centerX = render.width * 3 / 4
        val centerY = render.height / 2 + 24
        for (line in 0 until 24) {
            val angle = line * 15
            val x = centerX + wave(angle + frame, 220)
            val y = centerY + wave(angle + frame * 2, 160)
            render.drawLine(centerX, centerY, x, y, colorWheel(line))
        }
        render.fillRect(centerX - 120, centerY - 90, 240, 180, rgba(255, 255, 255, 26))
        render.fillRect(centerX - 80, centerY - 52, 160, 104, rgba(255, 64, 128, 82))
    }

    private fun drawAudioMatrix(render: RenderContext) {
        render.drawText("AUDIO COMMANDS", 48, 124, WHITE, 3)
        render.drawText("MUSIC ${if (musicEnabled) "ON" else "OFF"} VOL $musicVolume", 48, 180, CYAN, 2)
        render.drawText("LAST AUDIO COMMANDS $lastAudioCommands DROPPED $lastAudioDropped", 48, 220, YELLOW, 2)
        render.drawText("BURSTS $audioBursts  STRESS ${if (stressEnabled) "ON" else "OFF"}", 48, 260, GREEN, 2)

        val meterWidth = musicVolume * 3
        render.fillRect(48, 314, 765, 28, rgba(255, 255, 255, 32))
        render.fillRect(48, 314, meterWidth, 28, CYAN)

        val spriteId = RenderAssetId.sprite(DIAGNOSTICS_SPRITES)
        for (index in 0 until 16) {
            val x = 58 + index * 70
            val y = 400 + triangle(frame * 3 + index * 23, 80)
            render.drawSprite(spriteId, x, y, 52, 52, colorWheel(index), frame = index)
        }
    }

    private fun drawPerformanceMatrix(render: RenderContext) {
        render.drawText("PERFORMANCE BUDGET", 48, 124, WHITE, 3)
        render.drawText("PREV COMMANDS $lastRenderCommands DROPPED $lastRenderDropped", 48, 184, YELLOW, 2)
        render.drawText("TARGET STRESS $stressCommands  MODE ${if (stressEnabled) "ON" else "OFF"}", 48, 224, CYAN, 2)
        render.drawText("START TOGGLE  UP DOWN CHANGE COUNT", 48, 264, GREEN, 2)

        val width = (stressCommands.coerceAtMost(1_024) * 760) / 1_024
        render.fillRect(48, 326, 760, 34, rgba(255, 255, 255, 32))
        render.fillRect(48, 326, width, 34, if (stressCommands > 1_024) MAGENTA else GREEN)
        render.drawText("1024 COMMAND SWITCH HOST COPY LIMIT", 48, 382, WHITE, 2)

        val spriteId = RenderAssetId.sprite(DIAGNOSTICS_SPRITES)
        for (index in 0 until 48) {
            val x = 850 + (index % 8) * 46
            val y = 152 + (index / 8) * 46
            render.drawSprite(spriteId, x, y, 34, 34, colorWheel(index), frame = index % 16)
        }
    }

    private fun drawStressCommands(render: RenderContext) {
        val spriteId = RenderAssetId.sprite(DIAGNOSTICS_SPRITES)
        for (index in 0 until stressCommands) {
            val x = (index * 37 + frame * 5) % (render.width + 80) - 40
            val y = 96 + ((index * 19 + frame * 3) % (render.height - 96))
            if (index % 5 == 0) {
                render.drawSprite(spriteId, x, y, 18, 18, colorWheel(index), frame = index % 16)
            } else {
                render.fillRect(x, y, 14, 14, colorWheel(index))
            }
        }
    }

    private fun queueSound(sound: Sound) {
        if (pendingSoundCount >= pendingSoundIds.size) {
            return
        }
        pendingSoundIds[pendingSoundCount] = AudioAssetId.sound(sound.assetId)
        pendingSoundCount += 1
    }

    private fun resetDiagnostics() {
        lastRenderCommands = 0
        lastRenderDropped = 0
        lastAudioCommands = 0
        lastAudioDropped = 0
        audioBursts = 0
        lifecycleResets = 0
        checksum = 0x2D_44_49_41
    }

    private fun justPressed(input: InputState, button: InputButton): Boolean {
        val bit = InputState.bitFor(button)
        return (input.mask and bit) != 0 && (previousMask and bit) == 0
    }

    private fun pageName(): String {
        return when (page) {
            PAGE_VISUAL -> "VISUAL"
            PAGE_TEXT -> "TEXT"
            PAGE_AUDIO -> "AUDIO"
            else -> "PERF"
        }
    }

    private fun colorWheel(index: Int): Int {
        return when (index % 8) {
            0 -> rgba(255, 84, 84, 190)
            1 -> rgba(255, 180, 68, 190)
            2 -> rgba(250, 232, 88, 190)
            3 -> rgba(74, 222, 128, 190)
            4 -> rgba(45, 212, 191, 190)
            5 -> rgba(96, 165, 250, 190)
            6 -> rgba(192, 132, 252, 190)
            else -> rgba(244, 114, 182, 190)
        }
    }

    private fun wave(value: Int, radius: Int): Int {
        val wrapped = value and 511
        val triangle = if (wrapped < 256) wrapped else 511 - wrapped
        return ((triangle - 128) * radius) / 128
    }

    private fun triangle(value: Int, maximum: Int): Int {
        val wrapped = value % (maximum * 2)
        return if (wrapped < maximum) wrapped else maximum * 2 - wrapped
    }

    private fun mix(value: Int, salt: Int): Int {
        return (value * 1_103_515_245 + 12_345) xor salt
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return (r.coerceIn(0, 255)) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (a.coerceIn(0, 255) shl 24)
    }

    private enum class Sound(val assetId: String) {
        BLIP(Switch2dDiagnosticsAssets.BLIP_ID),
        CHORD(Switch2dDiagnosticsAssets.CHORD_ID),
        NOISE(Switch2dDiagnosticsAssets.NOISE_ID)
    }

    private companion object {
        const val PAGE_VISUAL = 0
        const val PAGE_TEXT = 1
        const val PAGE_AUDIO = 2
        const val PAGE_PERF = 3
        const val PAGE_COUNT = 4

        const val WHITE = -1
        const val CYAN = -0xff2d01
        const val YELLOW = -0x004b01
        const val GREEN = -0xb83a80
        const val MAGENTA = -0x2dff01
    }
}
