import com.kengine.Game
import com.kengine.font.getFontContext
import com.kengine.font.useFontContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.sound.keyboard.VirtualKeyboard

class Osc3xSynths : Game, Logging {

    init {
        useFontContext {
           addFont(Fonts.ARCADE_CLASSIC, Fonts.ARCADE_CLASSIC_TTF, fontSize = 13f)
        }
    }

    private val masterVolume = useState(0.5)

    private val osc3xSynth: Osc3xSynth = Osc3xSynth(
        x = 0.0, y = 0.0,
        font = getFontContext().getFont(Fonts.ARCADE_CLASSIC, 13f),
        masterVolume = masterVolume
    )

    private val keyboard = VirtualKeyboard(
        id = "keyboard",
        x = 0.0, y = 480.0 - 100.0,
        w = 640.0, h = 100.0,
        synth = osc3xSynth.osc3x,
        bgColor = Color.gray10,
        startOctave = 2,
        numOctaves = 4
    )

    private val osc3XVfx = Osc3xVfx(
        x = osc3xSynth.width.toInt(), y = 90,
        width = 640 - osc3xSynth.width.toInt(), height = 300,
        osc3xSynth = osc3xSynth,
    )

    private val controlPad = ControlPad(
        x = 445.0, y = 0.0,
        osc3xSynth = osc3xSynth,
        osc3xVfx = osc3XVfx,
        masterVolume = masterVolume
    )

    override fun update() {
        osc3xSynth.update()
        osc3XVfx.update()
        controlPad.update()
        // logger.info { "FPS: ${getClockContext().fps}" }
    }

    override fun draw() {
        useSDLContext {
            fillScreen(Color.black)
            osc3XVfx.draw()
            osc3xSynth.draw()
            controlPad.draw()
            keyboard.draw()
            flipScreen()
        }
    }

    override fun cleanup() {}
}
