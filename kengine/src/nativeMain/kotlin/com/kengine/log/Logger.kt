package com.kengine.log

import com.kengine.log.Logger.Level.DEBUG
import com.kengine.log.Logger.Level.ERROR
import com.kengine.log.Logger.Level.INFO
import com.kengine.log.Logger.Level.TRACE
import com.kengine.log.Logger.Level.WARN
import com.kengine.time.getCurrentMilliseconds
import kotlin.reflect.KClass

/**
 * A simple logger
 * TODO add file logger support
 */
class Logger {
    private val className: String

    constructor(className: String) {
        this.className = className
    }

    constructor(klass: KClass<*>)
            : this(klass.simpleName ?: "Unknown")

    enum class Level {
        TRACE, // ideal for expensive debugging that may affect fps, such as per-tile logging in map rendering
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    fun isTraceEnabled() = logLevel == TRACE
    fun isDebugEnabled() = logLevel == DEBUG
    fun isInfoEnabled() = logLevel == INFO
    fun isWarnEnabled() = logLevel == WARN
    fun isErrorEnabled() = logLevel == ERROR

    fun trace(message: () -> String?) = log(TRACE, message())
    fun debug(message: () -> String?) = log(DEBUG, message())
    fun info(message: () -> String?) = log(INFO, message())
    fun warn(message: () -> String?) = log(WARN, message())
    fun error(message: () -> String?) = log(ERROR, message())

    fun trace(message: String?) = log(TRACE, message)
    fun debug(message: String?) = log(DEBUG, message)
    fun info(message: String?) = log(INFO, message)
    fun warn(message: String?) = log(WARN, message)
    fun error(message: String?) = log(ERROR, message)

    fun traceStream() = LogStreamBuilder(TRACE, this)
    fun debugStream() = LogStreamBuilder(DEBUG, this)
    fun infoStream() = LogStreamBuilder(INFO, this)
    fun warnStream() = LogStreamBuilder(WARN, this)
    fun errorStream() = LogStreamBuilder(ERROR, this)

    fun traceStream(block: LogStreamBuilder.() -> Unit) {
        LogStreamBuilder(TRACE, this).apply(block).flush()
    }

    fun debugStream(block: LogStreamBuilder.() -> Unit) {
        LogStreamBuilder(DEBUG, this).apply(block).flush()
    }

    fun infoStream(block: LogStreamBuilder.() -> Unit) {
        LogStreamBuilder(INFO, this).apply(block).flush()
    }

    fun warnStream(block: LogStreamBuilder.() -> Unit) {
        LogStreamBuilder(WARN, this).apply(block).flush()
    }

    fun errorStream(block: LogStreamBuilder.() -> Unit) {
        LogStreamBuilder(ERROR, this).apply(block).flush()
    }

    // special exception helpers
    fun trace(e: Exception) = log(TRACE, exceptionToString(e))
    fun debug(e: Exception) = log(DEBUG, exceptionToString(e))
    fun info(e: Exception) = log(INFO, exceptionToString(e))
    fun warn(e: Exception) = log(WARN, exceptionToString(e))
    fun error(e: Exception) = log(ERROR, exceptionToString(e))

    fun trace(e: Exception, message: () -> String?) = trace(message).also { trace(exceptionToString(e)) }
    fun debug(e: Exception, message: () -> String?) = debug(message).also { debug(exceptionToString(e)) }
    fun info(e: Exception, message: () -> String?) = info(message).also { info(exceptionToString(e)) }
    fun warn(e: Exception, message: () -> String?) = warn(message).also { warn(exceptionToString(e)) }
    fun error(e: Exception, message: () -> String?) = error(message).also { error(exceptionToString(e)) }

    private fun exceptionToString(e: Exception) = "${e.message}\n${e.stackTraceToString()}"

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
    fun log(level: Level, message: String?) {
        if (level.ordinal >= logLevel.ordinal) {
            val timestamp = getCurrentMilliseconds()
            println("[${level.name}][$className][$timestamp] $message")
        }
    }

    fun log(level: Level, message: () -> String?) {
        if (level.ordinal >= logLevel.ordinal) {
            log(level, message())
        }
    }

    companion object {
        private var logLevel: Level = INFO

        /**
         * Sets the global log level.
         * Messages below this level will not be logged.
         */
        fun setLevel(level: Level) {
            logLevel = level
        }

        /**
         * helper for creating loggers for static classes
         */
        fun get(klass: KClass<*>) = cachedLoggerOf(klass)

    }

}