package com.kengine.map.tiled

import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.graphics.getSpriteContext
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.sdl.getSDLContext
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

    companion object {
        val GID_HORIZONTAL_FLAG = 0x80000000u  // bit 31 (1000...)
        val GID_VERTICAL_FLAG   = 0x40000000u  // bit 30 (0100...)
        val GID_DIAGONAL_FLAG   = 0x20000000u  // bit 29 (0010...)
        val TILE_INDEX_MASK     = 0x1FFFFFFFu  // bottom 29 bits for tile id
    }

    // position offset for rendering
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
                    offsetX = tileset.margin,
                    offsetY = tileset.margin,
                    paddingX = tileset.spacing,
                    paddingY = tileset.spacing
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

    private fun draw(layer: TiledMapLayer) {
        if (!layer.visible || layer.type != "tilelayer") {
            logger.debug { "Skipping layer ${layer.name}: visible=${layer.visible}, type=${layer.type}" }
            return
        }
        logger.debug { "Drawing tilelayer: ${layer.name}" }

        // calculate visible area based on screen dimensions and position
        val screenLeft = -p.x
        val screenRight = screenLeft + getSDLContext().screenWidth
        val screenTop = -p.y
        val screenBottom = screenTop + getSDLContext().screenHeight

        // convert to tile coordinates
        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layer.width!! - 1)
        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layer.height!! - 1)

        getSpriteContext().spriteBatch.begin()
        for (y in startY..endY) {
            for (x in startX..endX) {
                val rawGid = layer.getTileAt(x, y)
                if (rawGid == 0u) continue  // compare with 0u for UInt

                val decoded = decodeTileGid(rawGid)

                logger.trace() {
                    "Tile at ($x, $y): rawGid=$rawGid -> tileId=${decoded.tileId}, " +
                            "flipH=${decoded.flipH}, flipV=${decoded.flipV}, flipD=${decoded.flipD}"
                }

                if (decoded.tileId <= 0u) {
                    logger.debug { "Tile at ($x, $y) has tileId <= 0 after decoding, skipping." }
                    continue
                }

                val tilesetWithSprite = findTilesetForGid(decoded.tileId)
                val (tilePx, tilePy) = getTilePosition(decoded.tileId, tilesetWithSprite.tileset)
                logger.trace {
                    "Tile ($x,$y): Using tileset '${tilesetWithSprite.tileset.name}', " +
                            "tilePos=($tilePx,$tilePy), firstgid=${tilesetWithSprite.tileset.firstgid}, " +
                            "tileCount=${tilesetWithSprite.tileset.tileCount}, columns=${tilesetWithSprite.tileset.columns}"
                }

                val sprite = tilesetWithSprite.spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                drawSprite(x, y, sprite, decoded)
            }
        }
        getSpriteContext().spriteBatch.end()
    }

    private fun drawSprite(x: Int, y: Int, sprite: Sprite, decoded: DecodedTile) {
        val (angle, flip) = when {
            // no flags
            !decoded.flipH && !decoded.flipV && !decoded.flipD ->
                Pair(0.0, FlipMode.NONE)

            // single flags
            decoded.flipH && !decoded.flipV && !decoded.flipD -> // H
                Pair(0.0, FlipMode.HORIZONTAL)
            !decoded.flipH && decoded.flipV && !decoded.flipD -> // V
                Pair(0.0, FlipMode.VERTICAL)
            !decoded.flipH && !decoded.flipV && decoded.flipD -> // D
                Pair(90.0, FlipMode.VERTICAL)

            // double flags
            decoded.flipH && !decoded.flipV && decoded.flipD -> // HD
                Pair(90.0, FlipMode.NONE)
            decoded.flipH && decoded.flipV && !decoded.flipD -> // HV
                Pair(0.0, FlipMode.BOTH)
            !decoded.flipH && decoded.flipV && decoded.flipD -> // VD
                Pair(270.0, FlipMode.NONE)

            // triple flags
            decoded.flipH && decoded.flipV && decoded.flipD ->
                Pair(90.0, FlipMode.HORIZONTAL)

            else -> Pair(0.0, FlipMode.NONE)
        }

        getSpriteContext().spriteBatch.draw(sprite, p.x + (x * tileWidth), p.y + (y * tileHeight), flip, angle)
//        sprite.draw(
//            x = p.x + (x * tileWidth),
//            y = p.y + (y * tileHeight),
//            flip = flip,
//            angle = angle
//        )
    }

    private fun findTilesetForGid(gid: UInt): TilesetAndSpriteSheet {
        val tileset = tilesetsWithSprites
            .filter { gid >= it.tileset.firstgid }
            .maxByOrNull { it.tileset.firstgid }
            ?: throw IllegalStateException("No tileset found for gid: $gid")

        val lastGidInTileset = tileset.tileset.firstgid + tileset.tileset.tileCount!!.toUInt() - 1u
        if (gid > lastGidInTileset) {
            logger.warn {
                "GID $gid is outside of tileset '${tileset.tileset.name}' range [${tileset.tileset.firstgid}, $lastGidInTileset]. " +
                        "This may cause rendering issues."
            }
        }
        return tileset
    }

    private fun getTilePosition(tileId: UInt, tileset: Tileset): Pair<UInt, UInt> {
        val localId = tileId - tileset.firstgid
        val cols = tileset.columns ?: throw IllegalArgumentException("Tileset '${tileset.name}' must have defined columns.")

        val gridX = localId % cols.toUInt()
        val gridY = localId / cols.toUInt()

        logger.trace {
            "getTilePosition(tileId=$tileId): firstgid=${tileset.firstgid}, localId=$localId, cols=$cols, grid=($gridX,$gridY)"
        }

        return Pair(gridX, gridY)
    }

    private fun decodeTileGid(gid: UInt): DecodedTile {
        val flipH = (gid and GID_HORIZONTAL_FLAG) != 0u
        val flipV = (gid and GID_VERTICAL_FLAG) != 0u
        val flipD = (gid and GID_DIAGONAL_FLAG) != 0u
        val tileId = gid and TILE_INDEX_MASK

        logger.traceStream {
            writeLn("Decoding GID: ${gid.toString(2).padStart(32, '0')} (${gid})")
            writeLn("H FLAG:       ${GID_HORIZONTAL_FLAG.toString(2).padStart(32, '0')}")
            writeLn("V FLAG:       ${GID_VERTICAL_FLAG.toString(2).padStart(32, '0')}")
            writeLn("D FLAG:       ${GID_DIAGONAL_FLAG.toString(2).padStart(32, '0')}")
        }

        return DecodedTile(tileId, flipH, flipV, flipD)
    }

    data class DecodedTile(
        val tileId: UInt,
        val flipH: Boolean,
        val flipV: Boolean,
        val flipD: Boolean
    )

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