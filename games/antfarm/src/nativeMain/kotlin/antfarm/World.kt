package antfarm

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.log.Logging
import kotlin.random.Random

/**
 * The World represents the 2D grid-based antfarm environment.
 * Contains terrain, vegetation, and pheromone information.
 */
class World(
    val width: Int,
    val height: Int,
    val tileSize: Double = 10.0
) : Logging {
    
    // Surface vegetation layer (top of world) - MUST BE BEFORE tiles array!
    private val skyHeight = 15 // Top ~15 rows of visible sky
    private val surfaceY = 15 // Ground surface at row 15 (plants grow here, ~19% of screen is sky/plants)
    
    // Terrain layers (initialized AFTER surfaceY so determineTileType sees correct value)
    private val tiles: Array<Array<Tile>> = Array(height) { y ->
        Array(width) { x ->
            Tile(x, y, determineTileType(x, y))
        }
    }
    
    init {
        initializeWorld()
        logger.info("World created: ${width}x${height} tiles, surface at y=$surfaceY (${skyHeight} rows of sky)")
    }

    fun getSurfaceY(): Int = surfaceY
    
    private fun determineTileType(x: Int, y: Int): TileType {
        return when {
            y <= surfaceY -> TileType.AIR // Sky AND surface are AIR (show blue background, plants grow here)
            else -> TileType.DIRT // Underground is DIRT
        }
    }
    
    private fun initializeWorld() {
        // Add DENSE vegetation at ground level (surfaceY) - ants walk on/through this
        for (x in 0 until width) {
            if (Random.nextFloat() < 0.85f) { // 85% coverage
                tiles[surfaceY][x].vegetation = Vegetation(Random.nextInt(8, 20))
            }
        }
        
        // Add tall plants growing UP from surface into the sky
        for (x in 0 until width) {
            if (Random.nextFloat() < 0.4f) {
                for (dy in 1..Random.nextInt(2, 5)) {
                    val y = surfaceY - dy
                    if (y >= 0) {
                        tiles[y][x].vegetation = Vegetation(Random.nextInt(12, 25))
                    }
                }
            }
        }
        
        // Add vegetation clusters for more natural look
        for (i in 0 until width / 8) {
            val centerX = Random.nextInt(width)
            val clusterSize = Random.nextInt(4, 12)
            for (dx in -clusterSize/2..clusterSize/2) {
                val x = centerX + dx
                if (x >= 0 && x < width) {
                    if (Random.nextFloat() < 0.9f) {
                        tiles[surfaceY][x].vegetation = Vegetation(Random.nextInt(15, 30))
                    }
                }
            }
        }
        
        // Add some rocks in underground as obstacles
        for (i in 0 until width / 5) {
            val x = Random.nextInt(width)
            val y = Random.nextInt(surfaceY + 2, height) // Below ground
            if (tiles[y][x].type == TileType.DIRT) {
                tiles[y][x].type = TileType.ROCK
            }
        }
    }
    
    fun getTile(x: Int, y: Int): Tile? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return tiles[y][x]
    }
    
    fun canDigThrough(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        // Can only dig underground, not at surface or above
        if (y <= surfaceY) return false
        return tile.type == TileType.DIRT
    }
    
    fun dig(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        // Don't dig into sky or above ground!
        if (y <= surfaceY) return false // Don't dig surface or sky
        
        if (tile.type == TileType.DIRT || tile.type == TileType.GROUND) {
            // Instead of disappearing, dirt becomes a pile that must be moved
            tile.type = TileType.DIRT_PILE
            
            // Dirt piles wipe trails here (prevents "pheromone through wall")
            tile.pheromones.clearAll()
            
            return true
        }
        return false
    }
    
    fun clearDirtPile(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        if (tile.type == TileType.DIRT_PILE) {
            tile.type = TileType.AIR
            return true
        }
        return false
    }

    /**
     * Pickup a dirt pile at the given tile (turns it into AIR).
     */
    fun pickupDirtPile(x: Int, y: Int): Boolean {
        return clearDirtPile(x, y)
    }

    /**
     * Find a safe place to drop dirt as a mound above the surface.
     * - Never drop on the main shaft column
     * - Prefer sky cells 1–2 rows above surface
     * - Spread within a radius around preferredX
     */
    fun findMoundDropPosition(preferredX: Int, shaftX: Int): Pair<Int, Int>? {
        val yOptions = listOf(surfaceY - 2, surfaceY - 1).filter { it >= 0 }
        val radius = 8
        for (dy in yOptions) {
            // Try offsets in expanding order to distribute piles
            for (r in 0..radius) {
                val candidates = listOf(preferredX - r, preferredX + r)
                for (cx in candidates) {
                    if (cx < 0 || cx >= width) continue
                    if (cx == shaftX) continue // don't block shaft column
                    val t = getTile(cx, dy) ?: continue
                    if (t.type == TileType.AIR) {
                        return Pair(cx, dy)
                    }
                }
            }
        }
        return null
    }
    
    fun hasDirtPile(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        return tile.type == TileType.DIRT_PILE
    }
    
    fun canWalkThrough(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        
        // Ants can walk through:
        // 1. Underground tunnels (AIR below ground)
        // 2. Vegetation (plants)
        // 3. Dirt piles
        // 4. Ground surface itself
        // But NOT through sky (AIR above ground)
        
        if (y < surfaceY) {
            // Above ground - can only walk through vegetation, not empty sky
            return tile.vegetation != null
        } else if (y == surfaceY) {
            // Surface row: allow walking even if the vegetation was eaten (AIR), plus piles/ground
            return tile.type == TileType.AIR ||
                   tile.vegetation != null ||
                   tile.type == TileType.DIRT_PILE ||
                   tile.type == TileType.GROUND
        } else {
            // Below ground - can walk through AIR (tunnels), vegetation, dirt piles, ground
            return tile.type == TileType.AIR || 
                   tile.vegetation != null || 
                   tile.type == TileType.DIRT_PILE ||
                   tile.type == TileType.GROUND
        }
    }
    
    fun harvestVegetation(x: Int, y: Int, amount: Int = 1): Boolean {
        val tile = getTile(x, y) ?: return false
        if (tile.vegetation != null && tile.vegetation!!.size > 0) {
            val harvested = minOf(amount, tile.vegetation!!.size)
            tile.vegetation!!.size -= harvested
            if (tile.vegetation!!.size <= 0) {
                tile.vegetation = null
            }
            return true
        }
        return false
    }
    
    fun growPlants() {
        // Grow existing plants (ONLY at surface level)
        for (x in 0 until width) {
            val tile = tiles[surfaceY][x]
            if (tile.vegetation != null && tile.vegetation!!.size < 5) {
                if (Random.nextFloat() < 0.1f) { // 10% growth chance
                    tile.vegetation!!.size++
                }
            }
        }
        
        // Spawn new vegetation (ONLY at surface)
        for (x in 0 until width) {
            if (Random.nextFloat() < 0.05f) { // 5% spawn chance per growth cycle
                if (tiles[surfaceY][x].vegetation == null) {
                    tiles[surfaceY][x].vegetation = Vegetation(1)
                }
            }
        }
    }
    
    fun decayPheromones() {
        // Tuned decay rates:
        // - HOME: ultra slow (persistent trail)
        // - FOOD: fades but still sticks around
        // - EXPLORE: medium
        val homeRate = 0.0002  // 0.02% per tick
        val foodRate = 0.015   // 1.5% per tick
        val exploreRate = 0.01 // 1% per tick
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                tiles[y][x].pheromones.decay(homeRate, foodRate, exploreRate)
            }
        }
    }
    
    fun addPheromone(x: Int, y: Int, type: PheromoneType, strength: Double) {
        val tile = getTile(x, y) ?: return
        tile.pheromones.add(type, strength)
    }
    
    fun getPheromone(x: Int, y: Int, type: PheromoneType): Double {
        val tile = getTile(x, y) ?: return 0.0
        return tile.pheromones.get(type)
    }
    
    fun getPheromoneStrength(x: Int, y: Int, type: PheromoneType): Double {
        return getPheromone(x, y, type)
    }
    
    fun draw() {
        useGeometryContext {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val tile = tiles[y][x]
                    drawTile(tile, x, y)
                }
            }
        }
    }
    
    private fun drawPheromoneBars(tile: Tile, posX: Double, posY: Double) {
        useGeometryContext {
            val barH = 3.0
            val pad = 1.0
            val maxW = tileSize - pad * 2
            
            // Normalize 0..100 -> 0..1
            fun w(str: Double) = (maxW * (str.coerceIn(0.0, 100.0) / 100.0))
            
            val home = tile.pheromones.get(PheromoneType.HOME)
            val food = tile.pheromones.get(PheromoneType.FOOD)
            val explore = tile.pheromones.get(PheromoneType.EXPLORE)
            
            var row = 0
            
            // HOME bars disabled to reduce visual noise near the queen
            if (food > 1.0) {
                fillRectangle(posX + pad, posY + tileSize - pad - barH - row * (barH + 1), w(food), barH, Color(255u, 80u, 80u, 140u))
                row++
            }
            if (explore > 1.0) {
                fillRectangle(posX + pad, posY + tileSize - pad - barH - row * (barH + 1), w(explore), barH, Color(80u, 255u, 120u, 140u))
                row++
            }
        }
    }
    
    private fun drawTile(tile: Tile, x: Int, y: Int) {
        useGeometryContext {
            val posX = x * tileSize
            val posY = y * tileSize
            
            // Draw ground tiles
            when (tile.type) {
                TileType.AIR -> {
                    // Underground tunnels: draw BRIGHT cyan background to show clearly
                    if (y > surfaceY) {
                        fillRectangle(posX, posY, tileSize, tileSize, Color(0u, 255u, 255u, 255u)) // Bright cyan
                    }
                    // Sky: don't draw (show blue background)
                }
                TileType.GROUND -> fillRectangle(posX, posY, tileSize, tileSize, Color(139u, 90u, 43u, 255u))
                TileType.DIRT -> fillRectangle(posX, posY, tileSize, tileSize, Color(101u, 67u, 33u, 255u))
                TileType.ROCK -> fillRectangle(posX, posY, tileSize, tileSize, Color(128u, 128u, 128u, 255u))
                TileType.DIRT_PILE -> fillRectangle(posX, posY, tileSize, tileSize, Color(120u, 80u, 40u, 255u))
            }
            
            // Draw vegetation with stems and colorful flowers
            if (tile.vegetation != null) {
                val vegSize = tile.vegetation!!.size
                val greenIntensity = (50 + vegSize * 40).coerceIn(0, 255).toUByte()
                
                // Stem/trunk (green)
                val stemHeight = tileSize * (0.5 + vegSize * 0.15)
                fillRectangle(
                    posX, 
                    posY - (stemHeight - tileSize), 
                    tileSize, 
                    stemHeight,
                    Color(20u, greenIntensity, 10u, 255u)
                )
                
                // Flower on top (colorful, more nutritious)
                if (vegSize >= 3) {
                    val flowerColor = when ((x + y) % 5) {
                        0 -> Color(255u, 50u, 50u, 255u)   // Red
                        1 -> Color(255u, 200u, 50u, 255u)  // Yellow
                        2 -> Color(255u, 100u, 200u, 255u) // Pink
                        3 -> Color(200u, 100u, 255u, 255u) // Purple
                        else -> Color(255u, 255u, 255u, 255u) // White
                    }
                    val flowerSize = tileSize * 0.8
                    fillCircle(posX + tileSize / 2, posY - stemHeight + tileSize, (flowerSize / 2).toInt(), flowerColor)
                }
            }
            
            // Draw pheromones as thin bars (non-obstructive)
            drawPheromoneBars(tile, posX, posY)
        }
    }
    
    fun reset() {
        // Reset all tiles
        for (y in 0 until height) {
            for (x in 0 until width) {
                tiles[y][x] = Tile(x, y, determineTileType(x, y))
            }
        }
        initializeWorld()
    }
}

/**
 * A single tile in the world grid
 */
class Tile(
    val x: Int,
    val y: Int,
    var type: TileType
) {
    var vegetation: Vegetation? = null
    val pheromones = PheromoneMap()
}

enum class TileType {
    AIR,        // Empty space (tunnels, above ground)
    GROUND,     // Surface ground layer
    DIRT,       // Underground dirt that can be dug
    ROCK,       // Rocks that cannot be dug through
    DIRT_PILE   // Dug up dirt that needs to be moved
}

/**
 * Vegetation that grows on the surface
 */
class Vegetation(var size: Int = 1) {
    val maxSize = 5
}

