package com.kengine.time

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

fun getCurrentTimestampMicroseconds(): Long {
    memScoped {
        val timespec = alloc<timespec>()
        clock_gettime(CLOCK_REALTIME.toUInt(), timespec.ptr) // High-precision clock
        val seconds = timespec.tv_sec
        val nanoseconds = timespec.tv_nsec
        return (seconds * 1_000_000L) + (nanoseconds / 1_000L) // Convert nanoseconds to microseconds
    }
}

