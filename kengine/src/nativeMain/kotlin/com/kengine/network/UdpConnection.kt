package com.kengine.network

import com.kengine.log.Logging
import com.kengine.sdl.cinterop.SDLNet_UDPsocket
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl2.net.SDLNet_AllocPacket
import sdl2.net.SDLNet_FreePacket
import sdl2.net.SDLNet_GetError
import sdl2.net.SDLNet_UDP_Close
import sdl2.net.SDLNet_UDP_Open
import sdl2.net.SDLNet_UDP_Recv
import sdl2.net.SDLNet_UDP_Send

@OptIn(ExperimentalForeignApi::class)
class UdpConnection(
    private val address: IPAddress
) : NetworkConnection, Logging {

    private var udpSocket: CPointer<SDLNet_UDPsocket>? = null
    private var receiveJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        logger.info { "UDP connection started on ${address.host}:${address.port}" }
        udpSocket = SDLNet_UDP_Open(address.port.convert())?.reinterpret() ?: throw Exception(
            "Failed to open UDP connection on port ${address.port}: ${SDLNet_GetError()}"
        )
        logger.info { "UDP connection success: $id" }
    }

    override fun close() {
        logger.info { "UDP connection closed: $id" }
        receiveJob?.cancel()
        udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
            SDLNet_UDP_Close(socket)
            udpSocket = null
        }
    }

    /**
     * Sends data to the specified remote address.
     */
    fun send(data: ByteArray, remoteAddress: IPAddress) {
        val packet = SDLNet_AllocPacket(data.size.convert()) ?: throw Exception("Failed to allocate packet")
        try {
            packet.pointed.len = data.size.convert()

            // Set the remote address for the packet
            val sdlRemoteAddr = remoteAddress.toSDL()
            packet.pointed.address.host = sdlRemoteAddr.pointed.host
            packet.pointed.address.port = sdlRemoteAddr.pointed.port

            udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                val sent = SDLNet_UDP_Send(socket, -1, packet)
                if (sent == 0) {
                    logger.error { "Failed to send UDP packet: ${SDLNet_GetError()}" }
                } else {
                    logger.info { "Sent ${data.size} bytes to ${remoteAddress.host}:${remoteAddress.port}" }
                }
            } ?: throw Exception("UDP socket is not open")
        } finally {
            SDLNet_FreePacket(packet)
        }
    }

    override fun publish(data: ByteArray) {
        TODO()
    }

    override fun publish(data: UByteArray) {
        TODO()
    }

    override fun publish(data: String) {
        TODO()
    }

    override fun <T> publish(data: T, serializer: KSerializer<T>) {
        TODO()
    }

    /**
     * Subscribes to incoming UDP packets.
     * @param onReceive Callback invoked with the received data.
     */
    override fun subscribe(onReceive: (ByteArray) -> Unit) {
        receiveJob = coroutineScope.launch {
            memScoped {
                val packet = SDLNet_AllocPacket(1024.convert()) ?: throw Exception("Failed to allocate packet")
                try {
                    while (isActive) {
                        udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                            val received = SDLNet_UDP_Recv(socket, packet)
                            if (received > 0) {
                                val data = packet.pointed.data!!.readBytes(packet.pointed.len.convert())
                                onReceive(data)
                            } else {
                                // No data received, delay to prevent busy waiting
                                delay(10)
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
            onReceive(byteArray.decodeToString())
        }
    }

    override fun <T> subscribe(onReceive: (T) -> Unit, serializer: KSerializer<T>) {
        subscribe { byteArray: ByteArray ->
            val json = byteArray.decodeToString()
            onReceive(Json.decodeFromString(serializer, json))
        }
    }
}