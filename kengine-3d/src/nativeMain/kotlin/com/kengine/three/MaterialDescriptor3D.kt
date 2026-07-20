package com.kengine.three

import com.kengine.graphics.Color

data class MaterialTextureSet3D(
    val baseColor: GpuTextureAsset3D? = null,
    val normal: GpuTextureAsset3D? = null,
    val metallicRoughness: GpuTextureAsset3D? = null,
    val roughness: GpuTextureAsset3D? = null,
    val metallic: GpuTextureAsset3D? = null,
    val specular: GpuTextureAsset3D? = null,
    val emissive: GpuTextureAsset3D? = null,
    val ambient: GpuTextureAsset3D? = null,
    val alpha: GpuTextureAsset3D? = null,
    val displacement: GpuTextureAsset3D? = null
) {
    val hasBaseColor: Boolean
        get() = baseColor != null

    val hasSecondaryTextures: Boolean
        get() = listOfNotNull(
            normal,
            metallicRoughness,
            roughness,
            metallic,
            specular,
            emissive,
            ambient,
            alpha,
            displacement
        ).isNotEmpty()

    val all: List<GpuTextureAsset3D>
        get() = listOfNotNull(
            baseColor,
            normal,
            metallicRoughness,
            roughness,
            metallic,
            specular,
            emissive,
            ambient,
            alpha,
            displacement
        )

    val distinctTextureCount: Int
        get() = all.map { it.key }.distinct().size

    fun withBaseColor(textureAsset: GpuTextureAsset3D): MaterialTextureSet3D {
        return copy(baseColor = textureAsset)
    }
}

data class MaterialTextureSlotUsage3D(
    val baseColor: Int = 0,
    val normal: Int = 0,
    val metallicRoughness: Int = 0,
    val roughness: Int = 0,
    val metallic: Int = 0,
    val specular: Int = 0,
    val emissive: Int = 0,
    val ambient: Int = 0,
    val alpha: Int = 0,
    val displacement: Int = 0
) {
    val renderedSlotCount: Int
        get() = baseColor + normal

    val secondarySlotCount: Int
        get() = normal + metallicRoughness + roughness + metallic + specular + emissive + ambient + alpha + displacement

    val totalSlotCount: Int
        get() = baseColor + secondarySlotCount

    companion object {
        fun fromTextureSets(textureSets: Iterable<MaterialTextureSet3D>): MaterialTextureSlotUsage3D {
            var baseColor = 0
            var normal = 0
            var metallicRoughness = 0
            var roughness = 0
            var metallic = 0
            var specular = 0
            var emissive = 0
            var ambient = 0
            var alpha = 0
            var displacement = 0

            textureSets.forEach { textures ->
                if (textures.baseColor != null) baseColor += 1
                if (textures.normal != null) normal += 1
                if (textures.metallicRoughness != null) metallicRoughness += 1
                if (textures.roughness != null) roughness += 1
                if (textures.metallic != null) metallic += 1
                if (textures.specular != null) specular += 1
                if (textures.emissive != null) emissive += 1
                if (textures.ambient != null) ambient += 1
                if (textures.alpha != null) alpha += 1
                if (textures.displacement != null) displacement += 1
            }

            return MaterialTextureSlotUsage3D(
                baseColor = baseColor,
                normal = normal,
                metallicRoughness = metallicRoughness,
                roughness = roughness,
                metallic = metallic,
                specular = specular,
                emissive = emissive,
                ambient = ambient,
                alpha = alpha,
                displacement = displacement
            )
        }
    }
}

data class MaterialDescriptor3D(
    val name: String? = null,
    val baseColor: Color = Color.fromHex("ffffff"),
    val textures: MaterialTextureSet3D = MaterialTextureSet3D()
) {
    val textureAsset: GpuTextureAsset3D?
        get() = textures.baseColor

    val hasTexture: Boolean
        get() = textures.hasBaseColor

    val hasSecondaryTextures: Boolean
        get() = textures.hasSecondaryTextures

    val textureCount: Int
        get() = textures.distinctTextureCount

    fun upload(
        gpu: GpuContext,
        textureCache: GpuTextureCache3D? = null
    ): Material3D {
        fun loadTexture(asset: GpuTextureAsset3D): GpuTexture {
            return textureCache?.load(asset) ?: gpu.loadTexture3D(asset)
        }

        var texture: GpuTexture? = null
        var normalTexture: GpuTexture? = null
        try {
            texture = textureAsset?.let(::loadTexture)
            normalTexture = if (textureAsset != null) {
                loadTexture(textures.normal ?: GpuTextureAsset3D.flatNormalRgba8())
            } else {
                null
            }
        } catch (e: Throwable) {
            if (textureCache == null) {
                normalTexture?.cleanup()
                texture?.cleanup()
            }
            throw e
        }
        val textureOwnership =
            if (texture != null && textureCache != null) {
                GpuResourceOwnership3D.BORROWED
            } else {
                GpuResourceOwnership3D.OWNED
            }
        val normalTextureOwnership =
            if (normalTexture != null && textureCache != null) {
                GpuResourceOwnership3D.BORROWED
            } else {
                GpuResourceOwnership3D.OWNED
            }

        return Material3D(
            name = name,
            baseColor = baseColor,
            texture = texture,
            textureOwnership = textureOwnership,
            normalTexture = normalTexture,
            normalTextureOwnership = normalTextureOwnership,
            hasAuthoredNormalTexture = textures.normal != null
        )
    }

    companion object {
        fun solid(
            color: Color = Color.fromHex("ffffff"),
            name: String? = null,
            textures: MaterialTextureSet3D = MaterialTextureSet3D()
        ): MaterialDescriptor3D {
            require(!textures.hasBaseColor) {
                "Solid material descriptors cannot include a base color texture."
            }
            return MaterialDescriptor3D(
                name = name,
                baseColor = color,
                textures = textures
            )
        }

        fun textured(
            textureAsset: GpuTextureAsset3D,
            color: Color = Color.fromHex("ffffff"),
            name: String? = null,
            textures: MaterialTextureSet3D = MaterialTextureSet3D()
        ): MaterialDescriptor3D {
            return MaterialDescriptor3D(
                name = name,
                baseColor = color,
                textures = textures.withBaseColor(textureAsset)
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
