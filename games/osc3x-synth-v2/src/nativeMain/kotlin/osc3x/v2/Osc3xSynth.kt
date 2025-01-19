package osc3x.v2

import com.kengine.font.Font
import com.kengine.graphics.Color
import com.kengine.hooks.effect.useEffect
import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.sound.synth.Osc3x
import com.kengine.sound.synth.Oscillator
import com.kengine.ui.Align
import com.kengine.ui.FlexDirection
import com.kengine.ui.View
import com.kengine.ui.useView
import osc3x.v2.Osc3xSynth.Styles.Common.PANEL_PADDING
import osc3x.v2.Osc3xSynth.Styles.Common.PANEL_SPACING
import osc3x.v2.Osc3xSynth.Styles.Common.PANEL_WIDTH

class Osc3xSynth(
    private val x: Double = 0.0,
    private val y: Double = 0.0,
    private val font: Font,
    private val masterVolume: State<Double>// = useState(0.5)
) : Logging {
    val width: Double = PANEL_WIDTH * 3 + PANEL_PADDING * 2 + PANEL_SPACING * 2
    val height: Double = 380.0

    private object Styles {
        // Common sizes used across sections
        object Common {
            const val PANEL_WIDTH = 140.0
            const val PANEL_SPACING = 4.0
            const val PANEL_PADDING = 4.0
            const val CONTROL_SPACING = 2.0
            const val SMALL_BUTTON_SIZE = 20.0
            const val LABEL_HEIGHT = 12.0
        }

        // Main oscillator controls section
        object Main {
            const val SECTION_WIDTH = Common.PANEL_WIDTH - PANEL_PADDING * 2
            const val SECTION_HEIGHT = 120.0
            const val SLIDER_WIDTH = 20.0
            const val SLIDER_HEIGHT = 70.0
            const val KNOB_SIZE = 20.0
            const val KNOB_FREQ_DRAG_SCALE = 1000.0
            const val KNOB_DETUNE_DRAG_SCALE = 400.0
            const val WAVE_BUTTONS_SPACING = 2.0
        }

        // Effects section (LFO & Filter)
        object Effects {
            const val SECTION_WIDTH = Common.PANEL_WIDTH - PANEL_PADDING * 4 - Common.PANEL_SPACING
            const val SECTION_HEIGHT = 140.0
            const val HEADER_HEIGHT = 15.0
            const val SLIDER_WIDTH = 20.0
            const val SLIDER_HEIGHT = 60.0
            const val KNOB_SIZE = 20.0
            const val KNOB_FREQ_DRAG_SCALE = 100.0
            const val KNOB_RES_DRAG_SCALE = 200.0
        }

        // ADSR envelope section
        object ADSR {
            const val SECTION_WIDTH = Common.PANEL_WIDTH// - PANEL_PADDING * 2
            const val SECTION_HEIGHT = 100.0
            const val HEADER_HEIGHT = 15.0
            const val SLIDER_WIDTH = 20.0
            const val SLIDER_HEIGHT = 60.0
        }
    }

    private object Colors {
        val background = Color(0x1Au, 0x1Au, 0x1Au)
        val panelBg = Color(0x2Au, 0x2Au, 0x2Au)
        val moduleBg = Color(0x3Au, 0x3Au, 0x3Au)

        val mainSlider = Color.neonPurple
        val effectSlider = Color.neonBlue
        val adsrSlider = Color.neonCyan
        val knobs = Color.neonOrange
        val disabledButton = Color.neonOrange
        val hoverButton = Color.neonPurple
        val enabledButton = Color.neonGreen
        val waveformButton = Color.neonPink
        val labelText = Color.neonYellow
        val valueText = Color.white
    }

    val osc3x = Osc3x()

    private val synthView: View

    private data class OscillatorState(
        val enabled: State<Boolean>,
        val volume: State<Double>,
        val frequency: State<Double>,
        val detune: State<Double>,
        val waveform: State<Oscillator.Waveform>,
        val lfoEnabled: State<Boolean>,
        val lfoFreq: State<Double>,
        val lfoAmp: State<Double>,
        val filterEnabled: State<Boolean>,
        val filterCutoff: State<Double>,
        val filterRes: State<Double>,
        val adsrEnabled: State<Boolean>,
        val attack: State<Double>,
        val decay: State<Double>,
        val sustain: State<Double>,
        val release: State<Double>
    )

    private val oscillatorStates = Array(3) { index ->
        OscillatorState(
            enabled = useState(true).also { state ->
                state.subscribe { enabled -> osc3x.setEnabled(index, enabled) }
            },
            volume = useState(masterVolume.get()).also { state ->
                state.subscribe { volume -> osc3x.setVolume(index, volume) }
            },
            frequency = useState(440.0).also { state ->
                state.subscribe { frequency -> osc3x.setFrequency(index, frequency) }
            },
            detune = useState(0.0).also { state ->
                state.subscribe { detune -> osc3x.setDetune(index, detune) }
            },
            waveform = useState(Oscillator.Waveform.SINE).also { state ->
                state.subscribe { waveform -> osc3x.setWaveform(index, waveform) }
            },
            lfoEnabled = useState(false).also { state ->
                state.subscribe { enabled ->
                    osc3x.withOscillator(index) { enableLFO(enabled) }
                }
            },
            lfoFreq = useState(5.0).also { state ->
                state.subscribe { freq ->
                    osc3x.withOscillator(index) { setLFO(frequency = freq) }
                }
            },
            lfoAmp = useState(0.5).also { state ->
                state.subscribe { amp ->
                    osc3x.withOscillator(index) { setLFO(amplitude = amp) }
                }
            },
            filterEnabled = useState(true).also { state ->
                state.subscribe { enabled ->
                    osc3x.withOscillator(index) { enableFilter(enabled) }
                }
            },
            filterCutoff = useState(1000.0).also { state ->
                state.subscribe { cutoff ->
                    osc3x.withOscillator(index) { setFilterCutoff(cutoff) }
                }
            },
            filterRes = useState(0.5).also { state ->
                state.subscribe { res ->
                    osc3x.withOscillator(index) { setFilterResonance(res) }
                }
            },
            adsrEnabled = useState(true).also { state ->
                state.subscribe { enabled ->
                    osc3x.withOscillator(index) { enableADSR(enabled) }
                }
            },
            attack = useState(0.1).also { state ->
                state.subscribe { attack ->
                    osc3x.withOscillator(index) { setADSR(attack = attack) }
                }
            },
            decay = useState(0.1).also { state ->
                state.subscribe { decay ->
                    osc3x.withOscillator(index) { setADSR(decay = decay) }
                }
            },
            sustain = useState(0.7).also { state ->
                state.subscribe { sustain ->
                    osc3x.withOscillator(index) { setADSR(sustain = sustain) }
                }
            },
            release = useState(0.3).also { state ->
                state.subscribe { release ->
                    osc3x.withOscillator(index) { setADSR(release = release) }
                }
            }
        )
    }

    init {
        osc3x.setMasterVolume(masterVolume.get())
        useEffect({
            osc3x.setMasterVolume(masterVolume.get())
        }, masterVolume)

        for (i in 0..2) {
            syncOscillatorConfig(i)
        }

        synthView = useView(
            id = "osc3xsynth-v2-container",
            x = x,
            y = y,
            w = width,
            h = height,
            direction = FlexDirection.ROW,
            padding = Styles.Common.PANEL_SPACING,
            spacing = Styles.Common.PANEL_SPACING,
            bgColor = Colors.background
        ) {
            for (i in 0..2) {
                buildOscillatorPanel(i)
            }
        }
    }

    private fun View.buildOscillatorPanel(index: Int) {
//        val panelWidth = PANEL_WIDTH - PANEL_PADDING * 2
        val panelHeight = height - Styles.Common.PANEL_SPACING * 2

        view(
            id = "osc-$index-panel",
//            w = panelWidth,
            h = panelHeight,
            bgColor = Colors.panelBg,
            padding = Styles.Common.PANEL_SPACING,
            spacing = Styles.Common.PANEL_SPACING,
            direction = FlexDirection.COLUMN
        ) {
            buildMainControls(index, oscillatorStates[index])
            buildEffectsSection(index, oscillatorStates[index])
            buildADSRSection(index, oscillatorStates[index])
        }
    }

    private fun View.buildMainControls(index: Int, state: OscillatorState) {
        val panelWidth = PANEL_WIDTH - PANEL_PADDING * 2
        val panelHeight = Styles.Main.SECTION_HEIGHT
        view(
            id = "osc-$index-main-controls",
            w = panelWidth,
            h = panelHeight,
            bgColor = Color.neonBlue, // Colors.moduleBg,
            padding = Styles.Common.PANEL_SPACING,
            spacing = Styles.Common.CONTROL_SPACING,
            direction = FlexDirection.ROW
        ) {
            buildControlColumn(
                "V",
                state.volume,
                0.0, 1.0,
                enableState = state.enabled,
                sliderColor = Colors.mainSlider
            )
            buildControlColumn(
                "F",
                state.frequency,
                20.0, 20000.0,
                hasKnob = true,
                dragScale = Styles.Main.KNOB_FREQ_DRAG_SCALE,
                sliderColor = Colors.mainSlider
            )
            buildControlColumn(
                "D",
                state.detune,
                -100.0, 100.0,
                hasKnob = true,
                dragScale = Styles.Main.KNOB_DETUNE_DRAG_SCALE,
                sliderColor = Colors.mainSlider
            )
            buildWaveformButtons(index, state)
        }
    }

    private fun View.buildControlColumn(
        label: String,
        state: State<Double>,
        min: Double,
        max: Double,
        hasKnob: Boolean = false,
        dragScale: Double = 200.0,
        enableState: State<Boolean>? = null,
        sliderColor: Color = Colors.mainSlider,
        height: Double = Styles.Main.SECTION_HEIGHT,
        sliderHeight: Double = Styles.Main.SLIDER_HEIGHT
    ) {
        view(
            id = "${label.lowercase()}-column",
            w = Styles.Main.SLIDER_WIDTH + Styles.Common.CONTROL_SPACING,
            h = if (hasKnob) height else sliderHeight,
            direction = FlexDirection.COLUMN,
            spacing = Styles.Main.WAVE_BUTTONS_SPACING
        ) {
            text(
                id = "${label.lowercase()}-label",
                w = Styles.Main.SLIDER_WIDTH + Styles.Common.CONTROL_SPACING,
                h = Styles.Common.LABEL_HEIGHT,
                text = label,
                font = font,
                textColor = Colors.labelText,
                align = Align.CENTER
            )

            slider(
                id = "${label.lowercase()}-slider",
                w = Styles.Main.SLIDER_WIDTH,
                h = sliderHeight,
                min = min,
                max = max,
                state = state,
                bgColor = sliderColor
            )

            if (hasKnob) {
                knob(
                    id = "${label.lowercase()}-knob",
                    w = Styles.Main.KNOB_SIZE,
                    h = Styles.Main.KNOB_SIZE,
                    min = min,
                    max = max,
                    state = state,
                    dragScale = dragScale,
                    bgColor = Colors.knobs
                )
            }

            enableState?.let {
                button(
                    id = "${label.lowercase()}-enable",
                    w = Styles.Common.SMALL_BUTTON_SIZE,
                    h = Styles.Common.SMALL_BUTTON_SIZE,
                    bgColor = Colors.disabledButton,
                    hoverColor = Colors.hoverButton,
                    pressColor = Colors.enabledButton,
                    isToggle = true,
                    isPressed = it
                )
            }
        }
    }

    private fun View.buildWaveformButtons(index: Int, state: OscillatorState) {
        view(
            id = "waveform-$index-column",
            w = Styles.Common.SMALL_BUTTON_SIZE,
            h = Styles.Main.SECTION_HEIGHT,
            spacing = Styles.Common.CONTROL_SPACING,
            direction = FlexDirection.COLUMN
        ) {
            text(
                id = "wave-label",
                w = Styles.Common.SMALL_BUTTON_SIZE,
                h = Styles.Common.LABEL_HEIGHT,
                text = "W",
                font = font,
                textColor = Colors.labelText,
                align = Align.CENTER
            )

            Oscillator.Waveform.entries.forEach { waveform ->
                button(
                    id = "waveform-$index-$waveform",
                    w = Styles.Common.SMALL_BUTTON_SIZE,
                    h = Styles.Common.SMALL_BUTTON_SIZE,
                    bgColor = Colors.disabledButton,
                    hoverColor = Colors.hoverButton,
                    pressColor = Colors.enabledButton,
                    isToggle = true,
                    onToggle = { enabled ->
                        if (enabled) state.waveform.set(waveform)
                    }
                )
            }
        }
    }

    private fun View.buildEffectsSection(index: Int, state: OscillatorState) {
        val panelWidth = PANEL_WIDTH - PANEL_PADDING * 2
        val panelHeight = Styles.Effects.SECTION_HEIGHT - PANEL_PADDING * 2
        view(
            id = "effects-section-$index",
            w = panelWidth,
            h = panelHeight,
            bgColor = Color.neonPink, // Colors.moduleBg,
//            padding = Styles.Common.PANEL_PADDING,
//            spacing = Styles.Common.PANEL_SPACING,
            direction = FlexDirection.ROW
        ) {
            // LFO Panel
            buildEffectPanel(
                "LFO",
                panelWidth / 2,
                state.lfoEnabled,
                listOf(
                    EffectControl(
                        "F",
                        state.lfoFreq,
                        0.1, 20.0,
                        Styles.Effects.KNOB_FREQ_DRAG_SCALE
                    ),
                    EffectControl(
                        "A",
                        state.lfoAmp,
                        0.0, 1.0,
                        200.0
                    )
                ),
                onToggle = { enabled ->
                    state.lfoEnabled.set(enabled)
                    osc3x.withOscillator(index) {
                        enableLFO(enabled)
                    }
                }
            )

            // Filter Panel
            buildEffectPanel(
                "FIL",
                panelWidth / 2,
                state.filterEnabled,
                listOf(
                    EffectControl(
//                        "CUTOFF",
                        "CUT",
                        state.filterCutoff,
                        20.0, 20000.0,
                        Styles.Effects.KNOB_FREQ_DRAG_SCALE
                    ),
                    EffectControl(
//                        "RES",
                        "RES",
                        state.filterRes,
                        0.0, 1.0,
                        Styles.Effects.KNOB_RES_DRAG_SCALE
                    )
                ),
                onToggle = { enabled ->
                    state.filterEnabled.set(enabled)
                    osc3x.withOscillator(index) {
                        enableFilter(enabled)
                    }
                }
            )
        }
    }

    private data class EffectControl(
        val label: String,
        val state: State<Double>,
        val min: Double,
        val max: Double,
        val dragScale: Double
    )

    private fun View.buildEffectPanel(
        label: String,
        width: Double,
        enableState: State<Boolean>,
        controls: List<EffectControl>,
        onToggle: (Boolean) -> Unit
    ) {
        val panelWidth = PANEL_WIDTH - PANEL_PADDING * 2
        view(
            id = "${label.lowercase()}-panel",
            w = width,
            h = Styles.Effects.SECTION_HEIGHT - Styles.Common.PANEL_SPACING * 2,
            bgColor = Colors.moduleBg,
            padding = Styles.Common.PANEL_SPACING,
            spacing = Styles.Common.CONTROL_SPACING,
            direction = FlexDirection.COLUMN
        ) {
            // Header with label and enable button
            view(
                id = "${label.lowercase()}-header",
                w = panelWidth - Styles.Common.PANEL_SPACING * 2,
                h = Styles.Effects.HEADER_HEIGHT,
                direction = FlexDirection.ROW,
                spacing = Styles.Common.CONTROL_SPACING
            ) {
                text(
                    id = "${label.lowercase()}-label",
                    w = Styles.Common.SMALL_BUTTON_SIZE,
                    h = Styles.Effects.HEADER_HEIGHT,
                    text = label,
                    font = font,
                    textColor = Colors.labelText,
                    align = Align.LEFT
                )
                button(
                    id = "${label.lowercase()}-enable",
                    w = Styles.Common.SMALL_BUTTON_SIZE,
                    h = Styles.Common.SMALL_BUTTON_SIZE,
                    bgColor = Colors.disabledButton,
                    hoverColor = Colors.hoverButton,
                    pressColor = Colors.enabledButton,
                    isToggle = true,
                    onToggle = onToggle
                )
            }

            // Controls
            view(
                id = "${label.lowercase()}-controls",
                w = panelWidth - Styles.Common.PANEL_SPACING * 2,
                h = Styles.Effects.SECTION_HEIGHT - Styles.Effects.HEADER_HEIGHT - Styles.Common.PANEL_SPACING * 2,
                direction = FlexDirection.ROW,
                spacing = Styles.Common.CONTROL_SPACING
            ) {
                controls.forEach { control ->
                    buildControlColumn(
                        control.label,
                        control.state,
                        control.min,
                        control.max,
                        hasKnob = true,
                        dragScale = control.dragScale,
                        sliderColor = Colors.effectSlider
                    )
                }
            }
        }
    }

    private fun View.buildADSRSection(index: Int, state: OscillatorState) {
        val panelWidth = PANEL_WIDTH - PANEL_PADDING * 2
        view(
            id = "adsr-section-$index",
            w = panelWidth,
            h = Styles.ADSR.SECTION_HEIGHT,
            bgColor = Color.neonBlue,
            padding = Styles.Common.PANEL_SPACING,
            spacing = Styles.Common.PANEL_SPACING,
            direction = FlexDirection.COLUMN
        ) {
            // ADSR Header
            view(
                id = "adsr-header-$index",
                w = panelWidth - Styles.Common.PANEL_SPACING * 4,
                h = Styles.ADSR.HEADER_HEIGHT,
                direction = FlexDirection.ROW,
                spacing = Styles.Common.CONTROL_SPACING
            ) {
                text(
                    id = "adsr-label-$index",
                    w = 40.0,
                    h = Styles.ADSR.HEADER_HEIGHT,
                    text = "ADSR",
                    font = font,
                    textColor = Colors.labelText,
                    align = Align.LEFT
                )
                button(
                    id = "adsr-enable-$index",
                    w = Styles.Common.SMALL_BUTTON_SIZE,
                    h = Styles.Common.SMALL_BUTTON_SIZE,
                    bgColor = Colors.disabledButton,
                    hoverColor = Colors.hoverButton,
                    pressColor = Colors.enabledButton,
                    isToggle = true,
                    onToggle = { enabled ->
                        osc3x.withOscillator(index) {
                            enableADSR(enabled)
                        }
                    },
                    isPressed = state.adsrEnabled
                )
            }

            // ADSR Controls
            view(
                id = "adsr-controls-$index",
                w  = panelWidth - Styles.Common.PANEL_SPACING * 4,
                h = Styles.ADSR.SECTION_HEIGHT - Styles.ADSR.HEADER_HEIGHT -  Styles.Common.PANEL_SPACING * 2,
                direction = FlexDirection.ROW,
                bgColor = Colors.moduleBg,
                spacing = Styles.Common.CONTROL_SPACING
            ) {
                // Attack
                buildControlColumn(
                    "A",
                    state.attack,
                    0.01, 2.0,
                    hasKnob = false,
                    sliderColor = Colors.adsrSlider,
                    sliderHeight = Styles.ADSR.SLIDER_HEIGHT
                )
                // Decay
                buildControlColumn(
                    "D",
                    state.decay,
                    0.01, 2.0,
                    hasKnob = false,
                    sliderColor = Colors.adsrSlider,
                    sliderHeight = Styles.ADSR.SLIDER_HEIGHT
                )
                // Sustain
                buildControlColumn(
                    "S",
                    state.sustain,
                    0.0, 1.0,
                    hasKnob = false,
                    sliderColor = Colors.adsrSlider,
                    sliderHeight = Styles.ADSR.SLIDER_HEIGHT
                )
                // Release
                buildControlColumn(
                    "R",
                    state.release,
                    0.01, 2.0,
                    hasKnob = false,
                    sliderColor = Colors.adsrSlider,
                    sliderHeight = Styles.ADSR.SLIDER_HEIGHT
                )
            }
        }
    }


    // would follow similar patterns but I'll keep the code sample focused on the
    // main elements for now. Would you like me to continue with the effects and ADSR sections?
    private fun syncOscillatorConfig(index: Int) {
        val state = oscillatorStates[index]

        // Basic oscillator config through Osc3x's public API
        osc3x.setConfig(
            oscillator = index,
            enabled = state.enabled.get(),
            frequency = state.frequency.get(),
            waveform = state.waveform.get(),
            detune = state.detune.get(),
            volume = state.volume.get()
        )

        // Effect parameters through the safe oscillator access
        osc3x.withOscillator(index) {
            // ADSR
            enableADSR(state.adsrEnabled.get())
            setADSR(
                attack = state.attack.get(),
                decay = state.decay.get(),
                sustain = state.sustain.get(),
                release = state.release.get()
            )

            // LFO
            enableLFO(state.lfoEnabled.get())
            setLFO(
                frequency = state.lfoFreq.get(),
                amplitude = state.lfoAmp.get()
            )

            // Filter
            enableFilter(state.filterEnabled.get())
            setFilterCutoff(state.filterCutoff.get())
            setFilterResonance(state.filterRes.get())
        }
    }

    fun randomize() {
        // First randomize the base oscillator parameters through Osc3x
        osc3x.randomize()

        // Then randomize the additional parameters and sync UI state
        for (i in 0..2) {
            val config = osc3x.getConfig(i)
            val state = oscillatorStates[i]

            // Sync basic parameters from Osc3x
            state.enabled.set(config.enabled)
            state.volume.set(config.volume)
            state.frequency.set(config.frequency)
            state.detune.set(config.detune)
            state.waveform.set(config.waveform)

            // Randomize LFO parameters
            state.lfoEnabled.set((0..1).random() == 1)
            state.lfoFreq.set((1..20).random().toDouble()) // 1-20 Hz
            state.lfoAmp.set((0..100).random() / 100.0) // 0.0-1.0

            // Randomize Filter parameters
            state.filterEnabled.set((0..1).random() == 1)
            state.filterCutoff.set((200..20000).random().toDouble()) // 200-20000 Hz
            state.filterRes.set((0..80).random() / 100.0) // 0.0-0.8 to avoid self-oscillation

            // Randomize ADSR parameters
            state.adsrEnabled.set((0..1).random() == 1)
            state.attack.set((1..200).random() / 100.0) // 0.01-2.0 seconds
            state.decay.set((1..200).random() / 100.0) // 0.01-2.0 seconds
            state.sustain.set((0..100).random() / 100.0) // 0.0-1.0
            state.release.set((1..200).random() / 100.0) // 0.01-2.0 seconds

            // Apply all randomized parameters to the oscillator
            osc3x.withOscillator(i) {
                enableLFO(state.lfoEnabled.get())
                setLFO(
                    frequency = state.lfoFreq.get(),
                    amplitude = state.lfoAmp.get()
                )

                enableFilter(state.filterEnabled.get())
                setFilterCutoff(state.filterCutoff.get())
                setFilterResonance(state.filterRes.get())

                enableADSR(state.adsrEnabled.get())
                setADSR(
                    attack = state.attack.get(),
                    decay = state.decay.get(),
                    sustain = state.sustain.get(),
                    release = state.release.get()
                )
            }
        }
    }

    /**
     * Update oscillator configuration and sync state.
     * Base parameters go through Osc3x's public API while effect parameters use the withOscillator pattern.
     */
    fun setConfig(
        oscillator: Int,
        // Basic oscillator parameters
        enabled: Boolean? = null,
        frequency: Double? = null,
        detune: Double? = null,
        waveform: Oscillator.Waveform? = null,
        volume: Double? = null,
        // LFO parameters
        lfoEnabled: Boolean? = null,
        lfoFrequency: Double? = null,
        lfoAmplitude: Double? = null,
        // Filter parameters
        filterEnabled: Boolean? = null,
        filterCutoff: Double? = null,
        filterResonance: Double? = null,
        // ADSR parameters
        adsrEnabled: Boolean? = null,
        attack: Double? = null,
        decay: Double? = null,
        sustain: Double? = null,
        release: Double? = null
    ) {
        if (oscillator !in oscillatorStates.indices) return // Ignore invalid oscillator index

        val state = oscillatorStates[oscillator]

        // Update basic parameters through Osc3x's public API
        enabled?.let { state.enabled.set(it) }
        frequency?.let { state.frequency.set(it) }
        detune?.let { state.detune.set(it) }
        waveform?.let { state.waveform.set(it) }
        volume?.let { state.volume.set(it) }

        // Update effect parameters if provided
        lfoEnabled?.let { state.lfoEnabled.set(it) }
        lfoFrequency?.let { state.lfoFreq.set(it) }
        lfoAmplitude?.let { state.lfoAmp.set(it) }

        filterEnabled?.let { state.filterEnabled.set(it) }
        filterCutoff?.let { state.filterCutoff.set(it) }
        filterResonance?.let { state.filterRes.set(it) }

        adsrEnabled?.let { state.adsrEnabled.set(it) }
        attack?.let { state.attack.set(it) }
        decay?.let { state.decay.set(it) }
        sustain?.let { state.sustain.set(it) }
        release?.let { state.release.set(it) }

        // Sync with Osc3x engine
        syncOscillatorConfig(oscillator)
    }

    fun update() {
        osc3x.update()
    }

    fun draw() {
        synthView.draw()
    }

    fun setMaterVolume(volume: Double) {
        masterVolume.set(volume)
    }

}
