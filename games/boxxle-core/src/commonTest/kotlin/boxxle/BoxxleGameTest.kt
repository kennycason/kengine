package boxxle

import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioCommandBuffer
import com.kengine.audio.AudioCommandType
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoxxleGameTest {
    @Test
    fun requestsLoopedMainMusicThroughPortableAudio() {
        val game = BoxxleGame()
        val audio = AudioContext(commandCapacity = 4)

        audio.beginFrame()
        game.audio(audio)

        assertEquals(1, audio.commandCount)
        assertEquals(0, audio.droppedCommandCount)
        assertEquals(AudioCommandType.LOOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(
            AudioAssetId.music(BoxxleAssets.MAIN_ID),
            audio.commandField(0, AudioCommandBuffer.FIELD_ASSET_ID)
        )
        assertEquals(230, audio.commandField(0, AudioCommandBuffer.FIELD_VOLUME))
    }

    @Test
    fun pushesBoxOnFirstLevel() {
        val game = BoxxleGame()
        press(game, InputButton.DOWN)
        press(game, InputButton.RIGHT)
        press(game, InputButton.UP)
        press(game, InputButton.RIGHT)
        press(game, InputButton.DOWN)

        assertContains(game.snapshot(), "boxes=3,3")
    }

    @Test
    fun bResetsCurrentLevel() {
        val game = BoxxleGame()
        press(game, InputButton.DOWN)

        game.update(input(InputButton.B))

        assertContains(game.snapshot(), "level=0 player=1,1 boxes=2,2")
    }

    @Test
    fun shoulderButtonsChangeLevels() {
        val game = BoxxleGame()

        repeat(17) {
            game.update(input(InputButton.R))
        }
        assertContains(game.snapshot(), "level=0")

        game.update(input(InputButton.R))
        game.update(input())
        assertContains(game.snapshot(), "level=1")

        repeat(18) {
            game.update(input(InputButton.L))
        }
        assertContains(game.snapshot(), "level=0")
    }

    @Test
    fun desktopBriefTapMovesOnlyOneTile() {
        val game = BoxxleGame()

        repeat(3) {
            game.update(input(InputButton.RIGHT))
        }
        repeat(8) {
            game.update(input())
        }

        assertContains(game.snapshot(), "player=2,1")
    }

    @Test
    fun holdingDirectionChainsIntoNextMove() {
        val game = BoxxleGame()

        repeat(8) {
            game.update(input(InputButton.RIGHT))
        }

        assertContains(game.snapshot(), "player=3,1")
    }

    @Test
    fun n64TimingChainsHeldDirectionFaster() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        repeat(2) {
            game.update(input(InputButton.RIGHT))
        }

        assertContains(game.snapshot(), "player=3,1")
    }

    @Test
    fun n64SingleTapMovesOnlyOneTile() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        game.update(input(InputButton.RIGHT))
        repeat(3) {
            game.update(input())
        }

        assertContains(game.snapshot(), "player=2,1")
    }

    @Test
    fun n64TapDuringMoveQueuesNextTile() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        game.update(input(InputButton.RIGHT))
        game.update(input(InputButton.DOWN))
        game.update(input())

        assertContains(game.snapshot(), "player=2,2")
    }

    @Test
    fun n64LevelShortcutRequiresDeliberateHold() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        game.update(input(InputButton.R))
        game.update(input())
        assertContains(game.snapshot(), "level=0")

        repeat(5) {
            game.update(input(InputButton.R))
        }
        assertContains(game.snapshot(), "level=1")
    }

    @Test
    fun levelShortcutIsIgnoredWhileMoving() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        repeat(6) {
            game.update(input(InputButton.RIGHT, InputButton.R))
        }

        assertContains(game.snapshot(), "level=0")
    }

    @Test
    fun newlyPressedDirectionOverridesHeldDirectionWhenMoveCompletes() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)

        game.update(input(InputButton.RIGHT))
        game.update(input(InputButton.RIGHT, InputButton.DOWN))
        game.update(input(InputButton.RIGHT, InputButton.DOWN))

        assertContains(game.snapshot(), "player=2,2")
    }

    @Test
    fun finalLevelFitsN64RenderCommandCapacity() {
        val game = BoxxleGame(BoxxleTiming.Nintendo64)
        repeat(LEVEL_DATA.lastIndex) {
            repeat(5) {
                game.update(input(InputButton.R))
            }
            game.update(input())
        }

        val render = RenderContext(commandCapacity = 256)
        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
    }

    @Test
    fun playerDrawsBetweenTilesDuringMove() {
        val game = BoxxleGame()

        game.update(input(InputButton.RIGHT))
        val startX = spriteDrawX(game, PLAYER_RIGHT_FRAME)

        game.update(input())
        val movingX = spriteDrawX(game, PLAYER_RIGHT_FRAME)

        repeat(12) {
            game.update(input())
        }
        val landedX = spriteDrawX(game, PLAYER_RIGHT_FRAME)

        assertTrue(movingX > startX, "expected player sprite to move right from $startX, got $movingX")
        assertTrue(movingX < landedX, "expected player sprite to be between tiles before landing at $landedX, got $movingX")
    }

    @Test
    fun playerAndBoxDrawBetweenTilesDuringPush() {
        val game = BoxxleGame()
        press(game, InputButton.DOWN)
        press(game, InputButton.RIGHT)
        press(game, InputButton.UP)
        press(game, InputButton.RIGHT)

        game.update(input(InputButton.DOWN))
        val playerStartY = spriteDrawY(game, PLAYER_DOWN_FRAME)
        val boxStartY = spriteDrawY(game, BOX_PLACED_FRAME)

        game.update(input())
        val playerMovingY = spriteDrawY(game, PLAYER_DOWN_FRAME)
        val boxMovingY = spriteDrawY(game, BOX_PLACED_FRAME)

        repeat(12) {
            game.update(input())
        }
        val playerLandedY = spriteDrawY(game, PLAYER_DOWN_FRAME)
        val boxLandedY = spriteDrawY(game, BOX_PLACED_FRAME)

        assertTrue(playerMovingY > playerStartY, "expected player sprite to move down from $playerStartY, got $playerMovingY")
        assertTrue(playerMovingY < playerLandedY, "expected player sprite to be between tiles before landing at $playerLandedY, got $playerMovingY")
        assertTrue(boxMovingY > boxStartY, "expected box sprite to move down from $boxStartY, got $boxMovingY")
        assertTrue(boxMovingY < boxLandedY, "expected box sprite to be between tiles before landing at $boxLandedY, got $boxMovingY")
    }

    private fun press(game: BoxxleGame, vararg buttons: InputButton) {
        game.update(input(*buttons))
        repeat(14) {
            game.update(input())
        }
    }

    private fun spriteDrawX(game: BoxxleGame, frame: Int): Int {
        return spriteDrawField(game, frame, RenderCommandBuffer.FIELD_X)
    }

    private fun spriteDrawY(game: BoxxleGame, frame: Int): Int {
        return spriteDrawField(game, frame, RenderCommandBuffer.FIELD_Y)
    }

    private fun spriteDrawField(game: BoxxleGame, frame: Int, field: Int): Int {
        val render = RenderContext()
        render.beginFrame(320, 240)
        game.draw(render)

        for (index in 0 until render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_SPRITE &&
                render.commandField(index, RenderCommandBuffer.FIELD_PARAM) == frame
            ) {
                return render.commandField(index, field)
            }
        }
        error("Sprite frame $frame was not drawn")
    }

    private fun input(vararg buttons: InputButton): InputState {
        return InputState().also { state ->
            buttons.forEach { state.set(it) }
        }
    }

    private companion object {
        const val BOX_PLACED_FRAME = 2
        const val PLAYER_DOWN_FRAME = 5
        const val PLAYER_RIGHT_FRAME = 7
    }
}
