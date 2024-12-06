
import com.kengine.network.IPAddress
import com.kengine.network.NetworkConnection
import com.kengine.sdl.cinterop.SDLNet_TCPsocket
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl2.net.SDLNet_GetError
import sdl2.net.SDLNet_TCP_Close
import sdl2.net.SDLNet_TCP_Open
import sdl2.net.SDLNet_TCP_Recv
import sdl2.net.SDLNet_TCP_Send

@OptIn(ExperimentalForeignApi::class)
class TcpConnection(
    private val address: IPAddress
) : NetworkConnection {

    private var tcpSocket: CPointer<SDLNet_TCPsocket>? = null

    override val id: String
        get() = "${address.host}:${address.port}"

    override fun connect() {
        val sdlIpAddress = address.toSDL()
        tcpSocket = SDLNet_TCP_Open(sdlIpAddress) ?: throw Exception(
            "Failed to open TCP connection to ${address.host}:${address.port}: ${SDLNet_GetError()}"
        )
    }

    override fun close() {
        tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
            SDLNet_TCP_Close(socket)
            tcpSocket = null
        }
    }

    override fun publish(data: ByteArray) {
        tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
            val uByteData = data.map { it.toUByte() }.toUByteArray()
            SDLNet_TCP_Send(socket, uByteData.refTo(0), data.size)
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
            val buffer = ByteArray(1024)
            var shouldContinue = true
            while (shouldContinue) {
                // could probably just double bang this since it shouldn't be null...
                tcpSocket?.reinterpret<cnames.structs._TCPsocket>()?.let { socket ->
                    val received = SDLNet_TCP_Recv(socket, buffer.refTo(0), buffer.size)
                    if (received <= 0) {
                        shouldContinue = false
                    } else {
                        onReceive(buffer.copyOf(received))
                    }
                } ?: run {
                    shouldContinue = false // exit if tcpSocket is null
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