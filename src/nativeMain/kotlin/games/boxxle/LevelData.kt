package games.boxxle

data class LevelData(
    val tiles: List<List<Int>>,
    val boxes: List<List<Int>>,
    val goals: List<List<Int>>,
    val start: List<Int>,
    val scale: Double = 1.0
)

val LEVEL_DATA: List<LevelData> = listOf(
    LevelData( // 1
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 1, 1, 1),
            listOf(1, 0, 0, 0, 1, 0, 1, 0, 1),
            listOf(1, 1, 1, 0, 1, 1, 1, 0, 1),
            listOf(0, 1, 1, 0, 0, 0, 0, 0, 1),
            listOf(0, 1, 0, 0, 0, 1, 0, 0, 1),
            listOf(0, 1, 0, 0, 0, 1, 1, 1, 1),
            listOf(0, 1, 1, 1, 1, 1, 0, 0, 0)
        ),
        boxes = listOf(listOf(2, 2), listOf(3, 2), listOf(2, 3)),
        goals = listOf(listOf(7, 3), listOf(7, 4), listOf(7, 5)),
        start = listOf(1, 1)
    ),
    LevelData( // 2
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 1),
            listOf(1, 0, 0, 1, 0, 1, 1, 0, 1, 1),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 0, 1, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(listOf(3, 2), listOf(4, 3), listOf(2, 4), listOf(4, 6)),
        goals = listOf(listOf(1, 1), listOf(2, 1), listOf(1, 2), listOf(2, 2)),
        start = listOf(3, 1)
    ),
    LevelData( // 3
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 0, 0, 0),
            listOf(1, 1, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 1, 0, 0, 1, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(listOf(2, 2), listOf(2, 3), listOf(3, 4), listOf(2, 5)),
        goals = listOf(listOf(1, 5), listOf(2, 6), listOf(1, 6), listOf(4, 6)),
        start = listOf(1, 2)
    ),
    LevelData( // 4
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 1, 0, 0, 0, 0),
            listOf(0, 1, 0, 0, 1, 1, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 1, 1, 0, 1, 0, 1, 1, 0, 0),
            listOf(1, 0, 1, 0, 1, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(listOf(3, 2), listOf(2, 5), listOf(5, 6)),
        goals = listOf(listOf(1, 4), listOf(1, 5), listOf(1, 6)),
        start = listOf(2, 1)
    ),
    LevelData( // 5
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(listOf(2, 2), listOf(4, 3), listOf(7, 3), listOf(6, 4)),
        goals = listOf(listOf(2, 4), listOf(3, 4), listOf(2, 5), listOf(3, 5)),
        start = listOf(4, 1)
    ),
    LevelData( // 6
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0),
            listOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0),
            listOf(0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0)
        ),
        boxes = listOf(listOf(4, 6), listOf(6, 6), listOf(4, 8), listOf(6, 8)),
        goals = listOf(listOf(4, 4), listOf(8, 6), listOf(2, 8), listOf(6, 10)),
        start = listOf(6, 3),
        scale = 0.8
    ),
    LevelData( // 7
        tiles = listOf(
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(listOf(3, 3), listOf(5, 3), listOf(7, 3), listOf(4, 4), listOf(4, 5)),
        goals = listOf(listOf(1, 6), listOf(2, 6), listOf(3, 6), listOf(4, 6), listOf(5, 6)),
        start = listOf(8, 1)
    ),
    LevelData( // 8
        tiles = listOf(
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(listOf(4, 2), listOf(3, 3), listOf(5, 3), listOf(4, 4), listOf(6, 4)),
        goals = listOf(listOf(2, 2), listOf(1, 3), listOf(2, 3), listOf(1, 4), listOf(2, 4)),
        start = listOf(6, 1)
    ),
    LevelData( // 9
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0),
            listOf(0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0),
            listOf(1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(2, 3), listOf(8, 3),
            listOf(2, 6), listOf(5, 6), listOf(8, 6)
        ),
        goals = listOf(
            listOf(4, 4), listOf(5, 4), listOf(6, 4),
            listOf(4, 5), listOf(5, 5), listOf(6, 5)
        ),
        start = listOf(3, 7)
    ),
    LevelData( // 10
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 1, 0, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(
            listOf(3, 2), listOf(4, 2), listOf(5, 2),
            listOf(3, 3), listOf(2, 4)
        ),
        goals = listOf(
            listOf(4, 3), listOf(5, 3),
            listOf(3, 4), listOf(4, 4), listOf(5, 4)
        ),
        start = listOf(1, 3)
    ),
    LevelData( // 11
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0),
            listOf(1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(
            listOf(2, 2), listOf(8, 2), listOf(3, 3), listOf(9, 3)
        ),
        goals = listOf(
            listOf(4, 3), listOf(5, 3), listOf(6, 3), listOf(7, 3)
        ),
        start = listOf(1, 3)
    ),
    LevelData( // 12
        tiles = listOf(
            listOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(7, 3), listOf(5, 4),
            listOf(8, 4), listOf(2, 7), listOf(5, 7)
        ),
        goals = listOf(
            listOf(14, 6), listOf(15, 6), listOf(14, 7),
            listOf(15, 7), listOf(14, 8), listOf(15, 8)
        ),
        start = listOf(7, 2),
        scale = 0.8
    ),
    LevelData( // 13
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 0, 0),
            listOf(1, 1, 1, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 0, 0)
        ),
        boxes = listOf(
            listOf(3, 2), listOf(4, 3), listOf(5, 4)
        ),
        goals = listOf(
            listOf(4, 2), listOf(3, 3), listOf(5, 3)
        ),
        start = listOf(4, 4)
    ),
    LevelData( // 14
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(5, 3), listOf(3, 4), listOf(4, 4),
            listOf(5, 4), listOf(9, 4), listOf(5, 5), listOf(8, 5),
            listOf(3, 6), listOf(5, 6), listOf(7, 6)
        ),
        goals = listOf(
            listOf(16, 2), listOf(17, 2), listOf(16, 3), listOf(17, 3),
            listOf(16, 4), listOf(17, 4), listOf(18, 4), listOf(16, 5),
            listOf(17, 5), listOf(16, 6), listOf(17, 6)
        ),
        start = listOf(6, 4),
        scale = 0.75
    ),
    LevelData( // 15
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 1, 0, 0, 0),
            listOf(0, 1, 1, 0, 0, 1, 1, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(4, 3), listOf(3, 4), listOf(4, 5), listOf(5, 5)
        ),
        goals = listOf(
            listOf(3, 1), listOf(4, 1), listOf(4, 2), listOf(5, 3)
        ),
        start = listOf(1, 6)
    )
)
