package com.kengine.network

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import kotlinx.cinterop.toKString
import sdl3.net.SDLNet_Init
import sdl3.net.SDLNet_Quit
import sdl3.SDL_GetError

class NetworkContext : Context(), Logging {

    init {
        if (!SDLNet_Init()) {
            throw Exception("Failed to initialize SDL_net: ${SDL_GetError()?.toKString()}")
        }
    }

    private val connections = mutableMapOf<String, NetworkConnection>()

    fun connectTcp(ipAddress: IPAddress): TcpConnection {
        val connection = TcpConnection(ipAddress)
        connection.connect()
        connections[connection.id] = connection
        return connection
    }

    fun connectUdp(ipAddress: IPAddress): UdpConnection {
        val connection = UdpConnection(ipAddress)
        connection.connect()
        connections[connection.id] = connection
        return connection
    }

    fun getConnection(ipAddress: IPAddress): NetworkConnection {
        return connections[ipAddress.toString()]!!
    }

    fun getConnection(id: String): NetworkConnection {
        return connections[id]!!
    }

    fun isConnection(id: String): Boolean {
        return id in connections
    }

    fun closeConnection(id: String) {
        connections[id]?.close()
        connections.remove(id)
    }

    fun closeConnection(connection: NetworkConnection) {
        connections[connection.id]?.close()
        connections.remove(connection.id)
    }

    fun closeAllConnections() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    override fun cleanup() {
        logger.info { "Cleaning up NetworkContext"}
        closeAllConnections()
        SDLNet_Quit()
        currentContext = null
    }

    companion object {
        private var currentContext: NetworkContext? = null

        fun get(): NetworkContext {
            return currentContext ?: NetworkContext().also {
                currentContext = it
            }
        }
    }
}
