import com.kengine.Vec2
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.event.EventContext
import com.kengine.graphics.Sprite
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import kotlin.random.Random

class PingPongPidgeyEntity : Entity {
    private val pidgeySprite = Sprite("assets/sprites/pidgey.bmp")
    override val p: Vec2 = Vec2()
    override val v: Vec2 = Vec2()
    override val width = pidgeySprite.width
    override val height = pidgeySprite.height
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
        useContext(EventContext.get()) {
            subscribe(Events.BULBASAUR_ROAR) { e: BulbasaurRoarEvent ->
                Logger.info { "Pidgey heard bulbasaur's roar of ${e.decibels}dB" }
                v *= 1.05 // 5% increase in velocity
            }
        }
        useContext(SDLContext.get()) {
            p.x = Random.nextInt(0, screenWidth - pidgeySprite.width).toDouble()
            p.y = Random.nextInt(0, screenHeight - pidgeySprite.height).toDouble()
            v.x = Random.nextInt(1, 30).toDouble() * if (Random.nextBoolean()) 1 else -1
            v.y = Random.nextInt(1, 30).toDouble() * if (Random.nextBoolean()) 1 else -1
        }
        state = State.BOUNCE
    }

    private fun bounce(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            p += v * elapsedSeconds
            if (p.x < 0 || p.x > screenWidth - width) {
                v.x *= -1
            }
            if (p.y < 0 || p.y > screenHeight - height) {
                v.y *= -1
            }
        }
    }

    override fun draw(elapsedSeconds: Double) {
        pidgeySprite.draw(p.x, p.y)
    }

    override fun cleanup() {
        pidgeySprite.cleanup()
    }

}