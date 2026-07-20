package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals

class GpuUpload3DTest {
    @Test
    fun uploadByteSizesMatchArrayStorage() {
        assertEquals(12u, floatArrayOf(1f, 2f, 3f).gpuByteSize3D())
        assertEquals(3u, byteArrayOf(1, 2, 3).gpuByteSize3D())
        assertEquals(0u, FloatArray(0).gpuByteSize3D())
        assertEquals(0u, ByteArray(0).gpuByteSize3D())
    }
}
