package antfarm

import com.kengine.hooks.context.ContextRegistry
import com.kengine.time.ClockContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SimulationRunner {

    @BeforeTest
    fun setUp() {
        ContextRegistry.register(ClockContext.get())
    }

    @AfterTest
    fun tearDown() {
        ContextRegistry.clearAll()
    }

    @Test
    fun testAntColonySimulation() {
        println("\n=== Starting Headless Ant Colony Simulation ===\n")

        val world = World(width = 120, height = 80, tileSize = 10.0)
        val colony = AntColony(world, queenX = 60, queenY = 50)

        val simulationTicks = 600
        val deltaTime = 1.0 / 60.0

        for (tick in 0 until simulationTicks) {
            colony.update(deltaTime)

            if (tick % 6 == 0) {
                world.decayPheromones()
            }

            if (tick % 300 == 0) {
                world.growPlants()
            }

            if (tick % 60 == 0) {
                val ants = colony.ants
                println("[${ tick / 60 } s] Ants: ${ants.size} | Food: ${colony.getFoodStored()} | Plants: ${colony.getPlantsStored()}")
            }
        }

        println("\n=== Simulation Complete ===\n")

        assertTrue(colony.getAntCount() > 0, "Colony should have ants")
    }
}
