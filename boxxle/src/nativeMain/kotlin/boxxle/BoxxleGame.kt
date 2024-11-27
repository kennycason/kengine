package boxxle

import com.kengine.Game
import com.kengine.action.ActionsContext
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext
import com.kengine.sound.Sound
import com.kengine.sound.SoundContext
import com.kengine.time.getCurrentTimestampMilliseconds

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
                .also { it.setVolume(50) }
            finishSound = manager.getSound(Sounds.FINISH)
                .also { it.setVolume(50) }
        }
    }

    override fun update() {
        when (state) {
            State.BEGIN_PLAY -> beginPlay()
            State.PLAY -> play()
            State.WAIT_FOR_FINISH_MUSIC_TO_FINISH -> {}
        }
    }

    override fun draw() {
        useContext(SDLContext.get()) {
            fillScreen(255u, 255u, 255u, 255u)

            useContext(BoxxleContext.get()) {
                level.draw()
                player.draw()
            }

            flipScreen()
        }
    }

    private fun beginPlay() {
        mainSound.loop()
        state = State.PLAY
    }

    private fun play() {
        useContext(BoxxleContext.get()) {
            player.update()

            useContext(KeyboardContext.get()) {
                if ((keyboard.isRPressed()) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    reloadLevel()
                }
                if (keyboard.isReturnPressed() && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    loadLevel((level.levelNumber + 1) % LEVEL_DATA.size)
                }
                if (keyboard.isSpacePressed() && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    loadLevel((level.levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
                if (keyboard.isTPressed()) {
                    mainSound.setVolume(mainSound.getVolume() + 1)
                    finishSound.setVolume(finishSound.getVolume() + 1) // TODO global sound volumes?
                }
                if (keyboard.isGPressed()) {
                    mainSound.setVolume(mainSound.getVolume() - 1)
                    finishSound.setVolume(finishSound.getVolume() - 1)
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
