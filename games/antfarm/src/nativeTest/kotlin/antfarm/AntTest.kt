package antfarm

import com.kengine.hooks.context.ContextRegistry
import com.kengine.time.ClockContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AntTest {

    @BeforeTest
    fun setUp() {
        ContextRegistry.register(ClockContext.get())
    }

    @AfterTest
    fun tearDown() {
        ContextRegistry.clearAll()
    }

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

        ant.update(1.0)

        assertTrue(ant.energy < initialEnergy)
    }

    @Test
    fun testAntForagingBehavior() {
        val world = World(width = 50, height = 40)
        val colony = AntColony(world, queenX = 25, queenY = 20)
        val ant = Ant(x = 25, y = 22, world = world, colony = colony)

        assertEquals(AntState.EXPLORING, ant.state)

        world.getTile(25, 23)?.vegetation = Vegetation(size = 3)

        ant.update(0.1)

        if (ant.carryingFood) {
            assertEquals(AntState.RETURNING_HOME, ant.state)
        }
    }
}
