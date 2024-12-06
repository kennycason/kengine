package com.kengine.network

import cnames.structs.SDLNet_TCPsocket
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
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
) : NetworkConnection {

    private var udpSocket: CPointer<SDLNet_TCPsocket>? = null

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        val port = address.port.convert<UShort>()
        udpSocket = SDLNet_UDP_Open(port)?.reinterpret() ?: throw Exception(
            "Failed to open UDP connection on port $port: ${SDLNet_GetError()}"
        )
    }

    override fun close() {
        udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
            SDLNet_UDP_Close(socket)
            udpSocket = null
        }
    }

    override fun publish(data: ByteArray) {
        val packet = SDLNet_AllocPacket(data.size.convert()) ?: throw Exception("Failed to allocate packet")
        try {
            packet.pointed.len = data.size.convert()

            // create a copy of the address to assign TODO better way to do this>
            val ipAddress = address.toSDL()
            packet.pointed.address.host = ipAddress.pointed.host
            packet.pointed.address.port = ipAddress.pointed.port

            udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                SDLNet_UDP_Send(socket, -1, packet)
            }
        } finally {
            SDLNet_FreePacket(packet)
        }
    }

    override fun publish(data: UByteArray) {
        publish(data.map { it.toByte() }.toByteArray())
    }

    override fun publish(data: String) {
        publish(data.encodeToByteArray())
    }

    override fun <T> publish(data: T, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, data)
        publish(json.encodeToByteArray())
    }

    override fun subscribe(onReceive: (ByteArray) -> Unit) {
        memScoped {
            val packet = SDLNet_AllocPacket(1024.convert()) ?: throw Exception("Failed to allocate packet")
            try {
                var shouldContinue = true

                while (shouldContinue) {
                    // could probably just double bang this since it shouldn't be null...
                    udpSocket?.reinterpret<cnames.structs._UDPsocket>()?.let { socket ->
                        val received = SDLNet_UDP_Recv(socket, packet)
                        if (received > 0) {
                            val data = packet.pointed.data!!.readBytes(packet.pointed.len.convert())
                            onReceive(data)
                        } else {
                            shouldContinue = false // stop if no data is received
                        }
                    } ?: run {
                        shouldContinue = false // stop if udpSocket is null
                    }
                }
            } finally {
                SDLNet_FreePacket(packet) // ensure the packet is always freed
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