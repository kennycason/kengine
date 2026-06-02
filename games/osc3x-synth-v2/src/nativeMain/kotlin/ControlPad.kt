import com.kengine.graphics.Color
import com.kengine.hooks.state.State
import com.kengine.log.Logging
import com.kengine.sound.synth.Oscillator
import com.kengine.ui.FlexDirection
import com.kengine.ui.useView

class ControlPad(
    private val x: Double = 0.0,
    private val y: Double = 0.0,
    private val osc3xSynth: Osc3xSynth,
    private val osc3xVfx: Osc3xVfx,
    private val masterVolume: State<Double>
) : Logging {

    val colors = Color.neon(8)

    private val controlPadView = useView(
        id = "visualization-controls",
        x = x,
        y = y,
        w = 190.0,
        h = 90.0,
        bgColor = Color.neonBlue,
        padding = 5.0,
        spacing = 5.0,
        direction = FlexDirection.ROW
    ) {
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-1",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[0],
                hoverColor = colors[colors.size - 1 - 0],
                onClick = {
                    logger.info("Clicked pad 1")
                    osc3xVfx.nextEffect()
                }
            )
            button(
                id = "visualisation-change-button-2",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[1],
                hoverColor = colors[colors.size - 1 - 1],
                onClick = {
                    logger.info("Clicked pad 2")
                    osc3xSynth.randomize()
                }
            )
        }
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-3",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[2],
                hoverColor = colors[colors.size - 1 - 2],
                onClick = {
                    logger.info("Clicked pad 3")
                    osc3xSynth.setConfig(0, frequency = 110.0, detune = 0.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(1, frequency = 110.0, detune = 50.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(2, frequency = 110.0, detune = -50.0, waveform = Oscillator.Waveform.SAW)
                }
            )
            button(
                id = "visualisation-change-button-4",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[3],
                hoverColor = colors[colors.size - 1 - 3],
                onClick = {
                    logger.info("Clicked pad 4")
                    osc3xSynth.setConfig(0, frequency = 98.0, detune = 30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(1, frequency = 98.0, detune = -30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(2, frequency = 196.0, detune = 0.0, waveform = Oscillator.Waveform.SQUARE)
                }
            )
        }
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-5",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[4],
                hoverColor = colors[colors.size - 1 - 4],
                onClick = {
                    logger.info("Clicked pad 5 - Sub Bass")
                    osc3xSynth.setConfig(0, frequency = 55.0, detune = 0.0, waveform = Oscillator.Waveform.SINE)
                    osc3xSynth.setConfig(1, frequency = 55.0, detune = 5.0, waveform = Oscillator.Waveform.SINE)
                    osc3xSynth.setConfig(2, frequency = 110.0, detune = 0.0, waveform = Oscillator.Waveform.SINE)
                }
            )
            button(
                id = "visualisation-change-button-6",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[5],
                hoverColor = colors[colors.size - 1 - 5],
                onClick = {
                    logger.info("Clicked pad 6 - Bright Lead")
                    osc3xSynth.setConfig(0, frequency = 440.0, detune = 0.0, waveform = Oscillator.Waveform.SQUARE)
                    osc3xSynth.setConfig(1, frequency = 880.0, detune = 0.0, waveform = Oscillator.Waveform.SQUARE, volume = 0.3)
                    osc3xSynth.setConfig(2, frequency = 1320.0, detune = 0.0, waveform = Oscillator.Waveform.SINE, volume = 0.15)
                }
            )
        }
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-7",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[6],
                hoverColor = colors[colors.size - 1 - 6],
                onClick = {
                    logger.info("Clicked pad 7 - Detuned Pad")
                    osc3xSynth.setConfig(0, frequency = 261.6, detune = 7.0, waveform = Oscillator.Waveform.TRIANGLE)
                    osc3xSynth.setConfig(1, frequency = 329.6, detune = -7.0, waveform = Oscillator.Waveform.TRIANGLE)
                    osc3xSynth.setConfig(2, frequency = 392.0, detune = 5.0, waveform = Oscillator.Waveform.TRIANGLE)
                }
            )
            button(
                id = "visualisation-change-button-8",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = colors[7],
                hoverColor = colors[colors.size - 1 - 7],
                onClick = {
                    logger.info("Clicked pad 8 - Fifth Power")
                    osc3xSynth.setConfig(0, frequency = 110.0, detune = 0.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(1, frequency = 165.0, detune = 10.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(2, frequency = 220.0, detune = -10.0, waveform = Oscillator.Waveform.SQUARE)
                }
            )
        }
        slider(
            id = "visualization-slider",
            w = 20.0,
            h = 80.0,
            min = 0.0,
            max = 1.0,
            state = masterVolume,
            bgColor = Color.neonPurple,
            trackWidth = 3.0,
            trackColor = Color.neonCyan,
            handleWidth = 14.0,
            handleHeight = 7.0,
            handleColor = Color.neonOrange,
            onValueChanged = { value ->
                logger.info("Adjusting master volume => $value")
            }
        )
    }

    fun update() {
    }

    fun draw() {
        controlPadView.draw()
    }

}
