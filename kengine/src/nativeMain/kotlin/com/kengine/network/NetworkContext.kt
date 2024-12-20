package com.kengine.network

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class NetworkContext : Context(), Logging {

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
        closeAllConnections()
    }

    companion object {
        private var currentContext: NetworkContext? = null

        fun get(): NetworkContext {
            if (currentContext == null) {
                currentContext = NetworkContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create NetworkContext")
        }
    }
}
