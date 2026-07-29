package hextris

object Sprites {
    const val BLOCK_SPRITES = HextrisAssets.BLOCKS_SOURCE
    const val BLOCK_SPRITE_ID = HextrisAssets.BLOCKS_ID
    const val BLOCK_SIZE = HextrisAssets.BLOCKS_TILE_WIDTH

    val PIECE_SPRITES = mapOf(
        PieceType.O to (0 to 0),
        PieceType.L to (1 to 0),
        PieceType.J to (2 to 0),
        PieceType.I to (3 to 0),
        PieceType.S to (4 to 0),
        PieceType.Z to (5 to 0),
        PieceType.T to (0 to 1),
        PieceType.DOT to (1 to 1),
        PieceType.SCREW to (2 to 1),
        PieceType.SCREW_BACKWARDS to (3 to 1),
        PieceType.LONG_CROSS to (4 to 1),
        PieceType.CROSS to (5 to 1),
        PieceType.LAYERS to (0 to 2),
        PieceType.Y to (1 to 2),
        PieceType.U to (2 to 2),
        PieceType.LINE_5 to (3 to 2),
        PieceType.LINE_6 to (4 to 2),
        PieceType.BLOCK_3X2 to (5 to 2),
        PieceType.ZIG_ZAG to (0 to 3),
        PieceType.NOTCH_TOP to (1 to 3),
        PieceType.NOTCH_BOTTOM to (2 to 3),
        PieceType.LINE_2 to (3 to 3),
        PieceType.BIG_T to (4 to 3),
        PieceType.SHORT_PARALLEL to (5 to 3),
        PieceType.BIG_L_BACKWARDS to (0 to 4),
        PieceType.TO to (1 to 4),
        PieceType.SMALL_L to (2 to 4),
        PieceType.LINE_3 to (3 to 4)
    )

    fun colorIndex(type: PieceType): Int {
        val sprite = PIECE_SPRITES[type] ?: (0 to 0)
        return sprite.first + sprite.second * 6
    }
}
