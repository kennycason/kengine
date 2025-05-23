package com.kengine.map.tiled

import com.kengine.file.File
import com.kengine.log.Logging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.serialization.json.Json
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

@OptIn(ExperimentalForeignApi::class)
class TiledMapLoader : Logging {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun loadMap(filePath: String): TiledMap {
        val fullFilePath = "${File.pwd()}/$filePath"
        logger.info { "Loading map from: $fullFilePath" }
        val jsonContent = readFile(fullFilePath)
        val map = json.decodeFromString<TiledMap>(jsonContent)

        // determine the base directory of the map file
        val baseDirectory = fullFilePath.substringBeforeLast("/")

        // process external tilesets
        map.tilesets.forEach { tileset ->
            if (!tileset.isExternal()) {
                return@forEach
            }

            val sourceFile = tileset.source ?: throw IllegalStateException("External tileset reference has no source file.")

            // validate the file extension is .tsj
            if (!sourceFile.endsWith(".tsj")) {
                throw IllegalArgumentException("Tileset file $sourceFile is not in the expected .tsj format.")
            }

            // compute the full path based on the map's directory
            val tilesetPath = "${baseDirectory}/$sourceFile"

            try {
                val tilesetContent = readFile(tilesetPath)
                val tilesetData = json.decodeFromString<TilesetData>(tilesetContent)

                logger.info { "populating external tileset data: ${tilesetData.name}" }
                tileset.apply {
                    columns = tilesetData.columns
                    image = "${baseDirectory}/${tilesetData.image}"
                    imageHeight = tilesetData.imageHeight
                    imageWidth = tilesetData.imageWidth
                    margin = tilesetData.margin
                    name = tilesetData.name
                    spacing = tilesetData.spacing
                    tileCount = tilesetData.tileCount
                    tileHeight = tilesetData.tileHeight
                    tileWidth = tilesetData.tileWidth
                    tiles = tilesetData.tiles
                }
            } catch (e: Exception) {
                logger.error { "Failed to load external tileset at $tilesetPath: ${e.message}" }
                throw IllegalStateException("Cannot load external tileset: $tilesetPath", e)
            }
        }

        // Initialize map after loading external tilesets
        map.initialize()
        
        logger.info { "Map successfully loaded: ${map.width}x${map.height}, ${map.layers.size} layers, ${map.tilesets.size} tilesets." }
        return map
    }

    private fun readFile(filePath: String): String {
        val file = fopen(filePath, "r")
            ?: throw IllegalArgumentException("Cannot open file: $filePath. Please ensure the file exists and the path is correct.")
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
            .also { logger.info { "Raw map data loaded from $filePath" } }
    }
}
