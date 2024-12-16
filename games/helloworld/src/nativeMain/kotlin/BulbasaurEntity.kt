
import com.kengine.entity.SpriteEntity
import com.kengine.event.useEventContext
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.input.mouse.useMouseContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.useGameContext

class BulbasaurEntity : SpriteEntity(
    sprite = Sprite.fromFilePath("assets/sprites/bulbasaur.bmp")
), Logging {
    private val speed = 100.0
    private var state = State.INIT

    private enum class State {
        INIT,
        READY
    }

    override fun update() {
        super.update()
        when (state) {
            State.INIT -> init()
            State.READY -> ready()
        }
    }

    private fun ready() {
        v.x *= 0.9
        v.y *= 0.9
        val clock = getClockContext()
        useKeyboardContext {
            if (keyboard.isLeftPressed() || keyboard.isAPressed()) {
                v.x = -speed * clock.deltaTimeSec
                flipMode = FlipMode.NONE
            }
            if (keyboard.isRightPressed() || keyboard.isDPressed()) {
                v.x = speed * clock.deltaTimeSec
                flipMode = FlipMode.HORIZONTAL
            }
            if (keyboard.isUpPressed() || keyboard.isWPressed()) {
                v.y = -speed * clock.deltaTimeSec
            }
            if (keyboard.isDownPressed() || keyboard.isSPressed()) {
                v.y = speed * clock.deltaTimeSec
            }
            if (keyboard.isRPressed()) {
                angle += 1.0
            }
            if (keyboard.isFPressed()) {
                angle -= 1.0
            }
            if (keyboard.isSpacePressed()) {
                logger.info { "Bulbasaur ROARED!" }
                useEventContext {
                    publish(Events.BULBASAUR_ROAR, BulbasaurRoarEvent(decibels = 90.0))
                }
            }
            if (keyboard.isEscapePressed()) {
                useGameContext {
                    isRunning = false
                }
            }
        }
        useMouseContext {
            if (mouse.isLeftPressed() || mouse.isRightPressed()) {
                p.x = mouse.getCursor().x - width / 2
                p.y = mouse.getCursor().y - height / 2
            }
        }
        p.x += v.x
        p.y += v.y

        angle += 1.0
    }

    private fun init() {
        useSDLContext {
            p.x = screenWidth / 2.0 - width / 2.0
            p.y = screenHeight / 2.0 - height / 2.0
        }
        state = State.READY
    }
}