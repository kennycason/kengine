
import com.kengine.context.useContext
import com.kengine.entity.SpriteEntity
import com.kengine.event.EventContext
import com.kengine.graphics.Sprite
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import sdl2.SDLK_SPACE

class BulbasaurEntity : SpriteEntity(
    sprite = Sprite("assets/sprites/bulbasaur.bmp")
) {
    private val speed = 100.0
    private var state = State.INIT

    private enum class State {
        INIT,
        READY
    }

    override fun update(elapsedSeconds: Double) {
        super.update(elapsedSeconds)
        when (state) {
            State.INIT -> init()
            State.READY -> ready(elapsedSeconds)
        }
    }

    private fun ready(elapsedSeconds: Double) {
        v.x *= 0.9
        v.y *= 0.9
        useContext(KeyboardContext.get()) {
            if (keyboard.isLeftPressed()) {
                v.x = -speed * elapsedSeconds
            }
            if (keyboard.isRightPressed()) {
                v.x = speed * elapsedSeconds
            }
            if (keyboard.isUpPressed()) {
                v.y = -speed * elapsedSeconds
            }
            if (keyboard.isDownPressed()) {
                v.y = speed * elapsedSeconds
            }
            if (keyboard.isKeyPressed(SDLK_SPACE)) {
                Logger.info { "Bulbasaur ROARED!" }
                useContext(EventContext.get()) {
                    publish(Events.BULBASAUR_ROAR, BulbasaurRoarEvent(decibels = 90.0))
                }
            }
        }
        useContext(MouseContext.get()) {
            if (mouse.isLeftPressed() || mouse.isRightPressed()) {
                p.x = mouse.getCursor().x - width / 2
                p.y = mouse.getCursor().y - height / 2
                Logger.info { "Move Bulbasaur to mouse cursor $p" }
            }
        }
        p.x += v.x
        p.y += v.y
    }

    private fun init() {
        useContext(SDLContext.get()) {
            p.x = screenWidth / 2.0 - width / 2.0
            p.y = screenHeight / 2.0 - height / 2.0
        }
        state = State.READY
    }
}