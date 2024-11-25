import com.kengine.Vec2
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.Sprite
import com.kengine.sdl.SDLContext
import kotlin.random.Random

class BouncingPokeballEntity : Entity {
    private val pokeballSprite = Sprite("assets/sprites/pokeball.bmp")
    override val p: Vec2 = Vec2()
    override val v: Vec2 = Vec2()
    override val width = pokeballSprite.width
    override val height = pokeballSprite.height
    private var state = State.INIT

    private enum class State {
        INIT,
        BOUNCE
    }

    /**
     * Demo useContext() pattern + state pattern
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

}