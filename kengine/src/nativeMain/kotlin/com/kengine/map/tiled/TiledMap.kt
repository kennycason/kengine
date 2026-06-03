package com.kengine.map.tiled

import com.kengine.graphics.AnimatedSprite
import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.sdl.getSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import sdl3.image.SDL_SetTextureAlphaModFloat

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

    @Transient
    private var isCacheBuilt = false

    @Transient
    private val layersByName = layers.associateBy(TiledMapLayer::name)

    @Transient
    private lateinit var tilesetsWithSprites: List<TilesetAndSpriteSheet>

    @Transient
    private lateinit var gidToTilesetArray: Array<TilesetAndSpriteSheet?>

    @Transient
    private val animatedSprites = mutableMapOf<UInt, AnimatedSprite>()

    @Transient
    private val tilesetColumns = mutableMapOf<Tileset, UInt>()

    @Transient
    private val resolvedLayers = mutableMapOf<String, Array<ResolvedCell?>>()

    init {
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
        tilesets.forEach { tileset ->
            tilesetColumns[tileset] = tileset.columns!!.toUInt()
        }
        validateTilesets()
    }

    private fun validateTilesets() {
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

        buildResolvedLayers()

        logger.debug { "Resource cache build complete" }
        isCacheBuilt = true
    }

    private fun buildResolvedLayers() {
        layers.forEach { layer ->
            if (layer.type != "tilelayer" || layer.width == null || layer.height == null) return@forEach

            val layerWidth = layer.width
            val layerHeight = layer.height
            val cells = arrayOfNulls<ResolvedCell>(layerWidth * layerHeight)

            for (i in 0 until layerWidth * layerHeight) {
                val rawGid = layer.decodedData[i]
                if (rawGid == 0u) continue

                val tileId = rawGid and TILE_INDEX_MASK
                if (tileId == 0u) continue

                val flipH = (rawGid and GID_HORIZONTAL_FLAG) != 0u
                val flipV = (rawGid and GID_VERTICAL_FLAG) != 0u
                val flipD = (rawGid and GID_DIAGONAL_FLAG) != 0u

                val flipIndex = (if (flipH) 1 else 0) or
                    (if (flipV) 2 else 0) or
                    (if (flipD) 4 else 0)
                val (flip, angle) = flipRotationCache[flipIndex]

                val animated = animatedSprites[tileId]
                if (animated != null) {
                    cells[i] = ResolvedCell(
                        sprite = null,
                        animatedSprite = animated,
                        flip = if (flipH) FlipMode.HORIZONTAL else FlipMode.NONE,
                        angle = 0.0
                    )
                } else {
                    val tilesetWithSprite = findTilesetForGid(tileId)
                    val (px, py) = getTilePosition(tileId, tilesetWithSprite.tileset)
                    val sprite = tilesetWithSprite.spriteSheet.getTile(px.toInt(), py.toInt())
                    cells[i] = ResolvedCell(
                        sprite = sprite,
                        animatedSprite = null,
                        flip = flip,
                        angle = angle
                    )
                }
            }

            resolvedLayers[layer.name] = cells
        }
    }

    fun update() {
        animatedSprites.values.forEach { it.update() }
    }

    fun draw() {
        if (!isCacheBuilt) buildResourceCaches()

        layers.forEach { layer ->
            if (layer.visible && layer.type == "tilelayer") {
                drawResolved(layer)
            }
        }
    }

    fun draw(layerName: String) {
        if (!isCacheBuilt) buildResourceCaches()

        val layer = layersByName.getValue(layerName)
        if (layer.visible && layer.type == "tilelayer") {
            drawResolved(layer)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun drawResolved(layer: TiledMapLayer) {
        val cells = resolvedLayers[layer.name] ?: return
        val layerWidth = layer.width ?: return
        val layerHeight = layer.height ?: return

        val offsetX = p.x + layer.x
        val offsetY = p.y + layer.y

        val screenLeft = -offsetX
        val screenRight = screenLeft + getSDLContext().screenWidth
        val screenTop = -offsetY
        val screenBottom = screenTop + getSDLContext().screenHeight

        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layerWidth - 1)
        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layerHeight - 1)

        val tileWidthD = tileWidth.toDouble()
        val tileHeightD = tileHeight.toDouble()

        val hasOpacity = layer.opacity < 1f
        if (hasOpacity) {
            tilesetsWithSprites.forEach {
                SDL_SetTextureAlphaModFloat(it.spriteSheet.texture.texture, layer.opacity)
            }
        }

        for (y in startY..endY) {
            val baseY = y * tileHeightD + offsetY
            val rowOffset = y * layerWidth

            for (x in startX..endX) {
                val cell = cells[rowOffset + x] ?: continue
                val dstX = x * tileWidthD + offsetX

                if (cell.animatedSprite != null) {
                    cell.animatedSprite.draw(dstX, baseY, cell.flip)
                } else {
                    cell.sprite!!.draw(dstX, baseY, cell.flip, cell.angle)
                }
            }
        }

        if (hasOpacity) {
            tilesetsWithSprites.forEach {
                SDL_SetTextureAlphaModFloat(it.spriteSheet.texture.texture, 1f)
            }
        }
    }

    private val flipRotationCache = Array(8) {
        FlipMode.NONE to 0.0
    }.apply {
        this[0] = FlipMode.NONE to 0.0
        this[1] = FlipMode.HORIZONTAL to 0.0
        this[2] = FlipMode.VERTICAL to 0.0
        this[3] = FlipMode.BOTH to 0.0
        this[4] = FlipMode.VERTICAL to 90.0
        this[5] = FlipMode.NONE to 90.0
        this[6] = FlipMode.NONE to 270.0
        this[7] = FlipMode.HORIZONTAL to 90.0
    }

    private fun findTilesetForGid(gid: UInt): TilesetAndSpriteSheet =
        gidToTilesetArray[gid.toInt()] ?: throw IllegalStateException("No tileset found for gid: $gid")

    private fun getTilePosition(tileId: UInt, tileset: Tileset): Pair<UInt, UInt> {
        val localId = tileId - tileset.firstgid
        val cols = tilesetColumns[tileset]!!
        return localId % cols to localId / cols
    }

    fun cleanup() {
        animatedSprites.values.forEach { it.cleanup() }
        resolvedLayers.clear()
    }

    class ResolvedCell(
        val sprite: Sprite?,
        val animatedSprite: AnimatedSprite?,
        val flip: FlipMode,
        val angle: Double
    )

    private data class TilesetAndSpriteSheet(
        val tileset: Tileset,
        val spriteSheet: SpriteSheet
    )
}
