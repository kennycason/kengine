package com.kengine.network

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.net.SDLNet_GetAddressStatus
import sdl3.net.SDLNet_ResolveHostname
import sdl3.net.SDLNet_WaitUntilResolved

@OptIn(ExperimentalForeignApi::class)
data class IPAddress(val host: String, val port: UShort) {

    /**
     * Converts the host and port into an SDL-compatible IPaddress structure.
     */
    fun toSDL(): CPointer<cnames.structs.SDLNet_Address>? {
        val address = SDLNet_ResolveHostname(host)
        // wait for resolution (TODO make timeout configurable)
        if (SDLNet_WaitUntilResolved(address, 5000) != 0) {
            return null // resolution failed or timed out
        }
        if (SDLNet_GetAddressStatus(address) <= 0) {
            return null
        }
        return address
    }

    override fun toString(): String {
        return "${host}:${port}"
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
