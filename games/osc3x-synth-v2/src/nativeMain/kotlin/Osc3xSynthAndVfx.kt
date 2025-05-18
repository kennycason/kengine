import com.kengine.Game
import com.kengine.font.getFontContext
import com.kengine.font.useFontContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.input.keyboard.Keys
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.sound.keyboard.NoteGenerator
import com.kengine.sound.keyboard.VirtualKeyboard
import com.kengine.time.getCurrentMilliseconds

class Osc3xSynthAndVfx : Game, Logging {

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

    // Keyboard input handling
    private val keyboardNotes = mapOf(
        // Row 1: Number keys (1-9,0) - Accidental 1
        Keys.ONE to "C3", Keys.TWO to "D3", Keys.THREE to "E3", Keys.FOUR to "F3",
        Keys.FIVE to "G3", Keys.SIX to "A3", Keys.SEVEN to "B3", Keys.EIGHT to "C4",
        Keys.NINE to "D4", Keys.ZERO to "E4",

        // Row 2: QWERTY keys - Accidental 2
        Keys.Q to "C4", Keys.W to "D4", Keys.E to "E4", Keys.R to "F4",
        Keys.T to "G4", Keys.Y to "A4", Keys.U to "B4", Keys.I to "C5",
        Keys.O to "D5", Keys.P to "E5",

        // Row 3: ASDF keys - Accidental 3
        Keys.A to "C5", Keys.S to "D5", Keys.D to "E5", Keys.F to "F5",
        Keys.G to "G5", Keys.H to "A5", Keys.J to "B5", Keys.K to "C6",
        Keys.L to "D6",

        // Row 4: ZXCV keys - Accidental 4
        Keys.Z to "C6", Keys.X to "D6", Keys.C to "E6", Keys.V to "F6",
        Keys.B to "G6", Keys.N to "A6", Keys.M to "B6"
    )

    // Track currently pressed keys and their last press time
    private val activeKeys = mutableMapOf<UInt, Long>()
    private val keyRepeatDelay = 100L // milliseconds before a key can be triggered again

    override fun update() {
        osc3xSynth.update()
        osc3XVfx.update()
        controlPad.update()

        // Handle keyboard input
        useKeyboardContext {
            val currentTime = getCurrentMilliseconds()

            // Check each mapped key
            keyboardNotes.forEach { (keyCode, noteName) ->
                if (keyboard.isPressed(keyCode)) {
                    // If key is newly pressed or enough time has passed since last trigger
                    if (!activeKeys.containsKey(keyCode) ||
                        (currentTime - activeKeys[keyCode]!!) > keyRepeatDelay) {

                        // Parse note name to get note and octave
                        val note = noteName.substring(0, noteName.length - 1)
                        val octave = noteName.last().toString().toInt()

                        // Get frequency for this note
                        val frequency = NoteGenerator.getNoteFrequency(note, octave)

                        // Set frequency for all oscillators and trigger them
                        for (i in 0..2) {
                            osc3xSynth.osc3x.setFrequency(i, frequency)
                            osc3xSynth.osc3x.triggerNote(i)
                        }

                        // Update last press time
                        activeKeys[keyCode] = currentTime
                    }
                } else if (activeKeys.containsKey(keyCode)) {
                    // Key was released, release the note
                    for (i in 0..2) {
                        osc3xSynth.osc3x.releaseNote(i)
                    }
                    activeKeys.remove(keyCode)
                }
            }
        }
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
