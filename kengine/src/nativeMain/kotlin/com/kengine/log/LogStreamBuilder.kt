package com.kengine.log

class LogStreamBuilder(private val level: Logger.Level, private val logger: Logger) {
    private val messageBuilder = StringBuilder()

    fun write(part: () -> String?): LogStreamBuilder {
        if (level.ordinal >= level.ordinal) {
            write(part())
        }
        return this
    }

    fun write(part: () -> Any): LogStreamBuilder {
        if (level.ordinal >= level.ordinal) {
            write(part())
        }
        return this
    }

    fun write(part: String?): LogStreamBuilder {
        messageBuilder.append(part)
        return this
    }

    fun write(part: Any): LogStreamBuilder {
        messageBuilder.append(part)
        return this
    }

    fun writeLn(part: String?): LogStreamBuilder {
        messageBuilder.append(part)
        return ln()
    }

    fun writeLn(part: Any): LogStreamBuilder {
        messageBuilder.append(part)
        return ln()
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