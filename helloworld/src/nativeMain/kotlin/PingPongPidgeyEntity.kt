
import com.kengine.context.useContext
import com.kengine.entity.SpriteEntity
import com.kengine.event.EventContext
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import com.kengine.time.ClockContext
import kotlin.random.Random

class PingPongPidgeyEntity : SpriteEntity(
    sprite = Sprite("assets/sprites/pidgey.bmp")
) {

    private var state = State.INIT

    private enum class State {
        INIT,
        BOUNCE
    }

    /**
     * Demo useContext() pattern + state pattern
     */
    override fun update() {
        when (state) {
            State.INIT -> init()
            State.BOUNCE -> bounce()
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
            p.x = Random.nextInt(0, screenWidth - width).toDouble()
            p.y = Random.nextInt(0, screenHeight - height).toDouble()
            v.x = Random.nextInt(10, 60).toDouble() * if (Random.nextBoolean()) 1 else -1
            v.y = Random.nextInt(10, 60).toDouble() * if (Random.nextBoolean()) 1 else -1
        }
        state = State.BOUNCE
    }

    private fun bounce() {
        useContext(SDLContext.get()) {
            p += v * ClockContext.get().deltaTimeSec
            if (p.x < 0 || p.x > screenWidth - width) {
                v.x *= -1
            }
            if (p.y < 0 || p.y > screenHeight - height) {
                v.y *= -1
            }
        }

        Logger.info { "${v}" }

        if (v.x < 0) {
            flipMode = FlipMode.NONE
        } else {
            flipMode = FlipMode.HORIZONTAL
        }
    }

}