package games.boxxle

import com.kengine.Vec2
import com.kengine.graphics.SpriteContext

object Tiles {
    const val EMPTY = 0
    const val BRICK = 1
}

class Level(val data: LevelData) {
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet("boxxle")
    private val brickSprite = spriteSheet.getTile(0, 0)
    private val goalSprite = spriteSheet.getTile(3, 0)

    val tiles: List<List<Int>> = data.tiles
    val boxes: MutableList<Box> = mutableListOf()
    val goals: MutableList<Vec2> = mutableListOf()
    val start: Vec2

    init {
        for (box in data.boxes) {
            boxes.add(Box(p = Vec2(x = box[0].toDouble(), y = box[1].toDouble()), scale = data.scale))
        }
        for (goal in data.goals) {
            goals.add(Vec2(x = goal[0].toDouble(), y = goal[1].toDouble()))
        }
        start = Vec2(x = data.start[0].toDouble(), y = data.start[1].toDouble())
        brickSprite.scale.set(data.scale)
        goalSprite.scale.set(data.scale)
    }

    fun update(elapsedSeconds: Double) {

    }

    fun draw(elapsedSeconds: Double) {
        tiles.forEachIndexed { y, row ->
            row.forEachIndexed { x, tileType ->
                when (tileType) {
                    Tiles.BRICK -> {
                        brickSprite.draw(x * 32.0, y * 32.0)
                    }
                }
            }
        }

        for (box in boxes) {
            box.draw()
        }

        for (goal in goals) {
            goalSprite.draw(goal.x * 32, goal.y * 32)
        }
    }
}