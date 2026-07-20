import com.kengine.graphics.Color
import com.kengine.input.mouse.MouseInputEventSubscriber
import com.kengine.three.GpuFrame
import com.kengine.three.ui.GpuUiAlign3D
import com.kengine.three.ui.GpuUiContext3D
import com.kengine.three.ui.GpuUiDirection3D
import com.kengine.three.ui.GpuUiRenderer3D
import com.kengine.three.ui.GpuUiView3D
import kotlin.math.roundToInt

class ViewerInspectorUi(
    private val controls: ViewerControlState,
    private val activeModel: () -> ViewerLoadedModel,
    private val statusText: () -> String,
    private val onSelectModel: (Int) -> Unit,
    private val onSelectClip: (Int) -> Unit,
    private val onResetView: () -> Unit,
    private val onLoadModel: () -> Unit,
    private val onMessage: (String) -> Unit,
    private val onChanged: () -> Unit
) {
    val ui = GpuUiContext3D()

    init {
        ui.addView(buildPanel())
        ui.performLayout()
    }

    fun handleMouse(mouse: MouseInputEventSubscriber): Boolean {
        return ui.handleMouse(mouse)
    }

    fun prepare(renderer: GpuUiRenderer3D) {
        currentTexts().forEach(renderer::preloadText)
    }

    fun render(
        renderer: GpuUiRenderer3D,
        frame: GpuFrame
    ) {
        renderer.render(ui, frame)
    }

    private fun currentTexts(): List<String> {
        return listOf(
            "KENGINE 3D VIEWER",
            statusText().compactUiText(34),
            "MODEL ${controls.currentModelPreset.label.compactUiText(18)}",
            "PREV",
            "NEXT",
            "LOAD",
            "CLIP ${activeModel().selectedClipName()?.compactUiText(20) ?: "NONE"}",
            "SPEED ${controls.animationSpeed.formatUiPercent()} PCT",
            if (controls.animationPaused) "PLAY" else "PAUSE",
            "STOP",
            "ANIMATION SPEED",
            "LIGHT ${controls.currentLightPreset.label.compactUiText(18)}",
            "BG",
            "AMBIENT ${controls.currentLight.ambientStrength.formatUiPercent()} PCT",
            "DIFFUSE ${controls.currentLight.diffuseStrength.formatUiPercent()} PCT",
            "BACKGROUND ${controls.currentBackground.label.compactUiText(14)}",
            if (controls.showAxes) "AXES ON" else "AXES OFF",
            "RESET"
        )
    }

    private fun buildPanel(): GpuUiView3D {
        return GpuUiView3D(
            id = "viewer-inspector",
            desiredX = 16.0,
            desiredY = 16.0,
            desiredWidth = 360.0,
            desiredHeight = 394.0,
            backgroundColor = Color.fromHex("10151ee0"),
            direction = GpuUiDirection3D.COLUMN,
            padding = 12.0,
            spacing = 8.0
        ).apply {
            label(
                id = "title",
                text = { "KENGINE 3D VIEWER" },
                width = 336.0,
                height = 24.0,
                color = Color.fromHex("dbe7ff"),
                align = GpuUiAlign3D.LEFT
            )

            label(
                id = "status",
                text = { statusText().compactUiText(34) },
                width = 336.0,
                height = ROW_HEIGHT,
                color = Color.fromHex("98d6ffff"),
                align = GpuUiAlign3D.LEFT
            )

            controlRow("model-row") {
                label(
                    id = "model-label",
                    text = { "MODEL ${controls.currentModelPreset.label.compactUiText(18)}" },
                    width = 144.0,
                    height = ROW_HEIGHT,
                    color = Color.fromHex("f5f7fb")
                )
                smallButton("model-prev", "PREV") { onSelectModel(-1) }
                smallButton("model-next", "NEXT") { onSelectModel(1) }
                smallButton("model-load", "LOAD") { onLoadModel() }
            }

            controlRow("clip-row") {
                label(
                    id = "clip-label",
                    text = { "CLIP ${activeModel().selectedClipName()?.compactUiText(20) ?: "NONE"}" },
                    width = 180.0,
                    height = ROW_HEIGHT,
                    color = Color.fromHex("f5f7fb")
                )
                smallButton("clip-prev", "PREV") { onSelectClip(-1) }
                smallButton("clip-next", "NEXT") { onSelectClip(1) }
            }

            controlRow("playback-row") {
                label(
                    id = "playback-label",
                    text = { "SPEED ${controls.animationSpeed.formatUiPercent()} PCT" },
                    width = 182.0,
                    height = ROW_HEIGHT,
                    color = Color.fromHex("f5f7fb")
                )
                button(
                    id = "play-pause",
                    text = { if (controls.animationPaused) "PLAY" else "PAUSE" },
                    width = 74.0,
                    height = ROW_HEIGHT,
                    backgroundColor = BUTTON_BG,
                    hoverColor = BUTTON_HOVER,
                    pressColor = BUTTON_PRESS,
                    onClick = { onMessage(controls.toggleAnimationPlayback()) }
                )
                smallButton("stop", "STOP") { onMessage(controls.stopAnimationPlayback()) }
            }

            sliderRow(
                id = "speed-slider",
                label = { "ANIMATION SPEED" },
                value = { controls.animationSpeed },
                min = 0.25,
                max = 3.0,
                onValueChanged = {
                    controls.setAnimationSpeed(it)
                    onChanged()
                }
            )

            controlRow("light-row") {
                label(
                    id = "light-label",
                    text = { "LIGHT ${controls.currentLightPreset.label.compactUiText(18)}" },
                    width = 180.0,
                    height = ROW_HEIGHT,
                    color = Color.fromHex("f5f7fb")
                )
                smallButton("light-next", "NEXT") { onMessage(controls.cycleLight()) }
                smallButton("background-next", "BG") { onMessage(controls.cycleBackground()) }
            }

            sliderRow(
                id = "ambient-slider",
                label = { "AMBIENT ${controls.currentLight.ambientStrength.formatUiPercent()} PCT" },
                value = { controls.currentLight.ambientStrength.toDouble() },
                min = 0.0,
                max = 1.0,
                onValueChanged = {
                    controls.setAmbientStrength(it)
                    onChanged()
                }
            )

            sliderRow(
                id = "diffuse-slider",
                label = { "DIFFUSE ${controls.currentLight.diffuseStrength.formatUiPercent()} PCT" },
                value = { controls.currentLight.diffuseStrength.toDouble() },
                min = 0.0,
                max = 1.5,
                onValueChanged = {
                    controls.setDiffuseStrength(it)
                    onChanged()
                }
            )

            controlRow("debug-row") {
                label(
                    id = "debug-label",
                    text = { "BACKGROUND ${controls.currentBackground.label.compactUiText(14)}" },
                    width = 180.0,
                    height = ROW_HEIGHT,
                    color = Color.fromHex("f5f7fb")
                )
                button(
                    id = "axes-toggle",
                    text = { if (controls.showAxes) "AXES ON" else "AXES OFF" },
                    width = 74.0,
                    height = ROW_HEIGHT,
                    backgroundColor = BUTTON_BG,
                    hoverColor = BUTTON_HOVER,
                    pressColor = BUTTON_PRESS,
                    onClick = { onMessage(controls.toggleAxes()) }
                )
                smallButton("reset", "RESET") { onResetView() }
            }
        }
    }

    private fun GpuUiView3D.controlRow(
        id: String,
        block: GpuUiView3D.() -> Unit
    ) {
        view(
            id = id,
            width = 336.0,
            height = ROW_HEIGHT,
            direction = GpuUiDirection3D.ROW,
            spacing = 6.0,
            block = block
        )
    }

    private fun GpuUiView3D.sliderRow(
        id: String,
        label: () -> String,
        value: () -> Double,
        min: Double,
        max: Double,
        onValueChanged: (Double) -> Unit
    ) {
        view(
            id = id,
            width = 336.0,
            height = ROW_HEIGHT,
            direction = GpuUiDirection3D.ROW,
            spacing = 8.0
        ) {
            label(
                id = "$id-label",
                text = label,
                width = 138.0,
                height = ROW_HEIGHT,
                color = Color.fromHex("f5f7fb")
            )
            slider(
                id = "$id-control",
                value = value,
                onValueChanged = onValueChanged,
                width = 190.0,
                height = ROW_HEIGHT,
                min = min,
                max = max
            )
        }
    }

    private fun GpuUiView3D.smallButton(
        id: String,
        text: String,
        onClick: () -> Unit
    ) {
        button(
            id = id,
            text = { text },
            width = 58.0,
            height = ROW_HEIGHT,
            backgroundColor = BUTTON_BG,
            hoverColor = BUTTON_HOVER,
            pressColor = BUTTON_PRESS,
            onClick = onClick
        )
    }

    private fun String.compactUiText(maxLength: Int): String {
        val sanitized = uppercase()
            .map { char ->
                if (char in 'A'..'Z' || char in '0'..'9') {
                    char
                } else {
                    ' '
                }
            }
            .joinToString(separator = "")
            .trim()
            .replaceMultipleSpaces()
        if (sanitized.length <= maxLength) {
            return sanitized
        }
        return sanitized.take(maxLength).trim()
    }

    private fun String.replaceMultipleSpaces(): String {
        val result = StringBuilder()
        var previousWasSpace = false
        forEach { char ->
            if (char == ' ') {
                if (!previousWasSpace) {
                    result.append(char)
                }
                previousWasSpace = true
            } else {
                result.append(char)
                previousWasSpace = false
            }
        }
        return result.toString()
    }

    private fun Double.formatUiPercent(): String {
        return (this * 100.0).roundToInt().toString()
    }

    private fun Float.formatUiPercent(): String {
        return (this * 100.0f).roundToInt().toString()
    }

    companion object {
        private const val ROW_HEIGHT = 26.0
        private val BUTTON_BG = Color.fromHex("273244f2")
        private val BUTTON_HOVER = Color.fromHex("3b4a62ff")
        private val BUTTON_PRESS = Color.fromHex("5ca8ffff")
    }
}
