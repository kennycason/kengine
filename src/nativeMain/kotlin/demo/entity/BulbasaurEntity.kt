package demo.entity

import com.kengine.context.useContext
import com.kengine.entity.SpriteEntity
import com.kengine.graphics.Sprite
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.sdl.SDLContext

class BulbasaurEntity : SpriteEntity(
    sprite = Sprite("images/bulbasaur.bmp")
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
            if (keyboardInput.isLeftPressed()) {
                v.x = -speed * elapsedSeconds
            }
            if (keyboardInput.isRightPressed()) {
                v.x = speed * elapsedSeconds
            }
            if (keyboardInput.isUpPressed()) {
                v.y = -speed * elapsedSeconds
            }
            if (keyboardInput.isDownPressed()) {
                v.y = speed * elapsedSeconds
            }
        }
        useContext(MouseContext.get()) {
            if (mouseInput.isLeftPressed() || mouseInput.isRightPressed()) {
                p.x = mouseInput.getCursor().x - width / 2
                p.y = mouseInput.getCursor().y - height / 2
            }
        }
        p.x += v.x
        p.y += v.y
    }

    private fun init() {
        useContext(SDLContext.get()) {
            p.x = screenWidth / 2.0 - width / 2.0
        }
        state = State.READY
    }
}