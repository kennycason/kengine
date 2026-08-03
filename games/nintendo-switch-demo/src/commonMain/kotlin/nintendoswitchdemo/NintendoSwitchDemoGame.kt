package nintendoswitchdemo

import com.kengine.PortableGame
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext

private const val DESIGN_WIDTH = 1280
private const val DESIGN_HEIGHT = 720
const val DEMO_POKEBALL_SPRITE = "demo/pokeball"
const val DEMO_BLOCK_SPRITES = "demo/block-sprites"

class NintendoSwitchDemoGame : PortableGame {
    private var updates = 0
    private var draws = 0
    private var checksum = 0x4B_45_4E
    private var x = 180
    private var y = 140
    private var velocityX = 5
    private var velocityY = 3
    private var size = 96
    private var colorPhase = 0
    private val samples = IntArray(16)

    override fun update(input: InputState) {
        updates += 1

        val manualX = input.axis(InputButton.LEFT, InputButton.RIGHT)
        val manualY = input.axis(InputButton.UP, InputButton.DOWN)
        val manualSpeed = when {
            input.isPressed(InputButton.L) -> 5
            input.isPressed(InputButton.R) -> 15
            else -> 9
        }
        if (manualX != 0 || manualY != 0) {
            x += manualX * manualSpeed
            y += manualY * manualSpeed
        } else {
            x += velocityX
            y += velocityY
        }

        if (x < 36 || x > DESIGN_WIDTH - size - 36) {
            velocityX = -velocityX
            x = x.coerceIn(36, DESIGN_WIDTH - size - 36)
        }
        if (y < 36 || y > DESIGN_HEIGHT - size - 36) {
            velocityY = -velocityY
            y = y.coerceIn(36, DESIGN_HEIGHT - size - 36)
        }

        if (input.isPressed(InputButton.A)) {
            colorPhase += 9
        } else if (input.isPressed(InputButton.X)) {
            colorPhase += 13
        } else if (input.isPressed(InputButton.Y)) {
            colorPhase -= 7
        } else {
            colorPhase += 2
        }

        size = if (input.isPressed(InputButton.B)) {
            132 + wave(updates * 5) / 6
        } else {
            92 + wave(updates * 3) / 9
        }

        if (input.isPressed(InputButton.SELECT)) {
            checksum = checksum xor 0x53_45_4C
        }

        val sampleIndex = updates % samples.size
        val sample = mix(updates * 31, updates + checksum)
        samples[sampleIndex] = sample
        checksum = mix(checksum xor sample, updates)
    }

    override fun draw(render: RenderContext) {
        draws += 1
        val sample = samples[draws % samples.size]
        checksum = mix(checksum + sample, draws)

        val background = backgroundColor()
        val primary = primaryColor()
        val accent = accentColor()
        val hud = hudColor()
        val gradientBottom = colorMix(background, accent, 120, 255)
        val spriteX = scale(x, DESIGN_WIDTH, render.width)
        val spriteY = scale(y, DESIGN_HEIGHT, render.height)
        val spriteSize = renderSize(render)
        val inset = (spriteSize / 5).coerceIn(8, 32)
        val portableSpriteSize = (spriteSize * 3 / 5).coerceAtLeast(28)

        render.verticalGradient(background, gradientBottom, draws)

        for (stripe in 0 until 7) {
            val stripeX = ((stripe * 227) + draws * 3) % (render.width + 120) - 120
            render.fillRect(stripeX, 0, 28, render.height, colorMix(background, accent, 110, 255))
        }

        render.fillRect(0, 0, render.width, 8, hud)
        render.fillRect(0, render.height - 8, render.width, 8, hud)
        render.fillRect(0, 0, 8, render.height, hud)
        render.fillRect(render.width - 8, 0, 8, render.height, hud)

        render.fillRect(spriteX + 12, spriteY + 12, spriteSize, spriteSize, colorMix(background, primary, 72, 255))
        render.fillRect(spriteX, spriteY, spriteSize, spriteSize, primary)
        render.fillRect(spriteX + inset, spriteY + inset, spriteSize - inset * 2, spriteSize - inset * 2, accent)
        render.fillRect(spriteX + inset * 2, spriteY + inset * 2, spriteSize - inset * 4, spriteSize - inset * 4, hud)
        render.drawSprite(
            spriteId = RenderAssetId.sprite(DEMO_POKEBALL_SPRITE),
            x = spriteX + (spriteSize - portableSpriteSize) / 2,
            y = spriteY + (spriteSize - portableSpriteSize) / 2,
            width = portableSpriteSize,
            height = portableSpriteSize
        )

        render.fillRect(40, render.height - 76, barWidth(checksum xor 0x4920), 12, hud)
        render.fillRect(40, render.height - 56, barWidth(checksum xor updates), 12, primary)
        render.fillRect(40, render.height - 36, barWidth(checksum xor draws), 12, accent)
        render.drawSprite(
            spriteId = RenderAssetId.sprite(DEMO_BLOCK_SPRITES),
            x = render.width - 124,
            y = 36,
            width = 76,
            height = 76,
            frame = (draws / 8) % 28
        )

        val scanY = scanLineY(render)
        render.drawLine(render.width / 2, render.height / 2, spriteX + spriteSize / 2, spriteY + spriteSize / 2, hud)
        render.drawLine(36, scanY, render.width - 36, render.height - 1 - scanY, colorMix(primary, hud, 120, 255))
        render.drawText("KENGINE SWITCH", 36, 28, hud, 4)
        render.drawText("DPAD/WASD MOVE  A/X/Y COLOR  B SIZE  L/R SPEED", 36, 68, colorMix(hud, primary, 64, 255), 2)
        render.drawText("X:$x Y:$y U:$updates D:$draws", 36, 92, colorMix(hud, accent, 80, 255), 2)
    }

    override fun cleanup() {
        samples.fill(0)
    }

    fun checksum(): Int {
        return checksum
    }

    fun snapshot(): String {
        return "updates=$updates draws=$draws x=$x y=$y checksum=$checksum"
    }

    private fun renderSize(render: RenderContext): Int {
        val scaledWidth = scale(size, DESIGN_WIDTH, render.width)
        val scaledHeight = scale(size, DESIGN_HEIGHT, render.height)
        return minOf(scaledWidth, scaledHeight).coerceAtLeast(24)
    }

    private fun barWidth(seed: Int): Int {
        return 120 + ((seed and Int.MAX_VALUE) % 460)
    }

    private fun scanLineY(render: RenderContext): Int {
        return ((draws * 7) % render.height).coerceIn(0, render.height - 1)
    }

    private fun backgroundColor(): Int {
        return rgba(12 + wave(colorPhase) / 12, 18 + wave(colorPhase + 90) / 14, 30 + wave(colorPhase + 180) / 16)
    }

    private fun primaryColor(): Int {
        return rgba(80 + wave(colorPhase + 30) / 2, 90 + wave(colorPhase + 160) / 2, 120 + wave(colorPhase + 280) / 3)
    }

    private fun accentColor(): Int {
        return rgba(220 + wave(colorPhase + 310) / 12, 70 + wave(colorPhase + 50) / 2, 130 + wave(colorPhase + 210) / 2)
    }

    private fun hudColor(): Int {
        return rgba(245, 245, 235)
    }

    private fun mix(value: Int, salt: Int): Int {
        return (value * 1_103_515_245 + 12_345) xor salt
    }

    private fun wave(value: Int): Int {
        val wrapped = value and 511
        return if (wrapped < 256) wrapped else 511 - wrapped
    }

    private fun scale(value: Int, from: Int, to: Int): Int {
        if (to <= 0) return value
        return (value.toLong() * to.toLong() / from.toLong()).toInt()
    }

    private fun rgba(r: Int, g: Int, b: Int): Int {
        return (r.coerceIn(0, 255)) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (255 shl 24)
    }

    private fun colorMix(from: Int, to: Int, amount: Int, maximum: Int): Int {
        val safeMaximum = maximum.coerceAtLeast(1)
        val safeAmount = amount.coerceIn(0, safeMaximum)
        val inverse = safeMaximum - safeAmount

        val r = (((from shr 0) and 0xff) * inverse + ((to shr 0) and 0xff) * safeAmount) / safeMaximum
        val g = (((from shr 8) and 0xff) * inverse + ((to shr 8) and 0xff) * safeAmount) / safeMaximum
        val b = (((from shr 16) and 0xff) * inverse + ((to shr 16) and 0xff) * safeAmount) / safeMaximum
        return rgba(r, g, b)
    }
}
