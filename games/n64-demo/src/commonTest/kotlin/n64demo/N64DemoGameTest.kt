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
    fun bakedModelOverlayDoesNotStartWithLongestEdges() {
        N64DemoModelAssets.all.forEach { model ->
            val edgeLengths = model.edgeLengthsSquared()
            val previewLengths = edgeLengths.take(N64_EDGE_PREVIEW_BUDGET)

            assertTrue(previewLengths.isNotEmpty(), "${model.name} should have overlay preview edges")
            assertTrue(
                (previewLengths.maxOrNull() ?: 0) < (edgeLengths.maxOrNull() ?: 0),
                "${model.name} should avoid spending the first N64 overlay budget on the longest edges"
            )
        }
    }

    @Test
    fun bakedOverlayEdgesReferenceValidTriangles() {
        N64DemoModelAssets.all.forEach { model ->
            var edgeIndex = 0
            while (edgeIndex < model.edgeCount) {
                val edgeBase = edgeIndex * N64BakedWireModel3D.EDGE_FIELD_COUNT
                val start = model.edges[edgeBase]
                val end = model.edges[edgeBase + 1]
                val colorIndex = model.edges[edgeBase + 2]
                val firstTriangle = model.edges[edgeBase + 3]
                val secondTriangle = model.edges[edgeBase + 4]

                assertTrue(start in 0 until model.vertexCount, "${model.name} edge $edgeIndex should have a valid start")
                assertTrue(end in 0 until model.vertexCount, "${model.name} edge $edgeIndex should have a valid end")
                assertTrue(colorIndex in model.colors.indices, "${model.name} edge $edgeIndex should have a valid color")
                assertTrue(firstTriangle in -1 until model.triangleCount, "${model.name} edge $edgeIndex should have a valid first triangle")
                assertTrue(secondTriangle in -1 until model.triangleCount, "${model.name} edge $edgeIndex should have a valid second triangle")
                assertTrue(
                    firstTriangle >= 0 || secondTriangle >= 0,
                    "${model.name} edge $edgeIndex should be attached to at least one triangle"
                )

                edgeIndex += 1
            }
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

    @Test
    fun modelOverlayDrawsRearEdgesBeforeCoveringFaces() {
        val renderer = N64WireModelRenderer3D()
        val render = RenderContext(32)
        val model = N64BakedWireModel3D(
            name = "Occlusion Probe",
            source = "occlusion-probe.obj",
            license = "test",
            vertices = intArrayOf(
                -512, -384, 0,
                512, -384, 0,
                0, 512, 0,
                -64, 0, 384,
                64, 0, 384,
                0, 96, 384
            ),
            edges = intArrayOf(
                3, 4, 0, 1, -1
            ),
            triangles = intArrayOf(
                0, 1, 2, 0,
                3, 4, 5, 0
            ),
            colors = intArrayOf(n64Rgba(180, 210, 230))
        )

        render.beginFrame(320, 240)
        renderer.configure(
            render = render,
            targetX = 0,
            targetY = 0,
            targetZ = 0,
            yaw = 0,
            pitch = 0,
            cameraDistance = 4 * N64WireModelRenderer3D.WORLD_SCALE,
            projectionDistance = 128,
            centerX = 160,
            centerY = 120
        )

        assertEquals(2, renderer.drawModelTriangles(model, 0, 0, 0, 256, 0, 0, 0, 2, 1))
        assertEquals(1, renderer.lastDrawnEdges)
        assertTrue(
            render.firstCommandIndex(RenderCommandType.DRAW_LINE) < render.lastCommandIndex(RenderCommandType.DRAW_TRIANGLE),
            "rear overlay edge should be submitted before the nearer covering triangle"
        )
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

    private fun N64BakedWireModel3D.edgeLengthsSquared(): List<Int> {
        val lengths = mutableListOf<Int>()
        var edgeIndex = 0
        while (edgeIndex < edgeCount) {
            val edgeBase = edgeIndex * N64BakedWireModel3D.EDGE_FIELD_COUNT
            val start = edges[edgeBase]
            val end = edges[edgeBase + 1]
            val startBase = start * 3
            val endBase = end * 3
            val dx = vertices[startBase] - vertices[endBase]
            val dy = vertices[startBase + 1] - vertices[endBase + 1]
            val dz = vertices[startBase + 2] - vertices[endBase + 2]
            lengths += dx * dx + dy * dy + dz * dz
            edgeIndex += 1
        }
        return lengths
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

    private fun RenderContext.firstCommandIndex(type: Int): Int {
        var index = 0
        while (index < commandCount) {
            if (commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return index
            }
            index += 1
        }
        return Int.MAX_VALUE
    }

    private fun RenderContext.lastCommandIndex(type: Int): Int {
        var index = commandCount - 1
        while (index >= 0) {
            if (commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return index
            }
            index -= 1
        }
        return Int.MIN_VALUE
    }

    private companion object {
        const val N64_EDGE_PREVIEW_BUDGET = 14
    }
}
