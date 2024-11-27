package com.kengine

import com.kengine.log.Logger

class GameRunner(
    frameRate: Int = 60,
    gameBuilder: () -> Game
) {

    init {
        var game: Game? = null
        try {
            game = gameBuilder()
            GameLoop(frameRate) {
                game.update()
                game.draw()
            }
        } catch (e: Exception) {
            handleException(e)
        } finally {
            game?.cleanup()
        }
    }

    private fun handleException(e: Exception) {
        Logger.error { "Unhandled exception in GameLoop: ${e.message}" }
        Logger.error { "Stacktrace: ${e.stackTraceToString()}" }
        e.printStackTrace()
    }

}