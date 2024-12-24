package com.kengine.graphics

import com.kengine.hooks.context.getContext
import com.kengine.math.Vec2
import com.kengine.time.ClockContext

enum class LoopMode {
    /**
     *  Restart the animation from the first frame
     */
    WRAP_AROUND,

    /**
     * Reverse direction at the end of the animation
     */
    PING_PONG
}

class AnimatedSprite private constructor(
    private val sprites: List<Sprite>,
    private val frameDurationMs: Long,
    private val loopMode: LoopMode = LoopMode.WRAP_AROUND
) {
    private var currentFrameIndex = 0
    private var elapsedTime = 0.0
    private var pingPongDirection = 1 // 1 for forward, -1 for reverse

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) {
        updateFrame()
        sprites[currentFrameIndex].draw(p, flip)
    }

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE) {
        updateFrame()
        sprites[currentFrameIndex].draw(x, y, flip)
    }

    private fun updateFrame() {
        elapsedTime += getContext<ClockContext>().deltaTimeMs
        if (elapsedTime >= frameDurationMs) {
            elapsedTime -= frameDurationMs
            when (loopMode) {
                LoopMode.WRAP_AROUND -> updateWrapAround()
                LoopMode.PING_PONG -> updatePingPong()
            }
        }
    }

    private fun updateWrapAround() {
        currentFrameIndex = (currentFrameIndex + 1) % sprites.size
    }

    private fun updatePingPong() {
        currentFrameIndex += pingPongDirection
        if (currentFrameIndex == sprites.size || currentFrameIndex < 0) {
            pingPongDirection *= -1
            currentFrameIndex = (currentFrameIndex + pingPongDirection).coerceIn(0, sprites.size - 1)
        }
    }

    fun cleanup() {
        sprites.forEach { it.cleanup() }
    }

    companion object {
        fun fromSprites(
            sprites: List<Sprite>,
            frameDurationMs: Long,
            loopMode: LoopMode = LoopMode.WRAP_AROUND
        ): AnimatedSprite {
            return AnimatedSprite(sprites, frameDurationMs, loopMode)
        }

        fun fromSpriteSheet(
            spriteSheet: SpriteSheet,
            frameDurationMs: Long,
            loopMode: LoopMode = LoopMode.WRAP_AROUND
        ): AnimatedSprite {
            val sprites = mutableListOf<Sprite>()
            for (y in 0 until spriteSheet.rows) {
                for (x in 0 until spriteSheet.columns) {
                    sprites.add(spriteSheet.getTile(x, y))
                }
            }
            return AnimatedSprite(sprites, frameDurationMs, loopMode)
        }
    }

}
