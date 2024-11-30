package com.kengine.map.tiled

import com.kengine.log.Logging
import com.kengine.math.IntVec2
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
class TileMap(
    val width: Int,
    val height: Int,
    val tilewidth: Int,
    val tileheight: Int,
    val layers: List<Layer>,
    val tilesets: List<Tileset>,
    val orientation: String,
    @SerialName("renderorder")
    val renderOrder: String,
    val infinite: Boolean = false
) : Logging {

    init {
        logger.info { "Map: ${width}x${height}, tile: ${tilewidth}x${tileheight}" }
        logger.info { "Layers: ${layers.size}" }
        layers.forEach {
            logger.info { it.toString() }
        }
        logger.info { "Tilesets: ${tilesets.size}" }
        tilesets.forEach {
            logger.info { it.toString() }
        }
    }

    fun draw() {
        layers.forEach { layer ->
            if (layer.visible && layer.type == "tilelayer") {
                val tileId = getTileAt(layer, 0, 0)
                val tileset = findTilesetForTile(tileId, tilesets)
                if (tileset != null) {
                    val tilePosition = getTilePosition(tileId, tileset)
                    // logger.info { "${tilePosition.x}x${tilePosition.y}" }
                }
            }
        }
    }

    private fun getTileAt(layer: Layer, x: Int, y: Int): Int {
        val decodedData = layer.getDecodedData()
        return decodedData[y * layer.width + x]
    }

    private fun findTilesetForTile(tileId: Int, tilesets: List<Tileset>): Tileset? {
        return tilesets
            .sortedByDescending { it.firstgid }
            .find { it.firstgid <= tileId }
    }

    private fun getTilePosition(tileId: Int, tileset: Tileset): IntVec2 {
        val localId = tileId - tileset.firstgid
        val tileX = (localId % tileset.columns) * (tileset.tilewidth + tileset.spacing) + tileset.margin
        val tileY = (localId / tileset.columns) * (tileset.tileheight + tileset.spacing) + tileset.margin
        return IntVec2(tileX, tileY)
    }
}

@Serializable
data class Layer(
    val id: Int,
    val name: String,
    val type: String,
    val visible: Boolean,
    val width: Int,
    val height: Int,
    val data: String,
    val encoding: String,
    val opacity: Double,
    val x: Int,
    val y: Int
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun getDecodedData(): List<Int> {
        if (encoding != "base64") {
            throw IllegalArgumentException("Unsupported encoding: $encoding")
        }

        val decoded = Base64.decode(data)
        return List(decoded.size / 4) { index ->
            decoded[index * 4].toInt() and 0xFF or
                    ((decoded[index * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((decoded[index * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((decoded[index * 4 + 3].toInt() and 0xFF) shl 24)
        }
    }
}

@Serializable
data class Tileset(
    val firstgid: Int,
    val name: String,
    val tilewidth: Int,
    val tileheight: Int,
    val imagewidth: Int,
    val imageheight: Int,
    val image: String,
    val columns: Int,
    val tilecount: Int,
    val margin: Int = 0,
    val spacing: Int = 0
)

@OptIn(ExperimentalForeignApi::class)
class TileMapLoader : Logging {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun loadMap(filePath: String): TileMap {
        val jsonContent = readFile(filePath)
        return json.decodeFromString(TileMap.serializer(), jsonContent)
    }

    private fun readFile(filePath: String): String {
        val file = fopen(filePath, "r") ?: throw IllegalArgumentException("Cannot open file: $filePath")
        val buffer = ByteArray(8192)
        val stringBuilder = StringBuilder()
        try {
            while (true) {
                val bytesRead = fread(buffer.refTo(0), 1.toULong(), buffer.size.toULong(), file)
                if (bytesRead == 0.toULong()) break
                stringBuilder.append(buffer.decodeToString(endIndex = bytesRead.toInt()))
            }
        } finally {
            fclose(file)
        }
        return stringBuilder.toString()
            .also { logger.info(it) }
    }
}