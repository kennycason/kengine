package osc3x.v2

import com.kengine.Game
import com.kengine.font.getFontContext
import com.kengine.font.useFontContext
import com.kengine.graphics.Color
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext

class Osc3xGUIV2 : Game, Logging {

    init {
        useFontContext {
           addFont(Fonts.ARCADE_CLASSIC, Fonts.ARCADE_CLASSIC_TTF, fontSize = 16f)
        }
    }

    private val osc3xSynth: Osc3xSynthV2 = Osc3xSynthV2(
        x = 0.0, y = 0.0,
        font = getFontContext().getFont(Fonts.ARCADE_CLASSIC, 16f),
        defaultVolume = 0.25
    )

    private val osc3XVfx = Osc3xVfxV2(
        x = 0, y = osc3xSynth.height.toInt(),
        osc3xSynth = osc3xSynth
    )

    override fun update() {
        osc3xSynth.update()
        osc3XVfx.update()
        logger.info { "FPS: ${getClockContext().fps}" }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(Color.black)
            osc3XVfx.draw()
            osc3xSynth.draw()
            flipScreen()
        }
    }

    override fun cleanup() {}
}
