package com.kengine.log

import com.kengine.time.getCurrentTimestampMilliseconds

/**
 * A simple logger
 * TODO add file logger support
 */
object Logger {
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    private var logLevel: Level = Level.INFO

    fun debug(message: () -> String) = log(Level.DEBUG, message())
    fun info(message: () -> String) = log(Level.INFO, message())
    fun warn(message: () -> String) = log(Level.WARN, message())
    fun error(message: () -> String) = log(Level.ERROR, message())
    fun debug(message: () -> Any) = log(Level.DEBUG, message().toString())
    fun info(message: () -> Any) = log(Level.INFO, message().toString())
    fun warn(message: () -> Any) = log(Level.WARN, message().toString())
    fun error(message: () -> Any) = log(Level.ERROR, message().toString())

    fun debug(message: String) = log(Level.DEBUG, message)
    fun info(message: String) = log(Level.INFO, message)
    fun warn(message: String) = log(Level.WARN, message)
    fun error(message: String) = log(Level.ERROR, message)
    fun debug(message: Any) = log(Level.DEBUG, message.toString())
    fun info(message: Any) = log(Level.INFO, message.toString())
    fun warn(message: Any) = log(Level.WARN, message.toString())
    fun error(message: Any) = log(Level.ERROR, message.toString())

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