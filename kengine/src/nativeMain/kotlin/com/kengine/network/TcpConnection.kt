package com.kengine.network

import com.kengine.log.Logging
import com.kengine.sdl.cinterop.SDLNet_TCPsocket
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
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
import sdl2.net.IPaddress
import sdl2.net.SDLNet_GetError
import sdl2.net.SDLNet_ResolveHost
import sdl2.net.SDLNet_TCP_Close
import sdl2.net.SDLNet_TCP_Open
import sdl2.net.SDLNet_TCP_Recv
import sdl2.net.SDLNet_TCP_Send

@OptIn(ExperimentalForeignApi::class)
open class TcpConnection(
    private val address: IPAddress,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : NetworkConnection, Logging, AutoCloseable {

    companion object {
        const val DEFAULT_BUFFER_SIZE = 1024
    }

    var tcpSocket: CPointer<SDLNet_TCPsocket>? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var isRunning = false

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        logger.info { "Starting TCP connection on ${address.host}:${address.port}" }

        memScoped {
            val resolvedIp = alloc<IPaddress>()
            if (SDLNet_ResolveHost(resolvedIp.ptr, address.host, address.port) < 0) {
                throw Exception("Failed to resolve host ${address.host}: ${SDLNet_GetError()}")
            }

            tcpSocket = SDLNet_TCP_Open(resolvedIp.ptr)?.reinterpret()
                ?: throw Exception("Failed to open TCP connection: ${SDLNet_GetError()}")
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
        tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
            SDLNet_TCP_Close(socket)
            tcpSocket = null
        }
    }

    fun send(data: ByteArray) {
        if (!isRunning) throw IllegalStateException("Connection is not open")

        tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
            data.usePinned { pinned ->
                val sent = SDLNet_TCP_Send(socket, pinned.addressOf(0), data.size.convert())
                if (sent < data.size) {
                    throw Exception("Failed to send TCP data: ${SDLNet_GetError()}")
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
            memScoped {
                val buffer = ByteArray(bufferSize)

                try {
                    while (isRunning && isActive) {
                        val shouldContinue = tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
                            buffer.usePinned { pinned ->
                                val received = SDLNet_TCP_Recv(socket, pinned.addressOf(0), bufferSize.convert())
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
                                        logger.info { "Connection closed normally by peer" }
                                        isRunning = false
                                        false // stop receiving
                                    }
                                    else -> {
                                        logger.info { "Connection error or closed abruptly" }
                                        isRunning = false
                                        false // stop receiving
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