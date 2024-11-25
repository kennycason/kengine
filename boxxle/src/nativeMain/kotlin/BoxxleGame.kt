package games.boxxle

import com.kengine.Game
import com.kengine.action.ActionsContext
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext
import com.kengine.sound.Sound
import com.kengine.sound.SoundContext
import com.kengine.time.getCurrentTimestampMilliseconds
import sdl2.SDLK_RETURN
import sdl2.SDLK_SPACE
import sdl2.SDLK_r

class BoxxleGame : Game {
    enum class State {
        BEGIN_PLAY, PLAY, WAIT_FOR_FINISH_MUSIC_TO_FINISH
    }
    private var state = State.BEGIN_PLAY
    private var timeSinceOptionChange = 0L // TODO fix keyboard.timeSinceKeyPressed function
    private lateinit var mainSound: Sound
    private lateinit var finishSound: Sound
    init {
        useContext(SoundContext.get()) {
            manager.setSound(Sounds.FINISH, Sound(Sounds.FINISH_WAV))
            manager.setSound(Sounds.MAIN, Sound(Sounds.MAIN_WAV))
            manager.setSound(Sounds.TITLE, Sound(Sounds.TITLE_WAV))
            mainSound = manager.getSound(Sounds.MAIN)
            finishSound = manager.getSound(Sounds.FINISH)
        }
    }

    override fun update(elapsedSeconds: Double) {
        when (state) {
            State.BEGIN_PLAY -> beginPlay()
            State.PLAY -> play(elapsedSeconds)
            State.WAIT_FOR_FINISH_MUSIC_TO_FINISH -> {}
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
                finishSound.play()
                ActionsContext.get().timer(6000) {
                    state = State.BEGIN_PLAY
                    loadLevel((level.levelNumber + 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
                state = State.WAIT_FOR_FINISH_MUSIC_TO_FINISH
            }
        }
    }

    private fun reloadLevel() {
        useContext(BoxxleContext.get()) {
            loadLevel(level.levelNumber)
        }
    }

    private fun loadLevel(levelNumber: Int) {
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
