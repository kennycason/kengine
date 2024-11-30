package com.kengine.log

fun getLogger(klass: kotlin.reflect.KClass<*>): Logger = Logger(klass)
fun getLogger(klassName: String): Logger = Logger(klassName)