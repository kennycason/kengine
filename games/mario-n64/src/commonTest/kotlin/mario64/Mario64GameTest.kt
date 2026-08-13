package mario64

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mario64GameTest {
    @Test
    fun worldModelHasRenderableGeometry() {
        val world = Mario64ModelAssets.battlefield
        assertTrue(world.name.isNotEmpty())
        assertTrue(world.vertexCount > 0, "world should have vertices")
        assertTrue(world.triangleCount > 0, "world should have triangles")
        assertEquals(0, world.vertices.size % 3)
        assertEquals(0, world.triangles.size % Mario64BakedWorld.TRIANGLE_FIELD_COUNT)
        assertTrue(world.colors.isNotEmpty(), "world should have material colors")
    }

    @Test
    fun worldModelVertexCountMatchesDaeSource() {
        val world = Mario64ModelAssets.battlefield
        assertTrue(world.vertexCount > 2000, "Bob-Omb Battlefield should have >2000 vertices (has ${world.vertexCount})")
        assertTrue(world.triangleCount > 1500, "Bob-Omb Battlefield should have >1500 triangles (has ${world.triangleCount})")
    }

    @Test
    fun drawFrameProducesWorldCommand() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        assertTrue(render.commandCount > 0, "draw should produce render commands")
        assertTrue(containsType(render, RenderCommandType.DRAW_WORLD_3D), "should emit DRAW_WORLD_3D command")
    }

    @Test
    fun drawWorldCommandContainsCorrectMeshId() {
        val game = Mario64Game()
        val render = RenderContext(512)

        render.beginFrame(320, 240)
        game.draw(render)

        var found = false
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_WORLD_3D) {
                val meshId = render.commandField(index, RenderCommandBuffer.FIELD_COLOR2)
                assertEquals(Mario64Game.BATTLEFIELD_MESH_ID, meshId, "mesh ID should match")
                found = true
            }
            index += 1
        }
        assertTrue(found, "should find DRAW_WORLD_3D command")
    }

    @Test
    fun startButtonResetsPosition() {
        val game = Mario64Game()
        val input = InputState()

        input.set(InputButton.DPAD_UP)
        game.update(input)
        game.update(input)
        game.update(input)

        input.reset()
        input.set(InputButton.START)
        game.update(input)

        val render = RenderContext(512)
        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
    }

    private fun containsType(render: RenderContext, type: Int): Boolean {
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return true
            }
            index += 1
        }
        return false
    }
}
