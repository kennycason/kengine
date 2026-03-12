package antfarm

/**
 * Pheromone system for ant communication
 */
enum class PheromoneType {
    HOME,      // Leads back to the colony/queen
    FOOD,      // Leads to food sources
    EXPLORE,   // Exploration marker
    DANGER,    // Marks dangerous areas
    HUNGER     // Queen is hungry, needs food urgently
}

/**
 * A map of pheromone types to their strengths for a single tile
 */
class PheromoneMap {
    private val pheromones = mutableMapOf<PheromoneType, Double>()
    
    fun add(type: PheromoneType, strength: Double) {
        val current = pheromones[type] ?: 0.0
        // Diminishing returns - as pheromone builds up, adding more has less effect
        val diminishingFactor = 1.0 - (current / 100.0).coerceIn(0.0, 0.9)
        val actualAddition = strength * diminishingFactor
        pheromones[type] = (current + actualAddition).coerceIn(0.0, 100.0) // Max strength of 100
    }
    
    fun get(type: PheromoneType): Double {
        return pheromones[type] ?: 0.0
    }
    
    fun clear(type: PheromoneType) {
        pheromones.remove(type)
    }
    
    fun clearAll() {
        pheromones.clear()
    }
    
    // Per-type decay rates, so HOME can be mega slow
    fun decay(homeRate: Double, foodRate: Double, exploreRate: Double, dangerRate: Double = exploreRate, hungerRate: Double = exploreRate) {
        val toRemove = mutableListOf<PheromoneType>()
        for ((type, strength) in pheromones) {
            val rate = when (type) {
                PheromoneType.HOME -> homeRate
                PheromoneType.FOOD -> foodRate
                PheromoneType.EXPLORE -> exploreRate
                PheromoneType.DANGER -> dangerRate
                PheromoneType.HUNGER -> hungerRate
            }
            val newStrength = strength * (1.0 - rate)
            if (newStrength < 0.01) toRemove.add(type) else pheromones[type] = newStrength
        }
        toRemove.forEach { pheromones.remove(it) }
    }
}

