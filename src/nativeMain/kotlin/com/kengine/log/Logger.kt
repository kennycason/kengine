package com.kengine.log

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * A simple logger
 */
object Logger {
    enum class Level {
        INFO, DEBUG, WARN, ERROR
    }

    private var logLevel: Level = Level.INFO

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
    fun log(level: Level, message: String) {
        if (level.ordinal >= logLevel.ordinal) {
            val timestamp = currentTimestampString()
            println("[$timestamp] [${level.name}] $message")
        }
    }

    fun info(message: () -> String) = log(Level.INFO, message())
    fun debug(message: () -> String) = log(Level.DEBUG, message())
    fun warn(message: () -> String) = log(Level.WARN, message())
    fun error(message: () -> String) = log(Level.ERROR, message())

    /**
     * Gets the current timestamp as a string.
     */
    private fun currentTimestampString(): String {
        memScoped {
            val timeVal = alloc<timeval>()
            gettimeofday(timeVal.ptr, null)
            val seconds = timeVal.tv_sec
            val microseconds = timeVal.tv_usec
            return "$seconds.${microseconds.toString().padStart(6, '0')}"
        }
    }
}