package com.kengine.map.tiled

import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.log.Logging
import com.kengine.math.IntVec2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TiledMap(
    val width: Int,
    val height: Int,
    @SerialName("tilewidth")
    val tileWidth: Int,
    @SerialName("tileheight")
    val tileHeight: Int,
    val layers: List<Layer>,
    val tilesets: List<Tileset>,
    val orientation: String,
    @SerialName("renderorder")
    val renderOrder: String,
    val infinite: Boolean = false
) : Logging {

    private data class TilesetAndSpriteSheet(
        val tileset: Tileset,
        val spriteSheet: SpriteSheet
    )

    private val tilesetsWithSprites: List<TilesetAndSpriteSheet> by lazy {
        tilesets.map {
            TilesetAndSpriteSheet(
                tileset = it,
                spriteSheet = SpriteSheet.fromSprite(
                    sprite = Sprite.fromFilePath(it.image),
                    tileWidth = it.tileWidth,
                    tileHeight = it.tileHeight
                )
            )
        }
    }

    init {
        val logStreamBuilder = logger
            .infoStream()
            .writeLn { "Loading Map." }
            .writeLn { "Map: ${width}x${height}" }
            .writeLn { "Tile Dim: ${tileWidth}x${tileHeight}" }
            .writeLn { "Layers: ${layers.size}" }

        layers.forEach {
            logStreamBuilder.writeLn { it.toString() }
        }
        logStreamBuilder.writeLn { "Tilesets: ${tilesets.size}" }
        tilesets.forEach {
            logStreamBuilder.writeLn { it.toString() }
        }
    }

    fun draw() {
        layers
            .forEach { draw(it) }
    }

    fun draw(layer: Layer) {
        if (!layer.visible) return
        if (layer.type != "tilelayer") return

        for (x in 0 until layer.width) {
            for (y in 0 until layer.height) {
                val tileId = layer.getTileAt(x, y)
                val tilePosition = getTilePosition(tileId, tilesetsWithSprites.first().tileset)
//                logger.info { "${tilePosition.x}x${tilePosition.y} -> ${tileId}" }
                val sprite = tilesetsWithSprites
                    .first()
                    .spriteSheet.getTile(tilePosition.x, tilePosition.y)
                sprite.draw((x * tileWidth).toDouble(), (y * tileHeight).toDouble())
            }
        }
    }

    private fun getTilePosition(tileId: Int, tileset: Tileset): IntVec2 {
        val localId = tileId - tileset.firstgid
        val tileX = (localId % tileset.columns) * (tileset.tileWidth + tileset.spacing) + tileset.margin
        val tileY = (localId / tileset.columns) * (tileset.tileHeight + tileset.spacing) + tileset.margin
        return IntVec2(tileX, tileY)
    }

    override fun toString(): String {
        return "TiledMap(\n" +
                "width=$width, height=$height,\n\t" +
                "tileWidth=$tileWidth, tileHeight=$tileHeight,\n\t" +
                "layers=\n\t\t${layers.joinToString("\n\t\t")},\n\t" +
                "tilesets=\n\t\t${tilesets.joinToString("\n\t\t")},\n\t" +
                "orientation='$orientation',\n\t" +
                "renderOrder='$renderOrder',\n\t" +
                "infinite=$infinite\n" +
                ")"
    }


}



