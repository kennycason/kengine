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
import sdl3.net.SDLNet_AcceptClient
import sdl3.net.SDLNet_CreateServer
import sdl3.net.SDLNet_DestroyServer
import sdl3.net.SDL_GetError

@OptIn(ExperimentalForeignApi::class)
class TcpServer(
    private val address: IPAddress
) : Logging {
    private var server: CPointer<cnames.structs.SDLNet_Server>? = null
    private var isRunning = false

    fun start() {
        logger.info { "Starting TCP server on ${address.host}:${address.port}" }

        val sdlAddress = address.toSDL()
            ?: throw Exception("Failed to resolve host ${address.host}")

        server = SDLNet_CreateServer(sdlAddress, address.port)
            ?: throw Exception("Failed to create TCP server: ${SDL_GetError()}")

        isRunning = true
        logger.info { "TCP server started on ${address.host}:${address.port}" }
    }

    fun stop() {
        logger.info { "Stopping TCP server" }
        isRunning = false
        server?.let { srv ->
            SDLNet_DestroyServer(srv)
            server = null
        }
    }

    suspend fun acceptConnection(onAccept: suspend (TcpConnection) -> Unit) {
        while (isRunning) {
            server?.let { srv ->
                memScoped {
                    val clientStream = alloc<CPointerVar<cnames.structs.SDLNet_StreamSocket>>()
                    if (SDLNet_AcceptClient(srv, clientStream.ptr)) {
                        logger.info { "Accepted new TCP connection" }
                        val connection = TcpServerConnection(clientStream.value!!)
                        onAccept(connection)
                    }
                }
            }
            delay(100) // Prevent busy waiting
        }
    }

    private class TcpServerConnection(
        socket: CPointer<cnames.structs.SDLNet_StreamSocket>
    ) : TcpConnection(socket) {
        init {
            isRunning = true
        }
    }
}
