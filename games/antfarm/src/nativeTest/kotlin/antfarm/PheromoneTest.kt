package antfarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PheromoneTest {
    
    @Test
    fun testPheromoneAddition() {
        val pheromoneMap = PheromoneMap()
        
        pheromoneMap.add(PheromoneType.FOOD, 1.0)
        assertEquals(1.0, pheromoneMap.get(PheromoneType.FOOD), 0.01)
        
        pheromoneMap.add(PheromoneType.FOOD, 2.0)
        assertEquals(3.0, pheromoneMap.get(PheromoneType.FOOD), 0.01)
    }
    
    @Test
    fun testPheromoneMaxStrength() {
        val pheromoneMap = PheromoneMap()
        
        // Add more than max strength
        pheromoneMap.add(PheromoneType.HOME, 15.0)
        
        // Should be capped at 10.0
        assertEquals(10.0, pheromoneMap.get(PheromoneType.HOME), 0.01)
    }
    
    @Test
    fun testPheromoneDecay() {
        val pheromoneMap = PheromoneMap()
        
        pheromoneMap.add(PheromoneType.FOOD, 5.0)
        assertEquals(5.0, pheromoneMap.get(PheromoneType.FOOD), 0.01)
        
        // Decay by 10%
        pheromoneMap.decay(0.1, 0.1, 0.1)
        assertEquals(4.5, pheromoneMap.get(PheromoneType.FOOD), 0.01)
        
        // Decay again
        pheromoneMap.decay(0.1, 0.1, 0.1)
        assertEquals(4.05, pheromoneMap.get(PheromoneType.FOOD), 0.01)
    }
    
    @Test
    fun testPheromoneRemovalAfterDecay() {
        val pheromoneMap = PheromoneMap()
        
        pheromoneMap.add(PheromoneType.EXPLORE, 0.02)
        
        // Decay should remove pheromones below threshold
        pheromoneMap.decay(0.5, 0.5, 0.5)
        assertEquals(0.0, pheromoneMap.get(PheromoneType.EXPLORE), 0.01)
    }
    
    @Test
    fun testMultiplePheromoneTypes() {
        val pheromoneMap = PheromoneMap()
        
        pheromoneMap.add(PheromoneType.FOOD, 3.0)
        pheromoneMap.add(PheromoneType.HOME, 5.0)
        pheromoneMap.add(PheromoneType.DANGER, 2.0)
        
        assertEquals(3.0, pheromoneMap.get(PheromoneType.FOOD), 0.01)
        assertEquals(5.0, pheromoneMap.get(PheromoneType.HOME), 0.01)
        assertEquals(2.0, pheromoneMap.get(PheromoneType.DANGER), 0.01)
        
        // Decay all
        pheromoneMap.decay(0.2, 0.2, 0.2)
        
        assertEquals(2.4, pheromoneMap.get(PheromoneType.FOOD), 0.01)
        assertEquals(4.0, pheromoneMap.get(PheromoneType.HOME), 0.01)
        assertEquals(1.6, pheromoneMap.get(PheromoneType.DANGER), 0.01)
    }
}

