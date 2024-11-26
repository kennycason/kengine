package com.kengine.log

import com.kengine.log.Logger.Level.DEBUG
import com.kengine.log.Logger.Level.ERROR
import com.kengine.log.Logger.Level.INFO
import com.kengine.log.Logger.Level.WARN
import com.kengine.time.getCurrentTimestampMilliseconds

/**
 * A simple logger
 * TODO add file logger support
 */
object Logger {
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    private var logLevel: Level = INFO

    fun debug(message: () -> String) = log(DEBUG, message())
    fun info(message: () -> String) = log(INFO, message())
    fun warn(message: () -> String) = log(WARN, message())
    fun error(message: () -> String) = log(ERROR, message())
    fun debug(message: () -> Any) = log(DEBUG, message().toString())
    fun info(message: () -> Any) = log(INFO, message().toString())
    fun warn(message: () -> Any) = log(WARN, message().toString())
    fun error(message: () -> Any) = log(ERROR, message().toString())

    fun debug(message: String) = log(DEBUG, message)
    fun info(message: String) = log(INFO, message)
    fun warn(message: String) = log(WARN, message)
    fun error(message: String) = log(ERROR, message)
    fun debug(message: Any) = log(DEBUG, message.toString())
    fun info(message: Any) = log(INFO, message.toString())
    fun warn(message: Any) = log(WARN, message.toString())
    fun error(message: Any) = log(ERROR, message.toString())

    /**
     * Sets the global log level.
     * Messages below this level will not be logged.
     */
    fun setLevel(level: Level) {
        logLevel = level
    }

    /**
     * Logs a message with the specified level.
     */
    private fun log(level: Level, message: String) {
        if (level.ordinal >= logLevel.ordinal) {
            val timestamp = getCurrentTimestampMilliseconds()
            println("[$timestamp] [${level.name}] $message")
        }
    }

}