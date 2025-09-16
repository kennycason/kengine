package hextris

object Sprites {
    const val BLOCK_SPRITES = "assets/sprites/block_sprites.png"

    // Sprite dimensions
    const val BLOCK_SIZE = 24

    // Sprite positions in the sprite sheet (6x6 grid with 28 piece types)
    // The sprite sheet is organized as follows:
    // - Row 0: Pieces 1-6
    // - Row 1: Pieces 7-12
    // - Row 2: Pieces 13-18
    // - Row 3: Pieces 19-24
    // - Row 4: Pieces 25-28, followed by 2 blank tiles
    val PIECE_SPRITES = mapOf(
        // Row 0
        PieceType.O to (0 to 0),              // 1. Square
        PieceType.L to (1 to 0),              // 2. L
        PieceType.J to (2 to 0),              // 3. J (L-backwards)
        PieceType.I to (3 to 0),              // 4. I (line)
        PieceType.S to (4 to 0),              // 5. S (N)
        PieceType.Z to (5 to 0),              // 6. Z (N-backwards)

        // Row 1
        PieceType.T to (0 to 1),              // 7. T
        PieceType.DOT to (1 to 1),            // 8. Dot
        PieceType.SCREW to (2 to 1),          // 9. Screw
        PieceType.SCREW_BACKWARDS to (3 to 1), // 10. Screw backwards
        PieceType.LONG_CROSS to (4 to 1),     // 11. Long cross
        PieceType.CROSS to (5 to 1),          // 12. Cross

        // Row 2
        PieceType.LAYERS to (0 to 2),         // 13. Layers
        PieceType.Y to (1 to 2),              // 14. Y
        PieceType.U to (2 to 2),              // 15. U
        PieceType.LINE_5 to (3 to 2),         // 16. 5 line
        PieceType.LINE_6 to (4 to 2),         // 17. 6 line
        PieceType.BLOCK_3X2 to (5 to 2),      // 18. 3x2 block

        // Row 3
        PieceType.ZIG_ZAG to (0 to 3),        // 19. Zig-zag
        PieceType.NOTCH_TOP to (1 to 3),      // 20. 2x2 + notch top
        PieceType.NOTCH_BOTTOM to (2 to 3),   // 21. 2x2 + notch bottom
        PieceType.LINE_2 to (3 to 3),         // 22. 2-line
        PieceType.BIG_T to (4 to 3),          // 23. Big T
        PieceType.SHORT_PARALLEL to (5 to 3), // 24. Short parallel

        // Row 4
        PieceType.BIG_L_BACKWARDS to (0 to 4), // 25. Big L backwards
        PieceType.TO to (1 to 4),             // 26. TO
        PieceType.SMALL_L to (2 to 4),        // 27. Small L
        PieceType.LINE_3 to (3 to 4)          // 28. 3x1 line
        // (4 to 4) and (5 to 4) are blank
    )

    // For backward compatibility - maps colors to the first row of sprites
    val BLOCK_COLORS = listOf(
        0 to 0,  // Red
        1 to 0,  // Orange
        2 to 0,  // Yellow
        3 to 0,  // Green
        4 to 0,  // Blue
        5 to 0   // Purple
    )
}
