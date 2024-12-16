package com.kengine.map.tiled

import com.kengine.log.Logging
import com.kengine.test.expectThat
import kotlin.test.Test

class TiledMapLoaderTest : Logging {
    enum class Tiles(val id: UInt) {
        EMPTY(0u),
        BRICK(1u),
        BOX(2u),
        BOX_SET(3u),
    }

    @Test
    fun `load map test`() {
        val tiledMap = TiledMapLoader()
            .loadMap("src/nativeTest/resources/simple_map.tmj")
        expectThat(tiledMap.tileWidth).isEqualTo(32)
        expectThat(tiledMap.tileHeight).isEqualTo(32)
        expectThat(tiledMap.width).isEqualTo(4)
        expectThat(tiledMap.height).isEqualTo(4)

        val layersByName = tiledMap.layers.associateBy { it.name }
        expectThat(layersByName).containsKey("main")
        expectThat(layersByName).containsKey("bg")

        val mainLayer = layersByName["main"]!!
        expectThat(mainLayer.getTileAt(0, 0)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(1, 0)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(2, 0)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(3, 0)).isEqualTo(Tiles.BRICK.id)

        expectThat(mainLayer.getTileAt(0, 1)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(1, 1)).isEqualTo(Tiles.BOX.id)
        expectThat(mainLayer.getTileAt(2, 1)).isEqualTo(Tiles.EMPTY.id)
        expectThat(mainLayer.getTileAt(3, 1)).isEqualTo(Tiles.BRICK.id)

        expectThat(mainLayer.getTileAt(0, 2)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(1, 2)).isEqualTo(Tiles.BOX_SET.id)
        expectThat(mainLayer.getTileAt(2, 2)).isEqualTo(Tiles.EMPTY.id)
        expectThat(mainLayer.getTileAt(3, 2)).isEqualTo(Tiles.BRICK.id)

        expectThat(mainLayer.getTileAt(0, 3)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(1, 3)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(2, 3)).isEqualTo(Tiles.BRICK.id)
        expectThat(mainLayer.getTileAt(3, 3)).isEqualTo(Tiles.BRICK.id)
    }

    @Test
    fun `load ninja turdle all-tiles map`() {
        val tiledMap = TiledMapLoader()
            .loadMap("src/nativeTest/resources/ninja_turdle_all_tiles.tmj")

        // validate map properties
        expectThat(tiledMap) {
            property(TiledMap::tileWidth).isEqualTo(16)
            property(TiledMap::tileHeight).isEqualTo(16)
            property(TiledMap::width).isEqualTo(20)
            property(TiledMap::height).isEqualTo(47)
        }

        // validate layers exist
        val mainLayer = tiledMap.layers.first()
        expectThat(mainLayer.name).isEqualTo("main")

        // validate main layer properties
        expectThat(mainLayer) {
            property(TiledMapLayer::width).isEqualTo(100)
            property(TiledMapLayer::height).isEqualTo(17)
            satisfiesAll({ it.getTileAt(0, 0) == 1u }) // Check the first tile ID
        }

        // log ahead of asserts for easy visualization in case of failure
        logger.infoStream {
            (0 until tiledMap.height).forEach { row ->
                (0 until tiledMap.width).forEach { column ->
                    val tileId = (row * tiledMap.width) + column + 1
                    write(tileId).write(",")
                }
                ln()
            }
        }

        (0 until tiledMap.height).forEach { row ->
            (0 until tiledMap.width).forEach { column ->
                val tileId = ((row * tiledMap.width) + column + 1).toUInt()
                expectThat(mainLayer.getTileAt(column, row)).isEqualTo(tileId)
            }
        }
    }

    @Test
    fun `load ninja turdle map`() {
        val tiledMap = TiledMapLoader()
            .loadMap("src/nativeTest/resources/ninja_turdle_stomach_0.tmj")

        // Validate map properties
        expectThat(tiledMap) {
            property(TiledMap::tileWidth).isEqualTo(16)
            property(TiledMap::tileHeight).isEqualTo(16)
            property(TiledMap::width).isEqualTo(100)
            property(TiledMap::height).isEqualTo(17)
        }

        // Validate layers exist
        val layersByName = tiledMap.layers.associateBy { it.name }
        expectThat(layersByName.keys).containsAll("object", "fg", "main", "bg", "parallax")

        // Validate main layer properties
        val mainLayer = layersByName["main"]!!
        expectThat(mainLayer) {
            property(TiledMapLayer::width).isEqualTo(100)
            property(TiledMapLayer::height).isEqualTo(17)
            satisfiesAll({ it.getTileAt(0, 0) == 1u }) // Check the first tile ID

            expectThat(mainLayer.getTileAt(6, 8)).isEqualTo(203u)
            expectThat(mainLayer.getTileAt(6, 9)).isEqualTo(224u)
            expectThat(mainLayer.getTileAt(14, 0)).isEqualTo(203u)
        }

        // Validate object layer
        val objectLayer = layersByName["object"]!!
        expectThat(objectLayer.objects).isNotEmpty()
        val doorObject = objectLayer.objects!!.find { it.name == "out_1" }!!
        expectThat(doorObject) {
            property(TiledObject::type).isEqualTo("door")
            property(TiledObject::x).isEqualTo(-2.0)
            property(TiledObject::y).isEqualTo(61.0)
            property(TiledObject::width).isEqualTo(11.0)
            property(TiledObject::height).isEqualTo(53.0)
            val doorObjectProperties = doorObject.properties!!.associateBy { it.name }
            expectThat(doorObjectProperties).containsKey("map")
            expectThat(doorObjectProperties["map"]!!.value).isEqualTo("stomach_14")
            expectThat(doorObjectProperties).containsKey("to")
            expectThat(doorObjectProperties["to"]!!.value).isEqualTo("in_1")
        }

        // Validate tileset
        expectThat(tiledMap.tilesets).hasSize(1)
        val tileset = tiledMap.tilesets.first()
        expectThat(tileset.isExternal()).isTrue()
        expectThat(tileset) {
            property(Tileset::source).isEqualTo("tiles.tsj")
            property(Tileset::name).isEqualTo("tiles")
            property(Tileset::image).isEqualTo("tiles_padded.png")
            property(Tileset::imageWidth).isEqualTo(360)
            property(Tileset::imageHeight).isEqualTo(846)
            property(Tileset::tileWidth).isEqualTo(16)
            property(Tileset::tileHeight).isEqualTo(16)
            property(Tileset::tileCount).isEqualTo(940)
            property(Tileset::columns).isEqualTo(20)
        }

        // Validate tileset animation
        val animatedTile = tileset.tiles!!.find { it.id == 29 }!!
        expectThat(animatedTile.animation).isNotEmpty()
        expectThat(animatedTile.animation!!) {
            hasSize(2)
            satisfiesAll(
                { it[0].duration == 300 },
                { it[0].tileid == 29 },
                { it[1].tileid == 30 }
            )
        }
    }
}