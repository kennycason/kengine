package com.kengine.map.tiled

import com.kengine.test.expectThat
import kotlin.test.Test

class TiledMapLoaderTest {
    enum class Tiles(val id: Int) {
        EMPTY(0),
        BRICK(1),
        BOX(2),
        BOX_SET(3),
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
    fun `load ninja turdle map`() {
        val tiledMap = TiledMapLoader()
            .loadMap("src/nativeTest/resources/ninja_turdle_stomach_0.tmj")
        expectThat(tiledMap.tileWidth).isEqualTo(16)
        expectThat(tiledMap.tileHeight).isEqualTo(16)
        expectThat(tiledMap.width).isEqualTo(100)
        expectThat(tiledMap.height).isEqualTo(17)

        val layersByName = tiledMap.layers.associateBy { it.name }
        expectThat(layersByName).containsKey("object")
        expectThat(layersByName).containsKey("fg")
        expectThat(layersByName).containsKey("main")
        expectThat(layersByName).containsKey("bg")
        expectThat(layersByName).containsKey("parallax")

        val mainLayer = layersByName["main"]!!
       // expectThat(mainLayer.getTileAt(0, 0)).isEqualTo(Tiles.BRICK.id)

    }
}