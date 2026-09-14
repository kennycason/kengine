package com.kengine.input

class InputState(initialMask: Int = 0) {
    var mask: Int = initialMask
        private set

    /** Normalized horizontal position of the primary stick in [-1000, 1000]. */
    var leftStickX: Int = 0
        private set

    /** Normalized vertical position of the primary stick in [-1000, 1000]. */
    var leftStickY: Int = 0
        private set

    fun reset() {
        mask = 0
        leftStickX = 0
        leftStickY = 0
    }

    fun set(button: InputButton, pressed: Boolean = true) {
        val bit = bitFor(button)
        mask = if (pressed) {
            mask or bit
        } else {
            mask and bit.inv()
        }
    }

    fun setMask(mask: Int) {
        this.mask = mask
    }

    fun setLeftStick(x: Int, y: Int) {
        leftStickX = x.coerceIn(-ANALOG_AXIS_MAX, ANALOG_AXIS_MAX)
        leftStickY = y.coerceIn(-ANALOG_AXIS_MAX, ANALOG_AXIS_MAX)
    }

    fun copyFrom(other: InputState) {
        mask = other.mask
        leftStickX = other.leftStickX
        leftStickY = other.leftStickY
    }

    fun isPressed(button: InputButton): Boolean {
        return (mask and bitFor(button)) != 0
    }

    fun axis(negative: InputButton, positive: InputButton): Int {
        var value = 0
        if (isPressed(negative)) value -= 1
        if (isPressed(positive)) value += 1
        return value
    }

    companion object {
        const val ANALOG_AXIS_MAX = 1000

        fun bitFor(button: InputButton): Int {
            return when (button) {
                InputButton.LEFT,
                InputButton.DPAD_LEFT -> 1
                InputButton.RIGHT,
                InputButton.DPAD_RIGHT -> 1 shl 1
                InputButton.UP,
                InputButton.DPAD_UP -> 1 shl 2
                InputButton.DOWN,
                InputButton.DPAD_DOWN -> 1 shl 3
                InputButton.A -> 1 shl 4
                InputButton.B -> 1 shl 5
                InputButton.START -> 1 shl 6
                InputButton.X,
                InputButton.C_UP -> 1 shl 7
                InputButton.Y,
                InputButton.C_DOWN -> 1 shl 8
                InputButton.L -> 1 shl 9
                InputButton.R -> 1 shl 10
                InputButton.SELECT,
                InputButton.Z -> 1 shl 11
                InputButton.C_LEFT -> 1 shl 12
                InputButton.C_RIGHT -> 1 shl 13
            }
        }
    }
}
