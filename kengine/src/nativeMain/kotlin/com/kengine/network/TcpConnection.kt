
import com.kengine.network.NetworkConnection
import com.kengine.sdl.cinterop.SDLNet_TCPsocket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import sdl2.net.SDLNet_TCP_Close
import sdl2.net.SDLNet_TCP_Recv
import sdl2.net.SDLNet_TCP_Send

@OptIn(ExperimentalForeignApi::class)
class TcpConnection(
    override val id: Int,
    private val socket: SDLNet_TCPsocket
) : NetworkConnection {

    override fun publish(data: ByteArray) {
        val uByteData = data.map { it.toUByte() }.toUByteArray()
        SDLNet_TCP_Send(socket, uByteData.refTo(0), data.size)
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
            while (true) {
                val received = SDLNet_TCP_Recv(socket, buffer.refTo(0), buffer.size)
                if (received <= 0) break // connection closed
                onReceive(buffer.copyOf(received))
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
        SDLNet_TCP_Close(socket)
    }
}