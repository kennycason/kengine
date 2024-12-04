package kengine.playdate


class KenginePlaydateGame {
    enum class State {
        INIT, PLAY
    }

    private var state = State.INIT

    fun update() {
        when (state) {
            State.INIT -> {}
            State.PLAY -> {}
        }
    }

    fun draw() {

    }

    fun cleanup() {
    }

}
