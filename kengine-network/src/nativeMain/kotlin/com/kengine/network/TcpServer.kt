package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sdl3.SDL_GetError
import sdl3.net.NET_AcceptClient
import sdl3.net.NET_CreateServer
import sdl3.net.NET_DestroyServer
import sdl3.net.NET_GetAddressString
import sdl3.net.NET_GetStreamSocketAddress
import sdl3.net.NET_UnrefAddress

@OptIn(ExperimentalForeignApi::class)
class TcpServer(
    private val address: IPAddress
) : Logging {
    private var server: CPointer<cnames.structs.NET_Server>? = null
    private var isRunning = false
    private var acceptedConnectionCount = 0

    fun start() {
        logger.info { "Starting TCP server on ${address.host}:${address.port}" }

        server = address.withSDLAddress { sdlAddress ->
            NET_CreateServer(sdlAddress, address.port, 0u)
                ?: throw Exception("Failed to create TCP server: ${SDL_GetError()?.toKString()}")
        }

        isRunning = true
        logger.info { "TCP server started on ${address.host}:${address.port}" }
    }

    fun stop() {
        logger.info { "Stopping TCP server" }
        isRunning = false
        server?.let { srv ->
            NET_DestroyServer(srv)
            server = null
        }
    }

    suspend fun acceptConnection(onAccept: suspend (TcpConnection) -> Unit) = coroutineScope {
        while (isRunning) {
            var acceptedAny = false
            server?.let { srv ->
                do {
                    var acceptedConnection: TcpConnection? = null
                    acceptedConnection = memScoped {
                        val clientStream = alloc<CPointerVar<cnames.structs.NET_StreamSocket>>()
                        val acceptResult = NET_AcceptClient(srv, clientStream.ptr)

                        if (!acceptResult) {
                            throw Exception("Failed to accept TCP client: ${SDL_GetError()?.toKString()}")
                        }

                        clientStream.value?.let { socket ->
                            acceptedConnectionCount += 1
                            TcpServerConnection(socket, createConnectionId(socket, acceptedConnectionCount))
                        }
                    }

                    val connection = acceptedConnection
                    if (connection != null) {
                        acceptedAny = true
                        logger.debug { "Accepted new TCP connection: ${connection.id}" }
                        launch {
                            try {
                                onAccept(connection)
                            } catch (e: Exception) {
                                logger.error(e) { "Error handling accepted connection" }
                                connection.close()
                            }
                        }
                    }
                } while (acceptedConnection != null && isRunning)
            }
            if (!acceptedAny) {
                delay(10)
            }
        }
    }

    private fun createConnectionId(
        socket: CPointer<cnames.structs.NET_StreamSocket>,
        connectionNumber: Int
    ): String {
        val remoteAddress = NET_GetStreamSocketAddress(socket) ?: return "tcp-server-connection-$connectionNumber"
        return try {
            val remoteHost = NET_GetAddressString(remoteAddress)?.toKString()
            if (remoteHost == null) {
                "tcp-server-connection-$connectionNumber"
            } else {
                "$remoteHost#$connectionNumber"
            }
        } finally {
            NET_UnrefAddress(remoteAddress)
        }
    }

    private class TcpServerConnection(
        socket: CPointer<cnames.structs.NET_StreamSocket>,
        id: String
    ) : TcpConnection(socket, id) {
        init {
            logger.info { "Created server-side connection: $id" }
        }
    }
}
