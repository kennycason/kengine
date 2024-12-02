package com.kengine.map.tiled

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(32, tiledMap.tileWidth)
        assertEquals(32, tiledMap.tileHeight)
        assertEquals(4, tiledMap.width)
        assertEquals(4, tiledMap.height)

        val layersByName = tiledMap.layers.associateBy { it.name }
        assertTrue("main" in layersByName)
        assertTrue("bg" in layersByName)

        val mainLayer = layersByName["main"]!!
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(0, 0))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(1, 0))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(2, 0))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(3, 0))

        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(0, 1))
        assertEquals(Tiles.BOX.id, mainLayer.getTileAt(1, 1))
        assertEquals(Tiles.EMPTY.id, mainLayer.getTileAt(2, 1))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(3, 1))

        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(0, 2))
        assertEquals(Tiles.BOX_SET.id, mainLayer.getTileAt(1, 2))
        assertEquals(Tiles.EMPTY.id, mainLayer.getTileAt(2, 2))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(3, 2))

        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(0, 3))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(1, 3))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(2, 3))
        assertEquals(Tiles.BRICK.id, mainLayer.getTileAt(3, 3))
    }
}