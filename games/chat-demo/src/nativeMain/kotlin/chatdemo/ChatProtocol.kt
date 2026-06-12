package chatdemo

import kotlinx.serialization.Serializable

object ChatPorts {
    const val HOST = "127.0.0.1"
    const val FIRST = 25000
    const val LAST = 25020
    val RANGE: IntRange = FIRST..LAST
}

@Serializable
data class ChatPacket(
    val type: ChatPacketType,
    val from: String = "",
    val to: String? = null,
    val body: String = ""
)

@Serializable
enum class ChatPacketType {
    JOIN,
    PUBLIC_MESSAGE,
    PRIVATE_MESSAGE,
    SYSTEM,
    ERROR,
    LEAVE
}

fun parseClientLine(name: String, line: String): ChatPacket {
    val trimmed = line.trim()
    if (trimmed.startsWith("@")) {
        val targetAndBody = trimmed.drop(1)
        val target = targetAndBody.substringBefore(" ").trim()
        val body = targetAndBody.substringAfter(" ", "").trim()

        return if (target.isBlank() || body.isBlank()) {
            ChatPacket(
                type = ChatPacketType.ERROR,
                body = "Private messages must use @name message"
            )
        } else {
            ChatPacket(
                type = ChatPacketType.PRIVATE_MESSAGE,
                from = name,
                to = target,
                body = body
            )
        }
    }

    return ChatPacket(
        type = ChatPacketType.PUBLIC_MESSAGE,
        from = name,
        body = line
    )
}

fun renderClientPacket(packet: ChatPacket): String {
    return when (packet.type) {
        ChatPacketType.PUBLIC_MESSAGE -> "${packet.from}: ${packet.body}"
        ChatPacketType.PRIVATE_MESSAGE -> "[private] ${packet.from}: ${packet.body}"
        ChatPacketType.SYSTEM -> "[server] ${packet.body}"
        ChatPacketType.ERROR -> "[error] ${packet.body}"
        ChatPacketType.JOIN -> "[server] ${packet.from} joined"
        ChatPacketType.LEAVE -> "[server] ${packet.from} left"
    }
}
