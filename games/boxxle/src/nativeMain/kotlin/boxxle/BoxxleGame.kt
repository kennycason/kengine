package boxxle

import boxxle.context.BoxxleContext
import boxxle.context.getBoxxleContext
import boxxle.context.useBoxxleContext
import com.kengine.Game
import com.kengine.font.Font
import com.kengine.font.getFontContext
import com.kengine.font.useFontContext
import com.kengine.getGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.context.getContext
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.sound.Sound
import com.kengine.sound.useSoundContext
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.timeSinceMs
import com.kengine.time.useTimer

class BoxxleGame : Game, Logging {
    enum class State {
        INIT, PLAY, WAIT_FOR_FINISH_MUSIC_TO_FINISH
    }

    private var state = State.INIT
    private var timeSinceOptionChangeMs = 0L
    private lateinit var mainSound: Sound
    private lateinit var finishSound: Sound
    private val menuFont: Font by lazy {
        getFontContext().getFont(Fonts.ARCADE_CLASSIC, 32f)
    }
    private val inputDelayMs = 100L

    init {
        getGameContext().registerContext(BoxxleContext.get())
        useSoundContext {
            addSound(Sounds.FINISH, Sound(Sounds.FINISH_WAV))
            addSound(Sounds.MAIN, Sound(Sounds.MAIN_WAV))
            addSound(Sounds.TITLE, Sound(Sounds.TITLE_WAV))
            mainSound = getSound(Sounds.MAIN)
                .also { it.setVolume(20) }
            finishSound = getSound(Sounds.FINISH)
                .also { it.setVolume(20) }
        }
        useFontContext {
            addFont(Fonts.ARCADE_CLASSIC, Fonts.ARCADE_CLASSIC_TTF, fontSize = 32f)
        }
    }

    override fun update() {
        when (state) {
            State.INIT -> init()
            State.PLAY -> play()
            State.WAIT_FOR_FINISH_MUSIC_TO_FINISH -> {}
        }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(Color.white)

            useBoxxleContext {
                level.draw()
                player.draw()
            }

            val level = getBoxxleContext().level
            menuFont.drawText("LVL ${level.levelNumber}", 530, 440, r = 0x33u, g = 0x33u, b = 0x33u, a = 0xFFu)

            flipScreen()
        }
    }

    private fun init() {
        mainSound.loop()
        state = State.PLAY
    }

    private fun play() {
        useBoxxleContext {
            player.update()

            useKeyboardContext {
                if ((keyboard.isRPressed()) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    reloadLevel()
                }
                if (keyboard.isReturnPressed() && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    loadLevel((level.levelNumber + 1) % LEVEL_DATA.size)
                }
                if (keyboard.isSpacePressed() && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    loadLevel((level.levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
                if (keyboard.isTPressed()) {
                    mainSound.setVolume(mainSound.getVolume() + 1)
                    finishSound.setVolume(finishSound.getVolume() + 1) // TODO global sound volumes
                }
                if (keyboard.isGPressed()) {
                    mainSound.setVolume(mainSound.getVolume() - 1)
                    finishSound.setVolume(finishSound.getVolume() - 1)
                }
            }

            useControllerContext {
                if (controller.isButtonPressed(Buttons.X) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    reloadLevel()
                }
                if (controller.isButtonPressed(Buttons.R1) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    loadLevel((level.levelNumber + 1) % LEVEL_DATA.size)
                }
                if (controller.isButtonPressed(Buttons.L1) && timeSinceMs(timeSinceOptionChangeMs) > inputDelayMs) {
                    timeSinceOptionChangeMs = getCurrentMilliseconds()
                    loadLevel((level.levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
                if (controller.isButtonPressed(Buttons.R3)) {
                    mainSound.setVolume(mainSound.getVolume() + 1)
                    finishSound.setVolume(finishSound.getVolume() + 1)
                }
                if (controller.isButtonPressed(Buttons.L3)) {
                    mainSound.setVolume(mainSound.getVolume() - 1)
                    finishSound.setVolume(finishSound.getVolume() - 1)
                }
            }

            if (isLevelComplete()) {
                mainSound.stop()
                finishSound.play()
                useTimer(6000) {
                    state = State.INIT
                    loadLevel((level.levelNumber + 1 + LEVEL_DATA.size) % LEVEL_DATA.size)
                }
                state = State.WAIT_FOR_FINISH_MUSIC_TO_FINISH
            }
        }
    }

    private fun reloadLevel() {
        useBoxxleContext {
            loadLevel(level.levelNumber)
        }
    }

    private fun loadLevel(levelNumber: Int) {
        useBoxxleContext {
            level = Level(levelNumber)
            player.p.set(level.start)
            player.setScale(level.data.scale)
        }
    }

    override fun cleanup() {
    }

    private fun isLevelComplete(): Boolean {
        val level = getContext<BoxxleContext>().level
        for (goal in level.goals) {
            if (!level.boxes.any { it.p == goal }) {
                return false // a goal doesn't have a box on it
            }
        }
        return true // all goals must have boxes

    }

}
