package antfarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for World class - core logic only (no rendering)
 * These tests verify terrain generation, digging, vegetation, and pheromones
 */
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
        
        // Valid tile
        val tile = world.getTile(25, 20)
        assertNotNull(tile)
        assertEquals(25, tile.x)
        assertEquals(20, tile.y)
        
        // Out of bounds
        assertNull(world.getTile(-1, 20))
        assertNull(world.getTile(25, -1))
        assertNull(world.getTile(50, 20))
        assertNull(world.getTile(25, 40))
    }
    
    @Test
    fun testTerrainLayers() {
        val world = World(width = 50, height = 40)
        
        // Top rows should be AIR (for vegetation)
        val airTile = world.getTile(25, 2)
        assertNotNull(airTile)
        assertEquals(TileType.AIR, airTile.type)
        
        // Middle rows should be GROUND
        val groundTile = world.getTile(25, 6)
        assertNotNull(groundTile)
        assertEquals(TileType.GROUND, groundTile.type)
        
        // Deep rows should be DIRT
        val dirtTile = world.getTile(25, 20)
        assertNotNull(dirtTile)
        assertEquals(TileType.DIRT, dirtTile.type)
    }
    
    @Test
    fun testDigging() {
        val world = World(width = 50, height = 40)
        
        // Find a dirt tile
        val dirtTile = world.getTile(25, 20)
        assertNotNull(dirtTile)
        assertEquals(TileType.DIRT, dirtTile.type)
        
        // Dig it
        assertTrue(world.dig(25, 20))
        
        // Should now be AIR
        assertEquals(TileType.AIR, dirtTile.type)
        
        // Can't dig AIR
        assertFalse(world.dig(25, 20))
    }
    
    @Test
    fun testCanDigThrough() {
        val world = World(width = 50, height = 40)
        
        // DIRT can be dug
        assertTrue(world.canDigThrough(25, 20))
        
        // GROUND can be dug
        assertTrue(world.canDigThrough(25, 6))
        
        // AIR cannot be dug (already empty)
        world.dig(25, 20)
        assertFalse(world.canDigThrough(25, 20))
    }
    
    @Test
    fun testCanWalkThrough() {
        val world = World(width = 50, height = 40)
        
        // AIR can be walked through
        val airTile = world.getTile(25, 2)
        assertNotNull(airTile)
        assertTrue(world.canWalkThrough(25, 2))
        
        // DIRT cannot be walked through (until dug)
        assertFalse(world.canWalkThrough(25, 20))
        
        // After digging, can walk through
        world.dig(25, 20)
        assertTrue(world.canWalkThrough(25, 20))
    }
    
    @Test
    fun testVegetationHarvesting() {
        val world = World(width = 50, height = 40)
        
        // Find or create a vegetation tile
        val vegTile = world.getTile(25, 4)
        assertNotNull(vegTile)
        vegTile.vegetation = Vegetation(size = 3)
        
        // Harvest once
        assertTrue(world.harvestVegetation(25, 4))
        assertEquals(2, vegTile.vegetation?.size)
        
        // Harvest again
        assertTrue(world.harvestVegetation(25, 4))
        assertEquals(1, vegTile.vegetation?.size)
        
        // Harvest last piece
        assertTrue(world.harvestVegetation(25, 4))
        assertNull(vegTile.vegetation)
        
        // Can't harvest from empty tile
        assertFalse(world.harvestVegetation(25, 4))
    }
    
    @Test
    fun testPheromoneOperations() {
        val world = World(width = 50, height = 40)
        
        // Add pheromone
        world.addPheromone(25, 20, PheromoneType.FOOD, 5.0)
        assertEquals(5.0, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)
        
        // Add more
        world.addPheromone(25, 20, PheromoneType.FOOD, 2.0)
        assertEquals(7.0, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)
        
        // Decay
        world.decayPheromones()
        // After decay with foodRate=0.02, strength should be 7.0 * (1-0.02) = 6.86
        assertEquals(6.86, world.getPheromone(25, 20, PheromoneType.FOOD), 0.01)
    }
}

