//package com.kengine.map.tiled
//
//import com.kengine.graphics.FlipMode
//import com.kengine.graphics.Sprite
//import com.kengine.graphics.SpriteSheet
//import com.kengine.graphics.getSpriteContext
//import com.kengine.log.Logging
//import com.kengine.math.Vec2
//import com.kengine.sdl.getSDLContext
//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//
//@Serializable
//class TiledMap(
//    val width: Int,
//    val height: Int,
//    @SerialName("tilewidth")
//    val tileWidth: Int,
//    @SerialName("tileheight")
//    val tileHeight: Int,
//    val layers: List<TiledMapLayer>,
//    val tilesets: List<Tileset>,
//    val orientation: String,
//    @SerialName("renderorder")
//    val renderOrder: String,
//    val infinite: Boolean = false,
//) : Logging {
//
//    companion object {
//        val GID_HORIZONTAL_FLAG = 0x80000000u  // bit 31 (1000...)
//        val GID_VERTICAL_FLAG = 0x40000000u  // bit 30 (0100...)
//        val GID_DIAGONAL_FLAG = 0x20000000u  // bit 29 (0010...)
//        val TILE_INDEX_MASK = 0x1FFFFFFFu  // bottom 29 bits for tile id
//    }
//
//    // position offset for rendering
//    val p by lazy { Vec2() }
//
//    private val layersByName = layers.associateBy(TiledMapLayer::name)
//
//    private data class TilesetAndSpriteSheet(
//        val tileset: Tileset,
//        val spriteSheet: SpriteSheet
//    )
//
//    private val tilesetsWithSprites: List<TilesetAndSpriteSheet> by lazy {
//        tilesets.map { tileset ->
//            TilesetAndSpriteSheet(
//                tileset = tileset,
//                spriteSheet = SpriteSheet.fromSprite(
//                    sprite = Sprite.fromFilePath(tileset.image!!),
//                    tileWidth = tileset.tileWidth!!,
//                    tileHeight = tileset.tileHeight!!,
//                    offsetX = tileset.margin,
//                    offsetY = tileset.margin,
//                    paddingX = tileset.spacing,
//                    paddingY = tileset.spacing
//                )
//            )
//        }
//    }
//
//    init {
//        tilesets.sortedBy { it.firstgid }
//        logger
//            .infoStream()
//            .writeLn { "Loading Map." }
//            .writeLn { "Map: ${width}x${height}" }
//            .writeLn { "Tile Dim: ${tileWidth}x${tileHeight}" }
//            .writeLn { "Layers: ${layers.size}" }
//            .writeLn(layers)
//            .writeLn { "Tilesets: ${tilesets.size}" }
//            .writeLn(tilesets)
//            .flush()
//    }
//
//    fun draw() {
//        layers.forEach { draw(it) }
//    }
//
//    fun draw(layerName: String) {
//        draw(layersByName.getValue(layerName))
//    }
//
//    @OptIn(ExperimentalForeignApi::class)
//    private fun draw(layer: TiledMapLayer) {
//        if (!layer.visible || layer.type != "tilelayer") return
//
//        // calculate the screen and tile coordinates to draw
//        val screenLeft = -p.x
//        val screenRight = screenLeft + getSDLContext().screenWidth
//        val screenTop = -p.y
//        val screenBottom = screenTop + getSDLContext().screenHeight
//
//        val startX = (screenLeft / tileWidth).toInt().coerceAtLeast(0)
//        val endX = (screenRight / tileWidth).toInt().coerceAtMost(layer.width!! - 1)
//        val startY = (screenTop / tileHeight).toInt().coerceAtLeast(0)
//        val endY = (screenBottom / tileHeight).toInt().coerceAtMost(layer.height!! - 1)
//
//        val batch = getSpriteContext().spriteBatch
//        batch.begin()
//
//        for (y in startY..endY) {
//            for (x in startX..endX) {
//                val rawGid = layer.getTileAt(x, y)
//                if (rawGid == 0u) continue  // empty tile
//
//                val decoded = decodeTileGid(rawGid)
//                if (decoded.tileId <= 0u) continue
//
//                // find the tileset and tile position in the sheet
//                val tilesetWithSprite = findTilesetForGid(decoded.tileId)
//                val (tilePx, tilePy) = getTilePosition(decoded.tileId, tilesetWithSprite.tileset)
//
//                // get a tile view directly (no full Sprite object)
//                val tileView = tilesetWithSprite.spriteSheet.getTileView(tilePx.toInt(), tilePy.toInt())
//
//                // compute flipping/rotation if needed
////                val flipMode = when {
////                    decoded.flipH && decoded.flipV -> FlipMode.BOTH
////                    decoded.flipH -> FlipMode.HORIZONTAL
////                    decoded.flipV -> FlipMode.VERTICAL
////                    else -> FlipMode.NONE
////                }
////
////                val angle = if (decoded.flipD) 90.0 else 0.0
//
//                val angle: Double
//                val flipMode: FlipMode
//                when {
//                    // no flags
//                    !decoded.flipH && !decoded.flipV && !decoded.flipD -> {
//                        angle = 0.0
//                        flipMode = FlipMode.NONE
//                    }
//                    // single flags
//                    decoded.flipH && !decoded.flipV && !decoded.flipD -> { // H
//                        angle = 0.0
//                        flipMode = FlipMode.HORIZONTAL
//                    }
//
//                    !decoded.flipH && decoded.flipV && !decoded.flipD -> { // V
//                        angle = 0.0
//                        flipMode = FlipMode.VERTICAL
//                    }
//
//                    !decoded.flipH && !decoded.flipV && decoded.flipD -> { // D
//                        angle = 90.0
//                        flipMode = FlipMode.VERTICAL
//                    }
//                    // double flags
//                    decoded.flipH && !decoded.flipV && decoded.flipD -> { // HD
//                        angle = 90.0
//                        flipMode = FlipMode.NONE
//                    }
//
//                    decoded.flipH && decoded.flipV && !decoded.flipD -> { // HV
//                        angle = 0.0
//                        flipMode = FlipMode.BOTH
//                    }
//
//                    !decoded.flipH && decoded.flipV && decoded.flipD -> { // VD
//                        angle = 270.0
//                        flipMode = FlipMode.NONE
//                    }
//                    // triple flags
//                    decoded.flipH && decoded.flipV && decoded.flipD -> {
//                        angle = 90.0
//                        flipMode = FlipMode.HORIZONTAL
//                    }
//
//                    else -> {
//                        angle = 0.0
//                        flipMode = FlipMode.NONE
//                    }
//                }
//
//                // tileView.clip holds the sub-rect of the texture
//                // tileView.texture is the shared texture
//                val srcX = tileView.clip!!.x
//                val srcY = tileView.clip.y
//                val srcW = tileView.clip.w
//                val srcH = tileView.clip.h
//
//                val drawX = (x * tileWidth + p.x).toInt()
//                val drawY = (y * tileHeight + p.y).toInt()
//
//                batch.draw(
//                    texture = tileView.texture.texture,
//                    srcX = srcX, srcY = srcY, srcW = srcW, srcH = srcH,
//                    dstX = drawX, dstY = drawY,
//                    dstW = (srcW * tileView.scale.x).toInt(),
//                    dstH = (srcH * tileView.scale.y).toInt(),
//                    angle = angle,
//                    flip = flipMode
//                )
//
//            }
//        }
//
//        batch.end()
//    }
//
//    private fun findTilesetForGid(gid: UInt): TilesetAndSpriteSheet {
//        val tileset = tilesetsWithSprites
//            .filter { gid >= it.tileset.firstgid }
//            .maxByOrNull { it.tileset.firstgid }
//            ?: throw IllegalStateException("No tileset found for gid: $gid")
//
//        val lastGidInTileset = tileset.tileset.firstgid + tileset.tileset.tileCount!!.toUInt() - 1u
//        if (gid > lastGidInTileset) {
//            logger.warn {
//                "GID $gid is outside of tileset '${tileset.tileset.name}' range [${tileset.tileset.firstgid}, $lastGidInTileset]. " +
//                        "This may cause rendering issues."
//            }
//        }
//        return tileset
//    }
//
//    private fun getTilePosition(tileId: UInt, tileset: Tileset): Pair<UInt, UInt> {
//        val localId = tileId - tileset.firstgid
//        val cols = tileset.columns ?: throw IllegalArgumentException("Tileset '${tileset.name}' must have defined columns.")
//
//        val gridX = localId % cols.toUInt()
//        val gridY = localId / cols.toUInt()
//
////        if (logger.isTraceEnabled()) {
////            logger.trace {
////                "getTilePosition(tileId=$tileId): firstgid=${tileset.firstgid}, localId=$localId, cols=$cols, grid=($gridX,$gridY)"
////            }
////        }
//
//        return Pair(gridX, gridY)
//    }
//
//    private fun decodeTileGid(gid: UInt): DecodedTile {
//        val flipH = (gid and GID_HORIZONTAL_FLAG) != 0u
//        val flipV = (gid and GID_VERTICAL_FLAG) != 0u
//        val flipD = (gid and GID_DIAGONAL_FLAG) != 0u
//        val tileId = gid and TILE_INDEX_MASK
//
////        if (logger.isTraceEnabled()) {
////            logger.traceStream {
////                writeLn("Decoding GID: ${gid.toString(2).padStart(32, '0')} (${gid})")
////                writeLn("H FLAG:       ${GID_HORIZONTAL_FLAG.toString(2).padStart(32, '0')}")
////                writeLn("V FLAG:       ${GID_VERTICAL_FLAG.toString(2).padStart(32, '0')}")
////                writeLn("D FLAG:       ${GID_DIAGONAL_FLAG.toString(2).padStart(32, '0')}")
////            }
////        }
//
//        return DecodedTile(tileId, flipH, flipV, flipD)
//    }
//
//    data class DecodedTile(
//        val tileId: UInt,
//        val flipH: Boolean,
//        val flipV: Boolean,
//        val flipD: Boolean
//    )
//
//    override fun toString(): String {
//        return "TiledMap(\n" +
//                "width=$width, height=$height,\n\t" +
//                "tileWidth=$tileWidth, tileHeight=$tileHeight,\n\t" +
//                "layers=\n\t\t${layers.joinToString("\n\t\t")},\n\t" +
//                "tilesets=\n\t\t${tilesets.joinToString("\n\t\t")},\n\t" +
//                "orientation='$orientation',\n\t" +
//                "renderOrder='$renderOrder',\n\t" +
//                "infinite=$infinite\n" +
//                ")"
//    }
//}
