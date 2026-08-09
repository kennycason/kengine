package n64demo

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class N64DemoGameTest {
    @Test
    fun bakedModelsHaveRenderableGeometry() {
        assertTrue(N64DemoModelAssets.all.isNotEmpty())

        N64DemoModelAssets.all.forEach { model ->
            assertTrue(model.name.isNotEmpty())
            assertTrue(model.source.endsWith(".obj"))
            assertTrue(model.license.contains("CC0"))
            assertTrue(model.vertexCount > 0, "${model.name} should have vertices")
            assertTrue(model.edgeCount > 0, "${model.name} should have edges")
            assertTrue(model.triangleCount > 0, "${model.name} should have triangles")
            assertEquals(0, model.vertices.size % 3)
            assertEquals(0, model.edges.size % N64BakedWireModel3D.EDGE_FIELD_COUNT)
            assertEquals(0, model.triangles.size % N64BakedWireModel3D.TRIANGLE_FIELD_COUNT)
            assertTrue(model.colors.isNotEmpty(), "${model.name} should preserve material colors")
        }
    }

    @Test
    fun drawFrameFitsN64RenderCommandCapacity() {
        val game = N64DemoGame()
        val render = RenderContext(256)

        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
        assertTrue(render.commandCount <= 256)
        assertTrue(render.containsType(RenderCommandType.DRAW_TRIANGLE), "N64 demo should render filled model triangles")
        assertTriangleCommandCoordinatesStayReasonable(render)

        val input = InputState()
        input.set(InputButton.A)
        game.update(input)

        render.beginFrame(320, 240)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
        assertTrue(render.commandCount <= 256)
        assertTrue(render.containsType(RenderCommandType.DRAW_TRIANGLE), "N64 demo should render filled model triangles")
        assertTriangleCommandCoordinatesStayReasonable(render)
    }

    private fun assertTriangleCommandCoordinatesStayReasonable(render: RenderContext) {
        var index = 0
        while (index < render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_TRIANGLE) {
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_X))
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_Y))
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_WIDTH))
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_HEIGHT))
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_COLOR2))
                assertReasonableScreenCoordinate(render.commandField(index, RenderCommandBuffer.FIELD_PARAM))
            }
            index += 1
        }
    }

    private fun assertReasonableScreenCoordinate(value: Int) {
        assertTrue(value in -1024..1024, "triangle coordinate should stay fenced, was $value")
    }

    private fun RenderContext.containsType(type: Int): Boolean {
        var index = 0
        while (index < commandCount) {
            if (commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return true
            }
            index += 1
        }
        return false
    }
}
