package antfarm

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless simulation runner for testing and debugging ant behavior
 */
class SimulationRunner {
    
    @Test
    fun testAntColonySimulation() {
        println("\n=== Starting Headless Ant Colony Simulation ===\n")
        
        val world = World(width = 120, height = 80, tileSize = 10.0)
        val colony = AntColony(world, queenX = 60, queenY = 50)
        
        // Run simulation for 10 seconds (600 ticks at ~60fps)
        val simulationTicks = 600
        val deltaTime = 1.0 / 60.0 // 60 FPS
        
        for (tick in 0 until simulationTicks) {
            // Update colony
            colony.update(deltaTime)
            
            // Decay pheromones every 100ms equivalent
            if (tick % 6 == 0) {
                world.decayPheromones()
            }
            
            // Grow plants every 5 seconds
            if (tick % 300 == 0) {
                world.growPlants()
            }
            
            // Log statistics every second
            if (tick % 60 == 0) {
                logStatistics(tick / 60, world, colony)
            }
        }
        
        println("\n=== Simulation Complete ===\n")
        
        // Final analysis
        analyzeWorld(world, colony)
        
        // Assertions
        assertTrue(colony.getAntCount() > 0, "Colony should have ants")
        println("\nAll tests passed!")
    }
    
    private fun logStatistics(second: Int, world: World, colony: AntColony) {
        val ants = colony.ants
        
        // Count ant states
        val stateCount = mutableMapOf<AntState, Int>()
        for (state in AntState.values()) {
            stateCount[state] = 0
        }
        for (ant in ants) {
            stateCount[ant.state] = (stateCount[ant.state] ?: 0) + 1
        }
        
        // Count ants at different depths
        var surface = 0
        var shallow = 0
        var deep = 0
        for (ant in ants) {
            when {
                ant.y <= 15 -> surface++
                ant.y <= 30 -> shallow++
                else -> deep++
            }
        }
        
        println("[$second s] Ants: ${ants.size} | Food: ${colony.getFoodStored()} | Plants: ${colony.getPlantsStored()}")
        println("  States: EXPLORING=${stateCount[AntState.EXPLORING]} FORAGING=${stateCount[AntState.FORAGING]} RETURNING=${stateCount[AntState.RETURNING_HOME]} RESTING=${stateCount[AntState.RESTING]} CARRYING_DIRT=${stateCount[AntState.CARRYING_DIRT]}")
        println("  Depth: Surface=$surface Shallow=$shallow Deep=$deep")
    }
    
    private fun analyzeWorld(world: World, colony: AntColony) {
        println("\n=== World Analysis ===")
        
        // Count tunnel tiles (AIR underground)
        var tunnelTiles = 0
        var dirtPiles = 0
        var vegetationCount = 0
        
        for (y in 16 until world.height) { // Underground only
            for (x in 0 until world.width) {
                val tile = world.getTile(x, y)
                if (tile != null) {
                    when (tile.type) {
                        TileType.AIR -> tunnelTiles++
                        TileType.DIRT_PILE -> dirtPiles++
                        else -> {}
                    }
                }
            }
        }
        
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (world.getTile(x, y)?.vegetation != null) {
                    vegetationCount++
                }
            }
        }
        
        println("Underground tunnel tiles: $tunnelTiles")
        println("Dirt piles: $dirtPiles")
        println("Vegetation remaining: $vegetationCount")
        println("Colony stats:")
        println("  Ants: ${colony.getAntCount()}")
        println("  Food available: ${colony.getFoodStored()}")
        println("  Plants stored: ${colony.getPlantsStored()}")
        
        // Analyze tunnel spread
        val tunnelsByRow = mutableMapOf<Int, Int>()
        for (y in 16 until world.height) {
            var count = 0
            for (x in 0 until world.width) {
                if (world.getTile(x, y)?.type == TileType.AIR) {
                    count++
                }
            }
            if (count > 0) {
                tunnelsByRow[y] = count
            }
        }
        
        println("\nTunnel distribution by depth:")
        val sortedRows = tunnelsByRow.keys.sorted()
        for (y in sortedRows) {
            val count = tunnelsByRow[y] ?: 0
            val barLength = (count / 5).coerceAtMost(40)
            val bar = "█".repeat(barLength)
            println("  Row $y: $count tiles $bar")
        }
        
        // Check for side tunnels (not just vertical)
        var hasHorizontalTunnels = false
        for (y in 20 until world.height) {
            var consecutiveAir = 0
            for (x in 0 until world.width) {
                if (world.getTile(x, y)?.type == TileType.AIR) {
                    consecutiveAir++
                    if (consecutiveAir >= 5) {
                        hasHorizontalTunnels = true
                        break
                    }
                } else {
                    consecutiveAir = 0
                }
            }
            if (hasHorizontalTunnels) break
        }
        
        println("\nHas horizontal tunnels: $hasHorizontalTunnels")
        
        // Analyze pheromone strength
        var maxHomePheromone = 0.0
        var maxFoodPheromone = 0.0
        var maxExplorePheromone = 0.0
        
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val tile = world.getTile(x, y)
                if (tile != null) {
                    maxHomePheromone = maxOf(maxHomePheromone, tile.pheromones.get(PheromoneType.HOME))
                    maxFoodPheromone = maxOf(maxFoodPheromone, tile.pheromones.get(PheromoneType.FOOD))
                    maxExplorePheromone = maxOf(maxExplorePheromone, tile.pheromones.get(PheromoneType.EXPLORE))
                }
            }
        }
        
        println("\nPheromone peaks:")
        println("  HOME: $maxHomePheromone")
        println("  FOOD: $maxFoodPheromone")
        println("  EXPLORE: $maxExplorePheromone")
    }
}

