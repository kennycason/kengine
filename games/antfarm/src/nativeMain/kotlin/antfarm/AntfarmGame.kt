package antfarm

import com.kengine.Game
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.timeSinceMs

/**
 * Antfarm Simulator Game
 * 
 * A pheromone-based ant colony simulation with emergent behavior.
 * 
 * TIMING SYSTEM DOCUMENTATION:
 * ============================
 * Uses ABSOLUTE timestamps (from getClockContext().totalTimeMs) and timeSinceMs() helper.
 * Pattern learned from HextrisGame: Always store absolute time, never store durations.
 */
class AntfarmGame : Game, Logging {
    private val world: World
    private val antColony: AntColony
    
    // Timing - absolute timestamps
    private var lastUpdateTime = 0L
    private var lastPlantGrowthTime = 0L
    private var lastPheromoneDecayTime = 0L
    
    // Constants
    private val plantGrowthInterval = 2000L // 2 seconds
    private val pheromoneDecayInterval = 100L // 100ms
    
    init {
        logger.info("Initializing Antfarm Simulator...")
        
        // Initialize world and colony (queen deeper underground)
        world = World(width = 120, height = 80, tileSize = 10.0)
        antColony = AntColony(world, queenX = 60, queenY = 50)
        
        // Initialize timestamps
        val currentTime = getClockContext().totalTimeMs
        lastUpdateTime = currentTime
        lastPlantGrowthTime = currentTime
        lastPheromoneDecayTime = currentTime
        
        logger.info("Antfarm initialized with ${world.width}x${world.height} tiles")
    }
    
    override fun update() {
        val currentTime = getClockContext().totalTimeMs
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0 // Convert to seconds
        lastUpdateTime = currentTime
        
        // Handle input
        handleInput()
        
        // Update pheromone decay
        if (timeSinceMs(lastPheromoneDecayTime) > pheromoneDecayInterval) {
            world.decayPheromones()
            lastPheromoneDecayTime = currentTime
        }
        
        // Update plant growth
        if (timeSinceMs(lastPlantGrowthTime) > plantGrowthInterval) {
            world.growPlants()
            lastPlantGrowthTime = currentTime
        }
        
        // Update ant colony
        antColony.update(deltaTime)
    }
    
    override fun draw() {
        useSDLContext {
            // Sky blue background instead of black
            fillScreen(Color(135u, 206u, 235u, 255u)) // Sky blue
            
            useGeometryContext {
                // Draw world
                world.draw()
                
                // Draw ants
                antColony.draw()
                
                // Draw HUD at bottom
                drawHUD()
            }
            
            flipScreen()
        }
    }
    
    private fun drawHUD() {
        useGeometryContext {
            val screenWidth = 1200.0
            val screenHeight = 800.0
            val barY = screenHeight - 10.0 // 10px from bottom
            val barHeight = 4.0 // Very short lines
            val maxBarWidth = 400.0
            
            // Red bar: Ants (left side)
            val antWidth = (antColony.getAntCount() * 8.0).coerceIn(0.0, maxBarWidth)
            fillRectangle(10.0, barY, antWidth, barHeight, Color.red)
            
            // Green bar: Food (middle)
            val foodWidth = (antColony.getFoodStored() * 4.0).coerceIn(0.0, maxBarWidth)
            fillRectangle(screenWidth / 2 - foodWidth / 2, barY, foodWidth, barHeight, Color.green)
            
            // Blue bar: Plants stored (right side)
            val plantWidth = (antColony.getPlantsStored() * 4.0).coerceIn(0.0, maxBarWidth)
            fillRectangle(screenWidth - plantWidth - 10.0, barY, plantWidth, barHeight, Color.cyan)
        }
    }
    
    private fun handleInput() {
        useKeyboardContext {
            if (keyboard.isRPressed()) {
                // Reset simulation
                reset()
            }
        }
    }
    
    private fun reset() {
        val currentTime = getClockContext().totalTimeMs
        lastUpdateTime = currentTime
        lastPlantGrowthTime = currentTime
        lastPheromoneDecayTime = currentTime
        
        // Reset world first (recreates tiles and vegetation)
        world.reset()
        // Then reset colony (creates tunnel, spawns ants)
        antColony.reset()
        
        logger.info("Simulation reset - world regenerated")
    }
    
    override fun cleanup() {
        logger.info("Cleaning up Antfarm Simulator")
    }
}

