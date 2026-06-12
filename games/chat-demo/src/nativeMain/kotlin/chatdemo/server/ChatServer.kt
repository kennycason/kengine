package chatdemo.server

import chatdemo.ChatPacket
import chatdemo.ChatPacketType
import chatdemo.ChatPorts
import com.kengine.network.IPAddress
import com.kengine.network.NetworkContext
import com.kengine.network.TcpConnection
import com.kengine.network.TcpServer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private data class ClientSession(
    val name: String,
    val connection: TcpConnection
)

private sealed class ServerEvent {
    data class PacketReceived(
        val connection: TcpConnection,
        val packet: ChatPacket
    ) : ServerEvent()

    data class ConnectionClosed(
        val connection: TcpConnection
    ) : ServerEvent()
}

fun main(args: Array<String>) = runBlocking {
    NetworkContext.get()

    val requestedPort = args.firstOrNull()?.toIntOrNull()
    val serverInfo = startServer(requestedPort)
    val server = serverInfo.server
    val events = Channel<ServerEvent>(Channel.UNLIMITED)
    val sessionsByConnection = mutableMapOf<String, ClientSession>()
    val sessionsByName = mutableMapOf<String, ClientSession>()

    println("ChatServer listening on ${ChatPorts.HOST}:${serverInfo.port}")
    println("Clients can run: ./chatClient.kexe kentroid ${ChatPorts.HOST} ${serverInfo.port}")
    println("Private messages use @name message")

    val acceptJob = launch {
        server.acceptConnection { connection ->
            println("Accepted ${connection.id}")
            connection.subscribe(
                onReceive = { packet: ChatPacket ->
                    events.trySend(ServerEvent.PacketReceived(connection, packet))
                },
                serializer = ChatPacket.serializer()
            )
            launch {
                while (connection.isRunning) {
                    delay(50)
                }
                events.trySend(ServerEvent.ConnectionClosed(connection))
            }
        }
    }

    try {
        for (event in events) {
            when (event) {
                is ServerEvent.PacketReceived -> handlePacket(
                    connection = event.connection,
                    packet = event.packet,
                    sessionsByConnection = sessionsByConnection,
                    sessionsByName = sessionsByName
                )
                is ServerEvent.ConnectionClosed -> removeSession(
                    connection = event.connection,
                    sessionsByConnection = sessionsByConnection,
                    sessionsByName = sessionsByName,
                    closeConnection = true
                )
            }
        }
    } finally {
        acceptJob.cancelAndJoin()
        sessionsByConnection.values.forEach { it.connection.close() }
        server.stop()
        NetworkContext.get().cleanup()
    }
}

private data class StartedServer(
    val server: TcpServer,
    val port: Int
)

private fun startServer(requestedPort: Int?): StartedServer {
    val ports = requestedPort?.let { listOf(it) } ?: ChatPorts.RANGE.toList()
    var lastError: Throwable? = null

    for (port in ports) {
        try {
            val server = TcpServer(IPAddress(ChatPorts.HOST, port.toUShort()))
            server.start()
            return StartedServer(server, port)
        } catch (e: Throwable) {
            lastError = e
        }
    }

    throw IllegalStateException(
        "No available chat server port in ${ports.first()}..${ports.last()}",
        lastError
    )
}

private fun handlePacket(
    connection: TcpConnection,
    packet: ChatPacket,
    sessionsByConnection: MutableMap<String, ClientSession>,
    sessionsByName: MutableMap<String, ClientSession>
) {
    when (packet.type) {
        ChatPacketType.JOIN -> handleJoin(connection, packet, sessionsByConnection, sessionsByName)
        ChatPacketType.PUBLIC_MESSAGE -> handlePublicMessage(connection, packet, sessionsByConnection)
        ChatPacketType.PRIVATE_MESSAGE -> handlePrivateMessage(connection, packet, sessionsByConnection, sessionsByName)
        ChatPacketType.LEAVE -> removeSession(connection, sessionsByConnection, sessionsByName)
        ChatPacketType.ERROR,
        ChatPacketType.SYSTEM -> sendError(connection, "Client cannot send ${packet.type} packets")
    }
}

private fun handleJoin(
    connection: TcpConnection,
    packet: ChatPacket,
    sessionsByConnection: MutableMap<String, ClientSession>,
    sessionsByName: MutableMap<String, ClientSession>
) {
    val name = packet.from.trim()
    if (!isValidName(name)) {
        sendError(connection, "Invalid name. Use letters, digits, underscore, or hyphen.")
        connection.close()
        return
    }

    if (sessionsByName.containsKey(name)) {
        sendError(connection, "Name '$name' is already connected")
        connection.close()
        return
    }

    val session = ClientSession(name, connection)
    sessionsByConnection[connection.id] = session
    sessionsByName[name] = session
    println("$name joined from ${connection.id}")

    connection.send(
        ChatPacket(
            type = ChatPacketType.SYSTEM,
            body = "Connected as $name. Users online: ${sessionsByName.keys.sorted().joinToString(", ")}"
        ),
        ChatPacket.serializer()
    )
    broadcast(
        sessionsByConnection.values,
        ChatPacket(
            type = ChatPacketType.SYSTEM,
            body = "$name joined"
        ),
        exceptConnectionId = connection.id
    )
}

private fun handlePublicMessage(
    connection: TcpConnection,
    packet: ChatPacket,
    sessionsByConnection: MutableMap<String, ClientSession>
) {
    val session = sessionsByConnection[connection.id]
    if (session == null) {
        sendError(connection, "Join before sending messages")
        return
    }

    val message = packet.body.trim()
    if (message.isBlank()) {
        sendError(connection, "Cannot send an empty message")
        return
    }

    println("${session.name}: $message")
    broadcast(
        sessionsByConnection.values,
        ChatPacket(
            type = ChatPacketType.PUBLIC_MESSAGE,
            from = session.name,
            body = message
        )
    )
}

private fun handlePrivateMessage(
    connection: TcpConnection,
    packet: ChatPacket,
    sessionsByConnection: MutableMap<String, ClientSession>,
    sessionsByName: MutableMap<String, ClientSession>
) {
    val sender = sessionsByConnection[connection.id]
    if (sender == null) {
        sendError(connection, "Join before sending messages")
        return
    }

    val targetName = packet.to?.trim().orEmpty()
    val target = sessionsByName[targetName]
    if (target == null) {
        sendError(connection, "User '$targetName' does not exist")
        return
    }

    val message = packet.body.trim()
    if (message.isBlank()) {
        sendError(connection, "Cannot send an empty private message")
        return
    }

    println("[private] ${sender.name} -> $targetName: $message")
    val outgoing = ChatPacket(
        type = ChatPacketType.PRIVATE_MESSAGE,
        from = sender.name,
        to = targetName,
        body = message
    )
    val delivered = sendTo(target.connection, outgoing)
    if (!delivered) {
        removeSession(target.connection, sessionsByConnection, sessionsByName)
        sendError(connection, "User '$targetName' disconnected before delivery")
        return
    }

    if (target.connection.id != sender.connection.id) {
        sendTo(sender.connection, outgoing.copy(from = "to $targetName"))
    }
}

private fun removeSession(
    connection: TcpConnection,
    sessionsByConnection: MutableMap<String, ClientSession>,
    sessionsByName: MutableMap<String, ClientSession>,
    closeConnection: Boolean = true
) {
    val session = sessionsByConnection.remove(connection.id) ?: return
    sessionsByName.remove(session.name)
    if (closeConnection) {
        connection.close()
    }
    println("${session.name} left")
    broadcast(
        sessionsByConnection.values,
        ChatPacket(
            type = ChatPacketType.SYSTEM,
            body = "${session.name} left"
        )
    )
}

private fun broadcast(
    sessions: Collection<ClientSession>,
    packet: ChatPacket,
    exceptConnectionId: String? = null
) {
    sessions
        .filter { it.connection.id != exceptConnectionId }
        .forEach { sendTo(it.connection, packet) }
}

private fun sendError(connection: TcpConnection, message: String) {
    sendTo(
        connection,
        ChatPacket(
            type = ChatPacketType.ERROR,
            body = message
        )
    )
}

private fun sendTo(connection: TcpConnection, packet: ChatPacket): Boolean {
    return try {
        connection.send(packet, ChatPacket.serializer())
        true
    } catch (e: Exception) {
        println("Failed to send to ${connection.id}: ${e.message}")
        false
    }
}

private fun isValidName(name: String): Boolean {
    return name.isNotBlank() && name.length <= 24 && name.all { char ->
        char.isLetterOrDigit() || char == '_' || char == '-'
    }
}
