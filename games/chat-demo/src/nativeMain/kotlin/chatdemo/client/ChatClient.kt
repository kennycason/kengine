package chatdemo.client

import chatdemo.ChatPacket
import chatdemo.ChatPacketType
import chatdemo.ChatPorts
import chatdemo.parseClientLine
import chatdemo.renderClientPacket
import com.kengine.network.IPAddress
import com.kengine.network.NetworkContext
import com.kengine.network.TcpConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val name = args.firstOrNull()?.trim().orEmpty()
    if (!isValidName(name)) {
        println("Usage: chatClient.kexe <name> [host] [port]")
        println("Names may use letters, digits, underscore, or hyphen.")
        return@runBlocking
    }

    val host = args.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: ChatPorts.HOST
    val requestedPort = args.getOrNull(2)?.toIntOrNull()

    NetworkContext.get()
    val connection = connectToServer(host, requestedPort)
    if (connection == null) {
        val range = if (requestedPort == null) {
            "${ChatPorts.FIRST}..${ChatPorts.LAST}"
        } else {
            requestedPort.toString()
        }
        println("No chat server found at $host port $range")
        NetworkContext.get().cleanup()
        return@runBlocking
    }

    println("Connected to ${connection.id} as $name")
    println("Type messages, @name message for private messages, or /quit to exit.")

    connection.subscribe(
        onReceive = { packet: ChatPacket ->
            println(renderClientPacket(packet))
        },
        serializer = ChatPacket.serializer()
    )
    connection.send(
        ChatPacket(
            type = ChatPacketType.JOIN,
            from = name
        ),
        ChatPacket.serializer()
    )

    try {
        while (connection.isRunning) {
            val line = readlnOrNull() ?: break
            when {
                line.trim() == "/quit" -> {
                    connection.send(
                        ChatPacket(
                            type = ChatPacketType.LEAVE,
                            from = name
                        ),
                        ChatPacket.serializer()
                    )
                    break
                }
                line.isNotBlank() -> {
                    val packet = parseClientLine(name, line)
                    if (packet.type == ChatPacketType.ERROR) {
                        println(renderClientPacket(packet))
                    } else {
                        connection.send(packet, ChatPacket.serializer())
                    }
                }
            }
            delay(1)
        }
    } finally {
        connection.close()
        NetworkContext.get().cleanup()
    }
}

private fun connectToServer(host: String, requestedPort: Int?): TcpConnection? {
    val ports = requestedPort?.let { listOf(it) } ?: ChatPorts.RANGE.toList()

    for (port in ports) {
        val connection = TcpConnection(IPAddress(host, port.toUShort()))
        try {
            connection.connect()
            return connection
        } catch (e: Exception) {
            connection.close()
        }
    }

    return null
}

private fun isValidName(name: String): Boolean {
    return name.isNotBlank() && name.length <= 24 && name.all { char ->
        char.isLetterOrDigit() || char == '_' || char == '-'
    }
}
