package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.time.getClockContext

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
    private val frameDurations: List<Long>, // Duration for each frame
    private val loopMode: LoopMode = LoopMode.WRAP_AROUND
) : Logging {

    private var currentFrameIndex = 0
    private var elapsedTime = 0.0
    private var pingPongDirection = 1 // 1 = forward, -1 = reverse

    private val totalDurationMs = frameDurations.sum() // total cycle duration
//    private val frameLookup: List<Double> = frameDurations.runningFold(0.0) { acc, duration -> acc + duration }

    fun update() {
        elapsedTime += getClockContext().deltaTimeMs
        when (loopMode) {
            LoopMode.WRAP_AROUND -> updateWrapAround()
            LoopMode.PING_PONG -> updatePingPong()
        }
    }

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) {
        sprites[currentFrameIndex].draw(p, flip)
    }

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE) {
        sprites[currentFrameIndex].draw(x, y, flip)
    }

    private fun updateWrapAround() {
        elapsedTime %= totalDurationMs // keep within total duration

        var frameElapsed = 0.0
        for (i in sprites.indices) {
            frameElapsed += frameDurations[i]
            if (elapsedTime < frameElapsed) {
                currentFrameIndex = i
                break
            }
        }
    }

    // about 1ms slower.
//    private fun updateWrapAround() {
//        elapsedTime %= totalDurationMs // wrap time into range [0, totalDurationMs]
//
//        // Binary search to find the current frame
//        val frameIndex = frameLookup.binarySearch { time -> time.compareTo(elapsedTime) }
//        currentFrameIndex = if (frameIndex >= 0) frameIndex else -(frameIndex + 2)
//    }

    private fun updatePingPong() {
        while (elapsedTime >= frameDurations[currentFrameIndex]) {
            elapsedTime -= frameDurations[currentFrameIndex]

            currentFrameIndex += pingPongDirection
            if (currentFrameIndex >= sprites.size || currentFrameIndex < 0) {
                pingPongDirection *= -1
                currentFrameIndex = (currentFrameIndex + pingPongDirection).coerceIn(0, sprites.size - 1)
            }
        }
    }

    fun cleanup() {
        sprites.forEach { it.cleanup() }
    }

    companion object {
        fun fromSprites(
            sprites: List<Sprite>,
            frameDurations: List<Long>,
            loopMode: LoopMode = LoopMode.WRAP_AROUND
        ): AnimatedSprite {
            require(sprites.size == frameDurations.size) {
                "Sprite count and frame duration count must match!"
            }
            return AnimatedSprite(sprites, frameDurations, loopMode)
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
            val durations = List(sprites.size) { frameDurationMs }
            return AnimatedSprite(sprites, durations, loopMode)
        }
    }
}
