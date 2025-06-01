package com.kengine.sound.keyboard

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
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
}
