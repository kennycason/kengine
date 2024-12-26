package com.kengine.log

import kotlin.reflect.KClass


interface Logging {

    @Suppress("unused")
    val logger: Logger
        get() = cachedLoggerOf(this::class)
}

private val loggerCache = mutableMapOf<KClass<*>, Logger>()

fun cachedLoggerOf(klass: KClass<*>): Logger {
    return loggerCache.getOrPut(klass) { Logger(klass) }
}
