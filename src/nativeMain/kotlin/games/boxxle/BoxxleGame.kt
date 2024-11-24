package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext
import com.kengine.sound.Sound
import com.kengine.sound.SoundContext
import com.kengine.time.getCurrentTimestampMilliseconds
import platform.posix.sleep
import sdl2.SDLK_RETURN
import sdl2.SDLK_SPACE
import sdl2.SDLK_r

class BoxxleGame : Game {
    enum class State {
        BEGIN_PLAY, PLAY, FINISH
    }
    private var state = State.BEGIN_PLAY
    private var timeSinceOptionChange = 0L // TODO fix keyboard.timeSinceKeyPressed function
    private lateinit var mainSound: Sound
    private lateinit var finishSound: Sound
    init {
        useContext(SoundContext.get()) {
            manager.setSound("finish", Sound("sound/boxxle/finish.wav"))
            manager.setSound("main", Sound("sound/boxxle/main.wav"))
            manager.setSound("title", Sound("sound/boxxle/title.wav"))
            mainSound = manager.getSound("main")
            finishSound = manager.getSound("finish")
        }
    }

    override fun update(elapsedSeconds: Double) {
        when (state) {
            State.BEGIN_PLAY -> beginPlay()
            State.PLAY -> play(elapsedSeconds)
            State.FINISH -> finish()
        }
    }

    fun beginPlay() {
        mainSound.loop()
        state = State.PLAY
    }

    fun play(elapsedSeconds: Double) {
        useContext(BoxxleContext.get()) {
            player.update(elapsedSeconds)

            useContext(KeyboardContext.get()) {
                if (keyboard.isKeyPressed(SDLK_r) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    reloadLevel()
                }
                if (keyboard.isKeyPressed(SDLK_RETURN) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    loadLevel((level.levelNumber + 1) % LEVEL_DATA.size)
                }
                if (keyboard.isKeyPressed(SDLK_SPACE) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    loadLevel((level.levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
            }

            if (isLevelComplete()) {
                mainSound.stop()
                state = State.FINISH
            }
        }
    }

    private fun finish() {
        finishSound.play()
        sleep(6u) // track is 6 seconds long TODO handle more gracefully
        state = State.BEGIN_PLAY
        useContext(BoxxleContext.get()) {
            loadLevel((level.levelNumber + 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
        }
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            fillScreen(255u, 255u, 255u, 255u)

            useContext(BoxxleContext.get()) {
                level.draw(elapsedSeconds)
                player.draw(elapsedSeconds)
            }

            flipScreen()
        }
    }

    fun reloadLevel() {
        useContext(BoxxleContext.get()) {
            loadLevel(level.levelNumber)
        }
    }

    fun loadLevel(levelNumber: Int) {
        useContext(BoxxleContext.get()) {
            level = Level(levelNumber)
            player.p.set(level.start)
            player.setScale(level.data.scale)
        }
    }

    override fun cleanup() {
    }

    private fun isLevelComplete(): Boolean {
        useContext(BoxxleContext.get()) {
            for (goal in level.goals) {
                if (!level.boxes.any { it.p == goal }) {
                    return false // a goal doesn't have a box on it
                }
            }
            return true // all goals must have boxes
        }
    }

}
