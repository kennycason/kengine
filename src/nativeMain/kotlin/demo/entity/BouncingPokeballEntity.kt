package demo.entity

import com.kengine.Vec2D
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.Sprite
import com.kengine.sdl.SDLContext
import kotlin.random.Random

class BouncingPokeballEntity : Entity {
    override val p: Vec2D = Vec2D()
    override val v: Vec2D = Vec2D()
    override val width: Int by lazy { pokeballSprite.width }
    override val height: Int by lazy { pokeballSprite.height }
    private var state = State.INIT

    private enum class State {
        INIT,
        BOUNCE
    }

    /**
     * Demo useKontext() pattern + state pattern
     */
    override fun update(elapsedSeconds: Double) {
        when (state) {
            State.INIT -> init()
            State.BOUNCE -> bounce(elapsedSeconds)
        }
    }

    private fun init() {
        useContext(SDLContext.get()) {
            p.x = Random.nextInt(0, screenWidth - pokeballSprite.width).toDouble()
            p.y = Random.nextInt(0, screenHeight - pokeballSprite.height).toDouble()
            v.x = Random.nextInt(1, 100).toDouble() * if (Random.nextBoolean()) 1 else -1
            v.y = Random.nextInt(1, 100).toDouble() * if (Random.nextBoolean()) 1 else -1
        }
        state = State.BOUNCE
    }

    private fun bounce(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            p.x += v.x * elapsedSeconds
            p.y += v.y * elapsedSeconds
            if (p.x < 0 || p.x > screenWidth - width) {
                v.x *= -1
            }
            if (p.y < 0 || p.y > screenHeight - height) {
                v.y *= -1
            }
        }
    }

    override fun draw(elapsedSeconds: Double) {
        pokeballSprite.draw(p.x, p.y)
    }

    override fun cleanup() {
        pokeballSprite.cleanup()
    }

    companion object {
        val pokeballSprite = Sprite("images/pokeball.bmp") // shared sprite
    }
}