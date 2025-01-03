
import com.kengine.entity.SpriteEntity
import com.kengine.event.useEventContext
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.hooks.context.getContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.time.ClockContext
import kotlin.random.Random

class PingPongPidgeyEntity : SpriteEntity(
    sprite = Sprite.fromFilePath("assets/sprites/pidgey.bmp")
), Logging {

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
        useEventContext {
            subscribe(Events.BULBASAUR_ROAR) { e: BulbasaurRoarEvent ->
                logger.info { "Pidgey heard bulbasaur's roar of ${e.decibels}dB" }
                v *= 1.05 // 5% increase in velocity
            }
        }
        useSDLContext {
            p.x = Random.nextInt(30, screenWidth - width - 30).toDouble()
            p.y = Random.nextInt(30, screenHeight - height - 30).toDouble()
            v.x = Random.nextInt(10, 40).toDouble() * if (Random.nextBoolean()) 1 else -1
            v.y = Random.nextInt(10, 40).toDouble() * if (Random.nextBoolean()) 1 else -1
        }
        state = State.BOUNCE
    }

    private fun bounce() {
        useSDLContext {
            p += v * getContext<ClockContext>().deltaTimeSec
            if (p.x < 20 || p.x + width + v.x > screenWidth) {
                v.x *= -1
            }
            if (p.y < 20 || p.y + height + v.y > screenHeight) {
                v.y *= -1
            }
        }

        if (v.x < 0) {
            flipMode = FlipMode.NONE
        } else {
            flipMode = FlipMode.HORIZONTAL
        }
    }

}
