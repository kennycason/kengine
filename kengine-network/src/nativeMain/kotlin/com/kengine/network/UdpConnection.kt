package com.kengine.network

import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import platform.posix.memcpy
import sdl3.net.NET_CreateDatagramSocket
import sdl3.net.NET_Datagram
import sdl3.net.NET_DestroyDatagram
import sdl3.net.NET_DestroyDatagramSocket
import sdl3.net.NET_GetAddressString
import sdl3.net.NET_ReceiveDatagram
import sdl3.net.NET_SendDatagram
import sdl3.net.NET_UnrefAddress
import sdl3.net.NET_WaitUntilInputAvailable
import sdl3.SDL_GetError

/**
 * Connectionless protocol
 * No formal server/client roles
 * Any UDP socket can both send and receive
 */
@OptIn(ExperimentalForeignApi::class)
class UdpConnection(
    private val address: IPAddress,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : NetworkConnection, Logging, AutoCloseable {

    companion object {
        const val DEFAULT_BUFFER_SIZE = 1024
    }

    private var datagramSocket: CPointer<cnames.structs.NET_DatagramSocket>? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false
    private var isClosed = false

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        if (isClosed) {
            throw IllegalStateException("Connection is closed and cannot be reused")
        }
        if (datagramSocket != null || isRunning) {
            throw IllegalStateException("Connection is already open")
        }

        logger.info { "Starting UDP connection on ${address.host}:${address.port}" }

        datagramSocket = address.withSDLAddress { sdlAddress ->
            NET_CreateDatagramSocket(sdlAddress, address.port, 0u)
                ?: throw Exception("Failed to create UDP socket: ${SDL_GetError()?.toKString()}")
        }

        isRunning = true
        logger.info { "UDP connection established: $id" }
    }

    override fun close() {
        if (isClosed) {
            return
        }

        logger.info { "Closing UDP connection: $id" }
        isClosed = true
        isRunning = false
        runBlocking {
            receiveJob?.cancelAndJoin()
        }
        scope.cancel()
        datagramSocket?.let { socket ->
            NET_DestroyDatagramSocket(socket)
            datagramSocket = null
        }
    }

    fun send(data: ByteArray, remoteAddress: IPAddress) {
        if (!isRunning) throw IllegalStateException("Connection is not open")
        if (isClosed) throw IllegalStateException("Connection is closed")
        if (data.isEmpty()) return

        datagramSocket?.let { socket ->
            remoteAddress.withSDLAddress { sdlRemoteAddr ->
                data.usePinned { pinned ->
                    logger.debug { "Attempting to send ${data.size} bytes" }
                    val success = NET_SendDatagram(
                        socket,
                        sdlRemoteAddr,
                        remoteAddress.port,
                        pinned.addressOf(0),
                        data.size.convert()
                    )

                    if (!success) {
                        throw Exception("Failed to send UDP datagram: ${SDL_GetError()?.toKString()}")
                    }

                    logger.info { "Successfully sent ${data.size} bytes to ${remoteAddress.host}:${remoteAddress.port}" }
                }
            }
        } ?: throw IllegalStateException("UDP socket is not open")
    }

    fun send(data: UByteArray, remoteAddress: IPAddress) {
        send(data.map { it.toByte() }.toByteArray(), remoteAddress)
    }

    fun send(data: String, remoteAddress: IPAddress) {
        send(data.encodeToByteArray(), remoteAddress)
    }

    fun <T> send(data: T, remoteAddress: IPAddress, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, data)
        send(json.encodeToByteArray(), remoteAddress)
    }

    override fun subscribe(onReceive: (ByteArray) -> Unit) {
        if (!isRunning) throw IllegalStateException("Connection is not open")
        if (isClosed) throw IllegalStateException("Connection is closed")
        if (receiveJob != null) throw IllegalStateException("Already subscribed")

        receiveJob = scope.launch {
            try {
                logger.info { "Starting receive loop on ${address.host}:${address.port}" }
                while (isRunning && isActive) {
                    datagramSocket?.let { socket ->
                        memScoped {
                            // wait for incoming data
                            val socketPtr = alloc<CPointerVar<cnames.structs.NET_DatagramSocket>>()
                            socketPtr.value = socket

                            val waitResult = NET_WaitUntilInputAvailable(
                                socketPtr.ptr.reinterpret(),
                                1,
                                100
                            )
                            logger.debug { "Wait result: $waitResult" }

                            if (waitResult > 0) {
                                val dgramPtr = alloc<CPointerVar<NET_Datagram>>()
                                val receiveResult = NET_ReceiveDatagram(socket, dgramPtr.ptr)
                                logger.debug { "Receive attempt result: $receiveResult" }

                                if (receiveResult) {
                                    val dgram = dgramPtr.value
                                    dgram?.let { datagram ->
                                        logger.info {
                                            "Received datagram - Size: ${datagram.pointed.buflen}, " +
                                                "From: ${NET_GetAddressString(datagram.pointed.addr)}:${datagram.pointed.port}"
                                        }

                                        val data = try {
                                            val bytes = ByteArray(datagram.pointed.buflen)
                                            bytes.usePinned { pinnedData ->
                                                memcpy(
                                                    pinnedData.addressOf(0),
                                                    datagram.pointed.buf,
                                                    datagram.pointed.buflen.convert()
                                                )
                                            }
                                            bytes
                                        } finally {
                                            NET_DestroyDatagram(datagram)
                                        }

                                        dispatchReceive {
                                            onReceive(data)
                                        }
                                    }
                                } else {
                                    logger.info { "UDP receive error: ${SDL_GetError()?.toKString()}" }
                                    isRunning = false
                                }
                            } else if (waitResult < 0) {
                                logger.info { "UDP wait error: ${SDL_GetError()?.toKString()}" }
                                isRunning = false
                            }
                        }
                    } ?: break
                }
                logger.info { "Receive loop ending on ${address.host}:${address.port}" }
            } catch (e: CancellationException) {
                isRunning = false
            } catch (e: Exception) {
                logger.error(e) { "Error in receive loop" }
                isRunning = false
                throw e
            }
        }
    }

    private fun dispatchReceive(onReceive: () -> Unit) {
        scope.launch {
            try {
                onReceive()
            } catch (e: Exception) {
                logger.error(e) { "Error in receive callback" }
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

    fun resolveAddressOrThrow(host: String): IPAddress {
        val address = IPAddress(host, 0u) // port is irrelevant for resolution.
        val sdlAddress = address.toSDL()
            ?: throw Exception("Failed to resolve address: $host -> ${SDL_GetError()?.toKString()}")
        NET_UnrefAddress(sdlAddress)
        return address
    }
}
