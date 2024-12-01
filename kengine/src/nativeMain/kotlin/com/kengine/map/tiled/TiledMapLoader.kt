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
        return json.decodeFromString(TiledMap.serializer(), jsonContent)
            .also { logger.info { "Tiled map:\n$it" } }
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