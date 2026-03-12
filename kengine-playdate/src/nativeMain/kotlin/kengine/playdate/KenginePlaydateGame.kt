package kengine.playdate

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import playdate.api.PlaydateAPI

@OptIn(ExperimentalForeignApi::class)
class KenginePlaydateGame(private val playdate: CPointer<PlaydateAPI>) {
    enum class State {
        INIT, PLAY
    }

    private var state = State.INIT

    private val api get() = playdate.pointed

    fun update() {
        when (state) {
            State.INIT -> {
                api.system?.pointed?.logToConsole?.invoke("KenginePlaydateGame initialized")
                state = State.PLAY
            }
            State.PLAY -> {}
        }
    }

    fun draw() {
        when (state) {
            State.INIT -> {}
            State.PLAY -> {}
        }
    }

    fun cleanup() {
        api.system?.pointed?.logToConsole?.invoke("KenginePlaydateGame cleanup")
    }
}
