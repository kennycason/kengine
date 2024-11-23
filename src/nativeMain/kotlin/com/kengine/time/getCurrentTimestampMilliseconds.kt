package com.kengine.time

fun getCurrentTimestampMilliseconds(): Long {
    return getCurrentTimestampMicroseconds() / 1_000L // Divide by 1000 to get milliseconds
}