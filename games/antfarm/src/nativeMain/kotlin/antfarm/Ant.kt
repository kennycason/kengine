package antfarm

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.log.Logging
import kotlin.math.abs
import kotlin.random.Random

/**
 * Individual ant entity with pheromone-based behavior
 */
class Ant(
    var x: Int,
    var y: Int,
    val world: World,
    val colony: AntColony
) : Logging {
    
    var state = AntState.EXPLORING
    var carryingFood = false
    var energy = 100.0
    private var stuckCounter = 0
    private var lastLogTime = 0L
    
    // Movement tracking
    private var lastX = x
    private var lastY = y
    private var moveHistory = mutableListOf<Pair<Int, Int>>() // Track recent positions
    private val maxHistorySize = 10
    private var carryingDirt = false
    private var movementBias = if (Random.nextFloat() < 0.5f) -1 else 1 // Random left/right bias
    
    // Resting
    private var restUntilMs = 0L
    
    fun update(deltaTime: Double) {
        // If on a dirt pile anywhere, pick it up and carry out (prevents blocked tunnels)
        val tileHere = world.getTile(x, y)
        if (tileHere != null && tileHere.type == TileType.DIRT_PILE && !carryingDirt) {
            world.clearDirtPile(x, y)
            carryingDirt = true
            state = AntState.CARRYING_DIRT
            return
        }
        
        // Check resting state first
        if (state == AntState.RESTING) {
            if (com.kengine.time.getClockContext().totalTimeMs >= restUntilMs) {
                state = AntState.EXPLORING
            }
            return
        }
        
        // Consume energy based on activity (REDUCED - was too harsh)
        val energyCost = if (carryingFood) 0.15 else 0.1 // Carrying food costs more
        energy -= deltaTime * energyCost
        
        if (energy <= 0) {
            // Ant dies
            logger.info("Ant died at ($x,$y) - energy depleted")
            return
        }
        
        // If low energy and not carrying food, return home to eat
        if (energy < 30 && !carryingFood && state != AntState.RETURNING_HOME) {
            state = AntState.RETURNING_HOME
            logger.debug("Ant at ($x,$y) low on energy, returning home to feed")
        }
        
        // Periodic status logging
        if (com.kengine.time.timeSinceMs(lastLogTime) > 10000) { // Every 10 seconds
            logger.info("Ant at ($x,$y): state=$state, energy=${energy.toInt()}, carrying=$carryingFood")
            lastLogTime = com.kengine.time.getClockContext().totalTimeMs
        }
        
        // Track movement history to detect loops
        moveHistory.add(Pair(x, y))
        if (moveHistory.size > maxHistorySize) {
            moveHistory.removeAt(0)
        }
        
        // Check if stuck in a loop (visiting same positions)
        if (moveHistory.size == maxHistorySize) {
            val uniquePositions = moveHistory.distinct().size
            if (uniquePositions <= 2) {
                // Stuck in a loop, force random movement
                logger.debug("Ant stuck in loop at ($x,$y), forcing random movement")
                moveRandomly()
                moveHistory.clear()
                stuckCounter = 0
            }
        }
        
        // Check if completely stuck
        if (lastX == x && lastY == y) {
            stuckCounter++
            if (stuckCounter > 10) {
                // Force random movement if stuck
                moveRandomly()
                stuckCounter = 0
                moveHistory.clear()
            }
        } else {
            stuckCounter = 0
        }
        
        lastX = x
        lastY = y
        
        // Behavior based on state
        when (state) {
            AntState.EXPLORING -> exploreForFood()
            AntState.RETURNING_HOME -> returnToColony()
            AntState.FORAGING -> forage()
            AntState.CLEARING_DIRT -> clearDirt()
            AntState.CARRYING_DIRT -> carryDirtToSurface()
            AntState.RESTING -> {} // Do nothing while resting
        }
        
        // Drop pheromones
        dropPheromones()
    }
    
    private fun exploreForFood() {
        // Drop HOME pheromones as we explore OUTWARD from colony (stronger trail)
        val distanceFromQueen = abs(x - colony.queenX) + abs(y - colony.queenY)
        if (distanceFromQueen < 40) { // Within reasonable distance
            world.addPheromone(x, y, PheromoneType.HOME, 2.0) // Stronger HOME trail
        }
        
        // Check if standing on dirt pile - pick it up
        if (world.hasDirtPile(x, y) && Random.nextFloat() < 0.5f) {
            carryingDirt = true
            state = AntState.CARRYING_DIRT
            return
        }
        
        // Look for food in adjacent tiles FIRST
        val foodTile = findAdjacentFood()
        if (foodTile != null) {
            state = AntState.FORAGING
            x = foodTile.first
            y = foodTile.second
            return
        }
        
        // Look for food at surface level within range
        val nearbyFood = findNearbyFood(radius = 10)
        if (nearbyFood != null) {
            // Move toward food, digging if necessary
            moveTowardsSurface(nearbyFood.first, nearbyFood.second)
            return
        }
        
        // Follow food pheromones if present (but with randomness to avoid loops)
        if (Random.nextFloat() < 0.7f) { // 70% chance to follow pheromones
            val bestFoodDirection = findStrongestPheromone(PheromoneType.FOOD)
            if (bestFoodDirection != null && !isInRecentHistory(bestFoodDirection)) {
                x = bestFoodDirection.first
                y = bestFoodDirection.second
                return
            }
        }
        
        // Explore behavior: actively explore underground to create tunnel networks
        if (y < 25) {
            // Not deep enough - go deeper!
            if (Random.nextFloat() < 0.6f) { // 60% chance to go down
                // Try to move downward
                val downMoves = listOf(
                    Pair(0, 1),        // Straight down
                    Pair(-1, 1),       // Diagonal down-left
                    Pair(1, 1),        // Diagonal down-right
                    Pair(-1, 0),       // Horizontal
                    Pair(1, 0)
                ).shuffled()
                
                var moved = false
                for ((dx, dy) in downMoves) {
                    val nx = x + dx
                    val ny = y + dy
                    if (world.canWalkThrough(nx, ny)) {
                        x = nx
                        y = ny
                        updateMoveHistory()
                        moved = true
                        break
                    }
                }
                
                // If can't move, dig downward
                if (!moved && Random.nextFloat() < 0.7f) {
                    for ((dx, dy) in downMoves) {
                        val nx = x + dx
                        val ny = y + dy
                        if (world.dig(nx, ny)) {
                            logger.debug("Ant dug downward tunnel at ($nx,$ny)")
                            break
                        }
                    }
                }
            } else {
                moveRandomly()
            }
        } else {
            // Deep enough - ACTIVELY dig and explore horizontally!
            // 40% chance to proactively dig a new tunnel
            if (Random.nextFloat() < 0.4f) {
                val digDirections = listOf(
                    Pair(1, 0), Pair(-1, 0), // Horizontal (priority for side chambers)
                    Pair(1, 1), Pair(-1, 1), // Diagonal down
                    Pair(0, 1), // Down
                    Pair(1, -1), Pair(-1, -1) // Diagonal up
                ).shuffled()
                
                for ((dx, dy) in digDirections) {
                    val nx = x + dx
                    val ny = y + dy
                    // Avoid infinite edge runs: reduce digging if far from shaft or near edges
                    val tooFarFromShaft = kotlin.math.abs(nx - colony.queenX) > 80
                    val nearEdge = nx < 3 || nx > world.width - 4
                    if (tooFarFromShaft || nearEdge) continue
                    if (world.dig(nx, ny)) {
                        // Immediately pick up the dirt so the tunnel becomes open
                        world.pickupDirtPile(nx, ny)
                        carryingDirt = true
                        state = AntState.CARRYING_DIRT
                        logger.debug("Ant proactively dug tunnel at ($nx,$ny) depth=$ny")
                        x = nx
                        y = ny
                        updateMoveHistory()
                        return
                    }
                }
            }
            
            // Otherwise try to move through existing tunnels
            if (!tryMove()) {
                // Really stuck - dig aggressively!
                val digDirections = listOf(
                    Pair(1, 0), Pair(-1, 0), Pair(0, 1),
                    Pair(1, 1), Pair(-1, 1), Pair(1, -1), Pair(-1, -1)
                ).shuffled()
                
                for ((dx, dy) in digDirections) {
                    val nx = x + dx
                    val ny = y + dy
                    val tooFarFromShaft = kotlin.math.abs(nx - colony.queenX) > 80
                    val nearEdge = nx < 3 || nx > world.width - 4
                    if (tooFarFromShaft || nearEdge) continue
                    if (world.dig(nx, ny)) {
                        world.pickupDirtPile(nx, ny)
                        carryingDirt = true
                        state = AntState.CARRYING_DIRT
                        logger.debug("Ant dug escape tunnel at ($nx,$ny)")
                        x = nx
                        y = ny
                        updateMoveHistory()
                        break
                    }
                }
            }
        }
    }
    
    private fun tryMove(): Boolean {
        // Try to move in a random valid direction
        val directions = listOf(
            Pair(movementBias, 0), Pair(-movementBias, 0),
            Pair(0, -1), Pair(0, 1),
            Pair(movementBias, -1), Pair(-movementBias, -1),
            Pair(movementBias, 1), Pair(-movementBias, 1)
        ).shuffled()
        
        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy
            if (world.canWalkThrough(nx, ny) && !isInRecentHistory(Pair(nx, ny))) {
                x = nx
                y = ny
                updateMoveHistory()
                return true
            }
        }
        return false
    }
    
    private fun forage() {
        // Try to harvest WHOLE plant before moving on
        val vegetation = world.getTile(x, y)?.vegetation
        if (vegetation != null && vegetation.size > 0) {
            // Eat the whole plant bit by bit
            if (world.harvestVegetation(x, y, amount = 5)) { // Take 5 units at a time
                carryingFood = true
                // Keep foraging same spot until plant is gone
                val remaining = world.getTile(x, y)?.vegetation?.size ?: 0
                if (remaining <= 0) {
                    // Plant fully harvested, return home
                    state = AntState.RETURNING_HOME
                    logger.debug("Ant at ($x, $y) fully harvested plant, returning home")
                }
            } else {
                // No more food here, return home with what we have
                state = AntState.RETURNING_HOME
            }
        } else {
            // No food here anymore, continue exploring
            state = AntState.EXPLORING
        }
    }
    
    private fun returnToColony() {
        // Follow HOME pheromones back to colony
        
        // Drop FOOD pheromones if carrying food (trail for others to follow)
        if (carryingFood) {
            world.addPheromone(x, y, PheromoneType.FOOD, 3.0)
        }
        
        // Check if at colony
        if (abs(x - colony.queenX) <= 3 && abs(y - colony.queenY) <= 3) {
            // Deliver food if carrying
            if (carryingFood) {
                colony.addFood(5) // Carrying 5 units
                carryingFood = false
                logger.info("Ant delivered 5 food to colony")
            }
            
            // Eat stored food if hungry and rest
            if (energy < 80 && colony.consumeFood(1)) {
                energy = 100.0
                restUntilMs = com.kengine.time.getClockContext().totalTimeMs + 1500 // 1.5s rest
                state = AntState.RESTING
                logger.debug("Ant fed at colony, energy restored, resting")
                return
            }
            
            state = AntState.EXPLORING
            return
        }
        
        // Try to follow HOME pheromone gradient first (strong bias to avoid getting lost)
        val homeDirection = findStrongestPheromone(PheromoneType.HOME)
        if (homeDirection != null && Random.nextFloat() < 0.9f) { // 90% follow pheromones
            x = homeDirection.first
            y = homeDirection.second
            updateMoveHistory()
            return
        }
        
        // Otherwise move directly toward queen
        moveTowards(colony.queenX, colony.queenY, digOnlyIfNecessary = true)
    }
    
    private fun moveTowards(targetX: Int, targetY: Int, digOnlyIfNecessary: Boolean = false) {
        val dx = when {
            targetX > x -> 1
            targetX < x -> -1
            else -> 0
        }
        val dy = when {
            targetY > y -> 1
            targetY < y -> -1
            else -> 0
        }
        
        val nextX = x + dx
        val nextY = y + dy
        
        if (world.canWalkThrough(nextX, nextY)) {
            x = nextX
            y = nextY
        } else if (world.canDigThrough(nextX, nextY)) {
            // Only dig if necessary (within 5 tiles of target)
            val distance = abs(targetX - x) + abs(targetY - y)
            if (!digOnlyIfNecessary || distance < 5) {
                world.dig(nextX, nextY)
                x = nextX
                y = nextY
            } else {
                // Try to find path around obstacle
                tryAlternateRoute(targetX, targetY)
            }
        } else {
            // Can't dig (rock), try alternate route
            tryAlternateRoute(targetX, targetY)
        }
    }
    
    private fun tryAlternateRoute(targetX: Int, targetY: Int) {
        // Try horizontal then vertical
        if (abs(targetX - x) > abs(targetY - y)) {
            val dx = if (targetX > x) 1 else -1
            if (world.canWalkThrough(x + dx, y)) {
                x += dx
                return
            }
        }
        
        // Try vertical
        val dy = if (targetY > y) 1 else -1
        if (world.canWalkThrough(x, y + dy)) {
            y += dy
        }
    }
    
    private fun moveTowardsSurface(targetX: Int, targetY: Int) {
        // Prioritize moving upward if underground
        if (y > 8) {
            moveTowards(targetX, 5) // Move toward surface level
        } else {
            moveTowards(targetX, targetY)
        }
    }
    
    private fun findNearbyFood(radius: Int): Pair<Int, Int>? {
        val surfaceY = 4 // Vegetation row
        
        // Scan surface for vegetation within radius
        for (dx in -radius..radius) {
            val checkX = x + dx
            if (checkX < 0 || checkX >= world.width) continue
            
            val tile = world.getTile(checkX, surfaceY)
            if (tile?.vegetation != null && tile.vegetation!!.size > 0) {
                return Pair(checkX, surfaceY)
            }
        }
        
        return null
    }
    
    private fun updateMoveHistory() {
        moveHistory.add(Pair(x, y))
        if (moveHistory.size > maxHistorySize) {
            moveHistory.removeAt(0)
        }
    }
    
    private fun isInRecentHistory(position: Pair<Int, Int>): Boolean {
        return moveHistory.contains(position)
    }
    
    /**
     * Read pheromone gradient from 9 surrounding tiles (including self)
     * Returns the position with strongest pheromone
     */
    private fun readPheromoneGradient(type: PheromoneType): Pair<Int, Int>? {
        val positions = mutableListOf<Pair<Pair<Int, Int>, Double>>()
        
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                val strength = world.getPheromoneStrength(nx, ny, type)
                if (strength > 0.0) {
                    positions.add(Pair(Pair(nx, ny), strength))
                }
            }
        }
        
        // Return position with strongest pheromone
        return positions.maxByOrNull { it.second }?.first
    }
    
    private fun clearDirt() {
        // Pick up dirt and carry to surface
        if (world.clearDirtPile(x, y)) {
            carryingDirt = true
            state = AntState.CARRYING_DIRT
            logger.debug("Ant picked up dirt at ($x,$y)")
        } else {
            state = AntState.EXPLORING
        }
    }
    
    private fun carryDirtToSurface() {
        // Move toward surface (upward)
        val surfaceY = world.getSurfaceY()
        
        if (y <= surfaceY) {
            // At surface - drop dirt ABOVE surface (in sky) so it doesn't block entrance
            val shaftX = colony.queenX
            val dropPos = world.findMoundDropPosition(preferredX = shaftX, shaftX = shaftX)
            if (dropPos != null) {
                val t = world.getTile(dropPos.first, dropPos.second)
                if (t != null && t.type == TileType.AIR) {
                    t.type = TileType.DIRT_PILE
                    t.pheromones.clearAll() // Clear pheromones where dirt is placed
                }
            }
            carryingDirt = false
            state = AntState.EXPLORING
            logger.debug("Ant dropped dirt above surface")
            return
        }
        
        // Move upward, prefer following tunnels
        val neighbors = listOf(
            Pair(x, y - 1),     // Up (priority)
            Pair(x - 1, y - 1), // Up-left
            Pair(x + 1, y - 1), // Up-right
            Pair(x - 1, y),     // Left
            Pair(x + 1, y)      // Right
        )
        
        val validMoves = neighbors.filter { (nx, ny) ->
            world.canWalkThrough(nx, ny) && !isInRecentHistory(Pair(nx, ny))
        }
        
        if (validMoves.isNotEmpty()) {
            val nextPos = validMoves.first()
            x = nextPos.first
            y = nextPos.second
            updateMoveHistory()
        }
    }
    
    private fun moveRandomly() {
        val maxHorizontalFromShaft = 60

        // If we've wandered too far horizontally, bias back toward the queen shaft
        if (kotlin.math.abs(x - colony.queenX) > maxHorizontalFromShaft) {
            val dir = if (x > colony.queenX) -1 else 1
            val nx = x + dir
            if (world.canWalkThrough(nx, y)) {
                x = nx
                updateMoveHistory()
                return
            } else if (world.canDigThrough(nx, y)) {
                if (world.dig(nx, y)) {
                    world.pickupDirtPile(nx, y)
                    carryingDirt = true
                    state = AntState.CARRYING_DIRT
                    x = nx
                    updateMoveHistory()
                    return
                }
            }
        }

        // Prefer tiles with LOWER explore pheromone (encourages exploration of new areas)
        val directions = listOf(
            Pair(movementBias, 0),     // Biased horizontal
            Pair(-movementBias, 0),    // Opposite
            Pair(0, -1),               // Up (toward surface)
            Pair(0, 1),                // Down
            Pair(movementBias, -1),    // Diagonal biased
            Pair(-movementBias, -1),
            Pair(movementBias, 1),
            Pair(-movementBias, 1)
        )
        
        val candidates = directions.map { (dx, dy) ->
            val nx = x + dx
            val ny = y + dy
            val explore = world.getPheromone(nx, ny, PheromoneType.EXPLORE)
            Triple(nx, ny, explore)
        }.filter { (nx, ny, _) ->
            world.canWalkThrough(nx, ny) && !isInRecentHistory(Pair(nx, ny))
        }
        
        if (candidates.isNotEmpty() && Random.nextFloat() < 0.8f) {
            // Pick tile with lowest explore pheromone (least explored)
            val best = candidates.minByOrNull { it.third }!!
            x = best.first
            y = best.second
            updateMoveHistory()
            return
        }

        // Encourage side tunnels/chambers at depth
        if (y > colony.queenY + 6 && kotlin.math.abs(x - colony.queenX) < maxHorizontalFromShaft && Random.nextFloat() < 0.3f) {
            val dx = if (Random.nextBoolean()) movementBias else -movementBias
            val targetX = x + dx
            if (world.canWalkThrough(targetX, y) && !isInRecentHistory(Pair(targetX, y))) {
                x = targetX
                updateMoveHistory()
                return
            } else if (world.canDigThrough(targetX, y)) {
                if (world.dig(targetX, y)) {
                    world.pickupDirtPile(targetX, y)
                    carryingDirt = true
                    state = AntState.CARRYING_DIRT
                    x = targetX
                    updateMoveHistory()
                    return
                }
            }
        }
        
        // Fallback: random movement with shuffled directions
        
        // VERY STRONG preference for existing tunnels (98%)
        for ((dx, dy) in directions) {
            val nextX = x + dx
            val nextY = y + dy
            
            if (world.canWalkThrough(nextX, nextY) && !isInRecentHistory(Pair(nextX, nextY))) {
                x = nextX
                y = nextY
                updateMoveHistory()
                
                // Change bias occasionally for variety
                if (Random.nextFloat() < 0.05f) {
                    movementBias = -movementBias
                }
                return
            }
        }
        
        // Only dig if REALLY stuck (15% chance to allow chamber expansion)
        if (Random.nextFloat() < 0.15f) {
            for ((dx, dy) in directions) {
                val nextX = x + dx
                val nextY = y + dy
                
                if (world.canDigThrough(nextX, nextY)) {
                    if (world.dig(nextX, nextY)) {
                        world.pickupDirtPile(nextX, nextY)
                        carryingDirt = true
                        state = AntState.CARRYING_DIRT
                        x = nextX
                        y = nextY
                        updateMoveHistory()
                        return
                    }
                }
            }
        }
        
        // If truly stuck, clear move history and try again
        if (moveHistory.size >= maxHistorySize) {
            moveHistory.clear()
        }
    }
    
    private fun findAdjacentFood(): Pair<Int, Int>? {
        val directions = listOf(
            Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0),
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        )
        
        for ((dx, dy) in directions) {
            val checkX = x + dx
            val checkY = y + dy
            val tile = world.getTile(checkX, checkY)
            if (tile?.vegetation != null && tile.vegetation!!.size > 0) {
                return Pair(checkX, checkY)
            }
        }
        
        return null
    }
    
    private fun findStrongestPheromone(type: PheromoneType): Pair<Int, Int>? {
        val directions = listOf(
            Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0),
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        )
        
        var bestStrength = 0.0
        var bestDirection: Pair<Int, Int>? = null
        
        for ((dx, dy) in directions) {
            val checkX = x + dx
            val checkY = y + dy
            val strength = world.getPheromone(checkX, checkY, type)
            
            if (strength > bestStrength && (world.canWalkThrough(checkX, checkY) || world.canDigThrough(checkX, checkY))) {
                bestStrength = strength
                bestDirection = Pair(checkX, checkY)
            }
        }
        
        return bestDirection
    }
    
    private fun dropPheromones() {
        // Mark explored areas (so other ants prefer unexplored territory)
        world.addPheromone(x, y, PheromoneType.EXPLORE, 0.3)
        
        // Drop FOOD pheromone trail when carrying food (leads others to food source)
        if (carryingFood) {
            world.addPheromone(x, y, PheromoneType.FOOD, 0.5)
        }
        
        // Drop HOME pheromone when exploring near queen (creates home trail)
        if (state == AntState.EXPLORING && !carryingFood) {
            // Only near queen
            if (abs(x - colony.queenX) <= 5 && abs(y - colony.queenY) <= 5) {
                world.addPheromone(x, y, PheromoneType.HOME, 1.0)
            }
        }
    }
    
    fun draw() {
        useGeometryContext {
            val posX = x * world.tileSize + world.tileSize / 2
            val posY = y * world.tileSize + world.tileSize / 2
            val size = world.tileSize / 2.5 // Made slightly bigger
            
            // Draw ant body (simple polygon approximation with circles and rectangles)
            // Color based on what they're doing
            val antColor = when {
                carryingFood -> Color.orange
                carryingDirt -> Color(120u, 80u, 40u, 255u) // Tan/dirt color
                state == AntState.CLEARING_DIRT || state == AntState.CARRYING_DIRT -> Color(150u, 75u, 0u, 255u) // Darker brown
                else -> Color.red
            }
            
            // Body (main circle)
            fillCircle(posX, posY, (size / 1.5).toInt(), antColor)
            
            // Head (front circle)
            fillCircle(posX - size / 1.5, posY - size / 2, (size / 2).toInt(), antColor)
            
            // Abdomen (back circle)  
            fillCircle(posX + size / 1.5, posY + size / 2, (size / 2).toInt(), antColor)
            
            // Food indicator if carrying
            if (carryingFood) {
                fillCircle(posX, posY - size, (size / 3).toInt(), Color.green)
            }
        }
    }
}

enum class AntState {
    EXPLORING,      // Looking for food
    FORAGING,       // Harvesting food
    RETURNING_HOME, // Carrying food back to colony
    CLEARING_DIRT,  // Moving dirt piles out of the way
    CARRYING_DIRT,  // Carrying dirt to surface
    RESTING         // Resting at colony, recovering energy
}

