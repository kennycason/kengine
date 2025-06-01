package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl3.net.SDLNet_CreateClient
import sdl3.net.SDLNet_DestroyStreamSocket
import sdl3.net.SDLNet_ReadFromStreamSocket
import sdl3.net.SDLNet_WaitUntilConnected
import sdl3.net.SDLNet_WriteToStreamSocket
import sdl3.SDL_GetError

@OptIn(ExperimentalForeignApi::class)
open class TcpConnection private constructor(
    private val address: IPAddress,
    initialSocket: CPointer<cnames.structs.SDLNet_StreamSocket>?,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : NetworkConnection, Logging, AutoCloseable {

    // Primary constructor for normal connections
    constructor(
        address: IPAddress,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) : this(address, null, bufferSize)

    // Protected constructor for server connections
    protected constructor(
        socket: CPointer<cnames.structs.SDLNet_StreamSocket>
    ) : this(IPAddress("0.0.0.0", 0u), socket)


    companion object {
        const val DEFAULT_BUFFER_SIZE = 1024
    }

    private var streamSocket: CPointer<cnames.structs.SDLNet_StreamSocket>? = initialSocket
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var isRunning = false

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        logger.info { "Starting TCP connection on ${address.host}:${address.port}" }

        val sdlAddress = address.toSDL()
            ?: throw Exception("Failed to resolve host ${address.host}")

        streamSocket = SDLNet_CreateClient(sdlAddress, address.port)
            ?: throw Exception("Failed to create TCP client: ${SDL_GetError()}")

        // Wait for connection (SDL3 returns 1 for success)
        val connectionStatus = SDLNet_WaitUntilConnected(streamSocket, 5000)
        if (connectionStatus != 1) {
            throw Exception("Failed to connect: ${
                if (connectionStatus == 0) "Connection timed out"
                else SDL_GetError()?.toKString() ?: "Unknown error"
            }")
        }

        isRunning = true
        logger.info { "TCP connection established: $id" }
    }

    override fun close() {
        logger.info { "Closing TCP connection: $id" }
        isRunning = false
        runBlocking {
            receiveJob?.cancelAndJoin()
        }
        streamSocket?.let { socket ->
            SDLNet_DestroyStreamSocket(socket)
            streamSocket = null
        }
    }

    fun send(data: ByteArray) {
        if (!isRunning) throw IllegalStateException("Connection is not open")

        streamSocket?.let { socket ->
            data.usePinned { pinned ->
                val success = SDLNet_WriteToStreamSocket(socket, pinned.addressOf(0), data.size.convert())
                if (!success) {
                    throw Exception("Failed to send TCP data: ${SDL_GetError()}")
                }
                if (logger.isDebugEnabled()) {
                    logger.debug { "Sent ${data.size} bytes" }
                }
            }
        } ?: throw IllegalStateException("TCP socket is not open")
    }

    fun send(data: UByteArray) {
        send(data.map { it.toByte() }.toByteArray())
    }

    fun send(data: String) {
        send(data.encodeToByteArray())
    }

    fun <T> send(data: T, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, data)
        send(json.encodeToByteArray())
    }

    override fun subscribe(onReceive: (ByteArray) -> Unit) {
        if (!isRunning) throw IllegalStateException("Connection is not open")
        if (receiveJob != null) throw IllegalStateException("Already subscribed")

        receiveJob = scope.launch {
            val buffer = ByteArray(bufferSize)

            try {
                while (isRunning && isActive) {
                    val shouldContinue = streamSocket?.let { socket ->
                        buffer.usePinned { pinned ->
                            val received = SDLNet_ReadFromStreamSocket(
                                socket,
                                pinned.addressOf(0),
                                bufferSize.convert()
                            )

                            when {
                                received > 0 -> {
                                    val data = buffer.copyOf(received)
                                    if (logger.isDebugEnabled()) {
                                        logger.debug { "Received $received bytes" }
                                    }

                                    launch(Dispatchers.Default) {
                                        try {
                                            onReceive(data)
                                        } catch (e: Exception) {
                                            logger.error(e) { "Error in receive callback" }
                                        }
                                    }
                                    true // continue receiving
                                }
                                received == 0 -> {
                                    logger.info { "Connection closed normally" }
                                    isRunning = false
                                    false
                                }
                                else -> {
                                    logger.info { "Connection error: ${SDL_GetError()}" }
                                    isRunning = false
                                    false
                                }
                            }
                        }
                    } ?: false

                    if (!shouldContinue) {
                        break
                    }

                    delay(1) // prevent busy waiting
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in receive loop" }
                isRunning = false
                throw e
            }
        }
    }

    override fun subscribe(onReceive: (UByteArray) -> Unit) {
        subscribe { byteArray: ByteArray ->
            onReceive(byteArray.map { it.toUByte() }.toUByteArray())
        }
    }

    override fun subscribe(onReceive: (String) -> Unit) {
        subscribe { byteArray: ByteArray ->
            try {
                onReceive(byteArray.decodeToString())
            } catch (e: Exception) {
                logger.error(e) { "Error decoding received data as string" }
            }
        }
    }

    override fun <T> subscribe(onReceive: (T) -> Unit, serializer: KSerializer<T>) {
        subscribe { byteArray: ByteArray ->
            try {
                val json = byteArray.decodeToString()
                onReceive(Json.decodeFromString(serializer, json))
            } catch (e: Exception) {
                logger.error(e) { "Error deserializing received data" }
            }
        }
    }
}
