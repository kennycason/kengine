package com.kengine.action

class SequenceAction(
    private val actions: List<Action>,
    val onComplete: (() -> Unit)? = null
) : Action {
    private var currentIndex = 0

    override fun update(): Boolean {
        if (currentIndex >= actions.size) {
            onComplete?.invoke()
            return true
        }

        if (actions[currentIndex].update()) {
            currentIndex++
            if (currentIndex >= actions.size) {
                onComplete?.invoke()
                return true
            }
        }
        return false
    }
}
