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
        val fullFilePath = File.resolveAssetPath(filePath)
        logger.info { "Loading map from: $fullFilePath" }

        val map = when {
            fullFilePath.endsWith(".tmx") -> loadTmx(fullFilePath)
            else -> loadTmj(fullFilePath)
        }

        map.initialize()
        logger.info { "Map successfully loaded: ${map.width}x${map.height}, ${map.layers.size} layers, ${map.tilesets.size} tilesets." }
        return map
    }

    private fun loadTmj(fullFilePath: String): TiledMap {
        val jsonContent = readFile(fullFilePath)
        val map = json.decodeFromString<TiledMap>(jsonContent)
        val baseDirectory = fullFilePath.substringBeforeLast("/")

        map.tilesets.forEach { tileset ->
            if (!tileset.isExternal()) return@forEach

            val sourceFile = tileset.source
                ?: throw IllegalStateException("External tileset reference has no source file.")
            if (!sourceFile.endsWith(".tsj")) {
                throw IllegalArgumentException("Tileset file $sourceFile is not in the expected .tsj format.")
            }

            val tilesetPath = "${baseDirectory}/$sourceFile"
            try {
                val tilesetContent = readFile(tilesetPath)
                val tilesetData = json.decodeFromString<TilesetData>(tilesetContent)
                logger.info { "populating external tileset data: ${tilesetData.name}" }
                populateTileset(tileset, tilesetData, baseDirectory)
            } catch (e: Exception) {
                logger.error { "Failed to load external tileset at $tilesetPath: ${e.message}" }
                throw IllegalStateException("Cannot load external tileset: $tilesetPath", e)
            }
        }

        return map
    }

    private fun loadTmx(fullFilePath: String): TiledMap {
        val xmlContent = readFile(fullFilePath)
        val root = XmlElement.parse(xmlContent)
        val baseDirectory = fullFilePath.substringBeforeLast("/")

        val tilesets = root.childrenByName("tileset").map { tsEl ->
            val firstgid = tsEl.attrInt("firstgid")?.toUInt()
                ?: throw IllegalStateException("Tileset missing firstgid")
            val source = tsEl.attr("source")

            val tileset = Tileset(firstgid = firstgid, source = source)

            if (source != null) {
                val tilesetPath = "${baseDirectory}/$source"
                val tilesetXml = readFile(tilesetPath)
                val tsRoot = XmlElement.parse(tilesetXml)
                val tilesetDir = tilesetPath.substringBeforeLast("/")
                populateTilesetFromXml(tileset, tsRoot, tilesetDir)
            } else {
                populateTilesetFromXml(tileset, tsEl, baseDirectory)
            }

            tileset
        }

        val layers = mutableListOf<TiledMapLayer>()

        root.children.forEach { child ->
            when (child.name) {
                "layer" -> layers.add(parseTileLayer(child))
                "objectgroup" -> layers.add(parseObjectGroup(child))
            }
        }

        return TiledMap(
            width = root.attrInt("width")!!,
            height = root.attrInt("height")!!,
            tileWidth = root.attrInt("tilewidth")!!,
            tileHeight = root.attrInt("tileheight")!!,
            layers = layers,
            tilesets = tilesets,
            orientation = root.attr("orientation") ?: "orthogonal",
            renderOrder = root.attr("renderorder") ?: "right-down",
            infinite = root.attr("infinite") == "1"
        )
    }

    private fun parseTileLayer(el: XmlElement): TiledMapLayer {
        val dataEl = el.child("data")
        return TiledMapLayer(
            id = el.attrInt("id") ?: 0,
            name = el.attr("name") ?: "",
            type = "tilelayer",
            visible = el.attr("visible") != "0",
            opacity = el.attrFloat("opacity") ?: 1f,
            x = el.attrInt("x") ?: 0,
            y = el.attrInt("y") ?: 0,
            width = el.attrInt("width"),
            height = el.attrInt("height"),
            data = dataEl?.text,
            encoding = dataEl?.attr("encoding"),
            compression = dataEl?.attr("compression")
        )
    }

    private fun parseObjectGroup(el: XmlElement): TiledMapLayer {
        val objects = el.childrenByName("object").map { objEl ->
            val properties = objEl.child("properties")?.childrenByName("property")?.map { propEl ->
                TiledObject.Property(
                    name = propEl.attr("name") ?: "",
                    type = propEl.attr("type") ?: "string",
                    value = propEl.attr("value") ?: ""
                )
            }

            TiledObject(
                id = objEl.attrInt("id") ?: 0,
                name = objEl.attr("name") ?: "",
                type = objEl.attr("type") ?: "",
                x = objEl.attrDouble("x") ?: 0.0,
                y = objEl.attrDouble("y") ?: 0.0,
                width = objEl.attrDouble("width") ?: 0.0,
                height = objEl.attrDouble("height") ?: 0.0,
                rotation = objEl.attrDouble("rotation") ?: 0.0,
                visible = objEl.attr("visible") != "0",
                properties = properties
            )
        }

        return TiledMapLayer(
            id = el.attrInt("id") ?: 0,
            name = el.attr("name") ?: "",
            type = "objectgroup",
            visible = el.attr("visible") != "0",
            opacity = el.attrFloat("opacity") ?: 1f,
            x = el.attrInt("x") ?: 0,
            y = el.attrInt("y") ?: 0,
            draworder = el.attr("draworder"),
            objects = objects
        )
    }

    private fun populateTilesetFromXml(tileset: Tileset, el: XmlElement, baseDirectory: String) {
        val imageEl = el.child("image")

        tileset.apply {
            name = el.attr("name")
            columns = el.attrInt("columns")
            tileWidth = el.attrInt("tilewidth")
            tileHeight = el.attrInt("tileheight")
            tileCount = el.attrInt("tilecount")
            spacing = el.attrInt("spacing") ?: 0
            margin = el.attrInt("margin") ?: 0
            imageWidth = imageEl?.attrInt("width")
            imageHeight = imageEl?.attrInt("height")
            image = imageEl?.attr("source")?.let { src ->
                "${baseDirectory}/$src"
            }
            tiles = el.childrenByName("tile").mapNotNull { tileEl ->
                val tileId = tileEl.attrInt("id") ?: return@mapNotNull null
                val animationEl = tileEl.child("animation")
                val frames = animationEl?.childrenByName("frame")?.map { frameEl ->
                    Frame(
                        tileid = frameEl.attrInt("tileid")!!,
                        duration = frameEl.attrInt("duration")!!
                    )
                }
                val properties = tileEl.child("properties")?.childrenByName("property")?.map { propEl ->
                    TiledObject.Property(
                        name = propEl.attr("name") ?: "",
                        type = propEl.attr("type") ?: "string",
                        value = propEl.attr("value") ?: ""
                    )
                }
                TileData(
                    id = tileId,
                    animation = frames,
                    properties = properties
                )
            }
        }

        logger.info { "Populated tileset from XML: ${tileset.name}" }
    }

    private fun populateTileset(tileset: Tileset, data: TilesetData, baseDirectory: String) {
        tileset.apply {
            columns = data.columns
            image = "${baseDirectory}/${data.image}"
            imageHeight = data.imageHeight
            imageWidth = data.imageWidth
            margin = data.margin
            name = data.name
            spacing = data.spacing
            tileCount = data.tileCount
            tileHeight = data.tileHeight
            tileWidth = data.tileWidth
            tiles = data.tiles
        }
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
