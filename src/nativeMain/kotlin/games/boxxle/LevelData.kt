package games.boxxle

data class LevelData(
    val tiles: List<List<Int>>,
    val boxes: List<List<Int>>,
    val goals: List<List<Int>>,
    val start: List<Int>,
    val scale: Double = 2.0
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
        scale = 1.3
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
        scale = 1.3
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
        scale = 1.2
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
    ),
    LevelData( // 16
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(11, 3), listOf(13, 3), listOf(5, 4), listOf(6, 4), listOf(7, 4), listOf(9, 4),
            listOf(12, 4), listOf(6, 5), listOf(12, 5), listOf(5, 6), listOf(6, 6), listOf(9, 6),
            listOf(11, 6), listOf(13, 6), listOf(6, 7), listOf(5, 9), listOf(2, 10), listOf(3, 10),
            listOf(5, 10), listOf(6, 10)
        ),
        goals = listOf(
            listOf(17, 1), listOf(18, 1), listOf(17, 2), listOf(18, 2), listOf(17, 3), listOf(18, 3),
            listOf(17, 4), listOf(18, 4), listOf(15, 5), listOf(16, 5), listOf(17, 5), listOf(18, 5),
            listOf(15, 6), listOf(16, 6), listOf(17, 6), listOf(18, 6), listOf(15, 7), listOf(16, 7),
            listOf(17, 7), listOf(18, 7)
        ),
        start = listOf(7, 10),
        scale = 1.2
    ),
    LevelData( // 17
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(4, 2), listOf(4, 3), listOf(5, 4), listOf(4, 5), listOf(6, 5), listOf(7, 6)
        ),
        goals = listOf(
            listOf(1, 4), listOf(2, 4), listOf(3, 4), listOf(1, 5), listOf(2, 5), listOf(3, 5)
        ),
        start = listOf(4, 7)
    ),
    LevelData( // 18
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(4, 3), listOf(5, 4), listOf(4, 5), listOf(7, 5)
        ),
        goals = listOf(
            listOf(7, 1), listOf(6, 2), listOf(7, 2), listOf(8, 2), listOf(8, 3)
        ),
        start = listOf(7, 3)
    ),
    LevelData( // 19
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 1, 0, 0, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 1, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(2, 2), listOf(3, 2), listOf(4, 2), listOf(6, 4)
        ),
        goals = listOf(
            listOf(4, 3), listOf(5, 3), listOf(4, 4), listOf(5, 4)
        ),
        start = listOf(1, 1)
    ),

    LevelData( // 20
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(2, 2), listOf(5, 2), listOf(2, 3), listOf(2, 4), listOf(5, 4)
        ),
        goals = listOf(
            listOf(3, 2), listOf(4, 2), listOf(3, 3), listOf(3, 4), listOf(4, 4)
        ),
        start = listOf(1, 3)
    ),

    LevelData( // 21
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(6, 2), listOf(3, 3), listOf(5, 3), listOf(7, 3), listOf(2, 4), listOf(3, 4), listOf(7, 4)
        ),
        goals = listOf(
            listOf(2, 1), listOf(3, 1), listOf(4, 1), listOf(5, 1), listOf(3, 2), listOf(4, 2), listOf(5, 2)
        ),
        start = listOf(7, 5),
        scale = 2.0
    ),
    LevelData( // 22
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0),
            listOf(0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0),
            listOf(1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0),
            listOf(0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(10, 3), listOf(12, 3), listOf(11, 4), listOf(10, 5),
            listOf(12, 5), listOf(10, 6), listOf(12, 6), listOf(8, 8),
            listOf(10, 8), listOf(9, 9)
        ),
        goals = listOf(
            listOf(2, 7), listOf(1, 8), listOf(2, 8), listOf(3, 8),
            listOf(2, 9), listOf(1, 10), listOf(2, 10), listOf(3, 10),
            listOf(2, 11)
        ),
        start = listOf(11, 9),
        scale = 1.4
    ),
    LevelData( // 23
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(3, 1), listOf(2, 3), listOf(3, 3), listOf(4, 3),
            listOf(3, 4), listOf(2, 5), listOf(3, 5), listOf(4, 5)
        ),
        goals = listOf(
            listOf(1, 1), listOf(2, 1), listOf(4, 1), listOf(5, 1),
            listOf(1, 2), listOf(2, 2), listOf(4, 2), listOf(5, 2)
        ),
        start = listOf(5, 5),
        scale = 2.0
    ),
    LevelData( // 24
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1, 0),
            listOf(0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(7, 2), listOf(2, 3), listOf(5, 3), listOf(8, 3),
            listOf(3, 5), listOf(5, 5), listOf(3, 8), listOf(5, 9)
        ),
        goals = listOf(
            listOf(13, 1), listOf(14, 1), listOf(13, 2), listOf(14, 2),
            listOf(13, 3), listOf(14, 3), listOf(13, 4), listOf(14, 4)
        ),
        start = listOf(2, 2),
        scale = 1.4
    ),
    LevelData( // 25
        tiles = listOf(
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(4, 2), listOf(4, 3), listOf(3, 4), listOf(5, 5),
            listOf(4, 6), listOf(5, 7), listOf(4, 8)
        ),
        goals = listOf(
            listOf(1, 3), listOf(2, 3), listOf(1, 4), listOf(2, 4),
            listOf(1, 5), listOf(2, 5), listOf(2, 6)
        ),
        start = listOf(6, 1),
        scale = 1.5
    ),
    LevelData( // 26
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(11, 2), listOf(13, 2), listOf(10, 3), listOf(12, 3),
            listOf(10, 6), listOf(11, 6), listOf(10, 7), listOf(8, 9),
            listOf(10, 9), listOf(9, 10)
        ),
        goals = listOf(
            listOf(2, 4), listOf(3, 4), listOf(4, 4), listOf(1, 5),
            listOf(2, 5), listOf(3, 5), listOf(4, 5), listOf(2, 6),
            listOf(3, 6), listOf(4, 6)
        ),
        start = listOf(8, 5),
        scale = 1.3
    ),
    LevelData( // 27
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 0),
            listOf(0, 1, 0, 1, 1, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(3, 2), listOf(4, 2), listOf(5, 4), listOf(6, 5),
            listOf(2, 6), listOf(5, 6)
        ),
        goals = listOf(
            listOf(2, 3), listOf(3, 3), listOf(4, 3),
            listOf(2, 4), listOf(3, 4), listOf(4, 4)
        ),
        start = listOf(6, 1),
        scale = 2.0
    ),
    LevelData( // 28
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0),
            listOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(4, 2), listOf(6, 2), listOf(2, 3),
            listOf(6, 3), listOf(3, 4), listOf(4, 4)
        ),
        goals = listOf(
            listOf(5, 5), listOf(6, 5), listOf(3, 6),
            listOf(4, 6), listOf(5, 6), listOf(6, 6)
        ),
        start = listOf(8, 3),
        scale = 2.0
    ),
    LevelData( // 29
        tiles = listOf(
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0),
            listOf(1, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0),
            listOf(0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(5, 3), listOf(6, 3), listOf(7, 3),
            listOf(5, 6), listOf(6, 7), listOf(3, 9), listOf(6, 9)
        ),
        goals = listOf(
            listOf(4, 3), listOf(3, 4), listOf(3, 5),
            listOf(4, 5), listOf(3, 6), listOf(4, 6), listOf(3, 7)
        ),
        start = listOf(7, 8),
        scale = 1.5
    ),
    LevelData( // 30
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(5, 3), listOf(2, 4), listOf(4, 4),
            listOf(7, 4), listOf(5, 5), listOf(6, 5), listOf(3, 6),
            listOf(4, 6), listOf(5, 8), listOf(4, 9), listOf(6, 9),
            listOf(12, 9), listOf(5, 11), listOf(6, 11), listOf(7, 11)
        ),
        goals = listOf(
            listOf(14, 3), listOf(15, 3), listOf(16, 3), listOf(17, 3),
            listOf(14, 4), listOf(15, 4), listOf(16, 4), listOf(17, 4),
            listOf(14, 5), listOf(15, 5), listOf(16, 5), listOf(17, 5),
            listOf(14, 6), listOf(15, 6), listOf(16, 6), listOf(17, 6)
        ),
        start = listOf(2, 2),
        scale = 1.2
    ),
    LevelData( // 31
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(7, 2), listOf(10, 2), listOf(6, 3), listOf(10, 5),
            listOf(9, 6), listOf(11, 6), listOf(4, 7), listOf(7, 7),
            listOf(9, 7), listOf(11, 7)
        ),
        goals = listOf(
            listOf(1, 1), listOf(2, 1), listOf(1, 2), listOf(2, 2),
            listOf(1, 3), listOf(2, 3), listOf(1, 4), listOf(2, 4),
            listOf(1, 5), listOf(2, 5)
        ),
        start = listOf(6, 4),
        scale = 1.4
    ),
    LevelData( // 32
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0),
            listOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0),
            listOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(3, 3), listOf(4, 4), listOf(4, 5), listOf(5, 6),
            listOf(6, 7), listOf(7, 8), listOf(8, 9), listOf(9, 10)
        ),
        goals = listOf(
            listOf(6, 2), listOf(7, 2), listOf(8, 2), listOf(9, 2),
            listOf(10, 2), listOf(8, 3), listOf(9, 3), listOf(10, 3)
        ),
        start = listOf(1, 2),
        scale = 1.4
    ),
    LevelData( // 33
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(10, 2), listOf(12, 2), listOf(10, 3), listOf(13, 3),
            listOf(10, 4), listOf(12, 4), listOf(10, 5), listOf(10, 6),
            listOf(13, 6), listOf(9, 7), listOf(12, 7)
        ),
        goals = listOf(
            listOf(1, 6), listOf(2, 6), listOf(3, 6), listOf(4, 6),
            listOf(2, 7), listOf(3, 7), listOf(4, 7), listOf(1, 8),
            listOf(2, 8), listOf(3, 8), listOf(4, 8)
        ),
        start = listOf(11, 1),
        scale = 1.4
    ),
    LevelData( // 34
        tiles = listOf(
            listOf(0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(4, 2), listOf(6, 2), listOf(7, 3), listOf(10, 3), listOf(11, 3),
            listOf(11, 5), listOf(5, 6), listOf(6, 6), listOf(9, 6)
        ),
        goals = listOf(
            listOf(1, 5), listOf(2, 5), listOf(3, 5), listOf(1, 6), listOf(2, 6),
            listOf(3, 6), listOf(1, 7), listOf(2, 7), listOf(3, 7)
        ),
        start = listOf(10, 2),
        scale = 1.5
    ),
    LevelData( // 35
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(8, 3), listOf(9, 3), listOf(9, 4), listOf(9, 5),
            listOf(5, 6), listOf(8, 6), listOf(6, 7), listOf(9, 7),
            listOf(5, 8), listOf(8, 8)
        ),
        goals = listOf(
            listOf(1, 1), listOf(2, 1), listOf(1, 2), listOf(2, 2),
            listOf(1, 3), listOf(2, 3), listOf(1, 4), listOf(2, 4),
            listOf(1, 5), listOf(2, 5)
        ),
        start = listOf(10, 9),
        scale = 1.4
    ),
    LevelData( // 36
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0),
            listOf(1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(12, 2), listOf(7, 3), listOf(8, 3), listOf(7, 5),
            listOf(9, 5), listOf(12, 5), listOf(15, 5), listOf(12, 8), listOf(15, 8)
        ),
        goals = listOf(
            listOf(1, 6), listOf(2, 6), listOf(3, 6), listOf(1, 7), listOf(2, 7),
            listOf(3, 7), listOf(1, 8), listOf(2, 8), listOf(3, 8)
        ),
        start = listOf(16, 6),
        scale = 1.2
    ),
    LevelData( // 37
        tiles = listOf(
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0),
            listOf(0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(10, 2), listOf(11, 2), listOf(2, 3), listOf(4, 3), listOf(6, 3),
            listOf(8, 3), listOf(10, 3), listOf(3, 4), listOf(5, 4), listOf(4, 5),
            listOf(5, 5), listOf(2, 6), listOf(3, 7), listOf(4, 7), listOf(3, 8),
            listOf(5, 9), listOf(2, 10), listOf(3, 10), listOf(7, 11)
        ),
        goals = listOf(
            listOf(13, 5), listOf(12, 6), listOf(13, 6), listOf(14, 6), listOf(12, 7),
            listOf(13, 7), listOf(14, 7), listOf(12, 8), listOf(13, 8), listOf(14, 8),
            listOf(12, 9), listOf(13, 9), listOf(14, 9), listOf(12, 10), listOf(13, 10),
            listOf(14, 10), listOf(12, 11), listOf(13, 11), listOf(14, 11)
        ),
        start = listOf(3, 1),
        scale = 1.2
    ),
    LevelData( // 38
        tiles = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0),
            listOf(1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(5, 2), listOf(9, 2), listOf(8, 3), listOf(9, 3), listOf(4, 4),
            listOf(2, 5), listOf(3, 5), listOf(4, 5), listOf(8, 6), listOf(9, 7),
            listOf(2, 8), listOf(5, 8), listOf(7, 10), listOf(8, 11), listOf(9, 12)
        ),
        goals = listOf(
            listOf(4, 9), listOf(5, 9), listOf(4, 10), listOf(5, 10), listOf(1, 11),
            listOf(2, 11), listOf(3, 11), listOf(4, 11), listOf(5, 11), listOf(2, 12),
            listOf(3, 12), listOf(4, 12), listOf(5, 12), listOf(3, 13), listOf(4, 13)
        ),
        start = listOf(3, 1),
        scale = 1.0
    ),
    LevelData( // 39
        tiles = listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0),
            listOf(1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0)
        ),
        boxes = listOf(
            listOf(7, 7), listOf(8, 7), listOf(9, 7), listOf(8, 8), listOf(10, 8),
            listOf(6, 9), listOf(8, 9), listOf(3, 10), listOf(6, 10), listOf(11, 10),
            listOf(14, 10), listOf(7, 11), listOf(8, 11), listOf(10, 11), listOf(7, 12)
        ),
        goals = listOf(
            listOf(13, 1), listOf(14, 1), listOf(15, 1), listOf(13, 2), listOf(14, 2),
            listOf(15, 2), listOf(13, 3), listOf(14, 3), listOf(15, 3), listOf(13, 4),
            listOf(14, 4), listOf(15, 4), listOf(13, 5), listOf(14, 5), listOf(15, 5)
        ),
        start = listOf(1, 10),
        scale = 1.0
    ),
    LevelData( // 40
        tiles = listOf(
            listOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0),
            listOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
            listOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0),
            listOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0)
        ),
        boxes = listOf(
            listOf(2, 3), listOf(4, 3), listOf(6, 3), listOf(8, 3), listOf(10, 3), listOf(12, 3),
            listOf(1, 4), listOf(3, 4), listOf(5, 4), listOf(7, 4), listOf(9, 4), listOf(11, 4),
            listOf(2, 5), listOf(4, 5), listOf(6, 5), listOf(8, 5), listOf(10, 5), listOf(12, 5),
            listOf(1, 6), listOf(3, 6), listOf(5, 6), listOf(7, 6), listOf(9, 6), listOf(11, 6)
        ),
        goals = listOf(
            listOf(1, 1), listOf(2, 1), listOf(11, 1), listOf(12, 1),
            listOf(2, 2), listOf(4, 2), listOf(5, 2), listOf(6, 2),
            listOf(7, 2), listOf(8, 2), listOf(10, 2), listOf(12, 2),
            listOf(1, 7), listOf(3, 7), listOf(5, 7), listOf(6, 7),
            listOf(7, 7), listOf(8, 7), listOf(9, 7), listOf(11, 7),
            listOf(1, 8), listOf(2, 8), listOf(11, 8), listOf(12, 8)
        ),
        start = listOf(6, 4),
        scale = 1.75
    )
)
