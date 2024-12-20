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
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
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
import sdl3.net.SDLNet_CreateDatagramSocket
import sdl3.net.SDLNet_Datagram
import sdl3.net.SDLNet_DestroyDatagram
import sdl3.net.SDLNet_DestroyDatagramSocket
import sdl3.net.SDLNet_ReceiveDatagram
import sdl3.net.SDLNet_SendDatagram
import sdl3.net.SDLNet_WaitUntilResolved
import sdl3.net.SDL_GetError

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

    private var datagramSocket: CPointer<cnames.structs.SDLNet_DatagramSocket>? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        logger.info { "Starting UDP connection on ${address.host}:${address.port}" }

        val sdlAddress = address.toSDL()
            ?: throw Exception("Failed to resolve host ${address.host}")

        // Wait for address resolution to complete
        if (SDLNet_WaitUntilResolved(sdlAddress, 5000) != 0) {
            throw Exception("Failed to resolve address: ${SDL_GetError()}")
        }

        datagramSocket = SDLNet_CreateDatagramSocket(sdlAddress, address.port)
            ?: throw Exception("Failed to create UDP socket: ${SDL_GetError()}")

        isRunning = true
        logger.info { "UDP connection established: $id" }
    }

    override fun close() {
        logger.info { "Closing UDP connection: $id" }
        isRunning = false
        runBlocking {
            receiveJob?.cancelAndJoin()
        }
        datagramSocket?.let { socket ->
            SDLNet_DestroyDatagramSocket(socket)
            datagramSocket = null
        }
    }

    fun send(data: ByteArray, remoteAddress: IPAddress) {
        if (!isRunning) throw IllegalStateException("Connection is not open")

        datagramSocket?.let { socket ->
            val sdlRemoteAddr = remoteAddress.toSDL()
                ?: throw Exception("Failed to resolve remote host ${remoteAddress.host}")

            data.usePinned { pinned ->
                val success = SDLNet_SendDatagram(
                    socket,
                    sdlRemoteAddr,
                    remoteAddress.port,
                    pinned.addressOf(0),
                    data.size.convert()
                )

                if (!success) {
                    throw Exception("Failed to send UDP datagram: ${SDL_GetError()}")
                }

                if (logger.isDebugEnabled()) {
                    logger.debug { "Sent ${data.size} bytes to ${remoteAddress.host}:${remoteAddress.port}" }
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
        if (receiveJob != null) throw IllegalStateException("Already subscribed")

        receiveJob = scope.launch {
            try {
                while (isRunning && isActive) {
                    datagramSocket?.let { socket ->
                        memScoped {
                            val dgramPtr = alloc<CPointerVar<SDLNet_Datagram>>()
                            val success = SDLNet_ReceiveDatagram(socket, dgramPtr.ptr)

                            if (success) {
                                val dgram = dgramPtr.value
                                // TODO: Extract data from dgram into a ByteArray and pass to onReceive

                                if (logger.isDebugEnabled()) {
                                    dgram?.pointed?.addr?.let { senderAddr ->
                                        logger.debug { "Received datagram from remote endpoint: $senderAddr" }
                                    }
                                }

                                // Free the datagram after we're done with it
                                dgram?.let { SDLNet_DestroyDatagram(it) }
                            } else {
                                delay(timeMillis = 1)
                            }
                        }
                    } ?: break
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
