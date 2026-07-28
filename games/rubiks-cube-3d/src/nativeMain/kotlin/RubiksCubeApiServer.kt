import com.kengine.graphics.Color
import com.kengine.log.Logger
import com.kengine.network.IPAddress
import com.kengine.network.NetworkContext
import com.kengine.network.TcpConnection
import com.kengine.network.TcpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class RubiksCubeApiServer(
    private val host: String = HOST,
    private val port: Int = PORT
) {
    private val logger = Logger("RubiksCubeApi")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commands = Channel<ApiCommand>(Channel.UNLIMITED)
    private val json = Json { ignoreUnknownKeys = true }
    private var server: TcpServer? = null
    private var acceptJob: Job? = null

    fun start() {
        NetworkContext.get()
        val tcpServer = TcpServer(IPAddress(host, port.toUShort()))
        tcpServer.start()
        server = tcpServer
        acceptJob = scope.launch {
            tcpServer.acceptConnection { connection ->
                handleConnection(connection)
            }
        }
        logger.info { "Rubik's cube API listening on http://$host:$port" }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        server?.stop()
        server = null
        commands.close()
        scope.cancel()
        NetworkContext.get().cleanup()
    }

    fun drain(rubiksCube: RubiksCube) {
        while (true) {
            val command = commands.tryReceive().getOrNull() ?: return
            command.applyTo(rubiksCube)
        }
    }

    private fun handleConnection(connection: TcpConnection) {
        val pending = PendingHttpRequest()
        var handled = false

        connection.subscribe { bytes: ByteArray ->
            if (handled) {
                return@subscribe
            }

            pending.append(bytes)
            val request = pending.tryParse() ?: return@subscribe
            handled = true

            scope.launch {
                val response = handleRequest(request)
                sendResponse(connection, response)
                connection.close()
            }
        }
    }

    private suspend fun handleRequest(request: HttpRequest): HttpResponse {
        if (request.method == "OPTIONS") {
            return HttpResponse.json(204, buildJsonObject {})
        }
        if (request.method != "POST") {
            return errorResponse(405, "Only POST is supported.")
        }

        return try {
            when (request.path) {
                "/3x3/apply" -> handleApply(request.body)
                "/3x3/reset" -> enqueueCommand(ApiCommand.Reset())
                "/3x3/scramble" -> handleScramble(request.body)
                "/3x3/colors" -> handleColors(request.body)
                else -> errorResponse(404, "Unknown endpoint '${request.path}'.")
            }
        } catch (error: RubiksNotationParseException) {
            errorResponse(400, error.message ?: "Invalid notation.")
        } catch (error: IllegalArgumentException) {
            errorResponse(400, error.message ?: "Invalid request.")
        } catch (error: Exception) {
            logger.error(error) { "API request failed" }
            errorResponse(500, "Internal server error.")
        }
    }

    private suspend fun handleApply(body: String): HttpResponse {
        val trimmed = body.trim()
        val requestBody = if (trimmed.startsWith("{")) parseJsonObject(trimmed) else JsonObject(emptyMap())
        validateNotationType(requestBody["type"]?.jsonPrimitive?.contentOrNull)
        val sequence = if (trimmed.startsWith("{")) {
            sequenceFrom(requestBody)
        } else {
            trimmed
        }

        require(sequence.isNotBlank()) {
            "Apply request requires a notation string in 'notation', 'sequence', 'algorithm', 'moves', or 'notations'."
        }

        val moves = RubiksNotationParser.parse(sequence)
        return enqueueCommand(ApiCommand.Apply(sequence, moves))
    }

    private suspend fun handleScramble(body: String): HttpResponse {
        val objectBody = parseOptionalJsonObject(body)
        val turns = objectBody["turns"]?.jsonPrimitive?.intOrNull ?: DEFAULT_SCRAMBLE_TURNS
        require(turns in 1..200) {
            "Scramble turns must be between 1 and 200."
        }
        return enqueueCommand(ApiCommand.Scramble(turns))
    }

    private suspend fun handleColors(body: String): HttpResponse {
        val objectBody = parseJsonObject(body)
        val colorsObject = objectBody["colors"]?.jsonObject ?: objectBody
        val updates = mutableMapOf<RubiksCubeFace, Color>()

        colorsObject.forEach { (key, value) ->
            val face = RubiksCubeFace.fromApiName(key)
                ?: throw IllegalArgumentException("Unknown face '$key'. Use front, back, right, left, up, or down.")
            val hex = value.jsonPrimitive.contentOrNull
                ?: throw IllegalArgumentException("Color for '$key' must be an RGB hex string.")
            updates[face] = parseRgbColor(hex)
        }

        return enqueueCommand(ApiCommand.SetColors(updates))
    }

    private suspend fun enqueueCommand(command: ApiCommand): HttpResponse {
        commands.send(command)
        return command.response.await()
    }

    private fun parseJsonObject(body: String): JsonObject {
        require(body.isNotBlank()) {
            "Request body must be a JSON object."
        }
        return json.parseToJsonElement(body).jsonObject
    }

    private fun parseOptionalJsonObject(body: String): JsonObject {
        return if (body.isBlank()) JsonObject(emptyMap()) else parseJsonObject(body)
    }

    private fun validateNotationType(value: String?) {
        when (value?.trim()?.uppercase()) {
            null, "", "BASIC" -> return
            else -> throw IllegalArgumentException("Unsupported notation type '$value'. Use BASIC.")
        }
    }

    private fun sequenceFrom(body: JsonObject): String {
        val keys = listOf("notation", "sequence", "algorithm", "moves", "notations")
        for (key in keys) {
            val value = body[key] ?: continue
            return when (value) {
                is JsonArray -> value.joinToString(" ") { it.jsonPrimitive.contentOrNull.orEmpty() }
                else -> value.jsonPrimitive.contentOrNull.orEmpty()
            }
        }
        return ""
    }

    private fun parseRgbColor(value: String): Color {
        val hex = value.trim().removePrefix("#")
        require(hex.length == 6 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "RGB colors must be 6 hex characters, for example 00FF00."
        }
        return Color.fromHex(hex)
    }

    private fun sendResponse(connection: TcpConnection, response: HttpResponse) {
        val bodyBytes = response.body.encodeToByteArray()
        val headers = buildString {
            append("HTTP/1.1 ${response.status} ${statusText(response.status)}\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Connection: close\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: POST, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type\r\n")
            append("\r\n")
        }.encodeToByteArray()
        connection.send(headers.concat(bodyBytes))
    }

    private fun errorResponse(status: Int, message: String): HttpResponse {
        return HttpResponse.json(
            status,
            buildJsonObject {
                put("ok", false)
                put("error", message)
            }
        )
    }

    private sealed class ApiCommand {
        val response = CompletableDeferred<HttpResponse>()

        abstract fun applyTo(rubiksCube: RubiksCube)

        class Apply(
            private val sequence: String,
            private val moves: List<RubiksNotationMove>
        ) : ApiCommand() {
            override fun applyTo(rubiksCube: RubiksCube) {
                var queuedQuarterTurns = 0
                moves.forEach { notationMove ->
                    notationMove.toSliceMoves().forEachIndexed { index, sliceMove ->
                        rubiksCube.enqueue(sliceMove, historyToken = if (index == 0) notationMove.token else null)
                        queuedQuarterTurns += 1
                    }
                }

                response.complete(
                    okBody(rubiksCube) {
                        put("type", "BASIC")
                        put("sequence", sequence)
                        put("tokens", JsonArray(moves.map { JsonPrimitive(it.token) }))
                        put("queuedQuarterTurns", queuedQuarterTurns)
                    }
                )
            }
        }

        class Scramble(private val turns: Int) : ApiCommand() {
            override fun applyTo(rubiksCube: RubiksCube) {
                rubiksCube.scramble(turns)
                response.complete(
                    okBody(rubiksCube) {
                        put("turns", turns)
                    }
                )
            }
        }

        class Reset : ApiCommand() {
            override fun applyTo(rubiksCube: RubiksCube) {
                rubiksCube.reset()
                response.complete(okBody(rubiksCube))
            }
        }

        class SetColors(private val updates: Map<RubiksCubeFace, Color>) : ApiCommand() {
            override fun applyTo(rubiksCube: RubiksCube) {
                rubiksCube.setFaceColors(updates)
                response.complete(
                    okBody(rubiksCube) {
                        put("updatedFaces", JsonArray(updates.keys.map { JsonPrimitive(it.apiName) }))
                    }
                )
            }
        }
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val body: String
    )

    private class PendingHttpRequest {
        private var bytes = ByteArray(0)

        fun append(chunk: ByteArray) {
            bytes = bytes.concat(chunk)
        }

        fun tryParse(): HttpRequest? {
            val headerEnd = bytes.indexOfHeaderEnd()
            if (headerEnd < 0) {
                return null
            }

            val header = bytes.copyOfRange(0, headerEnd).decodeToString()
            val lines = header.split("\r\n")
            val requestParts = lines.firstOrNull()?.split(" ") ?: return null
            if (requestParts.size < 2) {
                throw IllegalArgumentException("Malformed HTTP request line.")
            }

            val headers = lines.drop(1)
                .mapNotNull { line ->
                    val colon = line.indexOf(':')
                    if (colon < 0) null else line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
                }
                .toMap()
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val bodyStart = headerEnd + HTTP_HEADER_DELIMITER.size
            val requestEnd = bodyStart + contentLength
            if (bytes.size < requestEnd) {
                return null
            }

            return HttpRequest(
                method = requestParts[0].uppercase(),
                path = requestParts[1].substringBefore("?"),
                body = bytes.copyOfRange(bodyStart, requestEnd).decodeToString()
            )
        }
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 6464
        private const val DEFAULT_SCRAMBLE_TURNS = 22
        private val HTTP_HEADER_DELIMITER = byteArrayOf(13, 10, 13, 10)
    }
}

private data class HttpResponse(
    val status: Int,
    val body: String
) {
    companion object {
        fun json(status: Int, body: JsonObject): HttpResponse {
            return HttpResponse(status, body.toString())
        }
    }
}

private fun okBody(rubiksCube: RubiksCube, extra: JsonObjectBuilder.() -> Unit = {}): HttpResponse {
    return HttpResponse.json(
        200,
        buildJsonObject {
            put("ok", true)
            extra()
            put("history", JsonArray(rubiksCube.history.map { JsonPrimitive(it) }))
            put("colors", faceColorsJson(rubiksCube.getFaceColors()))
        }
    )
}

private typealias JsonObjectBuilder = kotlinx.serialization.json.JsonObjectBuilder

private fun faceColorsJson(colors: Map<RubiksCubeFace, Color>): JsonObject {
    return buildJsonObject {
        RubiksCubeFace.entries.forEach { face ->
            put(face.apiName, colors.getValue(face).toRgbHex())
        }
    }
}

private fun Color.toRgbHex(): String {
    return byteToHex(r.toInt()) + byteToHex(g.toInt()) + byteToHex(b.toInt())
}

private fun byteToHex(value: Int): String {
    val digits = "0123456789ABCDEF"
    return "${digits[(value ushr 4) and 0xF]}${digits[value and 0xF]}"
}

private fun ByteArray.concat(other: ByteArray): ByteArray {
    val result = ByteArray(size + other.size)
    copyInto(result)
    other.copyInto(result, destinationOffset = size)
    return result
}

private fun ByteArray.indexOfHeaderEnd(): Int {
    if (size < 4) {
        return -1
    }
    for (index in 0..(size - 4)) {
        if (this[index] == 13.toByte() &&
            this[index + 1] == 10.toByte() &&
            this[index + 2] == 13.toByte() &&
            this[index + 3] == 10.toByte()
        ) {
            return index
        }
    }
    return -1
}

private fun statusText(status: Int): String {
    return when (status) {
        200 -> "OK"
        204 -> "No Content"
        400 -> "Bad Request"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        500 -> "Internal Server Error"
        else -> "OK"
    }
}
