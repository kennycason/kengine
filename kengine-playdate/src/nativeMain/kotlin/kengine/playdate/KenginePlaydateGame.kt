package kengine.playdate

import com.kengine.Game
import com.kengine.getGameContext
import com.kengine.sdl.useSDLContext
import kengine.playdate.context.KenginePlaydateContext

class KenginePlaydateGame : Game {
    enum class State {
        INIT, PLAY
    }

    private var state = State.INIT

    init {
        getGameContext().registerContext(KenginePlaydateContext.get())
    }

    override fun update() {
        when (state) {
            State.INIT -> {}
            State.PLAY -> {}
        }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(255u, 255u, 255u)
            flipScreen()
        }
    }

    override fun cleanup() {
    }

}
