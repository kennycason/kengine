package osc3x

import com.kengine.Game
import com.kengine.graphics.Color
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.sound.synth.Osc3xSynth
import com.kengine.sound.synth.Osc3xVfx

class Osc3xGUI : Game, Logging {

    private val osc3xSynth = Osc3xSynth(
        x = 0.0, y = 0.0, defaultVolume = 0.05
    )
    private val osc3XVfx = Osc3xVfx(
        x = 0, y = osc3xSynth.height.toInt(),
        osc3xSynth = osc3xSynth
    )

    override fun update() {
        osc3xSynth.update()
        osc3XVfx.update()
    }

    override fun draw() {
        useSDLContext {
            fillScreen(Color.black)
            osc3xSynth.draw()
            osc3XVfx.draw()
            flipScreen()
        }
    }

    override fun cleanup() {}

}
