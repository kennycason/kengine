package hextris

object Sprites {
    const val BLOCK_SPRITES = "assets/sprites/block_sprites.png"

    // Sprite dimensions
    const val BLOCK_SIZE = 24

    // Sprite positions in the sprite sheet (assuming 6 colors for Hextris)
    val BLOCK_COLORS = listOf(
        0 to 0,  // Red
        1 to 0,  // Orange
        2 to 0,  // Yellow
        3 to 0,  // Green
        4 to 0,  // Blue
        5 to 0   // Purple
    )
}
