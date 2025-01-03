package com.kengine

import com.kengine.log.Logging

class GameRunner(
    frameRate: Int = 60,
    gameBuilder: () -> Game
) : Logging {

    init {
        var game: Game? = null
        try {
            game = gameBuilder()
            GameLoop(
                frameRate,
                update = game::update,
                draw = game::draw
            ).start()
        } catch (e: Exception) {
            handleException(e)
        } finally {
            game?.cleanup()
        }
    }

    private fun handleException(e: Exception) {
        logger.error(e) { "Unhandled exception in GameLoop" }
    }
}
