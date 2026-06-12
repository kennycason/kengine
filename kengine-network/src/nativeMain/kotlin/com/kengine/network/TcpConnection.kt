package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl3.SDL_GetError
import sdl3.net.NET_CreateClient
import sdl3.net.NET_DestroyStreamSocket
import sdl3.net.NET_ReadFromStreamSocket
import sdl3.net.NET_WaitUntilConnected
import sdl3.net.NET_WriteToStreamSocket

@OptIn(ExperimentalForeignApi::class)
open class TcpConnection private constructor(
    private val address: IPAddress,
    initialSocket: CPointer<cnames.structs.NET_StreamSocket>?,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val connectionId: String? = null,
    private val canConnect: Boolean = initialSocket == null
) : NetworkConnection, Logging, AutoCloseable {

    constructor(
        address: IPAddress,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) : this(address, null, bufferSize, null, true)

    protected constructor(
        socket: CPointer<cnames.structs.NET_StreamSocket>,
        id: String
    ) : this(IPAddress("0.0.0.0", 0u), socket, DEFAULT_BUFFER_SIZE, id, false)

    companion object {
        const val DEFAULT_BUFFER_SIZE = 1024
        private const val FRAME_HEADER_SIZE = 4
        private const val MAX_FRAME_SIZE = 16 * 1024 * 1024
    }

    private var streamSocket: CPointer<cnames.structs.NET_StreamSocket>? = initialSocket
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isClosed = false

    var isRunning = initialSocket != null
        protected set

    override val id: String
        get() = connectionId ?: "${address.host}:${address.port}"

    override fun connect() {
        if (!canConnect) {
            throw IllegalStateException("Accepted TCP connections are already connected")
        }
        if (isClosed) {
            throw IllegalStateException("Connection is closed and cannot be reused")
        }
        if (streamSocket != null || isRunning) {
            throw IllegalStateException("Connection is already open")
        }

        logger.info { "Starting TCP connection on ${address.host}:${address.port}" }

        val socket = address.withSDLAddress { sdlAddress ->
            NET_CreateClient(sdlAddress, address.port, 0u)
                ?: throw Exception("Failed to create TCP client: ${SDL_GetError()?.toKString()}")
        }
        streamSocket = socket

        val connectionStatus = NET_WaitUntilConnected(socket, 5000)
        if (connectionStatus != 1) {
            NET_DestroyStreamSocket(socket)
            streamSocket = null
            throw Exception(
                "Failed to connect: ${
                    if (connectionStatus == 0) "Connection timed out"
                    else SDL_GetError()?.toKString() ?: "Unknown error"
                }"
            )
        }

        isRunning = true
        logger.info { "TCP connection established: $id" }
    }

    override fun close() {
        if (isClosed) {
            return
        }

        logger.info { "Closing TCP connection: $id" }
        isClosed = true
        isRunning = false
        runBlocking {
            receiveJob?.cancelAndJoin()
        }
        scope.cancel()
        streamSocket?.let { socket ->
            NET_DestroyStreamSocket(socket)
            streamSocket = null
        }
    }

    fun send(data: ByteArray) {
        write(data)
    }

    fun send(data: UByteArray) {
        send(data.map { it.toByte() }.toByteArray())
    }

    fun send(data: String) {
        sendFrame(data.encodeToByteArray())
    }

    fun <T> send(data: T, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, data)
        sendFrame(json.encodeToByteArray())
    }

    private fun write(data: ByteArray) {
        if (!isRunning) throw IllegalStateException("Connection is not open")
        if (isClosed) throw IllegalStateException("Connection is closed")
        if (data.isEmpty()) return

        streamSocket?.let { socket ->
            data.usePinned { pinned ->
                val success = NET_WriteToStreamSocket(socket, pinned.addressOf(0), data.size.convert())
                if (!success) {
                    throw Exception("Failed to send TCP data: ${SDL_GetError()?.toKString()}")
                }
                if (logger.isDebugEnabled()) {
                    logger.debug { "Sent ${data.size} bytes" }
                }
            }
        } ?: throw IllegalStateException("TCP socket is not open")
    }

    private fun sendFrame(data: ByteArray) {
        require(data.size <= MAX_FRAME_SIZE) {
            "TCP frame size ${data.size} exceeds max frame size $MAX_FRAME_SIZE"
        }

        val frame = ByteArray(FRAME_HEADER_SIZE + data.size)
        frame[0] = ((data.size ushr 24) and 0xFF).toByte()
        frame[1] = ((data.size ushr 16) and 0xFF).toByte()
        frame[2] = ((data.size ushr 8) and 0xFF).toByte()
        frame[3] = (data.size and 0xFF).toByte()
        data.copyInto(frame, destinationOffset = FRAME_HEADER_SIZE)
        write(frame)
    }

    override fun subscribe(onReceive: (ByteArray) -> Unit) {
        startReceiveLoop { data ->
            dispatchReceive("Error in receive callback") {
                onReceive(data)
            }
        }
    }

    override fun subscribe(onReceive: (UByteArray) -> Unit) {
        subscribe { byteArray: ByteArray ->
            onReceive(byteArray.map { it.toUByte() }.toUByteArray())
        }
    }

    override fun subscribe(onReceive: (String) -> Unit) {
        subscribeFrames { byteArray ->
            try {
                onReceive(byteArray.decodeToString())
            } catch (e: Exception) {
                logger.error(e) { "Error decoding received data as string" }
            }
        }
    }

    override fun <T> subscribe(onReceive: (T) -> Unit, serializer: KSerializer<T>) {
        subscribeFrames { byteArray ->
            try {
                val json = byteArray.decodeToString()
                onReceive(Json.decodeFromString(serializer, json))
            } catch (e: Exception) {
                logger.error(e) { "Error deserializing received data" }
            }
        }
    }

    private fun subscribeFrames(onReceive: (ByteArray) -> Unit) {
        val frameDecoder = FrameDecoder(MAX_FRAME_SIZE)
        startReceiveLoop { data ->
            frameDecoder.accept(data) { frame ->
                dispatchReceive("Error in framed receive callback") {
                    onReceive(frame)
                }
            }
        }
    }

    private fun startReceiveLoop(onReceive: (ByteArray) -> Unit) {
        if (!isRunning) throw IllegalStateException("Connection is not open")
        if (isClosed) throw IllegalStateException("Connection is closed")
        if (receiveJob != null) throw IllegalStateException("Already subscribed")

        receiveJob = scope.launch {
            val buffer = ByteArray(bufferSize)

            try {
                while (isRunning && isActive) {
                    var shouldDelay = false
                    val shouldContinue = streamSocket?.let { socket ->
                        buffer.usePinned { pinned ->
                            val received = NET_ReadFromStreamSocket(
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
                                    onReceive(data)
                                    true
                                }
                                received == 0 -> {
                                    shouldDelay = true
                                    true
                                }
                                else -> {
                                    logger.info { "Connection error: ${SDL_GetError()?.toKString()}" }
                                    isRunning = false
                                    false
                                }
                            }
                        }
                    } ?: false

                    if (!shouldContinue) {
                        break
                    }
                    if (shouldDelay) {
                        delay(1)
                    }
                }
            } catch (e: CancellationException) {
                isRunning = false
            } catch (e: Exception) {
                logger.error(e) { "Error in receive loop" }
                isRunning = false
                throw e
            }
        }
    }

    private fun dispatchReceive(errorMessage: String, onReceive: () -> Unit) {
        try {
            onReceive()
        } catch (e: Exception) {
            logger.error(e) { errorMessage }
        }
    }

    private class FrameDecoder(
        private val maxFrameSize: Int
    ) {
        private var pending = ByteArray(0)

        fun accept(chunk: ByteArray, onFrame: (ByteArray) -> Unit) {
            val buffer = ByteArray(pending.size + chunk.size)
            pending.copyInto(buffer)
            chunk.copyInto(buffer, destinationOffset = pending.size)

            var offset = 0
            while (buffer.size - offset >= FRAME_HEADER_SIZE) {
                val frameSize = readFrameSize(buffer, offset)
                if (frameSize < 0 || frameSize > maxFrameSize) {
                    throw IllegalStateException("TCP frame size $frameSize exceeds max frame size $maxFrameSize")
                }

                val frameStart = offset + FRAME_HEADER_SIZE
                val frameEnd = frameStart + frameSize
                if (buffer.size < frameEnd) {
                    break
                }

                onFrame(buffer.copyOfRange(frameStart, frameEnd))
                offset = frameEnd
            }

            pending = if (offset == buffer.size) {
                ByteArray(0)
            } else {
                buffer.copyOfRange(offset, buffer.size)
            }
        }

        private fun readFrameSize(buffer: ByteArray, offset: Int): Int {
            return ((buffer[offset].toInt() and 0xFF) shl 24) or
                ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
                ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
                (buffer[offset + 3].toInt() and 0xFF)
        }
    }
}
