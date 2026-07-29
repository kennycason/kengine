package com.kengine.assets

data class PortableSpriteAsset(
    val id: String,
    val source: String
)

data class PortableSpriteSheetAsset(
    val id: String,
    val source: String,
    val tileWidth: Int,
    val tileHeight: Int,
    val columns: Int
)

data class PortableMusicAsset(
    val id: String,
    val source: String
)

interface PortableAssetCatalog {
    val sprites: List<PortableSpriteAsset>
    val spriteSheets: List<PortableSpriteSheetAsset>
    val music: List<PortableMusicAsset>
}

object EmptyPortableAssetCatalog : PortableAssetCatalog {
    override val sprites: List<PortableSpriteAsset> = emptyList()
    override val spriteSheets: List<PortableSpriteSheetAsset> = emptyList()
    override val music: List<PortableMusicAsset> = emptyList()
}
