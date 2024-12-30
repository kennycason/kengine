package com.kengine.map.tiled

import com.kengine.graphics.AnimatedSprite
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.sdl.getSDLContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/*
 * TODO
 * sprite/texture batching
 * texture atlases to minimize textures
 */
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
        val GID_VERTICAL_FLAG = 0x40000000u  // bit 30 (0100...)
        val GID_DIAGONAL_FLAG = 0x20000000u  // bit 29 (0010...)
        val TILE_INDEX_MASK = 0x1FFFFFFFu  // bottom 29 bits for tile id
    }

    // position offset for rendering
    @Transient
    val p = Vec2()

    // Layer lookup
    private val layersByName = layers.associateBy(TiledMapLayer::name)

//    @Transient
//    private val batches: MutableMap<Texture, SpriteBatch> = mutableMapOf()

    // Tileset and spritesheet storage
    private data class TilesetAndSpriteSheet(
        val tileset: Tileset,
        val spriteSheet: SpriteSheet
    )

    @Transient
    private lateinit var tilesetsWithSprites: List<TilesetAndSpriteSheet>

    @Transient
    private lateinit var gidToTilesetArray: Array<TilesetAndSpriteSheet?>

    @Transient
    private val animatedSprites = mutableMapOf<UInt, AnimatedSprite>()

    private var isCacheBuilt = false

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

    /**
     * build transient fields after deserialization or when tilesets change.
     */
    private fun buildResourceCaches() {
        isCacheBuilt = true
        // initialize tilesets with spritesheets
        tilesetsWithSprites = tilesets.map { tileset ->
            val spriteSheet = SpriteSheet.fromSprite(
                sprite = Sprite.fromFilePath(tileset.image!!),
                tileWidth = tileset.tileWidth!!,
                tileHeight = tileset.tileHeight!!,
                offsetX = tileset.margin,
                offsetY = tileset.margin,
                paddingX = tileset.spacing,
                paddingY = tileset.spacing
            )

            // Process animations
            tileset.tiles?.forEach { tile ->
                tile.animation?.let { frames ->
                    val frameSprites = frames.map { frame ->
                        val (tilePx, tilePy) = getTilePosition(frame.tileid.toUInt() + tileset.firstgid, tileset)
                        spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                    }
                    val frameDurations = frames.map { it.duration.toLong() }
                    val animatedSprite = AnimatedSprite.fromSprites(
                        sprites = frameSprites,
                        frameDurations = frameDurations
                    )
                    animatedSprites[tile.id.toUInt() + tileset.firstgid] = animatedSprite
                }
            }

            TilesetAndSpriteSheet(tileset, spriteSheet)
        }

        // initialize batches for each texture
//        batches.clear()
//        tilesetsWithSprites.forEach {
//            batches[it.spriteSheet.texture] = SpriteBatch(it.spriteSheet.texture)
//        }

        // precompute GID lookup
        val maxGid = tilesetsWithSprites.maxOf {
            it.tileset.firstgid + it.tileset.tileCount!!.toUInt() - 1u
        }
        gidToTilesetArray = arrayOfNulls<TilesetAndSpriteSheet>(maxGid.toInt() + 1).also { array ->
            tilesetsWithSprites.forEach { tileset ->
                val start = tileset.tileset.firstgid.toInt()
                val end = (start + tileset.tileset.tileCount!!.toInt() - 1)
                for (i in start..end) {
                    array[i] = tileset
                }
            }
        }
    }

    /**
     * Updates animated tiles once per frame.
     */
    fun update() {
        animatedSprites.values.forEach { it.update() }
    }

    fun draw() {
        // batches.values.forEach { it.begin() }
        layers.forEach { draw(it) }
        // batches.values.forEach { it.end() }
    }

    fun draw(layerName: String) {
        //  batches.values.forEach { it.begin() }
        draw(layersByName.getValue(layerName))
        //  batches.values.forEach { it.end() }
    }

    private fun draw(layer: TiledMapLayer) {
        if (!layer.visible || layer.type != "tilelayer") return
        if (!isCacheBuilt) {
            buildResourceCaches()
        }

        // calculate the screen and tile coordinates to draw
        val screenLeft = -p.x
        val screenRight = screenLeft + getSDLContext().screenWidth
        val screenTop = -p.y
        val screenBottom = screenTop + getSDLContext().screenHeight

        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layer.width!! - 1)
        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layer.height!! - 1)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val rawGid = layer.getTileAt(x, y)
                if (rawGid == 0u) continue  // Skip empty tile

                val decoded = decodeTileGid(rawGid)
                if (decoded.tileId <= 0u) continue  // Skip invalid GID

                val (flipMode, angle) = decodeFlipAndRotation(decoded)
                val dstX = x * tileWidth + p.x
                val dstY = y * tileHeight + p.y

                // Handle Animated Tiles
                if (decoded.tileId in animatedSprites) {
                    animatedSprites[decoded.tileId]!!.draw(dstX, dstY, flipMode)
                } else {
                    // handle Static Tiles
                    val tilesetWithSprite = findTilesetForGid(decoded.tileId)
                    val (tilePx, tilePy) = getTilePosition(decoded.tileId, tilesetWithSprite.tileset)

                    val tile: Sprite = tilesetWithSprite.spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                    tile.draw(dstX, dstY, flipMode, angle) // Use Flip + Angle for static
                }

                // standard
                // val tile: Sprite = tilesetWithSprite.spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                // tile.draw(dstX, dstY, flipMode, angle)

                // batch
                // val batch = batches[tile.texture]!!
                // batch.draw(tile, dstX.toFloat(), dstY.toFloat(), flipMode, angle)
            }
        }
    }

    private fun decodeFlipAndRotation(decoded: DecodedTile): Pair<FlipMode, Double> {
        return when {
            // No flags set
            !decoded.flipH && !decoded.flipV && !decoded.flipD -> FlipMode.NONE to 0.0

            // Single flags
            decoded.flipH && !decoded.flipV && !decoded.flipD -> FlipMode.HORIZONTAL to 0.0
            !decoded.flipH && decoded.flipV && !decoded.flipD -> FlipMode.VERTICAL to 0.0
            !decoded.flipH && !decoded.flipV && decoded.flipD -> FlipMode.VERTICAL to 90.0

            // Double flags
            decoded.flipH && !decoded.flipV && decoded.flipD -> FlipMode.NONE to 90.0
            decoded.flipH && decoded.flipV && !decoded.flipD -> FlipMode.BOTH to 0.0
            !decoded.flipH && decoded.flipV && decoded.flipD -> FlipMode.NONE to 270.0

            // Triple flags
            decoded.flipH && decoded.flipV && decoded.flipD -> FlipMode.HORIZONTAL to 90.0

            // Default fallback (shouldn't be reached)
            else -> FlipMode.NONE to 0.0
        }
    }

    private fun findTilesetForGid(gid: UInt): TilesetAndSpriteSheet {
        return gidToTilesetArray[gid.toInt()]
            ?: throw IllegalStateException("No tileset found for gid: $gid")
    }

    private fun getTilePosition(tileId: UInt, tileset: Tileset): Pair<UInt, UInt> {
        val localId = tileId - tileset.firstgid
        val cols = tileset.columns ?: throw IllegalArgumentException("Tileset '${tileset.name}' must have defined columns.")

        val gridX = localId % cols.toUInt()
        val gridY = localId / cols.toUInt()

//        if (logger.isTraceEnabled()) {
//            logger.trace {
//                "getTilePosition(tileId=$tileId): firstgid=${tileset.firstgid}, localId=$localId, cols=$cols, grid=($gridX,$gridY)"
//            }
//        }

        return gridX to gridY
    }

    private fun decodeTileGid(gid: UInt): DecodedTile {
        val flipH = (gid and GID_HORIZONTAL_FLAG) != 0u
        val flipV = (gid and GID_VERTICAL_FLAG) != 0u
        val flipD = (gid and GID_DIAGONAL_FLAG) != 0u
        val tileId = gid and TILE_INDEX_MASK

//        if (logger.isTraceEnabled()) {
//            logger.traceStream {
//                writeLn("Decoding GID: ${gid.toString(2).padStart(32, '0')} (${gid})")
//                writeLn("H FLAG:       ${GID_HORIZONTAL_FLAG.toString(2).padStart(32, '0')}")
//                writeLn("V FLAG:       ${GID_VERTICAL_FLAG.toString(2).padStart(32, '0')}")
//                writeLn("D FLAG:       ${GID_DIAGONAL_FLAG.toString(2).padStart(32, '0')}")
//            }
//        }

        return DecodedTile(tileId, flipH, flipV, flipD)
    }

    fun cleanup() {
        animatedSprites.values.forEach { it.cleanup() }
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

    data class DecodedTile(
        val tileId: UInt,
        val flipH: Boolean,
        val flipV: Boolean,
        val flipD: Boolean
    )
}
