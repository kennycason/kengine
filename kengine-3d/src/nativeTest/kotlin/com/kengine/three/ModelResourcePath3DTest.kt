package com.kengine.three

import platform.posix.getpid
import platform.posix.system
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelResourcePath3DTest {
    @Test
    fun resolvesRelativeSiblingPath() {
        assertEquals(
            "/tmp/model/textures/albedo.png",
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/scene.gltf",
                path = "textures/albedo.png"
            )
        )
    }

    @Test
    fun decodesGltfUriPathAndStripsFragment() {
        assertEquals(
            "/tmp/model/buffers/main buffer.bin",
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/scene.gltf",
                path = "buffers/main%20buffer.bin#buffer0",
                decodeUriPath = true,
                stripFragment = true
            )
        )
    }

    @Test
    fun preservesAbsolutePathByDefault() {
        assertEquals(
            "/Assets/textures/albedo.png",
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/scene.gltf",
                path = "/Assets/textures/albedo.png"
            )
        )
    }

    @Test
    fun resolvesMissingRootRelativePathFromParentWhenRequested() {
        assertEquals(
            "/tmp/model/Maps/albedo.png",
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/materials.mtl",
                path = "/Maps/albedo.png",
                resolveRootRelativeFromParent = true
            )
        )
    }

    @Test
    fun preservesExistingAbsolutePathWhenRootRelativeModeIsRequested() {
        val dir = "/tmp/kengine-3d-resource-path-${getpid()}"
        system("mkdir -p $dir")
        val absolutePath = "$dir/albedo.png"
        system("touch $absolutePath")

        assertEquals(
            absolutePath,
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/materials.mtl",
                path = absolutePath,
                resolveRootRelativeFromParent = true
            )
        )
    }

    @Test
    fun preservesWindowsAbsolutePath() {
        val windowsPath = "C:\\assets\\albedo.png"
        assertEquals(
            windowsPath,
            ModelResourcePath3D.resolveSiblingPath(
                filePath = "/tmp/model/materials.mtl",
                path = windowsPath,
                resolveRootRelativeFromParent = true
            )
        )
    }
}
