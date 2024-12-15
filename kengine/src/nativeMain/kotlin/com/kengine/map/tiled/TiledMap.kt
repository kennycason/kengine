package com.kengine.map.tiled

import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.log.Logging
import com.kengine.math.IntVec2
import com.kengine.math.Vec2
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
    val layers: List<TiledMapLayer>,
    val tilesets: List<Tileset>,
    val orientation: String,
    @SerialName("renderorder")
    val renderOrder: String,
    val infinite: Boolean = false,
) : Logging {

    val p by lazy { Vec2() }

    private val layersByName = layers.associateBy(TiledMapLayer::name)

    private data class TilesetAndSpriteSheet(
        val tileset: Tileset,
        val spriteSheet: SpriteSheet
    )

    private val tilesetsWithSprites: List<TilesetAndSpriteSheet> by lazy {
        tilesets.map { tileset ->
            TilesetAndSpriteSheet(
                tileset = tileset,
                spriteSheet = SpriteSheet.fromSprite(
                    sprite = Sprite.fromFilePath(tileset.image!!),
                    tileWidth = tileset.tileWidth!!,
                    tileHeight = tileset.tileHeight!!,
                    offsetX = tileset.margin,    // Pass margin
                    offsetY = tileset.margin,    // Pass margin
                    paddingX = tileset.spacing,  // Pass spacing
                    paddingY = tileset.spacing   // Pass spacing
                )
            )
        }
    }

    init {
        tilesets.sortedBy { it.firstgid }
        logger
            .infoStream()
            .writeLn { "Loading Map." }
            .writeLn { "Map: ${width}x${height}" }
            .writeLn { "Tile Dim: ${tileWidth}x${tileHeight}" }
            .writeLn { "Layers: ${layers.size}" }
            .writeLn(layers)
            .writeLn { "Tilesets: ${tilesets.size}" }
            .writeLn(tilesets)
            .flush()
    }

    fun draw() {
        layers.forEach { draw(it) }
    }

    fun draw(layerName: String) {
        draw(layersByName.getValue(layerName))
    }

    fun draw(layer: TiledMapLayer) {
        if (!layer.visible) return
        if (layer.type != "tilelayer") return

        for (x in 0 until layer.width!!) {
            for (y in 0 until layer.height!!) {
                val tileId = layer.getTileAt(x, y)
                if (tileId > 0) { // Skip empty tiles
                    try {
                        val tilesetWithSprite = findTilesetForGid(tileId)
                        val tilePosition = getTilePosition(tileId, tilesetWithSprite.tileset)
                        val sprite = tilesetWithSprite.spriteSheet.getTile(
                            tilePosition.x / tilesetWithSprite.tileset.tileWidth!!,
                            tilePosition.y / tilesetWithSprite.tileset.tileHeight!!
                        )
                        sprite.draw(p.x + (x * tileWidth).toDouble(), p.y + (y * tileHeight).toDouble())
                    } catch (e: IllegalArgumentException) {
                        logger.error("Tile rendering error for GID $tileId at ($x, $y): ${e.message}")
                    }
                }
            }
        }
    }

    private fun findTilesetForGid(gid: Int): TilesetAndSpriteSheet {
        val tileset = tilesetsWithSprites.firstOrNull { gid >= it.tileset.firstgid }
            ?: throw IllegalStateException("No tileset found for gid: $gid")
        return tileset
    }

    private fun getTilePosition(tileId: Int, tileset: Tileset): IntVec2 {
        val localId = tileId - tileset.firstgid // adjust for tileset's firstgid
        if (localId < 0) throw IllegalArgumentException("Invalid tileId: $tileId (localId: $localId)")

        val cols = tileset.columns!!
        val x = (localId % cols) * (tileset.tileWidth!! + tileset.spacing) + tileset.margin
        val y = (localId / cols) * (tileset.tileHeight!! + tileset.spacing) + tileset.margin

        logger.debug { "GID: $tileId, LocalID: ${tileId - tileset.firstgid}, TilePos: $x,$y" }

        return IntVec2(x, y)
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

