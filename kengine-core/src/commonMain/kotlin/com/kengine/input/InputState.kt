package com.kengine.input

class InputState(initialMask: Int = 0) {
    var mask: Int = initialMask
        private set

    fun reset() {
        mask = 0
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

    fun copyFrom(other: InputState) {
        mask = other.mask
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
        fun bitFor(button: InputButton): Int {
            return 1 shl button.ordinal
        }
    }
}
