package com.kengine.sound.keyboard

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.Keys
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.input.mouse.useMouseContext
import com.kengine.sound.keyboard.NoteGenerator.WHITE_KEYS_PER_OCTAVE
import com.kengine.sound.synth.Osc3x
import com.kengine.ui.View
import com.kengine.ui.getViewContext

class VirtualKeyboard(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    private val synth: Osc3x,
    padding: Double = 0.0,
    bgColor: Color? = null,
    startOctave: Int = 3,
    numOctaves: Int = 3,
    parent: View? = null
) : View(id, x, y, w, h, bgColor, padding = padding, parent = parent) {

    init {
        getViewContext().addView(this)
    }

    // Track which keyboard keys are currently pressed to avoid triggering the same note multiple times
    // and to properly handle key releases when multiple keys are pressed simultaneously
    private val keyboardKeysPressed = mutableSetOf<UInt>()

    // Track whether the mouse button was previously pressed to detect new presses and releases
    private var wasMouseButtonPressed = false

    // Track which piano key is currently being played by the mouse
    private var mouseActiveKey: PianoKey? = null

    /**
     * Keyboard layout mapping to piano notes.
     * The mapping follows the QWERTY keyboard layout in a flattened sequence starting with number row:
     * "1234567890" (number row)
     * "QWERTYUIOP" (top letter row)
     * "ASDFGHJKL:" (middle letter row)
     * "ZXCVBNM<>?" (bottom letter row)
     * Plus additional keys: TAB, RETURN, BACKSPACE, etc.
     */
    private val keyboardKeys = listOf(
        // Row 0: 1234567890 (number row)
        Keys.ONE, Keys.TWO, Keys.THREE, Keys.FOUR, Keys.FIVE, Keys.SIX, Keys.SEVEN, Keys.EIGHT, Keys.NINE, Keys.ZERO,
        // Row 1: QWERTYUIOP
        Keys.Q, Keys.W, Keys.E, Keys.R, Keys.T, Keys.Y, Keys.U, Keys.I, Keys.O, Keys.P,
        // Row 2: ASDFGHJKL:
        Keys.A, Keys.S, Keys.D, Keys.F, Keys.G, Keys.H, Keys.J, Keys.K, Keys.L, 0x3AU, // 0x3AU is ':'
        // Row 3: ZXCVBNM<>?
        Keys.Z, Keys.X, Keys.C, Keys.V, Keys.B, Keys.N, Keys.M, 0x2CU, 0x2EU, 0x2FU,  // 0x2CU is ',', 0x2EU is '.', 0x2FU is '/'
        // Additional keys for more coverage
        Keys.TAB, Keys.BACKSPACE, Keys.RETURN, Keys.SPACE,
        Keys.HOME, Keys.END, Keys.PAGEUP, Keys.PAGEDOWN,
        Keys.INSERT, Keys.DELETE,
        Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6,
        Keys.F7, Keys.F8, Keys.F9, Keys.F10, Keys.F11, Keys.F12
    )

    data class PianoKey(
        val note: String,
        val frequency: Double,
        val isBlack: Boolean,
        var x: Double = 0.0,
        var width: Double = 0.0
    )

    private val notes = NoteGenerator.generateNotes(startOctave, numOctaves)

    val colors = Color.rainbow(WHITE_KEYS_PER_OCTAVE * numOctaves)

    private var activeKey: PianoKey? = null
    private val whiteKeyWidth: Double
        get() = layoutW / notes.count { !it.isBlack }
    private val blackKeyWidth: Double
        get() = whiteKeyWidth * 0.6
    private val blackKeyHeight: Double
        get() = layoutH * 0.6

    init {
        // Calculate key positions
        var whiteKeyX = 0.0
        notes.forEach { key ->
            if (!key.isBlack) {
                key.x = whiteKeyX
                key.width = whiteKeyWidth
                whiteKeyX += whiteKeyWidth
            }
        }

        // Position black keys relative to white keys
        notes.forEachIndexed { index, key ->
            if (key.isBlack) {
                val prevWhiteKey = notes.take(index).last { !it.isBlack }
                key.x = prevWhiteKey.x + whiteKeyWidth - (blackKeyWidth / 2)
                key.width = blackKeyWidth
            }
        }
    }

    override fun draw() {
        useGeometryContext {
            bgColor?.also {
                fillRectangle(layoutX, layoutY, layoutW, layoutH, it)
            }
            // Draw white keys first
            notes
                .filter { !it.isBlack }
                .forEachIndexed { i, key ->
                    val isActive = key == activeKey
                    fillRectangle(
                        layoutX + key.x,
                        layoutY,
                        key.width,
                        layoutH,
                        if (isActive) Color.gray80 else colors[i]
                    )
                    // Draw key border
                    drawRectangle(
                        layoutX + key.x,
                        layoutY,
                        key.width,
                        layoutH,
                        Color.gray10
                    )
                }

            // Draw black keys on top
            notes.filter { it.isBlack }.forEach { key ->
                val isActive = key == activeKey
                fillRectangle(
                    layoutX + key.x,
                    layoutY,
                    key.width,
                    blackKeyHeight,
                    if (isActive) Color.gray40 else Color.black
                )
            }
        }
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!isWithinBounds(mouseX, mouseY)) return

        val relativeX = mouseX - layoutX
        val relativeY = mouseY - layoutY

        // Check black keys first since they're on top
        val clickedBlackKey = notes.filter { it.isBlack }
            .firstOrNull { key ->
                relativeX >= key.x &&
                    relativeX <= key.x + key.width &&
                    relativeY <= blackKeyHeight
            }

        if (clickedBlackKey != null) {
            triggerNote(clickedBlackKey)
            return
        }

        // Then check white keys
        val clickedWhiteKey = notes.filter { !it.isBlack }
            .firstOrNull { key ->
                relativeX >= key.x &&
                    relativeX <= key.x + key.width
            }

        if (clickedWhiteKey != null) {
            triggerNote(clickedWhiteKey)
        }
    }

    override fun release(mouseX: Double, mouseY: Double) {
        releaseActiveNote()
    }

    private fun triggerNote(key: PianoKey) {
        releaseActiveNote()
        activeKey = key

        // Set frequency for all oscillators and trigger them
        for (i in 0..2) {
            synth.setFrequency(i, key.frequency)
            synth.triggerNote(i)  // Using the new triggerNote method
        }
    }

    private fun releaseActiveNote() {
        if (activeKey != null) {
            // Release all oscillators
            for (i in 0..2) {
                synth.releaseNote(i)
            }
            activeKey = null
        }
    }

    private fun calculateKeyPositions() {
        // Calculate key positions
        var whiteKeyX = 0.0
        notes.forEach { key ->
            if (!key.isBlack) {
                key.x = whiteKeyX
                key.width = whiteKeyWidth
                whiteKeyX += whiteKeyWidth
            }
        }

        // Position black keys relative to white keys
        notes.forEachIndexed { index, key ->
            if (key.isBlack) {
                val prevWhiteKey = notes.take(index).last { !it.isBlack }
                key.x = prevWhiteKey.x + whiteKeyWidth - (blackKeyWidth / 2)
                key.width = blackKeyWidth
            }
        }
    }

    override fun performLayout(offsetX: Double, offsetY: Double) {
        super.performLayout(offsetX, offsetY)
        calculateKeyPositions()  // Calculate positions after layout is set
    }

    /**
     * Updates the keyboard state based on both keyboard and mouse input.
     *
     * For keyboard input:
     * - Each key on the keyboard is mapped directly to the corresponding note in the piano keyboard.
     * - The mapping is sequential, with each key mapping to a single note.
     * - Checks the current state of each key every frame and triggers notes based on that state.
     *
     * For mouse input:
     * - Checks if the mouse is over the keyboard and if the left button is pressed.
     * - Determines which piano key is under the mouse cursor.
     * - Triggers notes based on the current state of the mouse button, not just on state transitions.
     * - Tracks which piano key is currently being played by the mouse to handle cases where the mouse
     *   moves from one key to another while the button is pressed.
     *
     * This approach makes mouse clicks as responsive as keyboard input by:
     * 1. Checking the current state every frame rather than relying on catching transitions
     * 2. Triggering notes immediately when the mouse button is pressed, without requiring a hold
     * 3. Handling cases where the mouse moves between keys while the button is pressed
     * 4. Using a similar state-based approach as keyboard input for consistency
     */
    fun update() {
        // Handle keyboard input
        useKeyboardContext {
            // Check each key in the mapping
            for (keyIndex in keyboardKeys.indices) {
                val keyCode = keyboardKeys[keyIndex]

                // Skip if the note index is out of range
                if (keyIndex >= notes.size) continue

                // Check if key is pressed
                val isKeyPressed = keyboard.isPressed(keyCode)

                // If key is newly pressed, trigger the note
                if (isKeyPressed && !keyboardKeysPressed.contains(keyCode)) {
                    keyboardKeysPressed.add(keyCode)
                    triggerNote(notes[keyIndex])
                }
                // If key is released, release the note if it's the active key
                else if (!isKeyPressed && keyboardKeysPressed.contains(keyCode)) {
                    keyboardKeysPressed.remove(keyCode)
                    if (activeKey == notes[keyIndex]) {
                        releaseActiveNote()
                    }
                }
            }
        }

        // Handle mouse input
        useMouseContext {
            val mouseX = mouse.cursor().x
            val mouseY = mouse.cursor().y
            val isMouseButtonPressed = mouse.isLeftPressed()

            // Check if mouse is within keyboard bounds
            if (isWithinBounds(mouseX, mouseY)) {
                val relativeX = mouseX - layoutX
                val relativeY = mouseY - layoutY

                // Find which piano key is under the mouse
                var keyUnderMouse: PianoKey? = null

                // Check black keys first since they're on top
                val blackKeyUnderMouse = notes.filter { it.isBlack }
                    .firstOrNull { key ->
                        relativeX >= key.x &&
                            relativeX <= key.x + key.width &&
                            relativeY <= blackKeyHeight
                    }

                if (blackKeyUnderMouse != null) {
                    keyUnderMouse = blackKeyUnderMouse
                } else {
                    // Then check white keys
                    val whiteKeyUnderMouse = notes.filter { !it.isBlack }
                        .firstOrNull { key ->
                            relativeX >= key.x &&
                                relativeX <= key.x + key.width
                        }

                    if (whiteKeyUnderMouse != null) {
                        keyUnderMouse = whiteKeyUnderMouse
                    }
                }

                // Handle mouse input based on current state, similar to keyboard input
                if (isMouseButtonPressed) {
                    // Mouse button is currently pressed
                    if (keyUnderMouse != null) {
                        // There's a key under the mouse
                        if (mouseActiveKey == null) {
                            // No previous active key, trigger the note immediately
                            mouseActiveKey = keyUnderMouse
                            triggerNote(keyUnderMouse)
                        } else if (mouseActiveKey != keyUnderMouse) {
                            // Different key than before, release previous and trigger new
                            if (activeKey == mouseActiveKey) {
                                releaseActiveNote()
                            }
                            mouseActiveKey = keyUnderMouse
                            triggerNote(keyUnderMouse)
                        }
                        // If it's the same key as before, do nothing (note is already playing)
                    }
                } else {
                    // Mouse button is not pressed
                    if (mouseActiveKey != null) {
                        // If we had an active mouse key, release it
                        if (activeKey == mouseActiveKey) {
                            releaseActiveNote()
                        }
                        mouseActiveKey = null
                    }
                }
            } else if (mouseActiveKey != null && !isMouseButtonPressed) {
                // Mouse moved outside bounds and we had an active key
                if (activeKey == mouseActiveKey) {
                    releaseActiveNote()
                }
                mouseActiveKey = null
            }

            // Update mouse button state for next frame
            wasMouseButtonPressed = isMouseButtonPressed
        }
    }
}
