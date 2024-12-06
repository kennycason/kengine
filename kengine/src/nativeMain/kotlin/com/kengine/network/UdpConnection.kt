package com.kengine.network

import com.kengine.sdl.cinterop.SDLNet_UDPsocket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl2.net.IPaddress
import sdl2.net.SDLNet_AllocPacket
import sdl2.net.SDLNet_FreePacket
import sdl2.net.SDLNet_UDP_Close
import sdl2.net.SDLNet_UDP_Recv
import sdl2.net.SDLNet_UDP_Send

@OptIn(ExperimentalForeignApi::class)
class UdpConnection(
    override val id: Int,
    private val socket: SDLNet_UDPsocket,
    private val address: IPaddress
) : NetworkConnection {

    override fun publish(data: ByteArray) {
        val packet = SDLNet_AllocPacket(data.size.convert()) ?: throw Exception("Failed to allocate packet")
        try {
            packet.pointed.len = data.size.convert()

            // create a copy of the address to assign TODO better way to do this>
            val ipAddress = address.reinterpret<IPaddress>()
            packet.pointed.address.host = ipAddress.host
            packet.pointed.address.port = ipAddress.port
            SDLNet_UDP_Send(socket, -1, packet)
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
                while (true) {
                    val received = SDLNet_UDP_Recv(socket, packet)
                    if (received > 0) {
                        onReceive(packet.pointed.data!!.readBytes(packet.pointed.len.convert()))
                    }
                }
            } finally {
                SDLNet_FreePacket(packet)
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

    override fun close() {
        SDLNet_UDP_Close(socket)
    }
}