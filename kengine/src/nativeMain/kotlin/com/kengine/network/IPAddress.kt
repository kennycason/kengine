package com.kengine.network

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import sdl2.net.IPaddress

@OptIn(ExperimentalForeignApi::class)
class IPAddress(val host: String, val port: Int) {

    /**
     * Converts the host and port into an SDL-compatible IPaddress structure.
     */
    fun toSDL(): CPointer<IPaddress> {
        val numericIP = toNumericIP(host) // Convert the host to numeric form
        val thisPort = port
        return nativeHeap.alloc<IPaddress>().apply {
            this.host = numericIP
            this.port = thisPort.convert()
        }.ptr
    }

    /**
     * Helper functions to format a UInt <> a dotted IP string.
     */
    companion object {
        private fun formatNumericIP(ip: UInt): String {
            val byte1 = (ip shr 24).toInt() and 0xFF
            val byte2 = (ip shr 16).toInt() and 0xFF
            val byte3 = (ip shr 8).toInt() and 0xFF
            val byte4 = ip.toInt() and 0xFF
            return "$byte1.$byte2.$byte3.$byte4"
        }

        fun toNumericIP(ip: String): UInt {
            val parts = ip.split(".").map { it.toInt() }
            if (parts.size != 4 || parts.any { it !in 0..255 }) {
                throw IllegalArgumentException("Invalid IPv4 address: $ip")
            }
            return ((parts[0] shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]).toUInt()
        }
    }
}