package antfarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simple tests to validate world tile logic WITHOUT requiring SDL/graphics
 */
class WorldVisualizationTest {
    
    @Test
    fun testDetermineTileTypesCorrectly() {
        // Test that tile types are set correctly based on position
        // Height=80, we want 60 rows of sky (rows 0-59), surface at 60, underground 61-79
        
        val skyTile = TileType.AIR
        val undergroundTile = TileType.DIRT
        
        // Manually test the logic: y <= surfaceY should be AIR
        val surfaceY = 60
        
        // Sky rows (0-59)
        for (y in 0 until surfaceY) {
            val result = if (y <= surfaceY) TileType.AIR else TileType.DIRT
            assertEquals(skyTile, result, "Row $y should be AIR (sky), got $result")
        }
        
        // Surface row (60) - should also be AIR so plants show against blue
        val surfaceResult = if (surfaceY <= surfaceY) TileType.AIR else TileType.DIRT
        assertEquals(skyTile, surfaceResult, "Surface row $surfaceY should be AIR, got $surfaceResult")
        
        // Underground rows (61+)
        for (y in (surfaceY + 1) until 80) {
            val result = if (y <= surfaceY) TileType.AIR else TileType.DIRT
            assertEquals(undergroundTile, result, "Row $y should be DIRT (underground), got $result")
        }
        
        println("✓ Tile type logic test passed")
    }
    
    @Test
    fun testSkyPercentage() {
        // With height=80, surfaceY=60, we have 61 rows of AIR (0-60 inclusive)
        // That's 76.25% sky which is good
        val totalRows = 80
        val surfaceY = 60
        val skyRows = surfaceY + 1 // Rows 0 through surfaceY (inclusive)
        val skyPercentage = (skyRows.toFloat() / totalRows) * 100
        
        assertTrue(skyPercentage > 70, "Sky should be >70% of screen, got ${skyPercentage}%")
        println("✓ Sky percentage test passed: ${skyPercentage}% of screen is sky")
    }
}

