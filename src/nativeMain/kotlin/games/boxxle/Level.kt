package games.boxxle

import com.kengine.Vec2D
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet

object Tiles {
    const val Empty = 0
    const val Brick = 1
}


data class Box(val p: Vec2D, val scale: Double)

class Level(levelData: LevelData) {
    companion object {
        private val sprites = SpriteSheet(Sprite("images/boxxle/boxxle.bmp"), 32, 32)
        private val brickSprite = sprites.getTile(0, 0)
        private val boxSprite = sprites.getTile(1, 0)
        private val boxSet = sprites.getTile(2, 0)
        private val goalSprite = sprites.getTile(3, 0)
    }

    val tiles: List<List<Int>> = levelData.tiles
    val boxes: MutableList<Box> = mutableListOf()
    val goals: MutableList<Vec2D> = mutableListOf()
    val start: Vec2D
    val scale: Double

    init {
        for (box in levelData.boxes) {
            boxes.add(Box(p = Vec2D(x = box[0].toDouble(), y = box[1].toDouble()), scale = levelData.scale))
        }
        for (goal in levelData.goals) {
            goals.add(Vec2D(x = goal[0].toDouble(), y = goal[1].toDouble()))
        }
        start = Vec2D(x = levelData.start[0].toDouble(), y = levelData.start[1].toDouble())
        scale = levelData.scale
    }

    fun draw() {
        tiles.forEachIndexed { y, row ->
            row.forEachIndexed { x, tileType ->
                when (tileType) {
                    Tiles.Brick -> {
                        brickSprite.draw(x * 32 * scale, y * 32 * scale)
                    }
                }
            }
        }

        for (box in boxes) {
            boxSprite.draw(box.p.x * 32 * scale, box.p.y * 32 * scale)
        }

        for (goal in goals) {
            goalSprite.draw(goal.x * 32 * scale, goal.y * 32 * scale)
        }
    }
}