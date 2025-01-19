package osc3x.v2

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

    private val controlPadView = useView(
        id = "visualization-controls",
        x = x,
        y = y,
        w = 110.0,
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
                bgColor = Color.neonMagenta,
                hoverColor = Color.neonPurple,
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
                bgColor = Color.neonTurquoise,
                hoverColor = Color.neonMagenta,
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
                bgColor = Color.neonPink,
                hoverColor = Color.neonMagenta,
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
                bgColor = Color.neonOrange,
                hoverColor = Color.neonMagenta,
                onClick = {
                    logger.info("Clicked pad 4")
                    osc3xSynth.setConfig(0, frequency = 98.0, detune = 30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(1, frequency = 98.0, detune = -30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.setConfig(2, frequency = 196.0, detune = 0.0, waveform = Oscillator.Waveform.SQUARE)
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
