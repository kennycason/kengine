package com.kengine.map.tiled

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
        val jsonContent = readFile(filePath)
        val map = json.decodeFromString<TiledMap>(jsonContent)

        // Load external tilesets
        map.tilesets.forEach { tilesetRef ->
            if (tilesetRef.isExternal()) {
                val tilesetPath = "src/nativeTest/resources/${tilesetRef.source!!.replace(".tsx", ".tsj")}"
                val tilesetContent = readFile(tilesetPath)
                val tilesetData = json.decodeFromString<TilesetData>(tilesetContent)

                // Copy data to reference
                tilesetRef.apply {
                    columns = tilesetData.columns
                    image = tilesetData.image
                    imageHeight = tilesetData.imageHeight
                    imageWidth = tilesetData.imageWidth
                    margin = tilesetData.margin
                    name = tilesetData.name
                    spacing = tilesetData.spacing
                    tileCount = tilesetData.tileCount
                    tileHeight = tilesetData.tileHeight
                    tileWidth = tilesetData.tileWidth
                }
            }
        }

        return map
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
            .also { logger.info { "Raw map data:\n$it" } }
    }

}