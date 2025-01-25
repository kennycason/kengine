package com.kengine.map.tiled

import com.kengine.graphics.AnimatedSprite
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteBatch
import com.kengine.graphics.SpriteSheet
import com.kengine.graphics.Texture
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
    var tilesets: List<Tileset>,
    val orientation: String,
    @SerialName("renderorder")
    val renderOrder: String,
    val infinite: Boolean = false,
) : Logging {

    companion object {
        val GID_HORIZONTAL_FLAG = 0x80000000u
        val GID_VERTICAL_FLAG = 0x40000000u
        val GID_DIAGONAL_FLAG = 0x20000000u
        val TILE_INDEX_MASK = 0x1FFFFFFFu
    }

    @Transient
    val p = Vec2()

    /**
     * batch mode still WIP
     * Batch Mode on avg 10.6ms/render
     * Batch Mode off 6.5ms/render
     * TiledMapDrawITest
     */
    @Transient
    private val enableBatch = false

    @Transient
    private var isCacheBuilt = false

    @Transient
    private val layersByName = layers.associateBy(TiledMapLayer::name)

    @Transient
    private val batches = mutableMapOf<Texture, SpriteBatch>()

    @Transient
    private lateinit var tilesetsWithSprites: List<TilesetAndSpriteSheet>

    @Transient
    private lateinit var gidToTilesetArray: Array<TilesetAndSpriteSheet?>

    @Transient
    private val animatedSprites = mutableMapOf<UInt, AnimatedSprite>()

    @Transient
    private val tilesetCache = HashMap<UInt, TilesetAndSpriteSheet>(4)

    @Transient
    private val spriteCache = HashMap<UInt, Sprite>(64)

    @Transient
    private val persistentSpriteCache = HashMap<UInt, Sprite>(1024)

    @Transient
    private val screenWidth = getSDLContext().screenWidth

    @Transient
    private val screenHeight = getSDLContext().screenHeight

    @Transient
    private val tilesetColumns = mutableMapOf<Tileset, UInt>()

    init {
        // Sort tilesets
        tilesets = tilesets.sortedBy { it.firstgid }

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

    fun initialize() {
        // Pre-compute tileset columns for faster lookup during rendering
        tilesets.forEach { tileset ->
            tilesetColumns[tileset] = tileset.columns!!.toUInt()
        }

        // Validation can be done in debug builds only
        validateTilesets()
    }

    private fun validateTilesets() {
        // Move validation to a separate method that's only called in debug builds
        tilesets.forEach { tileset ->
            require(tileset.columns != null) {
                "Tileset '${tileset.name ?: "unnamed"}' (firstgid: ${tileset.firstgid}) has no columns defined."
            }
            require(tileset.tileWidth != null) {
                "Tileset '${tileset.name ?: "unnamed"}' (firstgid: ${tileset.firstgid}) has no tileWidth defined."
            }
            require(tileset.tileHeight != null) {
                "Tileset '${tileset.name ?: "unnamed"}' (firstgid: ${tileset.firstgid}) has no tileHeight defined."
            }
            require(tileset.image != null) {
                "Tileset '${tileset.name ?: "unnamed"}' (firstgid: ${tileset.firstgid}) has no image defined."
            }
        }
    }

    private fun buildResourceCaches() {
        if (isCacheBuilt) return

        logger.debug { "Starting resource cache build..." }

        // initialize tilesets with spritesheets
        tilesetsWithSprites = tilesets.map { tileset ->
            logger.debug { "Loading spritesheet for tileset: ${tileset.name} from ${tileset.image}" }

            val spriteSheet = SpriteSheet.fromSprite(
                sprite = Sprite.fromFilePath(tileset.image!!),
                tileWidth = tileset.tileWidth!!,
                tileHeight = tileset.tileHeight!!,
                offsetX = tileset.margin,
                offsetY = tileset.margin,
                paddingX = tileset.spacing,
                paddingY = tileset.spacing
            )

            // process animations
            tileset.tiles?.forEach { tile ->
                tile.animation?.let { frames ->
                    val frameSprites = frames.map { frame ->
                        val (tilePx, tilePy) = getTilePosition(frame.tileid.toUInt() + tileset.firstgid, tileset)
                        spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                    }
                    val frameDurations = frames.map { it.duration.toLong() }
                    animatedSprites[tile.id.toUInt() + tileset.firstgid] = AnimatedSprite.fromSprites(
                        sprites = frameSprites,
                        frameDurations = frameDurations
                    )
                }
            }

            TilesetAndSpriteSheet(tileset, spriteSheet)
        }

        // Initialize GID lookup array
        val maxGid = tilesetsWithSprites.maxOf {
            it.tileset.firstgid + it.tileset.tileCount!!.toUInt() - 1u
        }
        gidToTilesetArray = arrayOfNulls<TilesetAndSpriteSheet>(maxGid.toInt() + 1).also { array ->
            tilesetsWithSprites.forEach { tilesetWithSprite ->
                val tileset = tilesetWithSprite.tileset
                val start = tileset.firstgid.toInt()
                val end = (start + tileset.tileCount!!.toInt() - 1)
                for (i in start..end) {
                    array[i] = tilesetWithSprite
                }
            }
        }

        // initialize batches if enabled
        if (enableBatch) {
            batches.clear()
            tilesetsWithSprites.forEach { tilesetAndSheet ->
                val texture = tilesetAndSheet.spriteSheet.texture
                if (!batches.containsKey(texture)) {
                    // calculate maximum possible tiles in view
                    val maxTilesInView = (screenWidth / tileWidth + 1) *
                        (screenHeight / tileHeight + 1)
                    batches[texture] = SpriteBatch(texture, maxTilesInView)
                }
            }
        }

        logger.debug { "Resource cache build complete" }
        isCacheBuilt = true
    }

    fun update() {
        animatedSprites.values.forEach { it.update() }
    }

    fun draw() {
        if (!isCacheBuilt) buildResourceCaches()

        layers.forEach { layer ->
            if (layer.visible && layer.type == "tilelayer") {
                if (enableBatch) {
                    draw(layer)
                } else {
                    drawNoBatch(layer)
                }
            }
        }
    }

    fun draw(layerName: String) {
        if (!isCacheBuilt) buildResourceCaches()

        val layer = layersByName.getValue(layerName)
        if (layer.visible && layer.type == "tilelayer") {
            if (enableBatch) {
                draw(layer)
            } else {
                drawNoBatch(layer)
            }
        }
    }

    private fun draw(layer: TiledMapLayer) {
        // Calculate visible region
        val screenLeft = -p.x
        val screenRight = screenLeft + getSDLContext().screenWidth
        val screenTop = -p.y
        val screenBottom = screenTop + screenHeight

        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layer.width!! - 1)
        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layer.height!! - 1)

        val tileWidthF = tileWidth.toFloat()
        val tileHeightF = tileHeight.toFloat()
        val offsetX = p.x.toFloat()
        val offsetY = p.y.toFloat()

        // Clear caches
        tilesetCache.clear()
        spriteCache.clear()

        // Track active batches to minimize state changes
        var currentBatch: SpriteBatch? = null
        var lastTexture: Texture? = null

        // Render tiles
        for (y in startY..endY) {
            val baseY = y * tileHeightF + offsetY

            for (x in startX..endX) {
                val rawGid = layer.getTileAt(x, y)
                if (rawGid == 0u) continue

                val decoded = decodeTileGid(rawGid)
                if (decoded.tileId <= 0u) continue

                if (decoded.tileId in animatedSprites) {
                    // handle animated tiles separately (non-batched)
                    currentBatch?.flush()
                    animatedSprites[decoded.tileId]!!.draw(
                        x * tileWidth + p.x,
                        y * tileHeight + p.y,
                        if (decoded.flipH) FlipMode.HORIZONTAL else FlipMode.NONE
                    )
                    continue
                }

                val (flip, angle) = decodeFlipAndRotation(decoded)

                // get cached tileset and sprite
                val tilesetWithSprite = tilesetCache.getOrPut(decoded.tileId) {
                    findTilesetForGid(decoded.tileId)
                }
                val tile = spriteCache.getOrPut(decoded.tileId) {
                    val (tilePx, tilePy) = getTilePosition(decoded.tileId, tilesetWithSprite.tileset)
                    tilesetWithSprite.spriteSheet.getTile(tilePx.toInt(), tilePy.toInt())
                }

                // minimize batch switches
                if (tile.texture != lastTexture) {
                    currentBatch?.flush()
                    currentBatch = batches[tile.texture]?.also {
                        if (!it.isBatching) it.begin()
                    }
                    lastTexture = tile.texture
                }

                // draw tile
                currentBatch?.draw(
                    tile,
                    x * tileWidthF + offsetX,
                    baseY,
                    flip = flip,
                    angle = angle
                )
            }
        }

        // flush final batch
        currentBatch?.flush()
    }

    private fun drawNoBatch(layer: TiledMapLayer) {
        // Pre-calculate these values once per frame
        val offsetX = p.x
        val offsetY = p.y

        // calculate visible region
        val screenLeft = -offsetX
        val screenRight = screenLeft + screenWidth
        val screenTop = -offsetY
        val screenBottom = screenTop + screenHeight

        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layer.width!! - 1)
        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layer.height!! - 1)

        // Cache commonly used values as doubles
        val tileWidthD = tileWidth.toDouble()
        val tileHeightD = tileHeight.toDouble()
        val offsetXD = offsetX
        val offsetYD = offsetY

        for (y in startY..endY) {
            val baseY = y * tileHeightD + offsetYD

            for (x in startX..endX) {
                val rawGid = layer.getTileAt(x, y)
                if (rawGid == 0u) continue

                val decoded = decodeTileGid(rawGid)
                if (decoded.tileId <= 0u) continue

                val dstX = x * tileWidthD + offsetXD

                if (decoded.tileId in animatedSprites) {
                    animatedSprites[decoded.tileId]!!.draw(dstX, baseY,
                        if (decoded.flipH) FlipMode.HORIZONTAL else FlipMode.NONE
                    )
                } else {
                    val tile = persistentSpriteCache.getOrPut(decoded.tileId) {
                        val tilesetWithSprite = findTilesetForGid(decoded.tileId)
                        val (px, py) = getTilePosition(decoded.tileId, tilesetWithSprite.tileset)
                        tilesetWithSprite.spriteSheet.getTile(px.toInt(), py.toInt())
                    }

                    val (flip, angle) = decodeFlipAndRotation(decoded)
                    tile.draw(dstX, baseY, flip, angle)
                }
            }
        }
    }

    // Cache flip/rotation results
    private val flipRotationCache = Array(8) {
        FlipMode.NONE to 0.0
    }.apply {
        // Precompute all possible flip/rotation combinations
        this[0] = FlipMode.NONE to 0.0  // !H !V !D
        this[1] = FlipMode.HORIZONTAL to 0.0  // H !V !D
        this[2] = FlipMode.VERTICAL to 0.0  // !H V !D
        this[3] = FlipMode.VERTICAL to 90.0  // !H !V D
        this[4] = FlipMode.NONE to 90.0  // H !V D
        this[5] = FlipMode.BOTH to 0.0  // H V !D
        this[6] = FlipMode.NONE to 270.0  // !H V D
        this[7] = FlipMode.HORIZONTAL to 90.0  // H V D
    }

    private fun decodeFlipAndRotation(decoded: DecodedTile): Pair<FlipMode, Double> {
        val index = (if (decoded.flipH) 1 else 0) or
                    (if (decoded.flipV) 2 else 0) or
                    (if (decoded.flipD) 4 else 0)
        return flipRotationCache[index]
    }

    private fun findTilesetForGid(gid: UInt): TilesetAndSpriteSheet =
        gidToTilesetArray[gid.toInt()] ?: throw IllegalStateException("No tileset found for gid: $gid")

    private fun getTilePosition(tileId: UInt, tileset: Tileset): Pair<UInt, UInt> {
        val localId = tileId - tileset.firstgid
        val cols = tilesetColumns[tileset]!!
        return localId % cols to localId / cols
    }

    private fun decodeTileGid(gid: UInt): DecodedTile {
        val flipH = (gid and GID_HORIZONTAL_FLAG) != 0u
        val flipV = (gid and GID_VERTICAL_FLAG) != 0u
        val flipD = (gid and GID_DIAGONAL_FLAG) != 0u
        val tileId = gid and TILE_INDEX_MASK
        return DecodedTile(tileId, flipH, flipV, flipD)
    }

    fun cleanup() {
        batches.values.forEach { it.cleanup() }
        animatedSprites.values.forEach { it.cleanup() }
        tilesetCache.clear()
        spriteCache.clear()
        persistentSpriteCache.clear()
    }

    data class DecodedTile(
        val tileId: UInt,
        val flipH: Boolean,
        val flipV: Boolean,
        val flipD: Boolean
    )

    private data class TilesetAndSpriteSheet(
        val tileset: Tileset,
        val spriteSheet: SpriteSheet
    )
}
