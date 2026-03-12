package antfarm

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.log.Logging
import com.kengine.time.getClockContext
import com.kengine.time.timeSinceMs

/**
 * The ant colony with queen and worker ants
 */
class AntColony(
    val world: World,
    val queenX: Int,
    val queenY: Int
) : Logging {
    
    val ants = mutableListOf<Ant>()
    private var plantsStored = 0  // Raw vegetation stored
    private var foodAvailable = 0 // Processed food for ants to eat
    private var queenHunger = 0.0 // Queen's hunger level (0-100)
    private var lastReproductionTime = 0L
    private var lastFoodProductionTime = 0L
    private val reproductionInterval = 5000L // 5 seconds
    private val foodProductionInterval = 1000L // 1 second - plants produce food
    private val maxAnts = 50
    
    init {
        // Create initial tunnel and minimal chamber
        createQueenChamber()
        
        // Spawn initial worker ants with full energy and some starting food
        plantsStored = 10 // Start with some food
        foodAvailable = 20
        for (i in 0 until 5) {
            spawnAnt()
        }
        
        lastReproductionTime = getClockContext().totalTimeMs
        lastFoodProductionTime = getClockContext().totalTimeMs
        logger.info("Colony established at ($queenX, $queenY) with tunnel from surface")
    }
    
    private fun createQueenChamber() {
        val surfaceY = 15 // Must match World.surfaceY
        
        // Force open a 1-tile wide vertical tunnel (don't use dig(), it blocks at surface)
        for (y in surfaceY..queenY) {
            world.getTile(queenX, y)?.type = TileType.AIR
        }
        
        // Small 3x3 starting space around queen
        for (dy in -1..1) {
            for (dx in -1..1) {
                world.getTile(queenX + dx, queenY + dy)?.type = TileType.AIR
            }
        }
        
        logger.info("Created tunnel from surface (y=$surfaceY) to queen (y=$queenY) at x=$queenX")
    }
    
    fun update(deltaTime: Double) {
        // Update all ants
        ants.forEach { ant ->
            ant.update(deltaTime)
        }
        
        // Remove dead ants and log
        val deadAnts = ants.filter { it.energy <= 0 }
        if (deadAnts.isNotEmpty()) {
            logger.info("${deadAnts.size} ant(s) died from energy depletion. Colony size: ${ants.size - deadAnts.size}")
        }
        ants.removeAll { it.energy <= 0 }
        
        // Food production: stored plants generate food over time
        if (timeSinceMs(lastFoodProductionTime) > foodProductionInterval) {
            if (plantsStored > 0) {
                // Each stored plant produces 0.1 food per second
                val foodProduced = (plantsStored * 0.1).toInt()
                if (foodProduced > 0) {
                    foodAvailable += foodProduced
                    logger.debug("Plants produced $foodProduced food. Available: $foodAvailable")
                }
            }
            lastFoodProductionTime = getClockContext().totalTimeMs
        }
        
        // Check for reproduction
        if (timeSinceMs(lastReproductionTime) > reproductionInterval) {
            if (foodAvailable >= 10 && ants.size < maxAnts) {
                foodAvailable -= 10
                spawnAnt()
                logger.info("New ant born! Colony size: ${ants.size}, Food: $foodAvailable, Plants: $plantsStored")
            } else if (ants.size < maxAnts) {
                logger.debug("Cannot reproduce: need 10 food (have $foodAvailable, plants: $plantsStored)")
            }
            lastReproductionTime = getClockContext().totalTimeMs
        }
        
        // Queen hunger increases over time
        queenHunger += deltaTime * 0.5
        queenHunger = queenHunger.coerceIn(0.0, 100.0)
        
        // Queen eats from food storage
        if (foodAvailable > 0 && queenHunger > 20) {
            val eaten = minOf(foodAvailable, 5)
            foodAvailable -= eaten
            queenHunger = maxOf(0.0, queenHunger - eaten * 10.0)
        }
        
        // Drop STRONG home pheromones around queen (VERY persistent trail)
        for (dy in -6..6) {
            for (dx in -6..6) {
                world.addPheromone(queenX + dx, queenY + dy, PheromoneType.HOME, 10.0)
            }
        }
        
        // If queen is hungry, emit hunger pheromones
        if (queenHunger > 30) {
            for (dy in -6..6) {
                for (dx in -6..6) {
                    world.addPheromone(queenX + dx, queenY + dy, PheromoneType.HUNGER, queenHunger / 10.0)
                }
            }
        }
    }
    
    fun draw() {
        useGeometryContext {
            // Draw queen as giant ant (2x size of regular ants)
            val queenPosX = queenX * world.tileSize + world.tileSize / 2
            val queenPosY = queenY * world.tileSize + world.tileSize / 2
            val queenSize = world.tileSize * 1.2
            val queenColor = Color(200u, 150u, 0u, 255u) // Golden color
            
            // Queen body (main circle - larger)
            fillCircle(queenPosX, queenPosY, (queenSize * 0.7).toInt(), queenColor)
            
            // Queen head (front)
            fillCircle(queenPosX - queenSize * 0.7, queenPosY - queenSize * 0.4, (queenSize * 0.45).toInt(), queenColor)
            
            // Queen abdomen (back - TWO circles for elongated look)
            fillCircle(queenPosX + queenSize * 0.7, queenPosY + queenSize * 0.3, (queenSize * 0.5).toInt(), queenColor)
            fillCircle(queenPosX + queenSize * 1.2, queenPosY + queenSize * 0.5, (queenSize * 0.4).toInt(), queenColor)
            
            // Draw all worker ants
            ants.forEach { it.draw() }
            
            // Draw food counter
            drawFoodCounter()
        }
    }
    
    private fun drawFoodCounter() {
        useGeometryContext {
            // Simple visual indicator - will be improved later
            val counterX = 10.0
            val counterY = 10.0
            val width = 100.0
            val height = 20.0
            
            // Background
            fillRectangle(counterX, counterY, width, height, Color(0u, 0u, 0u, 200u))
            
            // Food bar (available food)
            val foodBarWidth = (foodAvailable.toDouble() / 10.0 * width).coerceIn(0.0, width)
            fillRectangle(counterX, counterY, foodBarWidth, height, Color.green)
        }
    }
    
    private fun spawnAnt() {
        // Spawn near the queen
        val ant = Ant(
            x = queenX + (-2..2).random(),
            y = queenY + (-2..2).random(),
            world = world,
            colony = this
        )
        ants.add(ant)
    }
    
    fun addFood(amount: Int) {
        plantsStored += amount // Add harvested plants to storage
    }
    
    fun consumeFood(amount: Int): Boolean {
        if (foodAvailable >= amount) {
            foodAvailable -= amount
            return true
        }
        return false
    }
    
    fun getAntCount(): Int = ants.size
    
    fun getFoodStored(): Int = foodAvailable // Show available food in HUD
    
    fun getPlantsStored(): Int = plantsStored
    
    fun reset() {
        ants.clear()
        plantsStored = 0
        foodAvailable = 0
        
        // Create tunnel first, BEFORE respawning ants
        createQueenChamber()
        
        // Respawn initial ants
        for (i in 0 until 5) {
            spawnAnt()
        }
        
        lastReproductionTime = getClockContext().totalTimeMs
        lastFoodProductionTime = getClockContext().totalTimeMs
    }
}

