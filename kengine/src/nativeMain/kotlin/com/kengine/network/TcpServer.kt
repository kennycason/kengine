//package com.kengine.network
//
//import com.kengine.log.Logging
//import com.kengine.sdl.cinterop.SDLNet_TCPsocket
//import kotlinx.cinterop.CPointer
//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.alloc
//import kotlinx.cinterop.memScoped
//import kotlinx.cinterop.ptr
//import kotlinx.cinterop.reinterpret
//import kotlinx.coroutines.delay
//import sdl2.net.IPaddress
//import sdl2.net.SDLNet_GetError
//import sdl2.net.SDLNet_ResolveHost
//import sdl2.net.SDLNet_TCP_Accept
//import sdl2.net.SDLNet_TCP_Close
//import sdl2.net.SDLNet_TCP_Open
//
//@OptIn(ExperimentalForeignApi::class)
//class TcpServer(
//    private val address: IPAddress
//) : Logging {
//    private var tcpSocket: CPointer<SDLNet_TCPsocket>? = null
//    private var isRunning = false
//
//    fun start() {
//        logger.info { "Starting TCP server on ${address.host}:${address.port}" }
//
//        memScoped {
//            val resolvedIp = alloc<IPaddress>()
//            // Pass null for the host to bind to all interfaces
//            if (SDLNet_ResolveHost(resolvedIp.ptr, null as String?, address.port) < 0) {
//                throw Exception("Failed to resolve host for server: ${SDLNet_GetError()}")
//            }
//
//            tcpSocket = SDLNet_TCP_Open(resolvedIp.ptr)?.reinterpret()
//                ?: throw Exception("Failed to open TCP server socket: ${SDLNet_GetError()}")
//        }
//
//        isRunning = true
//        logger.info { "TCP server started on ${address.host}:${address.port}" }
//    }
//
//    fun stop() {
//        logger.info { "Stopping TCP server" }
//        isRunning = false
//        tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
//            SDLNet_TCP_Close(socket)
//            tcpSocket = null
//        }
//    }
//
//    suspend fun acceptConnection(onAccept: suspend (TcpConnection) -> Unit) {
//        while (isRunning) {
//            tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
//                val clientSocket = SDLNet_TCP_Accept(socket)?.reinterpret<SDLNet_TCPsocket>()
//                if (clientSocket != null) {
//                    logger.info { "Accepted new TCP connection" }
//                    val connection = TcpServerConnection(clientSocket)
//                    onAccept(connection)
//                }
//            }
//            delay(100) // Prevent busy waiting
//        }
//    }
//
//    private class TcpServerConnection(
//        socket: CPointer<SDLNet_TCPsocket>
//    ) : TcpConnection(IPAddress("0.0.0.0", 0u)) {
//        init {
//            tcpSocket = socket
//            isRunning = true
//        }
//    }
//}
