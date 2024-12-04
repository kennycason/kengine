package com.kengine.log

class LogStreamBuilder(private val level: Logger.Level, private val logger: Logger) {
    private val messageBuilder = StringBuilder()

    fun write(part: () -> String?): LogStreamBuilder {
        if (level.ordinal >= level.ordinal) {
            write(part())
        }
        return this
    }

    fun write(part: String?): LogStreamBuilder {
        messageBuilder.append(part)
        return this
    }

    fun <R> write(parts: Iterable<R>, toString: (R) -> String?): LogStreamBuilder {
        parts.forEach { write(toString(it)) }
        return this
    }

    fun <R> write(parts: Iterable<R>): LogStreamBuilder {
        parts.forEach { write(it.toString()) }
        return this
    }

    fun writeLn(part: () -> String?): LogStreamBuilder {
        if (level.ordinal >= level.ordinal) {
            write(part())
        }
        return ln()
    }

    fun writeLn(part: String?): LogStreamBuilder {
        messageBuilder.append(part)
        return ln()
    }

    fun <R> writeLn(parts: Iterable<R>, toString: (R) -> String?): LogStreamBuilder {
        parts.forEach { writeLn(toString(it)) }
        return this
    }

    fun <R> writeLn(parts: Iterable<R>): LogStreamBuilder {
        parts.forEach { writeLn(it.toString()) }
        return this
    }

    fun tab(): LogStreamBuilder {
        messageBuilder.append('\t')
        return this
    }

    fun ln(): LogStreamBuilder {
        messageBuilder.append('\n')
        return this
    }

    fun flush(): Logger {
        logger.log(level, messageBuilder.toString())
        return logger
    }

}