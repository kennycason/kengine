package com.kengine.network

import com.kengine.log.Logging
import com.kengine.sdl.cinterop.SDLNet_UDPsocket
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
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
import platform.posix.memcpy
import sdl2.net.IPaddress
import sdl2.net.SDLNet_AllocPacket
import sdl2.net.SDLNet_FreePacket
import sdl2.net.SDLNet_GetError
import sdl2.net.SDLNet_ResolveHost
import sdl2.net.SDLNet_UDP_Close
import sdl2.net.SDLNet_UDP_Open
import sdl2.net.SDLNet_UDP_Recv
import sdl2.net.SDLNet_UDP_Send

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
        const val DEFAULT_BUFFER_SIZE = 1024 // TODO make configurable
    }

    private var udpSocket: CPointer<SDLNet_UDPsocket>? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        logger.info { "Starting UDP connection on ${address.host}:${address.port}" }
        udpSocket = SDLNet_UDP_Open(address.port.convert())?.reinterpret()
            ?: throw Exception("Failed to open UDP socket: ${SDLNet_GetError()}")
        isRunning = true
        logger.info { "UDP connection established: $id" }
    }

    override fun close() {
        logger.info { "Closing UDP connection: $id" }
        isRunning = false
        runBlocking {
            receiveJob?.cancelAndJoin()
        }
        udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
            SDLNet_UDP_Close(socket)
            udpSocket = null
        }
    }

    fun send(data: ByteArray, remoteAddress: IPAddress) {
        if (!isRunning) throw IllegalStateException("Connection is not open")

        memScoped {
            val packet = SDLNet_AllocPacket(data.size.convert())
                ?: throw Exception("Failed to allocate packet")

            try {
                val sdlRemoteAddr = alloc<IPaddress>()
                if (SDLNet_ResolveHost(sdlRemoteAddr.ptr, remoteAddress.host, remoteAddress.port) < 0) {
                    throw Exception("Failed to resolve remote host: ${SDLNet_GetError()}")
                }

                packet.pointed.address.host = sdlRemoteAddr.host
                packet.pointed.address.port = sdlRemoteAddr.port

                // copy data into packet
                data.usePinned { pinned ->
                    memcpy(packet.pointed.data, pinned.addressOf(0), data.size.convert())
                }
                packet.pointed.len = data.size.convert()
                packet.pointed.maxlen = data.size.convert()

                udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                    // send without specifying a channel (-1), TOOD confirm how best to handle this
                    val sent = SDLNet_UDP_Send(socket, -1, packet)
                    if (sent == 0) {
                        throw Exception("Failed to send UDP packet: ${SDLNet_GetError()}")
                    }
                    logger.info { "Sent ${data.size} bytes to ${remoteAddress.host}:${remoteAddress.port}" }
                } ?: throw IllegalStateException("UDP socket is not open")
            } finally {
                SDLNet_FreePacket(packet)
            }
        }
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
            memScoped {
                val packet = SDLNet_AllocPacket(bufferSize.convert())
                    ?: throw Exception("Failed to allocate receive packet")

                try {
                    while (isRunning && isActive) {
                        udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                            if (SDLNet_UDP_Recv(socket, packet) > 0) {
                                val data = packet.pointed.data!!.readBytes(packet.pointed.len.convert())
                                logger.debug { "Received ${data.size} bytes from ${packet.pointed.address.host}:${packet.pointed.address.port}" }

                                launch(Dispatchers.Default) {
                                    try {
                                        onReceive(data)
                                    } catch (e: Exception) {
                                        logger.error(e) { "Error in receive callback" }
                                    }
                                }
                            } else {
                                delay(1)
                            }
                        } ?: break
                    }
                } finally {
                    SDLNet_FreePacket(packet)
                }
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