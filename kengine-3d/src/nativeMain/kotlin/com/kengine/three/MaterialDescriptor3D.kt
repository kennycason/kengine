package com.kengine.three

import com.kengine.graphics.Color

data class MaterialDescriptor3D(
    val name: String? = null,
    val baseColor: Color = Color.fromHex("ffffff"),
    val textureAsset: GpuTextureAsset3D? = null
) {
    val hasTexture: Boolean
        get() = textureAsset != null

    fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): Material3D {
        val texture = textureAsset?.let { asset ->
            textureCache?.load(asset) ?: gpu.loadTexture3D(asset)
        }
        val textureOwnership =
            if (texture != null && textureCache != null) {
                GpuResourceOwnership3D.BORROWED
            } else {
                GpuResourceOwnership3D.OWNED
            }

        return Material3D(
            name = name,
            baseColor = baseColor,
            texture = texture,
            textureOwnership = textureOwnership
        )
    }

    companion object {
        fun solid(
            color: Color = Color.fromHex("ffffff"),
            name: String? = null
        ): MaterialDescriptor3D {
            return MaterialDescriptor3D(
                name = name,
                baseColor = color
            )
        }

        fun textured(
            textureAsset: GpuTextureAsset3D,
            color: Color = Color.fromHex("ffffff"),
            name: String? = null
        ): MaterialDescriptor3D {
            return MaterialDescriptor3D(
                name = name,
                baseColor = color,
                textureAsset = textureAsset
            )
        }
    }
}

fun GpuContext.uploadMaterial3D(
    descriptor: MaterialDescriptor3D,
    textureCache: GpuTextureCache3D? = null
): Material3D {
    return descriptor.upload(this, textureCache)
}
