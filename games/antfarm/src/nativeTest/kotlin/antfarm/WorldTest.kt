package antfarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldTest {

    @Test
    fun testWorldDimensions() {
        val world = World(width = 50, height = 40, tileSize = 10.0)

        assertEquals(50, world.width)
        assertEquals(40, world.height)
        assertEquals(10.0, world.tileSize)
    }

    @Test
    fun testTileAccess() {
        val world = World(width = 50, height = 40)

        val tile = world.getTile(25, 20)
        assertNotNull(tile)
        assertEquals(25, tile.x)
        assertEquals(20, tile.y)

        assertNull(world.getTile(-1, 20))
        assertNull(world.getTile(25, -1))
        assertNull(world.getTile(50, 20))
        assertNull(world.getTile(25, 40))
    }

    @Test
    fun testTerrainLayers() {
        val world = World(width = 50, height = 40)

        // y <= surfaceY (15) should be AIR
        val airTile = world.getTile(25, 2)
        assertNotNull(airTile)
        assertEquals(TileType.AIR, airTile.type)

        // y > surfaceY should be DIRT
        val dirtTile = world.getTile(25, 20)
        assertNotNull(dirtTile)
        assertEquals(TileType.DIRT, dirtTile.type)
    }

    @Test
    fun testDigging() {
        val world = World(width = 50, height = 40)

        val dirtTile = world.getTile(25, 20)
        assertNotNull(dirtTile)
        assertEquals(TileType.DIRT, dirtTile.type)

        // Digging turns DIRT into DIRT_PILE (not AIR)
        assertTrue(world.dig(25, 20))
        assertEquals(TileType.DIRT_PILE, dirtTile.type)

        // Can clear the dirt pile to make it AIR
        assertTrue(world.clearDirtPile(25, 20))
        assertEquals(TileType.AIR, dirtTile.type)
    }

    @Test
    fun testCanDigThrough() {
        val world = World(width = 50, height = 40)

        // DIRT underground can be dug
        assertTrue(world.canDigThrough(25, 20))

        // AIR above surface cannot be dug
        assertFalse(world.canDigThrough(25, 2))

        // After digging, it's a DIRT_PILE, not diggable anymore
        world.dig(25, 20)
        assertFalse(world.canDigThrough(25, 20))
    }

    @Test
    fun testCanWalkThrough() {
        val world = World(width = 50, height = 40)

        // Surface row (surfaceY=15) with AIR is walkable
        assertTrue(world.canWalkThrough(25, 15))

        // DIRT underground cannot be walked through
        assertFalse(world.canWalkThrough(25, 20))

        // After digging + clearing, underground AIR is walkable
        world.dig(25, 20)
        world.clearDirtPile(25, 20)
        assertTrue(world.canWalkThrough(25, 20))
    }

    @Test
    fun testVegetationHarvesting() {
        val world = World(width = 50, height = 40)

        val vegTile = world.getTile(25, 4)
        assertNotNull(vegTile)
        vegTile.vegetation = Vegetation(size = 3)

        assertTrue(world.harvestVegetation(25, 4))
        assertEquals(2, vegTile.vegetation?.size)

        assertTrue(world.harvestVegetation(25, 4))
        assertEquals(1, vegTile.vegetation?.size)

        assertTrue(world.harvestVegetation(25, 4))
        assertNull(vegTile.vegetation)

        assertFalse(world.harvestVegetation(25, 4))
    }

    @Test
    fun testPheromoneOperations() {
        val world = World(width = 50, height = 40)

        world.addPheromone(25, 20, PheromoneType.FOOD, 5.0)
        assertEquals(5.0, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)

        world.addPheromone(25, 20, PheromoneType.FOOD, 2.0)
        // Diminishing returns: 2.0 * (1 - 5.0/100) = 1.9, total = 6.9
        val expected = 5.0 + 2.0 * (1.0 - 5.0 / 100.0)
        assertEquals(expected, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)

        world.decayPheromones()
        // foodRate = 0.015, so expected * (1 - 0.015)
        val afterDecay = expected * (1.0 - 0.015)
        assertEquals(afterDecay, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)
    }
}
