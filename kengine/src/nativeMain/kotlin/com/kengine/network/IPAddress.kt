package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl3.net.SDLNet_GetAddressStatus
import sdl3.net.SDLNet_ResolveHostname
import sdl3.net.SDLNet_WaitUntilResolved
import sdl3.net.SDL_GetError

@OptIn(ExperimentalForeignApi::class)
data class IPAddress(val host: String, val port: UShort) : Logging {

    // cache the resolved address
    private var resolvedAddress: CPointer<cnames.structs.SDLNet_Address>? = null

    /**
     * Converts the host and port into an SDL-compatible IPaddress structure.
     */
    fun toSDL(): CPointer<cnames.structs.SDLNet_Address>? {
        // Return cached address if available
        resolvedAddress?.let { return it }

        logger.info { "Resolving host: $host" }
        val address = if (host == "localhost" || host == "127.0.0.1") {
            SDLNet_ResolveHostname("127.0.0.1") // force numeric IP
        } else {
            SDLNet_ResolveHostname(host)
        }

        if (address == null) {
            logger.error { "Failed to resolve hostname immediately: $host, error: ${SDL_GetError()?.toKString()}" }
            return null
        }

        // SDLNet_WaitUntilResolved returns:
        // 1 if successfully resolved
        // -1 if resolution failed
        // 0 if still resolving
        logger.info { "Waiting for resolution: $host" }
        val resolutionStatus = SDLNet_WaitUntilResolved(address, 5000)
        if (resolutionStatus != 1) {
            logger.error {
                "Resolution ${if (resolutionStatus == 0) "timeout" else "failure"}: $host, error: ${SDL_GetError()?.toKString()}"
            }
            return null
        }

        // SDL3's GetAddressStatus returns:
        // 1 if successfully resolved
        // -1 if resolution failed
        // 0 if still resolving
        if (SDLNet_GetAddressStatus(address) != 1) {
            logger.error { "Address status invalid: $host, error: ${SDL_GetError()?.toKString()}" }
            return null
        }

        logger.info { "Host resolved successfully: $host" }
        resolvedAddress = address
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
