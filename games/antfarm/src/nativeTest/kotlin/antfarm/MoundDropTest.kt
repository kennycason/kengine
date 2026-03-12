package antfarm

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MoundDropTest {
    @Test
    fun testMoundDropIsAboveSurfaceAndNotShaft() {
        val world = World(width = 120, height = 80, tileSize = 10.0)
        val surface = world.getSurfaceY()
        val shaftX = 60

        val drop = world.findMoundDropPosition(preferredX = shaftX, shaftX = shaftX)
        assertNotNull(drop, "Should find a mound drop position")
        val (dx, dy) = drop
        assertTrue(dy < surface, "Drop should be above surface")
        assertTrue(dx != shaftX, "Drop should not be on shaft column")
    }
}

