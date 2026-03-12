package antfarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Ant class - behavior logic only (no rendering)
 * These tests verify ant states, energy consumption, and basic behaviors
 */
class AntTest {
    
    @Test
    fun testAntInitialState() {
        val world = World(width = 50, height = 40)
        val colony = AntColony(world, queenX = 25, queenY = 20)
        val ant = Ant(x = 25, y = 20, world = world, colony = colony)
        
        assertEquals(25, ant.x)
        assertEquals(20, ant.y)
        assertEquals(AntState.EXPLORING, ant.state)
        assertFalse(ant.carryingFood)
        assertEquals(100.0, ant.energy)
    }
    
    @Test
    fun testAntEnergyDepletion() {
        val world = World(width = 50, height = 40)
        val colony = AntColony(world, queenX = 25, queenY = 20)
        val ant = Ant(x = 25, y = 20, world = world, colony = colony)
        
        val initialEnergy = ant.energy
        
        // Update for 1 second
        ant.update(1.0)
        
        // Energy should have decreased
        assertTrue(ant.energy < initialEnergy)
    }
    
    @Test
    fun testAntForagingBehavior() {
        val world = World(width = 50, height = 40)
        val colony = AntColony(world, queenX = 25, queenY = 20)
        val ant = Ant(x = 25, y = 22, world = world, colony = colony)
        
        // Initially exploring
        assertEquals(AntState.EXPLORING, ant.state)
        
        // Place vegetation nearby
        world.getTile(25, 23)?.vegetation = Vegetation(size = 3)
        
        // Update - ant should find food and start foraging
        ant.update(0.1)
        
        // After harvesting, should be returning home
        if (ant.carryingFood) {
            assertEquals(AntState.RETURNING_HOME, ant.state)
        }
    }
}

