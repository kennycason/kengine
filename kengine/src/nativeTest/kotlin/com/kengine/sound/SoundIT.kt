package com.kengine.sound

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.file.File
import com.kengine.log.Logger
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.useTimer
import kotlin.test.Test

class SoundIT {

    @Test
    fun `basic sound test`() {
        createGameContext(
            title = "Sound Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                println(File.pwd())
                val sound = Sound("src/nativeTest/resources/assets/sounds/finish.wav")
                sound.play()

                object : Game {
                    override fun update() {
                        useTimer(6400L) {
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        logger.info { "Game loop ${getCurrentMilliseconds()}ms" }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
