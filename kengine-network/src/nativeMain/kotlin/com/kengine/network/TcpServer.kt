package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import sdl3.net.NET_AcceptClient
import sdl3.net.NET_CreateServer
import sdl3.net.NET_DestroyServer
import sdl3.SDL_GetError

@OptIn(ExperimentalForeignApi::class)
class TcpServer(
    private val address: IPAddress
) : Logging {
    private var server: CPointer<cnames.structs.NET_Server>? = null
    private var isRunning = false

    fun start() {
        logger.info { "Starting TCP server on ${address.host}:${address.port}" }

        val sdlAddress = address.toSDL()
            ?: throw Exception("Failed to resolve host ${address.host}")

        server = NET_CreateServer(sdlAddress, address.port, 0u)
            ?: throw Exception("Failed to create TCP server: ${SDL_GetError()}")

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

    suspend fun acceptConnection(onAccept: suspend (TcpConnection) -> Unit) {
        while (isRunning) {
            server?.let { srv ->
                memScoped {
                    val clientStream = alloc<CPointerVar<cnames.structs.NET_StreamSocket>>()
                    val acceptResult = NET_AcceptClient(srv, clientStream.ptr)

                    if (acceptResult) {
                        clientStream.value?.let { socket ->
                            logger.debug { "Accepted new TCP connection" }
                            val connection = TcpServerConnection(socket)
                            try {
                                onAccept(connection)
                            } catch (e: Exception) {
                                logger.error(e) { "Error handling accepted connection" }
                                connection.close()
                            }
                        } ?: run {
                            if (logger.isTraceEnabled()) {
                                logger.trace { "Accepted null client stream" }
                            }
                        }
                    }
                }
            }
            delay(10)  // low delay to be more responsive
        }
    }

    private class TcpServerConnection(
        socket: CPointer<cnames.structs.NET_StreamSocket>
    ) : TcpConnection(socket) {
        init {
            isRunning = true
            logger.info { "Created server-side connection" }
        }
    }
}
