package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelLoader3DTest {
    @Test
    fun detectsSupportedFormatsCaseInsensitively() {
        assertEquals(ModelFormat3D.GLB, ModelLoader3D.detectFormat("assets/models/world.glb"))
        assertEquals(ModelFormat3D.GLB, ModelLoader3D.detectFormat("assets/models/WORLD.GLB"))
        assertEquals(ModelFormat3D.OBJ, ModelLoader3D.detectFormat("assets/models/ship.obj"))
        assertEquals(ModelFormat3D.OBJ, ModelLoader3D.detectFormat("assets/models/SHIP.OBJ"))
    }

    @Test
    fun rejectsUnsupportedFormats() {
        assertFailsWith<IllegalArgumentException> {
            ModelLoader3D.detectFormat("assets/models/world.fbx")
        }
    }
}
